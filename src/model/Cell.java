package model;

import java.io.Serializable;

public class Cell implements Serializable {
    private static final long serialVersionUID=1L;
    Position pos;
    boolean isEmpty;
    int number;
    boolean isChosen;
    public Cell(Position pos,boolean isEmpty,int iconIndex) {
        this.pos=pos;
        this.isEmpty=isEmpty;
        this.number=iconIndex;
    }
    public boolean getIsChosen() {
        return isChosen;
    }
    public void setChosen(boolean chosen) {
        isChosen = chosen;
    }
    public Position getPos() {
        return pos;
    }
    public boolean isEmpty() {
        return isEmpty;
    }
    public int getNumber() {
        return number;
    }
    public void setempty(boolean empty) {
        isEmpty=empty;
        number=0;
    }
}