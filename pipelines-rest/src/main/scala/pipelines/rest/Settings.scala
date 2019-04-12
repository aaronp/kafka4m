package pipelines.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import pipelines.connect.RichKafkaProducer
import pipelines.kafka.PublishMessage
import pipelines.rest.routes.{PipelinesRoutes, SupportRoutes, StaticFileRoutes}
import monix.execution.Scheduler

case class Settings(rootConfig: Config, host: String, port: Int, materializer: ActorMaterializer) {

  object implicits {
    val computeScheduler                              = Scheduler.computation()
    val ioScheduler                                   = Scheduler.io()
    implicit val actorMaterializer: ActorMaterializer = materializer
    implicit val system                               = actorMaterializer.system
    implicit val routingSettings: RoutingSettings     = RoutingSettings(system)
  }

  val kafkaSupportRoutes: SupportRoutes = {
    val producer = RichKafkaProducer.strings(rootConfig)(implicits.ioScheduler)
    val publisher = (request: PublishMessage) => {
      producer.send(request.topic, request.key, request.data)
    }
    new SupportRoutes(rootConfig, publisher)
  }

  val kafkaRoutes  = PipelinesRoutes(rootConfig)(implicits.actorMaterializer, implicits.ioScheduler)
  val staticRoutes = StaticFileRoutes.fromRootConfig(rootConfig)

  override def toString = {
    import args4c.implicits._
    s"""$host:$port
       |pipelines config:
       |${rootConfig.getConfig("pipelines").summary()}
     """.stripMargin
  }
}

object Settings {

  def apply(rootConfig: Config): Settings = {
    val config                                   = rootConfig.getConfig("pipelines")
    implicit val system                          = ActorSystem(Main.getClass.getSimpleName.filter(_.isLetter))
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    new Settings(rootConfig, //
                 host = config.getString("host"), //
                 port = config.getInt("port"), //
                 materializer //
    )
  }
}
