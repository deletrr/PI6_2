#!/usr/bin/env bash
set -euo pipefail

DEVICE_ID="${1:-ESP32-VAG-001}"
PORT="${2:-/dev/ttyUSB0}"
CERTS_DIR="$(dirname "$0")/../../infra/certs"
FIRMWARE_DIR="$(dirname "$0")/parquimetro"

if [ ! -f "$CERTS_DIR/ca.crt" ]; then
  echo "ERRO: Execute infra/certs/gen_certs.sh primeiro"
  exit 1
fi

embed_cert() {
  local file="$1"
  local var="$2"
  echo "const char* $var = R\"EOF("
  cat "$file"
  echo ")EOF\";"
}

cat > "$FIRMWARE_DIR/certs_embedded.h" << EOF
#pragma once
$(embed_cert "$CERTS_DIR/ca.crt" "CA_CERT")
$(embed_cert "$CERTS_DIR/$DEVICE_ID.crt" "CLIENT_CERT")
$(embed_cert "$CERTS_DIR/$DEVICE_ID.key" "CLIENT_KEY")
EOF

echo "Certificados embedados em certs_embedded.h"
echo "Compilando e flasheando $DEVICE_ID em $PORT..."

cd "$(dirname "$0")"
pio run --environment esp32dev --target upload --upload-port "$PORT"

echo "Flash concluido para $DEVICE_ID"
