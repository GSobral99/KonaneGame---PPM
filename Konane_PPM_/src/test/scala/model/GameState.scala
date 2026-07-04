package model

case class GameState(board: Board, currentPlayer: Stone, openCoords: List[Coord2D], timeLimitMs: Long)

object GameState:
  def initKonaneBoard(size: Int, timeLimitMs: Long = 30000L): GameState =
    val (boardOpt, openCoords) = Board.initKonaneBoard(size, size, (0, 0), (0, 1))
    boardOpt match
      case Some(board) => GameState(board, Stone.Black, openCoords, timeLimitMs)
      case None        => throw new RuntimeException("Erro ao inicializar tabuleiro Konane")

  def initial(size: Int, timeLimitMs: Long = 30000L): GameState =
    initKonaneBoard(size, timeLimitMs)
object GameTimer:
  def startTimer(): Long = System.currentTimeMillis()

  def checkTimeExceeded(startTime: Long, limitMs: Long): Boolean =
    val elapsed = System.currentTimeMillis() - startTime
    elapsed > limitMs
