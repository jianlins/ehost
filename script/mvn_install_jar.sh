#!/usr/bin/env bash
mvn install:install-file -Dfile=lib/annotationadmin-integration-1.0.13.jar -DgroupId=gov.va.vinci -DartifactId=annotation-admin -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=lib/MultiSplit.jar -DgroupId=org.jdesktop.swingx \
        -DartifactId=multi-split -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=lib/utsapi2_0.jar -DgroupId=gov.nih.nlm.uts \
        -DartifactId=uts -Dversion=2.0 -Dpackaging=jar