#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Aplicando namespace"
kubectl apply -f namespace.yml

echo "==> Aplicando secrets (preencha secrets.yml antes)"
kubectl apply -f secrets.yml

echo "==> Subindo PostgreSQL"
kubectl apply -f postgres.yml
kubectl wait --namespace parquimetro \
  --for=condition=ready pod \
  --selector=app=postgres \
  --timeout=120s

echo "==> Subindo Mosquitto"
kubectl apply -f mosquitto.yml
kubectl wait --namespace parquimetro \
  --for=condition=ready pod \
  --selector=app=mosquitto \
  --timeout=60s

echo "==> Subindo backend"
kubectl apply -f backend.yml
kubectl wait --namespace parquimetro \
  --for=condition=ready pod \
  --selector=app=backend \
  --timeout=120s

echo "==> Aplicando Ingress"
kubectl apply -f ingress.yml

echo ""
echo "Deploy concluido."
kubectl get pods -n parquimetro
kubectl get services -n parquimetro
kubectl get ingress -n parquimetro
