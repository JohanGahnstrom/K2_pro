@ECHO OFF
SET APP_HOME=%~dp0
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%CLASSPATH%" (
 ECHO gradle-wrapper.jar is not bundled. Open the project in Android Studio or run gradle wrapper --gradle-version 8.13.
 EXIT /B 1
)
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
