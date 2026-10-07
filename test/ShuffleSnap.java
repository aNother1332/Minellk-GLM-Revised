import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameDifficulty;
import ui.ScreenManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;

/** 开发用工具：截取洗牌动画三个阶段的场景快照。 */
public class ShuffleSnap extends Application {

    private GameScreenLike game;
    private Stage stage;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.EASY);
        stage.setX(30);
        stage.setY(30);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = new GameScreenLike(f.get(null));

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.8), e -> start()),
                new KeyFrame(Duration.seconds(1.0), e -> shuffle()),
                new KeyFrame(Duration.seconds(1.35), e -> snap("shuffle_1_gather")),
                new KeyFrame(Duration.seconds(1.65), e -> snap("shuffle_2_spin")),
                new KeyFrame(Duration.seconds(2.05), e -> snap("shuffle_3_scatter")),
                new KeyFrame(Duration.seconds(2.6), e -> checkUnlocked()),
                new KeyFrame(Duration.seconds(2.8), e -> Platform.exit()));
        tl.play();
    }

    private void start() {
        invoke("startGame");
    }

    private void shuffle() {
        invoke("useShuffleItem");
    }

    private void checkUnlocked() {
        try {
            Field f = ui.BoardView.class.getDeclaredField("locked");
            f.setAccessible(true);
            System.out.println("[check] 动画结束后棋盘解锁: locked=" + f.get(boardView()));
        } catch (Exception e) {
            System.out.println("[check] " + e);
        }
    }

    private Object boardView() {
        try {
            Field f = game.obj.getClass().getDeclaredField("boardView");
            f.setAccessible(true);
            return f.get(game.obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invoke(String name) {
        try {
            java.lang.reflect.Method m = game.obj.getClass().getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(game.obj);
        } catch (Exception e) {
            System.out.println("[invoke] " + e);
        }
    }

    private static class GameScreenLike {
        final Object obj;
        GameScreenLike(Object o) { obj = o; }
    }

    private void snap(String name) {
        try {
            var img = stage.getScene().snapshot(null);
            var buf = new BufferedImage((int) img.getWidth(), (int) img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int[] px = new int[(int) (img.getWidth() * img.getHeight())];
            img.getPixelReader().getPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), px, 0, (int) img.getWidth());
            buf.setRGB(0, 0, (int) img.getWidth(), (int) img.getHeight(), px, 0, (int) img.getWidth());
            ImageIO.write(buf, "png", new File("screenshots", name + ".png"));
            System.out.println("[snap] saved " + name);
        } catch (Exception e) {
            System.out.println("[snap] failed: " + e);
        }
    }
}
