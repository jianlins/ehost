#!/usr/bin/env bash
base="$PWD"
if [ ! -d "$base/target/release" ]; then
  mkdir target/release
fi
jdkbase=$base/../../../Downloads/jdks

value=`cat pom.xml`
find_version() {
  local s=$1 regex=$2
  if [[ $s =~ $regex ]]
    then
      echo "${BASH_REMATCH[1]}"
  fi
}
version=$(find_version "$value" "<version>([^<]+)")
echo $version
cd target/deploy

#sed -i 's/java/jre\/bin\/java/g' run
#sed -i 's/java/jre\\bin\\java/g' run.bat
zip -r "../release/eHOST_${version}_win_jre.zip" "./"
zip -r "../release/eHOST_${version}_mac_jre.zip" "./"
zip -r "../release/eHOST_${version}_linux_jre.zip" "./"


sed -i 's/jre\/bin\/java/java/g' run
sed -i 's/jre\\bin\\java/java/g' run.bat
zip -r "../release/eHOST_${version}_wo_jre.zip" "./"

cd $jdkbase/linux/jdk1.8/
zip -ur "$base/target/release/eHOST_${version}_linux_jre.zip" "./jre"
cd $jdkbase/win/jdk1.8/
zip -ur "$base/target/release/eHOST_${version}_win_jre.zip" "./jre"
cd $jdkbase/mac/jdk1.8/
zip -ur "$base/target/release/eHOST_${version}_mac_jre.zip" "./jre"
