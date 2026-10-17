#!/bin/sh
set -eu

admin_database="${POSTGRES_DB:-$POSTGRES_USER}"

for database in \
    academico_estudiantes \
    academico_cursos \
    academico_evaluaciones \
    academico_disciplina \
    academico_reportes \
    academico_notificaciones \
    academico_horarios
do
    exists="$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$admin_database" --tuples-only --no-align --command "SELECT 1 FROM pg_database WHERE datname = '$database'")"
    if [ "$exists" != "1" ]; then
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$admin_database" --command "CREATE DATABASE \"$database\""
    fi
done
