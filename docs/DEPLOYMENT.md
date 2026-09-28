# AWS EC2 배포

Pick Eat 백엔드는 EC2에서 실행 JAR를 systemd로 관리하고, GitHub Actions가 CI 성공 후 새 릴리스를 배포한다.

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

- SSH 22: 관리자 고정 IP 또는 GitHub Actions가 접근 가능한 별도 경로
- HTTP 80 / HTTPS 443: 사용자 트래픽
- 8080, 5432: 열지 않음 (둘 다 인스턴스 내부에서만 사용)

저장소의 `deploy/ec2` 디렉터리를 EC2로 복사하고 최초 한 번 실행한다. 두 번째 인자는 API 도메인이다.

```bash
sudo ./deploy/ec2/bootstrap.sh ubuntu api.pickeat.kr
sudoedit /etc/pickeat/pickeat.env   # GOOGLE_PLACES_API_KEY 입력
```

bootstrap은 swap, Java·Docker·Nginx·certbot 설치, PostGIS 컨테이너 실행, Nginx 설정, 백업 cron, systemd 서비스 등록을 한 번에 처리한다. 여러 번 실행해도 기존 환경변수 파일과 DB 볼륨은 유지된다. 환경변수 파일은 Git에 커밋하지 않는다.

### 도메인과 HTTPS

1. DNS에 A 레코드 `api.pickeat.kr → Elastic IP`를 추가한다.
2. 전파를 확인한 뒤 인증서를 발급한다. 갱신은 certbot 타이머가 자동으로 처리한다.

```bash
sudo certbot --nginx -d api.pickeat.kr
```

### 백업

로컬 `pg_dump`는 실수로 데이터를 지운 경우를 대비한 것이다. 인스턴스 자체를 잃는 경우를 대비해 AWS Data Lifecycle Manager로 루트 EBS의 일일 스냅샷도 설정한다.

```bash
sudo /usr/local/bin/pickeat-backup          # 수동 백업
docker exec -i pickeat-postgres pg_restore -U pick_eat -d pick_eat --clean < /var/backups/pickeat/pick_eat-YYYY-MM-DD.dump
```

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

자동 헬스체크 실패 시 배포 스크립트가 직전 릴리스로 되돌린다. 수동 롤백은 `/opt/pickeat/releases`의 정상 릴리스를 `/opt/pickeat/current`가 가리키도록 바꾼 뒤 서비스를 재시작한다.
