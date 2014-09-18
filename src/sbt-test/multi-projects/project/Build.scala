import sbt._, Keys._

object MultiProjectBuild extends Build {

  lazy val a = Project(id = "a", base = file("a"))
  lazy val b = Project(id = "b", base = file("b"))

  lazy val root = Project(id = "multi-project", base = file(".")) dependsOn(a, b)

}
