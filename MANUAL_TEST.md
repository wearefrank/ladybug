Manual Test
===========

Ladybug is tested by [automated tests](./README.md#cicd). These automated tests do not cover all features of Ladybug. This document presents tests to be done manually. Please perform these tests before making a release of ladybug or a release of [ladybug-frontend](https://github.com/wearefrank/ladybug-frontend).

# Contents

- [Preparations](#preparations)
  - [Bruno](#bruno)
  - [Checkout](#checkout)
  - [Configure Frank!Runner to run backend](#configure-frankrunner-to-run-backend)
  - [Start up](#start-up)
- [Tests](#tests)
  - [Test 10: Debug tab tree view, layout of checkpoints](#test-10-debug-tab-tree-view-layout-of-checkpoints)
  - [Test 20: Views in the debug tree](#test-20-views-in-the-debug-tree)
  - [Test 30: Authorization](#test-30-authorization)

# Preparations

The options you have to run ladybug are described in [the readme](./README.md#how-to-change-and-test-ladybug). Here we present more detailed instructions. 

> [!WARNING]
> The instructions below were mostly executed using a MinGW command
> prompt that emulates Linux. If you use a DOS prompt or PowerShell, please
> use equivalent commands that are valid in your shell instead of literally
> using the shown commands. The exeption is calling `restart.bat`, which you
> probably cannot do in a MinGW command prompt. For readability, all paths
> are shown with `/` as the path separator, even though a DOS prompt uses
> `\`.

Please do the following:

### Bruno

Please install HTTP client Bruno. You need it to issue HTTP requests to Frank
configs. The requests to issue have been prepared along with this test
description. Get it from https://www.usebruno.com/.

### Checkout

* Create a work directory for performing these tests, say `work`.
* Open a command prompt and change directory to `work`.
* Clone the Frank!Runner, the Frank!Framework, ladybug and ladybug-frontend with the following commands:

  * `git clone https://github.com/wearefrank/frank-runner`.
  * `git clone https://github.com/frankframework/frankframework`.
  * `git clone https://github.com/wearefrank/ladybug`.
* Check out the versions you want for the F!F and ladybug. You can not choose arbitrarily what vesions to combine. The ladybug backend version used by the FF! is in `work/frankframework/ladybug/pom.xml`, the line `<ladybug.version>...</ladybug.version>` under `<properties>`. That value should be the artifact version mentioned in `work/ladybug/pom.xml`.

### Configure Frank!Runner to run backend

* Change directory to `work/frank-runner/specials/ladybug`.Copy `build-example.properties` to `build.properties`.
* Uncomment line `test.with.iaf=true` in the `build.properties` you created in the previous step. Uncomment some other lines if you want to speed up the build.
* Change directory to `work/frank-runner/specials/iaf-webapp`.
* Copy `build-example.properties` to `build.properties`.
* Search for the line `# configurations.dir=...`. Replace it by `configurations.dir=<path-to-ladybug-checkout-with-this-test-description>/manual-test/configurations`.
* Uncomment some lines of `build.properties` to speed up the build of the FF!.

### Start up

* Change directory to `work/frank-runner/specials/ladybug`. Run the command `./restart.bat`.

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
          * Pipe `Pipe checkForError`.
            * Information `Pipe checkForError`.
            * Left arrow `Pipe checkForError`.
          * Left arrow `exit state`.
          * Left arrow `Pipeline getPersonName`.
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

### Test 20: Views in the debug tree

**Step 10:** Open Bruno and import the prepared requests as shown. The directory to select is in the checkout of this test description.

![Bruno open](./manual-test/brunoOpen.jpg)

![Bruno select](./manual-test/brunoSelect.jpg)

The result should be as shown:

![Bruno after open](./manual-test/brunoAfterOpen.jpg)

**Step 20:** Apply the request named "Conclusion - valid".

**Step 30:** Go to ladybug and press refresh.

**Step 40:** Open the most recent report from the table (top row) in the debug tree by clicking.

**Step 50:** Check that there is:

* a root node with "Pipeline" in the name;
* another "Pipeline" start node for the pipeline;
* a start node for every pipe;
* input and output points for the session keys;
* a start point for node "Sender sendToMundo" within "Pipe sendToMundo".

**Step 60:** In Ladybug select view "Black box".

**Step 70:** Check that only the following nodes are left:

TODO: Add screen capture, to be made when the black box view works.

TODO: Then continue writing this test.

**Step 200:** Stop the FF!. Under Windows you can do this by pressing Ctrl-C in the Tomcat window.

**Step 210:** Restart the FF! with another springIbisTestTool that is dedicated to testing views. In a command prompt in directory `work/frank-runner/specials/ladybug`, run `./restart.bat -Dcustom=TestLadybugReportTableDifferentViewsDifferentColumns`. This adds a view named `White box view with less metadata`. This view has only the following metadata columns: "storageId", "name" and "correlationId".

**Step 220:** Switch between the views. Check that the columns in the report table change according to the selected view.

**Step 230:** Play with setting filters and adjusting the selected views. Are you able to manipulate reports into the table that do not agree with the shown filter? Do the different indicators of the applied filter remain in sync?

> [!NOTE]
> You can apply a filter on a column that exists in the white box view but not in the white box view with less metadata. When you apply such a filter and then switch to the view with less metadata, the filter is allowed to remain. This is OK when there is a clear indicator that the filter is still applied and if it is possible (for example with the clear button) to remove all filters.

### Test 30: Authorization

**Step 10:** This test starts with the situation of test 10.

**Step 20:** In the Frank!Runner checkpoint, add the following to `build/<Apache Tomcat dir>/conf/catalina.properties`:

    application.security.testtool.authentication.type=IN_MEMORY
    application.security.testtool.authentication.username=Admin
    application.security.testtool.authentication.password=Nimda

**Step 30:** Start the Frank!Framework.

**Step 40:** Browse to the Frank!Console. You should be allowed to view this without logging in.

**Step 50:** Browse to Ladybug. You should see a login dialog to enter the credentials. After providing these, you should have access to Ladybug.

**Step 60:** Rerun a report. Check that rerunning is allowed and that rerunning succeeds for a report that succeeded when created.
