package kafka4m.util

/** come on scala 2.13!
  */
object Using {

  def apply[A <: AutoCloseable, T](resource: A)(thunk: A => T): T = {
    try {
      thunk(resource)
    } finally {
      resource.close()
    }
  }
}
