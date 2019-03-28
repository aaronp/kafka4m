enablePlugins(CucumberPlugin)

CucumberPlugin.glue := "classpath:esa.deploy"

CucumberPlugin.features := List("classpath:esa.deploy.test")
