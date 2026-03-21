package com.j2me.launcher.model;

import java.time.LocalDateTime;

public class GameInfo {
    private String id;
    private String name;
    private String jarPath;
    private String jadPath;
    private String vendor;
    private String version;
    private GameType gameType = GameType.MIDLET;
    private int width = 240;
    private int height = 320;
    private int fpsCap = 60;
    private boolean audioEnabled = true;
    private LocalDateTime lastPlayed;
    private int playCount = 0;

    public enum GameType { MIDLET, WIPI, SKVM, KTF, LGT, UNKNOWN }

    public GameInfo() { this.id = java.util.UUID.randomUUID().toString(); }
    public GameInfo(String jarPath) { this(); this.jarPath = jarPath; }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJarPath() { return jarPath; }
    public void setJarPath(String jarPath) { this.jarPath = jarPath; }
    public String getJadPath() { return jadPath; }
    public void setJadPath(String jadPath) { this.jadPath = jadPath; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public GameType getGameType() { return gameType; }
    public void setGameType(GameType gameType) { this.gameType = gameType; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getFpsCap() { return fpsCap; }
    public void setFpsCap(int fpsCap) { this.fpsCap = fpsCap; }
    public boolean isAudioEnabled() { return audioEnabled; }
    public void setAudioEnabled(boolean audioEnabled) { this.audioEnabled = audioEnabled; }
    public LocalDateTime getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(LocalDateTime lastPlayed) { this.lastPlayed = lastPlayed; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public void incrementPlayCount() { this.playCount++; }

    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : getFileName();
    }

    public String getFileName() {
        if (jarPath == null) return "Unknown";
        int sep = Math.max(jarPath.lastIndexOf('/'), jarPath.lastIndexOf('\\'));
        String fileName = jarPath.substring(sep + 1);
        return fileName.replace(".jar", "").replace(".jad", "");
    }
}
