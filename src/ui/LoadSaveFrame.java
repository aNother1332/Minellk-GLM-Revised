package ui;
import app.UserSession;
import model.Record;
import model.RecordStore;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import static model.RecordStore.rightrecord;

public class LoadSaveFrame extends JFrame {
    private MainMenuFrame mainmenu;
    private static final String backgroundpath="resource/images/ruins.png";
    public LoadSaveFrame(MainMenuFrame mainmenu) {
        this.mainmenu =mainmenu;
        setTitle("读取存档");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(true);
        setUndecorated(false);

        JPanel bgPanel = new JPanel() {
            // 加载背景图片
            private final Image image = loadBackgroundImage();

            private Image loadBackgroundImage() {
                File file = new File(backgroundpath);
                return new ImageIcon(file.getPath()).getImage();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image !=null) {
                    // 图片铺满整个面板
                    g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        bgPanel.setLayout(new GridLayout(4, 1, 20, 20));//布局四行一列以及空格
        setContentPane(bgPanel);

        JLabel titleLabel=new JLabel("最近的三份存档", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel);

        List<Record> records=RecordStore.getrecords(UserSession.username, 3);

        if (records.isEmpty()) {
            JLabel norecord=new JLabel("暂无存档", SwingConstants.CENTER);
            norecord.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 28));
            norecord.setForeground(Color.WHITE);
            add(norecord);
        } else {
            for (int i=0;i<records.size();i++) {
                Record record=records.get(i);
                if (rightrecord(record)) {
                    add(enterrecord(record));//遍历所有的存档，添加进入存档的按钮
                } else {
                    JLabel brokenfile = new JLabel("发现损坏的存档文件", SwingConstants.CENTER);
                    brokenfile.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 18));
                    brokenfile.setForeground(Color.YELLOW);
                    add(brokenfile);
                }
            }
        }
        JButton closeBtn = new GameButton("返回主菜单", 220, 50);
        closeBtn.addActionListener(e -> {
            dispose();
            if (mainmenu!=null) {
                mainmenu.setVisible(true);
            }
        });
        add(closeBtn);
        setVisible(true);
    }

    private JButton enterrecord(Record record) {//进入存档的按钮
        String a=String.format("关卡：%d | 分数：%d", record.getLevel(), record.getScore());
        JButton enter = new GameButton(a, 650, 50);
        enter.addActionListener(e -> {
            if (rightrecord(record)==false) {//如果无效，报错
                JOptionPane.showMessageDialog(this, "存档无效", "错误", JOptionPane.ERROR_MESSAGE);
                dispose();
                if (mainmenu!=null) {
                    mainmenu.setVisible(true);
                }
                return;
            }
            dispose();//有效的话关闭这个界面
            if (mainmenu!=null) {
                mainmenu.dispose();
            }
            try {//如果有效，通过这个存档进入游戏
                new GameFrame(record);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "存档无效", "错误", JOptionPane.ERROR_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    new MainMenuFrame();
                });
            }
        });
        return enter;
    }
}