package model
import scala.collection.parallel.immutable.ParMap

type Coord2D = (Int, Int)
type Board = ParMap[Coord2D, Stone]

enum Stone:
  case Black, White
