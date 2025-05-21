@ECHO OFF
cd /d %~dp0
Set StartInDirectory=%CD%
setLocal EnableDelayedExpansion
java -Dspring.config.location=application.properties  -jar eHOST.jar -c ./

