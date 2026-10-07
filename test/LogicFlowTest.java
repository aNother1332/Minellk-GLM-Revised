import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.Cell;
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import ui.GameScreen;
import ui.ScreenManager;
import utils.Utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 开发用诊断：不占用真实鼠标（向节点直接派发 MouseEvent），验证游戏逻辑链路：
 * 开始游戏 → 配对消除 → 道具消除 → 无效点击保持选中 → 收回道具。可随时运行，不影响正在进行的游戏。
 */
public class LogicFlowTest extends Application {

    private GameScreen game;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setY(0);
        stage.setX(0);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = (GameScreen) f.get(null);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.6), e -> {
                    startButton().fire();
                    check("开局", "gameStarted", true);
                }),
                new KeyFrame(Duration.seconds(0.8), e -> clickPair()),
                new KeyFrame(Duration.seconds(1.6), e -> {
                    check("配对消除", "tiles", 30);
                    check("配对消除", "score", 10);
                }),
                new KeyFrame(Duration.seconds(1.8), e -> toolButton(1).fire()),
                new KeyFrame(Duration.seconds(1.9), e -> check("选中道具", "chosenTool", 1)),
                new KeyFrame(Duration.seconds(2.0), e -> clickOreTile()),
                new KeyFrame(Duration.seconds(2.8), e -> {
                    check("道具消除", "tiles", 29);
                    check("道具消除", "score", 20);
                    check("道具消除", "points", 1);
                    check("道具消除", "chosenTool", 0);
                }),
                new KeyFrame(Duration.seconds(3.0), e -> toolButton(1).fire()),
                new KeyFrame(Duration.seconds(3.1), e -> clickNonOreTile()),
                new KeyFrame(Duration.seconds(3.3), e -> {
                    check("无效点击不消除", "tiles", 29);
                    check("无效点击不扣除点数", "points", 1);
                    check("无效点击道具保持选中", "chosenTool", 1);
                    check("无效点击有横幅提示", "toastText", "无效");
                }),
                new KeyFrame(Duration.seconds(3.5), e -> toolButton(1).fire()),
                new KeyFrame(Duration.seconds(3.6), e -> {
                    check("收回道具", "chosenTool", 0);
                }),
                new KeyFrame(Duration.seconds(3.8), e -> clickPair()),
                new KeyFrame(Duration.seconds(4.6), e -> {
                    check("再次配对消除", "tiles", 27);
                    check("再次配对消除", "score", 30);
                }),
                new KeyFrame(Duration.seconds(4.8), e -> Platform.exit()));
        tl.play();
    }

    // ———— 操作 ————

    private void clickPair() {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        for (Position a : cells) {
            for (Position b : cells) {
                if (!a.equals(b) && Utils.findPath(board, a, b) != null) {
                    fireTileClick(tileOf(a));
                    fireTileClick(tileOf(b));
                    System.out.println("[test] pair clicked: " + a + " -> " + b);
                    return;
                }
            }
        }
        System.out.println("[test] no pair found!");
    }

    private void clickOreTile() {
        clickTileWithCategory(n -> n == 1 || n == 4 || n == 7 || n == 10);
    }

    private void clickNonOreTile() {
        clickTileWithCategory(n -> !(n == 1 || n == 4 || n == 7 || n == 10));
    }

    private void clickTileWithCategory(java.util.function.IntPredicate matcher) {
        for (Position p : board().getcellpositions()) {
            if (matcher.test(board().getCell(p.getRow(), p.getCol()).getNumber())) {
                fireTileClick(tileOf(p));
                System.out.println("[test] tile clicked: " + p);
                return;
            }
        }
        System.out.println("[test] no matching tile!");
    }

    /** 直接向棋子节点派发点击事件（走节点的事件处理管线，不依赖真实鼠标） */
    private void fireTileClick(javafx.scene.Node tile) {
        Point2D center = new Point2D(tile.getLayoutBounds().getWidth() / 2,
                tile.getLayoutBounds().getHeight() / 2);
        PickResult pick = new PickResult(tile, center.getX(), center.getY());
        MouseEvent click = new MouseEvent(MouseEvent.MOUSE_CLICKED,
                center.getX(), center.getY(), 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false,
                true, false, false, false, false, pick);
        javafx.event.Event.fireEvent(tile, click);
    }

    private Button startButton() {
        return findButton("开始游戏");
    }

    private Button toolButton(int index) {
        try {
            Field f = GameScreen.class.getDeclaredField("toolButtons");
            f.setAccessible(true);
            Button[] buttons = (Button[]) f.get(game);
            return buttons[index - 1];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Button findButton(String text) {
        for (javafx.scene.Node n : game.scene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        throw new IllegalStateException("button not found: " + text);
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.Node tileOf(Position position) {
        try {
            Field f = ui.BoardView.class.getDeclaredField("tileMap");
            f.setAccessible(true);
            Map<Position, javafx.scene.Node> map = (Map<Position, javafx.scene.Node>) f.get(boardView());
            return map.get(position);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ui.BoardView boardView() {
        try {
            Field f = GameScreen.class.getDeclaredField("boardView");
            f.setAccessible(true);
            return (ui.BoardView) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ———— 断言 ————

    private Object expectValue;

    private void check(String name, String key, Object expect) {
        expectValue = expect;
        Object actual = actualOf(key);
        boolean ok = actual instanceof String s && expect instanceof String sub
                ? s.contains(sub)
                : actual.equals(expect);
        System.out.println("[check] " + name + ": " + key + "=" + actual
                + (ok ? "  [OK]" : "  [FAIL] 期望 " + expect));
        if (key.equals("toastText")) {
            System.out.println("[debug] toast text = [" + ((javafx.scene.control.Label) get("toast")).getText() + "]");
        }
    }

    private Object actualOf(String key) {
        try {
            return switch (key) {
                case "gameStarted" -> get("gameStarted");
                case "chosenTool" -> get("chosenTool");
                case "points" -> ((model.GameSession) get("session")).getItemPoints();
                case "score" -> ((model.GameSession) get("session")).getScore();
                case "tiles" -> board().getcellpositions().size();
                case "toastText" -> ((javafx.scene.control.Label) get("toast")).getText() == null
                        ? "" : ((javafx.scene.control.Label) get("toast")).getText();
                default -> "?";
            };
        } catch (Exception e) {
            return "ERR:" + e.getMessage();
        }
    }

    private GameBoard board() {
        try {
            Field f = GameScreen.class.getDeclaredField("gameBoard");
            f.setAccessible(true);
            return (GameBoard) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
