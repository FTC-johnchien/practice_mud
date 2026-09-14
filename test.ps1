[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_HOME = "C:\Workspace\DevTools\jdk-25.0.4.1+1"
$env:M2_HOME = "C:\Workspace\DevTools\apache-maven-3.9.16"
$env:MAVEN_HOME = "C:\Workspace\DevTools\apache-maven-3.9.16"
$env:PATH = "C:\Workspace\DevTools\jdk-25.0.4.1+1\bin;C:\Workspace\DevTools\apache-maven-3.9.16\bin;$($env:PATH)"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
chcp 65001 | Out-Null
& "C:\Workspace\DevTools\apache-maven-3.9.16\bin\mvn.cmd" test $args
