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

To speed up the build copy build-exmaple.properties in this folder to build.properties and enable the relevant
properties as describes in the comments in this file.

You can also enable spring.profiles.active=storage.file to use the file storage and see the reports generated by the
unit tests of the ibis-ladybug project in the Ladybug GUI.

To test your Ladybug changes with the Frank!Framework enable test.with.iaf=true. By default it will test with the webapp
module of the Frank!Framework. This can be changed with the iaf.module property. The frank-runner/specials folder
contains a folder per module that can have it's own build.properties with custom properties (see the build.xml for
possible values).

Please do the following to set up your development environment:
* Install [Node.js](https://nodejs.org/en/), choose version 16. You should get executables `npm` version 8.x and `node` version 16.x. Check these versions using `npm -v` and `node -v`.
* Change directory to your checkout of ladybug-frontend.
* The Node Package Manager (npm) includes another package manager, yarn. We use that one because it is more stable. Enable it by executing the command `corepack enable`.
* Use `yarn -v` to check that you have yarn version 3.2.x.
* Run `yarn prepare` to prepare Git hooks. If you do not do this, you will not be able to commit or push in the ladybug-frontend project.
* Run `yarn install --frozen-lockfile` to install your dependencies. The flag ensures that you get exactly the same dependencies as the other developers have. If you want to include a new dependency, update `package.json` and do `yarn install`. File `yarn.lock` will be updated, the list of all dependencies including dependencies of other dependencies. Check in `yarn.lock` to ensure that other developers will update to the same dependencies as you.

Test your environment as follows:
* Start the backend using `frank-runner/specials/ibis-ladybug` as explained before. Wait until Tomcat is up and running.
* Execute command `ng serve` from within the `ladybug-frontend` checkout. This wil make Node.js serve the Ladybug frontend files and proxy the Ladybug backend api as the src folder contains a proxy.conf.json (this will circumvent CORS related problems as the frontend served by Node.js on port 4200 would call the api on port 80). It is now possible to use both the WebJars packaged Ladybug frontend and the Ladybug frontend served by Node.js. The page on the following url (also mentioned above) contains links to both frontends: `http://localhost`.

To run the unit tests of the frontend, run the following command:

```
ng test
```

At the time of writing, there are no unit tests but there are end-to-end tests.

End-to-end testing is done using [Cypress](https://www.cypress.io/). If you are running Cypress for the first time run
the following command in the root folder of the Ladybug frontend project (in case you don't have a direct internet
connection you might need to set HTTPS_PROXY, see https://docs.cypress.io/guides/references/proxy-configuration). Cypress is a dependency configured in `package.json`, so it should have been installed when you did `yarn install --frozen-lockfile`. You have the following options for running the tests:
* `yarn run e2e`. This runs the end-to-end tests headless and without user interaction.
* `yarn run e2e-interactive`. This opens a Window from which you can choose what tests to start. You can select the webbrowser you want to test with.

In case nothing happens after "Compiled successfully" you might need to run set HTTP_PROXY= to prevent Cypress from connecting to locahost using your
proxy.

When the tests are running, you will see the ibis-ladybug GUI. The GUI will show the effects of the commands that are applied by the tests.

Create and publish NPM package and WebJar
========================================

### Prerequisites
- Make sure the latest changes are committed (and preferably pushed) to the project
- Open your terminal and navigate to your project folder (so `PATH/TO/ladybug`)
- If this is the first time, you need to log into NPM
    - Create an account on [NPM](https://www.npmjs.com/signup) if you don't have one yet
    - In the terminal run the following command `npm login`, which prompts you to log into NPM

### Creating and publishing an NPM package
- Run the command `ng build`, to build the current project
- Copy the following files into the generated `dist/ladybug` folder
    - README.md
    - LICENSE
    - package.json
- In the `dist/ladybug/index.html` change the types of the three scripts on line 15 to `application/javascript` (so module -> application/javascript). The specific scripts are:
  - `runtime.e3a101410a4894ca.js`
  - `polyfills.42dedf2fcdca615b.js`
  - `main.134a36bdfc8f0ad9.js`
- Remove the `dependencies` and `devDependencies` from the copied `package.json` (`dist/ladybug/package.json`). Make sure you do not remove them from the original `package.json`.
- Navigate to `dist/ladybug` in your terminal
- Run `npm publish`
- Run `git tag vX.Y.Z`, with X.Y.Z being the latest version of the package, which you can find in your `package.json` (for example `0.0.12`)
- Run `git push origin vX.Y.Z`, with X.Y.Z being the version you just specified.
- You have now published the NPM package

### Creating and publishing WebJar
- Navigate to [WebJars](https://webjars.org)
- Click on the `Add a WebJar` button
- Select the type `NPM`
- As package name, type in: `@wearefrank/ladybug`
- Select the correct version
- Click on the `Deploy` button to deploy the WebJar. Wait for the Deploy Log to finish (there should be a `Deployed!` somewhere).
- You have now published the WebJar

### Prepare for next cycle
- Go into the file `ladybug-frontend/package.json` and increment the version number (for example `0.0.12` -> `0.0.13`)
- Commit and push the changes in `ladybug-frontend/package.json`, with the commit message `Set version to X.Y.Z for the next development cycle`
