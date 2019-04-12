package pipelines.users

/**
  * Provides a means to lookup a user
  *
  * @tparam F
  */
trait UserLookup[F[_]] {

  /**
    * @param id the user id
    * @return the user for this ID if it exists
    */
  def userById(id: User.Id): F[Option[User]]

  /**
    * Looks up a user by the username. It's not necessarily up to the implementation to verify (or hash) the username.
    *
    * That's down to its usage.
    *
    * @param userName
    * @return
    */
  def userByUserName(userName: String): F[Option[(User.Id, User)]]
}
