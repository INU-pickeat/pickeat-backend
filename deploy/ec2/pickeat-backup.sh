#!/usr/bin/env bash
# 매일 pg_dump로 로컬 백업을 만들고 7일치만 보관한다. 인스턴스 유실 대비는 EBS 스냅샷으로 한다.
set -Eeuo pipefail

BACKUP_DIR=/var/backups/pickeat
install -d -m 0700 "$BACKUP_DIR"
docker exec pickeat-postgres pg_dump -U pick_eat -Fc pick_eat > "$BACKUP_DIR/pick_eat-$(date +%F).dump"
find "$BACKUP_DIR" -name 'pick_eat-*.dump' -mtime +7 -delete
