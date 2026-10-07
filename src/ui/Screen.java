package ui;

import javafx.scene.Scene;

/**
 * 单个界面的抽象：所有界面共用一个 Stage，通过 ScreenManager 切换 Scene。
 */
public interface Screen {

    Scene scene();

    String title();

    /** 窗口宽度（0 表示不调整） */
    default double width() {
        return 0;
    }

    /** 窗口高度（0 表示不调整） */
    default double height() {
        return 0;
    }

    default boolean resizable() {
        return false;
    }

    /**
     * 用户点击窗口关闭按钮时回调。
     * @return true 表示拦截本次关闭（例如游戏界面保存进度后返回主菜单），false 表示正常退出程序
     */
    default boolean onCloseRequest() {
        return false;
    }
}
