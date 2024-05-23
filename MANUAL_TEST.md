Manual Test
===========

Ladybug is tested by [automated tests](./README.md#cicd). These automated tests do not cover all features of Ladybug. This document presents tests to be done manually. Please perform these tests before making a release of ladybug or a release of [ladybug-frontend](https://github.com/wearefrank/ladybug-frontend).

# Contents

- [Preparations](#preparations)

# Preparations

The options you have to run ladybug are described in [the readme](./README.md#how-to-change-and-test-ladybug). Here we present more detailed instructions. Please do the following:

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
* Change directory to `work/frank-runner/specials/ladybug`.Copy `build-example.properties` to `build.properties`.
* Uncomment line `test.with.iaf=true` in the `build.properties` you created in the previous step. Uncomment some other lines if you want to speed up the build.
* Change directory to `work/ladybug-frontend/src`. Copy `proxy.ff.conf.json` to `proxy.conf.json`.
* Change directory to `work/frank-runner/specials/ladybug`. Run the command `./restart.bat`.
* Change directory to `work/ladybug-frontend`. Run the command `yarn install --immutable`.
* Execute the command `yarn ng serve`.
* To see ladybug, browse to `http://localhost:4200/`.
* To see the Frank!Framework, browse to `http://localhost`.