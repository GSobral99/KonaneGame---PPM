import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

class MainGUI extends Application:
  override def start(primaryStage: Stage): Unit =
    val fxmlLoader = new FXMLLoader(getClass.getResource("/KonaneApp.fxml"))
    val root = fxmlLoader.load[Parent]()
    primaryStage.setTitle("Kōnane - Projeto PPM")
    primaryStage.setScene(new Scene(root))
    root.requestFocus()
    primaryStage.show()

object MainGUI:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[MainGUI], args*)
