import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.stage.Screen;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 开发用冒烟测试：自动走一遍「开局 → 选中 → 配对消除（连线/粒子/加分）→ 道具消除」，
 * 并用 scene.snapshot 在各特效阶段抓拍 PNG 到 screenshots/ 目录。
 * 仅用于开发期验证，不参与游戏本体编译。
 */
public class FxSmokeTest extends Application {

    private static final String OUT_DIR = "screenshots";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        new File(OUT_DIR).mkdirs();
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(20);
        stage.setY(20);
        stage.show();
        AudioPlayer.backgroundsound();

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> ScreenManager.showGame(GameDifficulty.EASY)),
                new KeyFrame(Duration.seconds(2.2), e -> snap("1_game_initial")),
                new KeyFrame(Duration.seconds(2.4), e -> startGame()),
                new KeyFrame(Duration.seconds(2.9), e -> diag()),
                new KeyFrame(Duration.seconds(3.0), e -> selectFirst()),
                new KeyFrame(Duration.seconds(3.5), e -> snap("2_selected")),
                new KeyFrame(Duration.seconds(4.0), e -> selectSecond_match()),
                new KeyFrame(Duration.seconds(4.18), e -> snap("3_line")),
                new KeyFrame(Duration.seconds(4.45), e -> snap("4_particles")),
                new KeyFrame(Duration.seconds(6.0), e -> snap("5_after_eliminate")),
                new KeyFrame(Duration.seconds(6.2), e -> useTool()),
                new KeyFrame(Duration.seconds(6.5), e -> snap("6_tool_particles")),
                new KeyFrame(Duration.seconds(7.8), e -> snap("7_after_tool")),
                new KeyFrame(Duration.seconds(8.0), e -> javafx.application.Platform.exit()));
        tl.play();
    }

    // ———— 反射工具 ————

    private static Object gameScreen() {
        try {
            Field f = ScreenManager.class.getDeclaredField("current");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object call(String method, Class<?>[] types, Object... args) {
        try {
            Method m = gameScreen().getClass().getDeclaredMethod(method, types);
            m.setAccessible(true);
            return m.invoke(gameScreen(), args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static GameBoard board() {
        try {
            Field f = gameScreen().getClass().getDeclaredField("gameBoard");
            f.setAccessible(true);
            return (GameBoard) f.get(gameScreen());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startGame() {
        call("startGame", new Class<?>[0]);
    }

    /** 打印棋盘布局诊断信息 */
    @SuppressWarnings("unchecked")
    private void diag() {
        try {
            Object gs = gameScreen();
            Field bf = gs.getClass().getDeclaredField("boardView");
            bf.setAccessible(true);
            BoardView bv = (BoardView) bf.get(gs);
            System.out.println("[diag] BoardView w=" + bv.getWidth() + " h=" + bv.getHeight());
            for (String f : new String[]{"rows", "cols", "cellSize", "offsetX", "offsetY"}) {
                Field ff = BoardView.class.getDeclaredField(f);
                ff.setAccessible(true);
                System.out.println("[diag] " + f + " = " + ff.get(bv));
            }
            Field tf = BoardView.class.getDeclaredField("tiles");
            tf.setAccessible(true);
            List<Object> tiles = (List<Object>) tf.get(bv);
            System.out.println("[diag] tile count = " + tiles.size());
            javafx.scene.Node first = (javafx.scene.Node) tiles.get(0);
            System.out.println("[diag] tile0 bounds=" + first.getLayoutBounds() + " scene=" + first.localToScene(first.getLayoutBounds()));
            javafx.scene.layout.StackPane sp = (javafx.scene.layout.StackPane) first;
            System.out.println("[diag] tile0 prefW=" + sp.prefWidth(-1) + " prefH=" + sp.prefHeight(-1)
                    + " maxW=" + sp.maxWidth(-1) + " children=" + sp.getChildren().size());
            for (javafx.scene.Node child : sp.getChildren()) {
                System.out.println("[diag]   child " + child.getClass().getSimpleName()
                        + " bounds=" + child.getLayoutBounds());
                if (child instanceof javafx.scene.image.ImageView iv) {
                    System.out.println("[diag]     image=" + (iv.getImage() == null ? "null"
                            : iv.getImage().getWidth() + "x" + iv.getImage().getHeight())
                            + " fitW=" + iv.getFitWidth() + " fitH=" + iv.getFitHeight());
                }
            }
            Field cf = BoardView.class.getDeclaredField("tileMap");
            cf.setAccessible(true);
            System.out.println("[diag] tileMap=" + ((java.util.Map<Object, Object>) cf.get(bv)).size());
        } catch (Exception e) {
            System.out.println("[diag] failed: " + e);
        }
    }

    /** 找到一对可消除的棋子，点击第一块 */
    private Position pairA;

    private void selectFirst() {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        for (Position a : cells) {
            for (Position b : cells) {
                if (!a.equals(b) && Utils.findPath(board, a, b) != null) {
                    pairA = a;
                    call("handleTileClick", new Class<?>[]{int.class, int.class}, a.getRow(), a.getCol());
                    return;
                }
            }
        }
        throw new IllegalStateException("棋盘上找不到可消除对");
    }

    /** 点击配对的第二块，触发消除动画 */
    private void selectSecond_match() {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        for (Position b : cells) {
            if (!b.equals(pairA) && Utils.findPath(board, pairA, b) != null) {
                call("handleTileClick", new Class<?>[]{int.class, int.class}, b.getRow(), b.getCol());
                return;
            }
        }
        throw new IllegalStateException("第二块丢失");
    }

    /** 选择钻石镐并消除一块矿石类方块 */
    private void useTool() {
        call("armTool", new Class<?>[]{int.class}, 1);
        GameBoard board = board();
        for (Position p : board.getcellpositions()) {
            int n = board.getCell(p.getRow(), p.getCol()).getNumber();
            if (n == 1 || n == 4 || n == 7 || n == 10) {
                call("handleTileClick", new Class<?>[]{int.class, int.class}, p.getRow(), p.getCol());
                return;
            }
        }
        System.out.println("[smoke] 没有找到矿石类方块，跳过道具测试");
    }

    // ———— 抓拍 ————

    private void snap(String name) {
        try {
            Scene scene = ((GameScreen) gameScreen()).scene();
            WritableImage img = scene.snapshot(null);
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[w * h];
            img.getPixelReader().getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), pixels, 0, w);
            buf.setRGB(0, 0, w, h, pixels, 0, w);
            ImageIO.write(buf, "png", new File(OUT_DIR, name + ".png"));
            System.out.println("[smoke] saved " + name);
        } catch (Exception e) {
            System.out.println("[smoke] snap failed: " + e);
        }
    }
}
