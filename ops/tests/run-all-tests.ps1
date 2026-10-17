[CmdletBinding()]
param(
    [switch]$SkipE2E,
    [switch]$SkipFrontendBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$mavenRepository = Join-Path $env:USERPROFILE '.m2\repository'
$modules = @(
    'servicio-estudiantes',
    'servicio-cursos',
    'servicio-evaluaciones',
    'servicio-disciplina',
    'servicio-notificaciones',
    'servicio-reportes',
    'api-gateway'
)

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no está disponible en PATH.'
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker no está disponible en PATH. Las integraciones usan Testcontainers.'
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop no está iniciado.'
}

$startedAt = Get-Date
foreach ($module in $modules) {
    $pom = Join-Path $projectRoot "backend\$module\pom.xml"
    Write-Host ''
    Write-Host "==> Backend: $module" -ForegroundColor Cyan
    & mvn -f $pom "-Dmaven.repo.local=$mavenRepository" test
    if ($LASTEXITCODE -ne 0) {
        throw "Falló la suite de $module."
    }
}

$frontend = Join-Path $projectRoot 'frontend'
Push-Location $frontend
try {
    Write-Host ''
    Write-Host '==> Frontend: pruebas unitarias' -ForegroundColor Cyan
    & npm run test:unit
    if ($LASTEXITCODE -ne 0) {
        throw 'Fallaron las pruebas unitarias del frontend.'
    }

    if (-not $SkipE2E) {
        Write-Host ''
        Write-Host '==> Frontend: pruebas E2E Chromium' -ForegroundColor Cyan
        & npm run test:e2e
        if ($LASTEXITCODE -ne 0) {
            throw 'Fallaron las pruebas E2E del frontend.'
        }
    }

    if (-not $SkipFrontendBuild) {
        Write-Host ''
        Write-Host '==> Frontend: compilación de producción' -ForegroundColor Cyan
        & npm run build
        if ($LASTEXITCODE -ne 0) {
            throw 'Falló la compilación de producción del frontend.'
        }
    }
}
finally {
    Pop-Location
}

$elapsed = (Get-Date) - $startedAt
Write-Host ''
Write-Host ("Suite completa aprobada en {0:mm\:ss}." -f $elapsed) -ForegroundColor Green
