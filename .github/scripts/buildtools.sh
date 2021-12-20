#!/bin/bash

buildtools_dir=~/buildtools
buildtools=$buildtools_dir/BuildTools.jar
spigot_version=$(grep -oP '<spigot.version>\K.*?(?=-)' ./pom.xml)

set -e
echo "Installing Spigot: $spigot_version"

if ! [[ -d $buildtools_dir && -f $buildtools ]]; then
  mkdir $buildtools_dir
  wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar -O $buildtools
fi

java -jar $buildtools --rev "$spigot_version" --remapped