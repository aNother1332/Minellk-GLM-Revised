package ui;

import model.Cell;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ItemPanel extends JPanel {
    private static JLabel label;
    private GameButton toolA;
    private GameButton toolB;
    private GameButton toolC;

    public static int points=2;
    private GameFrame gameFrame;
    private BoardPanel boardPanel;
    private int choosedtool=0;

    // 构造方法
    public ItemPanel(GameFrame gameFrame, int x, int y, int width, int height) {
        this.gameFrame=gameFrame;
        this.boardPanel=gameFrame.boardPanel;

        this.setBounds(x, y, 100, height);
        this.setMinimumSize(new Dimension(100, height));
        this.setMaximumSize(new Dimension(100, height));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(new Color(245, 245, 245));
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "道具"));//蚀刻边框，看起来非常的高级
        initialize();
        layoutcomponents();
        setupEventHandlers();
        mouse();
    }

    private void initialize() {
        label=new JLabel("<html>剩余<br>道具<br>点数<br>：" + points + "</html>");//Jlabel不能实现换行，必须套一个html,识别成网页文本才能用br换行
        label.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        //钻石镐
        toolA=new GameButton("", 40, 40);
        ImageIcon A=new ImageIcon("resource/images/item1.png");
        Image imgA = A.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);//缩放图片
        toolA.setIcon(new ImageIcon(imgA));
        toolA.setHorizontalAlignment(SwingConstants.CENTER);//图片居中显示
        toolA.setVerticalAlignment(SwingConstants.CENTER);
        toolA.setAlignmentX(Component.CENTER_ALIGNMENT);
        //钻石斧
        toolB=new GameButton("", 40, 40);
        ImageIcon B=new ImageIcon("resource/images/item2.png");
        Image imgB=B.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        toolB.setIcon(new ImageIcon(imgB));
        toolB.setHorizontalAlignment(SwingConstants.CENTER);
        toolB.setVerticalAlignment(SwingConstants.CENTER);
        toolB.setAlignmentX(Component.CENTER_ALIGNMENT);
        //钻石铲
        toolC=new GameButton("", 40, 40);
        ImageIcon C=new ImageIcon("resource/images/item3.png");
        Image imgC=C.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        toolC.setIcon(new ImageIcon(imgC));
        toolC.setHorizontalAlignment(SwingConstants.CENTER);
        toolC.setVerticalAlignment(SwingConstants.CENTER);
        toolC.setAlignmentX(Component.CENTER_ALIGNMENT);

        toolA.setEnabled(false);
        toolB.setEnabled(false);
        toolC.setEnabled(false);
    }
    //设置布局
    private void layoutcomponents() {
        this.add(Box.createVerticalGlue());//顶部空白
        this.add(label);
        this.add(Box.createVerticalStrut(30));//间距
        this.add(toolA);
        this.add(Box.createVerticalStrut(15));
        this.add(toolB);
        this.add(Box.createVerticalStrut(15));
        this.add(toolC);
        this.add(Box.createVerticalGlue());
    }
    // 按钮事件
    private void setupEventHandlers() {
        toolA.addActionListener(e -> usetool(1));
        toolB.addActionListener(e -> usetool(2));
        toolC.addActionListener(e -> usetool(3));
    }
    // 使用道具
    private void usetool(int toolType) {
        if (points>=1) {
            points-=1;
            updatepoint();
            choosedtool=toolType;
        }
    }
    // 鼠标监听（不变）
    private void mouse() {
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (choosedtool == 0) {return;};
                Cell clicked=boardPanel.getonecell(e.getX(), e.getY());//获得鼠标的位置，然后转换成棋盘的格子
                if (clicked==null) {
                    choosedtool=0;
                    return;
                }
                int number=clicked.getNumber();
                String name=StatusPanel.getname(number);
                boolean valid=false;
                String toolname = "";

                if (choosedtool==1) {
                    valid=(number==1||number==4||number==7||number==10);
                    toolname="钻石镐";
                } else if (choosedtool==2) {
                    valid=(number==2||number==5||number==8||number==11);
                    toolname="钻石斧";
                } else if (choosedtool==3) {
                    valid=(number==3||number==6||number==9||number==12);
                    toolname="钻石铲";
                }
                if (valid) {
                    boardPanel.killonecell(clicked);
                    clicked.setChosen(false);
                    gameFrame.getStatusPanel().addscore(10);
                    gameFrame.displayMessage("使用" + toolname + "消除了" + name + "×1");
                } else {
                    clicked.setChosen(false);
                    gameFrame.displayMessage(toolname + "对" + name + "无效！");
                }
                choosedtool=0;
            }
        });
    }

    public static void updatepoint() {
        label.setText("<html>剩余<br>道具<br>点数<br>："+points+"</html>");
    }
    public void setItemsEnabled(boolean enabled) {
        toolA.setEnabled(enabled);
        toolB.setEnabled(enabled);
        toolC.setEnabled(enabled);
    }
    public void reset() {
        points=2;
        updatepoint();
        setItemsEnabled(false);
        choosedtool=0;
    }
}