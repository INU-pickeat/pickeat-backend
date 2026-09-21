#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: sudo ./bootstrap.sh <deploy-user>" >&2
  exit 1
fi

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script with sudo." >&2
  exit 1
fi

DEPLOY_USER="$1"
DEPLOY_GROUP="$(id -gn "$DEPLOY_USER")"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SYSTEMCTL_PATH="$(command -v systemctl)"

apt-get update
apt-get install -y openjdk-21-jre-headless curl

install -d -m 0750 -o "$DEPLOY_USER" -g "$DEPLOY_GROUP" /opt/pickeat
install -d -m 0750 -o "$DEPLOY_USER" -g "$DEPLOY_GROUP" /opt/pickeat/releases
install -d -m 0750 -o root -g "$DEPLOY_GROUP" /etc/pickeat

if [[ ! -f /etc/pickeat/pickeat.env ]]; then
  install -m 0640 -o root -g "$DEPLOY_GROUP" "$SCRIPT_DIR/pickeat.env.example" /etc/pickeat/pickeat.env
fi

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

echo "Bootstrap complete. Edit /etc/pickeat/pickeat.env before the first deployment."
