package com.github.alexarchambault.sbt_notebook

import java.io.File
import sbt._, sbt.Keys._

object NotebookPlugin extends AutoPlugin {

  lazy val Notebook = config("notebook") extend Runtime


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
  
  private lazy val scopedUpdateAndRunSettings = Defaults.compileSettings ++ Defaults.runnerSettings ++ Seq(
    allDependencies := {
      projectDependencies.value ++ libraryDependencies.value
    }
  , moduleSettings <<= {
      Def.task {
        new InlineConfiguration(projectID.value, projectInfo.value, allDependencies.value, dependencyOverrides.value, ivyXML.value, ivyConfigurations.value, defaultConfiguration.value, ivyScala.value, ivyValidate.value, conflictManager.value)
      }
    }
  , ivyModule := { val is = ivySbt.value; new is.Module(moduleSettings.value) }
  , run <<= Defaults.runTask(fullClasspath, mainClass in run, runner in run)
  , runMain <<= Defaults.runMainTask(fullClasspath, runner in run)
  , update <<= {
      import sbt.CrossVersion._
      import sbt.Def.Initialize

      def unmanagedScalaInstanceOnly: Initialize[Task[Option[ScalaInstance]]] = Def.taskDyn {
        if(scalaHome.value.isDefined) Def.task(Some(scalaInstance.value)) else Def.task(None)
      }
      def updateTask: Initialize[Task[UpdateReport]] = Def.task {
        val depsUpdated = transitiveUpdate.value.exists(!_.stats.cached)
        val isRoot = executionRoots.value contains resolvedScoped.value
        val s = streams.value
        val scalaProvider = appConfiguration.value.provider.scalaProvider

        // Only substitute unmanaged jars for managed jars when the major.minor parts of the versions the same for:
        //   the resolved Scala version and the scalaHome version: compatible (weakly- no qualifier checked)
        //   the resolved Scala version and the declared scalaVersion: assume the user intended scalaHome to override anything with scalaVersion
        def subUnmanaged(subVersion: String, jars: Seq[File])  =  (sv: String) =>
          (partialVersion(sv), partialVersion(subVersion), partialVersion(scalaVersion.value)) match {
            case (Some(res), Some(sh), _) if res == sh => jars
            case (Some(res), _, Some(decl)) if res == decl => jars
            case _ => Nil
          }
        val subScalaJars: String => Seq[File] = unmanagedScalaInstanceOnly.value match {
          case Some(si) => subUnmanaged(si.version, si.jars)
          case None => sv => if(scalaProvider.version == sv) scalaProvider.jars else Nil
        }
        val transform: UpdateReport => UpdateReport = r => Classpaths.substituteScalaFiles(scalaOrganization.value, r)(subScalaJars)

        val show = Reference.display(thisProjectRef.value)
        Classpaths.cachedUpdate(s.cacheDirectory, show, (ivyModule in Notebook).value, updateConfiguration.value, transform, skip = (skip in update).value, force = isRoot, depsUpdated = depsUpdated, log = s.log)
      }

      updateTask
    }
  )
  
  override lazy val projectSettings = inConfig(Notebook)(scopedUpdateAndRunSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.github.alexarchambault.scala_notebook" %% "server" % "0.3.0-SNAPSHOT"
    )
  , fork := true // Forking so that the config options (in javaOptions, below) are given to scala-notebook
  , connectInput := true
  )) ++ Seq(
    notebookHost := "127.0.0.1"
  , notebookPort := 8999
  , notebookSecure := true
  , notebooksDirectory := baseDirectory.value / "notebooks"
  , notebookProjectName := name.value
  , javaOptions ++= Seq(
      "notebook.hostname"        -> notebookHost.value
    , "notebook.port"            -> notebookPort.value.toString
    , "notebook.notebooks.name"  -> notebookProjectName.value
    , "notebook.notebooks.dir"   -> notebooksDirectory.value.getAbsolutePath
    ).map{case (arg, value) => s"-D$arg=$value"}
  , notebook <<= Def.taskDyn {
      (runMain in Notebook).toTask(" com.bwater.notebook.Server" + (if (notebookSecure.value) "" else " --disable_security"))
    }
  )

}
