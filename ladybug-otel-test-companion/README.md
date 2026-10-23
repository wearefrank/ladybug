# Ladybug OpenTelemetry test companion

A small Spring Boot command line app that sends an OpenTelemetry trace to Ladybug over OTLP/HTTP, to test Ladybug's
OpenTelemetry support end-to-end. It is used by the Cypress test
`ladybug-frontend/cypress/e2e/open-telemetry/otel-test-companion.cy.ts`.

The trace consists of a root span `Otel root` with two child spans, `Otel here, hello world!` and
`Otel here, goodbye world`. The app sends the trace, then exits. The exit code is 0 when Ladybug accepted the trace and
non-zero otherwise.

## Prerequisites

The app needs a running Ladybug that accepts traces without authentication, for example
`ladybug-spring-boot-test-webapp` started with `NO_AUTH`. From directory `ladybug-spring-boot-test-webapp`:

```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dladybug.ui.test.mode=NO_AUTH"
```

This serves Ladybug on port 80. See the README of `ladybug-spring-boot-test-webapp` for other options. After running
the companion, refresh the Ladybug debug tab to see a report named `Otel root`.

## Configuration

The properties below are defined in `src/main/resources/application.properties`. Override them with command line
arguments like `--ladybug.otel.protocol=http/json`.

| Property                       | Default                                                     | Meaning                                                                                                                                         |
|--------------------------------|-------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `ladybug.otel.port`            | `80`                                                        | Port of the Ladybug webapp. Only used to compose the default of `ladybug.otel.endpoint`.                                                       |
| `ladybug.otel.endpoint`        | `http://localhost:${ladybug.otel.port}/ladybug/api/v1/traces` | OTLP/HTTP endpoint of Ladybug.                                                                                                                  |
| `ladybug.otel.protocol`        | `http/protobuf`                                             | `http/protobuf` or `http/json`.                                                                                                                 |
| `ladybug.otel.batching`        | `single-request`                                            | `single-request` sends the whole trace in one request. `hello-world-separate` first sends the `Otel here, hello world!` span, then the others. |
| `ladybug.otel.timeout-seconds` | `30`                                                        | Connect and request timeout.                                                                                                                    |

## Running from the command line

### With Maven

From this directory:

```
mvn spring-boot:run
```

Pass arguments with `-Dspring-boot.run.arguments`, for example:

```
mvn spring-boot:run -Dspring-boot.run.arguments="--ladybug.otel.port=8090 --ladybug.otel.protocol=http/json"
```

### As an executable jar

Build the jar from the root of the repository. Option `-am` also builds the parent pom, which is needed when it is not
in your local Maven repository yet:

```
mvn -pl ladybug-otel-test-companion -am package -DskipTests
```

This produces `ladybug-otel-test-companion/target/ladybug-otel-test-companion.jar`. The file name has no version, so
that the Cypress tests can find it. Run it like:

```
java -jar ladybug-otel-test-companion/target/ladybug-otel-test-companion.jar --ladybug.otel.protocol=http/json --ladybug.otel.batching=hello-world-separate
```

The Cypress tests run the jar in the same way. They set `ladybug.otel.endpoint` based on Cypress env variable
`backendServer`. Build the jar before running these tests.

## Running and debugging in IntelliJ

1. Open the root of the repository as a Maven project, so that IntelliJ knows module `ladybug-otel-test-companion`.
   The code uses Lombok, so make sure the Lombok plugin is installed and annotation processing is enabled
   (Settings | Build, Execution, Deployment | Compiler | Annotation Processors).
2. Open `src/main/java/org/wearefrank/ladybug/otel/companion/OtelTestCompanionApplication.java` and click the green
   run icon next to the `main` method. Choose **Run** or **Debug**. This creates a run configuration that you can
   reuse afterwards.
3. To change properties, edit the run configuration (Run | Edit Configurations...) and add them to
   **Program arguments**, for example `--ladybug.otel.port=8090 --ladybug.otel.protocol=http/json`.

To debug, set breakpoints, for example in `TraceSender.run()`, and start the run configuration with **Debug**. The app
exits as soon as the trace is sent, so breakpoints are the way to inspect it while it runs. To see what is sent,
put breakpoints in `TraceSender.sendProtobuf()` or `TraceSender.sendJson()`. To debug how Ladybug handles the trace,
also run the Ladybug webapp in debug mode and set breakpoints there.

Alternatively, debug the Maven run by attaching IntelliJ to it. Start the app with:

```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

Because of `suspend=y`, the app waits until a debugger attaches. In IntelliJ, create a run configuration of type
**Remote JVM Debug** with port 5005 and start it with **Debug**.
