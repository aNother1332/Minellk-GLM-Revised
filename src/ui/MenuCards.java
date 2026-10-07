package ui;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import model.GameDifficulty;
import model.Record;
import model.RecordStore;
import app.UserSession;

import java.util.List;
import java.util.Map;

/**
 * 主菜单上的功能卡片弹窗：难度选择、主题切换、读取存档、排行榜。
 * 统一使用 PixelDialog.showCard 的遮罩 + 卡片动画与浅色玻璃 UI。
 */
public final class MenuCards {

    private MenuCards() {}

    /** 登录卡片：登录 / 去注册 / 游客模式，错误在卡片内联提示 */
    public static PixelDialog.Card showLogin(Window owner) {
        return PixelDialog.showCard(owner, 360, card -> {
            VBox panel = card.panel();
            TextField username = new TextField();
            username.setPromptText("请输入用户名");
            PasswordField password = new PasswordField();
            password.setPromptText("请输入密码");
            styleField(username);
            styleField(password);

            Label error = UiTheme.label(" ", 12, UiTheme.DANGER);
            GridPane.setColumnIndex(error, 0);
            GridPane.setColumnSpan(error, 2);

            GridPane form = new GridPane(10, 12);
            form.setAlignment(Pos.CENTER);
            form.getColumnConstraints().add(new ColumnConstraints(70));
            form.add(UiTheme.label("用户名", 13, UiTheme.TEXT_MAIN), 0, 0);
            form.add(username, 1, 0);
            form.add(UiTheme.label("密码", 13, UiTheme.TEXT_MAIN), 0, 1);
            form.add(password, 1, 1);
            form.add(error, 0, 2);

            AppButton loginButton = new AppButton("登录", 120, 40, 15, AppButton.Variant.PRIMARY);
            loginButton.setOnAction(e -> {
                if (app.UserStore.login(username.getText().trim(), password.getText())) {
                    app.UserSession.login(username.getText().trim());
                    card.closeThen(() -> MainMenuScreen.instance().refresh());
                } else {
                    error.setText("用户名或密码错误");
                }
            });
            AppButton registerButton = new AppButton("注 册", 120, 40, 15, AppButton.Variant.TINTED);
            registerButton.setOnAction(e -> {
                card.closeThen(() -> showRegister(owner));
            });
            AppButton guestButton = new AppButton("游客模式", 120, 40, 15, AppButton.Variant.TINTED);
            guestButton.setOnAction(e -> {
                app.UserSession.logout();
                card.closeThen(() -> MainMenuScreen.instance().refresh());
            });

            HBox row = new HBox(16, loginButton, registerButton);
            row.setAlignment(Pos.CENTER);
            panel.getChildren().addAll(
                    UiTheme.label("MINELLK", 18, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    form,
                    row,
                    guestButton);
            username.requestFocus();
        });
    }

    /** 注册卡片：成功后回到登录卡片 */
    public static PixelDialog.Card showRegister(Window owner) {
        return PixelDialog.showCard(owner, 360, card -> {
            VBox panel = card.panel();
            TextField username = new TextField();
            username.setPromptText("请输入用户名");
            PasswordField password = new PasswordField();
            password.setPromptText("请输入密码");
            styleField(username);
            styleField(password);

            Label error = UiTheme.label(" ", 12, UiTheme.DANGER);
            GridPane.setColumnIndex(error, 0);
            GridPane.setColumnSpan(error, 2);

            GridPane form = new GridPane(10, 12);
            form.setAlignment(Pos.CENTER);
            form.getColumnConstraints().add(new ColumnConstraints(70));
            form.add(UiTheme.label("用户名", 13, UiTheme.TEXT_MAIN), 0, 0);
            form.add(username, 1, 0);
            form.add(UiTheme.label("密码", 13, UiTheme.TEXT_MAIN), 0, 1);
            form.add(password, 1, 1);
            form.add(error, 0, 2);

            AppButton registerButton = new AppButton("注 册", 160, 42, 15, AppButton.Variant.PRIMARY);
            registerButton.setOnAction(e -> {
                String user = username.getText().trim();
                String pass = password.getText();
                if (user.isEmpty() || pass.isEmpty()) {
                    error.setText("用户名和密码不能为空");
                    return;
                }
                if (app.UserStore.register(user, pass)) {
                    card.closeThen(() -> showLogin(owner));
                } else {
                    error.setText("用户名已存在");
                }
            });
            panel.getChildren().addAll(
                    UiTheme.label("注 册", 18, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    form,
                    registerButton,
                    plainButton("返回登录", () -> card.closeThen(() -> showLogin(owner))));
            username.requestFocus();
        });
    }

    /** 登录/注册卡片用的输入框：白底圆角 + 聚焦蓝描边 */
    private static void styleField(javafx.scene.control.TextInputControl field) {
        field.setFont(UiTheme.font(13));
        field.setPrefWidth(190);
        field.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: #1d1d1f;"
                + "-fx-highlight-fill: rgba(10,132,255,0.25);"
                + "-fx-border-color: #d2d2d7;"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 8;"
                + "-fx-prompt-text-fill: #a1a1a6;");
        field.focusedProperty().addListener((o, was, now) -> field.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-background-radius: 8;"
                        + "-fx-text-fill: #1d1d1f;"
                        + "-fx-highlight-fill: rgba(10,132,255,0.25);"
                        + "-fx-border-color: " + (now ? "#0a84ff" : "#d2d2d7") + ";"
                        + "-fx-border-width: " + (now ? 2 : 1) + ";"
                        + "-fx-border-radius: 8;"
                        + "-fx-prompt-text-fill: #a1a1a6;"));
    }

    /** 难度选择 */
    public static PixelDialog.Card showDifficulty(Window owner) {
        return PixelDialog.showCard(owner, 320, card -> {
            VBox panel = card.panel();
            panel.getChildren().addAll(
                    UiTheme.label("请选择游戏难度", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    wideButton("简单模式", () -> card.closeThen(() -> ScreenManager.showGame(GameDifficulty.EASY))),
                    wideButton("困难模式", () -> card.closeThen(() -> ScreenManager.showGame(GameDifficulty.HARD))),
                    wideButton("无尽模式", () -> card.closeThen(() -> ScreenManager.showGame(GameDifficulty.ENDLESS))),
                    plainButton("返回", card::close));
        });
    }

    /** 主题切换：点击后立即生效，卡片内给出反馈 */
    public static PixelDialog.Card showSkin(Window owner) {
        return PixelDialog.showCard(owner, 320, card -> {
            VBox panel = card.panel();
            Label feedback = UiTheme.label(" ", 13, UiTheme.GOOD);
            panel.getChildren().addAll(
                    UiTheme.label("请选择主题", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    wideButton("自然", () -> switchSkin("nature", "自然", feedback)),
                    wideButton("村庄", () -> switchSkin("village", "村庄", feedback)),
                    plainButton("返回", card::close),
                    feedback);
        });
    }

    private static void switchSkin(String skin, String displayName, Label feedback) {
        UserSession.currentSkin = skin;
        feedback.setText("已切换到" + displayName + "主题");
    }

    /** 读取存档：列出最近的存档，点击进入游戏 */
    public static PixelDialog.Card showLoadSave(Window owner) {
        return PixelDialog.showCard(owner, 560, card -> {
            VBox panel = card.panel();
            panel.getChildren().addAll(
                    UiTheme.label("最近的三份存档", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline());

            List<Record> records = RecordStore.getrecords(UserSession.username, 3);
            if (RecordStore.consumeTamperWarning()) {
                panel.getChildren().add(UiTheme.label("警告：存档被篡改", 13, UiTheme.DANGER));
            }
            if (records.isEmpty()) {
                panel.getChildren().add(UiTheme.label("暂无存档，先来玩一局吧", 14, UiTheme.TEXT_SECONDARY));
            } else {
                for (Record record : records) {
                    if (RecordStore.rightrecord(record)) {
                        panel.getChildren().add(wideButton(
                                String.format("关卡 %d · 分数 %d", record.getLevel(), record.getScore()),
                                () -> card.closeThen(() -> ScreenManager.showGame(record))));
                    } else {
                        panel.getChildren().add(UiTheme.label("发现损坏的存档文件", 13, UiTheme.DANGER));
                    }
                }
            }
            panel.getChildren().add(plainButton("返回主菜单", card::close));
        });
    }

    /** 设置：音乐/音效音量（实时生效并持久化） */
    public static void showSettings(Window owner) {
        PixelDialog.showCard(owner, 380, card -> {
            VBox panel = card.panel();
            panel.getChildren().addAll(
                    UiTheme.label("设 置", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline());

            panel.getChildren().add(volumeRow("背景音乐", model.Settings.getMusicVolume(), v -> {
                model.AudioPlayer.setMusicVolume(v);
            }));
            panel.getChildren().add(volumeRow("游戏音效", model.Settings.getSfxVolume(), v -> {
                model.AudioPlayer.setSfxVolume(v);
            }));
            panel.getChildren().add(plainButton("完成", card::close));
        });
    }

    /** 音量行：名称 + 滑条 + 百分比 */
    private static javafx.scene.layout.HBox volumeRow(String name, double initial, java.util.function.DoubleConsumer onChange) {
        Label caption = UiTheme.label(name, 14, UiTheme.TEXT_MAIN);
        javafx.scene.control.Slider slider = new javafx.scene.control.Slider(0, 100, initial * 100);
        slider.setPrefWidth(170);
        Label percent = UiTheme.label((int) Math.round(initial * 100) + "%", 13, UiTheme.ACCENT_C);
        percent.setPrefWidth(44);
        slider.valueProperty().addListener((o, was, now) -> {
            double v = now.doubleValue() / 100.0;
            percent.setText((int) Math.round(v * 100) + "%");
            onChange.accept(v);
        });
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(caption, slider, percent);
        return row;
    }

    /** 排行榜：扫描全部玩家存档，取每人历史最佳综合分 */
    public static PixelDialog.Card showLeaderboard(Window owner) {
        return PixelDialog.showCard(owner, 620, card -> {
            VBox panel = card.panel();
            panel.getChildren().addAll(
                    UiTheme.label("排行榜", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    headerRow(),
                    UiTheme.hline());

            List<Map.Entry<String, Record>> rankList = bestRecords();
            if (rankList.isEmpty()) {
                panel.getChildren().add(UiTheme.label("暂无排行榜数据，快来创造第一个纪录吧", 14, UiTheme.TEXT_SECONDARY));
            } else {
                int shown = Math.min(rankList.size(), 10);
                for (int i = 0; i < shown; i++) {
                    Record record = rankList.get(i).getValue();
                    panel.getChildren().add(rankRow(i + 1, record));
                }
            }
            panel.getChildren().add(plainButton("返回主界面", card::close));
        });
    }

    private static javafx.scene.layout.HBox rankRow(int rank, Record record) {
        Color rankColor = switch (rank) {
            case 1 -> Color.web("#B8860B");
            case 2 -> Color.web("#8E8E93");
            case 3 -> Color.web("#AD5A2B");
            default -> UiTheme.TEXT_SECONDARY;
        };
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(40);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                column(String.valueOf(rank), 90, rankColor),
                column(record.getUsername(), 180, UiTheme.TEXT_MAIN),
                column(String.format("%.2f", calculateScore(record)), 130, UiTheme.ACCENT_C));
        return row;
    }

    private static javafx.scene.layout.HBox headerRow() {
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(40);
        header.setAlignment(Pos.CENTER);
        header.getChildren().addAll(
                column("排名", 90, UiTheme.TEXT_SECONDARY),
                column("玩家", 180, UiTheme.TEXT_SECONDARY),
                column("综合分", 130, UiTheme.TEXT_SECONDARY));
        return header;
    }

    private static Label column(String text, double width, Color color) {
        Label label = UiTheme.label(text, 14, color);
        label.setPrefWidth(width);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private static AppButton wideButton(String text, Runnable action) {
        AppButton button = new AppButton(text, 240, 42, 15, AppButton.Variant.PRIMARY);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static AppButton plainButton(String text, Runnable action) {
        AppButton button = new AppButton(text, 240, 36, 14, AppButton.Variant.PLAIN);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }

    /** 每个玩家取历史最佳综合分，降序排列 */
    private static List<Map.Entry<String, Record>> bestRecords() {
        java.util.Map<String, Record> best = new java.util.HashMap<>();
        java.io.File saveFolder = new java.io.File("saves/");
        java.io.File[] files = saveFolder.listFiles((dir, name) -> name.endsWith("_saves.txt"));
        if (files != null) {
            for (java.io.File file : files) {
                String username = file.getName().replace("_saves.txt", "");
                for (Record record : RecordStore.allrecords(username)) {
                    if (!best.containsKey(username) || calculateScore(record) > calculateScore(best.get(username))) {
                        best.put(username, record);
                    }
                }
            }
        }
        List<Map.Entry<String, Record>> list = new java.util.ArrayList<>(best.entrySet());
        list.sort((a, b) -> Double.compare(calculateScore(b.getValue()), calculateScore(a.getValue())));
        return list;
    }

    /** 综合分：分数占比 + 剩余时间占比，难度越高系数越高 */
    private static double calculateScore(Record record) {
        int score = record.getScore();
        int time = record.getLefttime();
        double totalscore, totaltime;
        if (record.getDifficulty() == 1) {
            totalscore = 160;
            totaltime = 100;
            return (score / totalscore * 40 + time / totaltime * 35) * 1.2;
        } else if (record.getDifficulty() == 3) {
            totalscore = 600;
            totaltime = 90;
            return (score / totalscore * 50 + time / totaltime * 35) * 1.5;
        } else {
            totalscore = 500;
            totaltime = 150;
            return (score / totalscore * 50 + time / totaltime * 50) * 1.5;
        }
    }
}
