package ui

import model.GameState

case class GameConfig(
  boardSize      : Int               = 6,
  timerSecs      : Int               = 30,
  isVsAI         : Boolean           = false,
  difficulty     : String            = "none",
  initialRemoval : String            = "corner-tl-h",
  loadedState    : Option[GameState] = None
):
  def removalCoords(size: Int): ((Int, Int), (Int, Int)) =
    initialRemoval match
      case "corner-tl-h" => ((0,        0       ), (0,        1       ))
      case "corner-tl-v" => ((0,        0       ), (1,        0       ))
      case "corner-tr-h" => ((0,        size - 1), (0,        size - 2))
      case "corner-tr-v" => ((0,        size - 1), (1,        size - 1))
      case "corner-bl-h" => ((size - 1, 0       ), (size - 1, 1       ))
      case "corner-bl-v" => ((size - 1, 0       ), (size - 2, 0       ))
      case "corner-br-h" => ((size - 1, size - 1), (size - 1, size - 2))
      case "corner-br-v" => ((size - 1, size - 1), (size - 2, size - 1))
      case "center"      => ((size / 2 - 1, size / 2 - 1), (size / 2 - 1, size / 2))
      case _             => ((0, 0), (0, 1))
