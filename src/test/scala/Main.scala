import ui.*

import scala.io.StdIn.readLine

object Main:
  @main def run(): Unit =
    println("Escolha a interface: [1] TUI  [2] GUI")
    readLine("> ").trim match
      case "2" => MainGUI.main(Array.empty)
      case _   => TUI.startMenu()
