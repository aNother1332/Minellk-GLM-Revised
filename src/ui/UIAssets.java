package ui;

import app.UserSession;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源加载与共享样式：图片缓存、像素字体、按钮/背景工厂。
 * 图片只加载一次并缓存；棋子图标在加载时预缩放到固定纹理大小，
 * 运行期缩放交给 GPU 的 ImageView，避免窗口缩放时反复重采样。
 */
public final class UIAssets {

    public static final int ICON_TEXTURE = 128;

    private static final Map<String, Image> images = new HashMap<>();
    private static final Map<String, Color[]> palettes = new HashMap<>();
    private static final Image EMPTY = new Image("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    /**
     * 全局字体：优先加载 resource/fonts 下打包的像素字体（与原 Swing 版一致），
     * 未命中时回退到系统安装的同名字体，最后回退到系统默认字体。
     */
    private static final String FONT_FAMILY = resolveFontFamily();

    private UIAssets() {}

    private static String resolveFontFamily() {
        String wanted = "fusion-pixel-10px-monospaced-zh_hans.ttf";
        File bundled = new File("resource/fonts/" + wanted);
        if (bundled.exists()) {
            Font.loadFont(bundled.toURI().toString(), 12);
        } else {
            // 尝试从系统字体目录加载（开发机上已安装）
            File system = new File(System.getProperty("user.home")
                    + "/AppData/Local/Microsoft/Windows/Fonts/" + wanted);
            if (system.exists()) {
                Font.loadFont(system.toURI().toString(), 12);
            }
        }
        for (String family : Font.getFamilies()) {
            if (family.toLowerCase().startsWith("fusion pixel")) {
                return family;
            }
        }
        return "System";
    }

    /** 像素字体正文 */
    public static Font font(double size) {
        return Font.font(FONT_FAMILY, size);
    }

    /** 像素字体加粗（标题 / 数值，原版游戏全部使用粗体） */
    public static Font fontBold(double size) {
        return Font.font(FONT_FAMILY, FontWeight.BOLD, size);
    }

    public static Image image(String path) {
        return images.computeIfAbsent(path, p -> {
            File file = new File(p);
            return file.exists() ? new Image(file.toURI().toString()) : EMPTY;
        });
    }

    /** 加载并预缩放到指定大小（加载期插值，运行期零开销） */
    public static Image image(String path, int width, int height) {
        String key = path + "@" + width + "x" + height;
        return images.computeIfAbsent(key, k -> {
            File file = new File(path);
            return file.exists() ? new Image(file.toURI().toString(), width, height, true, true, false) : EMPTY;
        });
    }

    /** 当前皮肤的方块图标（预缩放纹理） */
    public static Image tileIcon(int index) {
        return image(tilePath(index), ICON_TEXTURE, ICON_TEXTURE);
    }

    private static String tilePath(int index) {
        return "resource/images/" + UserSession.currentSkin + "_" + index + ".png";
    }

    /** 背景图最大解码边长：在清晰度与内存占用间取平衡 */
    private static final int BG_MAX_DIM = 1200;

    /** 拉伸铺满容器的背景图（解码时限制最大边长，节省内存） */
    public static ImageView backgroundView(String path) {
        ImageView view = new ImageView(backgroundImage(path));
        view.setPreserveRatio(false);
        view.setMouseTransparent(true);
        return view;
    }

    /** 背景图统一入口：解码时按最大边长 1200 预缩放（约省 20–30% 背景内存） */
    public static Image backgroundImage(String path) {
        String key = "bg:" + path;
        return images.computeIfAbsent(key, k -> {
            File file = new File(path);
            return file.exists() ? new Image(file.toURI().toString(), BG_MAX_DIM, BG_MAX_DIM, true, true, false) : EMPTY;
        });
    }

    public static Background stretchedBackground(String path) {
        BackgroundImage bi = new BackgroundImage(image(path),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(100, 100, true, true, false, false));
        return new Background(bi);
    }

    public static Background solidBackground(Color color) {
        return new Background(new BackgroundFill(color, null, null));
    }

    /** 从方块图标采样主色调，用作消除粒子颜色 */
    public static Color[] palette(int iconIndex) {
        String key = UserSession.currentSkin + "_" + iconIndex;
        return palettes.computeIfAbsent(key, k -> {
            Image img = image(tilePath(iconIndex));
            return new Color[]{averageColor(img)};
        });
    }

    private static Color averageColor(Image img) {
        if (img == null || img.getWidth() < 2) {
            return Color.SANDYBROWN;
        }
        PixelReader reader = img.getPixelReader();
        double r = 0, g = 0, b = 0;
        int n = 0;
        for (int i = 1; i <= 6; i++) {
            for (int j = 1; j <= 6; j++) {
                int x = (int) (img.getWidth() * i / 7);
                int y = (int) (img.getHeight() * j / 7);
                Color c = reader.getColor(x, y);
                if (c.getOpacity() < 0.3) {
                    continue;
                }
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                n++;
            }
        }
        if (n == 0) {
            return Color.SANDYBROWN;
        }
        return new Color(r / n, g / n, b / n, 1);
    }
}
