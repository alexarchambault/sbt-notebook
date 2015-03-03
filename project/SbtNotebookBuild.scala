import sbt._, Keys._
import sbtrelease.ReleasePlugin._
import com.typesafe.sbt.pgp.PgpKeys

class SbtNotebookBuild(
  base: File
) extends Build {

  lazy val root = Project(id = "sbt-notebook", base = base)
    .settings(
      name := "sbt-notebook",
      organization := "com.github.alexarchambault",
      sbtPlugin := true,
      scalaVersion := "2.10.4",
      publishMavenStyle := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },
      pomExtra := {
        <url>https://github.com/alexarchambault/sbt-notebook</url>
        <licenses>
          <license>
            <name>Apache 2.0</name>
            <url>http://opensource.org/licenses/Apache-2.0</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git:github.com/alexarchambault/sbt-notebook.git</connection>
          <developerConnection>scm:git:git@github.com:alexarchambault/sbt-notebook.git</developerConnection>
          <url>github.com/alexarchambault/sbt-notebook.git</url>
        </scm>
        <developers>
          <developer>
            <id>alexarchambault</id>
            <name>Alexandre Archambault</name>
            <url>https://github.com/alexarchambault</url>
          </developer>
        </developers>
      },
      credentials += {
        Seq("SONATYPE_USER", "SONATYPE_PASS").map(sys.env.get) match {
          case Seq(Some(user), Some(pass)) =>
            Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
          case _ =>
            Credentials(Path.userHome / ".ivy2" / ".credentials")
        }
      },
      scalacOptions += "-target:jvm-1.7",
      ReleaseKeys.versionBump := sbtrelease.Version.Bump.Bugfix,
      ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
    )
    .settings(xerial.sbt.Sonatype.sonatypeSettings: _*)

}
