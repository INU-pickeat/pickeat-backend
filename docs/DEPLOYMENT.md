# AWS EC2 배포

Pick Eat 백엔드는 EC2에서 실행 JAR를 systemd로 관리하고, GitHub Actions가 CI 성공 후 새 릴리스를 배포한다.

## 현재 운영 상태 (2026-10-04)

- 운영 주소: `https://api.pickeat.kr` (Elastic IP `3.39.48.167`)
- EC2 생성, bootstrap, GitHub Secrets 등록, 최초 배포, DNS·HTTPS 연결까지 완료했다. 아래 1~3번은 새 인스턴스를 만들 때 따르는 절차다.
- `main`의 CI가 성공하면 자동 CD가 배포한다.
- 인증서 만료일은 2027-01-02이며 certbot 타이머가 자동 갱신한다.
- 아직 하지 않은 것: 자동 롤백 실검증, 후기 이미지용 S3 버킷·IAM 역할 생성.

## 확정 구조

- 실행 환경: AWS EC2 프리티어 **t3.micro** (2 vCPU, 1GB RAM) 한 대 + Ubuntu
- 애플리케이션: Java 21 실행 JAR를 systemd로 관리 (`JAVA_OPTS`로 힙 `-Xmx384m` 제한)
- DB: 같은 인스턴스의 Docker 컨테이너 `pickeat-postgres` (`postgis/postgis:16-3.4`, CI와 같은 이미지). `127.0.0.1:5432`에만 바인딩하고 데이터는 `pickeat-pgdata` 볼륨에 저장
- 프록시: Nginx가 80/443을 받아 `127.0.0.1:8080`으로 전달하고, `/actuator/**`는 외부에 404로 막음. HTTPS 인증서는 Let's Encrypt(certbot)
- 메모리 보완: 2GB swap (`vm.swappiness=10`)
- 백업: 매일 03:30 `pg_dump`로 `/var/backups/pickeat`에 7일치 보관
- 애플리케이션 경로: `/opt/pickeat`
- 환경변수 파일: `/etc/pickeat/pickeat.env` (DB 비밀번호·JWT 비밀값은 bootstrap이 최초 1회 무작위 생성)
- 배포 트리거: `main` 브랜치의 `Backend CI` 성공 또는 수동 실행
- 안전장치: GitHub 저장소 변수 `DEPLOY_ENABLED=true`일 때만 배포
- 검증: `GET /actuator/health`가 `UP`인지 확인
- 실패 처리: 헬스체크 실패 시 직전 심볼릭 링크로 롤백

사용자가 늘어 메모리가 부족해지면 DB만 RDS로 옮긴다. 애플리케이션은 `DB_URL` 등 환경변수만 바꾸면 된다.

## 1. EC2 준비

Ubuntu 24.04 t3.micro를 만들고 Elastic IP를 연결한다. 루트 EBS는 프리티어 한도(30GB) 안에서 20GB 이상을 권장한다. 보안 그룹은 다음만 허용한다.

- SSH 22: GitHub-hosted runner의 IP가 고정되지 않아 현재 `0.0.0.0/0`으로 열어 두었다. 키 인증 전용(`PasswordAuthentication no`)과 fail2ban으로 보완한다. 관리자 IP `/32`로만 열면 CD의 `scp`가 약 2분 뒤 exit 255로 실패한다. OIDC + SSM 배포로 바꾸면 22번을 닫을 수 있다
- HTTP 80 / HTTPS 443: 사용자 트래픽
- 8080, 5432: 열지 않음 (둘 다 인스턴스 내부에서만 사용)

저장소의 `deploy/ec2` 디렉터리를 EC2로 복사하고 최초 한 번 실행한다. 두 번째 인자는 API 도메인이다.

```bash
sudo ./deploy/ec2/bootstrap.sh ubuntu api.pickeat.kr
sudoedit /etc/pickeat/pickeat.env   # GOOGLE_PLACES_API_KEY, MAIL_PASSWORD 입력
```

회원가입 인증 메일은 기본 발신 계정 `cki08543@gmail.com`의 SMTP 앱 비밀번호를 `MAIL_PASSWORD`에 넣는다. Gmail 계정의 2단계 인증을 켠 뒤 발급한 앱 비밀번호를 쓴다.

bootstrap은 swap, Java·Docker·Nginx·certbot 설치, PostGIS 컨테이너 실행, Nginx 설정, 백업 cron, systemd 서비스 등록을 한 번에 처리한다. 여러 번 실행해도 기존 환경변수 파일과 DB 볼륨은 유지된다. 단, Nginx 설정 파일은 템플릿으로 덮어쓰므로 **HTTPS 발급 뒤에 다시 실행하면 certbot이 넣은 SSL 설정이 사라진다.** 그때는 `sudo certbot --nginx -d api.pickeat.kr`를 다시 실행한다. 환경변수 파일은 Git에 커밋하지 않는다.

### 도메인과 HTTPS

1. DNS에 A 레코드 `api.pickeat.kr → Elastic IP`를 추가한다.
2. 전파를 확인한 뒤 인증서를 발급한다. 갱신은 certbot 타이머가 자동으로 처리한다.

```bash
sudo certbot --nginx -d api.pickeat.kr --redirect
sudo certbot renew --dry-run
```

Nginx 설정 파일은 `/etc/nginx/sites-available/pickeat`이다. IP로 bootstrap했다면 발급 전에 `server_name`을 도메인으로 바꾼다.

확인할 때 `curl -I`(HEAD)는 쓰지 않는다. 인증 없이 허용된 메서드가 GET뿐이라 401이 나온다.

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://api.pickeat.kr/api/v1/discovery-spots   # 200
curl -s -o /dev/null -D - -H "Origin: https://www.pickeat.kr" https://api.pickeat.kr/api/v1/discovery-spots | grep -i access-control
```

### 백업

로컬 `pg_dump`는 실수로 데이터를 지운 경우를 대비한 것이다. 인스턴스 자체를 잃는 경우를 대비해 AWS Data Lifecycle Manager로 루트 EBS의 일일 스냅샷도 설정한다.

```bash
sudo /usr/local/bin/pickeat-backup          # 수동 백업
docker exec -i pickeat-postgres pg_restore -U pick_eat -d pick_eat --clean < /var/backups/pickeat/pick_eat-YYYY-MM-DD.dump
```

### 후기·프로필 이미지 S3

후기 이미지(`reviews/{memberId}/`)와 프로필 이미지(`profiles/{memberId}/`)는 같은 버킷을 쓰며, 클라이언트가 presigned URL로 S3에 직접 올린다. 설정하지 않아도 애플리케이션은 정상 동작하고, 업로드 URL 발급만 `REVIEW_006`(503)으로 거부된다.

1. S3 버킷을 만든다(서울 리전). 이미지를 그대로 제공하려면 `reviews/*`·`profiles/*`에 공개 읽기를 허용하거나 CloudFront를 앞에 둔다.
2. 버킷 CORS에 프론트 origin의 `PUT`을 허용한다.

```json
[{"AllowedOrigins": ["https://www.pickeat.kr", "https://pickeat.kr"], "AllowedMethods": ["PUT"], "AllowedHeaders": ["Content-Type"], "MaxAgeSeconds": 3600}]
```

3. EC2 인스턴스 역할에 `s3:PutObject`를 `arn:aws:s3:::<버킷>/reviews/*`와 `arn:aws:s3:::<버킷>/profiles/*`에만 허용한다. 액세스 키는 쓰지 않는다. (2026-10-08 프로필 이미지 추가 — 기존 정책이 `reviews/*`만 허용하면 `profiles/*`를 Resource에 추가해야 프로필 업로드 PUT이 403이 나지 않는다.)
4. `/etc/pickeat/pickeat.env`에 값을 넣고 서비스를 재시작한다.

```bash
REVIEW_IMAGE_BUCKET=<버킷 이름>
REVIEW_IMAGE_REGION=ap-northeast-2
REVIEW_IMAGE_BASE_URL=            # CloudFront를 쓰면 https://xxxx.cloudfront.net
sudo systemctl restart pickeat-backend.service
```

presigned PUT은 파일 크기를 제한하지 못한다. 필요해지면 S3 버킷 정책이나 presigned POST로 바꾼다.

## 2. GitHub Environment와 Secret

GitHub 저장소에 `production` Environment를 만든다. 필요한 경우 승인자를 설정한다.

Secrets:

- `EC2_HOST`: EC2 공개 DNS 또는 고정 도메인
- `EC2_USER`: 예: `ubuntu`
- `EC2_SSH_PRIVATE_KEY`: 배포 전용 SSH 개인 키
- `EC2_KNOWN_HOSTS`: EC2 SSH 호스트 키 한 줄

Repository variable:

- `DEPLOY_ENABLED`: 준비가 끝난 뒤에만 `true`

호스트 키는 신뢰 가능한 경로로 확인한 뒤 등록한다. 편의를 위해 SSH 검증을 끄지 않는다.

## 3. 첫 배포

1. `/etc/pickeat/pickeat.env` 설정을 확인한다.
2. GitHub의 `Backend CD` 워크플로를 수동 실행한다.
3. EC2에서 상태를 확인한다.

```bash
sudo systemctl status pickeat-backend.service
journalctl -u pickeat-backend.service -n 200 --no-pager
curl --fail http://127.0.0.1:8080/actuator/health
```

이후 `main` CI가 성공하면 CD가 자동 실행된다.

## 4. 상태 확인

```bash
docker ps --filter name=pickeat-postgres
free -h                                    # swap 사용량 확인
curl --fail https://api.pickeat.kr/api/v1/discovery-spots
```

## 롤백

자동 헬스체크 실패 시 배포 스크립트가 직전 릴리스로 되돌린다. 이 자동 롤백은 아직 실제 실패 배포로 검증하지 않았다. 수동 롤백은 `/opt/pickeat/releases`의 정상 릴리스를 `/opt/pickeat/current`가 가리키도록 바꾼 뒤 서비스를 재시작한다.
