#!/bin/bash
set -uo pipefail

cd "$(dirname "$0")"
umask 077
export PATH="/snap/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

BACKUP_DIR=./backups
RETENTION_DAYS=14
OUT="$BACKUP_DIR/eotm-$(date +%Y%m%d-%H%M%S).sql.gz"

mkdir -p "$BACKUP_DIR"

if docker compose exec -T db sh -c 'pg_dump --clean --if-exists --no-owner --no-privileges -U "$(cat /run/secrets/db_user)" "$POSTGRES_DB"' | gzip > "$OUT.part"; then
    mv "$OUT.part" "$OUT"
    find "$BACKUP_DIR" -name 'eotm-*.sql.gz' -mtime +$((RETENTION_DAYS - 1)) -delete
    echo "$OUT"
else
    rm -f "$OUT.part"
    exit 1
fi
