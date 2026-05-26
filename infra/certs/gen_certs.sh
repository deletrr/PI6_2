#!/usr/bin/env bash
set -euo pipefail

CERTS_DIR="$(dirname "$0")/../certs"
mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

DAYS=3650
COUNTRY="BR"
STATE="SP"
ORG="Parquimetro V7"

echo "==> Gerando CA"
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days $DAYS -key ca.key -out ca.crt \
  -subj "/C=$COUNTRY/ST=$STATE/O=$ORG/CN=ParquimetroCA"

echo "==> Gerando certificado do servidor MQTT"
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr \
  -subj "/C=$COUNTRY/ST=$STATE/O=$ORG/CN=mqtt.parquimetro.local"
openssl x509 -req -days $DAYS -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt \
  -extensions SAN \
  -extfile <(cat /etc/ssl/openssl.cnf <(printf "\n[SAN]\nsubjectAltName=DNS:mqtt.parquimetro.local,DNS:localhost,IP:127.0.0.1"))

echo "==> Gerando certificado do backend"
openssl genrsa -out backend.key 2048
openssl req -new -key backend.key -out backend.csr \
  -subj "/C=$COUNTRY/ST=$STATE/O=$ORG/CN=backend"
openssl x509 -req -days $DAYS -in backend.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out backend.crt

echo "==> Gerando certificado cliente ESP32 (template)"
gen_device_cert() {
  local DEVICE_ID="$1"
  openssl genrsa -out "$DEVICE_ID.key" 2048
  openssl req -new -key "$DEVICE_ID.key" -out "$DEVICE_ID.csr" \
    -subj "/C=$COUNTRY/ST=$STATE/O=$ORG/CN=$DEVICE_ID"
  openssl x509 -req -days $DAYS -in "$DEVICE_ID.csr" -CA ca.crt -CAkey ca.key \
    -CAcreateserial -out "$DEVICE_ID.crt"
  rm "$DEVICE_ID.csr"
  echo "  -> Certificado $DEVICE_ID gerado"
}

gen_device_cert "ESP32-VAG-001"
gen_device_cert "ESP32-VAG-002"

rm -f *.csr *.srl

echo ""
echo "Certificados gerados em: $CERTS_DIR"
echo ""
echo "Para adicionar ao K8s:"
echo "  kubectl create secret generic parquimetro-certs \\"
echo "    --from-file=ca.crt \\"
echo "    --from-file=server.crt \\"
echo "    --from-file=server.key \\"
echo "    --from-file=backend.crt \\"
echo "    --from-file=backend.key \\"
echo "    -n parquimetro"
echo ""
echo "JWT_SECRET (coloque em .env):"
openssl rand -base64 64 | tr -d '\n'
echo ""
