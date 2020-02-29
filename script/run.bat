@ECHO OFF
cd /d %~dp0
Set StartInDirectory=%CD%
setLocal EnableDelayedExpansion
jre/bin/java -jar ehost-${version}.jar

