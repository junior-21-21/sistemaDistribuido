[CmdletBinding()]
param(
    [string]$ApiUrl = 'http://localhost:8080',
    [string]$Service = 'servicio-cursos',
    [string]$ProbePath = '/api/aulas',
    [string]$Username = 'mantenimiento1',
    [string]$Password,
    [switch]$UseDemoCredentials,
    [ValidateRange(30, 600)][int]$RecoveryTimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($UseDemoCredentials -and [string]::IsNullOrWhiteSpace($Password)) {
    $Password = $(if ($env:PAD_CLAVE_MANTENIMIENTO1) { $env:PAD_CLAVE_MANTENIMIENTO1 } else { 'DemoMantenimiento2026*' })
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'Indica -Password o usa -UseDemoCredentials en el entorno local.'
}

$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$session = Invoke-RestMethod -Method Post -Uri "$ApiUrl/api/auth/login" `
    -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($session.token)" }

function Get-Status([string]$Url) {
    try {
        return [int](Invoke-WebRequest -Uri $Url -Headers $headers -UseBasicParsing -TimeoutSec 5).StatusCode
    }
    catch {
        if ($null -ne $_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $projectRoot
try {
    if ((Get-Status "$ApiUrl$ProbePath") -ne 200) { throw 'La sonda inicial no está disponible.' }
    docker compose stop $Service
    if ($LASTEXITCODE -ne 0) { throw "No se pudo detener $Service." }

    $failureStatus = Get-Status "$ApiUrl$ProbePath"
    if ($failureStatus -notin @(500, 502, 503, 504)) {
        throw "El gateway no degradó de forma controlada; respondió HTTP $failureStatus."
    }
    Write-Host "[OK] Caída controlada observada mediante HTTP $failureStatus." -ForegroundColor Green
}
finally {
    docker compose start $Service
    Pop-Location
}

$deadline = (Get-Date).AddSeconds($RecoveryTimeoutSeconds)
do {
    Start-Sleep -Seconds 2
    $status = Get-Status "$ApiUrl$ProbePath"
} while ($status -ne 200 -and (Get-Date) -lt $deadline)
if ($status -ne 200) { throw "$Service no se recuperó dentro del plazo." }
Write-Host '[OK] El servicio volvió a responder a través del gateway.' -ForegroundColor Green
