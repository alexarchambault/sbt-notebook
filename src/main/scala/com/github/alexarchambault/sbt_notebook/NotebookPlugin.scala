package com.github.alexarchambault.sbt_notebook

import java.io.File
import sbt._, sbt.Keys._

object NotebookPlugin extends AutoPlugin {

  lazy val Notebook = config("notebook") extend (Compile, Runtime)


  object autoImport {
    val notebookHost = taskKey[String]("Host of the scala-notebook server")
    val notebookPort = taskKey[Int]("Port of the scala-notebook server")
    val notebookSecure = taskKey[Boolean]("Secure notebook server (should? allow only one connection)")

    val notebooksDirectory = taskKey[File]("Directory the notebooks")
    val notebookProjectName = taskKey[String]("Notebooks project name")
    
    val notebook = taskKey[Unit]("Run the scala-notebook server")
  }
  
  import autoImport._

  override def trigger = allRequirements
  
  override lazy val projectSettings = inConfig(Notebook)(
    Defaults.compileSettings ++ Defaults.runnerSettings ++ Classpaths.ivyBaseSettings ++ 
    Seq(
      /* Overriding run and runMain defined by compileSettings so that they use the scoped fullClasspath 
       * - why don't they by default? */
      run <<= Defaults.runTask(fullClasspath, mainClass in run, runner in run)
    , runMain <<= Defaults.runMainTask(fullClasspath, runner in run)
      /* Overriding classDirectory defined by compileSettings so that we are given
        the classDirectory of the default scope in runMain below */     
    , classDirectory := crossTarget.value / "classes"
      /* Adding scala-notebook dependency */
    , resolvers += Resolver.sonatypeRepo("snapshots")
    , libraryDependencies ++= Seq(
        "com.github.alexarchambault.scala_notebook" %% "server" % "0.3.0-SNAPSHOT"
      )
      /* Forking so that the config options (in javaOptions, below) are given to scala-notebook */
    , fork := true
      /* Connecting input, so that the scala-notebook server is given the input events, although
         the goal of it - exiting on key press - is still buggy */
    , connectInput := true
    )
  ) ++ Seq(
    /* Default config values */
    notebookHost        := "127.0.0.1"
  , notebookPort        := 8999
  , notebookSecure      := true
  , notebooksDirectory  := baseDirectory.value / "notebooks"
  , notebookProjectName := name.value
    /* Giving the config values to scala-notebook as command-line config options */
  , javaOptions ++= Seq(
      "notebook.hostname"        -> notebookHost.value
    , "notebook.port"            -> notebookPort.value.toString
    , "notebook.notebooks.name"  -> notebookProjectName.value
    , "notebook.notebooks.dir"   -> notebooksDirectory.value.getAbsolutePath
    ).map{case (arg, value) => s"-D$arg=$value"}
    /* Definition of the notebook command/task */
  , notebook <<= Def.taskDyn {
      /* Compiling the root project, so that its build products and those of its dependency sub-projects are available
         in the classpath */
      (compile in Runtime).value
      
      /* Launching the scala-notebook server */
      (runMain in Notebook).toTask(" com.bwater.notebook.Server" + (if (notebookSecure.value) "" else " --disable_security"))
    }
  )

}
