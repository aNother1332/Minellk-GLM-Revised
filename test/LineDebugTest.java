import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
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
 * 开发用诊断：连续三次消除，在每次连线展示阶段截图并打印
 * BoardView 内部状态（lineAlpha / linePath / particles），定位连线特效逐渐消失的问题。
 */
public class LineDebugTest extends Application {

    private GameScreen game;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setX(20);
        stage.setY(20);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = (GameScreen) f.get(null);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.8), e -> invoke("startGame")),
                new KeyFrame(Duration.seconds(1.0), e -> clickPair(1)),
                new KeyFrame(Duration.seconds(1.15), e -> snap(1)),
                new KeyFrame(Duration.seconds(2.0), e -> clickPair(2)),
                new KeyFrame(Duration.seconds(2.15), e -> snap(2)),
                new KeyFrame(Duration.seconds(3.0), e -> clickPair(3)),
                new KeyFrame(Duration.seconds(3.15), e -> snap(3)),
                new KeyFrame(Duration.seconds(3.4), e -> snap(31)),
                new KeyFrame(Duration.seconds(3.7), e -> Platform.exit()));
        tl.play();
    }

    /** 找一对可消除的棋子，两次调用 handleTileClick 触发消除 */
    private Position pairA;

    private void clickPair(int round) {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        for (Position a : cells) {
            for (Position b : cells) {
                if (!a.equals(b) && Utils.findPath(board, a, b) != null) {
                    pairA = a;
                    invokeClick(a);
                    invokeClick(b);
                    System.out.println("[test] round " + round + " clicked " + a + " -> " + b);
                    return;
                }
            }
        }
        System.out.println("[test] round " + round + ": no pair found");
    }

    private void invokeClick(Position p) {
        invoke("handleTileClick", new Class<?>[]{int.class, int.class}, p.getRow(), p.getCol());
    }

    private void snap(int round) {
        try {
            BoardView view = boardView();
            Field alphaF = BoardView.class.getDeclaredField("lineAlpha");
            alphaF.setAccessible(true);
            javafx.beans.property.DoubleProperty alpha = (javafx.beans.property.DoubleProperty) alphaF.get(view);
            Field pathF = BoardView.class.getDeclaredField("linePath");
            pathF.setAccessible(true);
            List<?> path = (List<?>) pathF.get(view);
            Field pF = BoardView.class.getDeclaredField("particles");
            pF.setAccessible(true);
            List<?> particles = (List<?>) pF.get(view);
            System.out.println("[snap" + round + "] lineAlpha=" + String.format("%.2f", alpha.get())
                    + " linePath=" + (path == null ? "null" : path.size() + "点")
                    + " particles=" + particles.size());

            javafx.scene.Scene scene = game.scene();
            javafx.scene.image.WritableImage img = scene.snapshot(null);
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[w * h];
            img.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), pixels, 0, w);
            buf.setRGB(0, 0, w, h, pixels, 0, w);
            javax.imageio.ImageIO.write(buf, "png", new java.io.File("screenshots/line_" + round + ".png"));
        } catch (Exception e) {
            System.out.println("[snap" + round + "] failed: " + e);
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

    private BoardView boardView() {
        try {
            Field f = GameScreen.class.getDeclaredField("boardView");
            f.setAccessible(true);
            return (BoardView) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invoke(String method, Class<?>[] types, Object... args) {
        try {
            java.lang.reflect.Method m = game.getClass().getDeclaredMethod(method, types);
            m.setAccessible(true);
            m.invoke(game, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invoke(String method) {
        invoke(method, new Class<?>[0]);
    }
}
