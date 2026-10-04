package ui;

import app.UserSession;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static model.AudioPlayer.combosound;

public class StatusPanel extends JPanel {
    private JLabel timeLabel;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JLabel pairsLeftLabel;

    private int remainingTime;
    private Timer timer;
    private int score;

    private Map<Integer, Integer> comboMap;

    private GameFrame gameFrame;

    public StatusPanel(GameFrame gameFrame, int x, int y, int width, int height) {
        this.gameFrame=gameFrame;
        this.setBounds(x,y,width,height);
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));

        comboMap=new HashMap<>();//初始化现在的连击数和总分
        score=0;

       //展示剩余时间
        timeLabel=new JLabel("剩余时间：05:00");
        timeLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
        timeLabel.setForeground(Color.BLACK);
        add(timeLabel);

        //展示游戏状态
        statusLabel=new JLabel("状态：未开始");
        statusLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
        statusLabel.setForeground(Color.BLACK); // Default foreground color
        add(statusLabel);

        //展示当前分数
        scoreLabel=new JLabel("当前分数：0");
        scoreLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
        scoreLabel.setForeground(Color.BLACK); // Default foreground color
        add(scoreLabel);

        //展示剩余可消除对
        pairsLeftLabel=new JLabel("剩余可消除对：--");
        pairsLeftLabel.setFont(new Font("Fusion Pixel 10px Mono zh_hans", Font.BOLD, 18));
        pairsLeftLabel.setForeground(Color.BLACK); // Default foreground color
        add(pairsLeftLabel);

        //初始化计时器，每隔1000毫秒执行一次倒计时
        timer=new Timer(1000, e -> updateCountdown());
    }

    //设置总倒计时
    public void settime(int seconds) {
        this.remainingTime = seconds;
        updatetime();
    }
    //恢复剩余时间
    public void recovertime(int seconds) {
        this.remainingTime=seconds;
        updatetime();
    }
    //每秒计算剩余时间
    private void updateCountdown() {
        if (remainingTime>0) {
            remainingTime--;
            updatetime();
        } else {
            timer.stop();
            statusLabel.setText("状态：时间到！");
            gameFrame.defeat("时间耗尽！");
        }
    }
    //刷新剩余时间显示
    private void updatetime() {
        int minutes=remainingTime/60;
        int seconds=remainingTime%60;
        timeLabel.setText(String.format("剩余时间：%02d:%02d", minutes, seconds));
    }
    //设置状态
    public void setStatus(String status) {
        statusLabel.setText("状态：" + status);
    }
    //启动计时器
    public void startTimer() {
        if (!timer.isRunning()) {
            timer.start();
            statusLabel.setText("状态：进行中");
        }
    }

    //结束计时器
    public void stopTimer() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    public int getRemainingTime() {
        return remainingTime;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score=score;
        scoreLabel.setText("当前分数：" + this.score);
    }

    public void addscore(int iconnumber) {
        //获取这个棋子之前的连击数
        int oldcombo=comboMap.getOrDefault(iconnumber, 0);
        //创建一个列表，遍历所有棋子，记录除了这个棋子以外的所有棋子
        Set<Integer> reset=new HashSet<>();
        for (Integer existingIconId:comboMap.keySet()) {
            if (existingIconId.equals(iconnumber)==false) {
                reset.add(existingIconId);
            }
        }
        //将列表里的所有棋子reset
        for (Integer reseticon : reset) {
            comboMap.put(reseticon, 0);
        }
        //将这个棋子的连击数+1
        int newcombo=oldcombo+1;
        comboMap.put(iconnumber, newcombo);
        int points=10;
        if(newcombo>=3) {
            points=(newcombo-1)*10;
            ItemPanel.points+=2;
            ItemPanel.updatepoint();
        }
        score+=points;
        scoreLabel.setText("当前分数：" + score);
        String name=getname(iconnumber);
        String actionMessage=String.format("消除了[%s]×2，+%d分",name,points);
        if (newcombo>=3) {
            combosound();
            actionMessage+=String.format(" (连消×%d，额外加分)",newcombo);
        }
        gameFrame.displayMessage(actionMessage);
        displayleft();
    }
    static String getname(int iconIndex) {
        String skin=UserSession.currentSkin;
        if ("nature".equals(skin)) {
            switch (iconIndex) {
                case 1: return "石头";
                case 2: return "橡木原木";
                case 3: return "草方块";
                case 4: return "煤矿石";
                case 5: return "橡木树叶";
                case 6: return "泥土";
                case 7: return "铁矿石";
                case 8: return "南瓜";
                case 9: return "沙子";
                case 10: return "钻石矿石";
                case 11: return "西瓜";
                case 12: return "雪方块";
                default: return "";
            }
        } else {
            switch (iconIndex) {
                case 1: return "熔炉";
                case 2: return "橡木木板";
                case 3: return "泥土路径";
                case 4: return "砖块";
                case 5: return "工作台";
                case 6: return "沙子";
                case 7: return "炼药锅";
                case 8: return "箱子";
                case 9: return "黏土块";
                case 10: return "砂轮";
                case 11: return "书架";
                case 12: return "干草块";
                default: return "";
            }
        }

    }
    public void resetScore() {
        score=0;
        scoreLabel.setText("当前分数：0");
        comboMap.clear();
    }
    public void displayleft() {
        int pairsLeft=gameFrame.pairsleft();
        pairsLeftLabel.setText("剩余可消除对：" + pairsLeft);
    }
}