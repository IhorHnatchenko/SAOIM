$ErrorActionPreference = "Stop"

Write-Host "SAOIM Stage 11: unit tests" -ForegroundColor Cyan
java -version
mvn -version
mvn -B -ntp clean verify

Write-Host "`nReports:" -ForegroundColor Green
Write-Host "  target/surefire-reports"
Write-Host "  target/site/jacoco/index.html"
