param(
    [switch]$StartSonar,
    [switch]$NoFrontend,
    [switch]$KeepAlive
)

$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Split-Path -Parent $backendRoot
$frontendRoot = Join-Path $projectRoot 'frontend'
$runtimeRoot = Join-Path $backendRoot '.runtime\local-services'
$jar = Join-Path $backendRoot 'target\shuji-backend-1.0.0-SNAPSHOT.jar'
$java = 'C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe'

function Get-VerifiedContainer([string]$Name, [string]$ExpectedImage) {
    $raw = & docker inspect $Name 2>$null
    if ($LASTEXITCODE -ne 0) { throw "Required container '$Name' does not exist; this script never creates containers." }
    $container = @($raw | ConvertFrom-Json)[0]
    if ($container.Name.TrimStart('/') -ne $Name -or $container.Config.Image -ne $ExpectedImage) {
        throw "Container '$Name' failed the expected name/image check."
    }
    return $container
}

function Start-ExistingContainer([string]$Name, [string]$ExpectedImage) {
    $container = Get-VerifiedContainer $Name $ExpectedImage
    if ($container.State.Status -ne 'running') {
        $null = & docker start $Name
        if ($LASTEXITCODE -ne 0) { throw "Could not start existing container '$Name'." }
    }
    return Get-VerifiedContainer $Name $ExpectedImage
}

function Test-Listening([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Test-BackendHealth {
    try { return (Invoke-RestMethod -Uri 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 3).status -eq 'UP' }
    catch { return $false }
}

if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "Current backend JAR not found: $jar" }
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Validated Java 25 executable not found: $java" }
if (-not (Test-Path -LiteralPath $runtimeRoot -PathType Container)) { $null = New-Item -ItemType Directory -Path $runtimeRoot -Force }

$null = & docker info 2>$null
if ($LASTEXITCODE -ne 0) { throw 'Docker is not available.' }
$postgres = Start-ExistingContainer 'shuji-postgres' 'postgres:16-alpine'
$dbPort = $postgres.HostConfig.PortBindings.PSObject.Properties['5432/tcp'].Value[0]
if (-not $dbPort -or $dbPort.HostPort -ne '5433' -or $dbPort.HostIp -ne '127.0.0.1') {
    throw "The existing PostgreSQL container is not bound to the expected local-only port 127.0.0.1:5433."
}

if ($StartSonar) {
    $null = Start-ExistingContainer 'shuji-sonarqube' 'sonarqube:26.10.0.133078-community'
}

$existingBackend = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.ExecutablePath -eq $java -and $_.CommandLine -like '*shuji-backend-1.0.0-SNAPSHOT.jar*' } | Select-Object -First 1
if (Test-BackendHealth) {
    $backendPid = if ($existingBackend) { $existingBackend.ProcessId } else { $null }
    Write-Host 'Backend already healthy at http://localhost:8080.'
}
elseif (Test-Listening 8080) {
    if ($existingBackend) { $backendPid = $existingBackend.ProcessId; Write-Host "Backend is already starting (PID $backendPid); leaving it unchanged." }
    else { throw 'Port 8080 is occupied, but it is not the healthy Shuji backend.' }
}
elseif ($existingBackend) {
    $backendPid = $existingBackend.ProcessId
    Write-Host "Backend is already starting (PID $($existingBackend.ProcessId)); leaving it unchanged."
}
else {
    $envMap = @{}
    foreach ($entry in $postgres.Config.Env) {
        $separator = $entry.IndexOf('=')
        if ($separator -gt 0) { $envMap[$entry.Substring(0, $separator)] = $entry.Substring($separator + 1) }
    }
    if ($envMap.POSTGRES_DB -ne 'Sistema Escolar' -or $envMap.POSTGRES_USER -ne 'postgres' -or [string]::IsNullOrWhiteSpace($envMap.POSTGRES_PASSWORD)) {
        throw 'The verified PostgreSQL container does not match the expected local QA database settings.'
    }

    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $version = (& $java -version 2>&1 | Out-String)
    $ErrorActionPreference = $oldEap
    if ($LASTEXITCODE -ne 0 -or $version -notmatch 'version "25\.') { throw 'Java 25 is required to start the backend.' }

    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $stdout = Join-Path $runtimeRoot "backend-$timestamp.out.log"
    $stderr = Join-Path $runtimeRoot "backend-$timestamp.err.log"
    $randomBytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($randomBytes)
    $jwtSecret = [Convert]::ToBase64String($randomBytes)
    $rng.Dispose()
    [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    $keys = @('SPRING_PROFILES_ACTIVE','SERVER_PORT','SUPABASE_DB_HOST','SUPABASE_DB_PORT','SUPABASE_DB_NAME','SUPABASE_DB_USER','SUPABASE_DB_PASSWORD','SUPABASE_DB_SSLMODE','SUPABASE_URL','SUPABASE_SERVICE_ROLE_KEY','JWT_SECRET','CORS_ALLOWED_ORIGINS','LOCAL_STORAGE_ROOT','GEMINI_API_KEY','GEMINI_API_KEYS','RENIEC_API_KEY','RENIEC_ACCESS_TOKEN','MERCADOPAGO_API_URL','MERCADOPAGO_ACCESS_TOKEN')
    $previous = @{}
    foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
    try {
        [Environment]::SetEnvironmentVariable('SPRING_PROFILES_ACTIVE', 'local-docker', 'Process')
        [Environment]::SetEnvironmentVariable('SERVER_PORT', '8080', 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_HOST', '127.0.0.1', 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_PORT', '5433', 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_NAME', $envMap.POSTGRES_DB, 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_USER', $envMap.POSTGRES_USER, 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_PASSWORD', $envMap.POSTGRES_PASSWORD, 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_DB_SSLMODE', 'disable', 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_URL', '', 'Process')
        [Environment]::SetEnvironmentVariable('SUPABASE_SERVICE_ROLE_KEY', '', 'Process')
        [Environment]::SetEnvironmentVariable('JWT_SECRET', $jwtSecret, 'Process')
        [Environment]::SetEnvironmentVariable('CORS_ALLOWED_ORIGINS', 'http://localhost:4200,http://127.0.0.1:4200', 'Process')
        [Environment]::SetEnvironmentVariable('LOCAL_STORAGE_ROOT', (Join-Path $backendRoot '.runtime\private-files'), 'Process')
        [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', '', 'Process')
        [Environment]::SetEnvironmentVariable('GEMINI_API_KEYS', '', 'Process')
        [Environment]::SetEnvironmentVariable('RENIEC_API_KEY', '', 'Process')
        [Environment]::SetEnvironmentVariable('RENIEC_ACCESS_TOKEN', '', 'Process')
        [Environment]::SetEnvironmentVariable('MERCADOPAGO_API_URL', 'http://127.0.0.1:1', 'Process')
        [Environment]::SetEnvironmentVariable('MERCADOPAGO_ACCESS_TOKEN', '', 'Process')
        $arguments = '-Dspring.profiles.active=local-docker -jar "' + $jar + '"'
        $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $backendRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $backendPid = $process.Id
    }
    finally {
        foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
        $envMap.POSTGRES_PASSWORD = $null
        $jwtSecret = $null
    }

    $healthy = $false
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        if (Test-BackendHealth) { $healthy = $true; break }
        $process.Refresh()
        if ($process.HasExited -or (Test-Listening 8080)) { break }
        Start-Sleep -Seconds 2
    }
    $process.Refresh()
    if ($process.HasExited) { throw "Backend process exited; inspect local logs under $runtimeRoot." }
    if ($healthy) { Write-Host "Backend started with Java 25 (PID $($process.Id)) and health UP at http://localhost:8080. Logs: $runtimeRoot" }
    else { Write-Host "Backend startup is still in progress (PID $($process.Id)); run this script again to check without starting a duplicate. Logs: $runtimeRoot" }
}

if (-not $NoFrontend -and -not (Test-Listening 4200)) {
    if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules') -PathType Container)) {
        throw 'Frontend dependencies are missing; run npm ci in the frontend folder first.'
    }
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    $frontendOut = Join-Path $runtimeRoot 'frontend.out.log'
    $frontendErr = Join-Path $runtimeRoot 'frontend.err.log'
    $null = Start-Process -FilePath $npm -ArgumentList 'start -- --host 127.0.0.1 --port 4200' -WorkingDirectory $frontendRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr
    $ready = $false
    for ($attempt = 0; $attempt -lt 15; $attempt++) {
        if (Test-Listening 4200) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw "Frontend did not open port 4200; inspect $frontendOut and $frontendErr." }
    Write-Host 'Frontend started at http://localhost:4200.'
}
elseif (-not $NoFrontend) {
    Write-Host 'Port 4200 is already listening; leaving the frontend process unchanged.'
}

if ($KeepAlive) {
    if (-not $backendPid) { throw 'No local backend process was found to keep alive.' }
    Write-Host "Keeping backend PID $backendPid attached to this session; stop with Ctrl+C."
    Wait-Process -Id $backendPid
}

if ($StartSonar) { Write-Host 'Optional local SonarQube container is running.' }
