lazy val root = (project in file(".")).
  settings(
    name := "comicazi",
    version := "1.0",
    scalaVersion := "2.11.8",
    libraryDependencies += "com.tumblr" %% "colossus" % "0.8.1",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
    libraryDependencies += "org.mongodb" % "bson" % "3.1.0",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
    libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.5",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
