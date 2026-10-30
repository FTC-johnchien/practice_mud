param(
    [Parameter(Mandatory = $false)]
    [ValidateRange(0, 8)]
    [int]$Step = 0,

    [switch]$RecordSnapshot
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$SnapshotDir = Join-Path $ScriptDir ".gate_snapshots"
$Errors = New-Object System.Collections.Generic.List[string]

function Get-NormalizedSha256([string]$Text) {
    $norm = ($Text -replace "`r`n", "`n").TrimEnd("`n")
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($norm)
    return [System.BitConverter]::ToString([System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)).Replace("-", "")
}

function Get-FileNormalizedSha256([string]$FilePath) {
    $raw = Get-Content -Path $FilePath -Raw -Encoding UTF8
    if ($null -eq $raw) { $raw = "" }
    return Get-NormalizedSha256 $raw
}

function Get-ActiveLinesText([string]$Raw) {
    if ([string]::IsNullOrEmpty($Raw)) { return "" }
    $decomRegex = '(\u3010\u6B77\u53F2[\uFF0F\/]\u5DF2\u9664\u5F79\u3011|\u9664\u5F79)'
    $lines = $Raw -split "\r?\n"
    $active = $lines | Where-Object { $_ -notmatch $decomRegex }
    return ($active -join "`n")
}

function Test-NoFileUriLinks([string[]]$RelPaths, [string]$GateLabel) {
    foreach ($rp in $RelPaths) {
        $fp = Join-Path $script:RepoRoot $rp
        if (Test-Path $fp) {
            $content = Get-Content -Path $fp -Raw -Encoding UTF8
            if ($content -match '\]\(file:///') {
                Add-GateError "[$GateLabel] Hardcoded file:/// link remains in $rp (convert to relative path)"
            }
        }
    }
}

function Add-GateError([string]$Msg) {
    $script:Errors.Add($Msg)
    Write-Host "  [FAIL] $Msg" -ForegroundColor Red
}

function Pass-Note([string]$Msg) {
    Write-Host "  [PASS] $Msg" -ForegroundColor Green
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Instructor Gate Verifier - Target Gate: Step $Step" -ForegroundColor Cyan
Write-Host "RepoRoot: $RepoRoot" -ForegroundColor DarkCyan
Write-Host "=====================================================" -ForegroundColor Cyan

# Display signed-off progress from DOCS_ALIGNMENT_MASTER_PLAN.md
$masterPlanPath = Join-Path $RepoRoot "docs\3_task_lists\DOCS_ALIGNMENT_MASTER_PLAN.md"
if (Test-Path $masterPlanPath) {
    Write-Host "`n[Master Plan Progress]" -ForegroundColor Magenta
    $mpLines = Get-Content $masterPlanPath -Encoding UTF8 | Where-Object { $_ -match '^- \[[x ]\] \*\*(Gate \d+|\u6536\u5C3E\u5C01\u5B58)' }
    foreach ($ml in $mpLines) {
        if ($ml -match '^- \[x\]') { Write-Host "  $ml" -ForegroundColor Green }
        else { Write-Host "  $ml" -ForegroundColor Gray }
    }
}

# ---------------------------------------------------------
# Gate 0: Global Safety Red Lines & Immutability Locks
# ---------------------------------------------------------
Write-Host "`n[Gate 0] Checking global safety red lines and immutability locks..." -ForegroundColor Yellow

Push-Location $RepoRoot
try {
    $gitStatus = git status --porcelain -u
} finally {
    Pop-Location
}

$changedFiles = @()
foreach ($line in $gitStatus) {
    if ($line -match '^.. (.+)$') {
        $p = $Matches[1].Trim('"').Replace('\', '/')
        if ($p -match ' -> ') { $p = ($p -split ' -> ')[1] }
        $changedFiles += $p
    }
}

$governanceWhitelist = @(
    ".agents/skills/docs-alignment-instructor/SKILL.md",
    "docs/4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md",
    "docs/3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md",
    "scripts/verify-doc-alignment-gates.ps1",
    "docs/0_references/docs_hierarchy_review_report.md"
)

foreach ($f in $changedFiles) {
    if ($f.StartsWith("src/")) {
        Add-GateError "[REDLINE] Forbidden modification in src/: $f"
    }
    if ($f.StartsWith("docs/0_archive/") -or ($f.StartsWith("docs/0_references/") -and ($f -notin $governanceWhitelist))) {
        Add-GateError "[REDLINE] Forbidden modification in archive/references: $f"
    }
}
if (($changedFiles | Where-Object { $_.StartsWith("src/") -or $_.StartsWith("docs/0_archive/") -or ($_.StartsWith("docs/0_references/") -and ($_ -notin $governanceWhitelist)) }).Count -eq 0) {
    Pass-Note "src/, 0_archive/, and existing 0_references/ have 0 forbidden changes"
}

$guardPath = Join-Path $RepoRoot ".agents\rules\documentation_alignment_guardrails.md"
$guardHash = Get-FileNormalizedSha256 $guardPath
if ($guardHash -ne "72BA889D4C5D7E40925C41827D129952F99CDA48068C951393A26F5FC8C8CE2B") {
    Add-GateError "[REDLINE] .agents/rules/documentation_alignment_guardrails.md was modified (SHA256=$guardHash)!"
} else {
    Pass-Note "documentation_alignment_guardrails.md normalized SHA-256 verified"
}

$geminiLines = Get-Content (Join-Path $RepoRoot "GEMINI.md") -Encoding UTF8
$startIdx = -1
$endIdx = -1
for ($i = 0; $i -lt $geminiLines.Count; $i++) {
    if ($geminiLines[$i] -match '\u5C08\u6848\u7B2C\u4E00\u7D05\u7DDA\uFF1A100% \u8CC7\u6599\u9A45\u52D5\u8207\u96F6\u786C\u7DE8\u78BC\u9435\u5F8B') { $startIdx = $i }
    if ($startIdx -ge 0 -and $geminiLines[$i] -match 'ArchitectureGuardrailsTest.*100% \u7DA0\u71C8') { $endIdx = $i; break }
}
if ($startIdx -lt 0 -or $endIdx -lt 0) {
    Add-GateError "[REDLINE] GEMINI.md Rule #7 immutability lock section missing!"
} else {
    $redlineText = ($geminiLines[$startIdx..$endIdx]) -join "`n"
    $sha = Get-NormalizedSha256 $redlineText
    if ($sha -ne "A8E94EE97003F8A1148B0EEFB73C232B12061C5A9D0F69E7AB5042624371CBD4") {
        Add-GateError "[REDLINE] GEMINI.md Rule #7 immutability lock modified (SHA256=$sha)!"
    } else {
        Pass-Note "GEMINI.md Rule #7 immutability lock SHA-256 verified"
    }
}

$mdDocs = Get-ChildItem -Path (Join-Path $RepoRoot "docs") -Filter "*.md" -Recurse | Where-Object { $_.FullName -notmatch "0_archive|0_references" }
$over400 = @()
foreach ($doc in $mdDocs) {
    $lc = (Get-Content $doc.FullName -Encoding UTF8).Count
    if ($lc -gt 400) { $over400 += "$($doc.Name) ($lc lines)" }
    if ($doc.Name -like "DOCS_ALIGNMENT_*.md" -and $lc -gt 350) { $over400 += "$($doc.Name) ($lc lines > 350)" }
}
if ($over400.Count -gt 0) {
    Add-GateError "[LINE_LIMIT] Files exceeding line limit: $($over400 -join ', ')"
} else {
    Pass-Note "All non-archive docs are within line limits (<= 400, alignment docs <= 350)"
}

if ($Step -eq 0) {
    if ($Errors.Count -eq 0) { Write-Host "`n[OK] Pre-Flight Gate 0 PASSED!" -ForegroundColor Green; exit 0 }
    else { exit 1 }
}

# ---------------------------------------------------------
# Order Check: Cumulative Step Whitelist
# ---------------------------------------------------------
Write-Host "`n[Order Check] Verifying step-by-step file whitelist up to Step $Step..." -ForegroundColor Yellow

function Test-AllowedForStep([string]$RelPath, [int]$MaxStep) {
    if ($RelPath -in $script:governanceWhitelist) { return $true }
    if ($RelPath.StartsWith("scripts/.gate_snapshots/")) { return $true }
    if ($MaxStep -ge 1 -and $RelPath -eq "scripts/check-doc-links.ps1") { return $true }
    if ($MaxStep -ge 2 -and ($RelPath -eq "docs/README.md" -or $RelPath.StartsWith("docs/1_requirements/"))) { return $true }
    if ($MaxStep -ge 3 -and ($RelPath.StartsWith("docs/2_specifications/data_schemas/") -or $RelPath.StartsWith("docs/2_specifications/skills/"))) { return $true }
    if ($MaxStep -ge 4 -and ($RelPath -in @("docs/2_specifications/ARCHITECTURE_SUMMARY.md", "docs/2_specifications/MODULES_MAP.md") -or $RelPath.StartsWith("docs/2_specifications/backend/"))) { return $true }
    if ($MaxStep -ge 5 -and $RelPath.StartsWith("docs/2_specifications/frontend/")) { return $true }
    if ($MaxStep -ge 6 -and $RelPath.StartsWith("docs/3_task_lists/")) { return $true }
    if ($MaxStep -ge 7 -and ($RelPath.StartsWith("docs/4_active_tasks/") -or $RelPath -in @("GEMINI.md", "CHANGELOG.md"))) { return $true }
    return $false
}

function Get-StepTargetFiles([int]$TargetStep) {
    $targets = @()
    switch ($TargetStep) {
        1 { $targets += "scripts/check-doc-links.ps1" }
        2 {
            $targets += "docs/README.md"
            Get-ChildItem (Join-Path $RepoRoot "docs\1_requirements") -Filter "*.md" | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
        }
        3 {
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\data_schemas") -Filter "*.md" | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\skills") -Filter "*.md" -Recurse | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
        }
        4 {
            $targets += "docs/2_specifications/ARCHITECTURE_SUMMARY.md"
            $targets += "docs/2_specifications/MODULES_MAP.md"
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\backend") -Filter "*.md" | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
        }
        5 {
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\frontend") -Filter "*.md" | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
        }
        6 {
            Get-ChildItem (Join-Path $RepoRoot "docs\3_task_lists") -Filter "*.md" -Recurse | Where-Object {
                $_.Name -ne "DOCS_ALIGNMENT_MASTER_PLAN.md"
            } | ForEach-Object {
                $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
            }
        }
        7 {
            $targets += @("docs/4_active_tasks/SPRINT_GOAL.md", "docs/4_active_tasks/ACTIVE_TASKS.md", "docs/1_requirements/CYBERPUNK_CULTIVATION_SPEC.md", "GEMINI.md", "CHANGELOG.md")
        }
        8 {
            $targets += @("docs/2_specifications/data_schemas/ITEM_SCHEMA.md", "docs/2_specifications/skills/README.md", "docs/3_task_lists/README.md", "docs/3_task_lists/data_schemas/DUNGEON_SCHEMA_TASKS.md", "docs/3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md")
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\backend") -Filter "*.md" | ForEach-Object { $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/') }
            Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\frontend") -Filter "*.md" | ForEach-Object { $targets += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/') }
        }
    }
    return $targets
}

$outOfOrder = @()
foreach ($f in $changedFiles) {
    if (-not (Test-AllowedForStep -RelPath $f -MaxStep $Step)) {
        $outOfOrder += $f
    }
}
if ($outOfOrder.Count -gt 0) {
    Add-GateError "[OUT_OF_ORDER] Files modified beyond authorized Step ${Step}: $($outOfOrder -join ', ')"
} else {
    Pass-Note "Modified files strictly match Step 1..$Step whitelist"
}

for ($prev = 1; $prev -lt $Step; $prev++) {
    $snapFile = Join-Path $SnapshotDir "step_${prev}_snapshot.json"
    if (Test-Path $snapFile) {
        $snap = Get-Content $snapFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($prop in $snap.PSObject.Properties) {
            if ($Step -ge 7 -and $prev -eq 2 -and $prop.Name -eq "docs/1_requirements/CYBERPUNK_CULTIVATION_SPEC.md") { continue }
            if ($Step -eq 8 -and ($prop.Name -in (Get-StepTargetFiles -TargetStep 8))) {
                continue
            }
            $targetFull = Join-Path $RepoRoot $prop.Name
            if (Test-Path $targetFull) {
                $curHash = Get-FileNormalizedSha256 $targetFull
                if ($curHash -ne $prop.Value) {
                    Add-GateError "[REGRESSION] Previously signed-off Step $prev file changed: $($prop.Name)"
                }
            }
        }
    }
}

# ---------------------------------------------------------
# Gate 1 ~ Gate 7 Specific Checks
# ---------------------------------------------------------
Write-Host "`n[Gate $Step] Running specific gate assertions..." -ForegroundColor Yellow

switch ($Step) {
    1 {
        $scriptRaw = Get-Content (Join-Path $RepoRoot "scripts\check-doc-links.ps1") -Raw -Encoding UTF8
        if ($scriptRaw -notmatch 'GEMINI\.md') { Add-GateError "check-doc-links.ps1 does not scan root GEMINI.md" }
        if ($scriptRaw -notmatch 'ConvertFrom-Json') { Add-GateError "check-doc-links.ps1 missing ConvertFrom-Json validation" }
        if ($scriptRaw -notmatch '0_references') { Add-GateError "check-doc-links.ps1 does not exclude 0_references" }
        Push-Location $RepoRoot
        try {
            $null = powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1 2>&1 | Out-String
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        if ($code -eq 0) {
            Add-GateError "[TDD_FAIL] Upgraded check-doc-links.ps1 returned 0 before docs were fixed; must catch existing errors!"
        } else {
            Pass-Note "TDD red-light baseline verified (ExitCode=$code)"
        }
    }
    2 {
        $lore = Get-Content (Join-Path $RepoRoot "docs\1_requirements\WORLD_AND_LORE_BIBLE.md") -Raw -Encoding UTF8
        $loop = Get-Content (Join-Path $RepoRoot "docs\1_requirements\GAMEPLAY_CORE_LOOPS.md") -Raw -Encoding UTF8
        $uiux = Get-Content (Join-Path $RepoRoot "docs\1_requirements\UI_UX_REQUIREMENTS.md") -Raw -Encoding UTF8
        $readme = Get-Content (Join-Path $RepoRoot "docs\README.md") -Raw -Encoding UTF8

        if ($lore -notmatch '\u5F37\u6B96\u5E7E\u4E01\u8CEA|\u842C\u8C61\u6838\u5FC3') { Add-GateError "WORLD_AND_LORE_BIBLE.md missing D5 chitin/core-socket note" }
        if ($loop -match 'CombatRules|DrpgCombatLoop|ComboResolver|XpProgressionService') { Add-GateError "GAMEPLAY_CORE_LOOPS.md still contains hardcoded Java class names" }
        if ($loop -notmatch 'INT' -or $loop -notmatch 'WIS') { Add-GateError "GAMEPLAY_CORE_LOOPS.md missing INT / WIS attribute mapping" }
        if ($uiux -notmatch '\u5F85\u6DF1\u5165\u8A0E\u8AD6') { Add-GateError "UI_UX_REQUIREMENTS.md missing deferred discussion tag" }
        if ($uiux -match 'torch-darkness-vignette') { Add-GateError "UI_UX_REQUIREMENTS.md still contains .torch-darkness-vignette" }
        if ($readme -notmatch 'skills/' -or $readme -notmatch 'CODE_CONVERGENCE_AND_REFACTORING_TASKS\.md') {
            Add-GateError "docs/README.md missing skills/ or CODE_CONVERGENCE_AND_REFACTORING_TASKS.md"
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 2 L1 & docs/README.md checks passed" }
    }
    3 {
        $schemaFiles = Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\data_schemas\*.md")
        foreach ($sf in $schemaFiles) {
            $raw = Get-Content $sf.FullName -Raw -Encoding UTF8
            $blocks = [regex]::Matches($raw, '(?s)```json\r?\n(.*?)\r?\n```')
            $idx = 0
            foreach ($b in $blocks) {
                $idx++
                try { $null = $b.Groups[1].Value | ConvertFrom-Json }
                catch { Add-GateError "$($sf.Name) JSON block #$idx invalid: $($_.Exception.Message)" }
            }
        }
        $skillSchema = Get-Content (Join-Path $RepoRoot "docs\2_specifications\data_schemas\SKILL_SCHEMA.md") -Raw -Encoding UTF8
        if ($skillSchema -match '"tree_(overclock|chitin|water|fire|wood)"') {
            Add-GateError "SKILL_SCHEMA.md still contains shorthand treeId ($($Matches[0]))"
        }
        if ($skillSchema -match 'combo_2p_taiji_water_fire|domain_4p_elemental_cascade') {
            Add-GateError "SKILL_SCHEMA.md still contains legacy combo/domain IDs"
        }
        if ($skillSchema -notmatch 'tags') {
            Add-GateError "SKILL_SCHEMA.md missing comboRole vs tags mapping rule"
        }
        $treeFiles = Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\skills") -Filter "TREE_*.md" -Recurse
        if ($treeFiles.Count -ne 17) { Add-GateError "Unexpected TREE_*.md count: $($treeFiles.Count)" }
        foreach ($tf in $treeFiles) {
            $traw = Get-Content $tf.FullName -Raw -Encoding UTF8
            if ($traw -match '\|\s*\u9023\u651C\u89D2\u8272\s*\|') { Add-GateError "$($tf.Name) column 7 header not updated" }
            if ($traw -match '\u524D\u8A0E') { Add-GateError "$($tf.Name) still contains typo in header" }
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 3 Data Schemas & 17 Skill Trees checks passed" }
    }
    4 {
        $m1 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M1_TOWN_HUB_AND_FACILITIES.md") -Raw -Encoding UTF8
        $m2 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M2_DRPG_DUNGEON.md") -Raw -Encoding UTF8
        $m3 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M3_COMBAT_ENGINE.md") -Raw -Encoding UTF8
        $m31 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M3_1_BUFF_AND_STATUS_EFFECTS.md") -Raw -Encoding UTF8
        $m4 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M4_PARTY_AND_FORMATION.md") -Raw -Encoding UTF8
        $m5 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M5_SKILL_AND_PROGRESSION.md") -Raw -Encoding UTF8
        $m6 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M6_ITEM_AND_ECONOMY.md") -Raw -Encoding UTF8
        $m7 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M7_DATA_DRIVEN_TEMPLATES.md") -Raw -Encoding UTF8
        $m8 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M8_SAVE_AND_PERSISTENCE.md") -Raw -Encoding UTF8
        $m9 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M9_STATE_PIPELINE_AND_EVENTS.md") -Raw -Encoding UTF8
        $mmap = Get-Content (Join-Path $RepoRoot "docs\2_specifications\MODULES_MAP.md") -Raw -Encoding UTF8

        $m7Active = Get-ActiveLinesText $m7
        $mmapActive = Get-ActiveLinesText $mmap

        if ($m1 -notmatch '\u5B97\u9580\u5916\u9580\u96C6\u5E02') { Add-GateError "M1 missing 6th facility (market)" }
        if ($m1.Substring(0, [Math]::Min(900, $m1.Length)) -match '\u8CFD\u535A\u6676\u7247\u5BE6\u9A57\u5BA4') { Add-GateError "M1 header/Mermaid still contains old 5 cyber facility names" }
        if ($m2 -match '0\.15') { Add-GateError "M2 still contains float dangerRate 0.15" }
        if ($m3 -notmatch '25%.*33%|1/3' -or $m3 -notmatch '6.*8\s*\u79D2|7\s*\u79D2') { Add-GateError "M3 missing D3 poise reduction/duration alignment" }
        if ($m31 -notmatch 'BuffEffectHandler' -or $m31 -notmatch 'public interface BuffEffectHandler') {
            Add-GateError "M3_1 over-compressed section 9: missing public interface BuffEffectHandler contract"
        }
        if ($m4 -match '<=\s*25|\u795E\u8B58\u52D5\u6416') { Add-GateError "M4 still contains SAN <= 25 tier" }
        if ($m5 -notmatch 'tree_chitin_blade|\u5F37\u6B96\u5E7E\u4E01\u8CEA') { Add-GateError "M5 missing D5 chitin/core-socket alignment" }
        if ($m6 -match '\u5168\u670D\u6D88\u8017\u91CF') { Add-GateError "M6 still contains multiplayer phrasing" }
        if ($m6 -notmatch 'com\.example\.htmlmud\.town\.CognitiveProxyService') {
            Add-GateError "M6 section 5 representative classes table missing com.example.htmlmud.town.CognitiveProxyService"
        }
        if ($m7Active -match 'WatchService') { Add-GateError "M7 still contains active unconstitutional WatchService hot-reload" }
        if ($m8 -match 'slot_auto\.json' -or $m8 -notmatch 'autosave\.json') { Add-GateError "M8 auto-save filename not aligned to autosave.json" }
        if ($m9 -match '"TOWN_HUB"') { Add-GateError "M9 still contains viewMode TOWN_HUB" }
        if ($mmapActive -match '\u507D\u88DD\u5BF6\u7BB1\u602A|data/hub/facilities\.json|js/state/store\.js|action-bar-panel\.js') {
            Add-GateError "MODULES_MAP.md still contains active mimic or legacy paths ($($Matches[0]))"
        }
        if ($mmapActive -match '\x60store\.js\x60') {
            Add-GateError "MODULES_MAP.md section 3 FE-1 still lists store.js instead of state-store.js"
        }
        Test-NoFileUriLinks -RelPaths (Get-StepTargetFiles -TargetStep 4) -GateLabel "Gate4"
        if ($Errors.Count -eq 0) { Pass-Note "Step 4 Architecture & Backend M1~M9 checks passed" }
    }
    5 {
        $flg = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FRONTEND_LAYOUT_GUIDE.md") -Raw -Encoding UTF8
        $fe1 = Get-ActiveLinesText (Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE1_CORE_STATE_AND_PIPELINE.md") -Raw -Encoding UTF8)
        $fe3 = Get-ActiveLinesText (Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE3_BATTLE_ARENA_AND_FX.md") -Raw -Encoding UTF8)
        $fe4 = Get-ActiveLinesText (Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE4_TOWN_HUB_AND_ACTION_BAR.md") -Raw -Encoding UTF8)
        $fe6 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE6_CSS_TOKENS_AND_THEMES.md") -Raw -Encoding UTF8

        if ($flg -match '30\s*\u683C') { Add-GateError "FRONTEND_LAYOUT_GUIDE.md still says 30 slots instead of 40" }
        if ($flg -notmatch '\u73FE\u884C\u5DF2\u9A57\u8B49' -or $flg -notmatch '\u5F85\u6DF1\u5165\u8A0E\u8AD6') { Add-GateError "FRONTEND_LAYOUT_GUIDE.md missing D1 status tags" }
        if ($fe1 -match 'js/state/store\.js') { Add-GateError "FE1 still contains active js/state/store.js" }
        if ($fe3 -match 'js/helpers/buff-hud-helper\.js') { Add-GateError "FE3 still contains active js/helpers/buff-hud-helper.js" }
        if ($fe4 -match 'action-bar-panel\.js') { Add-GateError "FE4 still contains active action-bar-panel.js" }
        if ($fe6 -match 'exit-btn|\u56DB\u65B9\u51FA\u53E3\u7DB2\u683C\u6309\u9215') { Add-GateError "FE6 still contains .exit-btn" }
        if ($fe6 -notmatch 'style-07-battle-fx\.css') { Add-GateError "FE6 missing style-07-battle-fx.css" }
        Test-NoFileUriLinks -RelPaths (Get-StepTargetFiles -TargetStep 5) -GateLabel "Gate5"
        if ($Errors.Count -eq 0) { Pass-Note "Step 5 Frontend 8 manuals checks passed" }
    }
    6 {
        $m2t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M2_DRPG_DUNGEON_TASKS.md") -Raw -Encoding UTF8
        $m3t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M3_COMBAT_ENGINE_TASKS.md") -Raw -Encoding UTF8
        $m31t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M3_1_BUFF_AND_STATUS_TASKS.md") -Raw -Encoding UTF8
        $m5t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M5_SKILL_AND_PROGRESSION_TASKS.md") -Raw -Encoding UTF8
        $m7t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M7_DATA_DRIVEN_TEMPLATES_TASKS.md") -Raw -Encoding UTF8
        $m8t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M8_SAVE_AND_PERSISTENCE_TASKS.md") -Raw -Encoding UTF8
        $sst = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\data_schemas\SKILL_SCHEMA_TASKS.md") -Raw -Encoding UTF8
        $fe6t = Get-ActiveLinesText (Get-Content (Join-Path $RepoRoot "docs\3_task_lists\frontend\FE6_CSS_TOKENS_TASKS.md") -Raw -Encoding UTF8)

        if ($m2t -match '\u507D\u88DD\u5BF6\u7BB1\u602A|Task M2-2') { Add-GateError "M2_TASKS still contains Task M2-2" }
        if ($m3t -match 'COMBAT-ACTION-RELOAD|\u63DB\u5F48') { Add-GateError "M3_TASKS still contains Task M3-2" }
        if ($m3t -match '-\s*\[\s*\]\s*\*\*Task M3-1') { Add-GateError "M3_TASKS Task M3-1 not checked [x]" }
        if ($m3t -notmatch 'POISE-RANK-GATE|Task M3-3') { Add-GateError "M3_TASKS missing Task M3-3 (POISE-RANK-GATE)" }
        if ($m31t -notmatch 'valueFormula|\u905E\u6E1B|DR') { Add-GateError "M3_1_TASKS missing DR / valueFormula tasks" }
        if ($m5t -match '32\+\s*\u8CFD\u535A\u5929\u8CE6\u6A39') { Add-GateError "M5_TASKS still contains 32+ cyber talent tree task" }
        if ($m7t -match 'DATA-HOT-RELOAD|Task M7-1') { Add-GateError "M7_TASKS still contains Task M7-1" }
        if ($m8t -match '-\s*\[\s*\]\s*\*\*Task M8-2' -or $m8t -match 'slot_auto') { Add-GateError "M8_TASKS Task M8-2 not checked [x] or still has slot_auto" }
        if ($sst -match '-\s*\[\s*\]\s*\*\*Task SKILL-SCHEMA-1') { Add-GateError "SKILL_SCHEMA_TASKS Task SKILL-SCHEMA-1 not checked [x]" }
        if ($fe6t -match 'style-01-tokens\.css|style-03-dungeon-view\.css|style-06-modals-and-forms\.css') {
            Add-GateError "FE6_TASKS still contains non-existent CSS filenames"
        }
        Test-NoFileUriLinks -RelPaths (Get-StepTargetFiles -TargetStep 6) -GateLabel "Gate6"
        if ($Errors.Count -eq 0) { Pass-Note "Step 6 L3 22 task lists checks passed" }
    }
    7 {
        $sg = Get-Content (Join-Path $RepoRoot "docs\4_active_tasks\SPRINT_GOAL.md") -Raw -Encoding UTF8
        $gem = Get-Content (Join-Path $RepoRoot "GEMINI.md") -Raw -Encoding UTF8
        $chg = Get-Content (Join-Path $RepoRoot "CHANGELOG.md") -Raw -Encoding UTF8

        if ($sg -match '16\s*\u9805\u8CFD\u535A\u4FEE\u4ED9\u6280\u80FD|\u4E94\u500B\u554F\u984C|\u5FAE\u91CF\u5F37\u6B96\u57FA\u56E0\u69FD') {
            Add-GateError "SPRINT_GOAL.md still contains outdated terms ($($Matches[0]))"
        }
        if ($gem -match 'docs/3_task_lists/CHANGELOG\.md|docs/3_task_lists/future_improvements_history\.md') {
            Add-GateError "GEMINI.md still contains broken links"
        }
        if ($chg -notmatch '2026-10-29') {
            Add-GateError "CHANGELOG.md missing 2026-10-29 entry"
        }
        $allActiveRels = @("GEMINI.md")
        Get-ChildItem -Path (Join-Path $RepoRoot "docs") -Filter "*.md" -Recurse | Where-Object {
            $_.FullName -notmatch "0_archive|0_references"
        } | ForEach-Object {
            $allActiveRels += $_.FullName.Substring($RepoRoot.Length).TrimStart("\/").Replace('\', '/')
        }
        Test-NoFileUriLinks -RelPaths $allActiveRels -GateLabel "Gate7"
        Push-Location $RepoRoot
        try {
            $out = powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1 2>&1 | Out-String
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        Write-Host $out
        if ($code -ne 0) {
            Add-GateError "Final check-doc-links.ps1 failed (ExitCode=$code)!"
        } else {
            Pass-Note "Final check-doc-links.ps1 PASSED (ExitCode=0)"
        }
    }
    8 {
        $itemSchema = Get-Content (Join-Path $RepoRoot "docs\2_specifications\data_schemas\ITEM_SCHEMA.md") -Raw -Encoding UTF8
        $m6 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M6_ITEM_AND_ECONOMY.md") -Raw -Encoding UTF8
        $m7 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\backend\M7_DATA_DRIVEN_TEMPLATES.md") -Raw -Encoding UTF8
        $fe2 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE2_DRPG_VIEWPORT_RENDERER.md") -Raw -Encoding UTF8
        $fe5 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE5_PARTY_EQUIPMENT_AND_MODALS.md") -Raw -Encoding UTF8
        $skillsReadme = Get-Content (Join-Path $RepoRoot "docs\2_specifications\skills\README.md") -Raw -Encoding UTF8
        $l3Readme = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\README.md") -Raw -Encoding UTF8
        $dst = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\data_schemas\DUNGEON_SCHEMA_TASKS.md") -Raw -Encoding UTF8

        if ($itemSchema -notmatch 'equipSlot' -or $itemSchema -match '"minProgress"\s*:\s*35') { Add-GateError "ITEM_SCHEMA.md missing equipSlot contract note or still has minProgress: 35" }
        if ($m6 -match '"minProgress"\s*:\s*35' -or $m6 -match 'filterItem') { Add-GateError "M6_ITEM_AND_ECONOMY.md still has minProgress: 35 or filterItem" }
        if ($fe5 -match 'EQUIP_ITEM\(') { Add-GateError "FE5_PARTY_EQUIPMENT_AND_MODALS.md still contains EQUIP_ITEM(" }
        if ($fe2 -match '\u767C\u9001\u6307\u4EE4\s*`(step|turn)') { Add-GateError "FE2_DRPG_VIEWPORT_RENDERER.md still describes sending raw step/turn string commands" }
        if ($m7 -match 'Draft 2020-12' -or $m7 -notmatch 'draft-07') { Add-GateError "M7_DATA_DRIVEN_TEMPLATES.md not aligned to draft-07" }
        if ($skillsReadme -notmatch 'VS-01') { Add-GateError "docs/2_specifications/skills/README.md missing VS-01 phased scope note" }
        if ($dst -notmatch 'Task M2-1') { Add-GateError "DUNGEON_SCHEMA_TASKS.md missing Task M2-1 cross-reference note" }
        if ($l3Readme -notmatch '2026-10-29') { Add-GateError "docs/3_task_lists/README.md missing test baseline date snapshot" }
        Push-Location $RepoRoot
        try {
            $out = powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1 2>&1 | Out-String
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        Write-Host $out
        if ($code -ne 0) {
            Add-GateError "Step 8 check-doc-links.ps1 failed (ExitCode=$code)!"
        } else {
            Pass-Note "Step 8 check-doc-links.ps1 PASSED (ExitCode=0)"
        }
    }
}

if ($Errors.Count -gt 0) {
    Write-Host "`n[REJECTED] Gate $Step FAILED ($($Errors.Count) issue(s)). Return to gemini 3.8 for fixes." -ForegroundColor Red
    exit 1
} else {
    if ($RecordSnapshot -and $Step -ge 1) {
        if (-not (Test-Path $SnapshotDir)) {
            New-Item -ItemType Directory -Path $SnapshotDir -Force | Out-Null
        }
        $snapMap = [ordered]@{}
        $stepTargets = Get-StepTargetFiles -TargetStep $Step
        foreach ($rel in $stepTargets) {
            $full = Join-Path $RepoRoot $rel
            if (Test-Path $full) {
                $snapMap[$rel] = Get-FileNormalizedSha256 $full
            }
        }
        $snapPath = Join-Path $SnapshotDir "step_${Step}_snapshot.json"
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($snapPath, ($snapMap | ConvertTo-Json), $utf8NoBom)
        Pass-Note "Recorded Step $Step normalized hash snapshot -> scripts/.gate_snapshots/step_${Step}_snapshot.json"
    }
    Write-Host "`n[APPROVED] Gate $Step PASSED 100%!" -ForegroundColor Green
    exit 0
}
