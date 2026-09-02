package ui

import javafx.animation.{KeyFrame, Timeline}
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.{GridPane, StackPane, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.util.Duration
import logic.*
import model.*

import java.io.*
import scala.collection.parallel.CollectionConverters.ImmutableMapIsParallelizable
import scala.compiletime.uninitialized
import scala.util.Try
import scala.jdk.CollectionConverters.*

class AppController:

  // ── FXML -  ecrãs principais ───────────────────────────────────────────────
  @FXML var gamePane    : javafx.scene.layout.BorderPane = uninitialized
  @FXML var menuPane    : StackPane = uninitialized
  @FXML var settingsPane: StackPane = uninitialized

  // ── FXML - overlays de jogo ───────────────────────────────────────────────
  @FXML var winnerPane  : StackPane = uninitialized
  @FXML var savePane    : StackPane = uninitialized
  @FXML var loadPane    : StackPane = uninitialized

  // ── FXML - jogo ───────────────────────────────────────────────────────────
  @FXML var boardGrid     : GridPane = uninitialized
  @FXML var lblStatus     : Label    = uninitialized
  @FXML var lblTimer      : Label    = uninitialized
  @FXML var lblMessage    : Label    = uninitialized
  @FXML var btnUndo       : Button   = uninitialized
  @FXML var btnRestart    : Button   = uninitialized
  @FXML var btnStopCapture: Button   = uninitialized
  @FXML var btnSave       : Button   = uninitialized
  @FXML var btnMenu       : Button   = uninitialized
  @FXML var lblWinner     : Label    = uninitialized

  // ── FXML — settings ───────────────────────────────────────────────────────
  @FXML var rbPvP         : RadioButton = uninitialized
  @FXML var rbPvAI        : RadioButton = uninitialized
  @FXML var difficultyBox : VBox        = uninitialized
  @FXML var rbEasy        : RadioButton = uninitialized
  @FXML var rbMedium      : RadioButton = uninitialized
  @FXML var rbHard        : RadioButton = uninitialized
  @FXML var rbInitTLH     : RadioButton = uninitialized
  @FXML var rbInitTLV     : RadioButton = uninitialized
  @FXML var rbInitTRH     : RadioButton = uninitialized
  @FXML var rbInitTRV     : RadioButton = uninitialized
  @FXML var rbInitBLH     : RadioButton = uninitialized
  @FXML var rbInitBLV     : RadioButton = uninitialized
  @FXML var rbInitBRH     : RadioButton = uninitialized
  @FXML var rbInitBRV     : RadioButton = uninitialized
  @FXML var rbInitCenter  : RadioButton = uninitialized
  @FXML var rb15s         : RadioButton = uninitialized
  @FXML var rb30s         : RadioButton = uninitialized
  @FXML var rb60s         : RadioButton = uninitialized
  @FXML var rbNoTimer     : RadioButton = uninitialized
  @FXML var lblSummaryMode      : Label = uninitialized
  @FXML var lblSummaryDifficulty: Label = uninitialized
  @FXML var lblSummaryInit      : Label = uninitialized
  @FXML var lblSummaryTimer     : Label = uninitialized

  // ── FXML — save/load overlays ─────────────────────────────────────────────
  @FXML var saveNameInput: TextField        = uninitialized
  @FXML var lblSaveMsg   : Label            = uninitialized
  @FXML var saveFilesList: ListView[String] = uninitialized
  @FXML var lblLoadMsg   : Label            = uninitialized

  // ── Estado do jogo ────────────────────────────────────────────────────────
  private var config: GameConfig = GameConfig()

  private var history      : List[GameState]  = Nil
  private var selectedCoord: Option[Coord2D]  = None
  private var jumpInProgress: Boolean         = false
  private var currentStone : Option[Coord2D]  = None
  private var timeline     : Timeline         = uninitialized
  private var boardSize    : Int              = 6

  private val SAVE_DIR = "."

  // ── Inicialização ─────────────────────────────────────────────────────────

  @FXML
  def initialize(): Unit =
    setupSettingsToggleGroups()
    showMenu()

  // ─── Navegação entre camadas ──────────────────────────────────────────────

  private def showOnly(pane: StackPane): Unit =
    Seq(menuPane, settingsPane).foreach(p => setOverlay(p, visible = false))
    setOverlay(pane, visible = true)

  private def setOverlay(pane: StackPane, visible: Boolean): Unit =
    pane.setVisible(visible)
    pane.setManaged(visible)

  private def showMenu(): Unit     = showOnly(menuPane)
  private def showSettings(): Unit = showOnly(settingsPane)
  private def hideAllOverlays(): Unit =
    Seq(menuPane, settingsPane, winnerPane, savePane, loadPane)
      .foreach(p => setOverlay(p, visible = false))

  // ── Handlers do MENU ─────────────────────────────────────────────────────

  @FXML def handleMenuPlay(): Unit = showSettings()

  @FXML
  def handleShowLoad(): Unit =
    val saves = listSaveFiles()
    if saves.isEmpty then
      lblLoadMsg.setText("Nenhum ficheiro de save encontrado.")
    else
      lblLoadMsg.setText("")
      val items = saves.map(_.getName.stripSuffix(".txt")).asJava
      saveFilesList.getItems.setAll(items)
    setOverlay(loadPane, visible = true)

  // ── Handlers de SETTINGS ─────────────────────────────────────────────────

  @FXML
  def handleModeChange(): Unit =
    val isPvAI = rbPvAI.isSelected
    difficultyBox.setVisible(isPvAI)
    difficultyBox.setManaged(isPvAI)
    updateSummary()

  @FXML def handleInitChange(): Unit = updateSummary()

  @FXML
  def handleStartGame(): Unit =
    config = GameConfig(
      timerSecs =
        if rb15s.isSelected then 15
        else if rb60s.isSelected then 60
        else if rbNoTimer.isSelected then -1
        else 30,
      isVsAI = rbPvAI.isSelected,
      difficulty =
        if rbPvAI.isSelected then
          if rbEasy.isSelected then "easy"
          else if rbMedium.isSelected then "medium"
          else "hard"
        else "none",
      initialRemoval =
        if      rbInitTLH.isSelected    then "corner-tl-h"
        else if rbInitTLV.isSelected    then "corner-tl-v"
        else if rbInitTRH.isSelected    then "corner-tr-h"
        else if rbInitTRV.isSelected    then "corner-tr-v"
        else if rbInitBLH.isSelected    then "corner-bl-h"
        else if rbInitBLV.isSelected    then "corner-bl-v"
        else if rbInitBRH.isSelected    then "corner-br-h"
        else if rbInitBRV.isSelected    then "corner-br-v"
        else if rbInitCenter.isSelected then "center"
        else "corner-tl-h"
    )
    hideAllOverlays()
    startNewGame()

  @FXML def handleSettingsBack(): Unit = showMenu()
  
  // ── Jogo: arranque ────────────────────────────────────────────────────────

  private def startNewGame(): Unit =
    boardSize = config.boardSize
    val timerSecs   = if config.timerSecs == -1 then Int.MaxValue / 1000 else config.timerSecs
    val timeLimitMs = timerSecs * 1000L

    val (p1, p2) = config.removalCoords(boardSize)
    val (boardOpt, openCoords) = Board.initKonaneBoard(boardSize, boardSize, p1, p2)
    boardOpt match
      case Some(board) =>
        val state = GameState(board, Stone.Black, openCoords, timeLimitMs)
        history = List(state)
        jumpInProgress = false
        selectedCoord = None
        currentStone = None
        updateUI()
        startTimerForTurn()
      case None =>
        lblMessage.setText("Erro ao iniciar o tabuleiro.")

  private def loadState(state: GameState): Unit =
    history = List(state)
    jumpInProgress = false
    selectedCoord = None
    currentStone = None
    hideAllOverlays()
    updateUI()
    startTimerForTurn()
    runComputerTurnIfNeeded()

  // ── Timer ─────────────────────────────────────────────────────────────────
  private def startTimerForTurn(): Unit =
    if timeline != null then timeline.stop()
    if config.timerSecs == -1 then
      lblTimer.setText("∞")
      return
    val timeLimit = history.head.timeLimitMs
    val startTime = System.currentTimeMillis()

    timeline = new Timeline(new KeyFrame(Duration.millis(100), (_: ActionEvent) => {
      val remaining = math.max(0L, timeLimit - (System.currentTimeMillis() - startTime))
      val s = remaining / 1000
      lblTimer.setText(f"${s / 60}%02d:${s % 60}%02d")
      if remaining <= 0 then
        timeline.stop()
        lblMessage.setText("Tempo esgotado! Turno do adversário.")
        passTurnByTimeout()
    }))
    timeline.setCycleCount(javafx.animation.Animation.INDEFINITE)
    timeline.play()

  private def passTurnByTimeout(): Unit =
    val state = history.head
    val next  = state.copy(currentPlayer = toggle(state.currentPlayer))
    history = next :: history
    jumpInProgress = false
    selectedCoord = None
    currentStone = None
    updateUI()
    checkWin()
    if !isGameOver then
      startTimerForTurn()
      runComputerTurnIfNeeded()

  // ── UI do tabuleiro ───────────────────────────────────────────────────────

  private def updateUI(): Unit =
    boardGrid.getChildren.clear()
    val state     = history.head
    val validDest = getValidDestinations()

    for r <- 0 until boardSize; c <- 0 until boardSize do
      val coord = (r, c)
      val pane  = new StackPane()
      pane.setStyle("-fx-border-color: #808080; -fx-border-width: 1;")

      if selectedCoord.contains(coord) then
        pane.setStyle("-fx-background-color: #0066CC; -fx-border-color: #808080;")
      else if validDest.contains(coord) then
        pane.setStyle("-fx-background-color: #99CCFF; -fx-border-color: #808080;")

      state.board.get(coord) match
        case Some(Stone.Black) => pane.getChildren.add(new Circle(20, Color.BLACK))
        case Some(Stone.White) => pane.getChildren.add(new Circle(20, Color.WHITE))
        case None              =>

      val isHumanTurn = !config.isVsAI || state.currentPlayer == Stone.Black
      if isHumanTurn then
        val btn = new Button()
        btn.setMaxSize(Double.MaxValue, Double.MaxValue)
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;")
        val (row, col) = (r, c)
        btn.setOnAction((_: ActionEvent) => handleCellClick(row, col))
        pane.getChildren.add(btn)

      boardGrid.add(pane, c, r)

    val p        = if state.currentPlayer == Stone.Black then "Preto" else "Branco"
    val aiSuffix = if config.isVsAI && state.currentPlayer == Stone.White then " (Computador)" else ""
    lblStatus.setText(s"Turno: $p$aiSuffix")
    btnStopCapture.setDisable(!jumpInProgress)
    btnUndo.setDisable(history.tail.isEmpty)

  private def getValidDestinations(): List[Coord2D] =
    selectedCoord match
      case Some(from) => GameLogic.validSingleJumps(history.head.board, history.head.currentPlayer, from)
      case None       => Nil

  // ── Click / jogada ────────────────────────────────────────────────────────

  private def handleCellClick(r: Int, c: Int): Unit =
    if isGameOver then return
    val state   = history.head
    if config.isVsAI && state.currentPlayer == Stone.White then return
    val clicked = (r, c)

    if jumpInProgress then
      if clicked != currentStone.get && getValidDestinations().contains(clicked) then
        executePlay(currentStone.get, clicked)
      else if clicked != currentStone.get then
        lblMessage.setText("Deve continuar com a pedra atual ou parar.")
    else
      selectedCoord match
        case None =>
          if state.board.get(clicked).contains(state.currentPlayer) then
            selectedCoord = Some(clicked)
            updateUI()
        case Some(from) =>
          if clicked == from then
            selectedCoord = None
            updateUI()
          else if getValidDestinations().contains(clicked) then
            executePlay(from, clicked)
          else if state.board.get(clicked).contains(state.currentPlayer) then
            selectedCoord = Some(clicked)
            updateUI()
          else
            lblMessage.setText("Jogada inválida!")

  private def executePlay(from: Coord2D, to: Coord2D): Unit =
    val state = history.head
    val (newBoardOpt, newOpen) = GameLogic.play(state.board, state.currentPlayer, from, to, state.openCoords)
    newBoardOpt match
      case None     => lblMessage.setText("Erro na jogada!")
      case Some(nb) =>
        val further = GameLogic.validSingleJumps(nb, state.currentPlayer, to)
        if further.nonEmpty then
          history = state.copy(board = nb, openCoords = newOpen) :: history
          jumpInProgress = true
          currentStone = Some(to)
          selectedCoord = Some(to)
          lblMessage.setText("Pode continuar a capturar ou parar.")
          updateUI()
        else
          val next = state.copy(board = nb, openCoords = newOpen, currentPlayer = toggle(state.currentPlayer))
          history = next :: history
          jumpInProgress = false
          selectedCoord = None
          currentStone = None
          lblMessage.setText("Pronto.")
          updateUI()
          checkWin()
          if !isGameOver then
            startTimerForTurn()
            runComputerTurnIfNeeded()

  // ── Handlers dos botões do jogo ───────────────────────────────────────────

  @FXML
  def handleUndo(): Unit =
    if history.tail.nonEmpty then
      history = history.tail
      if config.isVsAI && history.head.currentPlayer == Stone.White && history.tail.nonEmpty then
        history = history.tail
      jumpInProgress = false
      selectedCoord = None
      currentStone = None
      lblMessage.setText("Jogada revertida.")
      updateUI()
      if !isGameOver then startTimerForTurn()

  @FXML
  def handleRestart(): Unit =
    setOverlay(winnerPane, visible = false)
    startNewGame()

  @FXML
  def handleStopCapture(): Unit =
    if jumpInProgress then
      val state = history.head
      val next  = state.copy(currentPlayer = toggle(state.currentPlayer))
      history = next :: history
      jumpInProgress = false
      selectedCoord = None
      currentStone = None
      lblMessage.setText("Captura interrompida. Turno do adversário.")
      updateUI()
      checkWin()
      if !isGameOver then
        startTimerForTurn()
        runComputerTurnIfNeeded()

  @FXML
  def handleShowMenu(): Unit =
    if timeline != null then timeline.stop()
    setOverlay(winnerPane, visible = false)
    showMenu()

  // ── Save overlay ──────────────────────────────────────────────────────────

  @FXML
  def handleShowSave(): Unit =
    saveNameInput.clear()
    lblSaveMsg.setText("")
    setOverlay(savePane, visible = true)

  @FXML
  def handleConfirmSave(): Unit =
    val rawName = saveNameInput.getText.trim
    if rawName.isEmpty then
      lblSaveMsg.setText("Nome inválido.")
    else
      val safeName = rawName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
      val file     = new File(s"$safeName.txt")
      val state    = history.head
      Try {
        val writer = new BufferedWriter(new FileWriter(file))
        try
          val playerStr = state.currentPlayer.toString
          val timeStr   = state.timeLimitMs.toString
          val sizeStr   = boardSize.toString
          val openStr   = state.openCoords.map { case (r, c) => s"$r,$c" }.mkString(";")
          val boardStr  = state.board.toList.map { case ((r, c), s) => s"$r,$c,$s" }.mkString(";")
          writer.write(s"$playerStr|$timeStr|$sizeStr|$openStr|$boardStr")
        finally
          writer.close()
      } match
        case scala.util.Success(_) =>
          lblSaveMsg.setText(s"Guardado em '$safeName.txt'.")
          setOverlay(savePane, visible = false)
          lblMessage.setText(s"Jogo guardado em '$safeName.txt'.")
        case scala.util.Failure(e) =>
          lblSaveMsg.setText(s"Erro: ${e.getMessage}")

  @FXML
  def handleCancelSave(): Unit = setOverlay(savePane, visible = false)

  // ── Load overlay ──────────────────────────────────────────────────────────

  @FXML
  def handleConfirmLoad(): Unit =
    val selected = saveFilesList.getSelectionModel.getSelectedItem
    if selected == null then
      lblLoadMsg.setText("Seleciona um ficheiro.")
    else
      val file = new File(s"$selected.txt")
      if !file.exists() then
        lblLoadMsg.setText(s"'$selected.txt' não encontrado.")
      else
        loadFromFile(file)

  @FXML
  def handleCancelLoad(): Unit = setOverlay(loadPane, visible = false)

  private def loadFromFile(file: File): Unit =
    val lineOpt: Option[String] = Try {
      val reader = new BufferedReader(new FileReader(file))
      try Option(reader.readLine())
      finally reader.close()
    }.toOption.flatten

    lineOpt match
      case None =>
        lblLoadMsg.setText(s"Erro ao ler '${file.getName}'.")
      case Some(line) =>
        Try {
          val parts  = line.split("\\|", -1)
          val player = if parts(0) == "Black" then Stone.Black else Stone.White
          val time   = parts(1).toLong
          val size   = parts(2).toInt
          val open   =
            if parts(3).isEmpty then Nil
            else parts(3).split(";").map { s =>
              val a = s.split(",")
              (a(0).toInt, a(1).toInt)
            }.toList
          val board: Board = parts(4).split(";").map { s =>
            val a     = s.split(",")
            val stone = if a(2) == "Black" then Stone.Black else Stone.White
            (a(0).toInt, a(1).toInt) -> stone
          }.toMap.par
          (GameState(board, player, open, time), size)
        } match
          case scala.util.Failure(e) =>
            lblLoadMsg.setText(s"Erro ao carregar: ${e.getMessage}")
          case scala.util.Success((state, size)) =>
            if timeline != null then timeline.stop()
            boardSize = size
            loadState(state)

  // ── Computador ────────────────────────────────────────────────────────────

  private def runComputerTurnIfNeeded(): Unit =
    if config.isVsAI && history.head.currentPlayer == Stone.White then
      doComputerTurn()

  private def doComputerTurn(): Unit =
    val state = history.head
    val moves = GameLogic.allValidMoves(state.board, state.currentPlayer)
    if moves.isEmpty then
      checkWin()
    else
      val rand = MyRandom(System.currentTimeMillis())
      val ((from, to), _) = config.difficulty match
        case "hard"   => bestMove(state.board, state.currentPlayer, moves, rand)
        case "medium" => mediumMove(state.board, state.currentPlayer, moves, rand)
        case _        => randomMoveChoice(moves, rand)

      GameLogic.play(state.board, state.currentPlayer, from, to, state.openCoords) match
        case (Some(nb), newOpen) =>
          val (finalBoard, finalOpen) = doComputerMultiJump(nb, state.currentPlayer, to, newOpen)
          val next = state.copy(board = finalBoard, openCoords = finalOpen, currentPlayer = Stone.Black)
          history = next :: history
          jumpInProgress = false
          selectedCoord = None
          currentStone = None
          lblMessage.setText("Computador jogou. O teu turno (Preto).")
          updateUI()
          checkWin()
          if !isGameOver then startTimerForTurn()
        case _ =>
          lblMessage.setText("Computador sem jogada válida.")

  @scala.annotation.tailrec
  private def doComputerMultiJump(board: Board, player: Stone, from: Coord2D, open: List[Coord2D]): (Board, List[Coord2D]) =
    val moves = GameLogic.validSingleJumps(board, player, from)
    if moves.isEmpty then (board, open)
    else
      GameLogic.play(board, player, from, moves.head, open) match
        case (Some(nb), newOpen) => doComputerMultiJump(nb, player, moves.head, newOpen)
        case _                   => (board, open)

  // ── AI helpers ────────────────────────────────────────────────────────────

  private def randomMoveChoice(moves: List[(Coord2D, Coord2D)], rand: MyRandom) =
    val (idx, nr) = rand.nextInt(moves.length)
    (moves(idx), nr)

  private def mediumMove(board: Board, player: Stone, moves: List[(Coord2D, Coord2D)], rand: MyRandom) =
    val best = moves.maxBy { case (f, t) =>
      GameLogic.play(board, player, f, t, Nil)._1
        .map(b => GameLogic.allValidMoves(b, player).length).getOrElse(0)
    }
    val (_, nr) = rand.nextInt(1)
    (best, nr)

  private def bestMove(board: Board, player: Stone, moves: List[(Coord2D, Coord2D)], rand: MyRandom) =
    val enemy  = toggle(player)
    val before = board.count(_._2 == enemy)
    val scored = moves.map(m =>
      m -> GameLogic.play(board, player, m._1, m._2, Nil)._1
        .map(b => before - b.count(_._2 == enemy)).getOrElse(0)
    )
    val maxS  = scored.map(_._2).max
    val bests = scored.filter(_._2 == maxS).map(_._1)
    val (idx, nr) = rand.nextInt(bests.length)
    (bests(idx), nr)

  // ── Win check ─────────────────────────────────────────────────────────────

  private def checkWin(): Unit =
    WinChecker.checkWinner(history.head.board, history.head.currentPlayer) match
      case Some(winner) =>
        if timeline != null then timeline.stop()
        val w = if winner == Stone.Black then "Preto" else "Branco"
        lblWinner.setText(s"O jogador $w venceu!")
        setOverlay(winnerPane, visible = true)
      case None =>

  private def isGameOver: Boolean = winnerPane.isVisible

  // ── Funções ───────────────────────────────────────────────────────────

  private def toggle(s: Stone): Stone =
    if s == Stone.Black then Stone.White else Stone.Black

  private def listSaveFiles(): List[File] =
    val dir = new File(SAVE_DIR)
    if !dir.exists() || !dir.isDirectory then Nil
    else dir.listFiles().filter(f => f.isFile && f.getName.endsWith(".txt")).toList.sortBy(_.getName)

  private def setupSettingsToggleGroups(): Unit =
    val modeGroup = new ToggleGroup()
    rbPvP.setToggleGroup(modeGroup)
    rbPvAI.setToggleGroup(modeGroup)

    val diffGroup = new ToggleGroup()
    rbEasy.setToggleGroup(diffGroup)
    rbMedium.setToggleGroup(diffGroup)
    rbHard.setToggleGroup(diffGroup)

    val initGroup = new ToggleGroup()
    Seq(rbInitTLH, rbInitTLV, rbInitTRH, rbInitTRV,
      rbInitBLH, rbInitBLV, rbInitBRH, rbInitBRV, rbInitCenter)
      .foreach(_.setToggleGroup(initGroup))

    val timerGroup = new ToggleGroup()
    Seq(rb15s, rb30s, rb60s, rbNoTimer).foreach(_.setToggleGroup(timerGroup))

    difficultyBox.setVisible(false)
    difficultyBox.setManaged(false)
    updateSummary()

  private def updateSummary(): Unit =
    val mode = if rbPvP.isSelected then "PvP" else "PvComputador"
    val diff =
      if rbPvAI.isSelected then
        if rbEasy.isSelected then "Fácil"
        else if rbMedium.isSelected then "Médio"
        else "Difícil"
      else "—"
    val timer =
      if rb15s.isSelected then "15s"
      else if rb60s.isSelected then "60s"
      else if rbNoTimer.isSelected then "Sem limite"
      else "30s"
    lblSummaryMode.setText(s"Modo: $mode")
    lblSummaryDifficulty.setText(s"Dificuldade: $diff")
    lblSummaryInit.setText(s"Início: $initLabel")
    lblSummaryTimer.setText(s"Tempo: $timer")

  private def initLabel: String =
    if      rbInitTLH.isSelected    then "Sup. Esq. — Horizontal"
    else if rbInitTLV.isSelected    then "Sup. Esq. — Vertical"
    else if rbInitTRH.isSelected    then "Sup. Dir. — Horizontal"
    else if rbInitTRV.isSelected    then "Sup. Dir. — Vertical"
    else if rbInitBLH.isSelected    then "Inf. Esq. — Horizontal"
    else if rbInitBLV.isSelected    then "Inf. Esq. — Vertical"
    else if rbInitBRH.isSelected    then "Inf. Dir. — Horizontal"
    else if rbInitBRV.isSelected    then "Inf. Dir. — Vertical"
    else if rbInitCenter.isSelected then "Centro"
    else "Sup. Esq. — Horizontal"