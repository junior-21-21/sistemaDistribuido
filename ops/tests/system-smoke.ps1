[CmdletBinding()]
param(
    [string]$FrontendUrl = 'http://localhost',
    [string]$ApiUrl = 'http://localhost:8080',
    [switch]$UseDemoCredentials,
    [switch]$SkipUnavailableAccounts,
    [ValidateRange(5, 600)]
    [int]$StartupTimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:checks = 0

function Wait-HttpReady {
    param([string]$Name, [string]$Url)
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Method GET -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 500) {
                return
            }
        }
        catch {
            # El contenedor puede estar iniciado mientras Spring todavía carga el contexto.
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "$Name no estuvo disponible en $Url después de $StartupTimeoutSeconds segundos."
}

function Assert-Status {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [int]$Expected,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [switch]$ExpectJson,
        [string]$CorrelationId
    )
    try {
        $arguments = @{
            Method = $Method
            Uri = $Url
            Headers = $Headers
            UseBasicParsing = $true
        }
        if ($null -ne $Body) {
            $arguments.ContentType = 'application/json'
            $arguments.Body = $Body | ConvertTo-Json
        }
        $response = Invoke-WebRequest @arguments
        $status = [int]$response.StatusCode
        if ($ExpectJson) {
            if ($response.Headers['Content-Type'] -notmatch 'application/json') {
                throw "$Name no devolvió Content-Type JSON."
            }
            $null = $response.Content | ConvertFrom-Json
        }
        if ($CorrelationId -and $response.Headers['X-Correlation-Id'] -ne $CorrelationId) {
            throw "$Name no propagó X-Correlation-Id."
        }
    }
    catch {
        $responseProperty = $_.Exception.PSObject.Properties['Response']
        if ($null -eq $responseProperty -or $null -eq $responseProperty.Value) {
            throw "$Name no respondió: $($_.Exception.Message)"
        }
        $status = [int]$responseProperty.Value.StatusCode
    }
    if ($status -ne $Expected) {
        throw "$Name devolvió HTTP $status; se esperaba $Expected."
    }
    $script:checks++
    Write-Host "[OK] $Name ($status)" -ForegroundColor Green
}

function Login {
    param([string]$Username, [string]$Password)
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    while ($true) {
        try {
            $session = Invoke-RestMethod -Method Post -Uri "$ApiUrl/api/auth/login" -ContentType 'application/json' -Body $body
            break
        }
        catch {
            $status = $null
            $responseProperty = $_.Exception.PSObject.Properties['Response']
            if ($null -ne $responseProperty -and $null -ne $responseProperty.Value) {
                $status = [int]$responseProperty.Value.StatusCode
            }
            if ($status -in @(500, 502, 503, 504) -and (Get-Date) -lt $deadline) {
                Start-Sleep -Seconds 2
                continue
            }
            if ($SkipUnavailableAccounts -and $status -in @(401, 403)) {
                Write-Warning "Se omite '$Username': sus credenciales demo ya no son válidas."
                return $null
            }
            throw "No se pudo autenticar a '$Username': $($_.Exception.Message)"
        }
    }
    if ([string]::IsNullOrWhiteSpace($session.token)) {
        throw "El login de '$Username' no devolvió un JWT."
    }
    $script:checks++
    Write-Host "[OK] Login $Username" -ForegroundColor Green
    return @{ Authorization = "Bearer $($session.token)" }
}

function Check-Get {
    param([string]$Name, [string]$Path, [hashtable]$Headers)
    $correlationId = "contract-$([guid]::NewGuid().ToString('N'))"
    $requestHeaders = @{} + $Headers
    $requestHeaders['X-Correlation-Id'] = $correlationId
    Assert-Status -Name $Name -Method GET -Url "$ApiUrl$Path" -Expected 200 `
        -Headers $requestHeaders -ExpectJson -CorrelationId $correlationId
}

function Check-Role {
    param(
        [string]$Username,
        [string]$Password,
        [System.Collections.IDictionary]$Checks
    )
    $headers = Login $Username $Password
    if ($null -eq $headers) {
        return $null
    }
    foreach ($check in $Checks.GetEnumerator()) {
        Check-Get $check.Key $check.Value $headers
    }
    return $headers
}

Wait-HttpReady -Name 'Frontend' -Url $FrontendUrl
Wait-HttpReady -Name 'Gateway' -Url "$ApiUrl/actuator/health"
Assert-Status -Name 'Frontend' -Method GET -Url $FrontendUrl -Expected 200
Assert-Status -Name 'Gateway sin token' -Method GET -Url "$ApiUrl/api/estudiantes/page?size=1" -Expected 401

if (-not $UseDemoCredentials) {
    throw 'Use -UseDemoCredentials con una instalación local sembrada, o adapte este script a su almacén seguro de credenciales.'
}

# Contraseñas: variable de entorno PAD_CLAVE_<USUARIO> (por ejemplo PAD_CLAVE_DOCENTE1) o la de demostración.
function Clave([string]$Usuario, [string]$Demo) {
    $variable = 'PAD_CLAVE_' + ($Usuario.ToUpperInvariant() -replace '[^A-Z0-9]', '_')
    $valor = [Environment]::GetEnvironmentVariable($variable)
    if ([string]::IsNullOrWhiteSpace($valor)) { return $Demo }
    return $valor
}

# --- Dirección (director/a y subdirección) ---
$director = Check-Role 'director' (Clave 'director' 'DemoDirector2026*') ([ordered]@{
    'Dashboard institucional' = '/api/estadisticas/dashboard'
    'Auditoría' = '/api/auditoria?entidad=ESTUDIANTE&size=1'
    'Estado de bimestres para dirección' = "/api/bimestres?anio=$((Get-Date).Year)"
    'Auditoría de bimestre para dirección' = "/api/bimestres/$((Get-Date).Year)/I/auditoria"
    'Usuarios paginados' = '/api/usuarios/page?size=1'
    'Módulos del colegio' = '/api/institucion/modulos'
    'Incidencias de integridad' = '/api/integridad/incidencias'
    'Nóminas de matrícula' = "/api/nominas?anio=$((Get-Date).Year)"
    'Permisos y justificaciones (Dirección)' = '/api/permisos'
})
$subdireccion = Check-Role 'coord.academica' (Clave 'coord.academica' 'DemoCoordinacion2026*') ([ordered]@{
    'Cursos' = '/api/cursos'
    'Docentes' = '/api/docentes'
    'Secciones' = '/api/secciones'
    'Carga académica' = '/api/asignaciones-academicas'
    'Notas paginadas' = '/api/notas/page?size=1'
    'Tickets para Dirección' = '/api/tickets'
    'Nóminas para Subdirección' = "/api/nominas?anio=$((Get-Date).Year)"
})
# --- Secretaría ---
$secretaria = Check-Role 'secretaria1' (Clave 'secretaria1' 'DemoSecretaria2026*') ([ordered]@{
    'Matrículas para secretaría' = '/api/estudiantes/page?size=1'
    'Secciones para secretaría' = '/api/secciones'
    'Horario por sección' = '/api/horarios/seccion/1'
    'Historial de consentimientos' = '/api/consentimientos?estudianteId=1'
    'Recuperación pedagógica' = '/api/recuperacion'
    'Nóminas para secretaría' = "/api/nominas?anio=$((Get-Date).Year)"
    'Permisos para secretaría' = '/api/permisos'
})
# --- Docente ---
$docente = Check-Role 'docente1' (Clave 'docente1' 'DemoDocente2026*') ([ordered]@{
    'Notas por docente' = '/api/notas/page?size=1'
    'Carga propia del docente' = '/api/asignaciones-academicas/mias'
    'Tickets por docente' = '/api/tickets'
    'Derivaciones por docente' = '/api/derivaciones'
    'Estado de bimestres para docente' = "/api/bimestres?anio=$((Get-Date).Year)"
})
if ($null -ne $docente) {
    # La sección se toma de la carga vigente del docente: un ID fijo deja de servir al recargar los datos demo.
    $carga = @(Invoke-RestMethod -Method GET -Uri "$ApiUrl/api/asignaciones-academicas/mias" -Headers $docente | ForEach-Object { $_ })
    if ($carga.Count -gt 0) {
        $seccionPropia = $carga[0].seccionId
        Check-Get 'Asistencia por docente' "/api/asistencias?seccionId=$seccionPropia&fecha=$((Get-Date).ToString('yyyy-MM-dd'))" $docente
        Check-Get 'Disciplina por docente' "/api/reportes-disciplinarios?seccionId=$seccionPropia" $docente
    }
    else {
        Write-Warning "docente1 no tiene carga académica activa; se omiten asistencia y disciplina por sección."
    }
}
# --- Auxiliares (uno con el encargo de responsable de convivencia) ---
$responsable = Check-Role 'coord.convivencia' (Clave 'coord.convivencia' 'DemoCoordinacion2026*') ([ordered]@{
    'Ingresos del día (responsable)' = '/api/ingresos/hoy'
    'Disciplina (responsable)' = '/api/reportes-disciplinarios'
    'Derivaciones (responsable)' = '/api/derivaciones'
    'Avisos a familias (responsable)' = '/api/notificaciones?size=1'
    'Permisos pendientes (TOE)' = '/api/permisos?estado=PENDIENTE'
})
$auxiliar = Check-Role 'auxiliar1' (Clave 'auxiliar1' 'DemoAuxiliar2026*') ([ordered]@{
    'Ingresos del día (auxiliar)' = '/api/ingresos/hoy'
    'Asistencia auxiliar' = '/api/asistencias?seccionId=1&fecha=2026-09-12'
    'Disciplina auxiliar' = '/api/reportes-disciplinarios'
    'Permisos (auxiliar)' = '/api/permisos'
    'Salidas de hoy (auxiliar)' = '/api/permisos/salidas-hoy'
})
$portero = Check-Role 'portero1' (Clave 'portero1' 'DemoPorteria2026*') ([ordered]@{
    'Ingresos del día (portería)' = '/api/ingresos/hoy'
    'Salidas de hoy (portería)' = '/api/permisos/salidas-hoy'
})
# --- Módulos opcionales ---
$psicologia = Check-Role 'psicologa1' (Clave 'psicologa1' 'DemoPsicologia2026*') ([ordered]@{
    'Derivaciones psicología' = '/api/derivaciones'
})
$salud = Check-Role 'enfermera1' (Clave 'enfermera1' 'DemoSalud2026*') ([ordered]@{
    'Fichas médicas' = '/api/fichas-medicas'
    'Atenciones médicas' = '/api/atenciones-medicas/ficha/1/page?size=1'
})
$mantenimiento = Check-Role 'mantenimiento1' (Clave 'mantenimiento1' 'DemoMantenimiento2026*') ([ordered]@{
    'Bandeja de mantenimiento' = '/api/tickets'
    'Aulas para mantenimiento' = '/api/aulas'
})
# --- Familia ---
$apoderado = Check-Role 'apoderado1' (Clave 'apoderado1' 'DemoApoderado2026*') ([ordered]@{
    'Portal de apoderado' = '/api/portal-apoderado/estudiantes'
    'Solicitudes del apoderado' = '/api/portal-apoderado/permisos'
})

# --- Separación de funciones ---
if ($null -ne $subdireccion) {
    Assert-Status -Name 'Subdirección no administra usuarios' -Method GET -Url "$ApiUrl/api/usuarios" -Expected 403 -Headers $subdireccion
}
if ($null -ne $portero) {
    Assert-Status -Name 'Portería no ve estudiantes' -Method GET -Url "$ApiUrl/api/estudiantes/page?size=1" -Expected 403 -Headers $portero
    Assert-Status -Name 'Portería no aprueba permisos' -Method GET -Url "$ApiUrl/api/permisos" -Expected 403 -Headers $portero
}
if ($null -ne $secretaria) {
    Assert-Status -Name 'Secretaría no aprueba nóminas' -Method POST -Url "$ApiUrl/api/nominas/1/aprobar" -Expected 403 -Headers $secretaria
}
if ($null -ne $docente) {
    Assert-Status -Name 'Docente bloqueado en usuarios' -Method GET -Url "$ApiUrl/api/usuarios" -Expected 403 -Headers $docente
    Assert-Status -Name 'Docente bloqueado en auditoría de bimestre' -Method GET `
        -Url "$ApiUrl/api/bimestres/$((Get-Date).Year)/I/auditoria" -Expected 403 -Headers $docente
}
if ($null -ne $auxiliar) {
    Assert-Status -Name 'Auxiliar sin encargo no ve avisos a familias' -Method GET `
        -Url "$ApiUrl/api/notificaciones?size=1" -Expected 403 -Headers $auxiliar
}
if ($null -ne $secretaria) {
    Assert-Status -Name 'Secretaría no gestiona cuentas' -Method GET -Url "$ApiUrl/api/usuarios" -Expected 403 -Headers $secretaria
}
if ($null -ne $mantenimiento) {
    Assert-Status -Name 'Mantenimiento bloqueado en estudiantes' -Method GET `
        -Url "$ApiUrl/api/estudiantes/page?size=1" -Expected 403 -Headers $mantenimiento
}

Write-Host ''
Write-Host "Smoke test aprobado: $script:checks verificaciones." -ForegroundColor Green
