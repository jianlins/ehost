@ECHO OFF
cd /d %~dp0
Set StartInDirectory=%CD%
setLocal EnableDelayedExpansion
Set spring.config.location=application.properties
java -jar ehost-${project.version}.jar -c ./

