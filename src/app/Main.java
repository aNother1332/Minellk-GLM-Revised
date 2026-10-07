package app;

import javafx.application.Application;
import javafx.stage.Stage;
import model.AudioPlayer;
import ui.ScreenManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        AudioPlayer.backgroundsound();
    }

    @Override
    public void stop() {
        AudioPlayer.stopmusic();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
