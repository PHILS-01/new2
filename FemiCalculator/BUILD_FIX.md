# Build Fix Notes

This project has been cleaned so it does not contain stale Gradle/Android build output.

The reported error was:
`activity_equations.xml:15: error: resource string/title_equations not found`

The current source already defines `title_equations` in:
`app/src/main/res/values/strings.xml`

and the current source tree does not contain `activity_equations.xml`. This means the reported `activity_equations.xml` came from an older/stale build state rather than the current source tree.

## Before building

1. Open the `FemiCalculator` folder in Android Studio.
2. Let Gradle sync finish.
3. If Android Studio asks for the Android SDK, allow it to configure the installed SDK.
4. Use **Build > Clean Project**.
5. Use **Build > Rebuild Project**.
6. If the old `activity_equations.xml` error still appears, close Android Studio and delete the project's `.gradle` and `app/build` directories, then reopen and rebuild.

Do not add a second `title_equations` string; the required resource is already present.

## SDK warning

If you see:
`sdk.dir property in local.properties file. Problem: Directory does not exist`

delete the project's `local.properties` file and reopen the project in Android Studio. Android Studio will recreate it using the SDK location configured on that computer.

## Gradle wrapper

The supplied project has `gradle-wrapper.properties` for Gradle 8.6, but the wrapper JAR was not included in the original ZIP. If Android Studio reports that the wrapper JAR is missing, use Android Studio's Gradle configuration or generate the wrapper from a machine with Gradle 8.6 installed.
