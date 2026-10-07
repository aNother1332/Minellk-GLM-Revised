package ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import model.AudioPlayer;
import model.Cell;
import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * JavaFX 棋盘视图：每个棋子是一个场景图节点（只有变化的节点会被重绘），
 * 连线/粒子/浮动文字绘制在顶层透明 Canvas 上，由单个 AnimationTimer 驱动。
 *
 * 消除流程（与原版节奏一致，特效更丰富）：
 *   显示黄色连线 → 连线淡出的同时棋子放大淡出 + 方块碎裂粒子 + 消除音效 → 通知逻辑层清空棋子。
 */
public class BoardView extends Pane {

    public interface ClickHandler {
        void onTileClick(int row, int col);

        void onEmptyClick();
    }

    private static final String BG_PATH = "resource/images/ruins.jpg";
    private static final double HOVER_SCALE = 1.15;
    private static final int LINE_SHOW_MS = 280;   // 连线停留时间（与原版一致）
    private static final int LINE_FADE_MS = 380;   // 连线淡出时间
    private static final int ELIMINATE_MS = 300;   // 棋子放大淡出时间（与原版一致）
    private static final double GRAVITY = 950;     // 粒子重力（像素/秒²）

    private GameBoard gameBoard;
    private int rows;
    private int cols;
    private double cellSize = 64;
    private double offsetX;
    private double offsetY;

    private final ImageView bgView = UIAssets.backgroundView(BG_PATH);
    private final Canvas gridCanvas = new Canvas(); // 静态网格线（含空格子的淡边框，与原版一致）
    private final Canvas fxCanvas = new Canvas();   // 连线 + 粒子 + 浮动文字
    private final List<Tile> tiles = new ArrayList<>();
    private final Map<Position, Tile> tileMap = new HashMap<>();

    private ClickHandler clickHandler;
    private boolean locked;

    private final AnimationTimer fxTimer;
    private long lastNanos;
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private final DoubleProperty lineAlpha = new SimpleDoubleProperty(1);
    private List<Position> linePath;
    private final Random random = new Random();

    public BoardView() {
        getChildren().addAll(bgView, gridCanvas);
        // 两个画布都只是绘制层，必须对鼠标透明，否则会盖住棋子拦截全部点击
        gridCanvas.setMouseTransparent(true);
        fxCanvas.setMouseTransparent(true);
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> {
            if (clickHandler != null) {
                clickHandler.onEmptyClick();
            }
        });
        fxTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = Math.min((now - lastNanos) / 1e9, 0.05);
                lastNanos = now;
                stepEffects(dt);
                renderOverlay();
                if (particles.isEmpty() && floatTexts.isEmpty() && linePath == null) {
                    stop();
                    fxCanvas.getGraphicsContext2D().clearRect(0, 0, fxCanvas.getWidth(), fxCanvas.getHeight());
                }
            }
        };
        widthProperty().addListener((o, was, now) -> layoutCells());
        heightProperty().addListener((o, was, now) -> layoutCells());
    }

    /** 棋子完全由 layoutCells 手工定位定尺寸，禁止父容器按首选尺寸重排（避免与 fitWidth 绑定形成反馈循环） */
    @Override
    protected void layoutChildren() {
        // no-op：布局由 layoutCells() 负责
    }

    public void setClickHandler(ClickHandler handler) {
        this.clickHandler = handler;
    }

    public boolean isLocked() {
        return locked;
    }

    /** 重建棋子节点（新开局 / 读取存档 / 重开时调用） */
    public void setGameBoard(GameBoard board) {
        this.gameBoard = board;
        this.rows = board.getRowCnt();
        this.cols = board.getColCnt();
        getChildren().removeAll(tiles);
        tiles.clear();
        tileMap.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                if (cell != null && !cell.isEmpty()) {
                    Tile tile = new Tile(cell);
                    tiles.add(tile);
                    tileMap.put(cell.getPos(), tile);
                    getChildren().add(tile);
                }
            }
        }
        // 特效层（连线/粒子/浮动文字）必须位于棋子之上且在场景图中
        if (!getChildren().contains(fxCanvas)) {
            getChildren().add(fxCanvas);
        }
        fxCanvas.toFront();
        stopEffects();
        layoutCells();
    }

    /** 按每个 Cell 的 isChosen 状态刷新选中边框 */
    public void refreshSelection() {
        for (Tile tile : tiles) {
            tile.updateFrame();
        }
    }

    /** 完整的消除序列：连线 → 特效 → 回调逻辑层清空棋子 */
    public void playMatch(Position a, Position b, List<Position> path, Runnable onTilesGone) {
        locked = true;
        linePath = new ArrayList<>(path);
        lineAlpha.set(1.0);
        ensureFxTimer();

        Timeline lineLife = new Timeline(
                new KeyFrame(Duration.millis(LINE_SHOW_MS)),
                new KeyFrame(Duration.millis(LINE_SHOW_MS + LINE_FADE_MS), new KeyValue(lineAlpha, 0.0)));
        lineLife.setOnFinished(e -> linePath = null);
        lineLife.play();

        PauseTransition strike = new PauseTransition(Duration.millis(LINE_SHOW_MS));
        strike.setOnFinished(e -> {
            burstAt(a);
            burstAt(b);
            AudioPlayer.hitsound();
            // 回调由两枚棋子的消除动画完成事件驱动（计数器），
            // 避免「隐藏棋子」与「补充新棋子」在同一帧竞争执行顺序
            int[] remaining = {2};
            Runnable one = () -> {
                if (--remaining[0] == 0) {
                    locked = false;
                    onTilesGone.run();
                }
            };
            Tile ta = tileMap.get(a);
            if (ta != null) {
                ta.eliminate(one);
            } else {
                one.run();
            }
            Tile tb = tileMap.get(b);
            if (tb != null) {
                tb.eliminate(one);
            } else {
                one.run();
            }
        });
        strike.play();
    }

    /** 道具消除：道具图标飞到方块处旋转一周，随后方块破坏（碎裂粒子 + 放大淡出） */
    public void playToolStrike(int row, int col, int tool, Runnable after) {
        locked = true;
        Position pos = new Position(row, col);
        Tile tile = tileMap.get(pos);

        // 道具使用特效：在方块中心旋转加速
        ImageView fx = new ImageView(UIAssets.image("resource/images/item" + tool + ".png", 52, 52));
        fx.setMouseTransparent(true);
        fx.relocate(centerX(pos) - 26, centerY(pos) - 26);
        getChildren().add(fx);
        fx.toFront();

        Timeline spin = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(fx.rotateProperty(), -30, Interpolator.EASE_IN),
                        new KeyValue(fx.scaleXProperty(), 0.8),
                        new KeyValue(fx.scaleYProperty(), 0.8)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(fx.rotateProperty(), 330, Interpolator.EASE_OUT),
                        new KeyValue(fx.scaleXProperty(), 1.3),
                        new KeyValue(fx.scaleYProperty(), 1.3)));
        spin.setOnFinished(e -> {
            // 方块破坏特效
            burstAt(pos);
            AudioPlayer.hitsound();
            FadeTransition fade = new FadeTransition(Duration.millis(160), fx);
            fade.setToValue(0);
            fade.setOnFinished(ev -> getChildren().remove(fx));
            fade.play();
            if (tile != null) {
                tile.eliminate(() -> {
                    locked = false;
                    after.run();
                });
            } else {
                locked = false;
                after.run();
            }
        });
        spin.play();
    }

    /** 无尽模式：在空位生成一个新棋子（缩放浮现）；若该位置曾有棋子则复用节点 */
    public void spawnTile(Cell cell) {
        Position pos = cell.getPos();
        Tile existing = tileMap.get(pos);
        if (existing != null) {
            existing.refreshIcon();
            existing.updateFrame(); // 同步选中框：补充的棋子不应继承上一次的选中状态
            existing.attachHover();
            popIn(existing);
            return;
        }
        Tile tile = new Tile(cell);
        tiles.add(tile);
        tileMap.put(pos, tile);
        getChildren().add(tile);
        tile.setOpacity(0);
        tile.setScaleX(0.2);
        tile.setScaleY(0.2);
        layoutCells();
        fxCanvas.toFront();
        popIn(tile);
    }

    private void popIn(javafx.scene.layout.StackPane node) {
        node.setVisible(true);
        node.setOpacity(0);
        node.setScaleX(0.2);
        node.setScaleY(0.2);
        Timeline pop = new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(node.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(node.scaleYProperty(), 1, Interpolator.EASE_OUT)));
        pop.play();
    }

    /** 两枚棋子无法连线时的红色闪烁反馈 */
    public void flashReject(Position a, Position b) {
        Tile ta = tileMap.get(a);
        Tile tb = tileMap.get(b);
        if (ta != null) {
            ta.showReject();
        }
        if (tb != null) {
            tb.showReject();
        }
    }

    /**
     * 洗牌动画：全部棋子被吸入棋盘中心旋转，随后分散回到各自位置。
     * onSwapped 在棋子聚拢时回调（此时应交换数据并刷新图标），
     * onDone 在全部分散归位后回调。动画期间棋盘保持锁定。
     */
    public void playShuffle(Runnable onSwapped, Runnable onDone) {
        locked = true;
        double centerX = offsetX + cols * cellSize / 2.0;
        double centerY = offsetY + rows * cellSize / 2.0;

        ParallelTransition gather = new ParallelTransition();
        List<javafx.scene.layout.StackPane> gathered = new ArrayList<>();
        for (Tile t : tiles) {
            if (!t.isVisible()) {
                continue;
            }
            gathered.add(t);
            double tx = centerX - (t.getLayoutX() + t.getWidth() / 2.0);
            double ty = centerY - (t.getLayoutY() + t.getHeight() / 2.0);
            Timeline move = new Timeline(new KeyFrame(Duration.millis(380),
                    new KeyValue(t.translateXProperty(), tx, Interpolator.EASE_BOTH),
                    new KeyValue(t.translateYProperty(), ty, Interpolator.EASE_BOTH),
                    new KeyValue(t.scaleXProperty(), 0.5, Interpolator.EASE_BOTH),
                    new KeyValue(t.scaleYProperty(), 0.5, Interpolator.EASE_BOTH),
                    new KeyValue(t.rotateProperty(), -120, Interpolator.EASE_BOTH)));
            gather.getChildren().add(move);
        }
        gather.setOnFinished(e -> {
            onSwapped.run();
            AudioPlayer.hitsound();
            ParallelTransition spin = new ParallelTransition();
            for (javafx.scene.layout.StackPane t : gathered) {
                Timeline rotate = new Timeline(new KeyFrame(Duration.millis(320),
                        new KeyValue(t.rotateProperty(), 240, Interpolator.EASE_BOTH)));
                spin.getChildren().add(rotate);
            }
            spin.setOnFinished(e2 -> {
                ParallelTransition scatter = new ParallelTransition();
                for (javafx.scene.layout.StackPane t : gathered) {
                    Timeline back = new Timeline(new KeyFrame(Duration.millis(440),
                            new KeyValue(t.translateXProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(t.translateYProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(t.scaleXProperty(), 1, Interpolator.EASE_BOTH),
                            new KeyValue(t.scaleYProperty(), 1, Interpolator.EASE_BOTH),
                            new KeyValue(t.rotateProperty(), 0, Interpolator.EASE_BOTH)));
                    scatter.getChildren().add(back);
                }
                scatter.setOnFinished(e3 -> {
                    locked = false;
                    if (onDone != null) {
                        onDone.run();
                    }
                });
                scatter.play();
            });
            spin.play();
        });
        gather.play();
    }

    /** 高亮一对可消除的棋子（提示道具）：蓝色呼吸描边约 2 秒 */
    public void hintPair(Position a, Position b) {
        Tile ta = tileMap.get(a);
        Tile tb = tileMap.get(b);
        if (ta != null) {
            ta.showHint();
        }
        if (tb != null) {
            tb.showHint();
        }
    }

    /** 洗牌后刷新所有棋子图标 */
    public void refreshIcons() {
        for (Tile tile : tiles) {
            tile.refreshIcon();
        }
    }

    /** 通关庆祝：全屏彩屑从顶部飘落（颜色取自当前棋盘与金色） */
    public void celebrate() {
        java.util.List<Color> palette = new ArrayList<>();
        palette.add(Color.web("#ffd83d"));
        palette.add(Color.web("#ff6b6b"));
        palette.add(Color.web("#4ecdc4"));
        palette.add(Color.web("#a78bfa"));
        palette.add(Color.web("#f9a8d4"));
        for (int i = 0; i < 130; i++) {
            Particle p = new Particle();
            p.x = random.nextDouble() * Math.max(getWidth(), 600);
            p.y = -random.nextDouble() * 500 - 20;
            p.vx = (random.nextDouble() - 0.5) * 120;
            p.vy = 140 + random.nextDouble() * 220;
            p.size = 4 + random.nextDouble() * 7;
            p.rot = random.nextDouble() * 360;
            p.vr = (random.nextDouble() - 0.5) * 540;
            p.maxLife = p.life = 1.6 + random.nextDouble() * 1.6;
            p.gravity = 260;
            p.color = palette.get(random.nextInt(palette.size())).deriveColor(0, 1.0, 0.85 + random.nextDouble() * 0.4, 1.0);
            particles.add(p);
        }
        ensureFxTimer();
    }

    /** 在两个棋子中点浮动显示加分文字 */
    public void showScorePopup(Position a, Position b, String text, Color color) {
        floatTexts.add(new FloatText(
                (centerX(a) + centerX(b)) / 2,
                (centerY(a) + centerY(b)) / 2 - cellSize * 0.4,
                text, color));
        ensureFxTimer();
    }

    // ———— 布局 ————

    private void layoutCells() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0 || cols == 0) {
            return;
        }
        // 正方形格子并在面板内居中（比原版的整块拉伸更美观）
        cellSize = Math.floor(Math.min(w / cols, h / rows));
        offsetX = (w - cellSize * cols) / 2;
        offsetY = (h - cellSize * rows) / 2;

        bgView.setFitWidth(w);
        bgView.setFitHeight(h);
        gridCanvas.setWidth(w);
        gridCanvas.setHeight(h);
        fxCanvas.setWidth(w);
        fxCanvas.setHeight(h);
        drawGrid();
        double tileEdge = cellSize - 4;
        for (Tile tile : tiles) {
            Position p = tile.cell.getPos();
            tile.relocate(offsetX + p.getCol() * cellSize + 2, offsetY + p.getRow() * cellSize + 2);
            tile.setPrefSize(tileEdge, tileEdge);
            tile.setMaxSize(tileEdge, tileEdge);
            tile.resize(tileEdge, tileEdge);
        }
    }

    private void drawGrid() {
        GraphicsContext g = gridCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());
        // 棋盘区域衬一层白色半透明玻璃板，让棋子从背景图中凸显出来
        g.setFill(Color.rgb(255, 255, 255, 0.42));
        g.fillRect(offsetX - 8, offsetY - 8, cellSize * cols + 16, cellSize * rows + 16);
        g.setStroke(Color.rgb(60, 60, 67, 0.20));
        g.setLineWidth(1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                g.strokeRect(offsetX + c * cellSize, offsetY + r * cellSize, cellSize, cellSize);
            }
        }
    }

    private double centerX(Position p) {
        return offsetX + (p.getCol() + 0.5) * cellSize;
    }

    private double centerY(Position p) {
        return offsetY + (p.getRow() + 0.5) * cellSize;
    }

    // ———— 特效 ————

    private void ensureFxTimer() {
        lastNanos = System.nanoTime();
        fxTimer.start();
    }

    private void stopEffects() {
        particles.clear();
        floatTexts.clear();
        linePath = null;
        fxTimer.stop();
        fxCanvas.getGraphicsContext2D().clearRect(0, 0, fxCanvas.getWidth(), fxCanvas.getHeight());
    }

    private void burstAt(Position p) {
        burstAt(p, 22);
    }

    /** Minecraft 方块破坏风格的方形粒子爆发，颜色取自方块图标主色调 */
    private void burstAt(Position p, int count) {
        Tile tile = tileMap.get(p);
        int icon = tile != null ? tile.cell.getNumber() : 0;
        Color base = icon > 0 ? UIAssets.palette(icon)[0] : Color.SANDYBROWN;
        double cx = centerX(p);
        double cy = centerY(p);
        for (int i = 0; i < count; i++) {
            Particle particle = new Particle();
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 70 + random.nextDouble() * 230;
            particle.vx = Math.cos(angle) * speed;
            particle.vy = Math.sin(angle) * speed - 150; // 整体向上喷发再受重力下落
            particle.x = cx + (random.nextDouble() - 0.5) * cellSize * 0.5;
            particle.y = cy + (random.nextDouble() - 0.5) * cellSize * 0.5;
            particle.size = 4 + random.nextDouble() * 6;
            particle.rot = random.nextDouble() * 360;
            particle.vr = (random.nextDouble() - 0.5) * 720;
            particle.maxLife = particle.life = 0.55 + random.nextDouble() * 0.45;
            particle.color = base.deriveColor(0, 1.0, 0.65 + random.nextDouble() * 0.7, 1.0);
            particles.add(particle);
        }
        ensureFxTimer();
    }

    private void stepEffects(double dt) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.vy += p.gravity * dt;
            p.vx *= (1 - 1.2 * dt); // 空气阻力
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.vr * dt;
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
            }
        }
        for (Iterator<FloatText> it = floatTexts.iterator(); it.hasNext(); ) {
            FloatText t = it.next();
            t.y -= 42 * dt;
            t.life -= dt;
            if (t.life <= 0) {
                it.remove();
            }
        }
    }

    private void renderOverlay() {
        GraphicsContext g = fxCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, fxCanvas.getWidth(), fxCanvas.getHeight());
        // 重置上一帧遗留的全局透明度（浮动文字会调低它，若不重置连线会越画越淡）
        g.setGlobalAlpha(1.0);
        if (linePath != null) {
            drawConnection(g);
        }
        for (Particle p : particles) {
            double alpha = p.life / p.maxLife;
            g.save();
            g.translate(p.x, p.y);
            g.rotate(p.rot);
            g.setGlobalAlpha(alpha);
            g.setFill(p.color);
            double size = p.size * (0.5 + 0.5 * alpha);
            g.fillRect(-size / 2, -size / 2, size, size);
            g.restore();
        }
        for (FloatText t : floatTexts) {
            double alpha = t.life / t.maxLife;
            g.save();
            g.setGlobalAlpha(alpha);
            g.setFont(UIAssets.fontBold(20));
            g.setTextAlign(TextAlignment.CENTER);
            g.setStroke(Color.rgb(0, 0, 0, 0.7));
            g.setLineWidth(3);
            g.strokeText(t.text, t.x, t.y);
            g.setFill(t.color);
            g.fillText(t.text, t.x, t.y);
            g.restore();
        }
    }

    private void drawConnection(GraphicsContext g) {
        if (linePath.size() < 2) {
            return;
        }
        double alpha = lineAlpha.get();
        if (alpha <= 0) {
            return;
        }
        double[] xs = new double[linePath.size()];
        double[] ys = new double[linePath.size()];
        for (int i = 0; i < linePath.size(); i++) {
            xs[i] = centerX(linePath.get(i));
            ys[i] = centerY(linePath.get(i));
        }
        g.setLineCap(StrokeLineCap.ROUND);
        g.setStroke(Color.rgb(255, 255, 0, 0.35 * alpha));
        g.setLineWidth(11);
        g.strokePolyline(xs, ys, xs.length);
        g.setStroke(Color.rgb(255, 255, 90, 0.95 * alpha));
        g.setLineWidth(4.5);
        g.strokePolyline(xs, ys, xs.length);
    }

    // ———— 棋子节点 ————

    private class Tile extends StackPane {
        final Cell cell;
        final ImageView icon;
        final Rectangle frame = new Rectangle();
        private Rectangle hintRect;
        private Rectangle rejectRect;

        Tile(Cell cell) {
            this.cell = cell;
            icon = new ImageView(UIAssets.tileIcon(cell.getNumber()));
            icon.setPreserveRatio(false);
            icon.fitWidthProperty().bind(widthProperty());
            icon.fitHeightProperty().bind(heightProperty());
            icon.setMouseTransparent(true);

            frame.setFill(null);
            frame.setStroke(Color.RED);
            frame.setStrokeWidth(3.5);
            frame.setMouseTransparent(true);
            frame.widthProperty().bind(widthProperty());
            frame.heightProperty().bind(heightProperty());
            frame.setVisible(cell.getIsChosen());

            getChildren().addAll(icon, frame);
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> {
                e.consume();
                // 未开局时也要转发，由 GameScreen 弹出「请先点击开始游戏」提示（与原版一致）
                if (clickHandler != null && !locked) {
                    clickHandler.onTileClick(cell.getPos().getRow(), cell.getPos().getCol());
                }
            });
            setOnMouseEntered(e -> {
                if (!locked) {
                    growTo(HOVER_SCALE);
                }
            });
            setOnMouseExited(e -> {
                if (!locked) {
                    growTo(1.0);
                }
            });
        }

        /** 悬停放大效果（消除动画期间会临时摘除，补充复用后需重新挂载） */
        void attachHover() {
            setOnMouseEntered(e -> {
                if (!locked) {
                    growTo(HOVER_SCALE);
                }
            });
            setOnMouseExited(e -> {
                if (!locked) growTo(1.0);
            });
        }

        void detachHover() {
            setOnMouseEntered(null);
            setOnMouseExited(null);
        }

        void updateFrame() {
            frame.setVisible(cell.getIsChosen());
        }

        /** 无法连线时的红色闪烁反馈 */
        void showReject() {
            if (rejectRect != null) {
                getChildren().remove(rejectRect);
            }
            rejectRect = new Rectangle();
            rejectRect.setManaged(false);
            rejectRect.setMouseTransparent(true);
            rejectRect.setFill(null);
            rejectRect.setStroke(UiTheme.DANGER);
            rejectRect.setStrokeWidth(3.5);
            rejectRect.setArcWidth(8);
            rejectRect.setArcHeight(8);
            rejectRect.widthProperty().bind(widthProperty());
            rejectRect.heightProperty().bind(heightProperty());
            getChildren().add(rejectRect);
            Timeline fade = new Timeline(new KeyFrame(Duration.millis(300),
                    new KeyValue(rejectRect.opacityProperty(), 0)));
            fade.setOnFinished(e -> {
                getChildren().remove(rejectRect);
                rejectRect = null;
            });
            fade.play();
        }

        /** 洗牌后按新的图标编号重设纹理 */
        void refreshIcon() {
            icon.setImage(UIAssets.tileIcon(cell.getNumber()));
        }

        /** 提示高亮：蓝色呼吸描边，2 秒后移除 */
        void showHint() {
            if (hintRect != null) {
                getChildren().remove(hintRect);
            }
            hintRect = new Rectangle();
            hintRect.setManaged(false);
            hintRect.setMouseTransparent(true);
            hintRect.setFill(null);
            hintRect.setStroke(Color.web("#0a84ff"));
            hintRect.setStrokeWidth(3.5);
            hintRect.setArcWidth(8);
            hintRect.setArcHeight(8);
            hintRect.widthProperty().bind(widthProperty());
            hintRect.heightProperty().bind(heightProperty());
            getChildren().add(hintRect);
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hintRect.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(330), new KeyValue(hintRect.opacityProperty(), 0.25)),
                    new KeyFrame(Duration.millis(660), new KeyValue(hintRect.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(990), new KeyValue(hintRect.opacityProperty(), 0.25)),
                    new KeyFrame(Duration.millis(1320), new KeyValue(hintRect.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(1900), new KeyValue(hintRect.opacityProperty(), 0)));
            pulse.setOnFinished(e -> {
                getChildren().remove(hintRect);
                hintRect = null;
            });
            pulse.play();
        }

        private void growTo(double scale) {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), this);
            st.setToX(scale);
            st.setToY(scale);
            st.playFromStart();
        }

        /** 放大淡出（与原版消除动画一致，改为节点属性动画，由 Prism 渲染线程插值） */
        void eliminate(Runnable after) {
            detachHover();
            setScaleX(1.0);
            setScaleY(1.0);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(scaleXProperty(), 1.0),
                            new KeyValue(scaleYProperty(), 1.0),
                            new KeyValue(opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(ELIMINATE_MS),
                            new KeyValue(scaleXProperty(), 1.5, Interpolator.EASE_OUT),
                            new KeyValue(scaleYProperty(), 1.5, Interpolator.EASE_OUT),
                            new KeyValue(opacityProperty(), 0.0, Interpolator.EASE_OUT)));
            timeline.setOnFinished(e -> {
                setVisible(false);
                if (after != null) {
                    after.run();
                }
            });
            timeline.play();
        }
    }

    private static class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double size;
        double rot;
        double vr;
        double life;
        double maxLife;
        double gravity = GRAVITY; // 逐粒子重力（庆祝彩屑较轻）
        Color color;
    }

    private static class FloatText {
        double x;
        double y;
        String text;
        Color color;
        final double maxLife = 0.9;
        double life = 0.9;

        FloatText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }
    }
}
