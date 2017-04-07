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


lazy val graphmanip = (project in file("graphmanip")).settings(commonSettings:_*).
  settings(
    name:= "graphmanip",
    mainClass in assembly := Some("andriusdap.orbweaver.graphmanip.App"),
    libraryDependencies += "it.unimi.dsi" % "webgraph-big" % "3.3.6",
    libraryDependencies += "org.rocksdb" % "rocksdbjni" % "5.2.1",
    libraryDependencies += "com.timgroup" % "iterata_2.11" % "0.1.6"
  )

lazy val subgrapher = (project in file("subgrapher")).settings(commonSettings:_*).
  settings(
    name:= "subgrapher",
    mainClass in assembly := Some("andriusdap.orbweaver.subgrapher.App"),
    libraryDependencies += "it.unimi.dsi" % "webgraph-big" % "3.3.6",
    libraryDependencies += "org.rocksdb" % "rocksdbjni" % "5.2.1"
  )