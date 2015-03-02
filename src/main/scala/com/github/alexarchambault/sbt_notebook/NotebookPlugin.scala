package com.github.alexarchambault.sbt_notebook

import java.util.UUID

import sbt._, sbt.Keys._

object NotebookPlugin extends AutoPlugin {

  lazy val Notebook = config("notebook") extend (Compile, Runtime)

  object autoImport {
    val jupyterKernelSparkVersion = settingKey[Option[String]]("Spark version for the Jupyter kernel")
    val jupyterKernelJoveVersion = settingKey[Option[String]]("Jove version for the Jupyter kernel")
    val jupyterKernelId = taskKey[String]("Jupyter kernel id")
    val jupyterKernelName = taskKey[String]("Jupyter kernel name")
    val jupyterKernel = taskKey[Unit]("Run a Jupyter kernel")
    val jupyterJoveMetaPath = taskKey[String]("jove-meta path")
    val jupyterJoveMetaConnectionFile = taskKey[File]("jove-meta connection file")
    val jupyterKernelSpecSetupForce = settingKey[Boolean]("Force Jupyter kernel spec set up")
    val jupyterKernelSpecSetup = taskKey[(File, () => Unit)]("Set up Jupyter kernel spec")
  }

  private def homeDir = Option(System getProperty "user.home").filterNot(_.isEmpty).orElse(sys.env.get("HOME").filterNot(_.isEmpty)).map(file) getOrElse {
    throw new Exception("Cannot get user home dir, set one in the HOME environment variable")
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
        val joveVersion = jupyterKernelJoveVersion.value getOrElse "0.1.1-1-SNAPSHOT"

        (jupyterKernelSparkVersion in Runtime).value match {
          case Some(sv) =>
            val binaryVersion = sv split '.' take 2 mkString "."
            "sh.jove" %% s"jove-spark-embedded-cli_$binaryVersion" % joveVersion
          case None =>
            "sh.jove" %% "jove-scala-embedded-cli" % joveVersion
        }
      },
      /* Connecting input, to interrupt on key press */
      connectInput := true
    )
  ) ++ Seq(
    /* Default config values */
    jupyterKernelSparkVersion := {
      libraryDependencies.value
        .find(m => m.organization == "org.apache.spark" && m.name.startsWith("spark-core"))
        .map(_.revision)
    },
    jupyterKernelJoveVersion := None,
    jupyterKernelId := moduleName.value,
    jupyterKernelName := name.value,
    jupyterJoveMetaPath := {
      val setUpFile = (homeDir /: List(".ipython", ".jove-meta-path"))(_ / _)
      if (!setUpFile.exists())
        throw new Exception(s"jove-meta set-up file (${setUpFile.getAbsolutePath}) not found, run jove-meta --setup must be run once")

      val path = IO.read(setUpFile)
      val joveMetaFile = file(path)
      if (!joveMetaFile.exists())
        scala.Console.err println s"Warning: jove-meta not found at path $path (from setup file ${setUpFile.getAbsolutePath})"

      path
    },
    jupyterJoveMetaConnectionFile := {
      file(Option(System.getProperty("java.io.tmpdir")) getOrElse "/tmp") / s"jove-meta-${jupyterKernelId.value}-${UUID.randomUUID()}.json"
    },
    jupyterKernelSpecSetup := {
      val dir = homeDir / ".ipython" / "kernels" / jupyterKernelId.value
      val ackFile = dir / ".sbt-jupyter"
      val kernelFile = dir / "kernel.json"

      if (dir.exists() && !ackFile.exists() && !jupyterKernelSpecSetupForce.value)
        throw new Exception(s"${dir.getAbsolutePath} already exists, force erasing it with  jupyterKernelSpecSetupForce := true")

      if (!dir.exists() && !dir.mkdirs())
        scala.Console.err println s"Warning: failed at creating ${dir.getAbsolutePath}, trying to generate setup anyway"

      if (!ackFile.exists())
        IO.write(ackFile, Array.empty[Byte])

      IO.write(kernelFile,
        s"""{
           |  "argv": ${Seq(jupyterJoveMetaPath, "--id", jupyterKernelId.value, "--quiet", "--meta-connection-file", jupyterJoveMetaConnectionFile.value.getAbsolutePath, "--connection-file", "{connection_file}").map("\"" + _ + "\"").mkString("[", ", ", "]")},
           |  "display_name": "${jupyterKernelName.value}",
           |  "language": "scala",
           |  "extensions": ["snb"]
           |}
         """.stripMargin
      )

      (kernelFile, { () => IO.delete(dir) })
    },
    /* Definitions of the notebook commands/tasks */
    jupyterKernel <<= Def.taskDyn {
      /* Compiling the root project, so that its build products and those of its dependency sub-projects are available
         in the classpath */
      (compile in Runtime).value

      val extraOpts = List("--exit-on-key-press", "--meta", "--connection-file", jupyterJoveMetaConnectionFile.value.getAbsolutePath)

      val mainClass =
        (jupyterKernelSparkVersion in Runtime).value.fold("jove.scala.JoveScalaEmbedded")(_ => "jove.spark.JoveSparkEmbedded")

      /* Launching the notebook kernel */
      (runMain in Notebook).toTask(s" $mainClass ${extraOpts mkString " "}")
    }
  )

}
