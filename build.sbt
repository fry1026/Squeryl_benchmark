name := "Scala IntelliJ Template with SBT"

version := "0.1.0"

scalaVersion := "2.10.4"

mainClass := Some("Hello")

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies  ++=  Seq(
  "org.squeryl" %% "squeryl" % "0.9.5-6",
  "org.postgresql" % "postgresql" % "9.3-1101-jdbc41",
  "com.h2database" % "h2" % "1.2.127"
)

// libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "0.4.2" // sample library