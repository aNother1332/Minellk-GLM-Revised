package ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Apple 风格扁平化 UI 主题：浅色液态玻璃面板、像素字体、
 * 克制的中性色与系统蓝强调色。全部界面共用，保证视觉一致。
 */
public final class UiTheme {

    /** 系统强调色（iOS 蓝） */
    public static final String ACCENT = "#0A84FF";
    public static final Color ACCENT_C = Color.web(ACCENT);
    public static final Color TEXT_MAIN = Color.web("#1D1D1F");
    public static final Color TEXT_SECONDARY = Color.web("#6E6E73");
    public static final Color DANGER = Color.web("#FF453A");
    public static final Color GOOD = Color.web("#30D158");
    /** 细线（浅色玻璃上使用） */
    public static final Color HAIRLINE = Color.web("#D2D2D7");

    private UiTheme() {}

    /** 全局像素字体（正文用），见 UIAssets 的加载逻辑 */
    public static Font font(double size) {
        return UIAssets.font(size);
    }

    /** 全局像素字体加粗（标题 / 数值） */
    public static Font fontSemi(double size) {
        return UIAssets.fontBold(size);
    }

    public static Label label(String text, double size, Color color) {
        Label label = new Label(text);
        label.setFont(fontSemi(size));
        label.setTextFill(color);
        return label;
    }

    /** 浅色液态玻璃卡片（弹窗 / 列表），半透明白 + 大圆角 + 柔和投影 */
    public static String cardStyle() {
        return "-fx-background-color: rgba(255,255,255,0.82);"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: rgba(255,255,255,0.65);"
                + "-fx-border-radius: 18;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 12;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,20,40,0.22), 24, 0.1, 0, 8);";
    }

    /** 浅色玻璃横条（游戏内状态栏 / 控制栏）：白色半透明贴边（高不透明度保证文字可读） */
    public static String hudBarStyle() {
        return "-fx-background-color: rgba(255,255,255,0.84);";
    }

    /** 浅色玻璃侧栏（物品栏，左缘圆角贴右边缘） */
    public static String hudItemStyle() {
        return "-fx-background-color: rgba(255,255,255,0.84);"
                + "-fx-background-radius: 16 0 0 16;"
                + "-fx-border-color: rgba(255,255,255,0.9);"
                + "-fx-border-width: 1 0 1 1;"
                + "-fx-border-radius: 16 0 0 16;";
    }

    /** 浮动通知条（白色玻璃圆角） */
    public static String toastStyle() {
        return "-fx-background-color: rgba(255,255,255,0.88);"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: rgba(255,255,255,0.9);"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 8 18 8 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0.1, 0, 6);";
    }

    /** 浅色玻璃上的物品栏槽位：半透明灰圆角 */
    public static String slotStyle() {
        return "-fx-background-color: rgba(120,120,128,0.12);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: rgba(60,60,67,0.18);"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 10;"
                + "-fx-padding: 4;";
    }

    /** 选中的槽位：系统蓝描边 */
    public static String slotArmedStyle() {
        return "-fx-background-color: rgba(10,132,255,0.16);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: " + ACCENT + ";"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 10;"
                + "-fx-padding: 3;";
    }

    /** 浅色玻璃内的细分隔线 */
    public static Region hline() {
        Region line = new Region();
        line.setStyle("-fx-background-color: rgba(60,60,67,0.18);");
        line.setPrefHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);
        return line;
    }

    /** 竖向细分隔线（状态栏用） */
    public static Region vline() {
        Region line = new Region();
        line.setStyle("-fx-background-color: rgba(60,60,67,0.20);");
        line.setPrefSize(1, 30);
        return line;
    }
}
