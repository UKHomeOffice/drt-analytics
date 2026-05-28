import net.nmoncho.sbt.dependencycheck.settings.{AnalyzerSettings, NvdApiSettings}

ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version := "v" + sys.env.getOrElse("DRONE_BUILD_NUMBER", sys.env.getOrElse("BUILD_ID", "DEV"))
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name := "drt-analytics",
    trapExit := false,
    libraryDependencies ++= AppDependencies.all,
    resolvers += "Artifactory Realm libs release" at "https://artifactory.digital.homeoffice.gov.uk/artifactory/libs-release/",
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") =>
        val log = sLog.value
        log.info("discarding MANIFEST.MF")
        MergeStrategy.discard
      case PathList("reference.conf") =>
        val log = sLog.value
        log.info("concatinating reference.conf")
        MergeStrategy.concat
      case PathList("version.conf") =>
        val log = sLog.value
        log.info("concatinating version.conf")
        MergeStrategy.concat
      case default =>
        val log = sLog.value
        log.debug(s"keeping last $default")
        MergeStrategy.last
    }
  )
  .settings(CodeCoverageSettings.codeCoverageSettings *)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(WartRemoverSettings.wartRemoverSettings *)

val nvdAPIKey = sys.env.getOrElse("NVD_API_KEY", "")

dependencyCheckNvdApi := NvdApiSettings(apiKey = nvdAPIKey)

ThisBuild / dependencyCheckAnalyzers := dependencyCheckAnalyzers.value.copy(
  ossIndex = AnalyzerSettings.OssIndex(
    enabled = Some(false),
    url = None,
    batchSize = None,
    requestDelay = None,
    useCache = None,
    warnOnlyOnRemoteErrors = None,
    username = None,
    password = None
  )
)
