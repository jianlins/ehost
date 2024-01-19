@ECHO OFF
cd /d %~dp0
Set StartInDirectory=%CD%
setLocal EnableDelayedExpansion
echo "Welcome to eHOST ${project.version}"
java -jar ehost-${project.version}.jar

