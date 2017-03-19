package andriusdap.orbweaver.utils

object Timer {

  def timed[T](function: () => T) = {
    val start = System.currentTimeMillis()
    val result = function.apply()
    val end = System.currentTimeMillis()

    println(s"finished in ${end - start}ms")
    result
  }

  def timedU(prefix:String)(function: () => Unit) = {
    timed[Unit] {
      function
    }
  }

}
