package com.github.alexarchambault.sbt_notebook

import sbt._, sbt.Keys._

object NotebookPlugin extends AutoPlugin {

  lazy val Notebook = config("notebook") extend (Compile, Runtime)


  object autoImport {
    val nbSparkVersion = settingKey[Option[String]]("Spark version for the notebook kernel")
    val nbKernelName = taskKey[String]("Project notebook kernel name")
    val nbIPythonMode = settingKey[Boolean]("Run the notebook kernel in single-threaded IPython mode")
    val nbKernel = taskKey[Unit]("Run a notebook kernel")
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
      libraryDependencies += {
        (nbSparkVersion in Runtime).value match {
          case Some(sv) =>
            val binaryVersion = sv split '.' take 2 mkString "."
            "sh.jove" %% s"jove-spark-embedded-cli_$binaryVersion" % "0.1.0-SNAPSHOT"
          case None =>
            "sh.jove" %% "jove-scala-embedded-cli" % "0.1.0-SNAPSHOT"
        }
      },
      /* Connecting input, to interrupt on key press */
      connectInput := true
    )
  ) ++ Seq(
    nbSparkVersion := {
      libraryDependencies.value
        .find(m => m.organization == "org.apache.spark" && m.name.startsWith("spark-core"))
        .map(_.revision)
    },
    /* Default config values */
    nbKernelName := name.value,
    nbIPythonMode := false,
    /* Definitions of the notebook commands/tasks */
    nbKernel <<= Def.taskDyn {
      /* Compiling the root project, so that its build products and those of its dependency sub-projects are available
         in the classpath */
      (compile in Runtime).value

      var extraOpts = List("--exit-on-key-press")

      if (!nbIPythonMode.value)
        extraOpts ::= "--meta"

      val mainClass =
        (nbSparkVersion in Runtime).value.fold("jove.scala.JoveScalaEmbedded")(_ => "jove.spark.JoveSparkEmbedded")

      /* Launching the notebook kernel */
      (runMain in Notebook).toTask(s" $mainClass ${extraOpts mkString " "}")
    }
  )

}
