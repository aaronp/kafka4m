package esa.core.users

/**
  * A means to look up roles for a user
  * @tparam F
  */
trait RoleRepo[F[_]] {

  /** This repo view assumes we'll have been pushed updates
    *
    * @return the current revision we know about.
    */
  def currentRevision : RoleRepo.Revision

  /**
    * @param userId the user id for which we're looking up roles
    * @return the current repo revision and user roles
    */
  def roleNamesForUser(userId : User.Id) : F[Option[(RoleRepo.Revision, Set[String])]]

  /** @return the current role/permission mappings we're aware of
    */
  def rolePermissions : (RoleRepo.Revision, Map[String, Set[String]])
}

object RoleRepo {
  type Revision = Long
}
