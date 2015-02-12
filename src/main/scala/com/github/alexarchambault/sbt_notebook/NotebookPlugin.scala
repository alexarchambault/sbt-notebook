package com.github.alexarchambault.sbt_notebook

import java.io.File
import sbt._, sbt.Keys._

object NotebookPlugin extends AutoPlugin {

  lazy val Notebook = config("notebook") extend (Compile, Runtime)


  object autoImport {
    val notebookHost = taskKey[String]("Host of the notebook server")
    val notebookPort = taskKey[Int]("Port of the notebook server")

    val notebooksDirectory = taskKey[File]("Directory the notebooks")
    val notebookKernelName = taskKey[String]("Notebooks project name")

    val notebookSharedVariables = taskKey[Boolean]("Whether the notebook sessions should share their variables")

    val notebookKernel = taskKey[Unit]("Run a notebook kernel")
    val notebook = taskKey[Unit]("Run notebook kernel and server")
  }
  
  import autoImport._

  override def trigger = allRequirements
  
  override lazy val projectSettings = inConfig(Notebook)(
    Defaults.compileSettings ++ Defaults.runnerSettings ++ Classpaths.ivyBaseSettings ++ 
    Seq(
      /* Overriding run and runMain defined by compileSettings so that they use the scoped fullClasspath */
      run <<= Defaults.runTask(fullClasspath, mainClass in run, runner in run),
      runMain <<= Defaults.runMainTask(fullClasspath, runner in run),
      /* Overriding classDirectory defined by compileSettings so that we are given
        the classDirectory of the default scope in runMain below */     
      classDirectory := crossTarget.value / "classes",
      /* Adding jove-embedded dependency */
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        // TODO Add scala or spark kernel, in shared mode (shared CP)
        "sh.jove" %% "jove-embedded" % "0.1.0-SNAPSHOT"
      ),
      /* Connecting input, to interrupt on key press */
      connectInput := true
    )
  ) ++ Seq(
    /* Default config values */
    notebookHost        := "",
    notebookPort        := 9000,
    notebooksDirectory  := baseDirectory.value / "notebooks",
    notebookKernelName := name.value,
    /* Definitions of the notebook commands/tasks */
    notebookKernel <<= Def.taskDyn {
      /* Compiling the root project, so that its build products and those of its dependency sub-projects are available
         in the classpath */
      (compile in Runtime).value

      var extraOpts = List.empty[String]

      if (notebookSharedVariables.value)
        extraOpts = "--shared" :: extraOpts
      
      /* Launching the notebook kernel */
      (runMain in Notebook).toTask(s" jove.embedded.EmbeddedKernel --exit-on-key-press ${extraOpts mkString " "}")
    },
    notebook <<= Def.taskDyn {
      // TODO Pass the notebook options (host, port, ...) to the notebook server
      // FIXME Factorize with above (only difference is the --notebook-server option here)
      (compile in Runtime).value

      var extraOpts = List.empty[String]

      if (notebookSharedVariables.value)
        extraOpts = "--shared" :: extraOpts

      (runMain in Notebook).toTask(s" jove.embedded.EmbeddedKernel --exit-on-key-press --notebook-server ${extraOpts mkString " "}")
    }
  )

}
