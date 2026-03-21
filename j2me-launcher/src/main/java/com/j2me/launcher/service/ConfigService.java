package com.j2me.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.j2me.launcher.model.AppConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.config/j2me-launcher";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    private final ObjectMapper mapper;
    private AppConfig config;

    public ConfigService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadConfig();
    }

    public void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                config = mapper.readValue(configFile, AppConfig.class);
            } else {
                config = new AppConfig();
                config.setFreej2mePath(System.getProperty("user.home") + "/Desktop/emulator/freej2me/build/freej2me.jar");
                config.setWiePath(System.getProperty("user.home") + "/.cargo/bin/wie");
                saveConfig();
            }
        } catch (IOException e) {
            config = new AppConfig();
        }
    }

    public void saveConfig() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configPath)) Files.createDirectories(configPath);
            mapper.writeValue(new File(CONFIG_FILE), config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AppConfig getConfig() { return config; }
    public void updateConfig(AppConfig newConfig) { this.config = newConfig; saveConfig(); }
    public String getConfigDir() { return CONFIG_DIR; }
}
