package ui;

import app.UserSession;
import model.*;
import model.Record;
import utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameFrame extends JFrame {

    int width;
    int height;
    String title;

    StatusPanel statusPanel;
    ControlPanel controlPanel;
    BoardPanel boardPanel;
    GameBoard gameBoard;
    GameDifficulty difficulty;
    ItemPanel itemPanel;

    private JLabel message;//提示信息
    private Cell[][] originalboard;//初始棋盘
    private int totaltime;//初始时间
    private int initial;//初始对数
    private int eliminated;


    //创建新游戏
    public GameFrame(String title,int width,int height,GameDifficulty difficulty) {
        super(title);
        this.setResizable(true); //允许缩放
        this.difficulty=difficulty;
        // 初始化棋盘
        Cell[][]board=generateboard(difficulty);

        int rows=board.length;
        int cols=board[0].length;
        this.gameBoard=new GameBoard(rows,cols,board);

        // 深拷贝保存，用于重新开始游戏
        this.originalboard= deepcopy(board);
        if(difficulty==GameDifficulty.EASY){//设置初始对数
            this.initial=16;
        }
        else{this.initial=50;}
        this.eliminated=0;//消除对数

        initFrame(title, width, height);
        // 初始化倒计时
        int total;
        if(difficulty==GameDifficulty.EASY){
            total=100;
        }
        else{total=150;}
        this.totaltime=total;
        statusPanel.settime(total);
    }
    //从存档中加载游戏
    public GameFrame(Record record) {
        super("连连看 - 读取存档");
        this.setResizable(true);
        Cell[][]save=record.getBoard();
        int rows=save.length;
        int cols=save[0].length;
        this.gameBoard=new GameBoard(rows, cols, save);
        this.originalboard = deepcopy(save);
        this.totaltime=record.getLefttime();
        this.difficulty=record.getLevel() == 1 ? GameDifficulty.EASY : GameDifficulty.HARD;

        initFrame("连连看 - 存档恢复", 1000, 900);

        statusPanel.recovertime(record.getLefttime());
        statusPanel.setStatus("未开始");
        statusPanel.setScore(record.getScore());// 设置存档的分数

        ItemPanel.updatepoint();

        // 恢复初始对数和已消除对数
        if(record.getLevel()==1){
            this.initial=16;
        }
        else{this.initial=50;}
        int remaining=record.getRemaincount();
        this.eliminated =(this.initial*2-remaining) / 2; // 计算已消除对数
    }

   //创建一个窗口，将board,control,status添加进来
    private void initFrame(String title,int width,int height){
        this.title=title;
        this.width=width;
        this.height=height;
        this.setLayout(new BorderLayout());
        this.setSize(width, height);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//关闭之后直接关闭游戏

        this.boardPanel=new BoardPanel(gameBoard, this, 0, 0, 800, 600);
        this.statusPanel=new StatusPanel(this, 0, 0, 800, 100);
        this.controlPanel=new ControlPanel(this,statusPanel, boardPanel, 0, 0, 800, 120);
        this.itemPanel=new ItemPanel(this, 800, 0, 100, 720);
        this.add(statusPanel,BorderLayout.NORTH);
        this.add(boardPanel,BorderLayout.CENTER);
        this.add(controlPanel,BorderLayout.SOUTH);
        this.add(itemPanel, BorderLayout.EAST);

        //顶部信息提示标签
        this.message=new JLabel("", SwingConstants.CENTER);
        message.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        message.setForeground(Color.BLACK);
        message.setBackground(new Color(245, 245, 245));
        message.setOpaque(true);
        message.setDoubleBuffered(true);//双缓冲，减少文字闪烁
        message.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        message.setVisible(false);
        //加到LayeredPane，位置设置在StatusPanel下方
        this.getLayeredPane().add(message,JLayeredPane.POPUP_LAYER);
        // 最后显示窗口
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        // 初始化剩余可消除对数显示
        statusPanel.displayleft();
    }

    public void showmessage(String message) {//显示顶部提示信息
        SwingUtilities.invokeLater(() -> {
            this.message.setText(message);
            Dimension size= this.message.getPreferredSize();//自动获取好的尺寸
            int width=Math.max(size.width+20,400);
            int height=size.height+8;
            int x=(this.getWidth()-width)/2;
            int y=70;
            this.message.setBounds(x,y,width,height);
            this.message.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
            this.message.setVisible(true);
            this.message.revalidate();//立刻刷新布局
            this.message.repaint();
            this.getLayeredPane().repaint();
        });
    }
    public void hidemessage() {//隐藏顶部提示信息
        SwingUtilities.invokeLater(() -> {
            message.setVisible(false);
            message.repaint();
        });
    }
    public int pairsleft() {//计算剩余对数
        return initial-eliminated;
    }
    public void addeliminate() {//增加已经消除的对数
        eliminated++;
    }

    //生成棋盘：
    //先生成随机图标：
    private List<Integer> iconlist(int totalicons, int maxIconIndex) {
        List<Integer> icons=new ArrayList<>();
        Random random=new Random();
        int total=totalicons/2;//棋子对数等于棋子总数的二分之一
        for (int i=0;i<total;i++) {
            int a=random.nextInt(maxIconIndex)+1;//+1：把0-n变成1-n+1
            icons.add(a);
            icons.add(a);
        }
        Collections.shuffle(icons);
        return icons;
    }
    //初始化棋盘
    private Cell[][] generateboard(GameDifficulty difficulty) {
        Cell[][]board;
        if (difficulty==GameDifficulty.EASY) {
            board=new Cell[11][11];
            for (int i=0;i<11;i++) {//先把所有图标初始化
                for (int j=0;j<11;j++) {
                    board[i][j]=new Cell(new Position(i, j), true, 0);
                }
            }
            List<Integer> easy=iconlist(32,5);//用一到五号图标
            int n=0;
            for (int i=1;i<=4;i++){//左上角
                for (int j=1;j<=4;j++){
                    board[i][j]=new Cell(new Position(i, j), false,easy.get(n++));
                }
            }
            for (int i=6;i<=9;i++){//右下角
                for (int j=6;j<=9;j++) {
                    board[i][j] = new Cell(new Position(i, j), false, easy.get(n++));
                }
            }
        }
        else{//困难模式的棋盘
            board=new Cell[12][12];
            for(int i=0;i<12;i++){
                for(int j=0;j<12;j++) {
                        board[i][j]=new Cell(new Position(i, j), true, 0);
                }
            }
            List<Integer> hardIcons=iconlist(100, 12);
            int n=0;
            for (int i=1;i<11;i++){
                for (int j=1;j<11;j++){
                    board[i][j]=new Cell(new Position(i, j), false,hardIcons.get(n++));
                }
            }
        }
        return board;
    }
    public void reset() {//重新开始游戏
        Cell[][] resetBoard = deepcopy(originalboard);//通过开局深拷贝的棋盘，恢复到初始状态
        gameBoard = new GameBoard(originalboard.length, originalboard[0].length, resetBoard);
        boardPanel.resetBoard(gameBoard);
        boardPanel.startgame(false);
        boardPanel.resetchoosen();
        itemPanel.reset();
        statusPanel.stopTimer();
        statusPanel.settime(totaltime);
        statusPanel.setStatus("未开始");
        statusPanel.resetScore();
        hidemessage();
        controlPanel.resetControlState();
        this.eliminated = 0;//重置消除的计数
        statusPanel.displayleft();
    }

    //深拷贝棋盘
    private Cell[][] deepcopy(Cell[][] original) {
        if (original==null) return null;
        int rows=original.length;
        int cols=original[0].length;
        Cell[][] copy=new Cell[rows][cols];
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                Cell originalCell=original[i][j];
                if (originalCell!=null) {//将原来棋子的属性复制给棋子b，再把深拷贝棋盘对应的位置换成棋子b
                    Cell b = new Cell(originalCell.getPos(), originalCell.isEmpty(), originalCell.getNumber());
                    b.setChosen(originalCell.getIsChosen());
                    copy[i][j]=b;
                } else {
                    copy[i][j]=null;
                }
            }
        }
        return copy;
    }

    //判定消除完成
    public boolean finish() {
        List<Position> cellpositions=gameBoard.getcellpositions();//获得非空棋子的位置，用列表的形式输出
       if(cellpositions.isEmpty()==true){
           return true;
        }
       else{
           return false;
       }
    }

    //判定死局
    public boolean deadlock() {
        List<Position> cellpositions=gameBoard.getcellpositions();
        if (cellpositions.size()<2&&ItemPanel.points==0) {
            return true;//如果剩下单个棋子，直接判断死局
        }
        for (int i=0;i<cellpositions.size();i++) {
            Position a1=cellpositions.get(i);//逐个获取非空棋子的位置
            Cell b1=gameBoard.getCell(a1.getRow(), a1.getCol());//通过非空棋子的位置找到这个棋子
            for (int j=i+1;j<cellpositions.size();j++) {
                Position a2=cellpositions.get(j);
                Cell b2=gameBoard.getCell(a2.getRow(),a2.getCol());
                if (b1.getNumber()==b2.getNumber()&&Utils.findPath(gameBoard, a1, a2)!=null) {
                    return false;//如果遍历剩下的棋子，能找到相同且能够连线的棋子，则判定为不是死局
                }
            }
        }
        if(ItemPanel.points==0){//需要道具积分为0判断死局
        return true;}
        else{return false;}
    }

    //胜利界面
    public void victory() {
        statusPanel.stopTimer();
        int score=statusPanel.getScore();
        if (UserSession.loggedIn) {//保存存档
            Record finalRecord=new Record(UserSession.username, score, statusPanel.getRemainingTime(), difficulty==GameDifficulty.EASY ? 1 : 2, gameBoard.getBoard(), countremains()
            );
            RecordStore.save(finalRecord);
        }
        //设置弹窗样式
        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setAlwaysOnTop(true);
        JPanel bgPanel=new JPanel() {
            private final String bgPath="resource/images/dialog.png";
            private Image loadimage() {
                File file=new File(bgPath);
                return new ImageIcon(file.getPath()).getImage();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image image=loadimage();
                g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setLayout(new BorderLayout());

        JPanel centerPanel=new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints a=new GridBagConstraints();

        JLabel resultLabel = new JLabel("恭喜通关！", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 24));
        a.gridx=0;a.gridy=0; a.insets=new Insets(10, 0, 5, 0);
        centerPanel.add(resultLabel, a);

        JLabel scoreLabel = new JLabel("最终得分：" + score, SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 18));
        a.gridx = 0; a.gridy = 1; a.insets = new Insets(5, 0, 20, 0);
        centerPanel.add(scoreLabel, a);

        bgPanel.add(centerPanel, BorderLayout.CENTER);
        // 按钮面板
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.setOpaque(false);
        //重新开始按钮
        JButton restart=new GameButton("重新开始", 120, 40);
        restart.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        restart.addActionListener(e -> {
            dialog.dispose();
            reset();
        });

        // 返回主界面按钮
        JButton back=new GameButton("返回主界面", 120, 40);
        back.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        back.addActionListener(e -> {
            dialog.dispose();
            this.dispose();
            SwingUtilities.invokeLater(MainMenuFrame::new);
        });
        buttons.add(restart);
        buttons.add(back);
        bgPanel.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(bgPanel);
        dialog.setVisible(true);
    }

    //失败界面
    public void defeat(String reason) {
        statusPanel.stopTimer();
        int score=statusPanel.getScore();
        if (UserSession.loggedIn) {//保存存档
            Record finalRecord=new Record(UserSession.username, score, statusPanel.getRemainingTime(), difficulty==GameDifficulty.EASY ? 1 : 2, gameBoard.getBoard(), countremains()
            );
            RecordStore.save(finalRecord);
        }
        //设置弹窗样式
        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setAlwaysOnTop(true);
        JPanel bgPanel=new JPanel() {
            private final String bgPath="resource/images/dialog.png";
            private Image loadimage() {
                File file=new File(bgPath);
                return new ImageIcon(file.getPath()).getImage();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image image=loadimage();
                g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setLayout(new BorderLayout());

        JPanel centerPanel=new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints a=new GridBagConstraints();

        JLabel resultLabel=new JLabel("游戏失败",SwingConstants.CENTER);
        resultLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 24));
        a.gridx=0;a.gridy=0; a.insets=new Insets(10, 0, 5, 0);
        centerPanel.add(resultLabel, a);

        JLabel reasonlabel=new JLabel("失败原因："+reason,SwingConstants.CENTER);
        reasonlabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 18));
        a.gridx = 0; a.gridy = 1; a.insets = new Insets(5, 0, 20, 0);
        centerPanel.add(reasonlabel, a);

        bgPanel.add(centerPanel, BorderLayout.CENTER);
        // 按钮面板
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.setOpaque(false);
        //重新开始按钮
        JButton restart=new GameButton("重新开始", 120, 40);
        restart.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        restart.addActionListener(e -> {
            dialog.dispose();
            reset();
        });

        // 返回主界面按钮
        JButton back=new GameButton("返回主界面", 120, 40);
        back.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        back.addActionListener(e -> {
            dialog.dispose();
            this.dispose();
            SwingUtilities.invokeLater(MainMenuFrame::new);
        });
        buttons.add(restart);
        buttons.add(back);
        bgPanel.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(bgPanel);
        dialog.setVisible(true);
    }

    //关闭窗口
    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {//监听到关闭窗口的事件
            if (UserSession.loggedIn) {
                Record record=new Record(UserSession.username, statusPanel.getScore(), statusPanel.getRemainingTime(),difficulty==GameDifficulty.EASY?1:2,gameBoard.getBoard(), countremains());
                RecordStore.save(record);
            }
            statusPanel.stopTimer();
            AudioPlayer.stopmusic();
            this.dispose();
            SwingUtilities.invokeLater(MainMenuFrame::new);
        }
        super.processWindowEvent(e);
    }
    //计算剩余的棋子数
    private int countremains() {
        int count=0;
        Cell[][] board=gameBoard.getBoard();
        for (int i=0;i<board.length;i++) {
            for (int j=0;j<board[i].length;j++) {
                Cell cell=board[i][j];
                if (cell!=null&&cell.isEmpty()==false) {
                    count++;
                }
            }
        }
        return count;
    }
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
    public void displayMessage(String message) {
        showmessage(message);
    }
    public ItemPanel getItemPanel() {
        return itemPanel;
    }
}