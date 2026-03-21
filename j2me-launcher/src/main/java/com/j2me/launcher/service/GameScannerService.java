package com.j2me.launcher.service;

import com.j2me.launcher.model.GameInfo;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;

public class GameScannerService {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, GameInfo> gameCache = new ConcurrentHashMap<>();

    public CompletableFuture<List<GameInfo>> scanFolder(String folderPath) {
        return CompletableFuture.supplyAsync(() -> {
            List<GameInfo> games = new ArrayList<>();
            Path path = Paths.get(folderPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) return games;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.{jar,jad}")) {
                for (Path file : stream) {
                    try {
                        GameInfo game = parseGameFile(file.toString());
                        if (game != null) {
                            GameInfo cached = gameCache.get(game.getJarPath());
                            if (cached != null) {
                                game.setLastPlayed(cached.getLastPlayed());
                                game.setPlayCount(cached.getPlayCount());
                                game.setWidth(cached.getWidth());
                                game.setHeight(cached.getHeight());
                                game.setFpsCap(cached.getFpsCap());
                                game.setAudioEnabled(cached.isAudioEnabled());
                            }
                            games.add(game);
                            gameCache.put(game.getJarPath(), game);
                        }
                    } catch (Exception e) { /* skip */ }
                }
            } catch (IOException e) { /* skip */ }

            games.sort(Comparator.comparing(GameInfo::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            return games;
        }, executor);
    }

    public GameInfo parseGameFile(String filePath) {
        try {
            if (filePath.toLowerCase().endsWith(".jar")) return parseJarFile(filePath);
            else if (filePath.toLowerCase().endsWith(".jad")) return parseJadFile(filePath);
        } catch (Exception e) { /* skip */ }
        return null;
    }

    private GameInfo parseJarFile(String jarPath) throws IOException {
        GameInfo game = new GameInfo(jarPath);
        try (JarFile jarFile = new JarFile(jarPath)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String midletName = attrs.getValue("MIDlet-Name");
                if (midletName != null) game.setName(midletName);
                game.setVendor(attrs.getValue("MIDlet-Vendor"));
                game.setVersion(attrs.getValue("MIDlet-Version"));

                if (attrs.getValue("WIPI-Version") != null) game.setGameType(GameInfo.GameType.WIPI);
                else if (attrs.getValue("SKVM-Version") != null) game.setGameType(GameInfo.GameType.SKVM);
                else if (attrs.getValue("KTF-Version") != null) game.setGameType(GameInfo.GameType.KTF);
                else if (attrs.getValue("LGT-Version") != null) game.setGameType(GameInfo.GameType.LGT);
                else game.setGameType(GameInfo.GameType.MIDLET);
            } else {
                game.setName(game.getFileName());
                game.setGameType(GameInfo.GameType.UNKNOWN);
            }
            String jadPath = jarPath.replace(".jar", ".jad");
            if (Files.exists(Paths.get(jadPath))) game.setJadPath(jadPath);
        }
        return game;
    }

    private GameInfo parseJadFile(String jadPath) throws IOException {
        GameInfo game = new GameInfo(jadPath.replace(".jad", ".jar"));
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(jadPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf(':');
                if (idx > 0) props.setProperty(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        if (props.getProperty("MIDlet-Name") != null) game.setName(props.getProperty("MIDlet-Name"));
        game.setVendor(props.getProperty("MIDlet-Vendor"));
        game.setVersion(props.getProperty("MIDlet-Version"));
        game.setJadPath(jadPath);

        if (props.getProperty("WIPI-Version") != null) game.setGameType(GameInfo.GameType.WIPI);
        else if (props.getProperty("SKVM-Version") != null) game.setGameType(GameInfo.GameType.SKVM);
        else if (props.getProperty("KTF-Version") != null) game.setGameType(GameInfo.GameType.KTF);
        else if (props.getProperty("LGT-Version") != null) game.setGameType(GameInfo.GameType.LGT);
        else game.setGameType(GameInfo.GameType.MIDLET);

        return game;
    }

    public void updateGameCache(GameInfo game) { gameCache.put(game.getJarPath(), game); }
    public void shutdown() { executor.shutdown(); }
}
