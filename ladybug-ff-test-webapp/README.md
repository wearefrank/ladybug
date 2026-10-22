# ladybug-ff-test-webapp

Runs Ladybug inside the Frank!Framework. The Ladybug backend is taken from this
repository (`ladybug-backend-springmvc`), so you can test your Ladybug changes
together with the Frank!Framework.

This module is not part of the default build. It is only included when Maven
profile `ff` is active.

## Running from IntelliJ

1. Open the Maven tool window (View | Tool Windows | Maven).
2. Expand **Profiles** and check **ff**.
3. Click **Reload All Maven Projects** if IntelliJ does not reload by itself.
4. Open `src/main/java/org/frankframework/runner/MainClass.java` and click the
   green run icon next to the `main` method.

## Building from the command line

```
mvn install -P ff
```
