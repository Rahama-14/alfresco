@echo off
rem -- Check if argument is INSTALL or REMOVE

if not ""%1"" == ""INSTALL"" goto remove

if exist @@BITROCK_INSTALLDIR@@\mysql\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\mysql\scripts\serviceinstall.bat INSTALL)
if exist @@BITROCK_INSTALLDIR@@\postgresql\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\postgresql\scripts\serviceinstall.bat INSTALL)
if exist @@BITROCK_INSTALLDIR@@\apache2\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\apache2\scripts\serviceinstall.bat INSTALL)
if exist @@BITROCK_INSTALLDIR@@\tomcat\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\tomcat\scripts\serviceinstall.bat INSTALL)
if exist @@BITROCK_INSTALLDIR@@\openoffice\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\openoffice\scripts\serviceinstall.bat INSTALL)
if exist @@BITROCK_INSTALLDIR@@\subversion\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\subversion\scripts\serviceinstall.bat INSTALL)
rem RUBY_APPLICATION_INSTALL
if exist @@BITROCK_INSTALLDIR@@\lucene\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\lucene\scripts\serviceinstall.bat INSTALL)

goto end

:remove

if exist @@BITROCK_INSTALLDIR@@\mysql\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\mysql\scripts\serviceinstall.bat)
if exist @@BITROCK_INSTALLDIR@@\postgresql\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\postgresql\scripts\serviceinstall.bat)
if exist @@BITROCK_INSTALLDIR@@\apache2\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\apache2\scripts\serviceinstall.bat)
if exist @@BITROCK_INSTALLDIR@@\tomcat\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\tomcat\scripts\serviceinstall.bat)
if exist @@BITROCK_INSTALLDIR@@\openoffice\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\openoffice\scripts\serviceinstall.bat)
if exist @@BITROCK_INSTALLDIR@@\subversion\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\subversion\scripts\serviceinstall.bat)
rem RUBY_APPLICATION_REMOVE
if exist @@BITROCK_INSTALLDIR@@\lucene\scripts\serviceinstall.bat (start /MIN @@BITROCK_INSTALLDIR@@\lucene\scripts\serviceinstall.bat)

:end
