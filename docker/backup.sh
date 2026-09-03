#!/bin/bash

OUT="/tmp/eotm-$(date +%Y%m%d-%H%M%S).sql"

docker compose exec -T db sh -c 'pg_dump --clean --if-exists --no-owner --no-privileges -U "$POSTGRES_USER" "$POSTGRES_DB"' > "$OUT.part"

if [ $? -eq 0 ]; then
    mv "$OUT.part" "$OUT"
    echo "$OUT"
else
    rm -f "$OUT.part"
    exit 1
fi
