package ui;


import utils.Utils;
import model.*;
import model.Rectangle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.AudioPlayer.hitsound;

public class BoardPanel extends JPanel {
    private static final String backgroundimage="resource/images/ruins.png";
    private static final Color emptycolor =new Color(0, 0, 0, 0);
    private static final Color cellbordercolor=new Color(255, 255, 255, 50);
    private Image boardimage=null;

    int distancex;
    int distancey;

    List<Image> imageList=new ArrayList<>();
    GameBoard gameBoard;
    List<Line> lineList=new ArrayList<>();
    int totalrow;
    int totalcol;
    boolean showline;
    int width;
    int height;
    int cellWidth;
    int cellHeight;
    Position first=null;
    Position second=null;

    private Position hoverPosition=null;

    boolean animating=false;
    private boolean gameStarted=false;
    private GameFrame gameFrame;

    private Position startpos=null;
    private Position endpos=null;
    private Timer eliminationtimer=null;
    private int step=0;
    private final int steps=15;
    private final int duration=300;
    private boolean eliminationAnimating=false;

    public Position getposition(int x,int y){//将xy坐标转化成对应的格子
        int col=x/cellWidth;
        int row=y/cellHeight;
        if (row<0||row>=totalrow||col<0||col>=totalcol) {
            return null;
        }
        return new Position(row, col);
    }
    public void showline(List<Position> path){
        lineList.clear();
        lineList.add(new Line(path));
        showline=true;
        repaint();
    }
    //创建并初始化游戏棋盘
    public BoardPanel(GameBoard gameBoard, GameFrame gameFrame, int distancex, int distancey, int width, int height){
        this.gameFrame=gameFrame;
        this.distancex = distancex;//棋盘距离窗口左边的距离
        this.distancey = distancey;//棋盘距离窗口上面的距离
        this.totalrow=gameBoard.getRowCnt();
        this.totalcol=gameBoard.getColCnt();
        this.width=width;
        this.height=height;
        this.gameBoard=gameBoard;
        this.cellWidth=width/totalcol;//每个棋子的宽度
        this.cellHeight=height/totalrow;//每个棋子的高度

        this.setBounds(distancex,distancey,width,height);
        this.setLayout(new GridLayout(this.totalrow,this.totalcol));//按照传入的行和列切割成网格
        this.setPreferredSize(new Dimension(this.width,this.height));

        boardimage=loadimage(backgroundimage);
        loadImages();

        this.addMouseListener(new MouseAdapter() {//鼠标监听
            @Override public void mouseClicked(MouseEvent e) {
                click(e.getX(),e.getY());}//当鼠标点击的时候执行handleclick
        });

        this.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Position position=getposition(e.getX(),e.getY());//获取鼠标的位置
                if (position==null){//如果鼠标不在格子上，就清空悬停位置，重新绘制
                    hoverPosition=null;
                    repaint();
                    return;
                }
                Cell cell=gameBoard.getCell(position.getRow(),position.getCol());
                if (cell==null||cell.isEmpty()) {
                    hoverPosition=null;
                } else {
                    hoverPosition=position;
                }
                repaint();
            }
        });
        this.addComponentListener(new ComponentAdapter() {//拉伸窗口
            @Override
            public void componentResized(ComponentEvent e) {
                resizeBoard();
            }
        });
    }
    private Image loadimage(String path) {//从指定路径加载图片
        if (path==null||path.isEmpty()) {
            return null;
        }
        File f=new File(path);
        if (f.exists()) {
            return new ImageIcon(f.getPath()).getImage();
        } else {
            return null;
        }
    }
    private void loadImages() {//将图片添加到imagelist
        imageList.clear();
        imageList.add(null);//占掉0号位置，因为图片编号从1开始
        File dir=new File("resource/images");
        String skinname=app.UserSession.currentSkin + "_";//生成如fruits_
        File[] files=dir.listFiles((d, name)->name.endsWith(".png")&&name.startsWith(skinname));//查找以skinname开头，以.png结尾的图片
        if (files!=null) {
            Arrays.sort(files, (f1, f2) -> {//将图片按照编号排序
                int n1=Integer.parseInt(f1.getName().replace(skinname, "").replace(".png", ""));
                int n2=Integer.parseInt(f2.getName().replace(skinname, "").replace(".png", ""));
                return Integer.compare(n1, n2);//从小到大排序
            });
            for (int i=0;i<files.length;i++){
                File file=files[i];
                ImageIcon icon=new ImageIcon(file.getPath());
                // 缩放图片
                Image image=icon.getImage().getScaledInstance(cellWidth - 4, cellHeight - 4, Image.SCALE_SMOOTH);
                // 添加到列表
                imageList.add(image);
            }
        }
    }
    //处理按下鼠标之后的操作
    public void click(int x, int y) {
        if (gameStarted==false) {//如果没有按开始游戏，按钮，则不允许开始游戏
            new Dialog(null, "请先点击【开始游戏】按钮！", 500,300);
            return;
        }
        if (animating||eliminationAnimating) {
            return;
        }
        Position pos=getposition(x, y);
        if (pos==null) {
            return;
        }//如果点击的位置不在棋盘上，无效

        Cell clicked=gameBoard.getCell(pos.getRow(), pos.getCol());//获得点击的位置在棋盘的哪一行哪一列
        if (clicked==null||clicked.isEmpty()){
            return;
        }//如果点击到空的地方，无效

        if (first==null) {//如果还没有进行第一次点击，那么把这次点击的位置赋值给第一次点击
            gameBoard.clearchosen();
            clicked.setChosen(true);
            first=pos;
            repaint(); return;
        }

        if (first.equals(pos)) {//如果点击到的地方是第一次点的地方则取消
            clicked.setChosen(false);
            first = null; second = null;
            repaint(); return;
        }
        //上面的都不满足，那么点击的位置就赋值给second
        second=pos;
        Cell firstcell=gameBoard.getCell(first.getRow(), first.getCol());
        Cell secondcell=gameBoard.getCell(second.getRow(), second.getCol());
        secondcell.setChosen(true);
        repaint();

        List<Position> path=Utils.findPath(gameBoard,first,second);//利用util代码判断是否连接，如果能够连接就生成路径

        if (path!=null) {
            animating=true;
            showline(path);
            Timer timer=new Timer(300, e -> {
                int iconnumber=firstcell.getNumber();
                startpos=first;
                endpos=second;
                startelimination();//播放消除动画
                hitsound();

                Timer clearTimer=new Timer(300, e2 -> {
                    firstcell.setempty(true);
                    secondcell.setempty(true);
                    firstcell.setChosen(false);
                    secondcell.setChosen(false);
                    gameFrame.addeliminate();//增加一个消除计数
                    gameFrame.getStatusPanel().displayleft();//刷新剩余棋子数
                    gameFrame.getStatusPanel().addscore(iconnumber);
                    showline=false;
                    lineList.clear();
                    first=null;
                    second=null;
                    animating = false;
                    repaint();
                    if (gameFrame.finish()){
                        gameFrame.victory();
                        return;
                    }
                    if (gameFrame.deadlock()) {
                        gameFrame.defeat("棋盘无任何可消除的图案对（死局）！");
                    }
                });
                clearTimer.setRepeats(false);
                clearTimer.start();
            });
            timer.setRepeats(false); timer.start();
        } else {//如果不能消除
            gameBoard.clearchosen();
            secondcell.setChosen(true);
            first=second;
            second=null;
            repaint();
        }
    }

    public void startgame(boolean started) {
        this.gameStarted=started;
    }

    private void startelimination() {
        eliminationAnimating=true;
        step=0;//重置动画到未开始状态
        if (eliminationtimer !=null)
            eliminationtimer.stop();
        eliminationtimer=new Timer(duration/steps, e -> {//用总时间除以总步数，得到每一步的间隔时间
            step++;
            if (step>=steps) {//动画结束的判断
                eliminationtimer.stop();
                eliminationAnimating=false;
            }
            repaint();
        });
        eliminationtimer.start();
    }

    public void resetBoard(GameBoard board) {
        this.gameBoard=board;
        first=null;
        second=null;
        showline=false;
        lineList.clear();
        animating=false;
        eliminationAnimating=false;
        if (eliminationtimer !=null) {
            eliminationtimer.stop();
        }
        repaint();
    }

    public void resetchoosen() {
        gameBoard.clearchosen();
        first=null;
        second=null;
        repaint(); }

    public void restartGame() {//因为dialog类在这不好用，所以重新写一个
        JDialog dialog = new JDialog(gameFrame, "确认重开游戏", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(gameFrame);
        dialog.setAlwaysOnTop(true);

        JPanel bgPanel = new JPanel() {
            private final String path="resource/images/dialog.png";
            private Image loadBg() {
                return new ImageIcon(path).getImage();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img=loadBg();
                g.drawImage(img, 0,0,getWidth(),getHeight(),this);
            }
        };
        bgPanel.setLayout(new BoxLayout(bgPanel, BoxLayout.Y_AXIS));
        bgPanel.setOpaque(false);
        dialog.setContentPane(bgPanel);

        JLabel label=new JLabel("确定要重新开始游戏吗？");
        label.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 20));
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        GameButton ok=new GameButton("确定", 100, 35);
        ok.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(e -> {
            dialog.dispose();
            gameFrame.reset();
        });

        // 布局
        bgPanel.add(Box.createVerticalStrut(30));
        bgPanel.add(label);
        bgPanel.add(Box.createVerticalStrut(10));
        bgPanel.add(ok);

        dialog.setVisible(true);
    }

    public Rectangle getRectangle(Position position) {
        return new Rectangle(position.getCol() * cellWidth, position.getRow() * cellHeight, cellWidth, cellHeight);
    }

    private void resizeBoard() {
        int w=getWidth(),h=getHeight();
        if (w<=0||h<=0) {
            return;
        }
        width=w; //传入新的长度和高度
        height=h;
        cellWidth=width/totalcol;
        cellHeight=height/totalrow;
        loadImages();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;//将普通绘图工具转为2D绘图工具
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);//开启抗锯齿

        //绘制背景图片
        g2.drawImage(boardimage, 0, 0, width, height, this);
        //绘制棋子
        for (int i=0;i<gameBoard.getRowCnt(); i++) {
            for (int j=0;j<gameBoard.getColCnt(); j++) {
                Cell cell=gameBoard.getCell(i, j);
                Rectangle rec=getRectangle(new Position(i, j));//获得这个格子的宽和高
                boolean hover=false;
                if (hoverPosition!=null&&hoverPosition.getRow()==i&&hoverPosition.getCol()==j){//判断鼠标是否悬停在某个棋子上
                    hover=true;
                }
                //绘制空格子
                if (cell.isEmpty()) {
                    g2.setColor(emptycolor);
                    g2.fillRect(rec.getX(),rec.getY(),rec.getWidth(),rec.getHeight());
                }
                //绘制有棋子的格子
                if (cell.isEmpty()==false) {
                    int dx=rec.getX()+2;//图片往右下移两个像素同时长宽各缩小四个像素，相当于整体往内收2个像素
                    int dy=rec.getY()+2;
                    int dw=rec.getWidth()-4;
                    int dh=rec.getHeight()-4;

                    //判断选中的格子是不是正在执行消除动画的两个格子
                    boolean iseliminating=eliminationAnimating&&((startpos!=null&&startpos.getRow()==i&&startpos.getCol()==j)||(endpos!=null&&endpos.getRow()==i&&endpos.getCol()==j));

                    if (hover&&cell.getIsChosen()==false&&iseliminating==false) {//悬停的时候放大
                         dx-=5;
                         dy-=5;
                         dw+=10;
                         dh+=10;//放大和上面同理
                    }
                    if (iseliminating) {
                        double progress=(double) step/steps;//表示动画进度（从0到1），括号代表强制类型转换
                        //逐渐放大
                        double scale=1.0+0.5*progress;//图标总共放大到1.5倍
                        int sw=(int)(dw * scale);//新的长宽：按照倍数缩放
                        int sh=(int)(dh * scale);
                        int sx=dx-(sw-dw)/2;//计算新的位置坐标
                        int sy=dy-(sh-dh)/2;
                        //逐渐透明
                        float alpha=1.0f-(float)progress;//alpha:透明度，用float：透明度规定是0.0-1.0的小数
                        Composite orig=g2.getComposite();//保存原有的绘画模式
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                        int idx=cell.getNumber();
                        if (idx>=1&&idx<imageList.size()){
                            g2.drawImage(imageList.get(idx),sx,sy,sw,sh, this);
                        }
                        g2.setComposite(orig);//恢复到原来的绘画模式
                    }
                    else {//剩下的都是普通图标
                        int idx=cell.getNumber();
                        if (idx>=1&&idx<imageList.size())
                            g2.drawImage(imageList.get(idx),dx,dy,dw,dh, this);
                    }
                }

                //棋子的边框
                if (cell.getIsChosen()) {
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRect(rec.getX()+1, rec.getY()+1, rec.getWidth()-3, rec.getHeight()-3);
                } else {//没有选择的棋子统一用普通边框
                    g2.setColor(cellbordercolor);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRect(rec.getX(), rec.getY(), rec.getWidth()-1, rec.getHeight()-1);
                }
            }
        }
        //绘制连线
        if(showline) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(4));
            Line line=lineList.get(0);
            List<Position> path=line.getPathPoints();
            for (int i=0;i<path.size()-1;i++) {
                    Rectangle r1=getRectangle(path.get(i));
                    Rectangle r2=getRectangle(path.get(i+1));
                    g2.drawLine((int)r1.centerposition().getX(),(int)r1.centerposition().getY(),(int)r2.centerposition().getX(),(int)r2.centerposition().getY());
            }
        }
    }

    public Cell getonecell(int x, int y) {
        Position pos=getposition(x, y);
        if (pos==null) return null;
        Cell cell=gameBoard.getCell(pos.getRow(),pos.getCol());
        if (cell!=null&&cell.isEmpty()==false) {
            return cell;
        } else {
            return null;
        }
    }

    // 直接消除单个棋子
    public void killonecell(Cell cell) {
        cell.setempty(true);
        repaint();
        // 刷新剩余棋子数
        gameFrame.getStatusPanel().displayleft();
    }
}