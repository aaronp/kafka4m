import org.scoverage.coveralls.Imports.CoverallsKeys._

name := "kafka4m"

organization := "com.github.aaronp"

enablePlugins(GhpagesPlugin)
enablePlugins(ParadoxPlugin)
enablePlugins(SiteScaladocPlugin)
enablePlugins(ParadoxMaterialThemePlugin) // see https://jonas.github.io/paradox-material-theme/getting-started.html

//ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox)

val username            = "aaronp"
val scalaTwelve         = "2.12.10"
val scalaThirteen       = "2.13.0"
val defaultScalaVersion = scalaThirteen
crossScalaVersions := Seq(scalaTwelve, scalaThirteen)

paradoxProperties += ("project.url" -> "https://aaronp.github.io/kafka4m/docs/current/")

Compile / paradoxMaterialTheme ~= {
  _.withLanguage(java.util.Locale.ENGLISH)
    .withColor("blue", "white")
    .withLogoIcon("kafka4m")
    .withLogoUri(new URI("https://images.app.goo.gl/fSucxpUKKzkWEqKv6"))
    .withRepository(uri("https://github.com/aaronp/kafka4m"))
    .withSocial(uri("https://github.com/aaronp"))
    .withoutSearch()
}

lazy val docker = taskKey[Unit]("Packages the app in a docker file").withRank(KeyRanks.APlusTask)

// see https://docs.docker.com/engine/reference/builder
docker := {
  val kafka4mAssembly = (assembly in Compile).value

  // contains the docker resources
  val deployResourceDir = (resourceDirectory in Compile).value.toPath.resolve("docker")
  val dockerTargetDir = {
    import eie.io._
    val dir = baseDirectory.value / "target" / "docker"
    dir.toPath.mkDirs()
  }

  Build.docker( //
               deployResourceDir = deployResourceDir, //
               appAssembly = kafka4mAssembly.asPath, //
               targetDir = dockerTargetDir, //
               logger = sLog.value //
  )
}

siteSourceDirectory := target.value / "paradox" / "site" / "main"

siteSubdirName in SiteScaladoc := "api/latest"

libraryDependencies ++= List(
  "io.monix"                   %% "monix"          % "3.1.0",
  "io.monix"                   %% "monix-reactive" % "3.1.0",
  "io.monix"                   %% "monix-eval"     % "3.1.0",
  "com.lihaoyi"                %% "sourcecode"     % "0.1.7",
  "com.github.aaronp"          %% "args4c"         % "0.7.0",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2",
  "com.typesafe"               % "config"          % "1.4.0",
  "org.apache.kafka"           % "kafka-clients"   % "2.3.1",
  "org.apache.kafka"           % "kafka-streams"   % "2.3.1"
)

libraryDependencies ++= List(
  "com.github.aaronp" %% "eie"            % "1.0.0" % "test",
  "org.scalactic"     %% "scalactic"      % "3.0.8" % "test",
  "org.scalatest"     %% "scalatest"      % "3.0.8" % "test",
  "ch.qos.logback"    % "logback-classic" % "1.2.3" % "test",
  "org.pegdown"       % "pegdown"         % "1.6.0" % "test",
  "junit"             % "junit"           % "4.12"  % "test",
  "com.github.aaronp" %% "dockerenv"      % "0.4.3" % "test",
  "com.github.aaronp" %% "dockerenv"      % "0.4.3" % "test" classifier ("tests")
)

publishMavenStyle := true
releaseCrossBuild := true
coverageMinimum := 70
coverageFailOnMinimum := true
git.remoteRepo := s"git@github.com:$username/kafka4m.git"
ghpagesNoJekyll := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
parallelExecution in Test := false
test in assembly := {}
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

// https://coveralls.io/github/aaronp/kafka4m
// https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token
coverallsTokenFile := Option((Path.userHome / ".sbt" / ".coveralls.kafka4m").asPath.toString)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "kafka4m.build"

// see http://scalameta.org/scalafmt/
scalafmtOnCompile in ThisBuild := true
scalafmtVersion in ThisBuild := "1.4.0"

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
testOptions in Test += (Tests.Argument(TestFrameworks.ScalaTest, "-h", s"target/scalatest-reports", "-oN"))

pomExtra := {
  <url>https://github.com/aaronp/kafka4m</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>Aaron</id>
        <name>Aaron Pritzlaff</name>
        <url>http://github.com/aaronp</url>
      </developer>
    </developers>
}
