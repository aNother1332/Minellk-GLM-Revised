@echo off
rem Minellk portable packaging: requires JDK 21+ (with jpackage/jlink), produces dist\Minellk-2.0-portable.zip
rem Steps: compile - jar - jlink trimmed runtime - jpackage app-image - clean runtime data - zip
cd /d "%~dp0"
setlocal enabledelayedexpansion

where javac >nul 2>nul || (echo JDK not found in PATH & exit /b 1)

echo [1/6] compiling sources...
if not exist out mkdir out
set "SRC="
for /r src %%f in (*.java) do set "SRC=!SRC! "%%%%f""
javac -encoding UTF-8 --module-path "lib\javafx" --add-modules javafx.controls -d out !SRC! || exit /b 1

echo [2/6] creating application jar...
if not exist build mkdir build
jar --create --file build\minellk.jar --main-class app.Main -C out . || exit /b 1

echo [3/6] trimming runtime with jlink...
if exist build\runtime rmdir /s /q build\runtime
jlink --add-modules java.base,java.desktop,java.logging,java.xml,jdk.charsets,javafx.base,javafx.graphics,javafx.controls --strip-debug --no-man-pages --no-header-files --compress=zip-6 --module-path "lib\javafx" --output build\runtime || exit /b 1

echo [4/6] assembling app image with jpackage...
if exist build\Minellk rmdir /s /q build\Minellk
if exist build\input rmdir /s /q build\input
mkdir build\input\libs
copy /y build\minellk.jar build\input\ >nul
copy /y lib\javafx\javafx-*.jar build\input\libs\ >nul
jpackage --type app-image --name Minellk --app-version 2.0 --vendor "aNother1332" --input build\input --main-jar minellk.jar --main-class app.Main --runtime-image build\runtime --dest build --java-options "--module-path" --java-options "$APPDIR/libs" --java-options "--add-modules" --java-options "javafx.controls" || exit /b 1

echo [5/6] attaching resources and cleaning runtime data...
xcopy /e /i /y resource build\Minellk\resource >nul
if exist build\Minellk\users.txt del build\Minellk\users.txt
if exist build\Minellk\settings.txt del build\Minellk\settings.txt
if exist build\Minellk\saves rmdir /s /q build\Minellk\saves

echo [6/6] zipping portable package...
if not exist dist mkdir dist
if exist dist\Minellk-2.0-portable.zip del dist\Minellk-2.0-portable.zip
powershell -NoProfile -Command "Compress-Archive -Path 'build\Minellk\*' -DestinationPath 'dist\Minellk-2.0-portable.zip' -CompressionLevel Optimal" || exit /b 1

echo.
echo Done: dist\Minellk-2.0-portable.zip
