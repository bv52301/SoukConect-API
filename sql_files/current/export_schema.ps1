Param(
  [string]$DbHost = $env:DB_HOST,
  [string]$DbPort = $env:DB_PORT,
  [string]$DbUser = $env:DB_USER,
  [string]$DbPass = $env:DB_PASS,
  [string]$DbName = $env:DB_NAME,
  [string]$DbSslMode = $env:DB_SSL_MODE,
  [string]$DbSslCa = $env:DB_SSL_CA,
  [string]$DbSslCert = $env:DB_SSL_CERT,
  [string]$DbSslKey = $env:DB_SSL_KEY
)

if (-not $DbHost -or -not $DbUser -or -not $DbName) {
  Write-Error "Usage: set DB_HOST, DB_USER, DB_PASS (optional), DB_NAME [DB_PORT]. Optional SSL: DB_SSL_MODE, DB_SSL_CA, DB_SSL_CERT, DB_SSL_KEY."
  exit 1
}

$DbPort = if ($DbPort) { $DbPort } else { 3306 }
$outDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Build-CommonArgs {
  param()
  $args = @("-h", $DbHost, "-P", $DbPort.ToString(), "-u", $DbUser)
  if ($DbPass) { $args += "-p$DbPass" }
  if ($DbSslMode) { $args += "--ssl-mode=$DbSslMode" }
  if ($DbSslCa)   { $args += "--ssl-ca=$DbSslCa" }
  if ($DbSslCert) { $args += "--ssl-cert=$DbSslCert" }
  if ($DbSslKey)  { $args += "--ssl-key=$DbSslKey" }
  return $args
}

$commonArgs = Build-CommonArgs

# Get tables
$tables = & mysql @commonArgs $DbName -N -e "SHOW TABLES;"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

foreach ($tbl in $tables) {
  if (-not $tbl) { continue }
  Write-Host "Exporting $tbl ..."
  & mysqldump @commonArgs --no-data --skip-comments --single-transaction $DbName $tbl | Out-File -FilePath (Join-Path $outDir "create_$tbl.sql") -Encoding UTF8
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Done. Files written to $outDir"
