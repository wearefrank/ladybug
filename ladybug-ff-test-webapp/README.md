# ladybug-ff-test-webapp

Runs Ladybug inside the Frank!Framework. The Ladybug backend is taken from this
repository (`ladybug-backend-springmvc`), so you can test your Ladybug changes
together with the Frank!Framework.

You can also run ladybug-ff-test-webapp using the Frank!Runner. Have a https://github.com/wearefrank/frank-runner
checkout as a sibling of this project. Write file `build.properties` in directory
`ladybug-ff-test-webapp`, see `build-example.properties` to see what properties
are supported. Then start using `restart.bat` or `restart.sh`.

## Running from IntelliJ

Select the run configuration **ladybug-ff-test-webapp** in the toolbar and click
the green run icon. This run configuration is shared through
`.run/ladybug-ff-test-webapp.run.xml` in the root of this repository, so
IntelliJ picks it up automatically.

The run configuration starts `org.frankframework.runner.MainClass` with the
following settings. Keep them if you change the run configuration or create your
own, for example in another IDE:

* The classpath of module `ladybug-ff-test-webapp` is used and the working
  directory is `$MODULE_WORKING_DIR$`. By default IntelliJ starts `MainClass` from
  the root of this repository. The Frank!Framework uses the working directory as
  its project directory and refuses to start when it finds a `.github` folder
  there, with the following error:

  ```
  java.io.IOException: assuming we're using IntelliJ, go to 'run options' enable [Use classpath of module] and set the working directory to [$MODULE_WORKING_DIR$].
  ```

* Dependencies with scope `provided` are added to the classpath. Some
  dependencies, for example `jakarta.servlet-api`, have scope `provided`.
  IntelliJ leaves these out of the classpath by default, but the embedded Tomcat
  of the Frank!Framework needs them. Without them you get:

  ```
  java.lang.ClassNotFoundException: jakarta.servlet.ServletException
  ```

If IntelliJ shows "Java file is located outside of the module source root, so it
won't be compiled" above `MainClass.java`, reload all Maven projects. This can
happen when IntelliJ reloaded the Maven projects while `pom.xml` contained merge
conflicts.

When the Frank!Framework has started, it is available at http://localhost:8080.

## Debugging from IntelliJ

Ladybug classes are taken from the modules in this repository, so you can set
breakpoints in them directly. Frank!Framework classes come from Maven jars. The
Frank!Framework publishes sources jars for them, so you can set breakpoints in
those too:

1. Press **Ctrl+N** (Navigate | Class) and type the name of a Frank!Framework
   class, for example `FrankApplication`. If the class is not listed, press
   **Ctrl+N** again or check **Include non-project items**. You can also
   **Ctrl+click** a Frank!Framework class in the code of this module.
2. If the file opens as decompiled code, click **Download Sources** in the
   banner at the top of the editor. To download the sources of all
   dependencies at once, click **Download Sources** in the Maven tool window.
3. Click in the left margin of a line to set a breakpoint.
4. Start run configuration **ladybug-ff-test-webapp** with **Debug** (the bug
   icon) instead of **Run**.

Breakpoints only work properly when the sources match the version that runs.
That is the version of property `ff.version`, see
[Choosing the Frank!Framework version](#choosing-the-frankframework-version).
The Maven tool window shows the resolved version under Dependencies. Do not
open the Frank!Framework classes from a local clone
of the Frank!Framework repository if that clone has another version, because the
line numbers will not match.

## Building from the command line

```
mvn install
```

## Choosing the Frank!Framework version

Property `ff.version` in `pom.xml` sets the version of all Frank!Framework
dependencies. It defaults to `10.3.0`. The `pom.xml` imports
`frankframework-parent` with this version, and the Frank!Framework
dependencies get their versions from it. Do not give them versions of their own,
because then they would no longer follow `ff.version`.

To test against another version, override the property on the command line:

```
mvn install -Dff.version=10.4.0
```

When you run from IntelliJ, change the value of `ff.version` in
`pom.xml` and reload all Maven projects. Do not commit that change unless you
want to change the default.
