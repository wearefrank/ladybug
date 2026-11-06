This are Martijn's idea on testing the work of branch fix-caching. The checks fall into two categories:

## Checking that caches are done well

The work was done by Claude but it did not run anything in GitHub Actions. We intend to merge this work without this test and then follow-up by checking subsequent runs. We should merge at the end of a working day and then remove all existing caches. First check is that the restore cache jobs have run the next morning and that there are caches. Next we should make dummy pull requests and run these GitHub Actions on them. We should check that all caches are hit.

## Checking that the right specs and the right backends are run

For test-with-ff we should temporarily have the videos on. We should run with and without building the code. From the videos we can see that the right backends are running. The job outputs should show what specs ran. It is also likely that tests fail when there would be a mismatch between the backend and the specs.

For jaxrs and springmvc tests, reviewing the stored artifacts should do mutch already. It is wise to turn on videos temporarily so we can have a look what is happening.