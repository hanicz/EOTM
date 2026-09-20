#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
source ./lib-deploy.sh
lock_deployment

TAG="${1:-latest}"
LIVE=$(live_color)
IDLE=$(other_color "$LIVE")

echo "live: $LIVE, deploying $IDLE from thanicz/eotm:$TAG"

IMAGE_TAG="$TAG" $COMPOSE pull "app_$IDLE"
IMAGE_TAG="$TAG" $COMPOSE up -d --force-recreate "app_$IDLE"

if ! wait_ready "$IDLE"; then
    $COMPOSE logs --tail 60 "app_$IDLE" || true
    $COMPOSE stop "app_$IDLE"
    exit 1
fi

if ! smoke_test "$IDLE"; then
    echo "smoke test failed on app_$IDLE"
    $COMPOSE logs --tail 60 "app_$IDLE" || true
    $COMPOSE stop "app_$IDLE"
    exit 1
fi

if ! switch_to "$IDLE"; then
    echo "nginx rejected the new upstream, traffic stays on $LIVE"
    $COMPOSE stop "app_$IDLE"
    exit 1
fi

printf 'IMAGE_TAG=%s\n' "$TAG" > .env
$COMPOSE stop "app_$LIVE"
docker image prune -f

echo "live: $IDLE"
