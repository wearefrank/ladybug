Manual Test
===========

Ladybug is tested by [automated tests](./README.md#cicd). These automated tests do not cover all features of Ladybug. This document presents tests to be done manually. Please perform these tests before making a release of ladybug or a release of [ladybug-frontend](https://github.com/wearefrank/ladybug-frontend).

# Contents

- [Preparations](#preparations)
  - [Checkout](#checkout)
  - [Configure Frank!Runner to run backend](#configure-frankrunner-to-run-backend)
  - [Configure checked out ladybug-frontend](#configure-checked-out-ladybug-frontend)
  - [Start up](#start-up)
- [Tests](#tests)
  - [Test 10: Debug tab tree view, layout of checkpoints](#test-10-debug-tab-tree-view-layout-of-checkpoints)

# Preparations

The options you have to run ladybug are described in [the readme](./README.md#how-to-change-and-test-ladybug). Here we present more detailed instructions. 

    WARNING: The instructions below were mostly executed using a MinGW command
    prompt that emulates Linux. If you use a DOS prompt or PowerShell, please
    use equivalent commands that are valid in your shell instead of literally
    using the shown commands. The exeption is calling `restart.bat`, which you
    probably cannot do in a MinGW command prompt. For readability, all paths
    are shown with `/` as the path separator, even though a DOS prompt uses
    `\`.

Please do the following:

### Checkout

* Create a work directory for performing these tests, say `work`.
* Open a command prompt and change directory to `work`.
* Clone the Frank!Runner, the Frank!Framework, ladybug and ladybug-frontend with the following commands:

  * `git clone https://github.com/wearefrank/frank-runner`.
  * `git clone https://github.com/frankframework/frankframework`.
  * `git clone https://github.com/wearefrank/ladybug`.
  * `git clone https://github.com/wearefrank/ladybug-frontend`.
* Check out the versions you want for the F!F, ladybug and ladybug-frontend. You can not choose arbitrarily what vesions to combine. A combination of versions is viable if:

  * The ladybug backend version is in `work/frankframework/ladybug/pom.xml`, the line `<ladybug.version>...</ladybug.version>` under `<properties>`. That value should be the artifact version mentioned in `work/ladybug/pom.xml`.
  * The ladybug-frontend you checked out should be compatible with the ladybug version you checked out. There does not have to be a match for the version numbers.
* Create file `ladybug/skip-replace-inject.txt`. It can have arbitrary contents and you can also leave it empty.

### Configure Frank!Runner to run backend

* Change directory to `work/frank-runner/specials/ladybug`.Copy `build-example.properties` to `build.properties`.
* Uncomment line `test.with.iaf=true` in the `build.properties` you created in the previous step. Uncomment some other lines if you want to speed up the build.
* Change directory to `work/frank-runner/specials/iaf-webapp`.
* Copy `build-example.properties` to `build.properties`.
* Search for the line `# configurations.dir=...`. Replace it by `configurations.dir=../../frankframework/webapp/src/main/resources/configurations`.
* Uncomment some lines of `build.properties` to speed up the build of the FF!.
* Copy the input configurations that belong to this test: Change directory to `work/ladybug/manual-test` and do `cp -r configurations ~/testLadybug/frankframework/webapp/src/main/resources/`. We choose to have all Frank configuration files together.

### Configure checked out ladybug-frontend

* Change directory to `work/ladybug-frontend/src`. Copy `proxy.ff.conf.json` to `proxy.conf.json`.

### Start up

* Change directory to `work/frank-runner/specials/ladybug`. Run the command `./restart.bat`.
* Change directory to `work/ladybug-frontend`. Run the command `yarn install --immutable`.
* Execute the command `yarn ng serve`.
* To see ladybug, browse to `http://localhost:4200/`.
* To see the Frank!Framework, browse to `http://localhost`.

# Tests

### Test 10: Debug tab tree view, layout of checkpoints

**Step 10:** In the Frank!Framework, use "Test a Pipeline" to run adapter "processXml" with input message `<person>Jan Smit</person>
`.

**Step 20:** In the Frank!Framework, use "Test a Pipeline" to run adapter "processXml" with input message `<person2>Jan Smit</person2>
`.

**Step 30:** In Ladybug (on port 4200), go to the Debug tab. Press the refresh button to refresh the table of reports. Check that the following are true:

* The last two reports have in their "Name" column the value `Pipeline processXml`.
* The second-last report is green to indicate success.
* The last report is red to indicate failure.

**Step 40:** Open the second-last report (the successful one). It should appear in the tree view below the table. Check that at least the following nodes exist with the shown indentation:

* Directory icon `Pipeline processXml`.
  * Right arrow `Pipeline processXml`.
    * Right arrow `Pipe validate`.
      * Information `Pipe validate`.
      * Left arrow `Pipe validate`.
    * Right arrow `Pipe getPersonNamePipe`.
      * Information `Pipe getPersonNamePipe`.
      * Right arrow `Sender getPersonNameSender`.
        * Right arrow `Pipeline getPersonName`.
          * Pipe `Pipe applyXslt`.
            * Information `Pipe applyXslt`.
            * Information `./getName.xsl`.
            * Left arrow `Pipe applyXslt`.
          * right arrow `exit state`.
          * right arrow `Pipeline getPersonName`.
        * Left arrow `getPersonNameSender`.
      * Left arrow `getPersonNamePipe`.
    * Left arrow `Pipeline processXml`.

**Step 50:** Are there nodes in addition to the ones shown in **Step 40**? Are these to be expected?

**Step 60:** Collapse and expand the nodes. Does this look good?

**Step 70:** Click each node and check its value as shown in the pane to the right of the tree. Does each node have a meaningful value?

**Step 100:** In the report table, open the last report (the unsuccessful one). It should appear in the tree view below the table. Check that at least the following nodes exist with the shown indentation:

* Directory icon `Pipeline processXml`.
  * Right arrow `Pipeline processXml`.
    * Right arrow `Pipe validate`.
      * Information `Pipe validate`.
      * Red cross `Pipe validate`.
    * Red cross `Pipeline processXml`.

**Step 110:** Are there nodes in addition to the ones shown in **Step 40**? Are these to be expected?

**Step 120:** Collapse and expand the nodes. Does this look good?

**Step 130:** Click each node and check its value as shown in the pane to the right of the tree. Does each node have a meaningful value?
