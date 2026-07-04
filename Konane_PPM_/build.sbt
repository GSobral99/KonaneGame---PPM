ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.2"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"

val javaFXVersion = "25.0.2"
val os = System.getProperty("os.name").toLowerCase match {
  case n if n.contains("win")   => "win"
  case n if n.contains("mac")   => "mac"
  case _                        => "linux"
}

libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-controls" % javaFXVersion classifier os,
  "org.openjfx" % "javafx-fxml"     % javaFXVersion classifier os,
  "org.openjfx" % "javafx-graphics" % javaFXVersion classifier os,
)

lazy val root = (project in file("."))
  .settings(
    name := "JG3_DanielMasqueiro129853_GoncaloSobral129850_RafaelSilva129834",
    Compile / mainClass := Some("ui.Main"),
    fork := true,
    javaOptions ++= Seq(
      "--module-path", (Compile / dependencyClasspath).value
        .map(_.data).filter(_.getName.contains("javafx")).mkString(java.io.File.pathSeparator),
      "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics"
    )
  )