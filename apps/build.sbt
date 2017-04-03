lazy val commonSettings = Seq(
  organization := "orb-weaver",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.1",
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  test in assembly := {}
)

lazy val urlsplit = (project in file("urlsplit")).settings(commonSettings:_*).
  settings(
    name:= "urlsplit",
    mainClass in assembly := Some("andriusdap.orbweaver.urlsplit.WowpalWabbit")
  )
