package ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Dialog extends JDialog {
    public Dialog(JFrame parent,String message,int width,int height) {
        super(parent,true); // 模态弹窗，必须关闭才能操作主界面
        setSize(width,height);
        setLocationRelativeTo(parent);
        setUndecorated(true);
        setAlwaysOnTop(true);

        JPanel bgPanel=new JPanel() {
            private final String dialogpath="resource/images/dialog.png";
            private Image loadBg() {
                File f=new File(dialogpath);
                return new ImageIcon(f.getPath()).getImage();
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
        setContentPane(bgPanel);

        // 提示文字
        JLabel label=new JLabel(message);
        label.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 20));
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 确定按钮
        GameButton ok=new GameButton("确定", 100, 35);
        ok.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 16));
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(e -> dispose());

        //添加到弹窗上
        bgPanel.add(Box.createVerticalStrut(100));
        bgPanel.add(label);
        bgPanel.add(Box.createVerticalStrut(20));
        bgPanel.add(ok);

        setVisible(true);
    }
}
