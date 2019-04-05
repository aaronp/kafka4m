enablePlugins(CucumberPlugin)

CucumberPlugin.glue := "classpath:kafkaquery.deploy"

CucumberPlugin.features := List("classpath:kafkaquery.deploy.test")
