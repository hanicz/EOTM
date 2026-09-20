#!/bin/bash

STATE_FILE=.live-color
UPSTREAM_FILE=nginx-conf.d/upstream.conf
LOCK_FILE=.deploy.lock
COMPOSE="docker compose --profile blue --profile green"
READY_TIMEOUT=120

lock_deployment() {
    exec 9>"$LOCK_FILE"
    if ! flock -n 9; then
        echo "another deployment is already running"
        exit 1
    fi
}

live_color() {
    cat "$STATE_FILE"
}

other_color() {
    if [ "$1" = "blue" ]; then echo "green"; else echo "blue"; fi
}

container_state() {
    docker inspect -f '{{.State.Status}}' "app_$1" 2>/dev/null || echo "missing"
}

health_state() {
    docker inspect -f '{{.State.Health.Status}}' "app_$1" 2>/dev/null || echo "unknown"
}

wait_ready() {
    local color=$1
    local deadline=$((SECONDS + READY_TIMEOUT))
    while [ $SECONDS -lt $deadline ]; do
        if [ "$(health_state "$color")" = "healthy" ]; then
            return 0
        fi
        if [ "$(container_state "$color")" = "exited" ]; then
            echo "app_$color exited during startup"
            return 1
        fi
        sleep 2
    done
    echo "app_$color did not become healthy within ${READY_TIMEOUT}s"
    return 1
}

smoke_test() {
    docker exec "app_$1" curl -fsS --max-time 5 -o /dev/null http://127.0.0.1:8080/
}

switch_to() {
    local color=$1
    cp "$UPSTREAM_FILE" "$UPSTREAM_FILE.bak"
    printf 'upstream eotm {\n  server app_%s:8080;\n  keepalive 16;\n}\n' "$color" > "$UPSTREAM_FILE"
    if ! docker exec nginx nginx -t; then
        mv "$UPSTREAM_FILE.bak" "$UPSTREAM_FILE"
        return 1
    fi
    docker exec nginx nginx -s reload
    rm -f "$UPSTREAM_FILE.bak"
    echo "$color" > "$STATE_FILE"
}
