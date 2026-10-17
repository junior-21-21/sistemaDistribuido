[CmdletBinding()]
param(
    [string]$ApiUrl = 'http://localhost:8080',
    [string]$Token,
    [string]$Username,
    [string]$Password,
    [Parameter(Mandatory)][string]$EmailDestination,
    [Parameter(Mandatory)][string]$SmsDestination,
    [Parameter(Mandatory)][string]$WhatsAppDestination
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($Token)) {
    if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
        throw 'Indica un Token de ADMIN_SISTEMA o Username y Password.'
    }
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $session = Invoke-RestMethod -Method Post -Uri "$ApiUrl/api/auth/login" `
        -ContentType 'application/json' -Body $body
    $Token = $session.token
}

$headers = @{ Authorization = "Bearer $Token" }
$destinations = [ordered]@{
    EMAIL = $EmailDestination
    SMS = $SmsDestination
    WHATSAPP = $WhatsAppDestination
}
foreach ($entry in $destinations.GetEnumerator()) {
    $request = @{ canal = $entry.Key; destinatario = $entry.Value } | ConvertTo-Json
    $result = Invoke-RestMethod -Method Post `
        -Uri "$ApiUrl/api/notificaciones/proveedores/certificar" `
        -Headers $headers -ContentType 'application/json' -Body $request
    if ($result.modo -ne 'WEBHOOK' -or $result.resultado -notmatch '^WEBHOOK:2\d\d$') {
        throw "El canal $($entry.Key) no quedó certificado con un proveedor real."
    }
    Write-Host "[OK] $($entry.Key): $($result.resultado)" -ForegroundColor Green
}
