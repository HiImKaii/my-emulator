package com.j2me.launcher.controller;

import com.j2me.launcher.model.*;
import com.j2me.launcher.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MainController {
    private final ConfigService configService = new ConfigService();
    private final GameScannerService scannerService = new GameScannerService();
    private final GameCacheService cacheService = new GameCacheService();
    private final GameLauncherService launcherService = new GameLauncherService(configService);

    private final ObservableList<GameInfo> allGames = FXCollections.observableArrayList();
    private final ObservableList<GameInfo> displayedGames = FXCollections.observableArrayList();
    private GameInfo selectedGame;
    private String currentView = "library";

    @FXML private Label lblStatus;
    @FXML private Label lblContentTitle;
    @FXML private TextField txtSearch;
    @FXML private GridPane gameGrid;
    @FXML private VBox sidePanel;
    @FXML private VBox gameDetailsPanel;
    @FXML private VBox settingsPanel;
    @FXML private TextField txtGamesFolder;
    @FXML private TextField txtFreej2mePath;
    @FXML private TextField txtWiePath;
    @FXML private Label lblGameTitle;
    @FXML private Label lblGameVendor;
    @FXML private Label lblGameType;
    @FXML private Spinner<Integer> spnWidth;
    @FXML private Spinner<Integer> spnHeight;
    @FXML private Spinner<Integer> spnFps;
    @FXML private CheckBox chkAudio;
    @FXML private Button btnLaunch;

    @FXML
    public void initialize() {
        spnWidth.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(120, 480, 240, 8));
        spnHeight.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(120, 800, 320, 8));
        spnFps.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 120, 60, 15));

        loadConfigToUI();

        String gamesFolder = configService.getConfig().getGamesFolder();
        if (gamesFolder != null && !gamesFolder.isEmpty()) {
            scanGames(gamesFolder);
        }
        updateStatus("Ready");
    }

    private void loadConfigToUI() {
        AppConfig config = configService.getConfig();
        txtGamesFolder.setText(config.getGamesFolder());
        txtFreej2mePath.setText(config.getFreej2mePath());
        txtWiePath.setText(config.getWiePath());
    }

    private void scanGames(String folderPath) {
        updateStatus("Scanning...");
        lblContentTitle.setText("Scanning...");

        CompletableFuture<List<GameInfo>> scanFuture = scannerService.scanFolder(folderPath);
        scanFuture.thenAccept(games -> {
            Platform.runLater(() -> {
                allGames.clear();
                allGames.addAll(games);
                filterAndDisplayGames();
                updateStatus("Found " + games.size() + " games");
                lblContentTitle.setText("Game Library (" + games.size() + ")");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                updateStatus("Error: " + ex.getMessage());
                lblContentTitle.setText("Game Library");
            });
            return null;
        });
    }

    private void filterAndDisplayGames() {
        String searchText = txtSearch != null ? txtSearch.getText() : "";
        List<GameInfo> filtered;

        if (searchText == null || searchText.isEmpty()) {
            filtered = currentView.equals("recent") ? cacheService.getRecentGamesWithInfo() : allGames;
        } else {
            String lowerSearch = searchText.toLowerCase();
            filtered = allGames.stream()
                    .filter(g -> g.getDisplayName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }
        displayedGames.setAll(filtered);
        refreshGameGrid();
    }

    private void refreshGameGrid() {
        gameGrid.getChildren().clear();
        int col = 0, row = 0, maxCols = 5;
        for (GameInfo game : displayedGames) {
            VBox card = createGameCard(game);
            gameGrid.add(card, col, row);
            col++;
            if (col >= maxCols) { col = 0; row++; }
        }
    }

    private VBox createGameCard(GameInfo game) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setMinSize(160, 180);
        card.setMaxSize(160, 180);
        card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-color: #333333; -fx-border-radius: 8; -fx-cursor: hand;");

        StackPane iconPane = new StackPane();
        iconPane.setMinSize(120, 120);
        iconPane.setMaxSize(120, 120);
        iconPane.setStyle("-fx-background-color: #333333; -fx-background-radius: 4;");
        Label iconLabel = new Label("📱");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #505050;");
        iconPane.getChildren().add(iconLabel);

        Label nameLabel = new Label(game.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-wrap-text: true; -fx-text-alignment: center;");
        nameLabel.setMaxWidth(140);
        nameLabel.setTextAlignment(TextAlignment.CENTER);

        Label typeLabel = new Label(getGameTypeText(game.getGameType()));
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #808080;");

        if (game.getLastPlayed() != null) {
            String dateStr = game.getLastPlayed().format(DateTimeFormatter.ofPattern("MMM dd"));
            Label playedLabel = new Label("▶ " + dateStr);
            playedLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a90d9;");
            card.getChildren().addAll(iconPane, nameLabel, typeLabel, playedLabel);
        } else {
            card.getChildren().addAll(iconPane, nameLabel, typeLabel);
        }

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) showGameDetails(game);
            else if (e.getClickCount() == 2) launchGame(game);
        });
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8; -fx-border-color: #4a90d9; -fx-border-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-color: #333333; -fx-border-radius: 8; -fx-cursor: hand;"));

        return card;
    }

    private String getGameTypeText(GameInfo.GameType type) {
        if (type == null) return "Unknown";
        switch (type) {
            case MIDLET: return "MIDlet";
            case WIPI: return "WIPI";
            case SKVM: return "SKVM";
            case KTF: return "KTF";
            case LGT: return "LGT";
            default: return "Unknown";
        }
    }

    private void showGameDetails(GameInfo game) {
        selectedGame = game;
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        gameDetailsPanel.setVisible(true);
        gameDetailsPanel.setManaged(true);
        sidePanel.setVisible(true);
        sidePanel.setManaged(true);

        lblGameTitle.setText(game.getDisplayName());
        lblGameVendor.setText("Vendor: " + (game.getVendor() != null ? game.getVendor() : "Unknown"));
        lblGameType.setText("Type: " + getGameTypeText(game.getGameType()));

        GameSettings settings = cacheService.getGameSettings(game.getJarPath());
        if (settings != null) {
            spnWidth.getValueFactory().setValue(settings.getWidth());
            spnHeight.getValueFactory().setValue(settings.getHeight());
            spnFps.getValueFactory().setValue(settings.getFpsCap());
            chkAudio.setSelected(settings.isAudioEnabled());
        } else {
            spnWidth.getValueFactory().setValue(game.getWidth());
            spnHeight.getValueFactory().setValue(game.getHeight());
            spnFps.getValueFactory().setValue(game.getFpsCap());
            chkAudio.setSelected(game.isAudioEnabled());
        }
        btnLaunch.setText("▶ Launch " + game.getGameType());
    }

    @FXML private void onLibraryClicked() {
        currentView = "library";
        lblContentTitle.setText("Game Library (" + allGames.size() + ")");
        filterAndDisplayGames();
        hideSidePanel();
    }

    @FXML private void onRecentClicked() {
        currentView = "recent";
        List<GameInfo> recent = cacheService.getRecentGamesWithInfo();
        lblContentTitle.setText("Recent (" + recent.size() + ")");
        filterAndDisplayGames();
        hideSidePanel();
    }

    @FXML private void onSettingsClicked() {
        gameDetailsPanel.setVisible(false);
        gameDetailsPanel.setManaged(false);
        settingsPanel.setVisible(true);
        settingsPanel.setManaged(true);
        sidePanel.setVisible(true);
        sidePanel.setManaged(true);
    }

    @FXML private void onSearchChanged() { filterAndDisplayGames(); }

    @FXML private void onRescanClicked() {
        String folder = txtGamesFolder.getText();
        if (folder != null && !folder.isEmpty()) scanGames(folder);
    }

    @FXML private void onLaunchClicked() {
        if (selectedGame != null) {
            saveCurrentGameSettings();
            launchGame(selectedGame);
        }
    }

    private void launchGame(GameInfo game) {
        updateStatus("Launching " + game.getDisplayName() + "...");
        cacheService.addRecentGame(game.getJarPath());
        cacheService.updateLastPlayed(game.getJarPath());

        // Launch detached — game runs independently, no need to wait
        launcherService.launchGame(game).thenAccept(success -> {
            Platform.runLater(() -> updateStatus(
                launcherService.getRunningGameCount() > 0
                    ? "Game running (" + launcherService.getRunningGameCount() + " instance(s))"
                    : "Game exited"
            ));
        });
    }

    private void saveCurrentGameSettings() {
        if (selectedGame == null) return;
        GameSettings settings = new GameSettings(selectedGame.getJarPath());
        settings.setWidth(spnWidth.getValue());
        settings.setHeight(spnHeight.getValue());
        settings.setFpsCap(spnFps.getValue());
        settings.setAudioEnabled(chkAudio.isSelected());
        selectedGame.setWidth(settings.getWidth());
        selectedGame.setHeight(settings.getHeight());
        selectedGame.setFpsCap(settings.getFpsCap());
        selectedGame.setAudioEnabled(settings.isAudioEnabled());
        cacheService.saveGameSettings(settings);
    }

    @FXML private void onBrowseGamesFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Games Folder");
        File selected = chooser.showDialog(lblStatus.getScene().getWindow());
        if (selected != null) txtGamesFolder.setText(selected.getAbsolutePath());
    }

    @FXML private void onBrowseFreej2me() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select FreeJ2ME JAR");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        File selected = chooser.showOpenDialog(lblStatus.getScene().getWindow());
        if (selected != null) txtFreej2mePath.setText(selected.getAbsolutePath());
    }

    @FXML private void onBrowseWie() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select WIE Binary");
        File selected = chooser.showOpenDialog(lblStatus.getScene().getWindow());
        if (selected != null) txtWiePath.setText(selected.getAbsolutePath());
    }

    @FXML private void onSaveSettings() {
        AppConfig config = configService.getConfig();
        config.setGamesFolder(txtGamesFolder.getText());
        config.setFreej2mePath(txtFreej2mePath.getText());
        config.setWiePath(txtWiePath.getText());
        configService.saveConfig();
        String folder = txtGamesFolder.getText();
        if (folder != null && !folder.isEmpty()) scanGames(folder);
        updateStatus("Settings saved");
    }

    @FXML private void onCloseSidePanel() { hideSidePanel(); }

    private void hideSidePanel() {
        sidePanel.setVisible(false);
        sidePanel.setManaged(false);
        gameDetailsPanel.setVisible(false);
        gameDetailsPanel.setManaged(false);
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        selectedGame = null;
    }

    private void updateStatus(String text) { lblStatus.setText(text); }

    public void shutdown() {
        scannerService.shutdown();
        launcherService.shutdown();
        cacheService.saveCache();
    }
}
