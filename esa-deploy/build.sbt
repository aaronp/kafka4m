enablePlugins(DockerPlugin, DockerComposePlugin, CucumberPlugin)

CucumberPlugin.glue := "classpath:esa.deploy"

CucumberPlugin.features := List("classpath:esa.deploy.test")

imageNames in docker := Seq(
  ImageName(s"porpoiseltd/${name.value}:latest")
)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value

  val resDir         = (resourceDirectory in Compile).value
  val entrypointPath = resDir.toPath.resolve("esa-boot.sh").toFile

  val logbackFile = resDir.toPath.resolve("logback.xml").toFile

  sLog.value.warn(s"Creating docker file with entrypoint ${entrypointPath.getAbsolutePath}")

  val appDir = "/opt/esa"
  //
  // see https://forums.docker.com/t/is-it-possible-to-pass-arguments-in-dockerfile/14488
  // for passing in args to docker in run (which basically just says to use $@)
  //
  val dockerFile = new Dockerfile {
    from("java")
    expose(7770)
    run("mkdir", "-p", s"$appDir/data")
    run("mkdir", "-p", s"$appDir/logs")
    run("mkdir", "-p", s"$appDir/app")
    env("DATA_DIR", s"$appDir/data/")
    volume(s"$appDir/data")
    volume(s"$appDir/config")
    volume(s"$appDir/logs")
    maintainer("Aaron Pritzlaff")
    add(logbackFile, s"$appDir/config/logback.xml")
    add(artifact, s"$appDir/app/esa.jar")
    add(entrypointPath, s"$appDir/app/esa-boot.sh")
    run("chmod", "700", s"$appDir/app/esa-boot.sh")
    workDir(s"$appDir/app")
    entryPoint("java", "-cp", s"$appDir/config:$appDir/app/esa.jar", EsaBuild.MainRestClass)
//    entryPoint(s"$appDir/app/esa-boot.sh")
  }

  sLog.value.info(s"Created dockerfile: ${dockerFile.instructions}")

  dockerFile
}
