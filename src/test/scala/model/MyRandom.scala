package model
trait Random {
  def nextInt(): (Int, Random)
  def nextInt(x: Int): (Int, Random)
}
case class MyRandom(seed: Long)  extends Random {
  def nextInt(): (Int, MyRandom) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRandom = MyRandom(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRandom)
  }

  def nextInt(x: Int): (Int, MyRandom) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRandom = MyRandom(newSeed)
    val n = (newSeed >>> 16).toInt % x
    (if n < 0 then -n else n, nextRandom)
  }

}
