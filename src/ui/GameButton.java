//根据名称和大小创建按钮，不创建图标位置
package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class GameButton extends JButton {
    static final String backgroundpath="resource/images/btn_bg.png";
    private static final String Fontname="Fusion Pixel 10px Mono zh_hans";
    private static final Color wordcolor=new Color(255, 255, 255);
    private static final double scale=1.03; // 悬停放大倍数

    private boolean hover=false;
    private final Dimension size;

    // 构造器：传入文字+固定宽高
    public GameButton(String text,int width,int height) {
        super(text);
        this.size = new Dimension(width, height);
        style();
        hover();
    }

    // 统一设置按钮样式
    private void style() {
        ImageIcon icon=new ImageIcon(backgroundpath);
        Image scaled=icon.getImage().getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
        setIcon(new ImageIcon(scaled));
        // 字体
        setFont(new Font(Fontname, Font.BOLD, 26));
        setForeground(wordcolor);
        // 透明无边框
        setContentAreaFilled(false);
        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        // 文字居中
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    // 悬停放大逻辑
    private void hover() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover=true;
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hover=false;
                repaint();
            }
        });
    }
    // 固定大小
    @Override
    public Dimension getPreferredSize() {
        return size;
    }
    @Override
    public Dimension getMaximumSize() {
        return size;
    }
    @Override
    public Dimension getMinimumSize() {
        return size;
    }
    // 悬停绘制
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d=(Graphics2D) g;//和BoardPanel里面的一样
        if (hover){
            g2d.translate(getWidth()/2,getHeight()/2);//从中心开始放大
            g2d.scale(scale, scale);
            g2d.translate(-getWidth()/2,-getHeight()/2);
        }
        super.paintComponent(g2d);
    }
}