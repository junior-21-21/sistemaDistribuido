param(
    [ValidateRange(1, 35)]
    [int]$EstudiantesPorSeccion = 20,
    [switch]$VaciarHorarios
)

$ErrorActionPreference = 'Stop'
$modoHorarios = if ($VaciarHorarios) { 'false' } else { 'true' }
$anteriores = @{
    DEMO_STUDENTS_PER_SECTION = $env:DEMO_STUDENTS_PER_SECTION
    DEMO_LOAD_SCHEDULES = $env:DEMO_LOAD_SCHEDULES
    DEMO_RESET_SCHEDULES = $env:DEMO_RESET_SCHEDULES
}

try {
    $env:DEMO_STUDENTS_PER_SECTION = [string]$EstudiantesPorSeccion
    $env:DEMO_LOAD_SCHEDULES = $modoHorarios
    $env:DEMO_RESET_SCHEDULES = 'true'

    docker compose --profile demo run --rm demo-data
    if ($LASTEXITCODE -ne 0) { throw 'Falló la carga de datos de demostración.' }

    docker compose --profile demo run --rm --no-deps --entrypoint psql demo-data `
        -h postgres -U postgres `
        -v "estudiantes_por_seccion=$EstudiantesPorSeccion" `
        -v "esperar_horarios=$modoHorarios" `
        -f /demo/verify-secundaria-2026.sql
    if ($LASTEXITCODE -ne 0) { throw 'La verificación de la demostración encontró inconsistencias.' }
}
finally {
    $env:DEMO_STUDENTS_PER_SECTION = $anteriores.DEMO_STUDENTS_PER_SECTION
    $env:DEMO_LOAD_SCHEDULES = $anteriores.DEMO_LOAD_SCHEDULES
    $env:DEMO_RESET_SCHEDULES = $anteriores.DEMO_RESET_SCHEDULES
}
