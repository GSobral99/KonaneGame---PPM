package ui

import model.*

object BoardPrinter:
  //T4
  def displayBoard(board: Board, rows: Int, cols: Int): Unit = {
    val colHeader = " " * 6 + (0 until cols).map(c => ('A' + c).toChar).mkString(" " * 3)
    println(colHeader)
    val lineSep = "    +" + ("---+" * cols)
    (0 until rows).foreach { r =>
      println(lineSep)
      val cells = (0 until cols).map { c =>
        board.get((r, c)) match {
          case Some(Stone.Black) => "B"
          case Some(Stone.White) => "W"
          case None => " "
        }
      }.mkString("| ", " | ", " |")
      println(f"${r}%2d  $cells")
    }
    println(lineSep)
  }

