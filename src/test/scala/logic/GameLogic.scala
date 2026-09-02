package logic

import model.*

import scala.annotation.tailrec

object GameLogic:

  // T1
  def randomMove(lstOpenCoords: List[Coord2D], rand: MyRandom): (Coord2D, MyRandom) =
    val (index, newRand) = rand.nextInt(lstOpenCoords.length)
    (lstOpenCoords(index), newRand)

  // verifica se um unico salto é valido: ortogonal, distância 2, captura exatamente uma pedra inimiga no meio, e destino vazio.
  private def isSingleJumpValid(board: Board, player: Stone, from: Coord2D, to: Coord2D): Boolean =
    val enemy = if player == Stone.Black then Stone.White else Stone.Black
    val (r1, c1) = from
    val (r2, c2) = to
    val dr = r2 - r1
    val dc = c2 - c1
    val isOrthogonal = (dr == 0) != (dc == 0)
    val isDistanceTwo = math.abs(dr) + math.abs(dc) == 2
    val midPos: Coord2D = (r1 + dr / 2, c1 + dc / 2)
    isOrthogonal && isDistanceTwo &&
      board.get(from).contains(player) &&
      !board.contains(to) &&
      board.get(midPos).contains(enemy)

  // devolve a pedra capturada num salto simples
  private def capturedPos(from: Coord2D, to: Coord2D): Coord2D =
    val (r1, c1) = from
    val (r2, c2) = to
    ((r1 + r2) / 2, (c1 + c2) / 2)

  // infere os limites do tabuleiro a partir das chaves do board
  private def boardBounds(board: Board): (Int, Int) =
    if board.isEmpty then (0, 0)
    else (board.keys.map(_._1).max, board.keys.map(_._2).max)

  // T2
  def play(board: Board, player: Stone, coordFrom: Coord2D, coordTo: Coord2D, lstOpenCoords: List[Coord2D]): (Option[Board], List[Coord2D]) =

    // salto único
    if isSingleJumpValid(board, player, coordFrom, coordTo) then
      val captured = capturedPos(coordFrom, coordTo)
      val newBoard = List(coordFrom, captured)
        .foldLeft(board)((b, pos) => b - pos) + (coordTo -> player)
      val newOpen = coordFrom :: captured :: lstOpenCoords.filterNot(_ == coordTo)
      (Some(newBoard), newOpen)
    else
      // tentar encontrar um caminho de saltos consecutivos
      type State = (Board, Coord2D, List[Coord2D], List[Coord2D])
      val directions = List((-2, 0), (2, 0), (0, -2), (0, 2))

      @tailrec
      def procurarCaminhoSaltos(fila: List[State], visitados: Set[Coord2D]): (Option[Board], List[Coord2D]) =
        fila match
          case Nil => (None, lstOpenCoords)
          case (currBoard, currPos, captured, currOpen) :: resto =>
            if currPos == coordTo then
              (Some(currBoard), currOpen)
            else
              val proximosEstados = directions.flatMap { (dr, dc) =>
                val (cr, cc) = currPos
                val nextPos: Coord2D = (cr + dr, cc + dc)
                if !visitados.contains(nextPos) && isSingleJumpValid(currBoard, player, currPos, nextPos) then
                  val cap = capturedPos(currPos, nextPos)
                  val nb  = List(currPos, cap).foldLeft(currBoard)((b, pos) => b - pos) + (nextPos -> player)
                  val no  = currPos :: cap :: currOpen.filterNot(_ == nextPos)
                  Some((nb, nextPos, cap :: captured, no))
                else None
              }
              procurarCaminhoSaltos(resto ++ proximosEstados, visitados + currPos)

      procurarCaminhoSaltos(List((board, coordFrom, Nil, lstOpenCoords.filterNot(_ == coordFrom))), Set(coordFrom))

  // devolve todos os destinos de salto único válidos a partir de uma posição,
  // filtrando coordenadas fora dos limites do tabuleiro
  def validSingleJumps(board: Board, player: Stone, from: Coord2D): List[Coord2D] =
    val (r, c) = from
    val (maxRow, maxCol) = boardBounds(board)
    List((r - 2, c), (r + 2, c), (r, c - 2), (r, c + 2))
      .filter { case (tr, tc) => tr >= 0 && tr <= maxRow && tc >= 0 && tc <= maxCol }
      .filter(to => isSingleJumpValid(board, player, from, to))

  // devolve todos os movimentos válidos para um jogador
  def allValidMoves(board: Board, player: Stone): List[(Coord2D, Coord2D)] =
    board.filter { case (_, s) => s == player }
      .keys.toList
      .flatMap(from => validSingleJumps(board, player, from).map(to => (from, to)))

  // T3 — joga de forma aleatória usando a função f para selecionar uma coordenada livre
  def playRandomly(board: Board, r: MyRandom, player: Stone, lstOpenCoords: List[Coord2D], f: (List[Coord2D], MyRandom) => (Coord2D, MyRandom)): (Option[Board], MyRandom, List[Coord2D], Option[Coord2D]) =
    val moves = allValidMoves(board, player)
    moves match
      case Nil =>
        (None, r, lstOpenCoords, None)
      case _ =>
        val fromCoords = moves.map(_._1).distinct
        val (selectedFrom, r2) = f(fromCoords, r)
        val destinations = moves.collect { case (`selectedFrom`, to) => to }
        val (selectedTo, r3) = f(destinations, r2)
        val (newBoardOpt, newOpen) = play(board, player, selectedFrom, selectedTo, lstOpenCoords)
        (newBoardOpt, r3, newOpen, Some(selectedTo))
