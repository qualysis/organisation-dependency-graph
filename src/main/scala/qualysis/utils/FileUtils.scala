package qualysis.utils

object FileUtils {

  def waitWhile(condition: () => Boolean, timeout: Int): Boolean = {
    for(i <- 1 to timeout/50) if (condition())  return true  else Thread.sleep(50)
    false
  }

}
