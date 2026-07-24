$ErrorActionPreference = "Stop"

Write-Host "SAOIM Stage 11: MySQL integration smoke test" -ForegroundColor Cyan
Write-Host "The command uses DB_URL, DB_USER and DB_PASSWORD from .env." -ForegroundColor Yellow
mvn -B -ntp -P integration-tests verify
