#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf .gradle app/build local.properties
if [ -x ./gradlew ]; then
  ./gradlew clean assembleDebug
else
  echo "Gradle wrapper scripts/JAR are not included in this project."
  echo "Open this folder in Android Studio, allow Gradle sync, then use Build > Clean Project and Build > Rebuild Project."
fi
