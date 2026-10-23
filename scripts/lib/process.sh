#!/usr/bin/env bash

terminate_child() {
    local child="${1:-}" attempt
    [[ -n "$child" ]] || return 0
    if kill -0 "$child" 2>/dev/null; then
        kill -TERM "$child" 2>/dev/null || true
        for ((attempt=0; attempt<50; attempt++)); do
            kill -0 "$child" 2>/dev/null || break
            sleep 0.1
        done
        if kill -0 "$child" 2>/dev/null; then kill -KILL "$child" 2>/dev/null || true; fi
    fi
    wait "$child" 2>/dev/null || true
}
