The test webapp can be run like:

```
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

This will set the server port. When the default port 80 suffices, do:

```
mvn spring-boot:run
```

If you want the UI to behave as though there is no authorization, add `-Dspring-boot.run.jvmArguments="-Dladybug.ui.test.mode=DONT_BLOCK_BACKEND"`. The backend still requires a login and enforces roles in this mode.

If you want to actually disable authorization in the backend too, add `-Dspring-boot.run.jvmArguments="-Dladybug.ui.test.mode=NO_AUTH"` instead.

To set a Spring profile and the port, use:

```
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8090 --spring.profiles.active=storage.file"
```