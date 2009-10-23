#!/bin/sh
# ---------------------------------
# Script to clean Tomcat temp files
# ---------------------------------
echo "Cleaning temporary Alfresco files from Tomcat..."
rm -rf tomcat/temp/Alfresco tomcat/work/Catalina/localhost/alfresco
rm -rf tomcat/work/Catalina/localhost/share
rm -rf tomcat/work/Catalina/localhost/mobile
rm -rf tomcat/work/Catalina/localhost/studio