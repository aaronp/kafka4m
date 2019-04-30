package pipelines.rest.users

import java.net.URLEncoder

import com.typesafe.config.Config
import pipelines.rest.jwt.Claims
import pipelines.users.LoginRequest

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

/**
  * A basic user controller which just writes down users locally
  *
  * @param userConfig
  */
class LocalUsers(userConfig: Config) extends LoginHandler {
  import eie.io._
  private val userDir = {
    val createIfNotExists = Try(userConfig.getBoolean("local.createIfNotExists")).getOrElse(false)
    val dir               = userConfig.getString("local.dir").asPath

    if (createIfNotExists) {
      dir.createIfNotExists()
    }
    dir
  }
  lazy val emailDir    = userDir.resolve("byEmail")
  lazy val userNameDir = userDir.resolve("byUserName")

  override def login(request: LoginRequest): Future[Option[Claims]] =
    Future.fromTry(Try {
      List(userDir, emailDir)
        .map { dir =>
          dir.resolve(safe(request.user)).resolve("password")
        }
        .collectFirst {
          case pwdFile if pwdFile.isFile && pwdFile.text == request.password =>
            Claims.after(5.minutes).forUser(request.user)
        }
    })

  private def safe(s: String) = URLEncoder.encode(s, "UTF-8")
}
