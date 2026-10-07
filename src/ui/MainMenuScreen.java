package ui;

import app.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 主菜单界面，等价于原 Swing 版 MainMenuFrame。
 */
public class MainMenuScreen implements Screen {

    private static MainMenuScreen instance;

    private final StackPane root = new StackPane();
    private final Scene scene = new Scene(root, 1000, 1000);
    private final Label userLabel = new Label();
    private final AppButton loadButton = new AppButton("读取存档", 300, 56, 18, AppButton.Variant.WHITE);

    private MainMenuScreen() {
        ImageView bg = UIAssets.backgroundView("resource/images/login page.jpg");
        bg.fitWidthProperty().bind(scene.widthProperty());
        bg.fitHeightProperty().bind(scene.heightProperty());

        VBox center = new VBox(18);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(280, 100, 30, 100));
        center.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        AppButton startButton = new AppButton("开始新游戏", 300, 56, 18, AppButton.Variant.PRIMARY);
        AppButton leaderboardButton = new AppButton("排行榜", 300, 56, 18, AppButton.Variant.WHITE);
        AppButton skinButton = new AppButton("更换主题", 300, 56, 18, AppButton.Variant.WHITE);

        startButton.setOnAction(e -> MenuCards.showDifficulty(scene.getWindow()));
        loadButton.setOnAction(e -> MenuCards.showLoadSave(scene.getWindow()));
        leaderboardButton.setOnAction(e -> MenuCards.showLeaderboard(scene.getWindow()));
        skinButton.setOnAction(e -> MenuCards.showSkin(scene.getWindow()));

        center.getChildren().addAll(startButton, loadButton, leaderboardButton, skinButton);

        HBox bottom = new HBox(12);
        bottom.setAlignment(Pos.BOTTOM_RIGHT);
        bottom.setPadding(new Insets(8, 12, 8, 12));
        // 关键：禁止 StackPane 把该工具条拉伸到全场景——
        // 否则这个透明的 HBox 会覆盖中央按钮并吞掉全部鼠标事件
        bottom.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // 毛玻璃胶囊：浅色半透明底 + 圆角，保证在明亮的背景图上可读
        bottom.setStyle("-fx-background-color: rgba(250,250,252,0.82);"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,20,40,0.22), 14, 0.1, 0, 4);");
        userLabel.setFont(UiTheme.fontSemi(14));
        userLabel.setTextFill(UiTheme.TEXT_MAIN);
        AppButton loginButton = new AppButton("登录", 84, 34, 14, AppButton.Variant.PRIMARY);
        loginButton.setOnAction(e -> MenuCards.showLogin(scene.getWindow()));
        bottom.getChildren().addAll(userLabel, loginButton);

        root.getChildren().addAll(bg, center, bottom);
        StackPane.setAlignment(center, Pos.CENTER);
        StackPane.setAlignment(bottom, Pos.BOTTOM_RIGHT);

        AppButton settingsButton = new AppButton("设置", 76, 30, 12, AppButton.Variant.WHITE);
        settingsButton.setOnAction(e -> MenuCards.showSettings(scene.getWindow()));

        Label version = UiTheme.label("Minellk v2.0 · JavaFX", 11, UiTheme.TEXT_MAIN);
        version.setOpacity(0.75);

        javafx.scene.layout.HBox corner = new javafx.scene.layout.HBox(10);
        corner.setAlignment(Pos.BOTTOM_LEFT);
        corner.getChildren().addAll(settingsButton, version);
        // 关键：禁止 StackPane 把它拉伸铺满场景，否则透明层会挡住全部按钮的点击
        corner.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.getChildren().add(corner);
        StackPane.setAlignment(corner, Pos.BOTTOM_LEFT);
        StackPane.setMargin(corner, new Insets(0, 0, 10, 12));
        refresh();
    }

    public static MainMenuScreen instance() {
        if (instance == null) {
            instance = new MainMenuScreen();
        }
        return instance;
    }

    public void refresh() {
        userLabel.setText("当前用户：" + UserSession.username);
        loadButton.setDisable(!UserSession.loggedIn);
    }

    @Override
    public Scene scene() {
        refresh();
        return scene;
    }

    @Override
    public String title() {
        return "Minellk-连连看";
    }

    @Override
    public double width() {
        return 1000;
    }

    @Override
    public double height() {
        return 1000;
    }

    @Override
    public boolean resizable() {
        return true;
    }
}
