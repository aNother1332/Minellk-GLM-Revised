package ui;

import app.UserSession;

/**
 * 方块图标的中文文名称，从原 StatusPanel.getname 抽出。
 */
public final class BlockCatalog {

    private BlockCatalog() {}

    public static String name(int iconIndex) {
        if ("nature".equals(UserSession.currentSkin)) {
            switch (iconIndex) {
                case 1: return "石头";
                case 2: return "橡木原木";
                case 3: return "草方块";
                case 4: return "煤矿石";
                case 5: return "橡木树叶";
                case 6: return "泥土";
                case 7: return "铁矿石";
                case 8: return "南瓜";
                case 9: return "沙子";
                case 10: return "钻石矿石";
                case 11: return "西瓜";
                case 12: return "雪方块";
                default: return "";
            }
        } else {
            switch (iconIndex) {
                case 1: return "熔炉";
                case 2: return "橡木木板";
                case 3: return "泥土路径";
                case 4: return "砖块";
                case 5: return "工作台";
                case 6: return "沙子";
                case 7: return "炼药锅";
                case 8: return "箱子";
                case 9: return "黏土块";
                case 10: return "砂轮";
                case 11: return "书架";
                case 12: return "干草块";
                default: return "";
            }
        }
    }
}
