import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.image.WritablePixelFormat;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameDifficulty;
import ui.ScreenManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/** 开发用工具：逐屏截图（screenshots/ui_*.png），用于审视整体 UI 设计。 */
public class UiSnapshotTest extends Application {

    private Stage stage;
    private ui.PixelDialog.Card menuCard1, menuCard2, menuCard3, menuCard4;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        new File("screenshots").mkdirs();
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(30);
        stage.setY(30);
        stage.show();
        AudioPlayer.backgroundsound();

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> snap("ui_1_menu")),
                new KeyFrame(Duration.seconds(1.2), e -> ui.MenuCards.showLogin(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(2.0), e -> snapLogin("ui_2_login")),
                new KeyFrame(Duration.seconds(2.2), e -> closeLogin()),
                new KeyFrame(Duration.seconds(2.4), e -> menuCard1 = ui.MenuCards.showDifficulty(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(3.2), e -> snap("ui_3_difficulty")),
                new KeyFrame(Duration.seconds(3.4), e -> { if (menuCard1 != null) menuCard1.close(); }),
                new KeyFrame(Duration.seconds(3.6), e -> menuCard2 = ui.MenuCards.showSkin(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(4.4), e -> snap("ui_4_skin")),
                new KeyFrame(Duration.seconds(4.6), e -> { if (menuCard2 != null) menuCard2.close(); }),
                new KeyFrame(Duration.seconds(4.8), e -> menuCard3 = ui.MenuCards.showLoadSave(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(5.6), e -> snap("ui_5_loadsave")),
                new KeyFrame(Duration.seconds(5.8), e -> { if (menuCard3 != null) menuCard3.close(); }),
                new KeyFrame(Duration.seconds(6.0), e -> menuCard4 = ui.MenuCards.showLeaderboard(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(6.8), e -> snap("ui_6_leaderboard")),
                new KeyFrame(Duration.seconds(7.0), e -> { if (menuCard4 != null) menuCard4.close(); }),
                new KeyFrame(Duration.seconds(7.2), e -> ScreenManager.showGame(GameDifficulty.EASY)),
                new KeyFrame(Duration.seconds(8.0), e -> previewGameStyles()),
                new KeyFrame(Duration.seconds(8.2), e -> snap("ui_7_game")),
                new KeyFrame(Duration.seconds(8.4), e -> Platform.runLater(() -> {
                    Scene gameScene = ((ui.Screen) current()).scene();
                    ui.PixelDialog.choice(gameScene.getWindow(), "恭喜通关！最终得分：120",
                            420, new String[]{"重新开始", "返回主界面"}, idx -> { });
                })),
                new KeyFrame(Duration.seconds(8.8), e -> {
                    Scene gameScene = ((ui.Screen) current()).scene();
                    if (gameScene != null && gameScene.getRoot() instanceof Pane p && p.getChildren().size() > 1) {
                        Node overlay = p.getChildren().get(p.getChildren().size() - 1);
                        snap("ui_8_dialog", overlay.getScene());
                    }
                }),
                new KeyFrame(Duration.seconds(9.0), e -> Platform.exit()));
        tl.play();

    }

    /** 预览游戏内样式：选中道具（跟随图标）+ 弹出横幅 */
    private void previewGameStyles() {
        try {
            Object game = current();
            java.lang.reflect.Method arm = game.getClass().getDeclaredMethod("armTool", int.class);
            arm.setAccessible(true);
            arm.invoke(game, 1);
            java.lang.reflect.Method toast = game.getClass().getDeclaredMethod("showToast", String.class);
            toast.setAccessible(true);
            toast.invoke(game, "消除了[橡木原木]×2，+10分（连消×3，额外加分）");
        } catch (Exception e) {
            System.out.println("[preview] " + e);
        }
    }

    private Object current() {
        try {
            Field f = ScreenManager.class.getDeclaredField("current");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void closeLogin() {
        ui.PixelDialog.closeTop(stage.getScene().getWindow());
    }

    private void snap(String name) {
        snap(name, stage.getScene());
    }

    private void snapLogin(String name) {
        snap(name, stage.getScene()); // 登录卡片在主菜单场景内
    }

    private void snap(String name, Scene scene) {
        try {
            if (scene == null) {
                System.out.println("[snap] " + name + ": no scene");
                return;
            }
            WritableImage img = scene.snapshot(null);
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[w * h];
            img.getPixelReader().getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), pixels, 0, w);
            buf.setRGB(0, 0, w, h, pixels, 0, w);
            ImageIO.write(buf, "png", new File("screenshots", name + ".png"));
            System.out.println("[snap] saved " + name);
        } catch (Exception e) {
            System.out.println("[snap] " + name + " failed: " + e);
        }
    }
}
