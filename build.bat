
@echo off
if not exist "bin" mkdir bin

::SETLOCAL EnableDelayedExpansion
cd src
set javaFiles=
for /r %%i in (*.java) do set javaFiles=!javaFiles! %%i
echo %javaFiles%
cd ..
::ENDLOCAL

::javac -cp %javaFiles% -d bin

jar cvMf bin/purchase.jar -C bin .