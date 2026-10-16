<#
.SYNOPSIS  Copies the repo's kubejs/ and config/ into a game instance without ever wiping quest edits made in game.

.DESCRIPTION
  FTB Quests rewrites its .snbt files whenever it saves (own formatting, order_index shifts), and the player edits
  quests in game. A plain copy from the repo would silently throw all of that away. For config/ftbquests this script
  keeps a state file in the instance (af9_sync_state.json: the canonical hash of every quest file as last synced) and decides per file:
    - the instance file is unchanged since the last sync (formatting aside): overwritten with the repo's (backed up first)
    - edited in game, the repo unchanged since: NOT overwritten; the instance file is copied INTO the repo to be committed
    - edited in game AND changed in the repo: left alone and reported as a conflict, to merge by hand
  Everything else (kubejs, other config) is copied when it differs, the replaced file backed up first.
  Close the game before running: it saves its quests on exit and would undo the copy.

.EXAMPLE   pwsh tools/sync-instance.ps1 -Instance "$env:USERPROFILE\curseforge\minecraft\Instances\All the Mods 9 - ATM9"
#>
param(
    [Parameter(Mandatory)] [string] $Instance,
    [string] $Repo,
    [string] $Tag = (Get-Date -Format 'yyyyMMdd_HHmm')
)
$ErrorActionPreference = 'Stop'
if (-not $Repo) { $Repo = Split-Path (Split-Path $MyInvocation.MyCommand.Path -Parent) -Parent }
$backup = Join-Path $Instance "af9_sync_backup_$Tag"
$statePath = Join-Path $Instance 'af9_sync_state.json'
$state = @{}
if (Test-Path $statePath) { (Get-Content $statePath -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $state[$_.Name] = $_.Value } }

# a quest file as the game would not care about: no BOM, no whitespace, no order_index
function Get-Canon([string] $path) {
    $text = [IO.File]::ReadAllText($path) -replace '^\uFEFF', ''
    $lines = $text -split "`n" | Where-Object { $_ -notmatch '^\s*order_index:' }
    $canon = ($lines -join '') -replace '\s+', ''
    $sha = [Security.Cryptography.SHA256]::Create()
    [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($canon))).Replace('-', '')
}
function Backup-File([string] $rel) {
    $dst = Join-Path $backup $rel
    New-Item -ItemType Directory -Force (Split-Path $dst) | Out-Null
    Copy-Item (Join-Path $Instance $rel) $dst
}

$copied = 0; $kept = @(); $adopted = @(); $conflicts = @()
foreach ($dir in 'kubejs', 'config') {
    foreach ($file in Get-ChildItem (Join-Path $Repo $dir) -Recurse -File) {
        $rel = $file.FullName.Substring($Repo.Length + 1)
        $target = Join-Path $Instance $rel
        $isQuest = $rel -like 'config\ftbquests\*' -and $file.Extension -eq '.snbt'
        if (-not (Test-Path $target)) {
            New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
            Copy-Item $file.FullName $target; $copied++
            if ($isQuest) { $state[$rel] = Get-Canon $file.FullName }
            continue
        }
        if ((Get-FileHash $file.FullName).Hash -eq (Get-FileHash $target).Hash) {
            if ($isQuest) { $state[$rel] = Get-Canon $file.FullName }
            continue
        }
        if (-not $isQuest) { Backup-File $rel; Copy-Item $file.FullName $target -Force; $copied++; continue }

        $repoCanon = Get-Canon $file.FullName
        $instCanon = Get-Canon $target
        if ($repoCanon -eq $instCanon) { $kept += $rel; $state[$rel] = $repoCanon; continue }   # game formatting only: keep the game's file
        $synced = $state[$rel]
        $instanceEdited = $synced -and ($instCanon -ne $synced)
        $repoChanged = (-not $synced) -or ($repoCanon -ne $synced)
        if (-not $instanceEdited) {
            Backup-File $rel; Copy-Item $file.FullName $target -Force; $copied++; $state[$rel] = $repoCanon
        } elseif (-not $repoChanged) {
            Copy-Item $target $file.FullName -Force; $adopted += $rel; $state[$rel] = $instCanon
        } else {
            $conflicts += $rel
        }
    }
}
$state | ConvertTo-Json | Set-Content $statePath -Encoding utf8
"copied into the instance: $copied   (backup: $backup)"
if ($adopted) { "EDITED IN GAME, copied into the repo (commit them):"; $adopted | ForEach-Object { "  $_" } }
if ($conflicts) { "CONFLICT, edited in game and changed in the repo, left alone (merge by hand):"; $conflicts | ForEach-Object { "  $_" } }
if ($kept) { "quest files the game only reformatted, kept: $($kept.Count)" }