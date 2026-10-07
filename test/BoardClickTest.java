import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Cell;
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import ui.BoardView;
import ui.GameScreen;
import ui.ScreenManager;
import utils.Utils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 开发用诊断：用 JavaFX Robot（真实输入管线）完整验证游戏棋盘交互：
 * 点击「开始游戏」→ 点击棋子选中 → 点击配对棋子消除。
 */
public class BoardClickTest extends Application {

    private Robot robot;
    private GameScreen game;
    private Position pairA;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setX(60);
        stage.setY(60);
        stage.show();
        AudioPlayer_backgroundsound();
        try {
            Field f = ScreenManager.class.getDeclaredField("current");
            f.setAccessible(true);
            game = (GameScreen) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SceneProbe.attach();

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.2), e -> clickButton("开始游戏")),
                new KeyFrame(Duration.seconds(2.0), e ->
                        System.out.println("[result] gameStarted=" + gameStarted() + "（期望 true）")),
                new KeyFrame(Duration.seconds(2.2), e -> clickTile("first")),
                new KeyFrame(Duration.seconds(3.0), e -> System.out.println(
                        "[result] firstSelection=" + (firstSelection() != null ? "已选中" : "null（期望已选中）"))),
                new KeyFrame(Duration.seconds(3.2), e -> clickTile("second")),
                new KeyFrame(Duration.seconds(5.4), e -> {
                    System.out.println("[result] score=" + score() + "（消除成功应为 10）"
                            + " remainingTiles=" + remainingTiles() + "（应为 30）");
                }),
                new KeyFrame(Duration.seconds(5.8), e -> Platform.exit()));
        tl.play();
    }

    private static void AudioPlayer_backgroundsound() {
        try {
            Class<?> c = Class.forName("model.AudioPlayer");
            c.getMethod("backgroundsound").invoke(null);
        } catch (Exception ignored) {
        }
    }

    /** 场景事件探针：记录每次点击命中的节点 */
    private static class SceneProbe {
        static void attach() {
            ScreenManager.stage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                Node hit = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
                String name = hit == null ? "null" : hit.getClass().getSimpleName();
                String extra = "";
                if (hit instanceof Button b) {
                    extra = "[" + b.getText() + "]";
                } else if (hit instanceof javafx.scene.image.ImageView) {
                    extra = "(棋子图标)";
                } else if (hit instanceof javafx.scene.shape.Rectangle) {
                    extra = "(选中框)";
                } else if (hit instanceof javafx.scene.canvas.Canvas) {
                    extra = "(画布!异常)";
                }
                System.out.println("[event] PRESSED scene=(" + (int) e.getSceneX() + "," + (int) e.getSceneY()
                        + ") hit=" + name + extra);
            });
        }
    }

    // ———— 反射工具 ————

    private Object get(String field) {
        try {
            Field f = game.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean gameStarted() {
        return (Boolean) get("gameStarted");
    }

    private Cell firstSelection() {
        return (Cell) get("firstSelection");
    }

    private int score() {
        return (Integer) get("score");
    }

    private GameBoard board() {
        return (GameBoard) get("gameBoard");
    }

    private BoardView boardView() {
        return (BoardView) get("boardView");
    }

    private long remainingTiles() {
        return board().getcellpositions().size();
    }

    // ———— 真实点击 ————

    private void clickButton(String text) {
        for (Node n : ScreenManager.stage().getScene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                Point2D p = b.localToScreen(b.getBoundsInLocal().getWidth() / 2,
                        b.getBoundsInLocal().getHeight() / 2);
                fireClick(p);
                System.out.println("[robot] click button [" + text + "] at (" + (int) p.getX() + "," + (int) p.getY() + ")");
                return;
            }
        }
        System.out.println("[robot] button not found: " + text);
    }

    /** 通过棋盘布局参数计算棋子中心的屏幕坐标，真实点击 */
    private void clickTile(String which) {
        try {
            BoardView view = boardView();
            GameBoard board = board();

            Position target = null;
            if ("first".equals(which)) {
                // 取第一个非空棋子
                target = board.getcellpositions().get(0);
                pairA = target;
            } else {
                // 找与 pairA 可消除的配对棋子
                for (Position p : board.getcellpositions()) {
                    if (!p.equals(pairA) && Utils.findPath(board, pairA, p) != null) {
                        target = p;
                        break;
                    }
                }
            }
            if (target == null) {
                System.out.println("[robot] no target for " + which);
                return;
            }

            double cellSize = fieldDouble(view, "cellSize");
            double offsetX = fieldDouble(view, "offsetX");
            double offsetY = fieldDouble(view, "offsetY");
            Point2D local = new Point2D(
                    offsetX + (target.getCol() + 0.5) * cellSize,
                    offsetY + (target.getRow() + 0.5) * cellSize);
            Point2D screen = view.localToScreen(local);
            if (screen == null) {
                System.out.println("[robot] localToScreen null");
                return;
            }
            fireClick(screen);
            System.out.println("[robot] click tile " + which + " " + target + " at (" + (int) screen.getX() + "," + (int) screen.getY() + ")");
        } catch (Exception e) {
            System.out.println("[robot] clickTile failed: " + e);
        }
    }

    private double fieldDouble(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (Double) f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fireClick(Point2D screen) {
        robot = new Robot();
        robot.mouseMove((int) screen.getX(), (int) screen.getY());
        new Timeline(new KeyFrame(Duration.millis(180), e ->
                robot.mouseClick(MouseButton.PRIMARY))).play();
    }
}
