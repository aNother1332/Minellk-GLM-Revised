package ui;

import app.UserSession;
import app.UserStore;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LoginFrame extends JFrame {

    private JTextField username;
    private JPasswordField passwordin;

    private static final Color wordcolor=new Color(255, 255, 255);//文字颜色
    private static final Color btncolor=new Color(50, 50, 50);//按钮颜色
    private static final Font uifont=new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 16);
    private static final String imagepath="resource/images/login_bg2.png";

    public LoginFrame(MainMenuFrame menu) {
        setTitle("登录");
        setSize(400, 380);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);//关掉上面那个标题栏
        setAlwaysOnTop(true);//始终显示在所有窗口的最上方
        setLayout(null);
        //绘制背景图片
        JPanel bgPanel = new JPanel() {
            private final Image bgImage=loadimage();
            private Image loadimage() {
                File file=new File(imagepath);
                if (file.exists()) {
                    return new ImageIcon(file.getPath()).getImage();
                }
                return null;
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        bgPanel.setLayout(null);
        setContentPane(bgPanel);

        //用户名
        JLabel userLabel=new JLabel("用户名");
        userLabel.setFont(uifont);
        userLabel.setForeground(wordcolor);
        userLabel.setBounds(50, 140, 80, 30);
        add(userLabel);

        //用户名输入框
        username=new JTextField();
        username.setFont(uifont);
        username.setBackground(btncolor);
        username.setForeground(wordcolor);
        username.setCaretColor(wordcolor);
        username.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        username.setBounds(140, 140, 200, 30);
        add(username);

        // 密码
        JLabel passLabel=new JLabel("密码");
        passLabel.setFont(uifont);
        passLabel.setForeground(wordcolor);
        passLabel.setBounds(50, 190, 80, 30);
        add(passLabel);

        //密码输入框
        passwordin = new JPasswordField();//特殊的JPasswordField，隐藏输入内容
        passwordin.setFont(uifont);
        passwordin.setBackground(btncolor);
        passwordin.setForeground(wordcolor);
        passwordin.setCaretColor(wordcolor);
        passwordin.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        passwordin.setBounds(140, 190, 200, 30);
        add(passwordin);
        //登录按钮
        JButton login=new GameButton("登录", 120,40);
        login.setBounds(50, 240, 120, 40);
        bgPanel.add(login);
        //注册按钮
        JButton register=new GameButton("注册", 120,40);
        register.setBounds(220, 240, 120, 40);
        bgPanel.add(register);
        //游客模式
        JButton guest=new GameButton("游客模式", 120,40);
        guest.setBounds(110, 300, 160, 40);
        bgPanel.add(guest);


        //添加关闭按钮
        JButton close=new JButton("X");
        close.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 20));
        close.setForeground(Color.WHITE);
        close.setBackground(new Color(250, 0, 200));
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setBounds(360, 10, 30, 30);
        close.addActionListener(e -> dispose());
        bgPanel.add(close);

        //登录操作
        login.addActionListener(e -> {
            String user=username.getText().trim();
            String pass=new String(passwordin.getPassword());//禁用了getText方法
            if (UserStore.login(user,pass)) {
                UserSession.login(user);
                dispose();
                if (menu!=null) {
                    menu.refresh();
                }
                else{
                    new MainMenuFrame();
                }
            } else {
                new Dialog(this, "用户名或密码错误",400,380);
            }
        });

        // 注册操作
        register.addActionListener(e -> {
            new RegisterFrame(this);
            dispose();
        });

        //游客操作
        guest.addActionListener(e -> {
            UserSession.logout();
            dispose();
            if (menu!=null) {
                menu.refresh();
            } else{
                new MainMenuFrame();
            }
        });

        setVisible(true);
    }
}