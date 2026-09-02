package ui

import model.*
import logic.*

import scala.io.StdIn.readLine
import scala.annotation.tailrec
import java.io.{BufferedReader, BufferedWriter, File, FileReader, FileWriter}
import scala.collection.parallel.CollectionConverters.ImmutableMapIsParallelizable
import scala.util.{Try, Using}

object TUI:

  // ── Saves ficam na raiz do projeto ──────────────────────────────────────
  private val SAVE_DIR = "."

  // Devolve todos os ficheiros .txt na raiz do projeto
  private def listSaveFiles(): List[File] =
    val dir = new File(SAVE_DIR)
    if !dir.exists() || !dir.isDirectory then Nil
    else dir.listFiles().filter(f => f.isFile && f.getName.endsWith(".txt")).toList

  // ─── Menus ───────────────────────────────────────────────────────────────
  @tailrec
  def startMenu(): Unit =
    println("\n╔══════════════════════════╗")
    println("║       KŌNANE TUI         ║")
    println("╠══════════════════════════╣")
    println("║  1. Novo Jogo            ║")
    println("║  2. Continuar jogo       ║")
    println("║  3. Sair                 ║")
    println("╚══════════════════════════╝")
    readLine("> ").trim match
      case "1" => configMenu()
      case "2" => loadAndContinue()
      case "3" => println("A sair... Até logo!")
      case _ => println("Opção inválida."); startMenu()

  def configMenu(): Unit =
    println("\n--- Configuração ---")

    println("Modo de Jogo:")
    println(" 1. Jogador Vs Jogador")
    println(" 2. Jogador Vs Computador")
    val modeChoice = readLine("> ").trim
    val isVsAI = modeChoice == "2"

    val size = readValidInt(
      "Tamanho do tabuleiro (mínimo 4, ex: 6 para 6x6):",
      n => n >= 4,
      "Deve ser >= 4"
    )

    val timeSec = readValidLong(
      "Tempo limite por jogada em segundos (ex: 30):",
      t => t > 0,
      "Deve ser positivo"
    )

    val difficulty = if isVsAI then chooseDifficulty() else 0

    val (boardOpt, openCoords) = Board.initKonaneBoard(size, size, (0, 0), (0, 1))
    boardOpt match
      case Some(board) =>
        val initialState = GameState(board, Stone.Black, openCoords, timeSec * 1000)
        gameLoop(List(initialState), MyRandom(System.currentTimeMillis()), size, difficulty, isVsAI)
      case None =>
        println("Erro ao inicializar tabuleiro.")
        startMenu()


  private def chooseDifficulty(): Int =
    println("\nNível de dificuldade do computador:")
    println("  1. Fácil   (jogadas aleatórias)")
    println("  2. Médio   (evita movimentos que o deixam sem saída)")
    println("  3. Difícil (maximiza capturas)")
    readLine("> ").trim match
      case "1" => 1
      case "2" => 2
      case "3" => 3
      case _ => println("Opção inválida, usando Fácil."); 1

  // ─── Game loop ───────────────────────────────────────────────────────────

  @tailrec
  def gameLoop(history: List[GameState], rand: MyRandom, size: Int, difficulty: Int, isVsAI: Boolean): Unit =
    val state = history.head
    println()
    BoardPrinter.displayBoard(state.board, size, size)

    WinChecker.checkWinner(state.board, state.currentPlayer) match
      case Some(winner) =>
        val name = if winner == Stone.Black then "Preto (B)" else "Branco (W)"
        println(s"\nJogo terminado! Vencedor: $name")
        askPlayAgain(size, difficulty)

      case None =>
        val playerName = if state.currentPlayer == Stone.Black then "Preto (B)" else "Branco (W)"
        println(s"Turno de: $playerName")
        println("  [J] Jogar  [U] Undo  [S] Salvar  [M] Menu  ")
        readLine("> ").trim.toUpperCase match

          case "J" =>
            val startTime = GameTimer.startTimer()
            val (newHistory, newRand) =
              humanTurn(history, rand, state, size, difficulty, startTime, isVsAI)
            gameLoop(newHistory, newRand, size, difficulty, isVsAI)

          case "U" =>
            if history.size > 1 then
              println("↩ Jogada revertida!")
              gameLoop(history.tail, rand, size, difficulty, isVsAI)
            else
              println("Não há histórico para reverter.")
              gameLoop(history, rand, size, difficulty, isVsAI)

          case "S" =>
            saveGame(state, size)
            gameLoop(history, rand, size, difficulty, isVsAI)

          case "M" =>
            println("A voltar ao menu principal...")
            startMenu()

          case _ =>
            gameLoop(history, rand, size, difficulty, isVsAI)

  // ─── Human turn ──────────────────────────────────────────────────────────
  @tailrec
  private def humanTurn(history: List[GameState], rand: MyRandom, state: GameState, size: Int, difficulty: Int, startTime: Long, isVsAI: Boolean, forcedFrom: Option[Coord2D] = None): (List[GameState], MyRandom) =

    if GameTimer.checkTimeExceeded(startTime, state.timeLimitMs) then
      println("Tempo esgotado! Vez do adversário.")
      val next = state.copy(
        currentPlayer = if state.currentPlayer == Stone.Black then Stone.White else Stone.Black
      )
      return (next :: history, rand)

    forcedFrom match
      case None => println(s"Coordenada de origem (ex: 2 B  ou  linha coluna):")
      case Some(c) => println(s"A continuar captura com a pedra em ${coordLabel(c)}.")

    val fromOpt = forcedFrom.orElse(readCoord(size))

    fromOpt match
      case None =>
        println("Coordenada inválida.")
        humanTurn(history, rand, state, size, difficulty, startTime, isVsAI, forcedFrom)
      case Some(from) =>
        val moves = GameLogic.validSingleJumps(state.board, state.currentPlayer, from)
        if moves.isEmpty then
          println("Essa pedra não tem jogadas válidas. Escolhe outra.")
          if forcedFrom.isDefined then
            val next = state.copy(currentPlayer = if state.currentPlayer == Stone.Black then Stone.White else Stone.Black)
            (next :: history, rand)
          else
            humanTurn(history, rand, state, size, difficulty, startTime, isVsAI, None)
        else
          println(s"Destinos válidos: ${moves.map(coordLabel).mkString(", ")}")
          println("Coordenada de destino:")
          val toOpt = readCoord(size)

          toOpt match
            case None =>
              println("Coordenada inválida.")
              humanTurn(history, rand, state, size, difficulty, startTime, isVsAI, forcedFrom)
            case Some(to) =>
              if GameTimer.checkTimeExceeded(startTime, state.timeLimitMs) then
                println("Tempo esgotado durante a jogada! Vez do adversário.")
                val next = state.copy(
                  currentPlayer = if state.currentPlayer == Stone.Black then Stone.White else Stone.Black
                )
                (next :: history, rand)
              else
                val (boardOpt, newOpen) = GameLogic.play(state.board, state.currentPlayer, from, to, state.openCoords)
                boardOpt match
                  case None =>
                    println("Jogada inválida! Tenta novamente.")
                    humanTurn(history, rand, state, size, difficulty, startTime, isVsAI, forcedFrom)
                  case Some(newBoard) =>
                    println(s"✔ Jogada: ${coordLabel(from)} → ${coordLabel(to)}")
                    val temporaryState = state.copy(board = newBoard, openCoords = newOpen)

                    val nextMoves = GameLogic.validSingleJumps(newBoard, state.currentPlayer, to)
                    if nextMoves.nonEmpty then
                      println(s"Podes continuar a capturar a partir de ${coordLabel(to)}. Destinos válidos: ${nextMoves.map(coordLabel).mkString(", ")}")
                      println("Desejas continuar a capturar (C) ou Parar (P)?")
                      readLine("> ").trim.toUpperCase match
                        case "C" =>
                          humanTurn(temporaryState :: history, rand, temporaryState, size, difficulty, startTime, isVsAI, Some(to))
                        case _ =>
                          finishTurn(temporaryState, history, rand, difficulty, isVsAI)
                    else
                      finishTurn(temporaryState, history, rand, difficulty, isVsAI)

  // ─── Finaliza o turno humano e passa para o Computador ──────────────────
  private def finishTurn(state: GameState, history: List[GameState], rand: MyRandom, difficulty: Int, isVsAI: Boolean): (List[GameState], MyRandom) =
    val afterHuman = state.copy(
      currentPlayer = if state.currentPlayer == Stone.Black then Stone.White else Stone.Black
    )

    WinChecker.checkWin(afterHuman) match
      case Some(_) => (afterHuman :: history, rand)
      case None =>
        if isVsAI then
          println("\nComputador a jogar...")
          val (compState, newRand) = computerTurn(afterHuman, rand, difficulty)
          (compState :: afterHuman :: history, newRand)
        else
          (afterHuman :: history, rand)

  // ─── Computer turn ────────────────────────────────────────────────────────

  private def computerTurn(state: GameState, rand: MyRandom, difficulty: Int): (GameState, MyRandom) =
    val moves = GameLogic.allValidMoves(state.board, state.currentPlayer)
    if moves.isEmpty then
      println("Computador sem jogadas válidas.")
      (state, rand)
    else
      val ((from, to), newRand) = difficulty match
        case 3 => bestMove(state.board, state.currentPlayer, moves, rand)
        case 2 => mediumMove(state.board, state.currentPlayer, moves, rand)
        case _ => randomMoveChoice(moves, rand)

      val (boardOpt, newOpen) = GameLogic.play(state.board, state.currentPlayer, from, to, state.openCoords)
      boardOpt match
        case Some(newBoard) =>
          println(s"  Computador: ${coordLabel(from)} → ${coordLabel(to)}")
          val nextState = state.copy(
            board = newBoard,
            openCoords = newOpen,
            currentPlayer = if state.currentPlayer == Stone.Black then Stone.White else Stone.Black
          )
          (nextState, newRand)
        case None =>
          println("  Computador sem jogada válida.")
          (state, newRand)

  // ─── AI difficulty helpers ────────────────────────────────────────────────

  private def randomMoveChoice(moves: List[(Coord2D, Coord2D)], rand: MyRandom): ((Coord2D, Coord2D), MyRandom) =
    val (idx, newRand) = rand.nextInt(moves.length)
    (moves(idx), newRand)

  private def mediumMove(board: Board, player: Stone, moves: List[(Coord2D, Coord2D)], rand: MyRandom): ((Coord2D, Coord2D), MyRandom) =
    val scoredMoves = moves.map { case (from, to) =>
      val (boardOpt, _) = GameLogic.play(board, player, from, to, Nil)
      val score = boardOpt.map(b => GameLogic.allValidMoves(b, player).length).getOrElse(0)
      ((from, to), score)
    }
    val best = scoredMoves.maxBy(_._2)
    val (_, newRand) = rand.nextInt(1)
    (best._1, newRand)

  private def bestMove(board: Board, player: Stone, moves: List[(Coord2D, Coord2D)], rand: MyRandom): ((Coord2D, Coord2D), MyRandom) =
    val enemy = if player == Stone.Black then Stone.White else Stone.Black
    val enemyCountBefore = board.count(_._2 == enemy)
    val scoredMoves = moves.map { case (from, to) =>
      val (boardOpt, _) = GameLogic.play(board, player, from, to, Nil)
      val captured = boardOpt.map(b => enemyCountBefore - b.count(_._2 == enemy)).getOrElse(0)
      ((from, to), captured)
    }
    val maxScore = scoredMoves.map(_._2).max
    val bestMoves = scoredMoves.filter(_._2 == maxScore).map(_._1)
    val (idx, newRand) = rand.nextInt(bestMoves.length)
    (bestMoves(idx), newRand)

  // ─── Save / Load ─────────────────────────────────────────────────────────

  private def saveGame(state: GameState, size: Int): Unit =
    println("\nIntroduza o nome para o ficheiro de save (sem extensão):")
    val rawName = readLine("> ").trim
    if rawName.isEmpty then
      println("Nome inválido. Save cancelado.")
      return

    val safeName = rawName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
    val filePath = s"$safeName.txt"
    val file = new File(filePath)

    if file.exists() then
      println(s"O ficheiro '$safeName.txt' já existe. Deseja substituir? [S/N]")
      readLine("> ").trim.toUpperCase match
        case "S" => // continuar
        case _   => println("Save cancelado."); return

    Try {
      Using(new BufferedWriter(new FileWriter(file))) { writer =>
        val playerStr = state.currentPlayer.toString
        val timeStr   = state.timeLimitMs.toString
        val sizeStr   = size.toString
        val openStr   = state.openCoords.map { case (r, c) => s"$r,$c" }.mkString(";")
        val boardStr  = state.board.toList.map { case ((r, c), s) => s"$r,$c,${s}" }.mkString(";")
        writer.write(s"$playerStr|$timeStr|$sizeStr|$openStr|$boardStr")
      }
    } match
      case scala.util.Success(_) => println(s"Jogo guardado em '$filePath'.")
      case scala.util.Failure(e) => println(s"Erro ao guardar: ${e.getMessage}")

  private def loadAndContinue(): Unit =
    val saves = listSaveFiles()

    if saves.isEmpty then
      println("Nenhum ficheiro de save encontrado.")
      startMenu()
      return

    println("\n╔══════════════════════════════════╗")
    println("║       Ficheiros de Save          ║")
    println("╠══════════════════════════════════╣")
    saves.zipWithIndex.foreach { case (f, i) =>
      val name = f.getName.stripSuffix(".txt")
      println(f"║  ${i + 1}%2d. $name%-28s║")
    }
    println("╠══════════════════════════════════╣")
    println("║   0. Cancelar                    ║")
    println("╚══════════════════════════════════╝")
    println("Escolha o número do save a carregar:")

    val choiceOpt = readLine("> ").trim.toIntOption
    choiceOpt match
      case Some(0) =>
        startMenu()
      case Some(n) if n >= 1 && n <= saves.length =>
        val file = saves(n - 1)
        loadFromFile(file)
      case _ =>
        println("Opção inválida.")
        loadAndContinue()

  private def loadFromFile(file: File): Unit =
    Try {
      Using(new BufferedReader(new FileReader(file))) { reader =>
        reader.readLine()
      }.get
    } match
      case scala.util.Failure(_) =>
        println(s"Erro ao ler '${file.getName}'.")
        startMenu()
      case scala.util.Success(null) =>
        println("Ficheiro de save vazio.")
        startMenu()
      case scala.util.Success(line) =>
        Try {
          val parts  = line.split("\\|", -1)
          val player = if parts(0) == "Black" then Stone.Black else Stone.White
          val time   = parts(1).toLong
          val size   = parts(2).toInt
          val open   = if parts(3).isEmpty then Nil else parts(3).split(";").map { s =>
            val a = s.split(","); (a(0).toInt, a(1).toInt)
          }.toList
          val board: Board = parts(4).split(";").map { s =>
            val a     = s.split(",")
            val stone = if a(2) == "Black" then Stone.Black else Stone.White
            (a(0).toInt, a(1).toInt) -> stone
          }.toMap.par
          (GameState(board, player, open, time), size)
        } match
          case scala.util.Failure(e) =>
            println(s"Erro ao carregar save: ${e.getMessage}")
            startMenu()
          case scala.util.Success((state, size)) =>
            println(s"Jogo '${file.getName.stripSuffix(".txt")}' carregado com sucesso!")

            println("\nComo desejas continuar este jogo?")
            println("  1. Jogador vs Jogador (PvP)")
            println("  2. Jogador vs Computador (PvE)")

            val modeChoice = readLine("> ").trim
            val isVsAI = modeChoice == "2"
            val diff = if isVsAI then chooseDifficulty() else 0

            gameLoop(List(state), MyRandom(System.currentTimeMillis()), size, diff, isVsAI)

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private def readCoord(size: Int): Option[Coord2D] =
    val input = readLine("> ").trim.toUpperCase.split("\\s+")
    scala.util.Try {
      val row = input(0).toInt
      val col = input(1).head - 'A'
      if row >= 0 && row < size && col >= 0 && col < size then Some((row, col))
      else None
    }.getOrElse(None)

  private def coordLabel(coord: Coord2D): String =
    val (r, c) = coord
    s"$r ${('A' + c).toChar}"

  private def askPlayAgain(size: Int, difficulty: Int): Unit =
    println("\nJogar novamente? [S/N]")
    readLine("> ").trim.toUpperCase match
      case "S" => configMenu()
      case _ => startMenu()

  @tailrec
  private def readValidInt(prompt: String, pred: Int => Boolean, err: String): Int =
    println(prompt)
    readLine("> ").trim.toIntOption match
      case Some(n) if pred(n) => n
      case _ => println(s"Valor inválido: $err"); readValidInt(prompt, pred, err)

  @tailrec
  private def readValidLong(prompt: String, pred: Long => Boolean, err: String): Long =
    println(prompt)
    readLine("> ").trim.toLongOption match
      case Some(n) if pred(n) => n
      case _ => println(s"Valor inválido: $err"); readValidLong(prompt, pred, err)

