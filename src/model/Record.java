package model;
import ui.ItemPanel;

import java.util.Date;

public class Record {
    private String username;
    private Date savetime;
    private int score;
    private int lefttime;
    private int difficulty;
    private Cell[][]board;
    private int remaincount;
    private String checksum;
    private int points;

    public Record(String username,int score,int lefttime,int difficulty,Cell[][]board,int remaincount){
        this.username=username;
        this.savetime =new Date();
        this.score=score;
        this.lefttime = lefttime;
        this.difficulty=difficulty;
        this.board = board;
        this.remaincount = remaincount;

    }//没有校验和，用于第一次创建存档
    public Record(String username, int score, int lefttime, int difficulty, Cell[][]board, int remaincount, String checksum,int points){
        this.username=username;
        this.savetime=new Date();
        this.score=score;
        this.lefttime=lefttime;
        this.difficulty=difficulty;
        this.board=board;
        this.remaincount = remaincount;
        this.checksum=checksum;
        this.points=points;
    }//有校验和，用于保存存档
    public String getUsername() {
        return username;
    }
    public Date getSavetime() {
        return savetime;
    }
    public int getScore() {
        return score;
    }
    public int getLefttime() {
        return lefttime;
    }
    public int getDifficulty() {
        return difficulty;
    }
    public Cell[][] getBoard() {
        return board;
    }
    public int getRemaincount() {
        return remaincount;
    }
    public String getChecksum() {
        return checksum;
    }
    public int getLevel() {
        return difficulty;
    }
    public int getPoints() {
        return ItemPanel.points;
    }
}