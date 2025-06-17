@echo off
setlocal enabledelayedexpansion

echo Cleaning previous build...
if exist "target\classes" rd /s /q "target\classes"
mkdir target\classes

echo Compiling project...

set CLASSPATH=lib\*
for /r lib %%i in (*.jar) do set CLASSPATH=!CLASSPATH!;%%i

set SOURCES=
for /r src %%i in (*.java) do set SOURCES=!SOURCES! "%%i"

javac -encoding UTF-8 -d target\classes -cp "%CLASSPATH%" %SOURCES%

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b %errorlevel%
)

echo Compilation successful!
echo Copying resources...
xcopy /s /y src\*.properties target\classes\ 2>nul
xcopy /s /y src\*.xml target\classes\ 2>nul
xcopy /s /y src\*.fxml target\classes\ 2>nul

echo Build completed successfully! 