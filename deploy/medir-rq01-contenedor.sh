#!/usr/bin/env bash
# RQ-01 dentro de un contenedor con los limites del manifiesto (1 CPU, 512 MB).
# Uso: deploy/medir-rq01-contenedor.sh [ejecuciones]   (por defecto 3)
# 1) compila libmemoria.so en ubuntu:24.04 (misma base que la imagen final)
# 2) corre el banco BancoRq01 en un contenedor con --cpus=1.0 --memory=512m
# Los datos crudos quedan en docs/mediciones/rq01-contenedor.txt
set -euo pipefail
cd "$(dirname "$0")/.."
EJEC=${1:-3}; CPUS=1.0; MEM=512m
SALIDA=docs/mediciones/rq01-contenedor.txt
mkdir -p docs/mediciones
{
  echo "RQ-01 en contenedor | cpus=$CPUS memoria=$MEM | ejecuciones=$EJEC | $(date -u +%F\ %T) UTC"
  echo "Maquina: $(nproc) CPU, $(grep -m1 'model name' /proc/cpuinfo | cut -d: -f2 | xargs)"
  docker --version
} | tee "$SALIDA"

docker run --rm -e UID_H="$(id -u)" -e GID_H="$(id -g)" -v "$PWD/c":/src/c -w /src/c ubuntu:24.04 bash -c \
  "apt-get update -qq && apt-get install -y -qq --no-install-recommends gcc make libc6-dev >/dev/null \
   && make clean >/dev/null && make libmemoria.so && chown \$UID_H:\$GID_H libmemoria.so"

for i in $(seq 1 "$EJEC"); do
  echo "=== ejecucion $i (cpus=$CPUS memoria=$MEM) ===" | tee -a "$SALIDA"
  docker run --rm --cpus="$CPUS" --memory="$MEM" \
    -v "$PWD":/repo:ro -v maven-cache:/root/.m2 maven:3.9-eclipse-temurin-21 \
    bash -c 'cp -r /repo /work && cd /work/asignador-service && mvn -B -ntp test -Dtest=BancoRq01' \
    2>&1 | tee -a "$SALIDA" | grep -v -E "^\[INFO\] (Download|---|Scanning|BUILD|Total|Finished|Reactor)"
done
echo "Datos crudos guardados en $SALIDA"
