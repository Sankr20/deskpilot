$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$props = Join-Path $root "deskpilot.properties"

if (!(Test-Path $props)) { Write-Error "deskpilot.properties not found: $props"; exit 2 }

$dpVer = (Get-Content $props | Where-Object { $_ -like "deskpilot.version=*" } | Select-Object -First 1) -replace "deskpilot.version=",""
if ([string]::IsNullOrWhiteSpace($dpVer)) { Write-Error "deskpilot.version missing in deskpilot.properties"; exit 2 }

$cache = Join-Path $root ".deskpilot\cli\$dpVer"
$jar   = Join-Path $cache "deskpilot.jar"
New-Item -ItemType Directory -Force -Path $cache | Out-Null

if (!(Test-Path $jar)) { Write-Error "CLI jar not found: $jar"; exit 2 }

& java -jar $jar @args
exit $LASTEXITCODE
