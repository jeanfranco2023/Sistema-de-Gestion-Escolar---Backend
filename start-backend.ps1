Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), [System.EnvironmentVariableTarget]::Process)
    }
}
Write-Host "Starting Shuji Backend on port $env:SERVER_PORT with Supabase host $env:SUPABASE_DB_HOST..."
java -jar .\target\shuji-backend-1.0.0-SNAPSHOT.jar
