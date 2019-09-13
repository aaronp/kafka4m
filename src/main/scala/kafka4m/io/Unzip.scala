package kafka4m.io

import java.nio.file.{Files, Path}
import java.util.zip.ZipInputStream

import kafka4m.util.Using

private[io] object Unzip {
  def to(zip: Path, dest: Path): Path = {
    Using(new ZipInputStream(Files.newInputStream(zip))) { in: ZipInputStream =>
      var entry = in.getNextEntry()
      while (entry != null) {
        try {
          if (!entry.isDirectory) {
            val target = dest.resolve(entry.getName)
            if (!Files.exists(target)) {
              Files.createDirectories(target.getParent)
            }
            Files.copy(in, target)
          }
        } finally {
          in.closeEntry()
          entry = in.getNextEntry()
        }
      }
    }
    dest
  }
}
