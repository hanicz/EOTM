#!/bin/bash
set -uo pipefail

cd "$(dirname "$0")"
umask 077
export PATH="/snap/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

ENV_FILE=./backup.env
if [ ! -f "$ENV_FILE" ]; then
    echo "missing $ENV_FILE"
    exit 1
fi
. "$ENV_FILE"

BACKUP_DIR=./backups
RETENTION_DAYS=14
RECIPIENTS=./backup-recipients.txt
REMOTE="${BACKUP_REMOTE:?BACKUP_REMOTE not set in $ENV_FILE}"
OUT="$BACKUP_DIR/eotm-$(date +%Y%m%d-%H%M%S).sql.gz"

mkdir -p "$BACKUP_DIR"

if docker compose exec -T db sh -c 'pg_dump --clean --if-exists --no-owner --no-privileges -U "$(cat /run/secrets/db_user)" "$POSTGRES_DB"' | gzip > "$OUT.part"; then
    mv "$OUT.part" "$OUT"
    find "$BACKUP_DIR" -name 'eotm-*.sql.gz' -mtime +$((RETENTION_DAYS - 1)) -delete
    echo "$OUT"
    if ! age -R "$RECIPIENTS" "$OUT" | rclone rcat "$REMOTE/$(basename "$OUT").age"; then
        echo "upload failed: $OUT"
        exit 1
    fi
    echo "uploaded: $REMOTE/$(basename "$OUT").age"
else
    rm -f "$OUT.part"
    exit 1
fi
