rem @echo off
rem ---------------------------------
rem Script to clean Tomcat temp files
rem ---------------------------------

set ALF_HOME=%~dp0..
set CATALINA_HOME=%ALF_HOME%\tomcat

echo Cleaning temporary Alfresco files from Tomcat...
del /S /F /Q %CATALINA_HOME%\temp\Alfresco %CATALINA_HOME%\work\Catalina\localhost\alfresco