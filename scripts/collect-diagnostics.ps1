$ErrorActionPreference = "Continue"
$OutputDirectory = Join-Path $PSScriptRoot "..\target\stage11-diagnostics"
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$Report = Join-Path $OutputDirectory "environment.txt"

"SAOIM Stage 11 diagnostics" | Set-Content $Report
"Generated: $(Get-Date -Format o)" | Add-Content $Report
"" | Add-Content $Report
"=== Windows ===" | Add-Content $Report
Get-ComputerInfo -Property WindowsProductName,WindowsVersion,OsBuildNumber,OsArchitecture 2>&1 | Out-String | Add-Content $Report
"=== Java ===" | Add-Content $Report
java -version 2>&1 | Out-String | Add-Content $Report
"=== Maven ===" | Add-Content $Report
mvn -version 2>&1 | Out-String | Add-Content $Report
"=== Video controllers ===" | Add-Content $Report
Get-CimInstance Win32_VideoController 2>&1 |
    Select-Object Name,DriverVersion,CurrentHorizontalResolution,CurrentVerticalResolution |
    Format-Table -AutoSize | Out-String | Add-Content $Report
"=== Screens ===" | Add-Content $Report
Add-Type -AssemblyName System.Windows.Forms
[System.Windows.Forms.Screen]::AllScreens 2>&1 |
    Select-Object DeviceName,Primary,Bounds,WorkingArea |
    Format-Table -AutoSize | Out-String | Add-Content $Report

Write-Host "Diagnostics saved to $Report" -ForegroundColor Green
Write-Host "The script does not copy .env or database passwords." -ForegroundColor Yellow
