@echo off
echo [DEPRECATED] "setmysql.bat" has been renamed to "install_sql_files.bat".
echo Forwarding to the new script...
call "%~dp0install_sql_files.bat" %*
exit /b %errorlevel%

