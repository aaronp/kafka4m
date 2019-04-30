package pipelines.rest.users

import com.typesafe.config.Config
import pipelines.rest.jwt.Claims
import pipelines.users.LoginRequest

import scala.concurrent.Future
import scala.concurrent.duration._

class FixedUsersHandler(userConfig: Config) extends LoginHandler {
  import args4c.implicits._
  val usersByName = userConfig.getConfig("fixed").collectAsMap()
  override def login(request: LoginRequest): Future[Option[Claims]] = {
    Future.successful(usersByName.get(request.user).filter(_ == request.password).map { _ =>
      Claims.after(5.minutes).forUser(request.user)
    })
  }
}
