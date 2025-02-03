# How to contribute to Ladybug

This document helps you to contribute to Ladybug. It focuses on how to start and test Ladybug. Ladybug is related to many different GitHub projects. There are multiple ways to start Ladybug that each have their own benefits for testing. Finally, it is explained what automated tests exist and how to maintain them.

# Overview of Ladybug and GitHub projects

Ladybug is a debugger that is intended for Java applications. A Java application debugged by Ladybug is expected to process incoming messages. These messages undergo a series of transformations. The Java application is expected to report about these to Ladybug, resulting in *checkpoints*. Each message received by the Java application results in a Ladybug *report*. Ladybug allows its users to *rerun* reports. This allows reports to be used as test cases.

Ladybug is treated as a stand-alone product that can be used by any Java application. An important target application however is the Frank!Framework, https://github.com/frankframework/frankframework; the Frank!Framework is shipped with Ladybug included (as Maven dependency). To test Ladybug as an independent project, we include it in the a small test application ladybug-test-webapp, https://github.com/wearefrank/ladybug-test-webapp. It provides links to generate Ladybug reports and provides access to the Ladybug user interface. The first choice on starting Ladybug is whether to combine it with the Frank!Framework or not.

The next choice for starting Ladybug has to do with the front-end. The project you are looking at, the Ladybug backend, includes a front-end based on library Echo2. This library is end-of-life, so a new front-end, GitHub project ladybug-frontend, is being developed that is based on Angular. This is project https://github.com/wearefrank/ladybug-frontend. Ladybug is shipped with ladybug-frontend included (as Maven dependency). For testing purposes, it is often useful to start a separate instance of ladybug-frontend using NodeJS. This instance updates dynamically when the source code of ladybug-frontend is changed, allowing for fast development. This can be done both for Ladybug without Frank!Framework as for Ladybug as part of the Frank!Framework. In the latter case, the ladybug-frontend is shown *popped-out*, without the main menu of the Frank!Framework next to it.

The next choice has to do with fine-tuning the Frank!Framework. The Frank!Framework uses Spring to configure Ladybug. Users of the Frank!Framework change this configuration, for example to add columns to the table in the debug tab. We use another test project, ladybug-ff-test-webapp (https://github.com/wearefrank/ladybug-ff-test-webapp), to test alternative Spring configurations. This is small Java project that has the Frank!Framework as a Maven dependency.

Finally, there is the choice whether to build the code locally or to use pre-built artifacts from the Nexus server managed by the maintainers of the Frank!Framework (or the Nexus server Maven Central for official releases of the Frank!Framework). Local builds are typically SNAPSHOT versions while Nexus releases do not. Nightly builds have a timestamp in their version number while official releases are of the form x.y.z without SNAPSHOT and without a timestamp.

Apart from the GitHub projects introduced so far, we have ladybug-ff-cypress-test (https://github.com/wearefrank/ladybug-ff-cypress-test). This project holds Cypress tests that test Ladybug as a part of the Frank!Framework. Project ladybug-frontend, the Angular frontend of Ladybug, has Cypress tests that test Ladybug as a standalone product.

The final project to mention is the Frank!Runner (https://github.com/wearefrank/frank-runner). This project was introduced to start the Frank!Framework, which is not trivial because the Frank!Framework (and also ladybug-test-webapp and ladybug-ff-test-webapp) have been developed to run in an application server, typically Apache Tomcat. The Frank!Runner has been extended over the years to support many different ways to run Ladybug. Starting a separate instance of ladybug-frontend is not done by the Frank!Runner however; that is supported by project ladybug-frontend itself.

Here is a summary of all these projects:

- https://github.com/frankframework/frankframework. An important Java application that uses the debugger Ladybug.
- https://github.com/wearefrank/ladybug (the project you are currently looking at). The backend of Ladybug and the Echo2-based frontend (Echo2 is end-of-life).
- https://github.com/wearefrank/ladybug-frontend. The Angular frontend being developed and Cypress tests that test Ladybug as a stand-alone project.
- https://github.com/wearefrank/ladybug-test-webapp. Simple web application that wraps Ladybug to test it as a stand-alone product.
- https://github.com/wearefrank/ladybug-ff-test-webapp. Small Java project that includes the Frank!Framework as a dependency - provides alternative Spring configurations to test with.
- https://github.com/wearefrank/ladybug-ff-cypress-test. Cypress tests that test Ladybug as part of the Frank!Framework.
- https://github.com/wearefrank/frank-runner. Tool to start different configurations of Ladybug and the Frank!Framework.

The figure below shows how these are related as Maven dependencies:

![Contributing pictures](././contributing-pictures/dependencies-related-to-ladybug.jpg)

To let all mentioned projects cooperate, please check them out in a common parent directory. The Frank!Runner assumes that the projects are checked out that way.

# Running Ladybug

Setups without ladybug-ff-test-webapp of Ladybug and/or Frank!Framework require the following steps to run:

1. Ensure that the `pom.xml` files of the projects reference each other. The only exception is a ladybug-frontend instance starting separately with NodeJS. Not relevant if a pre-built Frank!Framework version is used.
2. Configure the Frank!Runner to properly do the Maven build and to properly start Apache Tomcat.
3. Run the Maven build of ladybug-frontend separately (is not automated by the Frank!Runner) if applicable.
4. Start the appropriate `restart.bat` script to build and run the Frank!Framework.
5. If applicable, configure and start the separate ladybug-frontend.

Setups with ladybug-ff-test-webapp should be run as follows:

1. Use steps 1-3 to of the previous list to build the Frank!Framework (if no pre-built version).
2. Update the `pom.xml` of ladybug-ff-test-webapp to reference the right Frank!Framework version.
3. Configure the build of ladybug-ff-test-webapp.
4. Run its `restart.bat` file to start it (delegates to the Frank!Runner to deploy the executable in Apache Tomcat).

### Adjusting `pom.xml`

This step has been partly automated. See directory `frank-runner/specials/util/syncPomVersions`. This directory holds an ANT script. There are `.sh` and `.bat` scripts to execute the goals of this ANT script. For example, under Linux (or MinGW) you can execute `run.sh` to adjust your checkouts of Ladybug and the Frank!Framework. It causes the `pom.xml` of your Ladybug checkout to reference your checkout of ladybug-frontend. It also adjusts `frankframework/ladybug/pom.xml` so that your checkout of the Frank!Framework references your checkout of ladybug.

### Configuring the Frank!Runner

To start a pre-existing build of the Frank!Framework (available as local Maven artifact or on a Nexus server), use the ANT script in the root directory of the Frank!Runner. That script loads properties from a file `build.properties` that should be placed in the root directory of the Frank!Runner. You can configure `projects.dir` and `project.dir` to reference to your Frank application. See the Frank!Runner README for details: https://github.com/wearefrank/frank-runner/blob/master/README.md.

To build Ladybug and the Frank!Framework locally, you have to work with folders `frank-runner/specials/ladybug` and `frank-runner/specials/iaf-webapp`. Both of these directories hold and ANT scripts that can be executed on the command line by `.bat` and `.sh` scripts. `frank-runner/specials/ladybug` runs the Maven build of the Ladybug checkout and applies options you provide in a file `build.properties`, see below. Then it delegates to `frank-runner/specials/iaf-webapp`.

The ANT script of `frank-runner/specials/iaf-webapp` runs the Maven build of the Frank!Framework and applies options you provide in a file `build.properties`, see below. After this, it delegates to the root directory of the Frank!Runner to deploy the Frank!Framework to Apache Tomcat and to start it.

Both ANT scripts can be configured by `build.properties` files that you put in `frank-runner/specials/ladybug` and `frank-runner/specials/iaf-webapp`. Examples of these `build.properties` files are provided in `build-example.properties` files. These files provide clear documentation of the properties you can configure and their meaning. Please copy each `build-example.properties` to the corresponding `build.properties` and use the comments to finish these files.

For `frank-runner/specials/iaf-webapp`, there is an important option that is not yet documented in `build-example.properties`. If you put `skip.start=true`, the ANT script does not delegate to the root directory of the Frank!Runner and hence only the Nexus artifact of the local checkout of the Frank!Framework is built. This is very useful if you want to run ladybug-ff-test-webapp.

