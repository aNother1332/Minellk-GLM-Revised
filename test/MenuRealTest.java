import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import ui.MenuCards;
import ui.ScreenManager;

/** 开发用工具：在真实窗口中展示登录窗（圆角）与排行榜卡片，供外部系统级截屏。 */
public class MenuRealTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(0);
        stage.setY(0);
        stage.setAlwaysOnTop(true);
        stage.show();
        AudioPlayer.backgroundsound();

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> ui.MenuCards.showLogin(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(6.0), e ->
                        ui.PixelDialog.closeTop(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(7.0), e ->
                        MenuCards.showLeaderboard(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(20.0), e -> Platform.exit()));
        tl.play();
    }
}
