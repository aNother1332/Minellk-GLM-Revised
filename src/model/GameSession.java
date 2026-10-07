package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.Utils;

/**
 * 一局游戏的规则状态：分数、连击、道具点数、死局判定。
 * 纯数据与规则，不含任何 UI，便于单元测试与新玩法复用。
 */
public class GameSession {

    private int score;
    private int itemPoints;
    private final Map<Integer, Integer> comboMap = new HashMap<>();

    public GameSession(int startItemPoints) {
        reset(startItemPoints);
    }

    /** 开局/重开：清零分数与连击，重置道具点数 */
    public void reset(int startItemPoints) {
        score = 0;
        itemPoints = startItemPoints;
        comboMap.clear();
    }

    public int getScore() {
        return score;
    }

    public int getItemPoints() {
        return itemPoints;
    }

    /** 消除 iconNumber 一对：按连击规则计分，返回本次得分 */
    public int addPairScore(int iconNumber) {
        int oldCombo = comboMap.getOrDefault(iconNumber, 0);
        for (Map.Entry<Integer, Integer> e : comboMap.entrySet()) {
            if (!e.getKey().equals(iconNumber)) {
                e.setValue(0);
            }
        }
        int newCombo = oldCombo + 1;
        comboMap.put(iconNumber, newCombo);

        int points = 10;
        if (newCombo >= 3) {
            points = (newCombo - 1) * 10;
            itemPoints += 2;
        }
        score += points;
        return points;
    }

    public int comboOf(int iconNumber) {
        return comboMap.getOrDefault(iconNumber, 0);
    }

    /** 从存档恢复历史分数（不产生连击副作用） */
    public void restoreScore(int restoredScore) {
        this.score = restoredScore;
    }

    /** 重置某个图标的连击（供特殊玩法使用） */
    public void resetCombo(int iconNumber) {
        comboMap.put(iconNumber, 0);
    }

    /** 消耗一个道具点数，点数不足返回 false */
    public boolean spendItemPoint() {
        if (itemPoints < 1) {
            return false;
        }
        itemPoints--;
        return true;
    }

    /**
     * 死局判定：道具点数大于 0 时不会判负；否则同一图标的棋子分组后
     * 只在组内两两试连（与「所有棋子两两配对」结果一致，但大幅减少 BFS 次数）。
     */
    public boolean isDeadlock(GameBoard board) {
        if (itemPoints > 0) {
            return false;
        }
        List<Position> cells = board.getcellpositions();
        if (cells.size() < 2) {
            return true;
        }
        Map<Integer, List<Position>> groups = new HashMap<>();
        for (Position p : cells) {
            Cell cell = board.getCell(p.getRow(), p.getCol());
            groups.computeIfAbsent(cell.getNumber(), k -> new ArrayList<>()).add(p);
        }
        for (List<Position> group : groups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    if (Utils.findPath(board, group.get(i), group.get(j)) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
