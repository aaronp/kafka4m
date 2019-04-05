package kafkaquery

object Using {

  def apply[A <: AutoCloseable, T](resource: A)(thunk: A => T): T = {
    try {
      thunk(resource)
    } finally {
      resource.close()
    }
  }
}
