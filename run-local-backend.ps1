$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = "C:\Users\asher\.jdks\corretto-22.0.2"

if (Test-Path -LiteralPath $javaHome) {
  $env:JAVA_HOME = $javaHome
  $env:Path = "$javaHome\bin;$env:Path"
}

if (-not (Test-Path -LiteralPath "$repo\llamalib\target\release\chatly_llamalib.dll")) {
  Push-Location "$repo\llamalib"
  cargo build --release
  Pop-Location
}

Push-Location "$repo"
.\gradlew.bat :backend:bootRun
Pop-Location
