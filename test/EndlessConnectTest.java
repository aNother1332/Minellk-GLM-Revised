import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AudioPlayer;
import model.Cell;
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import ui.GameScreen;
import ui.ScreenManager;
import utils.Utils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 开发用诊断：无尽模式多轮消除 + 补充后，检查
 * (a) 模型与视图一致性（非空格子必须有可见棋子且图标匹配；空格子不能有可见棋子）
 * (b) 相邻同色棋子必须可连
 * (c) 同行/同列中间仅隔一个空格的同色棋子必须可连（用户报告的场景）
 */
public class EndlessConnectTest extends Application {

    private GameScreen game;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ScreenManager.init(stage);
        ScreenManager.showGame(GameDifficulty.ENDLESS);
        stage.setX(60);
        stage.setY(60);
        stage.show();
        AudioPlayer.backgroundsound();
        Field f = ScreenManager.class.getDeclaredField("current");
        f.setAccessible(true);
        game = (GameScreen) f.get(null);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(0.8), e -> button("开始游戏").fire()),
                new KeyFrame(Duration.seconds(1.2), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(2.6), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(4.0), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(5.4), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(6.8), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(8.2), e -> eliminateOnePair()),
                new KeyFrame(Duration.seconds(9.6), e -> fullAudit()),
                new KeyFrame(Duration.seconds(10.0), e -> Platform.exit()));
        tl.play();
    }

    private void eliminateOnePair() {
        GameBoard board = board();
        List<Position> cells = board.getcellpositions();
        outer:
        for (Position a : cells) {
            for (Position b : cells) {
                if (!a.equals(b) && Utils.findPath(board, a, b) != null) {
                    fireTileClick(tileOf(a));
                    fireTileClick(tileOf(b));
                    System.out.println("[test] eliminated " + a + " -> " + b);
                    break outer;
                }
            }
        }
    }

    /** 全面体检 */
    private void fullAudit() {
        GameBoard board = board();
        int rows = board.getRowCnt();
        int cols = board.getColCnt();
        int problems = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell == null) {
                    continue;
                }
                Node tile = tileOf(cell.getPos());
                boolean visible = tile != null && tile.isVisible();
                if (cell.isEmpty() && visible) {
                    System.out.println("[audit] FAIL 空格子(" + r + "," + c + ") 却有可见棋子");
                    problems++;
                }
                if (!cell.isEmpty() && !visible) {
                    System.out.println("[audit] FAIL 非空格子(" + r + "," + c + ") 棋子不可见! number=" + cell.getNumber());
                    problems++;
                }
                if (!cell.isEmpty() && visible && tile instanceof javafx.scene.layout.StackPane sp) {
                    for (Node child : sp.getChildren()) {
                        if (child instanceof javafx.scene.image.ImageView iv) {
                            var want = ui.UIAssets.tileIcon(cell.getNumber());
                            if (iv.getImage() != want) {
                                System.out.println("[audit] FAIL 格子(" + r + "," + c + ") 图标与编号不符! number=" + cell.getNumber());
                                problems++;
                            }
                        }
                    }
                }
            }
        }

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell == null || cell.isEmpty()) {
                    continue;
                }
                for (int[] d : dirs) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                        continue;
                    }
                    Cell other = board.getCell(nr, nc);
                    if (other != null && !other.isEmpty() && other.getNumber() == cell.getNumber()
                            && Utils.findPath(board, cell.getPos(), other.getPos()) == null) {
                        System.out.println("[audit] FAIL 相邻同色 (" + r + "," + c + ")-(" + nr + "," + nc + ") 无法连线!");
                        problems++;
                    }
                    int mr = r + d[0], mc = c + d[1];
                    int er = r + 2 * d[0], ec = c + 2 * d[1];
                    if (er < 0 || er >= rows || ec < 0 || ec >= cols) {
                        continue;
                    }
                    Cell mid = board.getCell(mr, mc);
                    Cell end = board.getCell(er, ec);
                    if (mid != null && mid.isEmpty() && end != null && !end.isEmpty()
                            && end.getNumber() == cell.getNumber()
                            && Utils.findPath(board, cell.getPos(), end.getPos()) == null) {
                        System.out.println("[audit] FAIL 隔空同色 (" + r + "," + c + ")-(" + er + "," + ec + ") 无法连线!");
                        problems++;
                    }
                }
            }
        }
        System.out.println("[audit] 完成，问题数=" + problems + "，剩余棋子=" + board.getcellpositions().size());
    }

    private void fireTileClick(Node tile) {
        Point2D center = new Point2D(tile.getLayoutBounds().getWidth() / 2,
                tile.getLayoutBounds().getHeight() / 2);
        PickResult pick = new PickResult(tile, center.getX(), center.getY());
        MouseEvent click = new MouseEvent(MouseEvent.MOUSE_CLICKED, center.getX(), center.getY(), 0, 0,
                MouseButton.PRIMARY, 1, false, false, false, false, false, true, false, false, false, false, pick);
        javafx.event.Event.fireEvent(tile, click);
    }

    @SuppressWarnings("unchecked")
    private Node tileOf(Position position) {
        try {
            Field f = ui.BoardView.class.getDeclaredField("tileMap");
            f.setAccessible(true);
            return ((java.util.Map<Position, Node>) f.get(boardView())).get(position);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ui.BoardView boardView() {
        try {
            Field f = GameScreen.class.getDeclaredField("boardView");
            f.setAccessible(true);
            return (ui.BoardView) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GameBoard board() {
        try {
            Field f = GameScreen.class.getDeclaredField("gameBoard");
            f.setAccessible(true);
            return (GameBoard) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Button button(String text) {
        for (Node n : game.scene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText()) && !b.isDisabled()) {
                return b;
            }
        }
        throw new IllegalStateException("button not found: " + text);
    }
}
