Set-Location -LiteralPath $PSScriptRoot

Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), [System.EnvironmentVariableTarget]::Process)
    }
}

$contenedorPostgres = "shuji-pg-local-audit"
$estadoContenedor = & docker inspect --format '{{.State.Status}}' $contenedorPostgres 2>$null
if ($LASTEXITCODE -ne 0 -or $estadoContenedor.Trim() -ne "running") {
    throw "El contenedor PostgreSQL local '$contenedorPostgres' no está iniciado."
}

$variablesContenedor = & docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' $contenedorPostgres 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "No se pudo leer la configuración del contenedor PostgreSQL local."
}
$lineaPasswordPostgres = $variablesContenedor | Where-Object { $_ -like "POSTGRES_PASSWORD=*" } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($lineaPasswordPostgres)) {
    throw "El contenedor PostgreSQL local no expone la credencial requerida."
}

$env:SUPABASE_DB_HOST = "127.0.0.1"
$env:SUPABASE_DB_PORT = "55439"
$env:SUPABASE_DB_NAME = "shuji_audit"
$env:SUPABASE_DB_USER = "postgres"
$env:SUPABASE_DB_PASSWORD = $lineaPasswordPostgres.Substring("POSTGRES_PASSWORD=".Length)
$env:SUPABASE_DB_SSLMODE = "disable"

Write-Host "Starting Shuji Backend on port $env:SERVER_PORT with local Docker database shuji_audit..."
java -jar .\target\shuji-backend-1.0.0-SNAPSHOT.jar
