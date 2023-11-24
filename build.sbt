import sbt.Keys.libraryDependencies

//Project version
ThisBuild / version := "0.1.0-SNAPSHOT"
// Define the Scala version to be used.
ThisBuild / scalaVersion := "2.13.10"
// Define the project name.
name := "Policeman'n'Thief"

compileOrder := CompileOrder.JavaThenScala
test / fork := true
run / fork := true
run / javaOptions ++= Seq(
  "-Xms8G",
  "-Xmx100G",
  "-XX:+UseG1GC"
)

unmanagedBase := baseDirectory.value / "lib"

// Define library versions. Adjust the version numbers according to your needs.
val scalaTestVersion = "3.2.15"
val typeSafeConfigVersion = "1.4.2"
val logbackVersion = "1.3.11"
val sfl4sVersion = "2.0.7"
val graphVizVersion = "0.18.1"
val sparkVersion = "3.5.0"

//val guavaVersion = "31.1-jre"
val netBuddyVersion = "1.14.4"
val catsVersion = "2.9.0"
val apacheCommonsVersion = "2.13.0"
val jGraphTlibVersion = "1.5.2"
val scalaParCollVersion = "1.0.4"
//val guavaAdapter2jGraphtVersion = "1.5.2"
val AkkaHttpVersion = "10.6.0"

lazy val commonDependencies = Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % scalaParCollVersion,
  "com.typesafe" % "config" % typeSafeConfigVersion, // Typesafe Config Library
  "ch.qos.logback" % "logback-classic" % logbackVersion, // Logback Classic Logger
  "org.slf4j" % "slf4j-api" % sfl4sVersion, // SLF4J API Module
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalatestplus" %% "mockito-4-2" % "3.2.12.0-RC2" % Test,
  "org.apache.spark" %% "spark-core" % sparkVersion, // Spark Core
  "org.apache.spark" %% "spark-sql" % sparkVersion, // Spark SQL
  "org.apache.spark" %% "spark-graphx" % sparkVersion, // Spark GraphX
  "org.apache.spark" %% "spark-mllib" % sparkVersion, // Spark MLlib
  "com.typesafe.akka" %% "akka-actor" % "2.8.0",
  "com.typesafe.akka" %% "akka-http" % "10.5.0",
  "com.typesafe.akka" %% "akka-stream" % "2.8.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0"
  //"org.yaml" %% "snakeyaml" % "1.29"
  //"org.apache.spark" %% "spark-streaming" % sparkVersion,
  //"org.apache.spark" %% "spark-streaming-twitter" % sparkVersion
).map(_.exclude("org.slf4j", "*"))


libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "31.1-jre",
  "org.yaml" % "snakeyaml" % "2.0",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.5.0" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.8.0" % "test"
    exclude ("com.google.guava", "guava") // Exclude transitive Guava
)


// Define your project and its dependencies.
lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.10",
    name := "RandomWalks",
    libraryDependencies ++= commonDependencies, // Adding common dependencies to your project
    //libraryDependencies += "your.group" % "your-artifact" % "1.0" from "External Libraries/netmodelsim.jar",
  )

// Assembly
// assemblyMergeStrategy in assembly := {
//   case PathList("META-INF", _*) => MergeStrategy.discard
//   case _                        => MergeStrategy.first
// }


exportJars := true

// Define Scala Compiler options.
scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs
  "--explain-types", // Explain type errors in more detail
  "-feature", // Emit warning and location for usages of features that should be imported explicitly
  "-Ytasty-reader"
)

// Define JVM options for running your project.
run / javaOptions ++= Seq(
  "-Xms512M", // Initial JVM heap size
  "-Xmx2G", // Maximum JVM heap size
  "-XX:+UseG1GC" // Use G1 Garbage Collector
)

// Define the main class. Replace with the actual main class of your application.
//Compile / mainClass := Some("com.lsc.Main")
//run / mainClass := Some("com.lsc.Main")
Compile / mainClass := Some("Game.GameLogic")
run / mainClass := Some("Game.GameLogic")

val jarName = "game.jar"
/*assembly/assemblyJarName := jarName


//Merging strategies
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}*/