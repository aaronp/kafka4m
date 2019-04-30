package pipelines.rest.users

import java.lang.reflect.Constructor

import com.typesafe.config.Config
import pipelines.rest.jwt.Claims
import pipelines.users.LoginRequest

import scala.concurrent.Future

trait LoginHandler {

  def login(request: LoginRequest): Future[Option[Claims]]
}

object LoginHandler {

  /**
    * Create a 'LoginHandler' from the given configuration. A class implementing 'LoginHandler' should either have a no-args
    * constructor OR a constructor which takes an instance of a typesafe Config, where the config is the 'pipelines.users' config
    *
    * @param rootConfig the root config
    * @return a new login handler
    */
  def apply(rootConfig: Config): LoginHandler = {
    val usersConfig = rootConfig.getConfig("pipelines.users")
    val c1assName   = usersConfig.getString("loginHandler")
    val c1ass       = Class.forName(c1assName).asInstanceOf[Class[LoginHandler]]

    c1ass.getConstructors.find(_.getParameterCount == 1) match {
      case Some(ctr: Constructor[LoginHandler]) => ctr.newInstance(usersConfig)
      case None                                 => c1ass.newInstance()
    }

  }
}
