package com.j2me.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.j2me.launcher.model.GameInfo;
import com.j2me.launcher.model.GameSettings;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class GameCacheService {
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.cache/j2me-launcher";
    private static final String GAMES_CACHE_FILE = CACHE_DIR + "/games.json";
    private static final String RECENT_FILE = CACHE_DIR + "/recent.json";
    private final ObjectMapper mapper;
    private final Map<String, GameInfo> gameCache = new HashMap<>();
    private final List<String> recentGames = new ArrayList<>();

    public GameCacheService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadCache();
    }

    private void loadCache() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) Files.createDirectories(cachePath);

            File gamesFile = new File(GAMES_CACHE_FILE);
            if (gamesFile.exists()) {
                GameInfo[] games = mapper.readValue(gamesFile, GameInfo[].class);
                for (GameInfo game : games) gameCache.put(game.getJarPath(), game);
            }

            File recentFile = new File(RECENT_FILE);
            if (recentFile.exists()) {
                String[] recent = mapper.readValue(recentFile, String[].class);
                recentGames.addAll(Arrays.asList(recent));
            }
        } catch (Exception e) { /* skip */ }
    }

    public void saveCache() {
        try {
            mapper.writeValue(new File(GAMES_CACHE_FILE), gameCache.values().toArray(new GameInfo[0]));
            mapper.writeValue(new File(RECENT_FILE), recentGames.toArray(new String[0]));
        } catch (Exception e) { /* skip */ }
    }

    public GameInfo getGame(String jarPath) { return gameCache.get(jarPath); }
    public void updateGame(GameInfo game) { gameCache.put(game.getJarPath(), game); saveCache(); }

    public void addRecentGame(String jarPath) {
        recentGames.remove(jarPath);
        recentGames.add(0, jarPath);
        if (recentGames.size() > 10) recentGames.remove(10);
        saveCache();
    }

    public List<String> getRecentGames() { return new ArrayList<>(recentGames); }

    public List<GameInfo> getRecentGamesWithInfo() {
        List<GameInfo> recent = new ArrayList<>();
        for (String path : recentGames) {
            GameInfo game = gameCache.get(path);
            if (game != null) recent.add(game);
        }
        return recent;
    }

    public void updateLastPlayed(String jarPath) {
        GameInfo game = gameCache.get(jarPath);
        if (game != null) {
            game.setLastPlayed(LocalDateTime.now());
            game.incrementPlayCount();
            saveCache();
        }
    }

    public GameSettings getGameSettings(String jarPath) {
        GameInfo game = gameCache.get(jarPath);
        if (game == null) return null;
        GameSettings settings = new GameSettings(jarPath);
        settings.setWidth(game.getWidth());
        settings.setHeight(game.getHeight());
        settings.setFpsCap(game.getFpsCap());
        settings.setAudioEnabled(game.isAudioEnabled());
        return settings;
    }

    public void saveGameSettings(GameSettings settings) {
        GameInfo game = gameCache.get(settings.getJarPath());
        if (game != null) {
            game.setWidth(settings.getWidth());
            game.setHeight(settings.getHeight());
            game.setFpsCap(settings.getFpsCap());
            game.setAudioEnabled(settings.isAudioEnabled());
            saveCache();
        }
    }
}
