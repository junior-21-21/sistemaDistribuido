#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE academico_estudiantes;
    CREATE DATABASE academico_cursos;
    CREATE DATABASE academico_evaluaciones;
    CREATE DATABASE academico_disciplina;
    CREATE DATABASE academico_reportes;
EOSQL
