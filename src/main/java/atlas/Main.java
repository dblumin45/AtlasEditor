package atlas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static Stage stage;
    @Override
    public void start(Stage primaryStage) throws Exception{
        stage = primaryStage;
        Parent root = FXMLLoader.<Parent>load(getClass().getResource("/format.fxml"));
        primaryStage.setTitle("Atlas Editor");
        primaryStage.setScene(new Scene(root, 1000, 800));
        primaryStage.show();

    }

    public static void setTitle(String fileName) {
        stage.setTitle("Atlas Editor -- " + fileName);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
