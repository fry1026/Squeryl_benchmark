name := "Scala IntelliJ Template with SBT"

version := "0.1.0"

scalaVersion := "2.11.5"

mainClass := Some("Hello")

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies  ++=  Seq(
  "org.squeryl" %% "squeryl" % "0.9.6+",
  "org.postgresql" % "postgresql" % "9.3-1101-jdbc41",
  "com.h2database" % "h2" % "1.2.127",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "com.wix" %% "accord-core" % "0.4.1"
)