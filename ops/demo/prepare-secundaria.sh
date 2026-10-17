#!/bin/sh
set -eu

students="${1:-20}"
mode="${2:-ready}"

case "$students" in
  ''|*[!0-9]*) echo "La cantidad debe ser un entero entre 1 y 35" >&2; exit 2 ;;
esac
if [ "$students" -lt 1 ] || [ "$students" -gt 35 ]; then
  echo "La cantidad debe estar entre 1 y 35" >&2
  exit 2
fi
case "$mode" in
  ready) load_schedules=true ;;
  generator) load_schedules=false ;;
  *) echo "Modo inválido. Usa ready o generator" >&2; exit 2 ;;
esac

DEMO_STUDENTS_PER_SECTION="$students" \
DEMO_LOAD_SCHEDULES="$load_schedules" \
DEMO_RESET_SCHEDULES=true \
docker compose --profile demo run --rm demo-data

docker compose --profile demo run --rm --no-deps --entrypoint psql demo-data \
  -h postgres -U postgres \
  -v "estudiantes_por_seccion=$students" \
  -v "esperar_horarios=$load_schedules" \
  -f /demo/verify-secundaria-2026.sql
