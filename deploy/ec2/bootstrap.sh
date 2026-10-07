#!/usr/bin/env bash
# t3.micro 한 대에 앱(systemd) + PostGIS(Docker) + Nginx를 구성한다. 여러 번 실행해도 안전하다.
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: sudo ./bootstrap.sh <deploy-user> <server-name>" >&2
  echo "Example: sudo ./bootstrap.sh ubuntu api.pickeat.kr" >&2
  exit 1
fi

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script with sudo." >&2
  exit 1
fi

DEPLOY_USER="$1"
SERVER_NAME="$2"
DEPLOY_GROUP="$(id -gn "$DEPLOY_USER")"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE=/etc/pickeat/pickeat.env
POSTGRES_ENV_FILE=/etc/pickeat/postgres.env
POSTGRES_IMAGE=postgis/postgis:16-3.4

# 1GB 메모리 보완용 swap
if [[ ! -f /swapfile ]]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=10' > /etc/sysctl.d/99-pickeat-swap.conf
  sysctl --system > /dev/null
fi

apt-get update
apt-get install -y openjdk-21-jre-headless curl openssl docker.io nginx certbot python3-certbot-nginx
systemctl enable --now docker
SYSTEMCTL_PATH="$(command -v systemctl)"

install -d -m 0750 -o "$DEPLOY_USER" -g "$DEPLOY_GROUP" /opt/pickeat
install -d -m 0750 -o "$DEPLOY_USER" -g "$DEPLOY_GROUP" /opt/pickeat/releases
install -d -m 0750 -o root -g "$DEPLOY_GROUP" /etc/pickeat

# 최초 1회: DB 비밀번호와 JWT 비밀값을 무작위로 생성한다.
if [[ ! -f "$ENV_FILE" ]]; then
  sed \
    -e "s/__DB_PASSWORD__/$(openssl rand -hex 24)/" \
    -e "s/__JWT_SECRET__/$(openssl rand -hex 48)/" \
    "$SCRIPT_DIR/pickeat.env.example" > "$ENV_FILE"
  chown root:"$DEPLOY_GROUP" "$ENV_FILE"
  chmod 0640 "$ENV_FILE"
fi

# PostGIS 컨테이너는 앱 환경변수 파일의 DB 계정을 그대로 사용한다.
DB_USERNAME="$(grep '^DB_USERNAME=' "$ENV_FILE" | cut -d= -f2-)"
DB_PASSWORD="$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
install -m 0600 -o root -g root /dev/null "$POSTGRES_ENV_FILE"
cat > "$POSTGRES_ENV_FILE" <<EOF
POSTGRES_DB=pick_eat
POSTGRES_USER=$DB_USERNAME
POSTGRES_PASSWORD=$DB_PASSWORD
EOF

if ! docker container inspect pickeat-postgres > /dev/null 2>&1; then
  docker run -d \
    --name pickeat-postgres \
    --restart unless-stopped \
    --env-file "$POSTGRES_ENV_FILE" \
    -p 127.0.0.1:5432:5432 \
    -v pickeat-pgdata:/var/lib/postgresql/data \
    --memory 384m \
    "$POSTGRES_IMAGE" \
    -c shared_buffers=96MB \
    -c effective_cache_size=256MB \
    -c work_mem=4MB \
    -c maintenance_work_mem=32MB \
    -c max_connections=20
fi

for attempt in {1..30}; do
  if docker exec pickeat-postgres pg_isready -U "$DB_USERNAME" -d pick_eat > /dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    echo "PostgreSQL did not become ready." >&2
    exit 1
  fi
  sleep 2
done

# Nginx 리버스 프록시 (HTTPS는 DNS 연결 후 certbot으로 추가)
sed "s/__SERVER_NAME__/$SERVER_NAME/g" "$SCRIPT_DIR/nginx-pickeat.conf" > /etc/nginx/sites-available/pickeat
ln -sfn /etc/nginx/sites-available/pickeat /etc/nginx/sites-enabled/pickeat
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

# 매일 03:30 DB 백업
install -m 0755 "$SCRIPT_DIR/pickeat-backup.sh" /usr/local/bin/pickeat-backup
echo '30 3 * * * root /usr/local/bin/pickeat-backup' > /etc/cron.d/pickeat-backup

sed \
  -e "s/__DEPLOY_USER__/$DEPLOY_USER/g" \
  -e "s/__DEPLOY_GROUP__/$DEPLOY_GROUP/g" \
  "$SCRIPT_DIR/pickeat-backend.service" > /etc/systemd/system/pickeat-backend.service

cat > /etc/sudoers.d/pickeat-backend-deploy <<EOF
$DEPLOY_USER ALL=(root) NOPASSWD: $SYSTEMCTL_PATH restart pickeat-backend.service, $SYSTEMCTL_PATH is-active pickeat-backend.service
EOF
chmod 0440 /etc/sudoers.d/pickeat-backend-deploy
visudo -cf /etc/sudoers.d/pickeat-backend-deploy

systemctl daemon-reload
systemctl enable pickeat-backend.service

cat <<EOF
Bootstrap complete.
Next:
  1. sudoedit $ENV_FILE  # GOOGLE_PLACES_API_KEY, MAIL_PASSWORD 입력
  2. DNS A 레코드 $SERVER_NAME -> 이 인스턴스의 Elastic IP
  3. sudo certbot --nginx -d $SERVER_NAME  # HTTPS 인증서 발급·자동 갱신
EOF
