@echo off
rem START or STOP Services
rem ----------------------------------
rem Check if argument is STOP or START

if not ""%1"" == ""START"" goto stop

if exist @@BITROCK_INSTALLDIR@@\hypersonic\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\server\hsql-sample-database\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\ingres\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\ingres\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\mysql\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\mysql\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\postgresql\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\postgresql\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\apache2\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\apache2\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\openoffice\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\openoffice\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\tomcat\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\tomcat\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\jetty\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\jetty\scripts\servicerun.bat START)
if exist @@BITROCK_INSTALLDIR@@\subversion\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\subversion\scripts\servicerun.bat START)
rem RUBY_APPLICATION_START
if exist @@BITROCK_INSTALLDIR@@\lucene\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\lucene\scripts\servicerun.bat START)
goto end

:stop

if exist @@BITROCK_INSTALLDIR@@\lucene\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\lucene\scripts\servicerun.bat STOP)
rem RUBY_APPLICATION_STOP
if exist @@BITROCK_INSTALLDIR@@\subversion\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\subversion\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\jetty\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\jetty\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\hypersonic\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\server\hsql-sample-database\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\tomcat\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\tomcat\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\openoffice\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\openoffice\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\apache2\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\apache2\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\ingres\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\ingres\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\mysql\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\mysql\scripts\servicerun.bat STOP)
if exist @@BITROCK_INSTALLDIR@@\postgresql\scripts\servicerun.bat (start /MIN @@BITROCK_INSTALLDIR@@\postgresql\scripts\servicerun.bat STOP)

:end
