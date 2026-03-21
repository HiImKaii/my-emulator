package com.j2me.launcher.service;

import com.j2me.launcher.model.GameInfo;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class GameLauncherService {
    private final ConfigService configService;
    // Track all running game processes for multi-instance support
    private final Set<Process> activeProcesses = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GameLauncherService(ConfigService configService) {
        this.configService = configService;
    }

    public CompletableFuture<Boolean> launchGame(GameInfo game) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String freej2mePath = configService.getConfig().getFreej2mePath();
                String wiePath = configService.getConfig().getWiePath();

                if (game.getGameType() == GameInfo.GameType.WIPI ||
                    game.getGameType() == GameInfo.GameType.SKVM ||
                    game.getGameType() == GameInfo.GameType.KTF ||
                    game.getGameType() == GameInfo.GameType.LGT) {
                    return launchWithWie(game, wiePath);
                } else {
                    return launchWithFreeJ2ME(game, freej2mePath);
                }
            } catch (Exception e) {
                System.err.println("Failed to launch: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    private boolean launchWithFreeJ2ME(GameInfo game, String freej2mePath) throws IOException {
        File freej2meJar = new File(freej2mePath);
        if (!freej2meJar.exists()) {
            System.err.println("FreeJ2ME not found: " + freej2mePath);
            return false;
        }

        int width = game.getWidth();
        int height = game.getHeight();
        int scale = Math.max(1, Math.min(4, 480 / width));

        String jarPath = new File(game.getJarPath()).toURI().toString();

        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
            "-jar", freej2mePath,
            jarPath,
            String.valueOf(width),
            String.valueOf(height),
            String.valueOf(scale)
        );
        pb.directory(freej2meJar.getParentFile());
        pb.redirectErrorStream(true);

        System.out.println("Launching: " + game.getDisplayName());
        Process process = pb.start();

        // Register process and spawn a non-blocking wait-for cleanup thread
        activeProcesses.add(process);
        executor.submit(() -> {
            try {
                int exitCode = process.waitFor();
                System.out.println("FreeJ2ME exited: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeProcesses.remove(process);
            }
        });

        return true; // Process started successfully, not waiting for it to finish
    }

    private boolean launchWithWie(GameInfo game, String wiePath) throws IOException {
        File wieBinary = new File(wiePath);
        if (!wieBinary.exists()) {
            System.err.println("WIE not found: " + wiePath);
            return false;
        }

        ProcessBuilder pb = new ProcessBuilder(wiePath, game.getJarPath());
        pb.redirectErrorStream(true);

        System.out.println("Launching with WIE: " + game.getDisplayName());
        Process process = pb.start();

        activeProcesses.add(process);
        executor.submit(() -> {
            try {
                int exitCode = process.waitFor();
                System.out.println("WIE exited: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeProcesses.remove(process);
            }
        });

        return true;
    }

    // Kill all running game processes (called before launching a new one from the same game slot)
    public void killAllGames() {
        for (Process p : activeProcesses) {
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        }
        activeProcesses.clear();
    }

    public boolean isGameRunning() {
        return !activeProcesses.isEmpty();
    }

    public int getRunningGameCount() {
        return activeProcesses.size();
    }

    // Session separation: do NOT kill games on launcher close, just stop waiting
    public void shutdown() {
        executor.shutdown();
    }
}
