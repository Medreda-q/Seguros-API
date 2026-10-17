@echo off
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper Startup Batch script
@REM ----------------------------------------------------------------------------

setlocal

set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

set "WRAPPER_JAR=%DIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  for %%I in (java.exe) do set "JAVA_EXE=%%~$PATH:I"
)

if not exist "%JAVA_EXE%" (
  if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe"
  )
)

if not exist "%JAVA_EXE%" (
  for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
    if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
  )
)

if not exist "%JAVA_EXE%" (
  echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
  echo Please set the JAVA_HOME variable in your environment to match the >&2
  echo location of your Java installation. >&2
  exit /b 1
)

"%JAVA_EXE%" "-Dmaven.multiModuleProjectDirectory=%DIR%" -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
exit /b %ERRORLEVEL%
