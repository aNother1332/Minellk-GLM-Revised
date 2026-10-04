package ui;

import app.UserSession;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SkinSelectionFrame extends JFrame {
    private MainMenuFrame mainMenuFrame;
    public SkinSelectionFrame(MainMenuFrame mainMenuFrame) {

        JPanel chooseskin=new JPanel() {
            // 加载背景图片
            private final Image image=loadBackgroundImage();
            private final String backgroundpath="resource/images/difficultypage.png";
            private Image loadBackgroundImage(){
                File file = new File(backgroundpath);
                return new ImageIcon(file.getPath()).getImage();
            }
            // 重写绘制：铺满背景图
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            }
        };
        chooseskin.setLayout(new BoxLayout(chooseskin, BoxLayout.Y_AXIS));//垂直布局
        setContentPane(chooseskin);

        this.mainMenuFrame=mainMenuFrame;
        setTitle("选择主题");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);//关掉上面那个标题栏
        setAlwaysOnTop(true);//始终显示在所有窗口的最上方

        JLabel titleLabel = new JLabel("请选择主题", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        //自然按钮
        JButton nature=new GameButton("自然", 180, 45);
        nature.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 24));
        nature.setFocusPainted(false);
        nature.setAlignmentX(Component.CENTER_ALIGNMENT);
        nature.addActionListener(e -> {
            UserSession.currentSkin="nature";
            new Dialog(this, "已切换到自然主题",500 ,300 );
            dispose();
        });
        //村庄
        JButton village=new GameButton("村庄", 180, 45);
        village.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 24));
        village.setFocusPainted(false);
        village.setAlignmentX(Component.CENTER_ALIGNMENT);
        village.addActionListener(e -> {
            UserSession.currentSkin="village";
            new Dialog(this, "已切换到村庄主题",500,300 );
            dispose();
        });
        //返回按钮
        JButton close=new GameButton("返回",140 , 45);
        close.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
        close.setFocusPainted(false);
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> {
            dispose();
        });
        //按顺序添加按钮
        chooseskin.add(Box.createVerticalStrut(20));   // 顶部留白
        chooseskin.add(titleLabel);                    // 标题
        chooseskin.add(Box.createVerticalStrut(30));    // 标题间距
        chooseskin.add(nature);
        chooseskin.add(Box.createVerticalStrut(15));    // 按钮间距
        chooseskin.add(village);
        chooseskin.add(Box.createVerticalStrut(20));    // 底部间距
        chooseskin.add(close);                      // 返回按钮

        setVisible(true);
    }
}