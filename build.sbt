import SonatypeKeys._

bintray.Plugin.bintrayPublishSettings

name := "sbt-notebook"

organization := "com.github.alexarchambault"

// licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

version := "0.1.2-SNAPSHOT"

sbtPlugin := true

scalaVersion := "2.10.4"

profileName := "alexandre.archambault"

xerial.sbt.Sonatype.sonatypeSettings

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

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
}
