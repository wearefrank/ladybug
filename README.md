Ladybug debug- and test tool by WeAreFrank!
===========================================

Ladybug, developed and maintained by the integration company WeAreFrank!, adds message based debugging and message based
unit testing, system testing and regression testing to your Java application. Call Ladybug at certain checkpoints in
your code (either directly or using AOP) to generate tree based reports. Implement a rerun method to be able to rerun
reports and optionally stub certain checkpoints for regression testing.


Releases
========

See [release notes](RELEASES.md).


Feedback
========

For bug reports and feature requests create a new issue at <https://github.com/ibissource/ibis-ladybug/issues>. Ladybug
is developed and maintained by [WeAreFrank!](https://wearefrank.nl/). Contact us at <https://wearefrank.nl/en/contact/>
or send an email to info@wearefrank.nl.


Ladybug online demo
===================

To see Ladybug in action as part of the Frank!Framework go to:

- https://ibis4example.ibissource.org/iaf/gui/#!/testing/ladybug

And in Open ZaakBrug as part of http://demodam.nl:

- https://open-zaakbrug.demodam.nl/debug


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

- https://github.com/ibissource/ibis-ladybug-test-webapp

And the Open ZaakBrug:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug

The Ladybug test webapp doesn't contain much files so you can easily look at all the files and get an idea of how
Ladybug has been integrated. The next paragraph describes how to run the Ladybug test webapp. For the Open ZaakBrug
the most important files to look at are:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/Application.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/resources/spring-ladybug.xml
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/tree/master/src/main/java/nl/haarlem/translations/zdstozgw/debug
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/controller/SoapController.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/translation/zgw/client/ZGWClient.java

The Frank!Framework is using AOP to integrate the Ladybug, see the Ladybug module of the Frank!Framework:

- https://github.com/ibissource/iaf/tree/master/ladybug

And the AOP related spring configuration xml:

- https://github.com/ibissource/iaf/blob/master/ladybug/src/main/resources/springIbisDebuggerAdvice.xml

The next paragraph also describes how to run Ladybug with the Frank!Framework.


How to change and test Ladybug
==============================


Clone the following projects to your Git folder or any other folder of your choice:

- https://github.com/ibissource/ibis-ladybug (backend code) (the project you are currently looking at)
- https://github.com/ibissource/ladybug-frontend (frontend code, not needed when making backend changes)
- https://github.com/ibissource/ibis-ladybug-test-webapp (webapp to test Ladybug)
- https://github.com/ibissource/frank-runner (building and running the test webapp with Ladybug)
- https://github.com/ibissource/iaf (in case you want to test your Ladybug changes with the Frank!Framework)

Read the Frank!Runner [README.md](https://github.com/ibissource/frank-runner#frankrunner) to learn how to integrate it
with your IDE but you can also just start the restart.bat in the frank-runner/specials/ibis-ladybug folder. This should
build and run the test webapp with Ladybug. When Tomcat has been started by the Frank!Runner browse to:

- http://localhost

This should display a page with links to the various resources available like the Ladybug frontend and API.

To speed up build time create a build.properties file in this folder with the following content:

```
#maven.skip.all=true

maven.skip.clean=true
maven.skip.tests=true
maven.skip.javadoc=true
maven.skip.recompile.all=true
maven.skip.source.jar=true

#build.echo2=true

#test.with.iaf=true
#iaf.module=webapp
#iaf.module=example
#iaf.module=test

#maven.verbose=true
```

To test your Ladybug changes with the Frank!Framework enable test.with.iaf=true. By default it will test with the webapp
module of the Frank!Framework. This can be changed with the iaf.module property. The frank-runner/specials folder
contains a folder per module that can have it's own build.properties with custom properties (see the build.xml for
possible values).

Install [Node.js](https://nodejs.org/en/) to serv the Ladybug frontend from the frontend project and test your changes
to the frontend code.

If you are running the Ladybug frontend with Node.js for the first time, you need to run the following command in the
root folder of the Ladybug frontend project, so that you have all necessary dependencies:

```
npm install
```

While having the Ladybug running with the Frank!Runner as described earlier execute the following command in the src
folder:

```
ng serve
```

This wil make Node.js serve the Ladybug frontend files and proxy the Ladybug backend api as the src folder contains a
proxy.conf.json (this will circumvent CORS related problems as the frontend served by Node.js on port 4200 would call
the api on port 80). It is now possible to use both the WebJars packaged Ladybug frontend and the Ladybug frontend
served by Node.js. The page on the following url (also mentioned above) contains links to both frontends:

- http://localhost

To run the unit tests of the frontend, run the following command:

```
npm test
```

At the time of writing, there are no unit tests but there are end-to-end tests.

End-to-end testing is done using [Cypress](https://www.cypress.io/). If you are running Cypress for the first time run
the following command in the root folder of the Ladybug frontend project:

```
npm install cypress
```

To run the Cypress tests using a graphical user interface, run the following command:

```
npm run e2e-interactive
```

This opens a window with a link that allows you to start the tests. You will see the ibis-ladybug GUI while the tests
run. The GUI will show the effects of the commands that are applied by the tests.

If you want to run the tests automatically without a GUI, then use the following command:

```
npm run e2e
```

Create NPM package and WebJar
=============================

The frontend is released as a NPM package and WebJar using the following steps:

- Commit changes to the frontend project
- npm login (one time action)
- ng build (in any folder of the project)
- Copy package.json, LICENSE and README.md in the root folder to the dist/ladybug folder
- Remove dependencies and devDependencies from dist/ladybug/package.json
- cd dist/ladybug
- npm publish
- git tag vX.Y.Z
- git push tag to origin
- Increment version number in package.json in the root folder
- Commit and push with message: Set version for next development cycle
- Goto https://webjars.org
- Push button Add a WebJar
- WebJar Type: NPM
- Package Name: @wearefrank/ladybug
- Select version
- Push button Deploy!
