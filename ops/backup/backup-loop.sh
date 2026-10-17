#!/bin/sh
set -eu

databases="academico_estudiantes academico_cursos academico_evaluaciones academico_disciplina academico_reportes academico_notificaciones"
interval_hours="${BACKUP_INTERVAL_HOURS:-24}"
retention_days="${BACKUP_RETENTION_DAYS:-30}"

case "$interval_hours" in *[!0-9]*|'') echo "BACKUP_INTERVAL_HOURS debe ser entero" >&2; exit 1;; esac
case "$retention_days" in *[!0-9]*|'') echo "BACKUP_RETENTION_DAYS debe ser entero" >&2; exit 1;; esac
if [ "$interval_hours" -lt 1 ] || [ "$retention_days" -lt 7 ] || [ "$retention_days" -gt 365 ]; then
    echo "Intervalo mínimo 1 hora; retención permitida entre 7 y 365 días" >&2
    exit 1
fi

if [ -z "${BACKUP_ENCRYPTION_KEY:-}" ]; then
    echo "BACKUP_ENCRYPTION_KEY es obligatoria para ejecutar respaldos cifrados" >&2
    exit 1
fi

mkdir -p /backups

while true; do
    timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
    for database in $databases; do
        plain="/tmp/${database}_${timestamp}.dump"
        encrypted="/backups/${database}_${timestamp}.dump.enc"
        pg_dump --format=custom --no-owner --no-acl --dbname="$database" --file="$plain"
        openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
            -in "$plain" -out="$encrypted" -pass env:BACKUP_ENCRYPTION_KEY
        sha256sum "$encrypted" > "${encrypted}.sha256"
        rm -f "$plain"
        /usr/local/bin/verify-restore.sh "$encrypted" "$database"
    done
    printf '%s todas-las-bases OK\n' "$timestamp" > /backups/latest-success.txt
    find /backups -type f -mtime "+$retention_days" -delete
    if [ "${BACKUP_RUN_ONCE:-false}" = "true" ]; then
        break
    fi
    sleep "$((interval_hours * 3600))"
done
