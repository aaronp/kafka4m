package kafkaquery.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import kafkaquery.connect.RichKafkaProducer
import kafkaquery.kafka.PublishMessage
import kafkaquery.rest.routes.{KafkaRoutes, KafkaSupportRoutes, StaticFileRoutes}

case class Settings(rootConfig: Config, host: String, port: Int, materializer: ActorMaterializer) {

  object implicits {
    implicit val scheduler                            = kafkaquery.connect.newSchedulerService()
    implicit val actorMaterializer: ActorMaterializer = materializer
    implicit val system                               = actorMaterializer.system
    implicit val executionContext                     = system.dispatcher
    implicit val routingSettings: RoutingSettings     = RoutingSettings(system)
  }

  val kafkaSupportRoutes: KafkaSupportRoutes = {
    val producer = RichKafkaProducer.strings(rootConfig)(implicits.scheduler)
    val publisher = (request: PublishMessage) => {
      producer.send(request.topic, request.key, request.data)
    }
    new KafkaSupportRoutes(rootConfig, publisher)
  }

  val kafkaRoutes  = KafkaRoutes(rootConfig)(implicits.actorMaterializer, implicits.scheduler)
  val staticRoutes = StaticFileRoutes.fromRootConfig(rootConfig)

  override def toString = {
    import args4c.implicits._
    s"""$host:$port
       |kafkaquery config:
       |${rootConfig.getConfig("kafkaquery").summary()}
     """.stripMargin
  }
}

object Settings {

  def apply(rootConfig: Config): Settings = {
    val config                                   = rootConfig.getConfig("kafkaquery")
    implicit val system                          = ActorSystem(Main.getClass.getSimpleName.filter(_.isLetter))
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    new Settings(rootConfig, //
                 host = config.getString("host"), //
                 port = config.getInt("port"), //
                 materializer //
    )
  }
}
