@echo off
echo =========================================
echo  GroceryPro — Store Management System (MySQL)
echo =========================================

echo.
echo [1] Checking MySQL JDBC Driver...
if not exist mysql-connector-j-8.3.0.jar (
    echo Downloading...
    curl.exe -L https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar -o mysql-connector-j-8.3.0.jar
) else (
    echo Found.
)

echo.
echo [2] Compiling Store.java...
javac -cp ".;mysql-connector-j-8.3.0.jar" src\Store.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Compilation successful!

echo.
echo [3] Launching...
echo    Make sure MYSQL_URL environment variable is set!
echo    Open your browser at: http://localhost:8080
echo.
java -cp "src;mysql-connector-j-8.3.0.jar" Store
pause
