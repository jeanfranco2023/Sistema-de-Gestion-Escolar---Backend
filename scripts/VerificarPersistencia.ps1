param([string]$SeleccionPruebas = '!CargaYRegresionNPlusOneTest')
$ErrorActionPreference = 'Stop'
$repoPrueba = Split-Path -Parent $PSScriptRoot
$javaPrueba = 'C:\Program Files\Java\jdk-21\bin\java.exe'
$jdbcPrueba = 'C:\Users\PC\.m2\repository\org\postgresql\postgresql\42.7.13\postgresql-42.7.13.jar'
$esquemaPrueba = 'codex_it_v18_' + [guid]::NewGuid().ToString('N').Substring(0, 10)
$configPrueba = @{}
foreach ($lineaPrueba in Get-Content -LiteralPath (Join-Path $repoPrueba '.env')) {
    if ($lineaPrueba -match '^\s*([A-Z0-9_]+)\s*=(.*)$') {
        $configPrueba[$matches[1]] = $matches[2].Trim().Trim([char]34).Trim([char]39)
    }
}
$copiasPrueba = Join-Path $repoPrueba 'target\codex-it-v18'
New-Item -ItemType Directory -Path $copiasPrueba -Force | Out-Null
foreach ($archivoPrueba in Get-ChildItem -LiteralPath (Join-Path $repoPrueba 'src\main\resources\db\migration') -Filter 'V*__*.sql') {
    $sqlPrueba = [IO.File]::ReadAllText($archivoPrueba.FullName)
    $sqlPrueba = [regex]::Replace($sqlPrueba, '\bpublic\b', $esquemaPrueba)
    $sqlPrueba = "SET LOCAL search_path TO $esquemaPrueba, public, extensions;`n" + $sqlPrueba
    [IO.File]::WriteAllText((Join-Path $copiasPrueba $archivoPrueba.Name), $sqlPrueba, [Text.UTF8Encoding]::new($false))
}
$variablesPrueba = @{
    TEST_DB_URL = "jdbc:postgresql://$($configPrueba.SUPABASE_DB_HOST):$($configPrueba.SUPABASE_DB_PORT)/postgres?sslmode=require&currentSchema=$esquemaPrueba,public,extensions"
    TEST_DB_USERNAME = $configPrueba.SUPABASE_DB_USER
    TEST_DB_PASSWORD = $configPrueba.SUPABASE_DB_PASSWORD
    TEST_DB_SCHEMA = $esquemaPrueba
    TEST_DB_FLYWAY_LOCATIONS = 'filesystem:' + $copiasPrueba.Replace('\','/')
    JAVA_HOME = 'C:\Program Files\Java\jdk-21'
}
$anterioresPrueba = @{}
$creadoPrueba = $false
try {
    & $javaPrueba -cp $jdbcPrueba (Join-Path $PSScriptRoot 'VerificarEsquema.java') $repoPrueba preparar $esquemaPrueba
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el esquema aislado de prueba.' }
    $creadoPrueba = $true
    foreach ($clavePrueba in $variablesPrueba.Keys) {
        $anterioresPrueba[$clavePrueba] = [Environment]::GetEnvironmentVariable($clavePrueba, 'Process')
        [Environment]::SetEnvironmentVariable($clavePrueba, $variablesPrueba[$clavePrueba], 'Process')
    }
    Push-Location -LiteralPath $repoPrueba
    try {
        & .\mvnw.cmd '-Dmaven.compiler.fork=true' "-Dtest=$SeleccionPruebas" test
        if ($LASTEXITCODE -ne 0) { throw 'Falló la verificación de persistencia o concurrencia; consultar Surefire.' }
    } finally { Pop-Location }
} finally {
    foreach ($clavePrueba in $anterioresPrueba.Keys) {
        [Environment]::SetEnvironmentVariable($clavePrueba, $anterioresPrueba[$clavePrueba], 'Process')
    }
    if ($creadoPrueba) {
        & $javaPrueba -cp $jdbcPrueba (Join-Path $PSScriptRoot 'VerificarEsquema.java') $repoPrueba limpiar $esquemaPrueba
        if ($LASTEXITCODE -ne 0) { throw "No se pudo eliminar el esquema temporal $esquemaPrueba" }
    }
}
