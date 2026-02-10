#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS="$ROOT/deskpilot.properties"

echo "[deskpilot] ROOT=$ROOT"
echo "[deskpilot] PROPS=$PROPS"

if [[ ! -f "$PROPS" ]]; then
  echo "[deskpilot][ERROR] deskpilot.properties not found: $PROPS" >&2
  exit 2
fi

DP_VER="$(
  grep -E '^[[:space:]]*deskpilot\.version[[:space:]]*=' "$PROPS" \
    | head -n1 \
    | sed -E 's/^[[:space:]]*deskpilot\.version[[:space:]]*=[[:space:]]*//' \
    | tr -d '\r' \
    | xargs
)"

if [[ -z "$DP_VER" ]]; then
  echo "[deskpilot][ERROR] deskpilot.version missing in deskpilot.properties" >&2
  exit 2
fi

CACHE="$ROOT/.deskpilot/cli/$DP_VER"
JAR="$CACHE/deskpilot.jar"
TAG="v$DP_VER"
URL="https://github.com/Sankr20/deskpilot/releases/download/$TAG/deskpilot.jar"

echo "[deskpilot] DP_VER=$DP_VER"
echo "[deskpilot] JAR=$JAR"
echo "[deskpilot] URL=$URL"

mkdir -p "$CACHE"

if [[ ! -f "$JAR" ]]; then
  echo "[deskpilot] CLI jar missing. Downloading..." >&2

  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$JAR" || true
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$JAR" || true
  fi

  if [[ ! -f "$JAR" ]]; then
    echo "[deskpilot][ERROR] Download failed: $URL" >&2
    exit 2
  fi
fi

exec java -jar "$JAR" "$@"
