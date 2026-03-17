@echo off
REM Run the local RFC service with JCo native library (Windows)

cd /d "%~dp0"

REM Default destination configuration
if not defined RFC_DEST_LOCAL_ASHOST set RFC_DEST_LOCAL_ASHOST=localhost
if not defined RFC_DEST_LOCAL_SYSNR set RFC_DEST_LOCAL_SYSNR=00
if not defined RFC_DEST_LOCAL_CLIENT set RFC_DEST_LOCAL_CLIENT=100
if not defined RFC_DEST_LOCAL_USER set RFC_DEST_LOCAL_USER=DEVELOPER
if not defined RFC_DEST_LOCAL_PASSWD set RFC_DEST_LOCAL_PASSWD=password
if not defined RFC_DEST_LOCAL_LANG set RFC_DEST_LOCAL_LANG=EN
if not defined PORT set PORT=8090

REM Load .env file if exists
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if not "%%a"=="" if not "%%a:~0,1%"=="#" set "%%a=%%b"
    )
)

echo Starting RFC service on port %PORT%...
echo Destination 'local' configured for: %RFC_DEST_LOCAL_ASHOST% system %RFC_DEST_LOCAL_SYSNR% client %RFC_DEST_LOCAL_CLIENT%
echo.
echo Test with:
echo   curl -X POST http://localhost:%PORT%/rfc/execute ^
echo     -H "Content-Type: application/json" ^
echo     -d "{\"destination\":\"local\",\"functionModule\":\"RFC_PING\",\"importing\":{}}"
echo.

REM Run with maven (keeps original JCo JAR name on classpath)
call mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.library.path=%~dp0lib"
