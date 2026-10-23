#!/usr/bin/env bash
set -euo pipefail
exec bash "$(dirname "$0")/release-all-mac.sh" macosArm64
