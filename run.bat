@echo off
chcp 65001 > nul
set "SCRIPT_DIR=%~dp0"
if exist "C:\Workspace\DevTools\jdk-25.0.4.1+1\bin\java.exe" set "JAVA_HOME=C:\Workspace\DevTools\jdk-25.0.4.1+1"
if exist "C:\Workspace\DevTools\apache-maven-3.9.16\bin\mvn.cmd" set "MAVEN_HOME=C:\Workspace\DevTools\apache-maven-3.9.16"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
set "SPRING_PROFILES_ACTIVE=dev"
echo [MUD] Starting Practice MUD with DevTools JDK 25 and Maven 3.9.16...
if exist "%MAVEN_HOME%\bin\mvn.cmd" (call "%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run) else (call "%SCRIPT_DIR%mvnw.cmd" spring-boot:run)
