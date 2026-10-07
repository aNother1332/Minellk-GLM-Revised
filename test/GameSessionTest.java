import model.Cell;
import model.GameBoard;
import model.GameSession;
import model.Position;

/** GameSession 规则测试（无 UI）：main 直接断言，用于回归。 */
public class GameSessionTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        testComboScoring();
        testItemPointEconomy();
        testDeadlock();
        System.out.println("GameSessionTest: OK=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, Object actual, Object expect) {
        boolean ok = actual.equals(expect);
        System.out.println("[check] " + name + "=" + actual + (ok ? "  [OK]" : "  [FAIL] 期望 " + expect));
        if (ok) passed++; else failed++;
    }

    private static void testComboScoring() {
        GameSession s = new GameSession(2);
        check("首对基础分", s.addPairScore(3), 10);
        check("第二对基础分", s.addPairScore(3), 10);
        check("连击3得分", s.addPairScore(3), 20);   // (3-1)*10
        check("连击4得分", s.addPairScore(3), 30);   // (4-1)*10
        check("总分累计", s.getScore(), 70);
        check("连击次数", s.comboOf(3), 4);
        // 换图标：连击清零重新计数
        check("换图标得分", s.addPairScore(5), 10);
        check("原图标连击已清零", s.comboOf(3), 0);
        check("换图标后总分", s.getScore(), 80);
        // 连击奖励：连击≥3 每次给 2 点
        GameSession r = new GameSession(2);
        r.addPairScore(1);
        r.addPairScore(1);
        check("连击前无奖励", r.getItemPoints(), 2);
        r.addPairScore(1);
        check("连击3奖励2点", r.getItemPoints(), 4);
        r.restoreScore(999);
        check("恢复历史分数", r.getScore(), 999);
    }

    private static void testItemPointEconomy() {
        GameSession s = new GameSession(1);
        check("初始点数", s.getItemPoints(), 1);
        check("消耗成功", s.spendItemPoint(), true);
        check("点数耗尽后消耗失败", s.spendItemPoint(), false);
        check("点数仍为0", s.getItemPoints(), 0);
    }

    private static void testDeadlock() {
        // 4×4 棋盘：编号 2 表示相邻可消除的一对
        GameSession s = new GameSession(0); // 点数为 0 才会判死局
        Cell[][] cells = new Cell[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                cells[i][j] = new Cell(new Position(i, j), true, 0);
            }
        }
        cells[1][1] = new Cell(new Position(1, 1), false, 1);
        cells[1][2] = new Cell(new Position(1, 2), false, 1);
        GameBoard board = new GameBoard(4, 4, cells);
        check("相邻同色不算死局", s.isDeadlock(board), false);

        // 两枚不同色的孤子 → 死局
        cells[1][2].setNumber(2);
        check("异色孤子是死局", s.isDeadlock(board), true);

        // 有道具点数时不判负
        GameSession withPoints = new GameSession(1);
        check("有点数不判负", withPoints.isDeadlock(board), true == false);
    }
}
