import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import ui.GameScreen;
import ui.ScreenManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.MouseInfo;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 开发用诊断：用 JavaFX Robot（真实输入管线）验证道具新交互：
 * 道具图标实时跟随鼠标 → 点击对应方块（道具旋转特效 + 方块破坏 + 横幅提示）
 * → 点错类别时提示无效且道具保持选中 → 点数在实际消除时扣除。
 */
public class ToolClickTest extends Application {

    private Robot robot;
    private GameScreen game;

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

        ScreenManager.stage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node hit = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            System.out.println("[event] PRESSED hit=" + (hit == null ? "null" : hit.getClass().getSimpleName()));
        });

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.2), e -> clickButton("开始游戏")),
                new KeyFrame(Duration.seconds(2.0), e -> check("开局", mapOf("gameStarted", true))),
                new KeyFrame(Duration.seconds(2.2), e -> clickToolButton(1)),
                new KeyFrame(Duration.seconds(3.0), e -> {
                    check("选中道具", mapOf(
                            "chosenTool", 1,
                            "toolCursorVisible", true,
                            "toastVisible", true,
                            "points", 2));
                    snap("tool_follow");
                }),
                new KeyFrame(Duration.seconds(3.2), e -> moveMouse(500, 500)),
                new KeyFrame(Duration.seconds(3.6), e -> checkFollows(500, 500)),
                new KeyFrame(Duration.seconds(3.8), e -> moveMouse(700, 300)),
                new KeyFrame(Duration.seconds(4.2), e -> {
                    checkFollows(700, 300);
                    snap("tool_follow2");
                }),
                new KeyFrame(Duration.seconds(4.4), e -> clickTile(true)),
                new KeyFrame(Duration.seconds(5.8), e -> check("道具消除", mapOf(
                        "tiles", 31,
                        "score", 10,
                        "points", 1,
                        "chosenTool", 0,
                        "toolCursorVisible", false))),
                new KeyFrame(Duration.seconds(6.0), e -> clickToolButton(1)),
                new KeyFrame(Duration.seconds(6.8), e -> clickTile(false)),
                new KeyFrame(Duration.seconds(8.0), e -> check("无效点击", mapOf(
                        "tiles", 31,
                        "points", 1,
                        "chosenTool", 1))),
                new KeyFrame(Duration.seconds(8.4), e -> Platform.exit()));
        tl.play();
    }

    /** 断言道具图标跟随鼠标：图标位置 = 鼠标场景坐标 + (12,10) 偏移 */
    private void checkFollows(int robotX, int robotY) {
        javafx.scene.image.ImageView cursor = toolCursor();
        java.awt.Point os = MouseInfo.getPointerInfo().getLocation();
        double scale = Screen.getPrimary().getOutputScaleX();
        Point2D expectedScene = game.scene().getRoot().screenToLocal(os.x / scale, os.y / scale);
        double dx = cursor.getLayoutX() - (expectedScene.getX() + 12);
        double dy = cursor.getLayoutY() - (expectedScene.getY() + 10);
        boolean ok = Math.abs(dx) <= 2 && Math.abs(dy) <= 2 && cursor.isVisible();
        System.out.println("[check] 跟随鼠标(" + robotX + "," + robotY + "): icon=("
                + (int) cursor.getLayoutX() + "," + (int) cursor.getLayoutY()
                + ") 期望≈(" + (int) (expectedScene.getX() + 12) + "," + (int) (expectedScene.getY() + 10)
                + ") 偏差=(" + (int) dx + "," + (int) dy + ") " + (ok ? "✓" : "✗"));
    }

    private void moveMouse(int x, int y) {
        robot = new Robot();
        robot.mouseMove(x, y);
        System.out.println("[robot] move to (" + x + "," + y + ")");
    }

    // ———— 断言 ————

    private Object expectValue;

    private void check(String name, java.util.Map<String, Object> expect) {
        StringBuilder sb = new StringBuilder("[check] " + name + ":");
        boolean allOk = true;
        for (var entry : expect.entrySet()) {
            expectValue = entry.getValue();
            Object actual = actualOf(entry.getKey());
            boolean ok = actual.equals(entry.getValue());
            allOk &= ok;
            sb.append(' ').append(entry.getKey()).append('=').append(actual).append(ok ? "" : "(期望" + entry.getValue() + ")");
        }
        System.out.println(sb + (allOk ? "  [OK]" : "  [FAIL]"));
    }

    private Object actualOf(String key) {
        return switch (key) {
            case "gameStarted" -> get("gameStarted");
            case "chosenTool" -> get("chosenTool");
            case "points" -> get("itemPoints");
            case "score" -> get("score");
            case "tiles" -> board().getcellpositions().size();
            case "toolCursorVisible" -> toolCursor().isVisible();
            case "toastVisible" -> toast().isVisible();
            case "toastContains" -> String.valueOf(toast().getText()).contains((String) expectValue);
            default -> "?";
        };
    }

    private java.util.Map<String, Object> mapOf(Object... kv) {
        var map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    // ———— 反射 ————

    private Object get(String field) {
        try {
            Field f = GameScreen.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GameBoard board() {
        return (GameBoard) get("gameBoard");
    }

    private javafx.scene.control.Label toast() {
        return (javafx.scene.control.Label) get("toast");
    }

    private javafx.scene.image.ImageView toolCursor() {
        return (javafx.scene.image.ImageView) get("toolCursor");
    }

    @SuppressWarnings("unchecked")
    private List<Button> toolButtons() {
        return (List<Button>) (List<?>) java.util.Arrays.asList((Object[]) get("toolButtons"));
    }

    // ———— 真实点击 ————

    private void fireClick(Point2D screen) {
        robot = new Robot();
        robot.mouseMove((int) screen.getX(), (int) screen.getY());
        new Timeline(new KeyFrame(Duration.millis(180), e ->
                robot.mouseClick(MouseButton.PRIMARY))).play();
    }

    private void clickButton(String text) {
        for (Node n : ScreenManager.stage().getScene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                Point2D p = b.localToScreen(b.getBoundsInLocal().getWidth() / 2,
                        b.getBoundsInLocal().getHeight() / 2);
                System.out.println("[robot] click button [" + text + "]");
                fireClick(p);
                return;
            }
        }
        System.out.println("[robot] button not found: " + text);
    }

    private void clickToolButton(int index) {
        Button b = toolButtons().get(index - 1);
        Point2D p = b.localToScreen(b.getBoundsInLocal().getWidth() / 2,
                b.getBoundsInLocal().getHeight() / 2);
        System.out.println("[robot] click tool button " + index);
        fireClick(p);
    }

    /** matching=true 找一个镐可消除的方块（矿石类），false 找一个不可消除的方块 */
    private void clickTile(boolean matching) {
        for (Position p : board().getcellpositions()) {
            int n = board().getCell(p.getRow(), p.getCol()).getNumber();
            boolean ore = n == 1 || n == 4 || n == 7 || n == 10;
            if (ore == matching) {
                javafx.scene.layout.Pane view = (javafx.scene.layout.Pane) get("boardView");
                Point2D local = new Point2D(d(view, "offsetX") + (p.getCol() + 0.5) * d(view, "cellSize"),
                        d(view, "offsetY") + (p.getRow() + 0.5) * d(view, "cellSize"));
                Point2D screen = view.localToScreen(local);
                System.out.println("[robot] click tile " + p + " number=" + n);
                fireClick(screen);
                return;
            }
        }
        System.out.println("[robot] no suitable tile");
    }

    private double d(Object o, String field) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return (Double) f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ———— 截图 ————

    private void snap(String name) {
        try {
            Scene scene = game.scene();
            WritableImage img = scene.snapshot(null);
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[w * h];
            img.getPixelReader().getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), pixels, 0, w);
            buf.setRGB(0, 0, w, h, pixels, 0, w);
            ImageIO.write(buf, "png", new File("screenshots/" + name + ".png"));
        } catch (Exception e) {
            System.out.println("[snap] failed: " + e);
        }
    }
}
