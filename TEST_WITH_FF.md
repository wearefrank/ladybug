# Testing Ladybug together with the Frank!Framework

The GitHub Actions workflow [`.github/workflows/test-with-ff.js.yml`](.github/workflows/test-with-ff.js.yml)
("Test with FF!") runs the Cypress suite in
[`ladybug-ff-cypress-test`](ladybug-ff-cypress-test) against a running
Frank!Framework (FF!) instance that has the Ladybug debugger enabled. It always
checks out `wearefrank/ladybug` (or another backend branch, see below), but it
can get the Frank!Framework in two different ways. This is controlled by the
`workflow_dispatch` input `buildLatestFFandLadybugCode`:

- `buildLatestFFandLadybugCode = false` (the default, and what runs on every
  push to `master`): the FF! and Ladybug backend jars are **not** built from
  source. Frank!Runner starts an application built from the Frank!Framework
  release published on the WeAreFrank! Nexus server. The `ladybug/` checkout
  is only used for its Cypress tests, its `TestConfigurations`, and (when
  `test-app=true`) the small custom `ladybug-ff-test-webapp` project.
- `buildLatestFFandLadybugCode = true`: Frank!Framework is checked out (from
  `frankframework/frankframework`, or from `ffFork`/`ffBranch` if given) and
  built from source together with the checked-out Ladybug backend
  (optionally on `backendBranch`), using `frank-runner/specials/ladybug`.
  The inputs `ffFork`, `ffBranch` and `backendBranch` are only allowed when
  this flag is `true`; the workflow fails fast otherwise (see the three
  "Check that ... is only set if it can be applied" steps).

Regardless of this flag, the job runs under a matrix of `test-app`, `storage`
and `dtap`, with a few combinations excluded (`dtap=PRD` is never combined
with `test-app=true` or with `storage=database`). That leaves exactly five
combinations, which is why each table below has five (data) columns — one per
surviving matrix combination. The two tables below describe the same five
combinations, once for `buildLatestFFandLadybugCode=false` and once for
`buildLatestFFandLadybugCode=true`.

## Table 1 — Nexus release (`buildLatestFFandLadybugCode=false`)

| Aspect | Default config, LOC | | Default config, PRD | Test webapp | |
|---|---|---|---|---|---|
| | **File** | **Database** | **File** | **File** | **Database** |
| **Frank project** | Frank2Example1, started as-is by Frank!Runner | same | same | ladybug-ff-test-webapp, a thin custom webapp with its own test pipes | same |
| **Source of the Frank configurations** | TestConfigurations/configurationsDefault | same | same | TestConfigurations/configurationsTestWebapp | same |
| **Origin of the FF!/Ladybug code that actually runs** | Released FF! artifact from Nexus; Ladybug is whatever version that release depends on. Nothing is compiled from the frankframework or ladybug checkouts. | same | same | ladybug-ff-test-webapp is Maven-built locally, but it only compiles its own pipes; it depends on `frankframework-webapp:LATEST` from Nexus (which again brings its own Ladybug version) | same |
| **How the server is started** | Frank!Runner restart script ("official release") | same | same | ladybug-ff-test-webapp's own restart script | same |
| **Authentication** | None | None | Required: `dtap.stage=PRD`, YAML users, CSRF disabled | None | None |
| **Cypress specs run** | common-over-test-envs + default | same | common-over-test-envs + prd | common-over-test-envs + with-ladybug-ff-test-webapp | same |

## Table 2 — Build from checkouts (`buildLatestFFandLadybugCode=true`)

| Aspect | Default config, LOC | | Default config, PRD | Test webapp | |
|---|---|---|---|---|---|
| | **File** | **Database** | **File** | **File** | **Database** |
| **Frank project** | frankframework webapp module (tests still compare against Frank2Example1's original files) | same | same | ladybug-ff-test-webapp | same |
| **Source of the Frank configurations** | TestConfigurations/configurationsDefault | same | same | TestConfigurations/configurationsTestWebapp | same |
| **Origin of the FF!/Ladybug code that actually runs** | Built from source: frankframework checkout (default repo, or `ffFork`/`ffBranch`) together with the ladybug checkout (optionally `backendBranch`); pom.xml files are synced so the two reference each other, then compiled together with Maven | same | same | Same FF!/Ladybug source build, but pom.xml files are synced so ladybug-ff-test-webapp resolves `frankframework-webapp` from the locally-built artifacts instead of `LATEST` on Nexus | same |
| **How the server is started** | Frank!Runner restart script builds and starts FF! + Ladybug together | same | same | Two steps: first the FF!+Ladybug build is done and installed into the local Maven cache without starting a server, then ladybug-ff-test-webapp is built and started against those local artifacts | same |
| **Authentication** | None | None | Required: `dtap.stage=PRD`, YAML users, CSRF disabled | None | None |
| **Cypress specs run** | common-over-test-envs + default | same | common-over-test-envs + prd | common-over-test-envs + with-ladybug-ff-test-webapp | same |

## Notes on the test environment setup

- **`configurations` is a matrix `include` value**, not a top-level matrix
  axis: it is `configurationsDefault` when `test-app=false` and
  `configurationsTestWebapp` when `test-app=true`. It selects a subdirectory
  under `ladybug/ladybug-ff-cypress-test/TestConfigurations`, which is always
  where the Frank configurations come from — the workflow never uses whatever
  configurations happen to ship inside `Frank2Example1` or
  `frankframework/webapp` themselves. This directory is passed to
  Frank!Runner as `override.configurations.dir` in `build.properties`.
- **`debugStorageName`** (`FileDebugStorage`/`DatabaseDebugStorage`) is another
  matrix `include` value, derived from `storage`, and is passed to Cypress via
  `--env` so the tests know which storage implementation to expect.
- **`RESOURCES_DIR`** is `$FRANK_APP/classes` only for the one case that uses
  the plain Nexus release without the test webapp
  (`buildLatestFFandLadybugCode=false` and `test-app=false`); in every other
  case it is `$FRANK_APP/src/main/resources`. This is where
  `DeploymentSpecifics.properties` and (for `dtap=PRD`) `localUsers.yml` get
  written.
- **The Ladybug JDBC datasource name** is derived from the basename of
  `FRANK_APP` (lower-cased), because Frank!Runner derives its own datasource
  name the same way from the directory it runs in.
- **`backendBranch`, `ffFork` and `ffBranch`** only affect *which source is
  checked out*; they have no effect on the running server unless
  `buildLatestFFandLadybugCode=true`, because table 1's server is never built
  from the checkouts in the first place.
