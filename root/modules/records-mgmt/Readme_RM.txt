Records Management AMP
----------------------

To install the Records Management AMP, you need to copy it to your Alfresco
server and run the ModuleManagment script in the extras/Module-Management
directory.

You must ensure that the Alfresco server is not running before starting the
ModuleManagement script.  Once the script has been run, you will have an 
updated alfresco.war file, with a backup of the previous WAR.  Restart the
Alfresco server and go to the Advanced Space Wizard: there will now be an 
option to create a File Plan from the Create from Scratch option.

Example of running ModuleManagement:

On Linux:
cd extras/Module-Management
sh ModuleManagement.sh install ../../RM.amp ../../tomcat/webapps/alfresco.war -verbose

On Windows:
cd extras\Module-Management
ModuleManagement.bat install ..\..\RM.amp ..\..\tomcat\webapps\alfresco.war -verbose


Note
----

The ModuleManagement scripts assume a Tomcat-based installation of Alfresco.