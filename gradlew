#!/usr/bin/env sh
set -eu
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_VERSION=9.5.1
GRADLE_HOME="$DIR/.gradle/local/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$DIR/.gradle/local"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$DIR/.gradle/local/gradle.zip"
  unzip -q -o "$DIR/.gradle/local/gradle.zip" -d "$DIR/.gradle/local"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
