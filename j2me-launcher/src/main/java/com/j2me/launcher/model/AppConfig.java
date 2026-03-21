package com.j2me.launcher.model;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    private String gamesFolder = "";
    private String freej2mePath = "";
    private String wiePath = "";
    private int defaultWidth = 240;
    private int defaultHeight = 320;
    private int defaultFpsCap = 60;
    private boolean defaultAudioEnabled = true;
    private List<String> recentGames = new ArrayList<>();
    private int maxRecentGames = 10;

    public String getGamesFolder() { return gamesFolder; }
    public void setGamesFolder(String gamesFolder) { this.gamesFolder = gamesFolder; }
    public String getFreej2mePath() { return freej2mePath; }
    public void setFreej2mePath(String freej2mePath) { this.freej2mePath = freej2mePath; }
    public String getWiePath() { return wiePath; }
    public void setWiePath(String wiePath) { this.wiePath = wiePath; }
    public int getDefaultWidth() { return defaultWidth; }
    public void setDefaultWidth(int defaultWidth) { this.defaultWidth = defaultWidth; }
    public int getDefaultHeight() { return defaultHeight; }
    public void setDefaultHeight(int defaultHeight) { this.defaultHeight = defaultHeight; }
    public int getDefaultFpsCap() { return defaultFpsCap; }
    public void setDefaultFpsCap(int defaultFpsCap) { this.defaultFpsCap = defaultFpsCap; }
    public boolean isDefaultAudioEnabled() { return defaultAudioEnabled; }
    public void setDefaultAudioEnabled(boolean defaultAudioEnabled) { this.defaultAudioEnabled = defaultAudioEnabled; }
    public List<String> getRecentGames() { return recentGames; }
    public void setRecentGames(List<String> recentGames) { this.recentGames = recentGames; }
    public int getMaxRecentGames() { return maxRecentGames; }
    public void setMaxRecentGames(int maxRecentGames) { this.maxRecentGames = maxRecentGames; }

    public void addRecentGame(String jarPath) {
        recentGames.remove(jarPath);
        recentGames.add(0, jarPath);
        if (recentGames.size() > maxRecentGames) {
            recentGames = new ArrayList<>(recentGames.subList(0, maxRecentGames));
        }
    }
}
