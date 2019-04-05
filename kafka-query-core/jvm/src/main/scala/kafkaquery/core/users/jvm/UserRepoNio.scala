package kafkaquery.core.users.jvm
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{LinkOption, Path}
import java.util.UUID

import eie.io._
import kafkaquery.core.users.User.Id
import kafkaquery.core.users.jvm.UserRepoNio.{UpdateUserResult, UserUpdated, UserValidationFailed}
import kafkaquery.core.users.{EmailValidation, User, UserLookup}

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Try}

object UserRepoNio {

  def apply(byIdDir: Path, byNameDir: Path, usernameHash: String => String): UserRepoNio = {
    new UserRepoNio(byIdDir, byNameDir, usernameHash)
  }

  sealed trait UpdateUserResult
  case object UserUpdated                                            extends UpdateUserResult
  case class UserValidationFailed(errors: List[UserValidationError]) extends UpdateUserResult

}

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
class UserRepoNio(byIdDir: Path, byNameDir: Path, usernameHash: String => String) extends UserLookup[Try] {

  private object Lock

  // only the service user can read/write the user files
  private val userFileAtt = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))

  /**
    * Updates a user.
    *
    * If the email has changed, then we need to ensure another user doesn't already have the same email
    * And if all is well, we'll need to delete the link from the old email.
    *
    * If the user name has changed, we need to ensure another user doesn't have the same username
    * And if all is well, we'll need to delete the link from the old username.
    *
    * Finally, delete and recreate the user entry for their ID.
    *
    * We need to also ensure that, if we DIDN'T change the username or email, that removing/recreating a file
    * for the ID is still valid.
    *
    * @param userId
    * @param user
    * @return
    */
  def updateUser(userId: Id, user: User): UpdateUserResult = Lock.synchronized {
    userById(userId) match {
      case Success(Some(existingUser)) =>
        // they changed the username, check if an existing user has that name already exists
        val newUsernameAlreadyExists = {
          val userNameChanged = existingUser.name != user.name
          userNameChanged && userByUserName(user.name).toOption.flatten.isDefined
        }

        val newEmailAlreadyExists = {
          val userEmailChanged = existingUser.email != user.email
          userEmailChanged && userByUserName(user.email).toOption.flatten.isDefined
        }

        val errors = asErrors(newUsernameAlreadyExists, existingUser.name, newEmailAlreadyExists, existingUser.email)

        if (errors.nonEmpty) {
          UserValidationFailed(errors)
        } else {
          UserUpdated
        }
      case _ =>
    }

    ???
  }

  def deleteUser(userId: Id): Boolean = Lock.synchronized {
    userById(userId) match {
      case Success(Some(existingUser)) =>
        true
      case _ => false
    }
  }

  def createUser(user: User): Either[List[UserValidationError], Id] = Lock.synchronized {
    val hashedUserName        = usernameHash(user.name)
    val userNameFile          = byNameDir.resolve(hashedUserName)
    val userNameAlreadyExists = userNameFile.exists(LinkOption.NOFOLLOW_LINKS)

    val hashedEmail            = usernameHash(user.email)
    val userEmailFile          = byNameDir.resolve(hashedEmail)
    val userEmailAlreadyExists = userEmailFile.exists(LinkOption.NOFOLLOW_LINKS)

    val errors = asErrors(userNameAlreadyExists, user.name, userEmailAlreadyExists, user.email)

    if (errors.nonEmpty) {
      Left(errors)
    } else {
      val id     = UUID.randomUUID().toString
      val idFile = byIdDir.resolve(id.toString).createIfNotExists(userFileAtt).text = user.jsonStr
      idFile.createHardLinkFrom(userNameFile)

      // the username may be the same as their email
      if (hashedUserName != hashedEmail) {
        idFile.createHardLinkFrom(userEmailFile)
      }

      Right(id)
    }
  }

  private def asErrors(userNameAlreadyExists: Boolean, userName: String, userEmailAlreadyExists: Boolean, email: String) = {
    val errorList = ListBuffer[UserValidationError]()
    if (userNameAlreadyExists) {
      errorList += UserAlreadyExistsWithName(userName)
    }
    if (userEmailAlreadyExists) {
      errorList += UserAlreadyExistsWithEmail(email)
    }
    if (!EmailValidation.isValid(email)) {
      errorList += InvalidEmailAddress(email)
    }
    errorList.toList
  }

  override def userById(id: Id): Try[Option[User]] = Lock.synchronized {
    Try {
      loadUserAtPath(byIdDir.resolve(id))
    }
  }

  override def userByUserName(rawUserName: String): Try[Option[(Id, User)]] = Lock.synchronized {
    val userName = usernameHash(rawUserName)
    Try {
      val symlinkToIdFile = byNameDir.resolve(userName)
      loadUserAtPath(symlinkToIdFile).map { user =>
        symlinkToIdFile.fileName -> user
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
