import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import ui.Screen;
import ui.ScreenManager;

import java.util.Set;

/**
 * 开发用诊断：记录场景收到的每一个真实鼠标事件（坐标 + 命中的节点），
 * 并用 JavaFX Robot（真实输入管线）点击主菜单按钮验证界面切换。
 */
public class MenuClickTest extends Application {

    private Robot robot;
    private int clicks = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                System.out.println("[uncaught] " + e));
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(60);
        stage.setY(60);
        stage.show();
        AudioPlayer.backgroundsound();

        Scene scene = ScreenManager.stage().getScene();
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Object hit = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            String node = hit == null ? "null" : hit.getClass().getSimpleName();
            String text = hit instanceof Button b ? "[" + b.getText() + "]" : "";
            System.out.println("[event] PRESSED scene=(" + (int) e.getSceneX() + "," + (int) e.getSceneY()
                    + ") screen=(" + (int) e.getScreenX() + "," + (int) e.getScreenY()
                    + ") hit=" + node + text);
        });

        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(1.2), e -> dumpHBox()),
                new javafx.animation.KeyFrame(Duration.seconds(1.5), e -> clickButton("开始新游戏")),
                new javafx.animation.KeyFrame(Duration.seconds(3.0), e -> System.out.println(
                        "[result] after 开始新游戏 -> " + currentScreenName())),
                new javafx.animation.KeyFrame(Duration.seconds(3.2), e -> clickButton("简单模式")),
                new javafx.animation.KeyFrame(Duration.seconds(5.0), e -> System.out.println(
                        "[result] after 简单模式 -> " + currentScreenName())),
                new javafx.animation.KeyFrame(Duration.seconds(5.4), e -> Platform.exit()));
        tl.play();
    }

    private Screen currentScreen() {
        try {
            java.lang.reflect.Field f = ScreenManager.class.getDeclaredField("current");
            f.setAccessible(true);
            return (Screen) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String currentScreenName() {
        Screen s = currentScreen();
        return s == null ? "null" : s.getClass().getSimpleName();
    }

    private Button findButton(String text) {
        Scene scene = currentScreen().scene();
        Set<Node> nodes = scene.getRoot().lookupAll(".button");
        for (Node n : nodes) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                return b;
            }
        }
        return null;
    }

    private void clickButton(String text) {
        Button btn = findButton(text);
        if (btn == null) {
            System.out.println("[robot] button not found or disabled: " + text);
            return;
        }
        Bounds b = btn.localToScreen(btn.getBoundsInLocal());
        int x = (int) (b.getMinX() + b.getWidth() / 2);
        int y = (int) (b.getMinY() + b.getHeight() / 2);
        robot = new Robot();
        System.out.println("[robot] click " + text + " at screen (" + x + "," + y + ")");
        robot.mouseMove(x, y);
        // 给悬浮事件留一帧时间，再按下
        new Timeline(new KeyFrame(Duration.millis(200),
                e -> robot.mouseClick(MouseButton.PRIMARY))).play();
    }

    private void dumpHBox() {
        Scene scene = ScreenManager.stage().getScene();
        for (Node n : scene.getRoot().lookupAll("HBox")) {
            System.out.println("[bounds] HBox layoutBounds=" + n.getLayoutBounds()
                    + " atScene=" + n.localToScene(0, 0));
        }
    }
}
