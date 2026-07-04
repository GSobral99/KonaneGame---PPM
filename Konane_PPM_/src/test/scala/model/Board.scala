package model

import scala.collection.parallel.immutable.ParMap
import scala.collection.parallel.CollectionConverters.*

object Board:
  def initFullBoard(rows: Int, cols: Int): Board = {
    val positions = for {
      r <- 0 until rows
      c <- 0 until cols
      stone = if ((r + c) % 2 == 0) Stone.Black else Stone.White
    } yield (r, c) -> stone

    positions.toMap.par
  }

  def initKonaneBoard(rows: Int, cols: Int, remove1: Coord2D, remove2: Coord2D): (Option[Board], List[Coord2D]) = {
    val fullBoard = initFullBoard(rows, cols)

    val (r1, c1) = remove1
    val (r2, c2) = remove2

    def isCorner(r: Int, c: Int): Boolean =
      (r == 0 || r == rows - 1) && (c == 0 || c == cols - 1)

    def isCenter(r: Int, c: Int): Boolean =
      (r == rows / 2 || r == (rows - 1) / 2) && (c == cols / 2 || c == (cols - 1)/ 2)

    val isAdjacent = Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1

    val colorsDifferent = (fullBoard.get(remove1), fullBoard.get(remove2)) match {
      case (Some(s1), Some(s2)) => s1 != s2
      case _ => false
    }

    val validPosition = (isCorner(r1, c1) || isCenter(r1, c1)) || (isCorner(r2, c2) || isCenter(r2, c2))

    if (validPosition && colorsDifferent && isAdjacent) {
      val finalBoard = fullBoard - remove1 - remove2
      (Some(finalBoard), List(remove1, remove2))
    } else {
      (None, Nil)
    }
  }
