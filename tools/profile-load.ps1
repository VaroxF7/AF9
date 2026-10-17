<#
.SYNOPSIS  Samples the game's JVM while a world loads, to show where the minutes go (GC pressure or a hot method).

.DESCRIPTION
  Start this BEFORE launching the game (or while it starts). It waits for the javaw whose command line holds the
  instance folder, then every few seconds writes
    - one `jstat -gcutil` line  (heap occupancy and cumulative GC time: a GC-bound load shows O/FGC/GCT climbing), and
    - the top frames of the "Render thread" (`jstack`), which runs the recipe reload on the client's first world load,
  until latest.log reports "Initial datapack load took" (or -MaxMinutes pass). It only reads from the JVM; nothing is changed.
  Needs a JDK 17+ with jstat/jstack (Adoptium 17 works against the launcher's Java 17 runtime).

.EXAMPLE   pwsh tools/profile-load.ps1 -Instance "$env:USERPROFILE\curseforge\minecraft\Instances\All the Mods 9 - ATM9"
#>
param(
    [Parameter(Mandatory)] [string] $Instance,
    [string] $JdkBin = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin',
    [int] $IntervalSeconds = 5,
    [int] $MaxMinutes = 12,
    [string] $Out = (Join-Path $Instance ("logs\af9_load_profile_{0}.txt" -f (Get-Date -Format 'yyyyMMdd_HHmm')))
)
$ErrorActionPreference = 'Stop'
$jstat = Join-Path $JdkBin 'jstat.exe'; $jstack = Join-Path $JdkBin 'jstack.exe'
foreach ($t in $jstat, $jstack) { if (-not (Test-Path $t)) { throw "$t not found: pass -JdkBin" } }
$log = Join-Path $Instance 'logs\latest.log'
$needle = [regex]::Escape($Instance)

Write-Host "Waiting for the game ($Instance) ..."
$proc = $null
while (-not $proc) {
    $proc = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' or Name='java.exe'" |
        Where-Object { $_.CommandLine -match $needle -and $_.CommandLine -match 'bootstraplauncher' } | Select-Object -First 1
    if (-not $proc) { Start-Sleep -Seconds 2 }
}
$procId = $proc.ProcessId
Write-Host "Sampling pid $procId every ${IntervalSeconds}s into $Out"
$start = Get-Date
$frames = @{}
function Write-Out([string] $s) { Add-Content -Path $Out -Value $s -Encoding utf8 }
Write-Out "# pid $procId started sampling $start"
Write-Out "# jstat columns: S0 S1 E O M CCS YGC YGCT FGC FGCT CGC CGCT GCT (O = old gen %, GCT = total GC seconds)"

while (((Get-Date) - $start).TotalMinutes -lt $MaxMinutes) {
    if (-not (Get-Process -Id $procId -ErrorAction SilentlyContinue)) { break }
    $stamp = (Get-Date).ToString('HH:mm:ss')
    try { Write-Out ("[{0}] gc  {1}" -f $stamp, ((& $jstat -gcutil $procId 2>$null | Select-Object -Last 1) -join ' ')) } catch {}
    try {
        $dump = & $jstack $procId 2>$null
        $text = $dump -join "`n"
        $m = [regex]::Match($text, '"Render thread".*?\n\n', 'Singleline')
        if ($m.Success) {
            $top = ($m.Value -split "`n" | Where-Object { $_ -match '^\s+at ' } | Select-Object -First 12)
            Write-Out ("[{0}] Render thread:" -f $stamp)
            $top | ForEach-Object { Write-Out $_ }
            if ($top) { $k = ($top | Select-Object -First 3) -join ' | '; $frames[$k] = 1 + [int]$frames[$k] }
        }
    } catch {}
    if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'Initial datapack load took' -Quiet)) { break }
    Start-Sleep -Seconds $IntervalSeconds
}
Write-Out ""
Write-Out "# Most frequent top-3-frame stacks of the Render thread (count = samples):"
$frames.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 15 | ForEach-Object { Write-Out ("{0,4}  {1}" -f $_.Value, $_.Key) }
Write-Host "Done: $Out"
