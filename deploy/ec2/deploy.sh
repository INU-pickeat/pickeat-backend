#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: ./deploy.sh <jar-path> <release-id>" >&2
  exit 1
fi

ARTIFACT_PATH="$1"
RELEASE_ID="$2"
APP_ROOT="/opt/pickeat"
RELEASE_DIR="$APP_ROOT/releases/$RELEASE_ID"
CURRENT_LINK="$APP_ROOT/current"
HEALTHCHECK_URL="http://127.0.0.1:8080/actuator/health"

if [[ ! -f "$ARTIFACT_PATH" ]]; then
  echo "Artifact not found: $ARTIFACT_PATH" >&2
  exit 1
fi

if [[ ! "$RELEASE_ID" =~ ^[0-9a-fA-F]{7,64}$ ]]; then
  echo "Release ID must be a Git commit SHA." >&2
  exit 1
fi

mkdir -p "$RELEASE_DIR"
install -m 0644 "$ARTIFACT_PATH" "$RELEASE_DIR/app.jar"

PREVIOUS_RELEASE=""
if [[ -L "$CURRENT_LINK" ]]; then
  PREVIOUS_RELEASE="$(readlink "$CURRENT_LINK")"
fi

ln -sfn "$RELEASE_DIR" "$APP_ROOT/current.next"
mv -Tf "$APP_ROOT/current.next" "$CURRENT_LINK"
sudo systemctl restart pickeat-backend.service

for attempt in {1..30}; do
  if curl --fail --silent --show-error "$HEALTHCHECK_URL" | grep -q '"status":"UP"'; then
    rm -f "$ARTIFACT_PATH" "$0"
    echo "Release $RELEASE_ID is healthy."
    exit 0
  fi
  sleep 2
done

echo "Release $RELEASE_ID failed its health check." >&2
if [[ -n "$PREVIOUS_RELEASE" && -d "$PREVIOUS_RELEASE" ]]; then
  ln -sfn "$PREVIOUS_RELEASE" "$APP_ROOT/current.next"
  mv -Tf "$APP_ROOT/current.next" "$CURRENT_LINK"
  sudo systemctl restart pickeat-backend.service
  echo "Rolled back to $PREVIOUS_RELEASE." >&2
fi
exit 1
