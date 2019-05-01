import scala.annotation.tailrec

package object geometry {

  def lcm(a: Int, b: Int): Int = {
    if (a == 0 || b == 0) {
      0
    } else {
      (a * b) / gcd(a, b)
    }
  }

  @tailrec
  def gcd(a: Int, b: Int): Int = {
    if (b == 0) {
      a
    } else {
      gcd(b, a % b)
    }
  }

}
