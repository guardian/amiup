name := """amiup"""

version := "1.0"

scalaVersion := "2.11.8"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-cloudformation" % "1.11.37",
  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.typelevel" %% "cats" % "0.7.2",
  "io.reactivex" %% "rxscala" % "0.26.3",
  "com.googlecode.lanterna" % "lanterna" % "3.0.0-beta3",
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

scalacOptions := Seq("-unchecked", "-deprecation")
