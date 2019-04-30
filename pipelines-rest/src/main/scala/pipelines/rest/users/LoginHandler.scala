package pipelines.rest.users

import com.typesafe.config.Config
import pipelines.rest.jwt.Claims
import pipelines.users.LoginRequest

trait LoginHandler {

  def login(request: LoginRequest): Option[Claims]
}

object LoginHandler {
  def apply(rootConfig: Config): LoginHandler = {
    val usersConfig = rootConfig.getConfig("pipelines.users")
    val c1assName   = usersConfig.getString("loginHandler")
    val c1ass       = Class.forName(c1assName).asInstanceOf[Class[LoginHandler]]
    val newInstance = c1ass.getConstructor(classOf[Config])
    newInstance.newInstance(rootConfig)
  }
}
