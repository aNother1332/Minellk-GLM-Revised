package model;

import java.io.Serializable;

public class Position implements Serializable {
    private static final long serialVersionUID = 1L;
    private int row;
    private int col;
    public Position(int row,int col) {
        this.row=row;
        this.col=col;
    }
    public int getRow() {
        return row;
    }
    public int getCol() {
        return col;
    }
    @Override
    public boolean equals(Object obj) {//重写equals方法，判断两个棋子是否相同
        if (this==obj) return true;
        if (obj==null||getClass()!=obj.getClass()) return false;
        Position other=(Position) obj;
        if(this.row==other.row&&this.col==other.col){
            return true;
        }
        else{return false;}
    }

    @Override
    public int hashCode() {//与equals配套：等值的Position必须等哈希，否则HashMap按值查找会失败
        return 31*row+col;
    }
}