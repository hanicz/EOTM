#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
source ./lib-deploy.sh
lock_deployment

LIVE=$(live_color)

docker compose --profile "$LIVE" up -d "app_$LIVE"
docker compose --profile "$LIVE" up -d

echo "live: $LIVE"
