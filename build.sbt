name := """amiup"""

version := "1.0"

scalaVersion := "2.13.4"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "cloudformation" % "2.15.47",
  "com.github.scopt" %% "scopt" % "4.0.0",
  "org.typelevel" %% "cats-core" % "2.3.0",
  "ch.qos.logback" %  "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.scalatest" %% "scalatest" % "3.2.2" % Test
)

scalacOptions := Seq("-unchecked", "-deprecation")

assemblyJarName in assembly := "amiup.jar"
