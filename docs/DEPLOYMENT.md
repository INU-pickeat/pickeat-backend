# AWS EC2 배포

Pick Eat 백엔드는 EC2에서 실행 JAR를 systemd로 관리하고, GitHub Actions가 CI 성공 후 새 릴리스를 배포한다.

## 확정 구조

- 실행 환경: AWS EC2 + Java 21 + systemd
- 애플리케이션 경로: `/opt/pickeat`
- 환경변수 파일: `/etc/pickeat/pickeat.env`
- 배포 트리거: `main` 브랜치의 `Backend CI` 성공 또는 수동 실행
- 안전장치: GitHub 저장소 변수 `DEPLOY_ENABLED=true`일 때만 배포
- 검증: `GET /actuator/health`가 `UP`인지 확인
- 실패 처리: 헬스체크 실패 시 직전 심볼릭 링크로 롤백

운영 DB는 PostgreSQL + PostGIS가 필요하다. RDS 또는 별도 PostgreSQL 호스트 중 하나를 인프라 생성 시 선택하고, 애플리케이션에는 JDBC URL만 주입한다.

## 1. EC2 준비

Ubuntu EC2를 만든 뒤 보안 그룹은 다음만 허용한다.

- SSH 22: 관리자 고정 IP 또는 GitHub Actions가 접근 가능한 별도 경로
- HTTP 80 / HTTPS 443: 사용자 트래픽
- 애플리케이션 8080: 외부에 직접 공개하지 않음
- PostgreSQL 5432: 애플리케이션 보안 그룹에서 DB로 나가는 연결만 허용

저장소의 `deploy/ec2` 디렉터리를 EC2로 복사하고 최초 한 번 실행한다.

```bash
sudo ./deploy/ec2/bootstrap.sh ubuntu
sudoedit /etc/pickeat/pickeat.env
```

환경변수 파일의 `CHANGE_ME` 값을 모두 실제 운영 값으로 바꾼다. 파일은 Git에 커밋하지 않는다.

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

## 4. HTTPS와 프록시

운영 공개 전 Nginx 또는 ALB에서 HTTPS를 종료하고 `/` 요청을 `127.0.0.1:8080`으로 전달한다. 8080 포트는 보안 그룹에서 공개하지 않는다.

## 롤백

자동 헬스체크 실패 시 배포 스크립트가 직전 릴리스로 되돌린다. 수동 롤백은 `/opt/pickeat/releases`의 정상 릴리스를 `/opt/pickeat/current`가 가리키도록 바꾼 뒤 서비스를 재시작한다.
