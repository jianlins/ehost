@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

if not exist "%PROJECT_DIR%\pom.xml" (
	echo [ERROR] Cannot find pom.xml in "%PROJECT_DIR%".
	exit /b 1
)

if not exist "%PROJECT_DIR%\lib\annotationadmin-integration-1.0.13.jar" (
	echo [ERROR] Missing file: "%PROJECT_DIR%\lib\annotationadmin-integration-1.0.13.jar"
	exit /b 1
)

if not exist "%PROJECT_DIR%\lib\MultiSplit.jar" (
	echo [ERROR] Missing file: "%PROJECT_DIR%\lib\MultiSplit.jar"
	exit /b 1
)

if not exist "%PROJECT_DIR%\lib\utsapi2_0.jar" (
	echo [ERROR] Missing file: "%PROJECT_DIR%\lib\utsapi2_0.jar"
	exit /b 1
)

pushd "%PROJECT_DIR%" || exit /b 1

call mvn install:install-file -Dfile="%PROJECT_DIR%\lib\annotationadmin-integration-1.0.13.jar" -DgroupId=gov.va.vinci -DartifactId=annotation-admin -Dversion=1.0 -Dpackaging=jar
if errorlevel 1 (
	popd
	exit /b 1
)

call mvn install:install-file -Dfile="%PROJECT_DIR%\lib\MultiSplit.jar" -DgroupId=org.jdesktop.swingx -DartifactId=multi-split -Dversion=1.0 -Dpackaging=jar
if errorlevel 1 (
	popd
	exit /b 1
)

call mvn install:install-file -Dfile="%PROJECT_DIR%\lib\utsapi2_0.jar" -DgroupId=gov.nih.nlm.uts -DartifactId=uts -Dversion=2.0 -Dpackaging=jar
if errorlevel 1 (
	popd
	exit /b 1
)

popd
echo [INFO] Local JAR installation complete.
exit /b 0