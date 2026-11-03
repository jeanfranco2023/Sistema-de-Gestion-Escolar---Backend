$ErrorActionPreference = 'Stop'
$backendPath = Split-Path $PSScriptRoot -Parent
$jarPath = [IO.Path]::GetFullPath((Join-Path $backendPath 'target\shuji-backend-1.0.0-SNAPSHOT.jar'))
try {
    $jarProcesses = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jarPath, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
} catch {
    throw 'No se pudo comprobar si una API mantiene el JAR abierto; detén las APIs antes de ejecutar verify.'
}
if ($jarProcesses.Count -gt 0) {
    throw 'Detén las APIs que usan el JAR antes de ejecutar verify: Maven puede reemplazar el archivo durante package/repackage.'
}
$qaSchema = 'qa_integral_' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$container = 'shuji-postgres'
$database = 'Sistema Escolar'
$logPath = Join-Path ([IO.Path]::GetTempPath()) ($qaSchema + '.log')

function Invoke-LocalSql([string] $sql) {
    $result = $sql | docker exec -i $container psql -U $dbUser -d $database -v ON_ERROR_STOP=1 -qAt 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Falló la operación SQL de QA local; no se muestran datos de las filas.' }
    return $result
}

$configuration = @(docker inspect $container | ConvertFrom-Json)[0]
if ($LASTEXITCODE -ne 0 -or $configuration.Config.Image -notmatch '^postgres:') {
    throw 'La prueba requiere el contenedor PostgreSQL local shuji-postgres.'
}
$containerEnv = @{}
foreach ($entry in $configuration.Config.Env) {
    $parts = $entry.Split('=', 2)
    $containerEnv[$parts[0]] = $parts[1]
}
if ($containerEnv['POSTGRES_DB'] -ne $database) { throw 'Base local inesperada.' }
$dbUser = $containerEnv['POSTGRES_USER']
$hostPort = $configuration.NetworkSettings.Ports.'5432/tcp'[0].HostPort
if ($configuration.NetworkSettings.Ports.'5432/tcp'[0].HostIp -ne '127.0.0.1') {
    throw 'La prueba requiere PostgreSQL publicado exclusivamente en loopback.'
}
$javaDirectory = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-25*' |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $javaDirectory) { throw 'Se requiere JDK 25.' }

try {
    # La copia permanece en memoria y en un esquema efímero de la MISMA base Docker.
    $dump = docker exec $container pg_dump -U $dbUser -d $database -n public --no-owner --no-privileges 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible preparar el esquema aislado.' }
    $isolatedDump = ($dump -join "`n") -replace '\bpublic\b', $qaSchema
    Invoke-LocalSql $isolatedDump | Out-Null
    $unsafeFunctions = Invoke-LocalSql "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='$qaSchema' AND (coalesce(p.proconfig::text,'') ~ '\mpublic\M' OR p.prosrc ~ '\mpublic\M');"
    if ([int]$unsafeFunctions -ne 0) { throw 'El esquema QA aún contiene funciones que apuntan a public.' }
    $dump = $null
    $isolatedDump = $null

    $env:JAVA_HOME = $javaDirectory.FullName
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $env:TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$hostPort/$database`?currentSchema=$qaSchema&sslmode=disable"
    $env:TEST_DB_USERNAME = $dbUser
    $env:TEST_DB_PASSWORD = $containerEnv['POSTGRES_PASSWORD']
    $env:TEST_DB_SCHEMA = $qaSchema
    $env:SPRING_FLYWAY_ENABLED = 'false'
    $env:SUPABASE_URL = 'https://example.invalid'
    $env:SUPABASE_SERVICE_ROLE_KEY = 'disabled-local-qa'
    $env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
    $env:MERCADOPAGO_API_URL = 'http://127.0.0.1:1'
    $env:MERCADOPAGO_ACCESS_TOKEN = ''
    $env:GEMINI_API_KEY = ''
    $env:GEMINI_API_KEYS = ''
    $env:RUN_MUTATING_POSTGRES_TESTS = 'true'
    Push-Location $backendPath
    try {
        Remove-Item -LiteralPath (Join-Path $backendPath 'target\jacoco.exec') -ErrorAction SilentlyContinue
        & .\mvnw.cmd verify *> $logPath
        $testExit = $LASTEXITCODE
    } finally { Pop-Location }
    Select-String -LiteralPath $logPath -Pattern 'Tests run:|BUILD SUCCESS|BUILD FAILURE|^\[ERROR\]   ' |
        ForEach-Object { $_.Line }
    Write-Output "QA log: $logPath"
    if ($testExit -ne 0) { throw 'Fallaron pruebas de integración en el esquema QA; revisar el log local.' }
} finally {
    # El guard impide que una limpieza accidental seleccione public u otro esquema.
    if ($qaSchema -notmatch '^qa_integral_[0-9a-f]{12}$') { throw 'Nombre de esquema QA inválido.' }
    Invoke-LocalSql "DROP SCHEMA IF EXISTS $qaSchema CASCADE;" | Out-Null
    Remove-Item Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
    Write-Output 'Esquema QA eliminado; se conserva una única base Docker.'
}
