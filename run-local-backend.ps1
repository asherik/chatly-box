$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path

function Get-JavaMajorVersion {
  param([string] $JavaExe)

  try {
    $versionOutput = & $JavaExe -version 2>&1
    $line = ($versionOutput | Select-Object -First 1).ToString()
    if ($line -match '"(?<version>[0-9]+)(\.|")') {
      return [int]$Matches.version
    }
  } catch {
    return $null
  }

  return $null
}

function Use-JavaHome {
  param([string] $JavaHome)

  $javaExe = Join-Path $JavaHome "bin\java.exe"
  if (-not (Test-Path -LiteralPath $javaExe)) {
    return $false
  }

  $major = Get-JavaMajorVersion $javaExe
  if ($major -ne $null -and $major -ge 22) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
    return $true
  }

  return $false
}

$javaReady = $false

if ($env:JAVA_HOME) {
  $javaReady = Use-JavaHome $env:JAVA_HOME
}

if (-not $javaReady) {
  $javaCommand = Get-Command java -ErrorAction SilentlyContinue
  if ($javaCommand) {
    $major = Get-JavaMajorVersion $javaCommand.Source
    $javaReady = $major -ne $null -and $major -ge 22
  }
}

if (-not $javaReady) {
  $candidateRoots = @(
    "$env:USERPROFILE\.jdks",
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Amazon Corretto"
  )

  foreach ($root in $candidateRoots) {
    if (-not (Test-Path -LiteralPath $root)) {
      continue
    }

    $candidates = Get-ChildItem -LiteralPath $root -Directory -Recurse -Depth 3 -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -match '(jdk|corretto|temurin).*(22|23|24|25|26)' }

    foreach ($candidate in $candidates) {
      if (Use-JavaHome $candidate.FullName) {
        $javaReady = $true
        break
      }
    }

    if ($javaReady) {
      break
    }
  }
}

if (-not $javaReady) {
  throw "Java 22+ not found. Install JDK 22+ or set JAVA_HOME before running this script."
}

if (-not (Test-Path -LiteralPath "$repo\llamalib\target\release\chatly_llamalib.dll")) {
  Push-Location "$repo\llamalib"
  cargo build --release
  Pop-Location
}

Push-Location "$repo"
.\gradlew.bat :backend:bootRun
Pop-Location
