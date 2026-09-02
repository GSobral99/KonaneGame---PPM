package logic
import model.*
//T5
object WinChecker:
  def hasValidMove(board: Board, player: Stone): Boolean =
    GameLogic.allValidMoves(board, player).nonEmpty

  def checkWinner(board: Board, currentPlayer: Stone): Option[Stone] =
    if !hasValidMove(board, currentPlayer) then
      val winner = if currentPlayer == Stone.Black then Stone.White else Stone.Black
      Some(winner)
    else
      None

  def checkWin(state: GameState): Option[Stone] =
    checkWinner(state.board, state.currentPlayer)

