param(
    [Parameter(Mandatory = $false)]
    [ValidateRange(0, 7)]
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

# 顯示目前 DOCS_ALIGNMENT_MASTER_PLAN.md 中的已簽核進度（方便跨電腦接手一眼看清）
$masterPlanPath = Join-Path $RepoRoot "docs\3_task_lists\DOCS_ALIGNMENT_MASTER_PLAN.md"
if (Test-Path $masterPlanPath) {
    Write-Host "`n[Master Plan Progress]" -ForegroundColor Magenta
    $mpLines = Get-Content $masterPlanPath -Encoding UTF8 | Where-Object { $_ -match '^- \[[x ]\] \*\*(Gate \d+|收尾封存)' }
    foreach ($ml in $mpLines) {
        if ($ml -match '^- \[x\]') { Write-Host "  $ml" -ForegroundColor Green }
        else { Write-Host "  $ml" -ForegroundColor Gray }
    }
}

# ---------------------------------------------------------
# Gate 0: 全域安全紅線護欄 (每次檢核必跑)
# ---------------------------------------------------------
Write-Host "`n[Gate 0] 檢查全域安全紅線與不可篡改保護鎖..." -ForegroundColor Yellow

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
    "docs/4_active_tasks/DOCS_ALIGNMENT_ACTIVE_TASKS.md",
    "docs/3_task_lists/DOCS_ALIGNMENT_MASTER_PLAN.md",
    "scripts/verify-doc-alignment-gates.ps1",
    "docs/0_references/docs_hierarchy_review_report.md"
)

foreach ($f in $changedFiles) {
    if ($f.StartsWith("src/")) {
        Add-GateError "[紅線違規] 嚴禁修改業務程式碼或靜態資料: $f"
    }
    if ($f.StartsWith("docs/0_archive/") -or ($f.StartsWith("docs/0_references/") -and ($f -notin $governanceWhitelist))) {
        Add-GateError "[紅線違規] 嚴禁修改封存庫或既有外部參考文件: $f"
    }
}
if (($changedFiles | Where-Object { $_.StartsWith("src/") -or $_.StartsWith("docs/0_archive/") -or ($_.StartsWith("docs/0_references/") -and ($_ -notin $governanceWhitelist)) }).Count -eq 0) {
    Pass-Note "src/、0_archive/、既有 0_references/ 維持 0 違規變更"
}

# 0.2 驗證 guardrails LF 正規化 SHA-256 與 GEMINI.md 第 7 條保護鎖
$guardPath = Join-Path $RepoRoot ".agents\rules\documentation_alignment_guardrails.md"
$guardHash = Get-FileNormalizedSha256 $guardPath
if ($guardHash -ne "72BA889D4C5D7E40925C41827D129952F99CDA48068C951393A26F5FC8C8CE2B") {
    Add-GateError "[紅線違規] .agents/rules/documentation_alignment_guardrails.md 遭到竄改 (SHA256=$guardHash)！"
} else {
    Pass-Note "documentation_alignment_guardrails.md SHA-256 跨平台校驗一致"
}

$geminiLines = Get-Content (Join-Path $RepoRoot "GEMINI.md") -Encoding UTF8
$startIdx = -1
$endIdx = -1
for ($i = 0; $i -lt $geminiLines.Count; $i++) {
    if ($geminiLines[$i] -match '專案第一紅線：100% 資料驅動與零硬編碼鐵律') { $startIdx = $i }
    if ($startIdx -ge 0 -and $geminiLines[$i] -match 'ArchitectureGuardrailsTest.*100% 綠燈') { $endIdx = $i; break }
}
if ($startIdx -lt 0 -or $endIdx -lt 0) {
    Add-GateError "[紅線違規] GEMINI.md 第 7 條保護鎖起訖區段遺失！"
} else {
    $redlineText = ($geminiLines[$startIdx..$endIdx]) -join "`n"
    $sha = Get-NormalizedSha256 $redlineText
    if ($sha -ne "A8E94EE97003F8A1148B0EEFB73C232B12061C5A9D0F69E7AB5042624371CBD4") {
        Add-GateError "[紅線違規] GEMINI.md 第 7 條保護鎖內文遭到修改 (SHA256=$sha)！"
    } else {
        Pass-Note "GEMINI.md 第 7 條不可篡改保護鎖 SHA-256 逐字一致"
    }
}

# 0.3 單檔行數紅線檢查 (一般與豁免上限 <= 400 行；治理專項檔須 <= 350 行)
$mdDocs = Get-ChildItem -Path (Join-Path $RepoRoot "docs") -Filter "*.md" -Recurse | Where-Object { $_.FullName -notmatch "0_archive|0_references" }
$over400 = @()
foreach ($doc in $mdDocs) {
    $lc = (Get-Content $doc.FullName -Encoding UTF8).Count
    if ($lc -gt 400) { $over400 += "$($doc.Name) ($lc 行)" }
    if ($doc.Name -like "DOCS_ALIGNMENT_*.md" -and $lc -gt 350) { $over400 += "$($doc.Name) ($lc 行 > 350)" }
}
if ($over400.Count -gt 0) {
    Add-GateError "[行數超標] 以下文件超過行數上限: $($over400 -join ', ')"
} else {
    Pass-Note "全部非封存文件行數皆合規 (<= 400 行，專項治理檔 <= 350 行)"
}

if ($Step -eq 0) {
    if ($Errors.Count -eq 0) { Write-Host "`n[OK] Pre-Flight Gate 0 通過！可依進度總表派發下一關 Step。" -ForegroundColor Green; exit 0 }
    else { exit 1 }
}

# ---------------------------------------------------------
# 順序防跳步檢查 (Allowed Cumulative Whitelist per Step)
# ---------------------------------------------------------
Write-Host "`n[Order Check] 驗證是否嚴格依序執行、未越權偷改後續步驟檔案..." -ForegroundColor Yellow

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
            $targets += @("docs/4_active_tasks/SPRINT_GOAL.md", "docs/4_active_tasks/ACTIVE_TASKS.md", "GEMINI.md", "CHANGELOG.md")
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
    Add-GateError "[跳步違規] 當前僅授權至 Step $Step，卻發現後續步驟或範圍外檔案被修改: $($outOfOrder -join ', ')"
} else {
    Pass-Note "變更檔案範圍完全符合 Step 1..$Step 順序白名單"
}

# 檢查前序已簽核快照是否被意外回退 (跨電腦 LF 正規化比對)
for ($prev = 1; $prev -lt $Step; $prev++) {
    $snapFile = Join-Path $SnapshotDir "step_${prev}_snapshot.json"
    if (Test-Path $snapFile) {
        $snap = Get-Content $snapFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($prop in $snap.PSObject.Properties) {
            $targetFull = Join-Path $RepoRoot $prop.Name
            if (Test-Path $targetFull) {
                $curHash = Get-FileNormalizedSha256 $targetFull
                if ($curHash -ne $prop.Value) {
                    Add-GateError "[回退警告] 已簽核的 Step $prev 檔案遭到後續步驟改動，請指導員人工確認 diff: $($prop.Name)"
                }
            }
        }
    }
}

# ---------------------------------------------------------
# 各階段專項實質內容檢核 (Gate 1 ~ Gate 7)
# ---------------------------------------------------------
Write-Host "`n[Gate $Step] 執行本階段專項細部指標檢核..." -ForegroundColor Yellow

switch ($Step) {
    1 {
        $scriptRaw = Get-Content (Join-Path $RepoRoot "scripts\check-doc-links.ps1") -Raw -Encoding UTF8
        if ($scriptRaw -notmatch 'GEMINI\.md') { Add-GateError "check-doc-links.ps1 未納入根目錄 GEMINI.md 掃描" }
        if ($scriptRaw -notmatch 'ConvertFrom-Json') { Add-GateError "check-doc-links.ps1 未實作 data_schemas JSON 區塊 ConvertFrom-Json 校驗" }
        if ($scriptRaw -notmatch '0_references') { Add-GateError "check-doc-links.ps1 未排除 0_references" }
        Push-Location $RepoRoot
        try {
            $null = powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1 2>&1 | Out-String
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        if ($code -eq 0) {
            Add-GateError "[TDD 反作弊失敗] 在尚未修復文件前，升級後的 check-doc-links.ps1 必須回傳非 0 (紅燈) 並抓出既有錯誤，目前卻回傳 PASS！"
        } else {
            Pass-Note "TDD 紅燈基準驗證成功 (ExitCode=$code)，腳本能精準捕獲既有斷鏈/JSON錯誤/虛構路徑"
        }
    }
    2 {
        $lore = Get-Content (Join-Path $RepoRoot "docs\1_requirements\WORLD_AND_LORE_BIBLE.md") -Raw -Encoding UTF8
        $loop = Get-Content (Join-Path $RepoRoot "docs\1_requirements\GAMEPLAY_CORE_LOOPS.md") -Raw -Encoding UTF8
        $uiux = Get-Content (Join-Path $RepoRoot "docs\1_requirements\UI_UX_REQUIREMENTS.md") -Raw -Encoding UTF8
        $readme = Get-Content (Join-Path $RepoRoot "docs\README.md") -Raw -Encoding UTF8

        if ($lore -notmatch '強殖幾丁質|萬象核心') { Add-GateError "WORLD_AND_LORE_BIBLE.md 未補入強殖基因併入強殖幾丁質樹/萬象核心插槽說明" }
        if ($loop -match 'CombatRules|DrpgCombatLoop|ComboResolver|XpProgressionService') { Add-GateError "GAMEPLAY_CORE_LOOPS.md 仍殘留寫死的具體 Java 類別名稱" }
        if ($loop -notmatch 'INT' -or $loop -notmatch 'WIS') { Add-GateError "GAMEPLAY_CORE_LOOPS.md 道基五維未補入 INT / WIS 屬性鍵對應" }
        if ($uiux -notmatch '待深入討論') { Add-GateError "UI_UX_REQUIREMENTS.md 未標註【待深入討論】過渡說明" }
        if ($uiux -match 'torch-darkness-vignette') { Add-GateError "UI_UX_REQUIREMENTS.md 仍殘留具體 CSS 類別名 .torch-darkness-vignette" }
        if ($readme -notmatch 'skills/' -or $readme -notmatch 'CODE_CONVERGENCE_AND_REFACTORING_TASKS\.md') {
            Add-GateError "docs/README.md 目錄樹未補齊 skills/ 或 CODE_CONVERGENCE_AND_REFACTORING_TASKS.md"
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 2 全部 4 份 L1 / 導覽文件指標全數通過" }
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
                catch { Add-GateError "$($sf.Name) JSON 區塊 #$idx 語法無效: $($_.Exception.Message)" }
            }
        }
        $skillSchema = Get-Content (Join-Path $RepoRoot "docs\2_specifications\data_schemas\SKILL_SCHEMA.md") -Raw -Encoding UTF8
        if ($skillSchema -match '"tree_(overclock|chitin|water|fire|wood)"') {
            Add-GateError "SKILL_SCHEMA.md 仍殘留簡寫 treeId ($($Matches[0]))"
        }
        if ($skillSchema -match 'combo_2p_taiji_water_fire|domain_4p_elemental_cascade') {
            Add-GateError "SKILL_SCHEMA.md 仍殘留舊合體技/領域 ID"
        }
        if ($skillSchema -notmatch 'tags') {
            Add-GateError "SKILL_SCHEMA.md 未補入 comboRole 與 tags 轉換映射規則"
        }
        $treeFiles = Get-ChildItem (Join-Path $RepoRoot "docs\2_specifications\skills") -Filter "TREE_*.md" -Recurse
        if ($treeFiles.Count -ne 17) { Add-GateError "TREE_*.md 數量異常 ($($treeFiles.Count))" }
        foreach ($tf in $treeFiles) {
            $traw = Get-Content $tf.FullName -Raw -Encoding UTF8
            if ($traw -match '\|\s*連攜角色\s*\|') { Add-GateError "$($tf.Name) 表頭仍為『連攜角色』，未改為『戰術／連攜定位』" }
            if ($traw -match '前討') { Add-GateError "$($tf.Name) 仍殘留錯字『前討』" }
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 3 Data Schemas 9 個 JSON 區塊與 17 棵專精樹契約全數通過" }
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

        if ($m1 -notmatch '宗門外門集市') { Add-GateError "M1 缺少宗門外門集市" }
        if ($m1.Substring(0, [Math]::Min(900, $m1.Length)) -match '賽博晶片實驗室') { Add-GateError "M1 頁首或 Mermaid 圖仍殘留舊版 5 大賽博設施名稱" }
        if ($m2 -match '0\.15') { Add-GateError "M2 仍殘留浮點數 dangerRate 0.15" }
        if ($m3 -notmatch '25%.*33%|1/3' -or $m3 -notmatch '6.*8\s*秒|7\s*秒') { Add-GateError "M3 未同步 Boss/菁英護體減傷 (25%~33%) 與 6~8 秒力竭規格" }
        if ($m4 -match '<=\s*25|神識動搖') { Add-GateError "M4 仍殘留 SAN <= 25 神識動搖多餘階層" }
        if ($m5 -notmatch 'tree_chitin_blade|強殖幾丁質') { Add-GateError "M5 未載明主角強殖基因併入強殖幾丁質樹與萬象核心插槽" }
        if ($m6 -match '全服消耗量') { Add-GateError "M6 仍殘留多人連線詞彙『全服消耗量』" }
        if ($m7 -match 'WatchService') { Add-GateError "M7 仍殘留違反憲法紅線之 WatchService 動態熱重載提案" }
        if ($m8 -match 'slot_auto\.json' -or $m8 -notmatch 'autosave\.json') { Add-GateError "M8 自動存檔檔名未修正為 autosave.json" }
        if ($m9 -match '"TOWN_HUB"') { Add-GateError "M9 仍殘留 viewMode: TOWN_HUB" }
        if ($mmap -match '偽裝寶箱怪|data/hub/facilities\.json|js/state/store\.js|action-bar-panel\.js') {
            Add-GateError "MODULES_MAP.md 仍殘留偽裝寶箱怪或錯誤路徑 ($($Matches[0]))"
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 4 頂層架構與後端 M1~M9 全部指標通過" }
    }
    5 {
        $flg = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FRONTEND_LAYOUT_GUIDE.md") -Raw -Encoding UTF8
        $fe1 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE1_CORE_STATE_AND_PIPELINE.md") -Raw -Encoding UTF8
        $fe3 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE3_BATTLE_ARENA_AND_FX.md") -Raw -Encoding UTF8
        $fe4 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE4_TOWN_HUB_AND_ACTION_BAR.md") -Raw -Encoding UTF8
        $fe6 = Get-Content (Join-Path $RepoRoot "docs\2_specifications\frontend\FE6_CSS_TOKENS_AND_THEMES.md") -Raw -Encoding UTF8

        if ($flg -match '30\s*格') { Add-GateError "FRONTEND_LAYOUT_GUIDE.md 仍殘留『30 格』儲物袋錯誤數字" }
        if ($flg -notmatch '現行已驗證' -or $flg -notmatch '待深入討論') { Add-GateError "FRONTEND_LAYOUT_GUIDE.md 未區分標註【現行已驗證】與【待深入討論】" }
        if ($fe1 -match 'js/state/store\.js') { Add-GateError "FE1 仍殘留錯誤路徑 js/state/store.js" }
        if ($fe3 -match 'js/helpers/buff-hud-helper\.js') { Add-GateError "FE3 仍殘留錯誤路徑 js/helpers/buff-hud-helper.js" }
        if ($fe4 -match 'action-bar-panel\.js') { Add-GateError "FE4 仍殘留不存在的 action-bar-panel.js" }
        if ($fe6 -match 'exit-btn|四方出口網格按鈕') { Add-GateError "FE6 仍殘留違規城鎮四方出口按鈕 (.exit-btn)" }
        if ($fe6 -notmatch 'style-07-battle-fx\.css') { Add-GateError "FE6 未正確引用真實第 7 層樣式檔 style-07-battle-fx.css" }
        if ($Errors.Count -eq 0) { Pass-Note "Step 5 前端 8 份規格手冊全部指標通過" }
    }
    6 {
        $m2t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M2_DRPG_DUNGEON_TASKS.md") -Raw -Encoding UTF8
        $m3t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M3_COMBAT_ENGINE_TASKS.md") -Raw -Encoding UTF8
        $m31t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M3_1_BUFF_AND_STATUS_TASKS.md") -Raw -Encoding UTF8
        $m5t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M5_SKILL_AND_PROGRESSION_TASKS.md") -Raw -Encoding UTF8
        $m7t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M7_DATA_DRIVEN_TEMPLATES_TASKS.md") -Raw -Encoding UTF8
        $m8t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\backend\M8_SAVE_AND_PERSISTENCE_TASKS.md") -Raw -Encoding UTF8
        $sst = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\data_schemas\SKILL_SCHEMA_TASKS.md") -Raw -Encoding UTF8
        $fe6t = Get-Content (Join-Path $RepoRoot "docs\3_task_lists\frontend\FE6_CSS_TOKENS_TASKS.md") -Raw -Encoding UTF8

        if ($m2t -match '偽裝寶箱怪|Task M2-2') { Add-GateError "M2_TASKS 未移除低 SAN 幻覺與偽裝寶箱怪 (Task M2-2)" }
        if ($m3t -match 'COMBAT-ACTION-RELOAD|換彈') { Add-GateError "M3_TASKS 未移除孤兒任務 Task M3-2 (兵刃換彈)" }
        if ($m3t -match '-\s*\[\s*\]\s*\*\*Task M3-1') { Add-GateError "M3_TASKS 的 Task M3-1 仍未勾選 [x]" }
        if ($m3t -notmatch 'POISE-RANK-GATE|Task M3-3') { Add-GateError "M3_TASKS 未新增 Task M3-3 (護體罡氣限定 Boss/菁英)" }
        if ($m31t -notmatch 'valueFormula|遞減|DR') { Add-GateError "M3_1_TASKS 未補齊 L2 M3_1 控場遞減 DR 與動態公式引擎待辦" }
        if ($m5t -match '32\+\s*賽博天賦樹') { Add-GateError "M5_TASKS 未移除舊版重疊之 32+ 賽博天賦樹待辦" }
        if ($m7t -match 'DATA-HOT-RELOAD|Task M7-1') { Add-GateError "M7_TASKS 未移除違憲熱重載 Task M7-1" }
        if ($m8t -match '-\s*\[\s*\]\s*\*\*Task M8-2' -or $m8t -match 'slot_auto') { Add-GateError "M8_TASKS 的 Task M8-2 未勾選 [x] 或仍殘留 slot_auto" }
        if ($sst -match '-\s*\[\s*\]\s*\*\*Task SKILL-SCHEMA-1') { Add-GateError "SKILL_SCHEMA_TASKS 的 Task SKILL-SCHEMA-1 仍未勾選 [x]" }
        if ($fe6t -match 'style-01-tokens\.css|style-03-dungeon-view\.css|style-06-modals-and-forms\.css') {
            Add-GateError "FE6_TASKS 仍殘留虛構 CSS 檔名"
        }
        if ($Errors.Count -eq 0) { Pass-Note "Step 6 L3 全部 22 份任務清單收斂指標通過" }
    }
    7 {
        $sg = Get-Content (Join-Path $RepoRoot "docs\4_active_tasks\SPRINT_GOAL.md") -Raw -Encoding UTF8
        $gem = Get-Content (Join-Path $RepoRoot "GEMINI.md") -Raw -Encoding UTF8
        $chg = Get-Content (Join-Path $RepoRoot "CHANGELOG.md") -Raw -Encoding UTF8

        if ($sg -match '16\s*項賽博修仙技能|五個問題|微量強殖基因槽') {
            Add-GateError "SPRINT_GOAL.md 仍殘留舊詞彙 ($($Matches[0]))"
        }
        if ($gem -match 'docs/3_task_lists/CHANGELOG\.md|docs/3_task_lists/future_improvements_history\.md') {
            Add-GateError "GEMINI.md 仍殘留失效連結路徑"
        }
        if ($chg -notmatch '2026-10-29') {
            Add-GateError "CHANGELOG.md 尚未登載 2026-10-29 文件對齊與看門狗升級紀錄"
        }
        Push-Location $RepoRoot
        try {
            $out = powershell -ExecutionPolicy Bypass -File .\scripts\check-doc-links.ps1 2>&1 | Out-String
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        Write-Host $out
        if ($code -ne 0) {
            Add-GateError "終極看門狗 check-doc-links.ps1 執行失敗 (ExitCode=$code)！"
        } else {
            Pass-Note "終極看門狗 check-doc-links.ps1 100% 綠燈通過 (ExitCode=0)"
        }
    }
}

if ($Errors.Count -gt 0) {
    Write-Host "`n[REJECTED] Gate $Step 審核未通過（共 $($Errors.Count) 項缺失），請退回 gemini 3.8 修正後重檢！" -ForegroundColor Red
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
        Pass-Note "已封存 Step $Step 跨平台簽核快照 -> scripts/.gate_snapshots/step_${Step}_snapshot.json"
    }
    Write-Host "`n[APPROVED] Gate $Step 審核 100% 通過！准予更新 DOCS_ALIGNMENT_MASTER_PLAN.md 並放行下一關！" -ForegroundColor Green
    exit 0
}

