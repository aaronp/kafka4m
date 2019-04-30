package pipelines

/**
  *
  * Approach for users is this:
  *
  * =============================== user logs in   =====================================================================
  *
  * Someone logs in w/ user/password, so we can look them up by their username and provide a JWT which contains the
  * username, expiry date, and __roles__
  *
  * We can do an initial role lookup from e.g. UserRoles:
  *
  * {{{
  *   getRoles(userName : String) : F[Set[String]]
  * }}}
  *
  * And add those roles to the JWT.
  *
  * =============================== user requests a route w/ a JWT token  ==============================================
  *
  * We have the roles for that user -- but what if we've changed the roles or user's roles in the meantime?
  *
  * If we stick a revision number on the JWT token, and push updates to the repo managing roles, then we can just compare
  * the revision in order to know whether we should revoke/regenerate their token.
  *
  * The thing we're trying to avoid is a round-trip per user per request for roles. Any change to any user's roles, or roles
  * themselves, are infrequent, so we can just have one optimistic lock version for the whole role/perms and let everyone
  * incur a lookup when something changes.
  *
  * =============================== pseudocode  ========================================================================
  *
  * 1) user logs in, we do a lookup for their roles and stamp the JWT the: username, roles, revision
  * 2) subsequent request, we have the role reference data cached to resolve the permissions for the roles if the revision
  *    checks out.
  *
  *
  */
package object users {
  type UserName = String
  type Email = String

}
