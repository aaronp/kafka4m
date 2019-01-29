package esa.core.users.jvm
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{LinkOption, Path}

import eie.io._
import esa.core.users.User.Id
import esa.core.users.{User, UserLookup}

import scala.util.Try

/**
  * A flat-file user lookup which indexes users on both ids and usernames.
  *
  * This class is thread-safe, isolating access using boring old locks. I'm assuming user queries
  * aren't going to be a killer point of contention, and if we have DDOS worries then we're not going to
  * solve that here.
  *
  * The layout is thus :
  * {{{
  *   <user id dir>/<id> # a file named by the userID which stores the serialized user
  *   <user name dir>/<username> # a symlink to a <user id dir>/<id>
  * }}}
  *
  * NOTE: This class doesn't do any validation of usernames. It's assumed something else has verified the
  * username is sensible -- contains alphanumeric characters.
  *
  * @param byIdDir the directory under which the users are stored by their id
  * @param byNameDir a directory where usernames are sym-linked to the user ids
  */
class UserRepo(byIdDir: Path, byNameDir: Path, usernameHash: String => String) extends UserLookup[Try] {

  private object Lock

  // only the service user can read/write the user files
  private val userFileAtt = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))

  private def nextUserIdUnsafe(): User.Id = {
    val ids = byIdDir.children.map(_.fileName.toLong)
    if (ids.isEmpty) {
      0L
    } else {
      ids.max + 1
    }
  }

  /**
    * To update a user
    * @param userId
    * @param user
    * @return
    */
  def updateUser(userId: Id, user: User): Option[Id] = Lock.synchronized {
    val userName     = usernameHash(user.name)
    val userNameFile = byNameDir.resolve(userName)
    if (userNameFile.exists(LinkOption.NOFOLLOW_LINKS)) {
      None
    } else {
      val id     = nextUserIdUnsafe()
      val idFile = byIdDir.resolve(id.toString).createIfNotExists(userFileAtt).text = user.jsonStr
      idFile.createHardLinkFrom(userNameFile)
      Option(id)
    }
  }

  def createUser(user: User): Option[Id] = Lock.synchronized {
    val userName     = usernameHash(user.name)
    val userNameFile = byNameDir.resolve(userName)
    if (userNameFile.exists(LinkOption.NOFOLLOW_LINKS)) {
      None
    } else {
      val id     = nextUserIdUnsafe()
      val idFile = byIdDir.resolve(id.toString).createIfNotExists(userFileAtt).text = user.jsonStr
      idFile.createHardLinkFrom(userNameFile)
      Option(id)
    }
  }

  override def userById(id: Long): Try[Option[User]] = Lock.synchronized {
    Try {
      loadUserAtPath(byIdDir.resolve(id.toString))
    }
  }

  override def userByUserName(rawUserName: String): Try[Option[(Id, User)]] = Lock.synchronized {
    val userName = usernameHash(rawUserName)
    Try {
      val symlinkToIdFile = byNameDir.resolve(userName)
      loadUserAtPath(symlinkToIdFile).map { user =>
        symlinkToIdFile.fileName.toLong -> user
      }
    }
  }

  private def loadUserAtPath(path: Path): Option[User] = {
    if (path.exists()) {
      Option(User.fromJson(path.text).right.get)
    } else {
      None
    }
  }

}
