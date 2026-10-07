package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 本地设置（settings.txt）：音乐/音效音量，取值 0.0–1.0。
 * 读写均为纯文本键值对，供 AudioPlayer 在启动时应用。
 */
public final class Settings {

    private static final String FILE = "settings.txt";
    private static double musicVolume = 0.8;
    private static double sfxVolume = 1.0;

    private Settings() {}

    public static double getMusicVolume() {
        return musicVolume;
    }

    public static double getSfxVolume() {
        return sfxVolume;
    }

    /** 设置音量并持久化（值会被夹在 0.0–1.0） */
    public static void setMusicVolume(double v) {
        musicVolume = clamp(v);
        save();
    }

    public static void setSfxVolume(double v) {
        sfxVolume = clamp(v);
        save();
    }

    public static void load() {
        File file = new File(FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                try {
                    if ("music".equals(parts[0].trim())) {
                        musicVolume = clamp(Double.parseDouble(parts[1].trim()));
                    } else if ("sfx".equals(parts[0].trim())) {
                        sfxVolume = clamp(Double.parseDouble(parts[1].trim()));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            System.out.println("[settings] 读取失败: " + e.getMessage());
        }
    }

    private static void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE))) {
            writer.write("music=" + String.format("%.2f", musicVolume));
            writer.newLine();
            writer.write("sfx=" + String.format("%.2f", sfxVolume));
        } catch (IOException e) {
            System.out.println("[settings] 保存失败: " + e.getMessage());
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
