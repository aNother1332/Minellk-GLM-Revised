package ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 模态弹窗：以「场景内全屏覆盖层」实现——半透明遮罩铺满整个游戏画面，
 * 浅色玻璃卡片居中。入场时遮罩淡入、卡片缩放淡入；退场时整体同步淡出。
 * 遮罩天然挡住下层所有交互，卡片内的按钮正常可点。
 */
public final class PixelDialog {

    /** 动画时长与缓动曲线（整体节奏柔和） */
    private static final Duration OPEN = Duration.millis(420);
    private static final Duration CLOSE = Duration.millis(260);
    private static final Interpolator OPEN_CURVE = Interpolator.SPLINE(0.16, 1, 0.3, 1);
    private static final Interpolator CLOSE_CURVE = Interpolator.SPLINE(0.7, 0, 0.84, 0);

    /** 每个场景当前打开的弹窗（供 ESC 关闭 / Enter 确认） */
    private static final Map<Scene, Card> OPEN_CARDS = new LinkedHashMap<>();

    private PixelDialog() {}

    /** 单按钮提示框 */
    public static void info(Window owner, String message) {
        info(owner, message, 400);
    }

    /** 无按钮提示框：点击卡片外任意位置整体渐隐关闭 */
    public static void info(Window owner, String message, double width) {
        showCard(owner, width, true, card -> {
            VBox panel = card.panel();
            panel.getChildren().add(textLabel(message));
            panel.getChildren().add(UiTheme.label("点击空白处关闭", 11, UiTheme.TEXT_SECONDARY));
        });
    }

    /** 多按钮选择框：onChoice 在点击按钮时回调（此时卡片已开始关闭动画） */
    public static void choice(Window owner, String message, String[] options, Consumer<Integer> onChoice) {
        choice(owner, message, 420, options, onChoice);
    }

    public static void choice(Window owner, String message, double width, String[] options, Consumer<Integer> onChoice) {
        showCard(owner, width, card -> {
            card.panel().getChildren().add(textLabel(message));
            HBox buttons = new HBox(20);
            buttons.setAlignment(Pos.CENTER);
            for (int i = 0; i < options.length; i++) {
                final int index = i;
                AppButton.Variant variant = i == 0 ? AppButton.Variant.PRIMARY : AppButton.Variant.TINTED;
                AppButton button = new AppButton(options[i], 120, 40, 15, variant);
                button.setOnAction(e -> {
                    card.close();
                    if (onChoice != null) {
                        onChoice.accept(index);
                    }
                });
                if (i == 0) {
                    card.setDefaultAction(button::fire);
                }
                buttons.getChildren().add(button);
            }
            card.panel().getChildren().add(buttons);
        });
    }

    /**
     * 打开自定义卡片弹窗（排行榜/难度选择等复用）。
     * builder 中向 card.panel() 填充内容，用 card.close()/closeThen() 关闭。
     */
    public static Card showCard(Window owner, double cardWidth, Consumer<Card> builder) {
        return showCard(owner, cardWidth, false, builder);
    }

    public static Card showCard(Window owner, double cardWidth, boolean dismissOnOutsideClick, Consumer<Card> builder) {
        Scene scene = owner.getScene();
        Pane rootPane = (Pane) scene.getRoot();

        // 遮罩：铺满整个场景（StackPane 子节点自动填满），天然挡住下层全部交互
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(15,18,28,0.45);");
        overlay.setOpacity(0);

        VBox panel = new VBox(14);
        panel.setStyle(UiTheme.cardStyle());
        panel.setPadding(new Insets(20, 24, 18, 24));
        panel.setAlignment(Pos.CENTER);
        // 关键：限制为首选尺寸，否则会被 StackPane 拉伸成整屏大块
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setMinWidth(Math.min(cardWidth, rootPane.getWidth() > 0 ? rootPane.getWidth() - 40 : cardWidth));

        Card card = new Card(rootPane, overlay, panel);
        builder.accept(card);
        overlay.getChildren().add(panel);
        rootPane.getChildren().add(overlay);
        OPEN_CARDS.put(scene, card);
        if (dismissOnOutsideClick) {
            // 点击卡片外的遮罩：整体渐隐关闭（点击卡片内部不受影响）
            overlay.setOnMouseClicked(e -> {
                if (e.getTarget() == overlay) {
                    card.close();
                }
            });
        }

        playOpen(overlay, panel);
        return card;
    }

    /** 弹窗句柄：向调用方暴露卡片内容与关闭动作 */
    public static final class Card {
        private final Pane rootPane;
        private final StackPane overlay;
        private final VBox panel;
        private Runnable onClosed;
        private Runnable defaultAction;
        private boolean closing;

        private Card(Pane rootPane, StackPane overlay, VBox panel) {
            this.rootPane = rootPane;
            this.overlay = overlay;
            this.panel = panel;
        }

        public VBox panel() {
            return panel;
        }

        /** 弹窗完全关闭（含退场动画）后回调 */
        public void onClosed(Runnable action) {
            this.onClosed = action;
        }

        /** 设置默认确认动作（Enter 键触发，见 confirmTop） */
        void setDefaultAction(Runnable action) {
            this.defaultAction = action;
        }

        void fireDefaultAction() {
            if (defaultAction != null) {
                defaultAction.run();
            }
        }

        /** 播放退场动画后移除弹窗 */
        public void close() {
            closeThen(null);
        }

        /** 播放退场动画，动画结束后执行动作（用于“关闭后再切换界面”的过渡） */
        public void closeThen(Runnable action) {
            if (closing) {
                return;
            }
            closing = true;
            Timeline close = new Timeline(new KeyFrame(CLOSE,
                    new KeyValue(overlay.opacityProperty(), 0, CLOSE_CURVE),
                    new KeyValue(panel.opacityProperty(), 0, CLOSE_CURVE),
                    new KeyValue(panel.scaleXProperty(), 0.94, CLOSE_CURVE),
                    new KeyValue(panel.scaleYProperty(), 0.94, CLOSE_CURVE)));
            close.setOnFinished(e -> {
                rootPane.getChildren().remove(overlay);
                OPEN_CARDS.values().removeIf(c -> c == this);
                if (action != null) {
                    action.run();
                }
                if (onClosed != null) {
                    onClosed.run();
                }
            });
            close.play();
        }
    }

    private static void playOpen(StackPane overlay, VBox panel) {
        overlay.setOpacity(0);
        panel.setOpacity(0);
        panel.setScaleX(0.92);
        panel.setScaleY(0.92);
        Timeline open = new Timeline(new KeyFrame(OPEN,
                new KeyValue(overlay.opacityProperty(), 1, OPEN_CURVE),
                new KeyValue(panel.opacityProperty(), 1, OPEN_CURVE),
                new KeyValue(panel.scaleXProperty(), 1, OPEN_CURVE),
                new KeyValue(panel.scaleYProperty(), 1, OPEN_CURVE)));
        open.play();
    }

    /** 关闭该窗口最上层的弹窗（ESC），返回是否有关闭 */
    public static boolean closeTop(Window owner) {
        Card card = OPEN_CARDS.get(owner.getScene());
        if (card != null) {
            card.close();
            return true;
        }
        return false;
    }

    /** 触发该窗口最上层弹窗的默认按钮（Enter），返回是否触发 */
    public static boolean confirmTop(Window owner) {
        Card card = OPEN_CARDS.get(owner.getScene());
        if (card != null && card.defaultAction != null) {
            card.fireDefaultAction();
            return true;
        }
        return false;
    }

    private static Label textLabel(String message) {
        Label label = UiTheme.label(message, 15, UiTheme.TEXT_MAIN);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        return label;
    }
}
