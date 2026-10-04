package model;

import java.util.ArrayList;
import java.util.List;

public class Line {
    private List<Position> pathPoints;
    public Line(List<Position> pathPoints) {
        this.pathPoints = pathPoints;
    }
    public List<Position> getPathPoints() {
        return pathPoints;
    }
}