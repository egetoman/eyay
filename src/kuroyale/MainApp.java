package kuroyale;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        ScreenNavigator navigator = new ScreenNavigator(primaryStage);
        navigator.showWelcomeScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}