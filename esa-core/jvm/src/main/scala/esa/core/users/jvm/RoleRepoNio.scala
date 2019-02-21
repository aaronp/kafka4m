package esa.core.users.jvm
import java.nio.file.Path

import esa.core.users.RoleRepo
import esa.core.users.RoleRepo.Revision
import esa.core.users.User.Id

import scala.util.Try

class RoleRepoNio(dir: Path) extends RoleRepo[Try] {

  /** This repo view assumes we'll have been pushed updates
    *
    * @return the current revision we know about.
    */
  override def currentRevision: Revision = ???

  /**
      * @param userId the user id for which we're looking up roles
      * @return the current repo revision and user roles
      */
    override def roleNamesForUser(userId: Id): Try[Option[(Revision, Set[String])]] = ???

  /** @return the current role/permission mappings we're aware of
      */
    override def rolePermissions: (Revision, Map[String, Set[String]]) = ???
}
