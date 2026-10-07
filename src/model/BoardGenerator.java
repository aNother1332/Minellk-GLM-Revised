package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 棋盘生成与工具：按难度生成随机棋盘、深拷贝，集中管理各难度的初始参数。
 * 从原 GameFrame 中的生成逻辑抽出，使游戏逻辑与界面解耦。
 */
public final class BoardGenerator {
    private BoardGenerator() {}

    public static int initialPairs(GameDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 16;
            case HARD -> 50;
            case ENDLESS -> 40;
        };
    }

    public static int totalTime(GameDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 100;
            case HARD -> 150;
            case ENDLESS -> 90;
        };
    }

    public static Cell[][] generate(GameDifficulty difficulty) {
        Cell[][] board = difficulty == GameDifficulty.HARD ? new Cell[12][12] : new Cell[11][11];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = new Cell(new Position(i, j), true, 0);
            }
        }
        List<Integer> icons = switch (difficulty) {
            case EASY -> iconList(32, 5);
            case ENDLESS -> iconList(60, 5);
            case HARD -> iconList(100, 12);
        };
        int n = 0;
        if (difficulty == GameDifficulty.EASY) {
            for (int i = 1; i <= 4; i++) {
                for (int j = 1; j <= 4; j++) {
                    board[i][j] = new Cell(new Position(i, j), false, icons.get(n++));
                }
            }
            for (int i = 6; i <= 9; i++) {
                for (int j = 6; j <= 9; j++) {
                    board[i][j] = new Cell(new Position(i, j), false, icons.get(n++));
                }
            }
        } else if (difficulty == GameDifficulty.ENDLESS) {
            // 无尽模式：内圈随机散布 60 枚（30 对），消除后在原位补充，保持连通性
            List<Position> interior = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                for (int j = 1; j <= 9; j++) {
                    interior.add(new Position(i, j));
                }
            }
            Collections.shuffle(interior);
            for (int k = 0; k < icons.size(); k++) {
                Position pos = interior.get(k);
                board[pos.getRow()][pos.getCol()] = new Cell(pos, false, icons.get(k));
            }
        } else {
            for (int i = 1; i < 11; i++) {
                for (int j = 1; j < 11; j++) {
                    board[i][j] = new Cell(new Position(i, j), false, icons.get(n++));
                }
            }
        }
        return board;
    }

    private static List<Integer> iconList(int totalIcons, int maxIconIndex) {
        List<Integer> icons = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < totalIcons / 2; i++) {
            int a = random.nextInt(maxIconIndex) + 1;
            icons.add(a);
            icons.add(a);
        }
        Collections.shuffle(icons);
        return icons;
    }

    /** 洗牌道具：随机重排棋盘上所有非空棋子的图标（数量与种类保持不变） */
    public static void shuffleNumbers(GameBoard board) {
        List<Integer> numbers = new ArrayList<>();
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < board.getRowCnt(); i++) {
            for (int j = 0; j < board.getColCnt(); j++) {
                Cell cell = board.getCell(i, j);
                if (cell != null && !cell.isEmpty()) {
                    numbers.add(cell.getNumber());
                    cells.add(cell);
                }
            }
        }
        Collections.shuffle(numbers);
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setNumber(numbers.get(i));
        }
    }

    public static Cell[][] deepCopy(Cell[][] original) {
        if (original == null) {
            return null;
        }
        Cell[][] copy = new Cell[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[i].length; j++) {
                Cell cell = original[i][j];
                if (cell != null) {
                    Cell b = new Cell(cell.getPos(), cell.isEmpty(), cell.getNumber());
                    b.setChosen(cell.getIsChosen());
                    copy[i][j] = b;
                }
            }
        }
        return copy;
    }
}
