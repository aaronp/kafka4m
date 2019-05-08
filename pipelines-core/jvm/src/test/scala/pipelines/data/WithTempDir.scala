package pipelines.data

import java.nio.file.Path
import java.util.UUID

object WithTempDir {

  def apply[A](f: Path => A): A = {
    import eie.io._
    val dir = s"target/${UUID.randomUUID}".asPath
    try {
      f(dir)
    } finally {
      dir.delete()
    }
  }
}
