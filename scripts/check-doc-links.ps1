[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $ScriptDir
$DocsDir = Join-Path $WorkspaceRoot "docs"

$hasErrors = $false

# =============================================================================
# Check A: Markdown Link Validity
# Scope: docs/**/*.md (excluding 0_archive and 0_references) + root GEMINI.md
# =============================================================================
Write-Host "SCAN [A]: Scanning Markdown links..." -ForegroundColor Cyan

$mdFilesA = @()
if (Test-Path $DocsDir) {
    $mdFilesA += Get-ChildItem -Path $DocsDir -Filter "*.md" -Recurse | Where-Object {
        $_.FullName -notmatch "0_archive" -and $_.FullName -notmatch "0_references"
    }
}
$geminiFile = Join-Path $WorkspaceRoot "GEMINI.md"
if (Test-Path $geminiFile) {
    $mdFilesA += Get-Item $geminiFile
}

$brokenLinks = @()
$totalLinksChecked = 0

foreach ($file in $mdFilesA) {
    $lines = Get-Content -Path $file.FullName -Encoding UTF8
    
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $lineNum = $i + 1
        
        # Strip inline backtick segments to avoid false link matching
        $lineClean = $line -replace '\x60[^\x60\r\n]*\x60', ''
        
        $linkMatches = [regex]::Matches($lineClean, '\[.*?\]\(([^)]+)\)')
        foreach ($item in $linkMatches) {
            $rawTarget = $item.Groups[1].Value.Trim()
            
            if ($rawTarget.StartsWith("http://") -or $rawTarget.StartsWith("https://") -or $rawTarget.StartsWith("mailto:") -or $rawTarget.StartsWith("#")) {
                continue
            }
            # Skip documentation placeholder links
            if ($rawTarget -match '(\.\./path/file\.md|path/to/file|path/file\.md)') {
                continue
            }
            
            $targetPath = $null
            try {
                if ($rawTarget.StartsWith("file:///")) {
                    $cleanTarget = $rawTarget.Substring(8) -replace '#.*$', ''
                    $cleanTarget = $cleanTarget -replace '\?.*$', ''
                    $targetPath = [System.Uri]::UnescapeDataString($cleanTarget)
                } else {
                    $relPath = $rawTarget -replace '#.*$', ''
                    $relPath = $relPath -replace '\?.*$', ''
                    $relPath = $relPath.Trim()
                    if ([string]::IsNullOrWhiteSpace($relPath)) {
                        continue
                    }
                    $dir = Split-Path -Parent $file.FullName
                    $targetPath = [System.IO.Path]::GetFullPath((Join-Path $dir $relPath))
                }
            } catch {
                $targetPath = $null
            }
            
            $totalLinksChecked++
            if ($targetPath -eq $null -or -not (Test-Path $targetPath)) {
                $relFile = $file.FullName.Replace($WorkspaceRoot, "").TrimStart("\/")
                $brokenLinks += [PSCustomObject]@{
                    File = $relFile
                    Line = $lineNum
                    Link = $rawTarget
                    Resolved = if ($targetPath) { $targetPath.Replace($WorkspaceRoot, "").TrimStart("\/") } else { "Invalid path" }
                }
            }
        }
    }
}

# =============================================================================
# Check B: Backtick Physical File Path Existence
# Scope: docs/**/*.md (excluding 0_archive, 0_references, DOCS_ALIGNMENT_*.md)
# =============================================================================
Write-Host "SCAN [B]: Checking backtick physical file paths..." -ForegroundColor Cyan

$filesB = @()
if (Test-Path $DocsDir) {
    $filesB = Get-ChildItem -Path $DocsDir -Filter "*.md" -Recurse | Where-Object {
        $_.FullName -notmatch "0_archive" -and
        $_.FullName -notmatch "0_references" -and
        $_.Name -notlike "DOCS_ALIGNMENT_*.md"
    }
}

# Exemption pattern using Unicode escapes to guarantee ASCII safety in PowerShell 5.1
$exemptionRegex = '(\u3010\u5df2\u6c7a\u8b70\u5f85\u5be6\u4f5c\u3011|\u3010\u5019\u9078\u63d0\u6848\u3011|\u3010\u6b77\u53f2[\uff0f\/]\u5df2\u9664\u5f79\u3011|\u9664\u5f79|\u5c01\u5b58|\u5df2\u522a\u9664|\u6b78\u6a94)'
$pathRegex = '\x60((?:src/main/|src/test/|static/|js/|css/|data/|scripts/)[^\x60*\s]+\.(?:java|js|css|html|json|ps1|md))\x60'

$brokenPaths = @()
$totalPathsChecked = 0

foreach ($file in $filesB) {
    $lines = Get-Content -Path $file.FullName -Encoding UTF8
    
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $lineNum = $i + 1
        
        # Skip line if it contains exemption tags
        if ($line -match $exemptionRegex) {
            continue
        }
        
        $matches = [regex]::Matches($line, $pathRegex)
        foreach ($m in $matches) {
            $rawPath = $m.Groups[1].Value
            # Skip wildcards and dynamic runtime saves
            if ($rawPath -like "*saves/*" -or $rawPath -like "saves/*" -or $rawPath -like "*\**") {
                continue
            }
            
            $resolved = ""
            if ($rawPath.StartsWith("static/")) {
                $resolved = Join-Path $WorkspaceRoot ("src/main/resources/" + $rawPath)
            } elseif ($rawPath.StartsWith("js/") -or $rawPath.StartsWith("css/")) {
                $resolved = Join-Path $WorkspaceRoot ("src/main/resources/static/" + $rawPath)
            } elseif ($rawPath.StartsWith("data/")) {
                $resolved = Join-Path $WorkspaceRoot ("src/main/resources/" + $rawPath)
            } else {
                $resolved = Join-Path $WorkspaceRoot $rawPath
            }
            
            $totalPathsChecked++
            if (-not (Test-Path $resolved)) {
                $relFile = $file.FullName.Replace($WorkspaceRoot, "").TrimStart("\/")
                $brokenPaths += [PSCustomObject]@{
                    File = $relFile
                    Line = $lineNum
                    RawPath = $rawPath
                    Resolved = $resolved.Replace($WorkspaceRoot, "").TrimStart("\/")
                }
            }
        }
    }
}

# =============================================================================
# Check C: JSON Syntax in data_schemas/*.md
# Scope: docs/2_specifications/data_schemas/*.md
# =============================================================================
Write-Host "SCAN [C]: Checking JSON syntax in data_schemas/*.md..." -ForegroundColor Cyan

$schemaDir = Join-Path $DocsDir "2_specifications/data_schemas"
$brokenJsonBlocks = @()
$totalJsonBlocksChecked = 0

if (Test-Path $schemaDir) {
    $schemaFiles = Get-ChildItem -Path $schemaDir -Filter "*.md"
    $jsonBlockRegex = '(?s)\x60\x60\x60json[ \t]*\r?\n(.*?)\r?\n\x60\x60\x60'
    
    foreach ($file in $schemaFiles) {
        $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
        $jsonMatches = [regex]::Matches($content, $jsonBlockRegex)
        $blockIdx = 0
        
        foreach ($jm in $jsonMatches) {
            $blockIdx++
            $totalJsonBlocksChecked++
            $jsonText = $jm.Groups[1].Value
            
            try {
                $jsonText | ConvertFrom-Json | Out-Null
            } catch {
                $brokenJsonBlocks += [PSCustomObject]@{
                    File = $file.Name
                    BlockIndex = $blockIdx
                    Error = $_.Exception.Message
                }
            }
        }
    }
}

# =============================================================================
# Summary and Exit Code
# =============================================================================
Write-Host ""
Write-Host "STATS: Checked $totalLinksChecked Markdown links, $totalPathsChecked backtick file paths, and $totalJsonBlocksChecked schema JSON blocks." -ForegroundColor Yellow

if ($brokenLinks.Count -gt 0) {
    $hasErrors = $true
    Write-Host ""
    Write-Host "FAIL [A]: Found $($brokenLinks.Count) broken Markdown link(s):" -ForegroundColor Red
    foreach ($err in $brokenLinks) {
        Write-Host ("  [{0}:{1}] -> {2}" -f $err.File, $err.Line, $err.Link) -ForegroundColor DarkRed
        Write-Host ("     Missing: {0}" -f $err.Resolved) -ForegroundColor DarkGray
    }
} else {
    Write-Host "PASS [A]: All Markdown links are valid." -ForegroundColor Green
}

if ($brokenPaths.Count -gt 0) {
    $hasErrors = $true
    Write-Host ""
    Write-Host "FAIL [B]: Found $($brokenPaths.Count) non-existent backtick file path(s):" -ForegroundColor Red
    foreach ($err in $brokenPaths) {
        $tick = [char]96
        Write-Host ("  [{0}:{1}] -> {2}{3}{2}" -f $err.File, $err.Line, $tick, $err.RawPath) -ForegroundColor DarkRed
        Write-Host ("     Resolved: {0}" -f $err.Resolved) -ForegroundColor DarkGray
    }
} else {
    Write-Host "PASS [B]: All backtick file paths exist." -ForegroundColor Green
}

if ($brokenJsonBlocks.Count -gt 0) {
    $hasErrors = $true
    Write-Host ""
    Write-Host "FAIL [C]: Found $($brokenJsonBlocks.Count) invalid schema JSON block(s):" -ForegroundColor Red
    foreach ($err in $brokenJsonBlocks) {
        Write-Host ("  [{0} #{1}] -> {2}" -f $err.File, $err.BlockIndex, $err.Error) -ForegroundColor DarkRed
    }
} else {
    Write-Host "PASS [C]: All schema JSON blocks are syntactically valid." -ForegroundColor Green
}

if ($hasErrors) {
    Write-Host ""
    Write-Host "OVERALL: FAIL (One or more checks failed. See details above.)" -ForegroundColor Red
    exit 1
} else {
    Write-Host ""
    Write-Host "OVERALL: PASS (All checks passed successfully!)" -ForegroundColor Green
    exit 0
}
