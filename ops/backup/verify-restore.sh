#!/bin/sh
set -eu

encrypted="${1:?Indica el respaldo cifrado}"
source_database="${2:?Indica la base de origen}"
verify_database="pad_restore_verify"
plain="/tmp/${source_database}_verify.dump"
evidence="/backups/restore-verifications.jsonl"
started_at="$(date +%s)"

cleanup() {
    rm -f "$plain"
    dropdb --if-exists --force "$verify_database" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

sha256sum -c "${encrypted}.sha256"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
    -in "$encrypted" -out "$plain" -pass env:BACKUP_ENCRYPTION_KEY
dropdb --if-exists --force "$verify_database" >/dev/null 2>&1 || true
createdb "$verify_database"
pg_restore --exit-on-error --no-owner --no-acl --dbname="$verify_database" "$plain"
source_tables="$(psql --dbname="$source_database" --tuples-only --no-align \
    --command "SELECT count(*) FROM pg_tables WHERE schemaname='public'")"
restored_tables="$(psql --dbname="$verify_database" --tuples-only --no-align \
    --command "SELECT count(*) FROM pg_tables WHERE schemaname='public'")"
if [ "$source_tables" != "$restored_tables" ]; then
    echo "La restauración no conserva todas las tablas: origen=$source_tables restauradas=$restored_tables" >&2
    exit 1
fi
checksum="$(sha256sum "$encrypted" | cut -d ' ' -f 1)"
finished_at="$(date +%s)"
verified_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
printf '{"verifiedAt":"%s","database":"%s","backup":"%s","sha256":"%s","tables":%s,"durationSeconds":%s,"status":"OK"}\n' \
    "$verified_at" "$source_database" "$(basename "$encrypted")" "$checksum" \
    "$restored_tables" "$((finished_at - started_at))" >> "$evidence"
echo "Restauración verificada: $source_database"
