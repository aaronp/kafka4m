enablePlugins(CucumberPlugin)

CucumberPlugin.glue := "classpath:pipelines.deploy"

CucumberPlugin.features := List("classpath:pipelines.deploy.test")
