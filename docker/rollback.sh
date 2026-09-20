#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
source ./lib-deploy.sh
lock_deployment

LIVE=$(live_color)
PREV=$(other_color "$LIVE")

if [ "$(container_state "$PREV")" = "missing" ]; then
    echo "no app_$PREV container to roll back to"
    exit 1
fi

echo "rolling back from $LIVE to $PREV"
$COMPOSE start "app_$PREV"

if ! wait_ready "$PREV"; then
    $COMPOSE logs --tail 60 "app_$PREV" || true
    $COMPOSE stop "app_$PREV"
    exit 1
fi

if ! switch_to "$PREV"; then
    echo "nginx rejected the upstream, traffic stays on $LIVE"
    $COMPOSE stop "app_$PREV"
    exit 1
fi

$COMPOSE stop "app_$LIVE"

echo "live: $PREV"
