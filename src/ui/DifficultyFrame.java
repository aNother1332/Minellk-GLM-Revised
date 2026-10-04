package ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class DifficultyFrame extends JFrame {
    public DifficultyFrame() {
        setTitle("选择游戏难度");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setUndecorated(true);//关掉上面那个标题栏
        setAlwaysOnTop(true);//始终显示在所有窗口的最上方
        //绘制背景图片
        JPanel difficultypanel=new JPanel() {
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
        difficultypanel.setLayout(new BoxLayout(difficultypanel, BoxLayout.Y_AXIS));//垂直布局
        setContentPane(difficultypanel);

        JLabel titleLabel=new JLabel("请选择游戏难度", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 30));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);//居中

        JButton easy=new GameButton("简单模式", 120, 40);
        easy.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 15));
        easy.setFocusPainted(false);
        easy.setAlignmentX(Component.CENTER_ALIGNMENT); // 按钮居中
        easy.addActionListener(e -> {
            dispose();
            new GameFrame("连连看 - 简单模式", 800, 900, GameDifficulty.EASY);
        });

        JButton hard=new GameButton("困难模式", 120, 40);
        hard.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 15));
        hard.setFocusPainted(false);
        hard.setAlignmentX(Component.CENTER_ALIGNMENT); // 按钮居中
        hard.addActionListener(e -> {
            dispose();
            new GameFrame("连连看 - 困难模式", 800, 900, GameDifficulty.HARD);
        });
        JButton back=new GameButton("返回", 100, 35);
        back.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 15));
        back.setFocusPainted(false);
        back.setAlignmentX(Component.CENTER_ALIGNMENT); // 按钮居中
        back.addActionListener(e -> {
            dispose();
            new MainMenuFrame();
        });
        //将按钮添加到界面上
        difficultypanel.add(Box.createVerticalStrut(20));
        difficultypanel.add(titleLabel);
        difficultypanel.add(Box.createVerticalStrut(30));
        difficultypanel.add(easy);
        difficultypanel.add(Box.createVerticalStrut(15));
        difficultypanel.add(hard);
        difficultypanel.add(Box.createVerticalStrut(15));
        difficultypanel.add(back);

        setVisible(true);
    }
}