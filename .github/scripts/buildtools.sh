#!/bin/bash

buildtools_dir=~/buildtools
buildtools=$buildtools_dir/BuildTools.jar
spigot_regex='version\("spigot", "([0-9.]*)'

while read line
do
  if [[ $line =~ $spigot_regex ]]
  then
    spigot_version=${BASH_REMATCH[1]}
  fi 
done < ./settings.gradle.kts

set -e
echo "Installing Spigot: $spigot_version"

if ! [[ -d $buildtools_dir && -f $buildtools ]]; then
  mkdir $buildtools_dir
  wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar -O $buildtools
fi

java -jar $buildtools --rev "$spigot_version" --remapped