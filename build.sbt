name := """amiup"""

version := "1.0"

scalaVersion := "2.13.10"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "cloudformation" % "2.20.78",
  "software.amazon.awssdk" % "autoscaling" % "2.20.79",
  "com.github.scopt" %% "scopt" % "4.1.0",
  "org.typelevel" %% "cats-core" % "2.9.0",
  "ch.qos.logback" %  "logback-classic" % "1.4.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)

scalacOptions := Seq("-unchecked", "-deprecation")

assemblyJarName in assembly := "amiup.jar"

assemblyMergeStrategy in assembly := {
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList(ps@_*) if Set("customization.config" , "examples-1.json" , "paginators-1.json", "service-2.json" , "waiters-2.json").contains(ps.last) =>
    MergeStrategy.discard
  case y =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(y)
}
