$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path

Push-Location "$repo\frontend"
if (-not (Test-Path -LiteralPath "node_modules")) {
  npm install
}
npm run db:push
npm run db:seed
npm run dev
Pop-Location
