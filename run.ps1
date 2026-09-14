[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $ScriptDir

try {
    # 1. 偵測 JDK 路徑 (優先使用 DevTools 目錄，否則使用系統 JAVA_HOME 或 PATH)
    if (Test-Path "C:\Workspace\DevTools\jdk-25.0.4.1+1") {
        $env:JAVA_HOME = "C:\Workspace\DevTools\jdk-25.0.4.1+1"
    } elseif (-not $env:JAVA_HOME) {
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if ($javaCmd) {
            $env:JAVA_HOME = (Split-Path -Parent (Split-Path -Parent $javaCmd.Source))
        }
    }
    if ($env:JAVA_HOME) {
        $env:PATH = "$env:JAVA_HOME\bin;$($env:PATH)"
    }

    # 2. 偵測 Maven 執行工具 (優先 DevTools，次之系統 mvn，最後 fallback 至 mvnw.cmd)
    $mvnExecutable = $null
    if (Test-Path "C:\Workspace\DevTools\apache-maven-3.9.16\bin\mvn.cmd") {
        $env:M2_HOME = "C:\Workspace\DevTools\apache-maven-3.9.16"
        $env:MAVEN_HOME = "C:\Workspace\DevTools\apache-maven-3.9.16"
        $mvnExecutable = "C:\Workspace\DevTools\apache-maven-3.9.16\bin\mvn.cmd"
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        $mvnExecutable = "mvn"
    } elseif (Test-Path "$ScriptDir\mvnw.cmd") {
        $mvnExecutable = "$ScriptDir\mvnw.cmd"
    } else {
        $mvnExecutable = "mvn"
    }

    $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
    chcp 65001 | Out-Null

    Write-Host "[MUD] Starting Practice MUD with Java: $env:JAVA_HOME and Maven: $mvnExecutable..." -ForegroundColor Cyan
    & $mvnExecutable spring-boot:run
} finally {
    Pop-Location
}
