# start.ps1 — Load .env and run the app (PowerShell)
$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env file not found. Copy .env.example to .env and fill in values."
    exit 1
}

Get-Content $envFile | Where-Object { $_ -match '^\s*[^#]\S*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}

mvn clean spring-boot:run "-Dspring-boot.run.jvmArguments=--enable-preview"
