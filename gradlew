#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar is not bundled. Run 'gradle wrapper --gradle-version 8.13' once, or open the project in Android Studio." >&2
  exit 1
fi
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
