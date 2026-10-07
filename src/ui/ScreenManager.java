package ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import model.AudioPlayer;
import model.GameDifficulty;
import model.Record;

/**
 * 界面导航：持有唯一的 Stage，各界面通过 Scene 切换。
 */
public final class ScreenManager {

    private static Stage stage;
    private static Screen current;

    private ScreenManager() {}

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(720);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> {
            boolean intercepted = current != null && current.onCloseRequest();
            if (intercepted) {
                e.consume();
            } else {
                AudioPlayer.stopmusic();
                Platform.exit();
            }
        });
    }

    public static Stage stage() {
        return stage;
    }

    public static void show(Screen screen) {
        current = screen;
        stage.setScene(screen.scene());
        if (screen.width() > 0 && screen.height() > 0) {
            stage.setWidth(screen.width());
            stage.setHeight(screen.height());
        }
        stage.setResizable(screen.resizable());
        stage.setTitle(screen.title());
        stage.centerOnScreen();
        stage.show();
    }

    public static void showMainMenu() {
        show(MainMenuScreen.instance());
    }

    public static void showGame(GameDifficulty difficulty) {
        show(new GameScreen(difficulty));
    }

    public static void showGame(Record record) {
        show(new GameScreen(record));
    }
}
