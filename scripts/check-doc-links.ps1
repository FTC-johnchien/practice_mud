[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $ScriptDir
$DocsDir = Join-Path $WorkspaceRoot "docs"

Write-Host "SCAN: Scanning markdown links under $DocsDir..." -ForegroundColor Cyan

$mdFiles = Get-ChildItem -Path $DocsDir -Filter "*.md" -Recurse | Where-Object {
    $_.FullName -notmatch "0_archive"
}

$brokenLinks = @()
$totalChecked = 0

foreach ($file in $mdFiles) {
    $lines = Get-Content -Path $file.FullName -Encoding UTF8
    
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $lineNum = $i + 1
        
        $linkMatches = [regex]::Matches($line, '\[.*?\]\(([^)]+)\)')
        foreach ($item in $linkMatches) {
            $rawTarget = $item.Groups[1].Value.Trim()
            
            if ($rawTarget.StartsWith("http://") -or $rawTarget.StartsWith("https://") -or $rawTarget.StartsWith("mailto:") -or $rawTarget.StartsWith("#")) {
                continue
            }
            # 略過文檔中的示例佔位路徑
            if ($rawTarget -match '(\.\./path/file\.md|path/to/file)') {
                continue
            }
            
            $targetPath = ""
            if ($rawTarget.StartsWith("file:///")) {
                $targetPath = $rawTarget.Substring(8)
                $targetPath = $targetPath -replace '#.*$', ''
                $targetPath = [System.Uri]::UnescapeDataString($targetPath)
            } else {
                $relPath = $rawTarget -replace '#.*$', ''
                if ([string]::IsNullOrWhiteSpace($relPath)) {
                    continue
                }
                $dir = Split-Path -Parent $file.FullName
                $targetPath = [System.IO.Path]::GetFullPath((Join-Path $dir $relPath))
            }
            
            $totalChecked++
            if (-not (Test-Path $targetPath)) {
                $errObj = [PSCustomObject]@{
                    File = $file.FullName.Replace($WorkspaceRoot, "").TrimStart("\/")
                    Line = $lineNum
                    Link = $rawTarget
                    Resolved = $targetPath.Replace($WorkspaceRoot, "").TrimStart("\/")
                }
                $brokenLinks += $errObj
            }
        }
    }
}

Write-Host "STATS: Checked $totalChecked internal links." -ForegroundColor Yellow

if ($brokenLinks.Count -gt 0) {
    Write-Host "FAIL: Found $($brokenLinks.Count) broken links:" -ForegroundColor Red
    foreach ($err in $brokenLinks) {
        Write-Host "  [$($err.File):$($err.Line)] -> $($err.Link)" -ForegroundColor DarkRed
        Write-Host "     Missing: $($err.Resolved)" -ForegroundColor DarkGray
    }
    exit 1
} else {
    Write-Host "PASS: All internal Markdown links are valid!" -ForegroundColor Green
    exit 0
}
