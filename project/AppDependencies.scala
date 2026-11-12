import sbt.*

object AppDependencies {
  private val pekkoVersion = "1.7.1"
  private val sparkVersion = "4.3.0"
  private val netlibVersion = "3.0.3"

  private val drtLibVersion = "v1501"

  val compileDependencies: Seq[ModuleID] = Seq(
    "org.apache.pekko"       %% "pekko-slf4j"             % pekkoVersion,
    "ch.qos.logback"          % "logback-classic"         % "1.6.8",
    "ch.qos.logback.contrib"  % "logback-json-classic"    % "0.1.5",
    "ch.qos.logback.contrib"  % "logback-jackson"         % "0.1.5",
    "org.apache.spark"       %% "spark-mllib"             % sparkVersion,
    "org.apache.spark"       %% "spark-sql"               % sparkVersion,
    "org.scala-lang.modules" %% "scala-xml"               % "2.5.0",
    "org.apache.pekko"       %% "pekko-http"              % "1.4.2",
    "org.apache.pekko"       %% "pekko-persistence"       % pekkoVersion,
    "org.apache.pekko"       %% "pekko-persistence-query" % pekkoVersion,
    "org.apache.pekko"       %% "pekko-stream"            % pekkoVersion,
    "org.apache.pekko"       %% "pekko-pki"               % pekkoVersion,
    "org.apache.pekko"       %% "pekko-persistence-jdbc"  % "1.3.0",
    "org.postgresql"          % "postgresql"              % "42.7.14",
    "joda-time"               % "joda-time"               % "2.15.1",
    "uk.gov.homeoffice"      %% "drt-lib"                 % drtLibVersion,
    "org.typelevel"          %% "cats-core"               % "2.13.0",
    "software.amazon.awssdk"  % "s3"                      % "2.58.0",
    "com.typesafe"           %% "ssl-config-core"         % "0.7.2",
    "dev.ludovic.netlib"      % "blas"                    % netlibVersion,
    "dev.ludovic.netlib"      % "lapack"                  % netlibVersion,
    "dev.ludovic.netlib"      % "arpack"                  % netlibVersion
  )

  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest"    %% "scalatest"                 % "3.2.20"     % Test,
    "org.specs2"       %% "specs2-core"               % "4.23.0"     % Test,
    "org.apache.pekko" %% "pekko-testkit"             % pekkoVersion % Test,
    "org.apache.pekko" %% "pekko-stream-testkit"      % pekkoVersion % Test,
    "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % Test
  )

  val all: Seq[ModuleID] = compileDependencies ++ testDependencies

  val dependencyOverrides: Seq[ModuleID] = Seq("io.netty" % "netty-all" % "4.2.20.Final")
}
