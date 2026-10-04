package ui;

import model.Record;
import model.RecordStore;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class LeaderboardFrame extends JFrame {
    private static final String backgroundpath="resource/images/rank_bg.png";
    private static final String savepath="saves/";

    private Image backgroundImage;
    private GameButton back;
    private BackgroundPanel bgPanel;

    private static final Font title=new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 36);
    private static final Font head=new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 22);
    private static final Font wordfont=new Font("Fusion Pixel 10px Mono zh_hans", Font.PLAIN, 20);

    public LeaderboardFrame() {
        this.setTitle("游戏排行榜");
        this.setSize(800, 650);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//叉掉窗口之后仅关闭这个窗口，不关闭程序
        this.setLocationRelativeTo(null);//居中
        setUndecorated(true);//删掉标题栏
        this.setResizable(false);

        //背景图片
        this.backgroundImage=new ImageIcon(backgroundpath).getImage();

        //背景面板
        bgPanel=new BackgroundPanel();
        bgPanel.setLayout(null);
        bgPanel.setBounds(0, 0, 800, 650);
        this.add(bgPanel);

        // 初始化组件
        addbuttons();
        showdata();

        this.setVisible(true);
    }

    // 初始化固定组件：标题、表头、返回按钮
    private void addbuttons() {
        //标题
        JLabel title=new JLabel("排行榜", SwingConstants.CENTER);
        title.setFont(LeaderboardFrame.title);
        title.setForeground(Color.BLACK);
        title.setBounds(200, 40, 400, 60);
        bgPanel.add(title);

        //文字
        JLabel word1=new JLabel("排名", SwingConstants.CENTER);
        word1.setFont(LeaderboardFrame.head);
        word1.setForeground(Color.BLACK);
        word1.setBounds(120, 110, 120, 40);
        bgPanel.add(word1);
        //文字
        JLabel word2=new JLabel("玩家", SwingConstants.CENTER);
        word2.setFont(LeaderboardFrame.head);
        word2.setForeground(Color.BLACK);
        word2.setBounds(340, 110, 120, 40);
        bgPanel.add(word2);
        //文字
        JLabel word3=new JLabel("综合分", SwingConstants.CENTER);
        word3.setFont(LeaderboardFrame.head);
        word3.setForeground(Color.BLACK);
        word3.setBounds(560, 110, 120, 40);
        bgPanel.add(word3);
        // 返回按钮
        back=new GameButton("返回主界面", 220, 55);
        back.setBounds(290, 550, 220, 55);
        back.addActionListener(e ->{
                this.dispose();
        new MainMenuFrame().setVisible(true);});
        bgPanel.add(back);
    }

    // 加载排行榜数据
    private void showdata() {
        Map<String, Record> bestrecord=new HashMap<>();
        File saveFolder=new File(savepath);
        File[] files=saveFolder.listFiles((dir, name) -> name.endsWith("_saves.txt"));
        if (files!=null) {
                for (File file:files) {
                    String fileName=file.getName();//获取文件的名字
                    String username=fileName.replace("_saves.txt", "");//从文件名获取用户名
                    List<Record> allrecords=RecordStore.allrecords(username);
                    for (Record record:allrecords) {
                        double score=calculatescore(record);
                        if (bestrecord.containsKey(username)==false||score>calculatescore(bestrecord.get(username))) {
                            //如果这个用户还没有过成绩，或者这次的成绩高于最好成绩，那么存这次的
                            bestrecord.put(username, record);
                        }
                    }
                }
        }
        // 无数据提示
        if (bestrecord.isEmpty()) {
            JLabel emptyTip = new JLabel("暂无排行榜数据", SwingConstants.CENTER);
            emptyTip.setFont(wordfont);
            emptyTip.setForeground(Color.BLACK);
            emptyTip.setBounds(250, 220, 300, 40);
            bgPanel.add(emptyTip);
            return;
        }

        // 排序
        List<Map.Entry<String, Record>> rankList = new ArrayList<>(bestrecord.entrySet());
        rankList.sort((a, b) -> Double.compare(calculatescore(b.getValue()), calculatescore(a.getValue())));

        // 动态生成排行文字
        int startY = 200;
        final int rowHeight = 35;

        for (int i = 0; i < rankList.size(); i++) {
            Record record = rankList.get(i).getValue();
            double totalScore = calculatescore(record);
            String scoreStr = String.format("%.2f", totalScore);

            // 排名
            JLabel rankLabel = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
            rankLabel.setFont(wordfont);
            rankLabel.setForeground(Color.BLACK);
            rankLabel.setBounds(120, startY, 120, rowHeight);
            bgPanel.add(rankLabel);

            // 用户名
            JLabel userLabel = new JLabel(record.getUsername(), SwingConstants.CENTER);
            userLabel.setFont(wordfont);
            userLabel.setForeground(Color.BLACK);
            userLabel.setBounds(340, startY, 120, rowHeight);
            bgPanel.add(userLabel);

            // 综合分
            JLabel scoreLabel = new JLabel(scoreStr, SwingConstants.CENTER);
            scoreLabel.setFont(wordfont);
            scoreLabel.setForeground(Color.BLACK);
            scoreLabel.setBounds(560, startY, 120, rowHeight);
            bgPanel.add(scoreLabel);

            startY += rowHeight;
            if (startY > 500) break;
        }
    }

    // 综合分计算
    private double calculatescore(Record record) {
        int score=record.getScore();
        int time=record.getLefttime();
        int difficulty=record.getDifficulty();
        double totalscore, totaltime;
        if (difficulty==1) {
            totalscore=160;
            totaltime=100;
            double Score=(score/totalscore)*40;
            double Time=(time/totaltime)*35;
            return (Score+Time)*1.2;
        } else {
            totalscore=500;
            totaltime=150;
            double Score=(score/totalscore)*50;
            double Time=(time/totaltime)*50;
            return (Score+Time)*1.5;
        }
    }

    private class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}