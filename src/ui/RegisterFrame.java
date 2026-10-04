package ui;

import app.UserStore;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RegisterFrame extends JFrame {
    private JTextField username;
    private JPasswordField passname;
    private static final String imagepath="resource/images/login_bg2.png";
    private static final Color wordcolor=new Color(255, 255, 255);//文字颜色
    private static final Color btncolor=new Color(50, 50, 50);//按钮颜色
    private static final Font uifont=new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 16);

    public RegisterFrame(LoginFrame loginFrame) {
        setTitle("注册");
        setSize(400, 380);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);//关掉上面那个标题栏
        setAlwaysOnTop(true);//始终显示在所有窗口的最上方
        setLayout(null);
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
        JLabel userlabel=new JLabel("用户名");
        userlabel.setFont(uifont);
        userlabel.setForeground(wordcolor);
        userlabel.setBounds(50, 140, 80, 30);
        bgPanel.add(userlabel);
        //输入用户名
        username=new JTextField();
        username.setFont(uifont);
        username.setBackground(btncolor);
        username.setForeground(wordcolor);
        username.setCaretColor(wordcolor);
        username.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        username.setBounds(140, 140, 200, 30);
        bgPanel.add(username);
        //密码
        JLabel passlabel=new JLabel("密码");
        passlabel.setFont(uifont);
        passlabel.setForeground(wordcolor);
        passlabel.setBounds(50, 190, 80, 30);
        bgPanel.add(passlabel);
        //输入密码
        passname=new JPasswordField();
        passname.setFont(uifont);
        passname.setBackground(btncolor);
        passname.setForeground(wordcolor);
        passname.setCaretColor(wordcolor);
        passname.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        passname.setBounds(140, 190, 200, 30);
        bgPanel.add(passname);
        //注册按钮
        JButton register=new GameButton("注册", 120,40);
        register.setBounds(140, 240, 120, 40);
        add(register);
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
        //按下注册按钮
        register.addActionListener(e -> {
            String user=username.getText();
            String pass=new String(passname.getPassword());
            if (user.isEmpty()||pass.isEmpty()) {
                new Dialog(this, "不能为空",400 ,380 );
                return;
            }
            boolean success=UserStore.register(user, pass);
            if (success) {
                new Dialog(this, "注册成功",400,380);
                dispose();
                new LoginFrame(null);
            } else {
                new Dialog(this, "用户名已存在",400,380);
            }
        });
        setVisible(true);
    }
}