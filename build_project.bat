@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=C:\Users\Dell\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "D:\Department of Computer Science\abhi\newtrafficanalyzer"
call gradlew.bat assembleDebug --no-daemon
