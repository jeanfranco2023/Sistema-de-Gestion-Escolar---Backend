$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSCommandPath
$envFile = Join-Path $backendRoot '.env.local-docker'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Missing .env.local-docker' }

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $separator = $line.IndexOf('=')
        if ($separator -gt 0) {
            $name = $line.Substring(0, $separator).Trim()
            $value = $line.Substring($separator + 1).Trim()
            if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw "Invalid environment variable name: $name" }
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

$containerRunning = docker inspect --format '{{.State.Running}}' shuji-postgres 2>$null
if ($LASTEXITCODE -ne 0) { throw 'Docker container shuji-postgres was not found' }
if ($containerRunning -ne 'true') { docker start shuji-postgres | Out-Null }

if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = 'local-docker' }

Push-Location $backendRoot
try {
    & .\mvnw.cmd spring-boot:run
    $appExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $appExitCode
