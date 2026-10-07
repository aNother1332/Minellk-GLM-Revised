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
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import ui.GameScreen;
import ui.ScreenManager;
import utils.Utils;

import java.lang.reflect.Field;
import java.util.List;

/** 开发用诊断：无尽模式——消除一对后自动补充两个新棋子，棋盘保持可玩。 */
public class EndlessModeTest extends Application {

    private GameScreen game;
    private Position pairA;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.ENDLESS);
        stage.setX(60);
        stage.setY(60);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = (GameScreen) f.get(null);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.8), e -> {
                    findButton("开始游戏").fire();
                }),
                new KeyFrame(Duration.seconds(1.4), e -> check("开局棋子数", "tiles", 60)),
                new KeyFrame(Duration.seconds(1.6), e -> clickPair()),
                new KeyFrame(Duration.seconds(3.4), e -> {
                    check("消除并补充后棋子数", "tiles", 60);
                    check("消除计数", "eliminated", 1);
                    check("补充后无自动选中", "noFrameVisible", true);
                }),
                new KeyFrame(Duration.seconds(3.6), e -> clickPair()),
                new KeyFrame(Duration.seconds(5.4), e -> {
                    check("再次消除补充后", "tiles", 60);
                    check("再次消除计数", "eliminated", 2);
                }),
                new KeyFrame(Duration.seconds(5.6), e -> Platform.exit()));
        tl.play();
    }

    private Button findButton(String text) {
        Scene scene = game.scene();
        for (Node n : scene.getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                return b;
            }
        }
        return null;
    }

    private void clickPair() {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        for (Position a : cells) {
            for (Position b : cells) {
                if (!a.equals(b) && Utils.findPath(board, a, b) != null) {
                    pairA = a;
                    fireTileClick(tileOf(a));
                    fireTileClick(tileOf(b));
                    System.out.println("[test] pair: " + a + " -> " + b);
                    return;
                }
            }
        }
        System.out.println("[test] no pair found!");
    }

    private void fireTileClick(javafx.scene.Node tile) {
        javafx.geometry.Point2D center = new javafx.geometry.Point2D(
                tile.getLayoutBounds().getWidth() / 2, tile.getLayoutBounds().getHeight() / 2);
        javafx.scene.input.PickResult pick = new javafx.scene.input.PickResult(tile, center.getX(), center.getY());
        javafx.scene.input.MouseEvent click = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED, center.getX(), center.getY(), 0, 0,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, false,
                true, false, false, false, false, pick);
        javafx.event.Event.fireEvent(tile, click);
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.Node tileOf(Position position) {
        try {
            Field f = ui.BoardView.class.getDeclaredField("tileMap");
            f.setAccessible(true);
            java.util.Map<Position, javafx.scene.Node> map = (java.util.Map<Position, javafx.scene.Node>) f.get(boardView());
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

    private GameBoard board() {
        try {
            Field f = GameScreen.class.getDeclaredField("gameBoard");
            f.setAccessible(true);
            return (GameBoard) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                case "tiles" -> board().getcellpositions().size();
                case "eliminated" -> {
                    Field f = GameScreen.class.getDeclaredField("eliminated");
                    f.setAccessible(true);
                    yield f.get(game);
                }
                case "noFrameVisible" -> {
                    boolean anyVisible = false;
                    Field f = ui.BoardView.class.getDeclaredField("tiles");
                    f.setAccessible(true);
                    for (Object t : (java.util.List<?>) f.get(boardView())) {
                        Field ff = t.getClass().getDeclaredField("frame");
                        ff.setAccessible(true);
                        javafx.scene.shape.Rectangle frame = (javafx.scene.shape.Rectangle) ff.get(t);
                        if (frame.isVisible()) {
                            anyVisible = true;
                        }
                    }
                    yield !anyVisible;
                }
                default -> "?";
            };
        } catch (Exception e) {
            return "ERR:" + e.getMessage();
        }
    }
}
