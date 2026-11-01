<#
 Copies a set of open-licensed performance mods from the GregTech Odyssey instance into an ATM9 instance
 (use a COPY of the instance first), or removes them again with -Remove.
 The jars are not stored in this repo; they are read from the GTO instance's mods folder.
 Close the game before running. Example:
   .\install-perf-mods.ps1 -Target "C:\Users\Arnik\curseforge\minecraft\Instances\ATM9 perf test"
   .\install-perf-mods.ps1 -Target "...\ATM9 perf test" -Remove
#>
param(
  [Parameter(Mandatory=$true)][string]$Target,
  [string]$Source = "C:\Users\Arnik\curseforge\minecraft\Instances\GregTech Odyssey",
  [switch]$Remove
)
$ErrorActionPreference = 'Stop'
# name prefix -> licence / purpose
$mods = [ordered]@{
  'radium-mc1.20.1'        = 'LGPL-3.0, Lithium port: entity, collision, AI, block ticks'
  'alternate_current'      = 'MIT, redstone'
  'fastnbt'                = 'LGPL-3.0, NBT speed'
  'leanobject'             = 'LGPL-3.0, memory footprint'
  'fastrecipesearch'       = 'LGPL-3.0, vanilla recipe search'
  'gnetum'                 = 'LGPL-3.0, spreads HUD updates over frames'
  'ImmediatelyFast'        = 'LGPL-3.0, immediate-mode rendering'
  'AsyncParticles'         = 'LGPL-3.0, particles off the render thread'
  'flerovium'              = 'LGPL-3.0, Embeddium add-on'
  'KryptonReforged'        = 'LGPL, networking'
  'fastspawner'            = 'MIT, spawner ticks'
}
$tmods = Join-Path $Target 'mods'
if (-not (Test-Path $tmods)) { throw "No mods folder in $Target" }
$record = Join-Path $Target 'af9_perf_mods.txt'
if ($Remove) {
  if (-not (Test-Path $record)) { throw "Nothing recorded to remove ($record missing)" }
  Get-Content $record | ForEach-Object { $p = Join-Path $tmods $_; if (Test-Path $p) { Remove-Item $p; "removed $_" } }
  Remove-Item $record; return
}
$added = @()
foreach ($k in $mods.Keys) {
  $existing = Get-ChildItem $tmods -Filter '*.jar' | Where-Object { $_.Name -like "$k*" }
  if ($existing) { "skip $k (already present: $($existing[0].Name))"; continue }
  $src = Get-ChildItem (Join-Path $Source 'mods') -Filter '*.jar' | Where-Object { $_.Name -like "$k*" } | Select-Object -First 1
  if (-not $src) { "MISSING in source: $k"; continue }
  Copy-Item $src.FullName $tmods
  $added += $src.Name
  "added $($src.Name)  [$($mods[$k])]"
}
if ($added) { Add-Content $record $added }
"Done. Start the instance once, then compare spark profiles (see docs/perf-mods.md). Undo with -Remove."
