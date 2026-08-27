This subdirectory of the Ladybug project tests a feature of the Frank!Framework and Ladybug together. When multiple instances of the Frank!Framework find each other through Hazelcast, then the Frank!Console shows a drop-down menu. The options in the drop-down denote the instances found by the Frank!Console, so each option corresponds to a value of property instance.name of a Frank!Framework instance. When an option is selected then Ladybug is refreshed. The debug tab of Ladybug should show the reports produced by the instance(s) that have the selected value of property instance.name. The instance by which a Ladybug report was created is in column Application.

Do the test as follows:
* When testing the latest Docker image of the FF!, remove the Frank!Framework images on your development computer. Otherwise, "latest" may point to a Docker image that is actually older.
* Set the `FF_VERSION` environment variable to the Frank!Framework version to test, e.g. on Windows PowerShell: `$env:FF_VERSION="<version>"; docker compose up --build --force-recreate` or on Linux/Mac: `FF_VERSION=<version> docker compose up --build --force-recreate`. If `FF_VERSION` is not set, "latest" is used.
* Browse to http://localhost:8091. The Frank!Console should not have the mentioned drop-down menu. The instance name should be "frank-other".
* Run the example configuration "frank-other-config" in Test a Pipeline.
* Check in Ladybug that there is a report.
* Browse to http://localhost:8090. The Frank!Console should have the mentioned drop-down. Select "frank-console-holder".
* Run example configuration "frank-console-holder-config"in Test a Pipeline.
* Go to Ladybug. Check that there is a filter that "Application" is "frank-config-holder" and that only reports from that instance.name are shown.
* In the Frank!Console drop-down, select "frank-other".
* Check that Ladybug now filters on Application = frank-other and that only reports of that instance are shown. No refresh or other workarounds should be needed to have the Frank!Framework refresh Ladybug after the drop-down is changed.