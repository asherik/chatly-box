$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$defaultLibclangPath = "C:\Program Files\LLVM\bin"
$candidatePaths = @(
    $env:LIBCLANG_PATH,
    $defaultLibclangPath,
    "C:\Program Files (x86)\LLVM\bin"
) | Where-Object { $_ -and $_.Trim().Length -gt 0 }

$libclangDir = $null
foreach ($candidate in $candidatePaths) {
    if (Test-Path (Join-Path $candidate "libclang.dll")) {
        $libclangDir = $candidate
        break
    }
    if (Test-Path (Join-Path $candidate "clang.dll")) {
        $libclangDir = $candidate
        break
    }
}

if (-not $libclangDir) {
    Write-Host "libclang.dll was not found. llama-cpp-sys-2 uses bindgen, so local LLVM is required." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install LLVM once:" -ForegroundColor Yellow
    Write-Host "  winget install LLVM.LLVM"
    Write-Host ""
    Write-Host "Then open a new PowerShell and run:" -ForegroundColor Yellow
    Write-Host "  cd $root"
    Write-Host "  .\build-local.ps1"
    Write-Host ""
    Write-Host "Expected file after install: $($defaultLibclangPath)\libclang.dll"
    exit 1
}

$env:LIBCLANG_PATH = $libclangDir
Write-Host "LIBCLANG_PATH=$env:LIBCLANG_PATH"

Push-Location $root
try {
    cargo build --release
}
finally {
    Pop-Location
}
