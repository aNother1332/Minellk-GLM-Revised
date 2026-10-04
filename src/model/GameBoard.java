package model;

import java.util.ArrayList;
import java.util.List;

public class GameBoard {
    int rowCnt;
    int colCnt;
    Cell[][] board;

    public GameBoard(int rowCnt, int colCnt, Cell[][] board) {
        this.rowCnt = rowCnt;
        this.colCnt = colCnt;
        this.board = board;
    }
    public int getRowCnt() {
        return rowCnt;
    }
    public int getColCnt() {
        return colCnt;
    }
    public Cell getCell(int row, int col) {
        return board[row][col];
    }
    public Cell[][] getBoard() {
        return board;
    }
    public void clearchosen() {
        for (int i=0;i<rowCnt;i++) {
            for (int j=0;j<colCnt;j++) {
                if (board[i][j]!=null) {
                    board[i][j].setChosen(false);
                }
            }
        }
    }
    public List<Position> getcellpositions() {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < rowCnt; i++) {
            for (int j = 0; j < colCnt; j++) {
                Cell cell = board[i][j];
                if (cell != null && !cell.isEmpty()) {
                    positions.add(new Position(i, j));
                }
            }
        }
        return positions;
    }
}