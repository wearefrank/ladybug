Ladybug debug- and test tool by WeAreFrank!
===========================================

Ladybug, developed and maintained by the integration company WeAreFrank!, adds message based debugging and message based
unit testing, system testing and regression testing to your Java application. Call Ladybug at certain checkpoints in
your code (either directly or using AOP) to generate tree based reports. Implement a rerun method to be able to rerun
reports and optionally stub certain checkpoints for regression testing.


# Contents

- [Releases](#releases)
- [Feedback](#feedback)
- [Ladybug online demo](#ladybug-online-demo)
- [How to use Ladybug](#how-to-use-ladybug)
- [How to incorporate Ladybug into your application or framework](#how-to-incorporate-ladybug-into-your-application-or-framework)
- [How to change and test Ladybug](#how-to-change-and-test-ladybug)
  - [General setup](#general-setup)
  - [Backend development](#backend-development)
  - [Testing backend changes with Frank!Framework](#testing-backend-changes-with-frankframework)
  - [Frontend development](#frontend-development)
  - [Testing frontend changes with the test webapp](#testing-frontend-changes-with-the-test-webapp)
  - [Testing frontend changes with Frank!Framework](#testing-frontend-changes-with-frankframework)
  - [Testing frontend changes with unit tests](#testing-frontend-changes-with-unit-tests)
- [How the frontend is published](#how-the-frontend-is-published)
- [OpenTelemetry](#opentelemetry)


Releases
========

See [release notes](RELEASES.md).

Feedback
========

For bug reports and feature requests create a new issue at <https://github.com/wearefrank/ladybug/issues>. Ladybug
is developed and maintained by [WeAreFrank!](https://wearefrank.nl/). Contact us at <https://wearefrank.nl/en/contact>
or email to info@wearefrank.nl.

Ladybug online demo
===================

To see Ladybug in action as part of the Frank!Framework go to:

- https://frank2example.frankframework.org/iaf/gui/#/testing/ladybug

How to use Ladybug
==================

Ladybug is incorporated into the Frank!Framework and as such documented as part of the Frank!Manual. For a quick
introduction to Ladybug, read:

- https://frank-manual.readthedocs.io/en/latest/operator/ladybug.html

For a detailed explanation please read (which will also explain the use of Ladybug as a test tool):

- https://frank-manual.readthedocs.io/en/latest/testing/ladybug/ladybug.html


How to incorporate Ladybug into your application or framework
=============================================================

There are two main area's to consider, the actual logging/debugging of business logic and the configuration/integration
of Ladybug into your project.

The first and most interesting area is the actual logging/debugging of business logic. This is similar to code that you
write for logging. When we look at the following code in the ZGWClient.java of the Open ZaakBrug we can see that the
Debugger (a wrapper around the Ladybug TestTool class) is initialized in the same way as a normal Logger. Logging the
json for the POST is also similar except that the debugger will return an object (which a logger doesn't). This is
because the user of the Ladybug GUI can decide to stub a checkpoint in which case the Ladybug will return a different
json than it got as a parameter value. Every startpoint (which will make the tree indent one level) must have a
corresponding endpoint (to make the indent go back one level). In this case the endpoint is using a Lambda function so
that in case of stubbing not only the Ladybug will return a different value but it will also skip the actual code of
doing a post to another system (which at the time of running a test might not be connected and would give an error).
Other than using Lambda's adding checkpoint statements are pretty straight forward an similar to using a traditional
logger.

```
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final Debugger debug = Debugger.getDebugger(MethodHandles.lookup().lookupClass());
	...
	json = debug.startpoint(debugName, json);
	url = debug.inputpoint("url", url);
	log.debug("POST: " + url + ", json: " + json);
	...
	String zgwResponse = (String) debug.endpoint(debugName, () -> {
			...
			String response = this.restTemplateService.getRestTemplate().postForObject(finalUrl, entity, String.class);
			...
			return response;
		});
```

The second area to consider while integrating Ladybug into your project is the Spring configuration and enabling of the
servlets needed to serve the Ladybug frontend. This can basically be done in two ways (direct and using 
[AOP](https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/core.html#spring-core)). We hope that the
following examples for both methods are enough to get you running. If not, don't hesitate to [contact us](#feedback) for
help.

Direct integration of the Ladybug has been done in the Ladybug test webapp:

- https://github.com/wearefrank/ladybug-test-webapp

The Quarkus application to demo and test Ladybug:

- https://github.com/wearefrank/ladybug-quarkus

And the Open ZaakBrug:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug

The first two project do not contain many files, so you can easily look at all the files and get an idea of how Ladybug
has been integrated. The [next section](#how-to-change-and-test-ladybug) describes how to run the Ladybug test webapp.
For the Open ZaakBrug the most important files to look at are:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/Application.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/resources/spring-ladybug.xml
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/tree/master/src/main/java/nl/haarlem/translations/zdstozgw/debug
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/controller/SoapController.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/translation/zgw/client/ZGWClient.java

The Frank!Framework is using AOP to integrate the Ladybug, see the Ladybug module of the Frank!Framework:

- https://github.com/frankframework/frankframework/tree/master/ladybug

And the AOP related spring configuration xml:

- https://github.com/frankframework/frankframework/blob/master/ladybug/src/main/resources/springIbisDebuggerAdvice.xml

The [next section](#how-to-change-and-test-ladybug) also describes how to run Ladybug with the Frank!Framework.


How to change and test Ladybug
==============================

General setup
-------------

Clone the following projects to your Git folder or any other folder of your choice:

- https://github.com/wearefrank/ladybug (backend code) (the project you are currently looking at)
- https://github.com/wearefrank/ladybug-frontend (frontend code, not needed when making backend changes)
- https://github.com/wearefrank/ladybug-test-webapp (webapp to test Ladybug)
- https://github.com/wearefrank/frank-runner (building and running the test webapp with Ladybug)
- https://github.com/frankframework/frankframework (in case you want to test your Ladybug changes with the Frank!Framework)

Backend development
-------------------

Read the Frank!Runner [README.md](https://github.com/wearefrank/frank-runner#frankrunner) to learn how to integrate it
with your IDE but you can also just start the restart.bat in the `frank-runner/specials/ladybug` folder. This should
build and run the test webapp with Ladybug. When Tomcat has been started by the Frank!Runner browse to:

- http://localhost

This should display a page with links to the various resources available like the Ladybug frontend and API.

To speed up the build copy `build-example.properties` in folder `frank-runner/specials/ladybug` to `build.properties` and enable the relevant
properties as describes in the comments in this file. If you uncomment `test.with.iaf=true` as explained in the next section [Testing backend changes with Frank!Framework](#testing-backend-changes-with-frankframework), you will also build the Frank!Framework,
by default using the ANT script of `frank-runner/specials/iaf-webapp`. You can speed up the build of the FF! by copying
`build-example.properties` in folder `frank-runner/specials/iaf-webapp` to `build.properties` and by then editing that
`build.properties`.

In `frank-runner/specials/ladybug/build.properties`, you can also enable `spring.profiles.active=storage.file` to use the file storage and see the reports generated by the
unit tests of the ladybug project in the Ladybug GUI.

When a pull request is made on https://github.com/wearefrank/ladybug, a GitHub Action is run that triggers the Cypress tests of the frontend. Please go to https://github.com/wearefrank/ladybug-frontend/actions to see whether these tests succeed. These are the Cypress tests described in section [Testing frontend changes with unit tests](#testing-frontend-changes-with-unit-tests).

Testing backend changes with Frank!Framework
--------------------------------------------

To test your Ladybug changes with the Frank!Framework enable `test.with.iaf=true`. By default, it will test with the webapp
module of the Frank!Framework. This can be changed with the `iaf.module` property. The `frank-runner/specials` folder
contains a folder per module that can have it's own `build.properties` with custom properties. To see possible values, copy from
the corresponding `build-example.properties` as explained in the previous section [Backend development](#backend-development).

Frontend development
--------------------

Please do the following to set up your development environment for the front-end:
* Install [Node.js](https://nodejs.org/en/), choose version 20. You should get executables `npm` version 10.x and `node` version 20.x. Check these versions using `npm -v` and `node -v`.
* Change directory to your checkout of ladybug-frontend.
* The Node Package Manager (npm) includes another package manager, pnpm. We use that one because it is more stable. Enable it by executing the command `corepack enable`. You should do this in a command prompt that runs with administrator permissions.
* Use `pnpm -v` to check that you have the right version of pnpm. The version you are using should equal the version that is asked in `package.json`. In `package.json` you have a line like: `"packageManager": "pnpm@x.x.x"`.
* Run `pnpm install --frozen-lock` to install your dependencies. The flag ensures that you get exactly the same dependencies as the other developers have. If you want to include a new dependency, update `package.json` and do `pnpm install`. File `pnpm-lock.yaml` will be updated, the list of all dependencies including dependencies of other dependencies. Check in `pnpm-lock.yaml` to ensure that other developers will update to the same dependencies as you. If it is not your intention to introduce a new dependency, then do not check-in `pnpm-lock.yaml`!. If you made an error with these installation instructions, you may have unwanted updates of the file.
* In case you don't have a direct internet connection you might need to set HTTPS_PROXY, see https://docs.cypress.io/guides/references/proxy-configuration.
* Run `pnpm prepare` to prepare Git hooks. If you do not do this, you will not be able to commit or push in the ladybug-frontend project.

Testing frontend changes with the test webapp
---------------------------------------------

* Start the backend using `frank-runner/specials/ladybug` as explained at [Backend development](#backend-development). This is without `test.with.iaf`. Wait until Tomcat is up and running.
* Execute command `pnpm start` or `pnpm ng serve` from within the `ladybug-frontend` checkout. This will make Node.js serve the Ladybug frontend files and proxy the Ladybug backend api as the src folder contains a proxy.conf.json (this will circumvent CORS related problems as the frontend served by Node.js on port 4200 would call the api on port 80). It is now possible to use both the real frontend that is eventually seen by end users (on port 80) and the Ladybug frontend served by Node.js (on port 4200). The page on the following url (also mentioned above) contains links to both frontends: `http://localhost`.

Testing frontend changes with Frank!Framework
---------------------------------------------

The Frank!Framework incorporates Ladybug and configures it a bit different then the test webapp. It for example adds a Gray box view which you might want to test. The Frank!Framework serves the Ladybug on a different url than the test webapp. If you want to run the Ladybug frontend in combination with the Frank!Framework, start it using `pnpm startWithFF`. Alternatively, you can run the Maven build of the ladybug frontend and adjust the backend's `pom.xml` locally to reference your locally built ladybug-frontend artifact. In case you also have backend changes you can start the Frank!Framework as eplained above (with test.with.iaf=true). Otherwise it is easier to for example run Frank2Example1. See the Frank!Runner [README.md](https://github.com/wearefrank/frank-runner#frankrunner) for more information on how to start a Frank2Example1 or another Frank.

Testing frontend changes with unit tests
----------------------------------------

To run the unit tests of the frontend, run the following command:

```
pnpm ng test
```

At the time of writing, there are no unit tests but there are end-to-end tests.

End-to-end testing is done using [Cypress](https://www.cypress.io/). Cypress is a dependency configured in `package.json`, so it should have been installed when you did ``pnpm install --frozen-lock``. You have the following options for running the tests:
* `pnpm run e2e`. This runs the end-to-end tests headless and without user interaction.
* `pnpm run e2e-interactive`. This opens a Window from which you can choose what tests to start. You can select the webbrowser you want to test with.

In case nothing happens after "Compiled successfully" you might need to run `set HTTP_PROXY=` to prevent Cypress from connecting to locahost using your
proxy.

When the tests are running, you will see the ladybug GUI. The GUI will show the effects of the commands that are applied by the tests.

How the frontend is published
=============================

Customers do not start the frontend using `pnpm ng serve`. They have access to the frontend because it is a Maven dependency of the backend, the project you are currently looking at. The backend `pom.xml` references the frontend as follows:

```
<dependency>
  <groupId>org.wearefrank</groupId>
  <artifactId>ladybug-frontend</artifactId>
  <version>${frontend.version}</version>
</dependency>
```

At the start of the backend `pom.xml`, the frontend version to use is configured by giving property `frontend.version` a value.

The GitHub project of the frontend, https://github.com/wearefrank/ladybug-frontend has a `pom.xml` file that lets you build this artifact. The maintainers of Ladybug have a Jenkins server that automatically runs the Maven build when a commit is pushed on the master branch. This build publishes a new version at a proprietary Nexus repository https://nexus.frankframework.org/.

> [!NOTE]  
> The Maven build uses the Angular build to transpile the Typescript code and then zips the result into a .jar file.

What version number will be published by the Jenkins server? In the frontend `pom.xml` the version number should be a SNAPSHOT version, e.g. `0.1.0-SNAPSHOT` (value of property `revision` as defined in the frontend `pom.xml`). The build on Jenkins will replace the word `SNAPSHOT` by a timestamp. If the frontend `pom.xml` has version `0.1.0-SNAPSHOT` and if the Jenkins build starts at July 1 2024, 15:00:00, the published version will be `0.1.0-20240701.150000`. If you want to use that code as the official frontend of ladybug, update the backend `pom.xml` to reference that version.

You can also test your frontend code as Maven artifact before merging your code with the master branch. To do this, you need Maven 3.9.6 or later running on Java 17 or Java 21. Run `mvn clean install` in your checkout of the frontend. This will build the frontend artifact with exactly the version number in `pom.xml` (e.g. `0.1.0-SNAPSHOT`, no timestamp in this case) and install it on your development device. You can temporarily update the backend `pom.xml` to reference that frontend version. If you then start ladybug as described before, your work is reachable at port 80 (not 4200). You can test your work as explained in the sections on backend development.

> [!WARNING]  
> When you run the Maven build on your development device, it will update `package.json`. Please do not check in that change. Otherwise, the build will not work for other developers anymore.

OpenTelemetry
=============

Ladybug is able to create telemetry data from Ladybug reports and send it to a telemetry-collector. A telemetry-collector provides an overview based on time that helps to detect latency problems. There are two collectors available to send the data to: Zipkin and Jaeger. To use Zipkin, you can run the following command: 

`docker run --rm -d -p 9411:9411 --name zipkin openzipkin/zipkin`

To work with Jaeger, you can run the following command: 

`docker run --rm -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 -p 16686:16686 -p 4317:4317 -p 4318:4318 -p 9411:9411 jaegertracing/all-in-one:latest`

To choose between one of the collectors in the Ladybug application, there is a bean available to make your choice. You have to add the following and change the string value of this bean to the collector you want to use. For Zipkin, enter the endpoint in the string value. For Jaeger (which doesn't use a endpoint), you can just enter "jaeger":
```
<bean name="openTelemetryEndpoint" class="java.lang.String">
		<constructor-arg value=""/>
</bean>
```
You can enable this bean in the springTestToolWebApp.xml file in the ladybug-test-webapp project. This is a simple webapp to test the Ladybug application. See https://github.com/wearefrank/ladybug-test-webapp. When a Ladybug report is created, a trace will now be sent to the chosen collector.

Collecting OpenTelemetry data
=============================

In Ladybug, there is also an API available to gather telemetry data from OpenTelemetry. When code is instrumented with the OpenTelemetry library, it is possible to use the endpoint from this API to gather it in Ladybug. For a manual OpenTelemetry-instrumentation, you can configure the Zipkin exporter and make use of the endpoint to Ladybug. See code example below:

```
SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
.addSpanProcessor(BatchSpanProcessor.builder(ZipkinSpanExporter.builder().setEndpoint("http://localhost/ladybug/api/collector/").build()).build())
.setResource(resource)
.build();
```

For more info about OpenTelemetry, see https://opentelemetry.io/