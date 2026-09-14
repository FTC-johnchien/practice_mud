@echo off
chcp 65001 > nul
set "JAVA_HOME=C:\Workspace\DevTools\jdk-25.0.4.1+1"
set "M2_HOME=C:\Workspace\DevTools\apache-maven-3.9.16"
set "MAVEN_HOME=C:\Workspace\DevTools\apache-maven-3.9.16"
set "PATH=C:\Workspace\DevTools\jdk-25.0.4.1+1\bin;C:\Workspace\DevTools\apache-maven-3.9.16\bin;%PATH%"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
echo [MUD] Starting Practice MUD with DevTools JDK 25 and Maven 3.9.16...
call "C:\Workspace\DevTools\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
