package com.j2me.launcher.model;

public class GameSettings {
    private String jarPath;
    private int width = 240;
    private int height = 320;
    private int fpsCap = 60;
    private boolean audioEnabled = true;

    public GameSettings() {}
    public GameSettings(String jarPath) { this.jarPath = jarPath; }

    public String getJarPath() { return jarPath; }
    public void setJarPath(String jarPath) { this.jarPath = jarPath; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getFpsCap() { return fpsCap; }
    public void setFpsCap(int fpsCap) { this.fpsCap = fpsCap; }
    public boolean isAudioEnabled() { return audioEnabled; }
    public void setAudioEnabled(boolean audioEnabled) { this.audioEnabled = audioEnabled; }
}
