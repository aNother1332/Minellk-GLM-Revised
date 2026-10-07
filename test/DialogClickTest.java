import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameDifficulty;
import ui.GameScreen;
import ui.ScreenManager;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * 开发用诊断：验证弹窗内按钮可以点击（回归「弹窗里的按钮无法点击」）。
 * 触发重新开始弹窗 → 点击其中的「确定」→ 断言弹窗关闭且游戏已重置。
 */
public class DialogClickTest extends Application {

    private GameScreen game;
    private int baselineChildren;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setX(60);
        stage.setY(60);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = (GameScreen) f.get(null);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.8), e -> startButton().fire()),
                new KeyFrame(Duration.seconds(1.4), e -> check("开局", "gameStarted", true)),
                new KeyFrame(Duration.seconds(1.6), e -> {
                    baselineChildren = game.scene().getRoot().getChildrenUnmodifiable().size();
                    findButton("重新开始").fire();
                }),
                new KeyFrame(Duration.seconds(2.2), e -> {
                    int now = game.scene().getRoot().getChildrenUnmodifiable().size();
                    System.out.println("[check] 弹窗打开: 覆盖层已叠加=" + (now == baselineChildren + 1)
                            + " (子节点 " + baselineChildren + "->" + now + ")");
                    Button confirm = findButton("确定");
                    System.out.println("[check] 弹窗内确定按钮存在: " + (confirm != null));
                    if (confirm != null) {
                        confirm.fire();
                    }
                }),
                new KeyFrame(Duration.seconds(3.4), e -> {
                    int now = game.scene().getRoot().getChildrenUnmodifiable().size();
                    System.out.println("[check] 点击确定后弹窗已关闭: " + (now == baselineChildren)
                            + " (子节点 " + now + ")");
                    check("游戏已重置", "gameStarted", false);
                    check("棋子已恢复", "tiles", 32);
                }),
                new KeyFrame(Duration.seconds(3.7), e -> Platform.exit()));
        tl.play();
    }

    private Button startButton() {
        return findButton("开始游戏");
    }

    private Button findButton(String text) {
        Scene scene = game.scene();
        Set<Node> nodes = scene.getRoot().lookupAll(".button");
        for (Node n : nodes) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                return b;
            }
        }
        return null;
    }

    private Object expectValue;

    private void check(String name, String key, Object expect) {
        expectValue = expect;
        Object actual = actualOf(key);
        boolean ok = actual.equals(expect);
        System.out.println("[check] " + name + ": " + key + "=" + actual + (ok ? "  [OK]" : "  [FAIL] 期望 " + expect));
    }

    private Object actualOf(String key) {
        try {
            return switch (key) {
                case "gameStarted" -> get("gameStarted");
                case "tiles" -> {
                    Field f = GameScreen.class.getDeclaredField("gameBoard");
                    f.setAccessible(true);
                    model.GameBoard board = (model.GameBoard) f.get(game);
                    yield board.getcellpositions().size();
                }
                default -> "?";
            };
        } catch (Exception e) {
            return "ERR:" + e.getMessage();
        }
    }

    private Object get(String field) {
        try {
            Field f = GameScreen.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
