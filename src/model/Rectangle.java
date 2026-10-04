package model;

import java.awt.*;

public class Rectangle {
    int x;
    int y;
    int width;
    int height;
    public Rectangle(int x,int y,int width,int height) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
    }
    public Point centerposition() {
        return new Point(x + width / 2, y + height / 2);
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
}