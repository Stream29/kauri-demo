#!/usr/bin/env bash

acquire_build_lock() {
    local owner
    KAURI_BUILD_LOCK_PATH="$(pwd)/.gradle/kauri/gradle-mac.lock"
    KAURI_OWNS_BUILD_LOCK=0
    mkdir -p "$(dirname "$KAURI_BUILD_LOCK_PATH")"
    if mkdir "$KAURI_BUILD_LOCK_PATH" 2>/dev/null; then
        if ! printf '%s\n' "$$" >"$KAURI_BUILD_LOCK_PATH/owner.pid"; then
            rm -f "$KAURI_BUILD_LOCK_PATH/owner.pid"
            rmdir "$KAURI_BUILD_LOCK_PATH" 2>/dev/null || true
            return 1
        fi
        KAURI_OWNS_BUILD_LOCK=1
        export KAURI_BUILD_LOCK_PID="$$"
        return 0
    fi
    owner=$(cat "$KAURI_BUILD_LOCK_PATH/owner.pid" 2>/dev/null || true)
    if [[ -n "$owner" && "$owner" == "$PPID" && "$owner" == "${KAURI_BUILD_LOCK_PID:-}" ]] &&
        kill -0 "$owner" 2>/dev/null; then
        return 0
    fi
    echo "Repository build lock is owned by PID ${owner:-unknown}; refusing concurrent work." >&2
    if [[ ! "$owner" =~ ^[0-9]+$ ]] || ! kill -0 "$owner" 2>/dev/null; then
        echo "Owner is not provably active; verify ownership before manually removing this stale lock." >&2
    fi
    return 1
}

release_build_lock() {
    if [[ "${KAURI_OWNS_BUILD_LOCK:-0}" == 1 &&
        "$(cat "$KAURI_BUILD_LOCK_PATH/owner.pid" 2>/dev/null || true)" == "$$" ]]; then
        rm -rf "$KAURI_BUILD_LOCK_PATH"
    fi
}
