package ui;

import app.UserSession;
import model.AudioPlayer;

import javax.swing.*;
import java.awt.*;

public class MainMenuFrame extends JFrame {

    private boolean loggedIn=false;
    private String currentUser="游客";

    private JLabel userLabel;
    private JButton startButton;
    private JButton loadButton;
    private JButton leaderboardButton;
    private JButton skinButton;
    private JButton loginButton;

    private static final String bgimagepath="resource/images/login page.png";
    private static final String BTN_FONT="Fusion Pixel 10px Mono zh_hans";

    public MainMenuFrame() {
        setTitle("Minellk-连连看");
        setSize(1000, 1000);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//关闭窗口=退出游戏
        setResizable(true);

        JPanel background=new JPanel() {
            private Image backgroundimage=loadBg();
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundimage!=null)
                    g.drawImage(backgroundimage, 0, 0, getWidth(), getHeight(), this);//强制拉伸到与窗口一样大
            }
            private Image loadBg() {
                java.io.File f=new java.io.File(bgimagepath);
                if (f.exists()) {
                    return new ImageIcon(f.getPath()).getImage();
                } else {
                    return null;
                }
            }
        };
        background.setLayout(new BorderLayout());
        setContentPane(background);


        //按键
        JPanel centerPanel=new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(300, 100, 30, 100));

        startButton=new GameButton("开始新游戏", 300, 70);
        loadButton=new GameButton("读取存档", 300, 70);
        leaderboardButton=new GameButton("排行榜", 300, 70);
        skinButton=new GameButton("更换主题", 300, 70);

        loadButton.setEnabled(false);

        startButton.addActionListener(e -> {
            dispose();
            new DifficultyFrame(); });
        loadButton.addActionListener(e -> {
            setVisible(false);
            new LoadSaveFrame(this); });
        leaderboardButton.addActionListener(e -> {
            dispose();
            new LeaderboardFrame(); });
        skinButton.addActionListener(e -> new SkinSelectionFrame(this));

        for (JButton b : new JButton[]{startButton, loadButton, leaderboardButton, skinButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(Box.createVerticalStrut(18));//设置间距
            centerPanel.add(b);
        }
        centerPanel.add(Box.createVerticalGlue());
        background.add(centerPanel, BorderLayout.CENTER);

        //底部：用户信息和登录按钮
        JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 12));//距离右下角12
        //用户信息
        userLabel=new JLabel("当前用户：" + currentUser);
        userLabel.setFont(new Font(BTN_FONT, Font.BOLD, 18));
        userLabel.setForeground(Color.WHITE);
        bottomPanel.add(userLabel);

        //登录按钮
        loginButton=new GameButton("登录", 100, 40);
        loginButton.setFont(new Font(BTN_FONT, Font.BOLD, 16)); // 单独改小字体
        loginButton.addActionListener(e -> {
                    for (Window window : Window.getWindows()) {
                        if (window instanceof LoginFrame && window.isShowing()) {
                            return; // 遍历所有窗口，如果登录界面已经打开，直接return，不创建新窗口
                        }
                    }
                    new LoginFrame(this);});
        bottomPanel.add(loginButton);

        background.add(bottomPanel, BorderLayout.SOUTH);
        refresh();
        AudioPlayer.backgroundsound();
        setVisible(true);
    }

    public void refresh() {//更新当前状态
        userLabel.setText("当前用户：" + UserSession.username);
        loggedIn=UserSession.loggedIn;
        currentUser=UserSession.username;
        loadButton.setEnabled(loggedIn);
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING)//如果点击右上角，就额外把音乐关掉
            AudioPlayer.stopmusic();
        super.processWindowEvent(e);//其他和正常关闭一样
    }
}