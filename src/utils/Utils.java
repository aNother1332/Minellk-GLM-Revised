package utils;

import model.Cell;
import model.GameBoard;
import model.Position;

import java.util.*;
public class Utils {
    private static final int[][] direction={
            {-1, 0}, // 上//n=0
            {1, 0},  // 下//n=1
            {0, -1}, // 左//n=2
            {0, 1}   // 右//n=3
    };
    static class Node {
        Position pos;
        int dir;
        int turns;
        List<Position> path;
        public Node(Position pos,int dir,int turns,List<Position> path) {//创建一个搜索节点
            this.pos=pos;
            this.dir=dir;
            this.turns=turns;
            this.path=path;
        }
    }
    public static boolean isValid(GameBoard board,int row,int col) {
        return row>=0&&row<board.getRowCnt()&&col>=0&&col<board.getColCnt();//判断是否越界
    }
    public static List<Position> findPath(GameBoard board,Position start,Position end) {
        if (start.equals(end)) {
            return null;
        }
        Cell startcell=board.getCell(start.getRow(), start.getCol());
        Cell endcell=board.getCell(end.getRow(), end.getCol());

        if (startcell.isEmpty()||endcell.isEmpty()) {
            return null;
        }
        if (startcell.getNumber()!=endcell.getNumber()) {
            return null;
        }
        int rows=board.getRowCnt();
        int cols=board.getColCnt();

        boolean[][][] visited=new boolean[rows][cols][4];//记录行，列，方向
        Queue<Node> queue=new LinkedList<>();//BFS序列

        for (int d=0;d<4;d++) {
            int nr=start.getRow()+direction[d][0];
            int nc=start.getCol()+direction[d][1];
            if (!isValid(board,nr,nc)) {
                continue;
            }
            if (!board.getCell(nr, nc).isEmpty()&&!(nr==end.getRow()&&nc==end.getCol())) {
                continue;
            }
            List<Position> path=new ArrayList<>();
            path.add(start);
            path.add(new Position(nr,nc));
            queue.offer(
                    new Node(new Position(nr, nc), d, 0, path)
            );
            visited[nr][nc][d]=true;
        }
        while (!queue.isEmpty()) {
            Node cur=queue.poll();
            Position p=cur.pos;

            if (p.equals(end)) {
                return cur.path;//能够连接就返回路径
            }
            for (int nd=0;nd<4;nd++) {
                int nr=p.getRow()+ direction[nd][0];
                int nc=p.getCol()+ direction[nd][1];

                if (!isValid(board, nr, nc)) {
                    continue;
                }
                int newTurns =cur.turns+(cur.dir == nd ? 0 : 1);
                if (newTurns>2) {
                    continue;
                }
                if (visited[nr][nc][nd]) {
                    continue;
                }
                if (!board.getCell(nr, nc).isEmpty()&&!(nr==end.getRow()&&nc==end.getCol())) {
                    continue;
                }
                visited[nr][nc][nd]=true;
                List<Position> newPath=new ArrayList<>(cur.path);
                newPath.add(new Position(nr, nc));
                queue.offer(
                        new Node(new Position(nr, nc), nd, newTurns, newPath)
                );
            }
        }
        return null;
    }
}