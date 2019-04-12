import java.nio.file.Path
import eie.io._
import sbt.KeyRanks
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

val repo = "pipelines"
name := repo

val username            = "aaronp"
val scalaTwelve         = "2.12.8"
val defaultScalaVersion = scalaTwelve
val scalaVersions       = Seq(scalaTwelve)

crossScalaVersions := scalaVersions
organization := s"com.github.${username}"
scalaVersion := defaultScalaVersion
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// see https://github.com/sbt/sbt-ghpages
// this exposes the 'ghpagesPushSite' task
enablePlugins(GhpagesPlugin)
enablePlugins(GitVersioning)
//enablePlugins(PamfletPlugin)
enablePlugins(SiteScaladocPlugin)

// see http://scalameta.org/scalafmt/
scalafmtOnCompile in ThisBuild := true
scalafmtVersion in ThisBuild := "1.4.0"

// Define a `Configuration` for each project, as per http://www.scala-sbt.org/sbt-site/api-documentation.html
val Core             = config("pipelinesCoreJVM")
val PipelinesRest    = config("pipelinesRest")
val PipelinesDeploy  = config("pipelinesDeploy")
val PipelinesKafka   = config("pipelinesKafka")
val PipelinesEval    = config("pipelinesEval")
val Expressions      = config("expressions")
val ExpressionsAst   = config("expressionsAst")
val ExpressionsSpark = config("expressionsSpark")

git.remoteRepo := s"git@github.com:$username/$repo.git"
ghpagesNoJekyll := true

val typesafeConfig: ModuleID = "com.typesafe"      % "config"  % "1.3.3"
val args4cModule: ModuleID   = "com.github.aaronp" %% "args4c" % "0.6.2"

val logging = List("com.typesafe.scala-logging" %% "scala-logging" % "3.9.2", "ch.qos.logback" % "logback-classic" % "1.2.3")

def testLogging = logging.map(_ % "test")

val testDependencies = List(
  "junit"                  % "junit"      % "4.12"  % "test",
  "org.scalatest"          %% "scalatest" % "3.0.7" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.1" % "test",
  "org.pegdown"            % "pegdown"    % "1.6.0" % "test"
)

val simulacrum: ModuleID = "com.github.mpilquist" %% "simulacrum" % "0.13.0"

lazy val scaladocSiteProjects = List(
  (pipelinesCoreJVM, Core),
  (pipelinesRest, PipelinesRest),
  (pipelinesDeploy, PipelinesDeploy),
  (pipelinesKafka, PipelinesKafka),
  (pipelinesEval, PipelinesEval),
  (expressions, Expressions),
  (ExpressionsAst, ExpressionsAst)
)

lazy val scaladocSiteSettings = scaladocSiteProjects.flatMap {
  case (project: Project, conf) =>
    SiteScaladocPlugin.scaladocSettings(
      conf,
      mappings in (Compile, packageDoc) in project,
      s"api/${project.id}"
    )
  case _ => Nil // ignore cross-projects
}

lazy val settings = scalafmtSettings

def additionalScalcSettings = List(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-unchecked",
  //  "-explaintypes", // Explain type errors in more detail.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", // Turn on future language features.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",     // Warn when nullary methods return Unit.
  //  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

val baseScalacSettings = List(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:reflectiveCalls", // Allow reflective calls
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked",
  "-language:reflectiveCalls", // Allow reflective calls
  "-language:higherKinds",         // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  //"-Xlog-implicits",
  "-Xfuture" // Turn on future language features.
)

val scalacSettings = baseScalacSettings

val commonSettings: Seq[Def.Setting[_]] = Seq(
  //version := parentProject.settings.ver.value,
  organization := s"com.github.${username}",
  scalaVersion := defaultScalaVersion,
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  autoAPIMappings := true,
  exportJars := false,
  crossScalaVersions := scalaVersions,
  libraryDependencies ++= testDependencies,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= scalacSettings,
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := s"${repo}.build",
  assemblyMergeStrategy in assembly := {
    case str if str.contains("application.conf") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
  // see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  //(testOptions in Test) += (Tests.Argument(TestFrameworks.ScalaTest, "-h", s"target/scalatest-reports-${name.value}", "-oN"))
)

test in assembly := {}

// don't publish the root artifact
publishArtifact := false

publishMavenStyle := true

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(SiteScaladocPlugin)
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(
    pipelinesCoreJS,
    pipelinesCoreJVM,
    pipelinesRest,
    pipelinesClientXhr,
    pipelinesClientJvm,
    pipelinesDeploy,
    pipelinesEval,
    pipelinesKafka,
    expressions,
    expressionsAst,
    expressionsSpark
  )
  .settings(scaladocSiteSettings)
  .settings(
    paradoxProperties += ("project.url" -> "https://aaronp.github.io/pipelines/docs/current/"),
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    siteSourceDirectory := target.value / "paradox" / "site" / "main",
    siteSubdirName in ScalaUnidoc := "api/latest",
    publish := {},
    publishLocal := {}
  )

lazy val docker = taskKey[Unit]("Packages the app in a docker file").withRank(KeyRanks.APlusTask)

// see https://docs.docker.com/engine/reference/builder
docker := {
  val pipelinesAssembly = (assembly in (pipelinesRest, Compile)).value

  // contains the docker resources
  val deployResourceDir = (resourceDirectory in (pipelinesDeploy, Compile)).value.toPath

  // contains the web resources
  val webResourceDir = (resourceDirectory in (pipelinesClientXhr, Compile)).value.toPath.resolve("web")
  val jsArtifacts = {
    val path            = (fullOptJS in (pipelinesClientXhr, Compile)).value.data.asPath
    val dependencyFiles = path.getParent.find(_.fileName.endsWith("-jsdeps.min.js")).toList
    path :: dependencyFiles
  }

  val dockerTargetDir = {
    val dir = baseDirectory.value / "target" / "docker"
    dir.toPath.mkDirs()
  }

  Build.docker( //
    deployResourceDir = deployResourceDir, //
    jsArtifacts = jsArtifacts, //
    webResourceDir = webResourceDir, //
    restAssembly = pipelinesAssembly.asPath, //
    targetDir = dockerTargetDir, //
    logger = sLog.value //
  )
}

lazy val pipelinesCoreCrossProject = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(TestNGPlugin)
  .settings(
    name := "pipelines-core",
    libraryDependencies ++= List(
      // http://julienrf.github.io/endpoints/quick-start.html
      "org.julienrf" %%% "endpoints-algebra"             % "0.9.0",
      "org.julienrf" %%% "endpoints-json-schema-generic" % "0.9.0",
      "org.julienrf" %%% "endpoints-json-schema-circe"   % "0.9.0",
      "com.lihaoyi"  %%% "scalatags"                     % "0.6.8"
    ),
    //https://dzone.com/articles/5-useful-circe-feature-you-may-have-overlooked
    libraryDependencies ++= (List(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser",
      "io.circe" %%% "circe-java8",
      "io.circe" %%% "circe-literal",
      "io.circe" %%% "circe-shapes"
    ).map(_ % "0.11.1"))
  )
  .in(file("pipelines-core"))
  .jvmSettings(commonSettings: _*)
  .jvmSettings(
    name := "pipelines-core-jvm",
    coverageMinimum := 85,
    coverageFailOnMinimum := true,
    libraryDependencies += typesafeConfig,
    libraryDependencies ++= testLogging ++ List(
      "com.lihaoyi"         %% "sourcecode"          % "0.1.5", // % "test",
      "org.scala-js"        %% "scalajs-stubs"       % scalaJSVersion % "provided",
      "com.github.aaronp"   %% "eie"                 % "0.0.5",
      "org.reactivestreams" % "reactive-streams"     % "1.0.2",
      "org.reactivestreams" % "reactive-streams-tck" % "1.0.2" % "test",
      "org.pegdown"         % "pegdown"              % "1.6.0" % "test"
    ),
    // put scaladocs under 'api/latest'
    siteSubdirName in SiteScaladoc := "api/latest"
  )
  .jsSettings(name := "pipelines-core-js")
  .jsSettings(
    libraryDependencies ++= List(
      "com.lihaoyi"   %%% "scalatags" % "0.6.8",
      "com.lihaoyi"   %%% "scalarx"   % "0.4.0",
      "org.scalatest" %%% "scalatest" % "3.0.7" % "test"
    ))

lazy val pipelinesCoreJVM = pipelinesCoreCrossProject.jvm
lazy val pipelinesCoreJS  = pipelinesCoreCrossProject.js

lazy val example = project
  .in(file("example"))
  .settings(name := "example", coverageFailOnMinimum := false)
  .settings(commonSettings: _*)
  .settings((stringType in AvroConfig) := "String")
  .settings(libraryDependencies ++= testDependencies)
  .settings(libraryDependencies ++= List(
    "org.apache.avro" % "avro" % "1.8.2"
  ))

lazy val expressions = project
  .in(file("expressions"))
  .settings(name := "expressions", coverageMinimum := 30, coverageFailOnMinimum := true)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= testDependencies)
  .settings(
    libraryDependencies ++= List(
      "org.apache.avro" % "avro"           % "1.8.2",
      "org.scala-lang"  % "scala-reflect"  % "2.12.8", // % "provided",
      "org.scala-lang"  % "scala-compiler" % "2.12.8" // % "provided"
    ))
  .dependsOn(example % "test->compile")

lazy val expressionsAst = project
  .in(file("expressions-ast"))
  .settings(name := "expressions-ast", coverageMinimum := 30, coverageFailOnMinimum := true)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= testDependencies)
  .settings(libraryDependencies ++= List(
    "com.lihaoyi" %% "fastparse" % "2.1.0"
  ))
  .dependsOn(expressions % "compile->compile;test->test")
  .dependsOn(example % "test->compile")

lazy val expressionsSpark = project
  .in(file("expressions-spark"))
  .settings(name := "expressions-spark", coverageMinimum := 30, coverageFailOnMinimum := true)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= testDependencies)
  .settings(
    libraryDependencies ++= List(
      "org.apache.spark" %% "spark-core" % "2.4.0",
      "org.apache.spark" %% "spark-sql"  % "2.4.0",
      "org.apache.spark" %% "spark-avro" % "2.4.0"
    ))
  .dependsOn(expressions % "compile->compile;test->test")
  .dependsOn(expressionsAst % "compile->compile;test->test")
  .dependsOn(example % "test->compile")

lazy val pipelinesDeploy = project
  .in(file("pipelines-deploy"))
  .settings(commonSettings: _*)
  .settings(name := s"${repo}-deploy")
  .dependsOn(pipelinesRest % "compile->compile;test->test")

lazy val pipelinesClientXhr: Project = project
  .in(file("pipelines-client-xhr"))
  .dependsOn(pipelinesCoreJS % "compile->compile;test->test")
  .settings(name := s"${repo}-client-xhr")
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies += "org.julienrf" %%% "endpoints-xhr-client-circe" % "0.9.0",
    libraryDependencies += "com.lihaoyi"  %%% "scalatags"                  % "0.6.8",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom"                % "0.9.2"
  )
lazy val pipelinesClientJvm = project
  .in(file("pipelines-client-jvm"))
  .dependsOn(pipelinesCoreJS % "compile->compile;test->test")
  .settings(name := s"${repo}-client-jvm")
  .settings(
    libraryDependencies += "org.julienrf" %% "endpoints-akka-http-client"       % "0.9.0",
    libraryDependencies += "org.julienrf" %% "endpoints-akka-http-server-circe" % "0.4.0"
  )

lazy val pipelinesKafka = project
  .in(file("pipelines-kafka"))
  .dependsOn(pipelinesCoreJVM % "compile->compile;test->test")
  .settings(name := s"${repo}-kafka")
  .settings(commonSettings: _*)
  .settings(libraryDependencies += args4cModule)
  .settings(libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.2.0")
  .settings(libraryDependencies += "org.apache.kafka" % "kafka-streams" % "2.2.0")
  .settings(libraryDependencies ++= typesafeConfig :: logging)
  .dependsOn(pipelinesCoreJVM % "compile->compile;test->test")
  .dependsOn(pipelinesEval % "compile->compile;test->test")

lazy val pipelinesEval = project
  .in(file("pipelines-eval"))
  .settings(name := s"${repo}-eval")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= List("monix", "monix-execution", "monix-eval", "monix-reactive", "monix-tail").map { art =>
    "io.monix" %% art % "3.0.0-RC2"
  })
  .settings(libraryDependencies ++= typesafeConfig :: logging)
  .dependsOn(pipelinesCoreJVM % "compile->compile;test->test")
  .dependsOn(expressionsAst % "compile->compile;test->test")
  .dependsOn(example % "test->compile")

lazy val pipelinesRest = project
  .in(file("pipelines-rest"))
  .dependsOn(pipelinesEval % "compile->compile;test->test")
  .dependsOn(pipelinesKafka % "compile->compile;test->test")
  .settings(name := s"${repo}-rest")
  .settings(commonSettings: _*)
  .settings(mainClass in (Compile, run) := Some(Build.MainRestClass))
  .settings(mainClass in (Compile, packageBin) := Some(Build.MainRestClass))
  .settings(libraryDependencies ++= typesafeConfig :: logging)
  .settings(libraryDependencies ++= List("com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % "test", "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19" % "test"))
  .settings(libraryDependencies += args4cModule)
  .settings(libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.25.2")
  .settings(libraryDependencies ++= List(
    "org.julienrf" %% "endpoints-akka-http-server" % "0.9.0",
//    "org.julienrf" %% "endpoints-akka-http-server-circe" % "0.4.0",
    "org.julienrf" %% "endpoints-algebra-circe" % "0.9.0",
    "org.julienrf" %% "endpoints-openapi"       % "0.9.0"
  ))

// see https://leonard.io/blog/2017/01/an-in-depth-guide-to-deploying-to-maven-central/
pomIncludeRepository := (_ => false)

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url>https://github.com/${username}/${repo}
  </url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>${username}</id>
        <name>Aaron Pritzlaff</name>
        <url>https://github.com/${username}/${repo}
        </url>
      </developer>
    </developers>
}
