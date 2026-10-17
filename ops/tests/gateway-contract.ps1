[CmdletBinding()]
param(
    [string]$ApiUrl = 'http://localhost:8080',
    # Clave de mantenimiento1 (las cuentas temporales copian su hash). Por defecto: PAD_CLAVE_MANTENIMIENTO1 o la demo.
    [string]$Password = $(if ($env:PAD_CLAVE_MANTENIMIENTO1) { $env:PAD_CLAVE_MANTENIMIENTO1 } else { 'DemoMantenimiento2026*' })
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$prefix = 'contract-'
$testYear = 2099
$script:checks = 0

function Wait-HttpReady([string]$Url, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ([int]$response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "El gateway no estuvo disponible en $Url dentro del plazo."
}

function Invoke-EstudiantesSql([string]$Sql) {
    & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres `
        -d academico_estudiantes -c $Sql | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falló la preparación SQL de identidades contractuales.' }
}

function Invoke-EvaluacionesSql([string]$Sql) {
    & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres `
        -d academico_evaluaciones -c $Sql | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falló la limpieza SQL del período contractual.' }
}

function Login([string]$Username) {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $deadline = (Get-Date).AddSeconds(180)
    while ($true) {
        try {
            $session = Invoke-RestMethod -Method POST -Uri "$ApiUrl/api/auth/login" `
                -ContentType 'application/json' -Body $body
            break
        }
        catch {
            $status = if ($null -ne $_.Exception.Response) {
                [int]$_.Exception.Response.StatusCode
            } else { 0 }
            if ($status -in @(500, 502, 503, 504) -and (Get-Date) -lt $deadline) {
                Start-Sleep -Seconds 2
                continue
            }
            throw
        }
    }
    if ([string]::IsNullOrWhiteSpace($session.token)) { throw "$Username no devolvió JWT." }
    return @{ Authorization = "Bearer $($session.token)" }
}

function Assert-Status {
    param([string]$Name, [string]$Method, [string]$Path, [hashtable]$Headers,
          [int]$Expected, [object]$Body = $null, [switch]$ExpectJson)
    $correlationId = "contract-$([guid]::NewGuid().ToString('N'))"
    $requestHeaders = @{} + $Headers
    $requestHeaders['X-Correlation-Id'] = $correlationId
    try {
        $arguments = @{
            Method = $Method; Uri = "$ApiUrl$Path"; Headers = $requestHeaders;
            UseBasicParsing = $true; TimeoutSec = 15
        }
        if ($null -ne $Body) {
            $arguments.ContentType = 'application/json'
            $arguments.Body = $Body | ConvertTo-Json
        }
        # Compatible con Windows PowerShell 5.1 (sin -SkipHttpErrorCheck): los 4xx/5xx
        # llegan como WebException y se leen desde su respuesta.
        try {
            $response = Invoke-WebRequest @arguments
            $status = [int]$response.StatusCode
            $contentType = $response.Headers['Content-Type']
            $content = $response.Content
            $responseCorrelation = $response.Headers['X-Correlation-Id']
        }
        catch [System.Net.WebException] {
            $errorResponse = $_.Exception.Response
            if ($null -eq $errorResponse) { throw }
            $status = [int]$errorResponse.StatusCode
            $contentType = $errorResponse.ContentType
            $responseCorrelation = $errorResponse.Headers['X-Correlation-Id']
            $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
        catch {
            # PowerShell 7 lanza HttpResponseException en lugar de WebException.
            if ($_.Exception.GetType().Name -ne 'HttpResponseException') { throw }
            $errorResponse = $_.Exception.Response
            $status = [int]$errorResponse.StatusCode
            $contentType = "$($errorResponse.Content.Headers.ContentType)"
            $valores = $null
            $responseCorrelation = if ($errorResponse.Headers.TryGetValues('X-Correlation-Id', [ref]$valores)) { @($valores)[0] } else { $null }
            $content = $_.ErrorDetails.Message
        }
    }
    catch {
        throw "$Name no respondió: $($_.Exception.Message)"
    }
    if ($status -ne $Expected) { throw "$Name devolvió HTTP $status; se esperaba $Expected." }
    if ($responseCorrelation -ne $correlationId) { throw "$Name no propagó X-Correlation-Id." }
    if ($ExpectJson) {
        if ($contentType -notmatch 'application/json') { throw "$Name no devolvió JSON." }
        $null = $content | ConvertFrom-Json
    }
    $script:checks++
    Write-Host "[OK] $Name ($status)" -ForegroundColor Green
    return $content
}

Push-Location $projectRoot
try {
    Wait-HttpReady "$ApiUrl/actuator/health"
    Invoke-EstudiantesSql "DELETE FROM usuarios WHERE username LIKE '$prefix%';"
    Invoke-EvaluacionesSql "DELETE FROM bimestres_auditoria WHERE anio=$testYear; DELETE FROM bimestres_estado WHERE anio=$testYear;"
    foreach ($role in @('DIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR')) {
        $username = "$prefix$($role.ToLowerInvariant().Replace('_','-'))"
        $email = "$username@example.test"
        $sql = "INSERT INTO usuarios(username,password,nombre,apellido,email,rol,estado,debe_cambiar_clave) SELECT '$username',password,'Contrato','$role','$email','$role','ACTIVO',false FROM usuarios WHERE username='mantenimiento1'; INSERT INTO usuario_roles(usuario_id,rol) SELECT id,'$role' FROM usuarios WHERE username='$username';"
        Invoke-EstudiantesSql $sql
    }

    $director = Login "$prefix`director"
    $auxiliar = Login "${prefix}auxiliar"
    $secretaria = Login "$prefix`secretaria"
    $docente = Login "$prefix`docente"

    $students = Assert-Status 'Docente consulta estudiantes con alcance vacío' GET `
        '/api/estudiantes/page?size=5' $docente 200 -ExpectJson
    if (($students | ConvertFrom-Json).totalElements -ne 0) {
        throw 'El docente temporal sin carga académica recibió estudiantes ajenos.'
    }
    $notes = Assert-Status 'Docente consulta notas con alcance vacío' GET `
        '/api/notas/page?size=5' $docente 200 -ExpectJson
    if (($notes | ConvertFrom-Json).totalElements -ne 0) {
        throw 'El docente temporal sin carga académica recibió notas ajenas.'
    }
    Assert-Status 'Docente consulta estado de bimestres' GET `
        "/api/bimestres?anio=$testYear" $docente 200 -ExpectJson | Out-Null
    Assert-Status 'Docente no consulta auditoría de bimestres' GET `
        "/api/bimestres/$testYear/I/auditoria" $docente 403 -ExpectJson | Out-Null

    Assert-Status 'Dirección cierra bimestre' POST "/api/bimestres/$testYear/I/cerrar" `
        $director 200 @{ motivo = 'Cierre contractual automatizado' } -ExpectJson | Out-Null
    Assert-Status 'Docente no reabre bimestre' POST "/api/bimestres/$testYear/I/reabrir" `
        $docente 403 @{ motivo = 'Intento contractual no autorizado' } -ExpectJson | Out-Null
    Assert-Status 'Dirección reabre bimestre' POST "/api/bimestres/$testYear/I/reabrir" `
        $director 200 @{ motivo = 'Reapertura contractual automatizada' } -ExpectJson | Out-Null
    Assert-Status 'Dirección consulta auditoría completa' GET `
        "/api/bimestres/$testYear/I/auditoria" $director 200 -ExpectJson | Out-Null

    Assert-Status 'Secretaría consulta consentimientos' GET `
        '/api/consentimientos?estudianteId=1' $secretaria 200 -ExpectJson | Out-Null
    Assert-Status 'Contrato POST de consentimiento llega al servicio' POST `
        '/api/consentimientos' $secretaria 400 @{} -ExpectJson | Out-Null

    # Encargos: un auxiliar ve los avisos a familias solo cuando Dirección le asigna el encargo
    Assert-Status 'Auxiliar sin encargo no ve avisos a familias' GET '/api/notificaciones?size=1' `
        $auxiliar 403 -ExpectJson | Out-Null
    Invoke-EstudiantesSql "INSERT INTO usuario_roles(usuario_id,rol) SELECT id,'RESPONSABLE_CONVIVENCIA' FROM usuarios WHERE username='${prefix}auxiliar';"
    $responsable = Login "${prefix}auxiliar"
    Assert-Status 'Auxiliar con encargo de convivencia ve avisos' GET '/api/notificaciones?size=1' `
        $responsable 200 -ExpectJson | Out-Null
    Assert-Status 'Secretaría ya no registra ingresos (lo hace el auxiliar)' POST '/api/ingresos' `
        $secretaria 403 @{ codigo = '00000000' } -ExpectJson | Out-Null
    Assert-Status 'Todos consultan los módulos del colegio' GET '/api/institucion/modulos' `
        $docente 200 -ExpectJson | Out-Null
    Assert-Status 'Solo Dirección cambia módulos' PUT '/api/institucion/modulos/PSICOLOGIA' `
        $secretaria 403 @{ habilitado = $true } -ExpectJson | Out-Null

    Write-Host "Prueba contractual aprobada: $script:checks verificaciones." -ForegroundColor Green
}
finally {
    Invoke-EstudiantesSql "DELETE FROM usuarios WHERE username LIKE '$prefix%';"
    Invoke-EvaluacionesSql "DELETE FROM bimestres_auditoria WHERE anio=$testYear; DELETE FROM bimestres_estado WHERE anio=$testYear;"
    Pop-Location
}
