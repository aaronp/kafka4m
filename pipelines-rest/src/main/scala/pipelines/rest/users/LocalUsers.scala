package pipelines.rest.users

import com.typesafe.config.Config
import pipelines.rest.jwt.Claims
import pipelines.users.LoginRequest

/**
  * A basic user controller which just writes down users locally
  *
  * @param rootConfig
  */
class LocalUsers(rootConfig: Config) extends LoginHandler {
  import eie.io._
  private val userDir = {
    val createIfNotExists = rootConfig.getBoolean("pipelines.users.local.createIfNotExists")
    val dir               = rootConfig.getString("pipelines.users.local.dir").asPath

    if (createIfNotExists)
      dir
  }
  override def login(request: LoginRequest): Option[Claims] = {
    ???
  }
}
