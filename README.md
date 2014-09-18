sbt-notebook plugin
========

A sbt plugin that adds [scala-notebook](https://github.com/Bridgewater/scala-notebook) capabilities to sbt projects.

This plugin mainly adds a `notebook` task to your sbt project, that launches a scala-notebook server with your project build products and its dependencies on the classpath. You can think of it as a graphical in-browser `console`.

### Setup

* Add the following lines to `project/plugins.sbt`:
```scala
addSbtPlugin("com.github.alexarchambault" %% "sbt-notebook" % "0.1.1")
```
 For the development version:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.github.alexarchambault" %% "sbt-notebook" % "0.1.2-SNAPSHOT")
```
Alternatively, you can also add them to all your projects at once by adding them to `~/.sbt/0.13/plugins/build.sbt`.

* That's it! Type `notebook` at the sbt prompt, and a browser window should open at the right address. This address will also be printed in the console.

### Known issues

* Does not work with scala versions other than 2.10.

* scala-notebook relies on akka 2.1.4. It seems not to work with akka >= 2.2.0. See (this)[https://github.com/Bridgewater/scala-notebook/issues/46] and (this project)[https://github.com/andypetrella/scala-notebook/tree/spark].

* Typing a key in the console while the notebook command is running does not fully stop the scala-notebook server as it should.
It seems to be stuck like described in these [SO](http://stackoverflow.com/questions/18748758/akka-application-cant-exit-the-application-after-shutting-down-actor-system) [questions](http://stackoverflow.com/questions/17669250/how-to-shut-down-the-dispatcher-thread-in-akka-actorsystem), as the main function of the server has already exited. Some tuning of the config files or upgrading the akka version may solve this issue. As a temporary workaround, one can disable the forking of the server, by adding this line to `build.sbt`: `fork in Notebook := false`, but the other options (hostname, port, notebooks dir, notebooks project name will then be ignored).

* Connection to the server is not encrypted. By default, this may not be a problem, as only one session to the server can be opened. If one disables this (adding the line `notebookSecure := false` to `build.sbt`) and opens a notebook on another machine, then this may be an issue.

Copyright (c) 2014 Alexandre Archambault, and others. See LICENSE file for more details.

Released under Apache 2.0 license.
