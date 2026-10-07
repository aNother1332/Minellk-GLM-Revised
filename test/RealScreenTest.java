import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameDifficulty;
import ui.ScreenManager;

/**
 * 开发用工具：在真实窗口中依次展示「游戏界面 + 通知横幅」和「确认弹窗」，
 * 供外部系统级截屏核对真实渲染效果（snapshot 会掩盖透明合成问题）。
 */
public class RealScreenTest extends Application {

    private Stage dialogStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setX(0);
        stage.setY(0);
        stage.setAlwaysOnTop(true);
        stage.show();
        AudioPlayer.backgroundsound();

        // 游戏窗口置顶并定位到 (0,0)，方便外部截屏
        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.2), e -> invoke("startGame")),
                new KeyFrame(Duration.seconds(1.6), e -> invokeToast("消除了[橡木原木]×2，+10分（连消×3，额外加分）")),
                new KeyFrame(Duration.seconds(5.5), e -> invokeToast("使用钻石镐消除了煤矿石×1")),
                new KeyFrame(Duration.seconds(9.0), e -> openDialog()),
                new KeyFrame(Duration.seconds(24.0), e -> Platform.exit()));
        tl.play();

        // 弹窗打开 10 秒后由后台线程经 Platform.runLater 关闭，让时间线继续
        Thread closer = new Thread(() -> {
            try {
                Thread.sleep(19000);
                Platform.runLater(() -> {
                    if (dialogStage != null && dialogStage.isShowing()) {
                        dialogStage.close();
                    }
                });
            } catch (InterruptedException ignored) {
            }
        });
        closer.setDaemon(true);
        closer.start();
    }

    private void openDialog() {
        // 场景内覆盖层不阻塞线程，直接调用即可
        ui.PixelDialog.choice(gameWindow(), "确定要重新开始游戏吗？", 420,
                new String[]{"确定", "取消"}, idx -> { });
    }

    private Window gameWindow() {
        return ScreenManager.stage().getScene().getWindow();
    }

    private void invoke(String name) {
        try {
            Object game = current();
            java.lang.reflect.Method m = game.getClass().getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(game);
        } catch (Exception e) {
            System.out.println("[preview] " + e);
        }
    }

    private void invokeToast(String text) {
        try {
            Object game = current();
            java.lang.reflect.Method m = game.getClass().getDeclaredMethod("showToast", String.class);
            m.setAccessible(true);
            m.invoke(game, text);
        } catch (Exception e) {
            System.out.println("[preview] " + e);
        }
    }

    private Object current() {
        try {
            java.lang.reflect.Field f = ScreenManager.class.getDeclaredField("current");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
