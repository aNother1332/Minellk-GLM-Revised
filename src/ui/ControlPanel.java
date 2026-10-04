package ui;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    StatusPanel statusPanel;
    BoardPanel boardPanel;
    GameFrame gameFrame;
    JButton startButton;
    JButton restartButton;
    int offSetX;
    int offSetY;
    int width;
    int height;

    private boolean gameStarted=false;

    private static final Color PANEL_BG=new Color(255, 255, 255);


    public ControlPanel(GameFrame gameFrame,StatusPanel statusPanel,BoardPanel boardPanel,int offSetX,int offSetY,int width,int height) {
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        this.setBounds(offSetX, offSetY, width, height);
        this.offSetX=offSetX;
        this.offSetY=offSetY;
        this.width=width;
        this.height=height;
        this.statusPanel=statusPanel;
        this.boardPanel=boardPanel;
        this.gameFrame=gameFrame;
        this.setBackground(PANEL_BG);

        this.startButton=new GameButton("开始游戏", 150, 50);
        this.restartButton=new GameButton("重新开始", 150, 50);

        restartButton.setEnabled(false);//初始状态下禁用重新开始按钮

        this.add(startButton);
        this.add(restartButton);

        this.startButton.addActionListener(e -> {
            if (gameStarted==false) {
                statusPanel.setStatus("游戏中");
                statusPanel.startTimer();
                boardPanel.startgame(true);
                gameStarted=true;
                gameFrame.getItemPanel().setItemsEnabled(true);
                startButton.setEnabled(false);
                restartButton.setEnabled(true);
            }
        });

       //重新开始按钮
        this.restartButton.addActionListener(e -> {
            if (gameStarted) {
                boardPanel.restartGame(); // Notify board to restart
            }
        });
    }
    //统一设置按钮样式
    public void resetControlState() {
        gameStarted=false;
        startButton.setEnabled(true);
        restartButton.setEnabled(false);
    }

}