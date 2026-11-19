#!/usr/bin/env bash
# Medicion de RQ-03 (facilidad de despliegue).
# Uso: deploy/medir-rq03.sh nativo|java [corridas]
# Cada corrida parte de cero: borra contenedores, imagenes y cache de construccion
# (docker system prune -af), construye con --no-cache y mide:
#   - tiempo de construccion
#   - tiempo desde "docker compose up" hasta la primera simulacion correcta
#   - tamano de la imagen final (descomprimido, suma de capas)
# ATENCION: docker system prune -af elimina TODAS las imagenes y contenedores sin uso de esta maquina.
set -euo pipefail

VARIANTE=${1:?uso: $0 nativo|java [corridas]}
CORRIDAS=${2:-3}
cd "$(dirname "$0")/.."

case "$VARIANTE" in
  nativo) COMPOSE=deploy/docker-compose.yml;      IMAGEN=click/asignador-service:nativo; MOTOR=native ;;
  java)   COMPOSE=deploy/docker-compose.java.yml; IMAGEN=click/asignador-service:java;   MOTOR=java ;;
  *) echo "Variante invalida: $VARIANTE (use nativo o java)"; exit 1 ;;
esac

PETICION='{"bloques":[{"inicio":0,"tamano":100},{"inicio":100,"tamano":500},{"inicio":600,"tamano":200},{"inicio":800,"tamano":300}],"operaciones":[{"tipo":"ASIGNAR","proceso":1,"tamano":212},{"tipo":"ASIGNAR","proceso":2,"tamano":417},{"tipo":"ASIGNAR","proceso":3,"tamano":112},{"tipo":"ASIGNAR","proceso":4,"tamano":426}],"estrategias":["PRIMERO_QUE_QUEPA"]}'

dif() { awk -v a="$1" -v b="$2" 'BEGIN{printf "%.1f", a-b}'; }

echo "RQ-03 variante=$VARIANTE corridas=$CORRIDAS"
echo "corrida | construccion_s | arranque_hasta_primera_simulacion_s | total_s | imagen_descomprimida_MB | veredicto_250MB"

for i in $(seq 1 "$CORRIDAS"); do
  docker compose -f "$COMPOSE" down -v >/dev/null 2>&1 || true
  docker system prune -af >/dev/null 2>&1

  t0=$(date +%s.%N)
  docker compose -f "$COMPOSE" build --no-cache >/dev/null 2>&1
  t1=$(date +%s.%N)
  docker compose -f "$COMPOSE" up -d >/dev/null 2>&1

  limite=$((SECONDS + 180))
  respuesta=""
  until respuesta=$(curl -s -f -X POST localhost:8080/simular -H 'Content-Type: application/json' -d "$PETICION" 2>/dev/null) \
        && [[ "$respuesta" == *"\"motor\":\"$MOTOR\""* && "$respuesta" == *'"atendidas":2'* ]]; do
    if [ "$SECONDS" -ge "$limite" ]; then
      echo "ERROR: el servicio no dio una simulacion correcta en 180 s"
      docker compose -f "$COMPOSE" logs --tail 30
      exit 1
    fi
    sleep 0.2
  done
  t2=$(date +%s.%N)

  # Tamano descomprimido = suma de las capas (equivale al SIZE clasico de "docker images")
  mb=$(docker history --human=false --format '{{.Size}}' "$IMAGEN" | awk '{s+=$1} END{printf "%.1f", s/1e6}')
  veredicto=$(awk -v m="$mb" 'BEGIN{print (m < 250) ? "cumple" : "NO cumple"}')
  printf "%d | %s | %s | %s | %s | %s\n" "$i" "$(dif "$t1" "$t0")" "$(dif "$t2" "$t1")" "$(dif "$t2" "$t0")" "$mb" "$veredicto"
done

echo
echo "Salida cruda de docker images (para el informe):"
docker images "$IMAGEN"
echo
echo "Comprobacion: la imagen final no debe contener gcc"
docker run --rm --entrypoint sh "$IMAGEN" -c 'command -v gcc >/dev/null && echo "gcc PRESENTE (mal)" || echo "gcc ausente (bien)"'
docker compose -f "$COMPOSE" down -v >/dev/null 2>&1 || true
