package ui;

import app.UserSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import model.AudioPlayer;
import model.BoardGenerator;
import model.Cell;
import model.GameBoard;
import model.GameDifficulty;
import model.Position;
import model.Record;
import model.RecordStore;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏主界面：整合原 Swing 版 GameFrame / StatusPanel / ControlPanel / ItemPanel 的全部逻辑。
 * 计分与连击规则、道具规则、存档时机均与原版保持一致。
 */
public class GameScreen implements Screen {

    // 面板/槽位样式统一取自 UiTheme

    private final GameDifficulty difficulty;
    private final Cell[][] originalBoard; // 开局深拷贝，重开时恢复
    private final int totalTime;
    private final int initialPairs;

    private GameBoard gameBoard;
    private final BoardView boardView = new BoardView();

    private final Label timeLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label pairsLabel = new Label();
    private final Label toast = new Label();
    private AppButton startButton;
    private AppButton restartButton;
    private AppButton pauseButton;
    private final java.util.List<AppButton> instantItemButtons = new ArrayList<>();
    private final Button[] toolButtons = new Button[3];
    private final Label toolPointsLabel = new Label();
    private final ImageView toolCursor = new ImageView(); // 跟随鼠标的道具图标
    private double lastMouseX;
    private double lastMouseY;

    private Timeline timer;
    private int remainingTime;
    private final model.GameSession session = new model.GameSession(2);
    private int eliminated;
    private int chosenTool; // 0=未选择 1=钻石镐 2=钻石斧 3=钻石铲
    private boolean gameStarted;
    private boolean paused;
    private final boolean endless;
    private final java.util.Random random = new java.util.Random();
    private Label pairsCaption;

    private final Scene scene;
    private SequentialTransition toastAnim;
    private Cell firstSelection;

    /** 创建新游戏 */
    public GameScreen(GameDifficulty difficulty) {
        this.difficulty = difficulty;
        this.endless = difficulty == GameDifficulty.ENDLESS;
        this.originalBoard = BoardGenerator.deepCopy(BoardGenerator.generate(difficulty));
        this.totalTime = BoardGenerator.totalTime(difficulty);
        this.initialPairs = BoardGenerator.initialPairs(difficulty);
        this.remainingTime = totalTime;
        this.scene = buildScene();
        resetGame();
    }

    /** 从存档恢复游戏 */
    public GameScreen(Record record) {
        this.difficulty = record.getLevel() == 1 ? GameDifficulty.EASY
                : record.getLevel() == 3 ? GameDifficulty.ENDLESS : GameDifficulty.HARD;
        this.endless = difficulty == GameDifficulty.ENDLESS;
        this.originalBoard = BoardGenerator.deepCopy(record.getBoard());
        this.totalTime = record.getLefttime(); // 与原版一致：重开后恢复到存档时刻的剩余时间
        this.initialPairs = BoardGenerator.initialPairs(difficulty);
        this.remainingTime = record.getLefttime();
        this.session.reset(record.getPoints());
        this.session.restoreScore(record.getScore());
        this.eliminated = (initialPairs * 2 - record.getRemaincount()) / 2;
        this.scene = buildScene();

        Cell[][] saved = record.getBoard();
        this.gameBoard = new GameBoard(saved.length, saved[0].length, saved);
        boardView.setGameBoard(gameBoard);
        statusLabel.setText("未开始");
        updateTimeLabel();
        scoreLabel.setText(String.valueOf(session.getScore()));
        updateToolLabel();
        updatePairsLabel();
        setToolsEnabled(false);
    }

    // ———— 界面构建 ————

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setTop(buildStatusBar());
        root.setCenter(boardView);
        root.setRight(buildItemPanel());
        root.setBottom(buildControlBar());

        boardView.setClickHandler(new BoardView.ClickHandler() {
            @Override
            public void onTileClick(int row, int col) {
                handleTileClick(row, col);
            }

            @Override
            public void onEmptyClick() {
                // 点到棋盘空白处不做处理：已选中的道具保持跟随鼠标，直到被使用或收回
            }
        });

        // 顶层覆盖层：提示横幅 + 跟随鼠标的道具（都不拦截棋盘点击）
        toast.setFont(UIAssets.fontBold(15));
        toast.setTextFill(UiTheme.TEXT_MAIN);
        toast.setStyle(UiTheme.toastStyle());
        toast.setTextAlignment(TextAlignment.CENTER);
        toast.setVisible(false);
        toast.setMouseTransparent(true);
        toolCursor.setMouseTransparent(true);
        // 关键：脱离布局管理，否则每次布局都会把图标摆回容器中心，覆盖跟随鼠标的坐标
        toolCursor.setManaged(false);
        toolCursor.setVisible(false);
        toolCursor.setFitWidth(44);
        toolCursor.setFitHeight(44);
        toolCursor.setRotate(-25);

        StackPane layers = new StackPane(root, toast, toolCursor);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(70, 0, 0, 0));
        // 键盘支持：ESC 关闭弹窗/呼出暂停，Enter 触发弹窗默认按钮
        layers.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ESCAPE -> {
                    if (PixelDialog.closeTop(scene.getWindow())) {
                        e.consume();
                    } else if (gameStarted && !paused && !boardView.isLocked()) {
                        openPause();
                        e.consume();
                    }
                }
                case ENTER -> {
                    if (PixelDialog.confirmTop(scene.getWindow())) {
                        e.consume();
                    }
                }
                default -> { }
            }
        });
        layers.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            if (chosenTool != 0) {
                toolCursor.relocate(e.getX() + 12, e.getY() + 10);
            }
        });
        return new Scene(layers, 1000, 900);
    }

    private Pane buildStatusBar() {
        StackPane bar = new StackPane();
        bar.setStyle(UiTheme.hudBarStyle());
        bar.setPrefHeight(58);

        pairsCaption = UiTheme.label(endless ? "消除对数" : "剩余对数", 11, UiTheme.TEXT_SECONDARY);
        pairsLabel.setFont(UiTheme.fontSemi(17));
        pairsLabel.setTextFill(UiTheme.TEXT_MAIN);

        VBox pairsBox = new VBox(2, pairsCaption, pairsLabel);
        pairsBox.setAlignment(Pos.CENTER);
        pairsBox.setMinWidth(120);

        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                stat("剩余时间", timeLabel),
                UiTheme.vline(),
                stat("状态", statusLabel),
                UiTheme.vline(),
                stat("分数", scoreLabel),
                UiTheme.vline(),
                pairsBox);
        bar.getChildren().addAll(stats);
        return bar;
    }

    /** 状态栏统计格：金色小标题 + 白色数值 */
    private VBox stat(String name, Label value) {
        value.setFont(UIAssets.fontBold(17));
        value.setTextFill(UiTheme.TEXT_MAIN);
        Label caption = UiTheme.label(name, 12, UiTheme.TEXT_SECONDARY);
        VBox box = new VBox(2, caption, value);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(120);
        return box;
    }

    private Pane buildItemPanel() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(132);
        panel.setPadding(new Insets(14, 10, 14, 10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle(UiTheme.hudItemStyle());

        panel.getChildren().add(UiTheme.label("道具", 15, UiTheme.TEXT_MAIN));
        panel.getChildren().add(UiTheme.hline());

        toolPointsLabel.setFont(UIAssets.fontBold(20));
        toolPointsLabel.setTextFill(UiTheme.TEXT_MAIN);
        toolPointsLabel.setWrapText(true);

        String[] tooltips = {"钻石镐", "钻石斧", "钻石铲"};
        for (int i = 0; i < 3; i++) {
            ImageView icon = new ImageView(UIAssets.image("resource/images/item" + (i + 1) + ".png", 40, 40));
            Button button = new Button("", icon);
            button.setPadding(new Insets(4));
            button.setCursor(Cursor.HAND);
            button.setFocusTraversable(false);
            button.setTooltip(new Tooltip(tooltips[i]));
            button.setDisable(true);
            button.disabledProperty().addListener((o, was, now) -> button.setOpacity(now ? 0.4 : 1.0));
            final int tool = i + 1;
            button.setOnAction(e -> armTool(tool));
            toolButtons[i] = button;
        }
        refreshToolStyles();

        Region gap = new Region();
        gap.setPrefHeight(2);

        AppButton hintBtn = miniItemButton("提示", () -> useHintItem());
        AppButton shuffleBtn = miniItemButton("洗牌", () -> useShuffleItem());
        AppButton timeBtn = miniItemButton("+10秒", () -> useTimeItem());

        Label hint = UiTheme.label("上方道具点击方块使用，下方道具立即生效", 10, UiTheme.TEXT_SECONDARY);
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);

        panel.getChildren().addAll(toolPointsLabel,
                toolButtons[0], toolButtons[1], toolButtons[2],
                gap, hintBtn, shuffleBtn, timeBtn, hint);
        instantItemButtons.clear();
        instantItemButtons.addAll(java.util.List.of(hintBtn, shuffleBtn, timeBtn));
        return panel;
    }

    private Pane buildControlBar() {
        HBox bar = new HBox(24);
        bar.setPrefHeight(64);
        bar.setAlignment(Pos.CENTER);
        bar.setStyle(UiTheme.hudBarStyle());
        bar.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.web("#3C3C43", 0.16),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0))));

        startButton = new AppButton("开始游戏", 140, 44, 16, AppButton.Variant.PRIMARY);
        restartButton = new AppButton("重新开始", 140, 44, 16, AppButton.Variant.TINTED);
        pauseButton = new AppButton("暂停", 110, 44, 16, AppButton.Variant.TINTED);
        restartButton.setDisable(true);
        pauseButton.setDisable(true);
        startButton.setOnAction(e -> startGame());
        pauseButton.setOnAction(e -> openPause());
        restartButton.setOnAction(e -> {
            if (gameStarted && !paused) {
                stopTimer(); // 确认期间暂停计时
            }
            PixelDialog.choice(scene.getWindow(),
                    "确定要重新开始游戏吗？", 340,
                    new String[]{"确定", "取消"}, choice -> {
                        if (choice == 0) {
                            resetGame();
                        } else if (gameStarted && timer != null) {
                            timer.play(); // 取消则继续计时
                        }
                    });
        });
        bar.getChildren().addAll(startButton, restartButton, pauseButton);
        return bar;
    }

    // ———— 游戏流程 ————

    private void startGame() {
        if (gameStarted) {
            return;
        }
        gameStarted = true;
        statusLabel.setText("游戏中");
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        setToolsEnabled(true);
        startButton.setDisable(true);
        restartButton.setDisable(false);
        pauseButton.setDisable(false);
    }

    /** 游戏内暂停：停止计时并弹出暂停卡片 */
    private void openPause() {
        if (!gameStarted || paused || boardView.isLocked()) {
            return;
        }
        paused = true;
        stopTimer();
        pauseButton.setDisable(true);
        PixelDialog.showCard(scene.getWindow(), 300, true, card -> {
            card.onClosed(this::resumeIfPaused);
            VBox p = card.panel();
            p.getChildren().addAll(
                    UiTheme.label("游戏暂停", 20, UiTheme.TEXT_MAIN),
                    UiTheme.hline(),
                    pauseCardButton("继续游戏", AppButton.Variant.PRIMARY, card::close),
                    pauseCardButton("重新开始", AppButton.Variant.TINTED,
                            () -> card.closeThen(this::resetGame)),
                    pauseCardButton("返回主界面", AppButton.Variant.TINTED,
                            () -> card.closeThen(() -> ScreenManager.showMainMenu())));
        });
    }

    /** 即时生效型道具的小按钮 */
    private AppButton miniItemButton(String text, Runnable action) {
        AppButton button = new AppButton(text, 96, 34, 13, AppButton.Variant.TINTED);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setDisable(true);
        button.disabledProperty().addListener((o, was, now) -> button.setOpacity(now ? 0.4 : 1.0));
        button.setOnAction(e -> action.run());
        return button;
    }

    private AppButton pauseCardButton(String text, AppButton.Variant variant, Runnable action) {
        AppButton button = new AppButton(text, 220, 42, 15, variant);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }

    private void resumeIfPaused() {
        if (paused && gameStarted && timer != null) {
            timer.play();
        }
        paused = false;
        pauseButton.setDisable(!gameStarted);
    }

    private void tick() {
        if (remainingTime > 0) {
            remainingTime--;
            updateTimeLabel();
        } else {
            stopTimer();
            statusLabel.setText("时间到！");
            Platform.runLater(() -> defeat("时间耗尽！"));
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void handleTileClick(int row, int col) {
        if (!gameStarted) {
            PixelDialog.info(scene.getWindow(), "请先点击【开始游戏】按钮！", 380);
            return;
        }
        if (boardView.isLocked()) {
            return;
        }
        Cell cell = gameBoard.getCell(row, col);
        if (cell == null || cell.isEmpty()) {
            return;
        }

        if (chosenTool != 0) {
            applyTool(cell);
            return;
        }

        // ———— 常规两两消除 ————
        if (firstSelection == null) {
            gameBoard.clearchosen();
            cell.setChosen(true);
            firstSelection = cell;
            boardView.refreshSelection();
            return;
        }
        if (firstSelection == cell) {
            cell.setChosen(false);
            firstSelection = null;
            boardView.refreshSelection();
            return;
        }
        Cell a = firstSelection;
        Cell b = cell;
        b.setChosen(true);
        boardView.refreshSelection();

        List<Position> path = Utils.findPath(gameBoard, a.getPos(), b.getPos());
        if (path == null) {
            boardView.flashReject(a.getPos(), b.getPos()); // 红色闪烁：无法连线
            // 与原版一致：不能消除时，第二次点击变为新的第一次选中
            gameBoard.clearchosen();
            b.setChosen(true);
            firstSelection = b;
            boardView.refreshSelection();
            return;
        }
        firstSelection = null;
        int iconNumber = a.getNumber();
        Position pa = a.getPos();
        Position pb = b.getPos();
        boardView.playMatch(pa, pb, path, () -> {
            a.setempty(true);
            b.setempty(true);
            a.setChosen(false);
            b.setChosen(false);
            eliminated++;
            int pts = addscore(iconNumber);
            boolean combo = session.comboOf(iconNumber) >= 3;
            boardView.showScorePopup(pa, pb, "+" + pts, combo ? Color.GOLD : Color.WHITE);
            checkAfterElimination(pa, pb);
        });
    }

    /** 提示道具：先确认存在可消除对，再扣点并高亮 */
    private void useHintItem() {
        if (!itemGuard()) {
            return;
        }
        Position[] pair = findHintPair();
        if (pair == null) {
            showToast("当前没有可消除的棋子对");
            return;
        }
        session.spendItemPoint();
        updateToolLabel();
        boardView.hintPair(pair[0], pair[1]);
        showToast("已高亮一对可消除的棋子");
    }

    /** 洗牌道具：动画期间暂停计时，棋子吸入中心旋转后分散到新排布 */
    private void useShuffleItem() {
        if (!itemGuard()) {
            return;
        }
        session.spendItemPoint();
        updateToolLabel();
        boardView.playShuffle(
                () -> {
                    BoardGenerator.shuffleNumbers(gameBoard);
                    boardView.refreshIcons();
                    boardView.refreshSelection();
                    firstSelection = null;
                },
                () -> showToast("棋盘已洗牌"));
    }

    /** 时间道具：剩余时间 +10 秒 */
    private void useTimeItem() {
        if (!itemGuard()) {
            return;
        }
        session.spendItemPoint();
        updateToolLabel();
        remainingTime += 10;
        updateTimeLabel();
        showToast("剩余时间 +10 秒");
    }

    /** 即时道具的前置检查（不扣点）：开局、非动画中、非暂停、未选中鼠标道具 */
    private boolean itemGuard() {
        if (!gameStarted) {
            PixelDialog.info(scene.getWindow(), "请先点击【开始游戏】按钮！", 380);
            return false;
        }
        if (boardView.isLocked() || paused) {
            return false;
        }
        if (chosenTool != 0) {
            showToast("请先按 ESC 收回当前道具");
            return false;
        }
        return true;
    }

    /** 找一对可消除的棋子（按图标分组后在组内两两试连） */
    private Position[] findHintPair() {
        Map<Integer, List<Position>> groups = new HashMap<>();
        for (Position p : gameBoard.getcellpositions()) {
            Cell cell = gameBoard.getCell(p.getRow(), p.getCol());
            groups.computeIfAbsent(cell.getNumber(), k -> new ArrayList<>()).add(p);
        }
        for (List<Position> group : groups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    if (Utils.findPath(gameBoard, group.get(i), group.get(j)) != null) {
                        return new Position[]{group.get(i), group.get(j)};
                    }
                }
            }
        }
        return null;
    }

    /** 使用道具消除单个棋子（类别规则与原版一致；点数在实际消除时才扣除） */
    private void applyTool(Cell cell) {
        int tool = chosenTool;
        int number = cell.getNumber();
        String name = BlockCatalog.name(number);
        String toolLabel = toolName(tool);
        boolean valid = switch (tool) {
            case 1 -> number == 1 || number == 4 || number == 7 || number == 10;
            case 2 -> number == 2 || number == 5 || number == 8 || number == 11;
            default -> number == 3 || number == 6 || number == 9 || number == 12;
        };
        // 清掉可能残留的配对选择，避免悬挂的红色选中框
        if (firstSelection != null) {
            firstSelection.setChosen(false);
            firstSelection = null;
            boardView.refreshSelection();
        }
        if (!valid) {
            wiggleToolCursor();
            showToast(toolLabel + "对" + name + "无效！（" + toolLabel + "只能消除" + toolCategory(tool) + "）");
            return; // 道具保持选中，可继续点击其他方块
        }
        session.spendItemPoint(); // 点数在实际消除时扣除
        updateToolLabel();
        chosenTool = 0;
        toolCursor.setVisible(false);
        refreshToolStyles();
        Position pos = cell.getPos();
        boardView.playToolStrike(pos.getRow(), pos.getCol(), tool, () -> {
            cell.setempty(true);
            cell.setChosen(false);
            int pts = addscore(10); // 与原版一致：道具消除按 +10 分计
            boardView.showScorePopup(pos, pos, "+" + pts, Color.WHITE);
            showToast("使用" + toolLabel + "消除了" + name + "×1");
            // 原版道具消除后不检查胜负；这里补上，避免用道具清空棋盘后游戏卡住
            checkAfterElimination(pos);
        });
    }

    /** 选中/收回道具：选中后道具图标跟随鼠标，再次点击同一个道具收回（点数在实际使用时才扣除） */
    private void armTool(int tool) {
        if (chosenTool == tool) {
            chosenTool = 0;
            toolCursor.setVisible(false);
            refreshToolStyles();
            return;
        }
        chosenTool = tool;
        toolCursor.setImage(UIAssets.image("resource/images/item" + tool + ".png", 44, 44));
        toolCursor.relocate(lastMouseX + 12, lastMouseY + 10);
        toolCursor.setVisible(true);
        refreshToolStyles();
        showToast("已选择" + toolName(tool) + "，点击" + toolCategory(tool) + "方块使用");
    }

    private static String toolName(int tool) {
        return switch (tool) {
            case 1 -> "钻石镐";
            case 2 -> "钻石斧";
            default -> "钻石铲";
        };
    }

    private static String toolCategory(int tool) {
        return switch (tool) {
            case 1 -> "矿石类";
            case 2 -> "木制类";
            default -> "土质类";
        };
    }

    /** 无效点击时道具图标左右摆动提示 */
    private void wiggleToolCursor() {
        Timeline wiggle = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(toolCursor.rotateProperty(), -25)),
                new KeyFrame(Duration.millis(60), new KeyValue(toolCursor.rotateProperty(), -48)),
                new KeyFrame(Duration.millis(130), new KeyValue(toolCursor.rotateProperty(), -2)),
                new KeyFrame(Duration.millis(210), new KeyValue(toolCursor.rotateProperty(), -25)));
        wiggle.play();
    }

    /** 道具按钮外观：选中的道具高亮显示 */
    private void refreshToolStyles() {
        for (int i = 0; i < toolButtons.length; i++) {
            boolean armed = chosenTool == i + 1;
            toolButtons[i].setStyle(armed ? UiTheme.slotArmedStyle() : UiTheme.slotStyle());
        }
    }

    /** 计分与连击（规则在 GameSession 中），返回本次得分 */
    private int addscore(int iconNumber) {
        int pts = session.addPairScore(iconNumber);
        int combo = session.comboOf(iconNumber);
        if (combo >= 3) {
            updateToolLabel();
        }
        scoreLabel.setText(String.valueOf(session.getScore()));

        String message = String.format("消除了[%s]×2，+%d分", BlockCatalog.name(iconNumber), pts);
        if (combo >= 3) {
            AudioPlayer.combosound();
            message += String.format("（连消×%d，额外加分）", combo);
        }
        showToast(message);
        updatePairsLabel();
        return pts;
    }

    private void checkAfterElimination(Position... freed) {
        updatePairsLabel();
        if (endless) {
            refillEndless(freed);
            return;
        }
        if (gameBoard.getcellpositions().isEmpty()) {
            Platform.runLater(this::victory);
        } else if (deadlock()) {
            Platform.runLater(() -> defeat("棋盘无任何可消除的图案对（死局）！"));
        }
    }

    /** 无尽模式：消除后在被消除的原位补充新棋子（保持棋盘形状与连通性）；
     *  若补充后没有任何可消除对，则自动洗牌（有界重试，保证一定可玩） */
    private void refillEndless(Position... freed) {
        for (Position p : freed) {
            Cell cell = gameBoard.getCell(p.getRow(), p.getCol());
            if (cell != null && cell.isEmpty()) {
                cell.setNumber(1 + random.nextInt(5));
                boardView.spawnTile(cell);
            }
        }
        int tries = 0;
        while (findHintPair() == null && tries < 60) {
            BoardGenerator.shuffleNumbers(gameBoard);
            tries++;
        }
        if (tries > 0) {
            boardView.refreshIcons();
            boardView.refreshSelection();
            firstSelection = null;
            showToast("无可消除对，已自动洗牌");
        }
    }

    /** 消除完成判定 */
    private boolean finish() {
        return gameBoard.getcellpositions().isEmpty();
    }

    /** 死局判定（规则在 GameSession 中） */
    private boolean deadlock() {
        return session.isDeadlock(gameBoard);
    }

    private void victory() {
        // 先播放通关庆祝（彩屑 + 升级音效），随后再弹出结算弹窗
        boardView.celebrate();
        AudioPlayer.combosound();
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(1000));
        pause.setOnFinished(e -> gameOver("恭喜通关！", null));
        pause.play();
    }

    private void defeat(String reason) {
        gameOver("游戏失败", "失败原因：" + reason);
    }

    private void gameOver(String title, String subtitle) {
        stopTimer();
        saveProgress();
        String message = subtitle == null
                ? title + "！\n最终得分：" + session.getScore()
                : title + "\n" + subtitle;
        PixelDialog.choice(scene.getWindow(), message, 420,
                new String[]{"重新开始", "返回主界面"}, choice -> {
                    if (choice == 0) {
                        resetGame();
                    } else {
                        ScreenManager.showMainMenu();
                    }
                });
    }

    /** 与原版一致：胜利、失败或关窗时，登录用户自动存档 */
    private void saveProgress() {
        if (!UserSession.loggedIn) {
            return;
        }
        RecordStore.save(new Record(UserSession.username, session.getScore(), remainingTime,
                difficulty == GameDifficulty.EASY ? 1 : difficulty == GameDifficulty.ENDLESS ? 3 : 2,
                gameBoard.getBoard(), countRemaining(), session.getItemPoints()));
    }

    private int countRemaining() {
        int count = 0;
        for (Cell[] row : gameBoard.getBoard()) {
            for (Cell cell : row) {
                if (cell != null && !cell.isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 重新开始：恢复开局棋盘并复位全部状态 */
    private void resetGame() {
        stopTimer();
        timer = null;
        gameBoard = new GameBoard(originalBoard.length, originalBoard[0].length, BoardGenerator.deepCopy(originalBoard));
        boardView.setGameBoard(gameBoard);
        remainingTime = totalTime;
        updateTimeLabel();
        session.reset(2);
        scoreLabel.setText("0");
        updateToolLabel();
        chosenTool = 0;
        toolCursor.setVisible(false);
        refreshToolStyles();
        eliminated = 0;
        statusLabel.setText("未开始");
        hideToast();
        gameStarted = false;
        paused = false;
        firstSelection = null;
        startButton.setDisable(false);
        restartButton.setDisable(true);
        pauseButton.setDisable(true);
        setToolsEnabled(false);
        updatePairsLabel();
    }

    // ———— 显示辅助 ————

    private void showToast(String text) {
        toast.setText(text);
        toast.setVisible(true);
        toast.setOpacity(1);
        if (toastAnim != null) {
            toastAnim.stop();
        }
        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(1800));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(320), toast);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toast.setVisible(false));
        toastAnim = new SequentialTransition(fadeIn, hold, fadeOut);
        toastAnim.play();
    }

    private void hideToast() {
        if (toastAnim != null) {
            toastAnim.stop();
        }
        toast.setVisible(false);
        toast.setOpacity(1);
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("%02d:%02d", remainingTime / 60, remainingTime % 60));
    }

    private void updateToolLabel() {
        toolPointsLabel.setText("× " + session.getItemPoints());
    }

    private void updatePairsLabel() {
        if (endless) {
            pairsLabel.setText(String.valueOf(eliminated));
        } else {
            pairsLabel.setText(String.valueOf(initialPairs - eliminated));
        }
    }

    private void setToolsEnabled(boolean enabled) {
        for (Button button : toolButtons) {
            button.setDisable(!enabled);
        }
        for (AppButton button : instantItemButtons) {
            button.setDisable(!enabled);
        }
    }

    // ———— Screen ————

    @Override
    public Scene scene() {
        return scene;
    }

    @Override
    public String title() {
        return "连连看 - " + switch (difficulty) {
            case EASY -> "简单模式";
            case HARD -> "困难模式";
            case ENDLESS -> "无尽模式";
        };
    }

    @Override
    public double width() {
        return 1000;
    }

    @Override
    public double height() {
        return 900;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    /** 与原版一致：游戏中关窗自动存档并回到主菜单 */
    @Override
    public boolean onCloseRequest() {
        saveProgress();
        stopTimer();
        ScreenManager.showMainMenu();
        return true;
    }
}
