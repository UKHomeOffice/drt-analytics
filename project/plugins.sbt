ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" % "scala-xml" % VersionScheme.Always
)

val pluginSbtScoverageVersion = sys.props.getOrElse(
  "plugin.sbtscoverage.version", "2.0.6"
)

val pluginSbtCoverallsVersion = sys.props.getOrElse(
  "plugin.sbtcoveralls.version", "1.3.5"
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % pluginSbtScoverageVersion)
addSbtPlugin("org.scoverage" % "sbt-coveralls" % pluginSbtCoverallsVersion)

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.1")
addSbtPlugin("net.vonbuchholtz" %% "sbt-dependency-check" % "5.0.0")
addDependencyTreePlugin
