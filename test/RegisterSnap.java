import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import ui.MenuCards;
import ui.ScreenManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class RegisterSnap extends Application {
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(30);
        stage.setY(30);
        stage.show();
        AudioPlayer.backgroundsound();
        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> MenuCards.showLogin(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(1.6), e -> MenuCards.showRegister(stage.getScene().getWindow())),
                new KeyFrame(Duration.seconds(2.4), e -> {
                    try {
                        var img = stage.getScene().snapshot(null);
                        var buf = new java.awt.image.BufferedImage((int) img.getWidth(), (int) img.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        int[] px = new int[(int) (img.getWidth() * img.getHeight())];
                        img.getPixelReader().getPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), px, 0, (int) img.getWidth());
                        buf.setRGB(0, 0, (int) img.getWidth(), (int) img.getHeight(), px, 0, (int) img.getWidth());
                        ImageIO.write(buf, "png", new File("screenshots/register_card.png"));
                        System.out.println("[snap] saved");
                    } catch (Exception ex) {
                        System.out.println("[snap] failed " + ex);
                    }
                }),
                new KeyFrame(Duration.seconds(2.8), e -> Platform.exit()));
        tl.play();
    }
}
