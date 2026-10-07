import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.GameDifficulty;
import ui.MenuCards;
import ui.ScreenManager;

import java.util.Set;

/**
 * 开发用回归工具：模拟 JavaFX 拾取，检查每个关键按钮中心点的最顶层节点
 * 是否就是按钮本身（或其子节点）——专门捕捉「透明容器拉伸后盖住按钮」这类回归。
 * 不依赖真实鼠标，可随时运行。
 */
public class HitTestSuite extends Application {

    private int passed;
    private int failed;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ScreenManager.init(stage);
        ScreenManager.showMainMenu();
        stage.setX(30);
        stage.setY(30);
        stage.show();
        AudioPlayer.backgroundsound();

        javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.8), e -> {
                    // 主菜单按钮
                    hitCheck("主菜单/开始新游戏", "开始新游戏");
                    hitCheck("主菜单/读取存档", "读取存档");
                    hitCheck("主菜单/排行榜", "排行榜");
                    hitCheck("主菜单/更换主题", "更换主题");
                    hitCheck("主菜单/登录", "登录");
                    hitCheck("主菜单/设置", "设置");
                    // 主菜单卡片：排行榜卡片内按钮
                    ui.PixelDialog.Card card = MenuCards.showLeaderboard(stage.getScene().getWindow());
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    pause.setOnFinished(e2 -> {
                        hitCheck("排行榜卡片/返回主界面", "返回主界面");
                        card.close();
                        // 游戏界面按钮
                        javafx.animation.PauseTransition p2 = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
                        p2.setOnFinished(e3 -> {
                            ScreenManager.showGame(GameDifficulty.EASY);
                            javafx.animation.PauseTransition p3 = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
                            p3.setOnFinished(e4 -> {
                                hitCheck("游戏/开始游戏", "开始游戏");
                                hitCheck("游戏/重新开始", "重新开始");
                                hitCheck("游戏/暂停", "暂停");
                                System.out.println("HitTestSuite: OK=" + passed + " FAIL=" + failed);
                                Platform.exit();
                            });
                            p3.play();
                        });
                        p2.play();
                    });
                    pause.play();
                }),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(6), e -> Platform.exit()));
        tl.play();
    }

    /** 检查按钮中心点最顶层节点是否属于按钮自身 */
    private void hitCheck(String name, String buttonText) {
        Scene scene = currentScene();
        Set<Node> buttons = scene.getRoot().lookupAll(".button");
        Node target = null;
        for (Node n : buttons) {
            if (n instanceof javafx.scene.control.Button b && buttonText.equals(b.getText())) {
                target = b;
                break;
            }
        }
        if (target == null) {
            System.out.println("[hit] " + name + ": 按钮不存在  [FAIL]");
            failed++;
            return;
        }
        if (target.isDisable()) {
            System.out.println("[hit] " + name + ": 禁用状态（点击无反应为设计行为），仅验证未被遮挡");
        }
        Bounds bounds = target.localToScene(target.getBoundsInLocal());
        Point2D center = new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2,
                bounds.getMinY() + bounds.getHeight() / 2);

        Node top = topmostNodeAt(scene.getRoot(), center);
        boolean ok = isSelfOrDescendantOf(target, top);
        System.out.println("[hit] " + name + ": 顶层节点=" + describe(top) + (ok ? "  [OK]" : "  [FAIL] 被遮挡"));
        if (ok) passed++; else failed++;
    }

    private String describe(Node n) {
        if (n == null) {
            return "null";
        }
        String cls = n.getClass().getSimpleName();
        if (n instanceof javafx.scene.control.Labeled l && !l.getText().isEmpty()) {
            return cls + "[" + l.getText() + "]";
        }
        return cls;
    }

    private boolean isSelfOrDescendantOf(Node ancestor, Node candidate) {
        Node n = candidate;
        while (n != null) {
            if (n == ancestor) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /** 按 JavaFX 拾取规则模拟：取包含该点的最顶层（z 序最高）可见非透明节点 */
    private Node topmostNodeAt(Parent root, Point2D scenePoint) {
        return pickRecursive(root, scenePoint, root);
    }

    private Node pickRecursive(Parent parent, Point2D scenePoint, Node currentBest) {
        Node best = currentBest;
        for (Node child : parent.getChildrenUnmodifiable()) {
            // 注意：禁用的按钮仍占据点击区域（点击无反应是设计行为），不能跳过
            if (child.isMouseTransparent() || !child.isVisible()) {
                continue;
            }
            Point2D local = child.sceneToLocal(scenePoint);
            if (!child.contains(local)) {
                continue;
            }
            best = child; // 后遍历的子节点 z 序更高
            if (child instanceof Parent p) {
                best = pickRecursive(p, scenePoint, best);
            }
        }
        return best;
    }

    private Scene currentScene() {
        try {
            java.lang.reflect.Field f = ScreenManager.class.getDeclaredField("stage");
            f.setAccessible(true);
            return ((javafx.stage.Stage) f.get(null)).getScene();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
