sbt-notebook plugin
========

Not published to any public repo yet.

A sbt plugin that adds [scala-notebook](https://github.com/Bridgewater/scala-notebook) capabilities to any sbt project.

This plugin mainly adds a `notebook` task to your sbt project, that launches a scala-notebook server with your project build products and its dependencies on the classpath. You can think of it as a graphical in-browser `console`.

### Setup

1. Add the following line to `project/plugins.sbt`:
```scala
  addSbtPlugin("com.github.alexarchambault" %% "sbt-notebook" % "0.1.0")
```
Alternatively, you can also add it to all your projects at once by adding it to `~/.sbt/0.13/plugins/build.sbt`.

2. That's it! Type `notebook` at the sbt prompt, and a browser window should open at the right address. This address will also be printed on the command-line.

### Known issues

* Typing a key in the console while the notebook command is running does not fully stops the scala-notebook server. It seems to be stuck like described in [these](http://stackoverflow.com/questions/18748758/akka-application-cant-exit-the-application-after-shutting-down-actor-system) [SO](http://stackoverflow.com/questions/17669250/how-to-shut-down-the-dispatcher-thread-in-akka-actorsystem) questions, as the main function of the server has already exited. Some tuning of the config files or upgrading the akka version may solve this issue. As a temporary workaround, one can disable the forking of the server, by adding this line to `build.sbt`: `fork in Notebook := false`, but the other options (hostname, port, notebooks dir, notebooks project name will then be ignored).

* Connection to the server is not encrypted. By default, this may not be a problem, as only one session to the server can be opened. If one disables this (adding the line `notebookSecure := false` to `build.sbt`) and opens a notebook on another machine, then this may be an issue.

Copyright 2014 Alexandre Archambault

Released under Apache 2.0 license.
