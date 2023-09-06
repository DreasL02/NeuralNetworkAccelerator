scalaVersion := "2.13.10"

scalacOptions ++= Seq(
	"-deprecation",
	"-feature",
	"-unchecked",
	"-Xfatal-warnings",
	"-language:reflectiveCalls",
)

resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
	Resolver.sonatypeRepo("releases")
)

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.5" cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.5"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.5"
