package pipelines.connect

class DockerEnvTest extends BaseDockerSpec("scripts/kafka") {
  "DockerEnv" ignore {
    "start/stop docker" in {
      if (isDockerRunning()) {
        stopDocker() shouldBe true
      }

      isDockerRunning() shouldBe false
      startDocker() shouldBe true
      startDocker() shouldBe true
      stopDocker() shouldBe true
      isDockerRunning() shouldBe false
    }
  }
}
