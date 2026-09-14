[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $ScriptDir

try {
    # 1. 偵測 JDK 路徑
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

    # 2. 偵測 Maven 執行工具
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

    if ($args.Count -eq 0) {
        & $mvnExecutable clean test
    } else {
        $hasGoal = $false
        foreach ($a in $args) {
            if (-not $a.StartsWith("-")) {
                $hasGoal = $true
                break
            }
        }
        if (-not $hasGoal) {
            & $mvnExecutable test @args
        } else {
            & $mvnExecutable @args
        }
    }
} finally {
    Pop-Location
}
