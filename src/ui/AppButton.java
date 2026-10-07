package ui;

import javafx.scene.Cursor;
import javafx.scene.control.Button;

/**
 * Apple 风格扁平按钮：
 * PRIMARY 蓝底白字（主操作）、WHITE 白底深字（浅色背景上的次操作）、
 * TINTED 浅蓝底蓝字（卡片内次操作）、GLASS 半透明白（深色 HUD 上）、
 * PLAIN 纯文字（返回类操作）。
 */
public class AppButton extends Button {

    public enum Variant { PRIMARY, WHITE, TINTED, GLASS, PLAIN }

    private final Variant variant;

    public AppButton(String text, double width, double height, double fontSize, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(UiTheme.fontSemi(fontSize));
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setCursor(Cursor.HAND);
        setFocusTraversable(false);
        disabledProperty().addListener((o, was, now) -> setOpacity(now ? 0.4 : 1.0));
        hoverProperty().addListener((o, was, now) -> refreshStyle());
        pressedProperty().addListener((o, was, now) -> refreshStyle());
        refreshStyle();
    }

    /** 默认主操作样式 */
    public AppButton(String text, double width, double height, double fontSize) {
        this(text, width, height, fontSize, Variant.PRIMARY);
    }

    private void refreshStyle() {
        String bg;
        String fg;
        boolean underline = false;
        switch (variant) {
            case PRIMARY -> {
                bg = isPressed() ? "#0664C8" : isHover() ? "#2492FF" : UiTheme.ACCENT;
                fg = "#FFFFFF";
            }
            case WHITE -> {
                bg = isPressed() ? "rgba(240,240,245,0.92)" : isHover() ? "rgba(255,255,255,0.98)" : "rgba(255,255,255,0.92)";
                fg = "#1D1D1F";
            }
            case TINTED -> {
                bg = isPressed() ? "#C7DEFB" : isHover() ? "#D8E9FF" : "#E5F0FF";
                fg = UiTheme.ACCENT;
            }
            case GLASS -> {
                bg = isPressed() ? "rgba(255,255,255,0.30)" : isHover() ? "rgba(255,255,255,0.22)" : "rgba(255,255,255,0.14)";
                fg = "#FFFFFF";
            }
            case PLAIN -> {
                bg = "transparent";
                fg = isHover() ? "#3A9BFF" : UiTheme.ACCENT;
                underline = isHover();
            }
            default -> {
                bg = "transparent";
                fg = "#FFFFFF";
            }
        }
        String style = "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 0;"
                + "-fx-underline: " + underline + ";";
        setStyle(style);
        setTextFill(javafx.scene.paint.Color.web(fg));
    }
}
