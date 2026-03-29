@echo off
echo =========================================
echo  GroceryPro — Store Management System
echo =========================================

echo.
echo [1] Checking SQLite JDBC Driver...
if not exist sqlite-jdbc-3.41.2.2.jar (
    echo Downloading...
    curl -L -o sqlite-jdbc-3.41.2.2.jar "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar"
) else (
    echo Found.
)

echo.
echo [2] Compiling Store.java...
javac -cp ".;sqlite-jdbc-3.41.2.2.jar" src\Store.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Compilation successful!

echo.
echo [3] Launching...
echo    Open your browser at: http://localhost:8080
echo.
java -cp "src;sqlite-jdbc-3.41.2.2.jar" Store
pause
