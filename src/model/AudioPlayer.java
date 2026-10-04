package model;

import javax.sound.sampled.*;
import java.io.File;


public class AudioPlayer {
    private static Clip background;
    private static Clip hit;
    private static Clip combo;

    //音频路径
    private static final String backgroundpath="resource/sounds/minecraft.wav";
    private static final String hitpath="resource/sounds/hit.wav";
    private static final String combopath="resource/sounds/levelup.wav";

    private static boolean music=false;

    // 程序启动时预加载所有音频
    static {
        try {
            background=load(backgroundpath);
            hit=load(hitpath);
            combo=load(combopath);
        } catch (Exception e) {
        }
    }

    // 加载音频工具方法（只预加载调用）
    private static Clip load(String filePath) {
        try {
            File audio=new File(filePath);
            AudioInputStream stream=AudioSystem.getAudioInputStream(audio);
            Clip clip=AudioSystem.getClip();
            clip.open(stream);
            // 音效播放完毕自动复位
            clip.addLineListener(event -> {
                if (event.getType()==LineEvent.Type.STOP) {
                    clip.setFramePosition(0);
                }
            });
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    // 背景音乐
    public static void backgroundsound() {
        if(music==false){
        music=true;
        background.setFramePosition(0);
        background.loop(Clip.LOOP_CONTINUOUSLY);//循环播放
    }
    }

    public static void stopmusic() {
        if (background!=null&&background.isRunning()) {
            background.stop();
            music = false;
        }
    }
    // 消除音效
    public static void hitsound() {
        hit.stop();
        hit.setFramePosition(0);
        hit.start();
    }

    // 连击音效
    public static void combosound() {
        combo.stop();
        combo.setFramePosition(0);
        combo.start();
    }
}