@echo off
set VERSION=9.6.0
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-%VERSION%-bin\fincip-native\gradle-%VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle %VERSION% is not cached. Download it from https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip
  exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
