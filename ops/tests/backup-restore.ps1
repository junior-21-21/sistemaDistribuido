[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:BACKUP_ENCRYPTION_KEY)) {
    throw 'Define BACKUP_ENCRYPTION_KEY en el entorno antes de probar la restauración.'
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $projectRoot
try {
    docker compose --profile operations build backup-postgres
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo construir la imagen operativa de backup.' }

    docker compose --profile operations run --rm -e BACKUP_RUN_ONCE=true backup-postgres
    if ($LASTEXITCODE -ne 0) { throw 'Falló el ciclo aislado de backup/restauración.' }

    docker compose --profile operations run --rm --entrypoint sh backup-postgres `
        -c 'test -s /backups/restore-verifications.jsonl && test -s /backups/latest-success.txt'
    if ($LASTEXITCODE -ne 0) { throw 'No se generó evidencia persistente de restauración.' }
    Write-Host '[OK] Las seis bases se respaldaron, descifraron y restauraron en una base temporal.' -ForegroundColor Green
}
finally {
    Pop-Location
}
