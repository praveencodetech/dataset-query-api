# ==============================================================================
# verify-prod-local.ps1
# Verifies the Spring Boot dataset service against a real PostgreSQL instance
# using the 'prod' profile on Windows.
#
# Prerequisite:
#   Set $env:DATABASE_URL in PowerShell before running:
#   $env:DATABASE_URL="postgres://user:password@host:port/dbname"
# ==============================================================================
$ErrorActionPreference = "Stop"

if (-not $env:DATABASE_URL) {
    Write-Error @"
ERROR: DATABASE_URL environment variable is not set.
Usage:
  `$env:DATABASE_URL="postgres://<user>:<password>@<host>:<port>/<dbname>"
  .\scripts\verify-prod-local.ps1
"@
    exit 1
}

Write-Host "==> DATABASE_URL is set (hidden for security)."
Write-Host "==> Starting application with SPRING_PROFILES_ACTIVE=prod on port 8080..."

$env:SPRING_PROFILES_ACTIVE = "prod"
$env:PORT = "8080"

# Package jar if missing
if (-not (Test-Path "target\dataset-service.jar")) {
    Write-Host "==> Packaging dataset-service.jar..."
    mvn clean package -DskipTests -B
}

# Start background process
$process = Start-Process java -ArgumentList "-jar", "target\dataset-service.jar" -PassThru -NoNewWindow

try {
    Write-Host "==> Waiting for application to report UP on http://localhost:8080/actuator/health..."
    $maxAttempts = 30
    $healthUrl = "http://localhost:8080/actuator/health"
    $isUp = $false

    for ($i = 1; $i -le $maxAttempts; $i++) {
        try {
            $resp = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($resp -and $resp.status -eq "UP") {
                Write-Host "==> Application is UP and healthy (attempt $i)."
                $isUp = $true
                break
            }
        } catch {
            # continue waiting
        }
        Start-Sleep -Seconds 2
    }

    if (-not $isUp) {
        Write-Error "ERROR: Application failed to start or report UP within 60 seconds."
        exit 1
    }

    Write-Host ""
    Write-Host "==> 1. Inserting Record 1 (Alice - Engineering)..."
    $r1 = Invoke-RestMethod -Uri "http://localhost:8080/api/dataset/employees/record" -Method Post -ContentType "application/json" -Body '{"name":"Alice","age":30,"department":"Engineering"}'
    Write-Host ($r1 | ConvertTo-Json -Compress)

    Write-Host "==> 2. Inserting Record 2 (Bob - Marketing)..."
    $r2 = Invoke-RestMethod -Uri "http://localhost:8080/api/dataset/employees/record" -Method Post -ContentType "application/json" -Body '{"name":"Bob","age":25,"department":"Marketing"}'
    Write-Host ($r2 | ConvertTo-Json -Compress)

    Write-Host "==> 3. Inserting Record 3 (Carol - Engineering)..."
    $r3 = Invoke-RestMethod -Uri "http://localhost:8080/api/dataset/employees/record" -Method Post -ContentType "application/json" -Body '{"name":"Carol","age":35,"department":"Engineering"}'
    Write-Host ($r3 | ConvertTo-Json -Compress)

    Write-Host ""
    Write-Host "==> 4. Querying GroupBy department..."
    $grouped = Invoke-RestMethod -Uri "http://localhost:8080/api/dataset/employees/query?groupBy=department" -Method Get
    Write-Host ($grouped | ConvertTo-Json -Depth 4)

    Write-Host ""
    Write-Host "==> 5. Querying SortBy age ascending..."
    $sorted = Invoke-RestMethod -Uri "http://localhost:8080/api/dataset/employees/query?sortBy=age&order=asc" -Method Get
    Write-Host ($sorted | ConvertTo-Json -Depth 4)

    Write-Host ""
    Write-Host "==> Verification completed successfully against real PostgreSQL!"
}
finally {
    Write-Host ""
    Write-Host "==> Stopping application process (PID $($process.Id))..."
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Write-Host "==> Verification script finished."
}
