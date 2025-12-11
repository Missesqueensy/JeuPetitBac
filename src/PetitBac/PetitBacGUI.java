
package PetitBac;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetitBacGUI extends Application {

    private TextArea consoleArea;
    private Label statusLabel;
    private Label roundLabel;
    private Label letterLabel;
    private ProgressBar progressBar;
    private VBox player1Box;
    private VBox player2Box;
    private Label score1Label;
    private Label score2Label;
    private Button startButton;
    private Button stopButton;
    
    // Nouvelle grille de jeu
    private GridPane gameGrid;
    private final String[] THEMES = {"Country", "City", "GirlName", "BoyName", "Fruit", "Color", "Object"};
    
    // Stocker les scores globaux
    private int gamer1TotalScore = 0;
    private int gamer2TotalScore = 0;
    
    private AgentContainer container;
    private boolean gameRunning = false;
    private Timeline progressTimeline;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🎮 PETIT BAC - Jeu Multi-Agents");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #16213e, #0f3460);");

        VBox header = createHeader();
        root.setTop(header);

        HBox center = createGameBoard();
        root.setCenter(center);

        VBox bottom = createConsole();
        root.setBottom(bottom);

        VBox rightPanel = createControlPanel();
        root.setRight(rightPanel);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            stopGame();
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        redirectSystemOut();
        
        consoleArea.appendText("===========================================\n");
        consoleArea.appendText("  PETIT BAC - Prêt à démarrer\n");
        consoleArea.appendText("===========================================\n\n");
    }

    private VBox createHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 15;");

        Label titleLabel = new Label("🎮 PETIT BAC");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.web("#00d4ff"));
        titleLabel.setEffect(createGlowEffect());

        Label subtitleLabel = new Label("Jeu Multi-Agents avec JADE");
        subtitleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web("#a8dadc"));

        statusLabel = new Label("⏸ En attente de démarrage");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#ffd60a"));

        header.getChildren().addAll(titleLabel, subtitleLabel, statusLabel);
        return header;
    }

    /*private VBox createGameBoard() {
        VBox board = new VBox(15);
        board.setPadding(new Insets(15));
        board.setAlignment(Pos.TOP_CENTER);

        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);

        VBox roundBox = createInfoCard("MANCHE", "0/3");
        roundLabel = (Label) ((VBox) roundBox.getChildren().get(0)).getChildren().get(1);
        
        VBox letterBox = createInfoCard("LETTRE", "?");
        letterLabel = (Label) ((VBox) letterBox.getChildren().get(0)).getChildren().get(1);

        infoBox.getChildren().addAll(roundBox, letterBox);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);
        progressBar.setStyle("-fx-accent: #00d4ff;");

        // Nouvelle grille de jeu
        gameGrid = createGameGrid();

        HBox playersBox = new HBox(40);
        playersBox.setAlignment(Pos.CENTER);
        playersBox.setPadding(new Insets(10));

        player1Box = createPlayerCard("Gamer1", "BFS", Color.web("#06ffa5"));
        player2Box = createPlayerCard("Gamer2", "A*", Color.web("#ff006e"));

        playersBox.getChildren().addAll(player1Box, player2Box);

        board.getChildren().addAll(infoBox, progressBar, gameGrid, playersBox);
        return board;
    }*/
    
    private HBox createGameBoard() {
        HBox mainBoard = new HBox(20); // Changé de VBox à HBox
        mainBoard.setPadding(new Insets(15));
        mainBoard.setAlignment(Pos.CENTER);
        
        // Créer les cartes des joueurs
        player1Box = createPlayerCard("Gamer1", "BFS", Color.web("#06ffa5"));
        player2Box = createPlayerCard("Gamer2", "A*", Color.web("#ff006e"));
        
        // Créer le centre avec la grille
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.TOP_CENTER);
        
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);

        VBox roundBox = createInfoCard("MANCHE", "0/3");
        roundLabel = (Label) ((VBox) roundBox.getChildren().get(0)).getChildren().get(1);
        
        VBox letterBox = createInfoCard("LETTRE", "?");
        letterLabel = (Label) ((VBox) letterBox.getChildren().get(0)).getChildren().get(1);

        infoBox.getChildren().addAll(roundBox, letterBox);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);
        progressBar.setStyle("-fx-accent: #00d4ff;");

        // Nouvelle grille de jeu
        gameGrid = createGameGrid();
        
        centerContent.getChildren().addAll(infoBox, progressBar, gameGrid);
        
        // Ajouter tout dans l'HBox principale
        mainBoard.getChildren().addAll(player1Box, centerContent, player2Box);
        
        return mainBoard; // Retourne HBox au lieu de VBox
    }


    private GridPane createGameGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // En-tête de colonnes
        Label headerTheme = createGridHeader("THÈME");
        GridPane.setConstraints(headerTheme, 0, 0);
        
        Label headerGamer1 = createGridHeader("GAMER1");
        headerGamer1.setTextFill(Color.web("#06ffa5"));
        GridPane.setConstraints(headerGamer1, 1, 0);
        
        Label headerPts1 = createGridHeader("PTS");
        GridPane.setConstraints(headerPts1, 2, 0);
        
        Label headerGamer2 = createGridHeader("GAMER2");
        headerGamer2.setTextFill(Color.web("#ff006e"));
        GridPane.setConstraints(headerGamer2, 3, 0);
        
        Label headerPts2 = createGridHeader("PTS");
        GridPane.setConstraints(headerPts2, 4, 0);

        grid.getChildren().addAll(headerTheme, headerGamer1, headerPts1, headerGamer2, headerPts2);

        // Lignes pour chaque thème
        for (int i = 0; i < THEMES.length; i++) {
            int row = i + 1;
            
            // Thème
            Label themeLabel = createGridCell(THEMES[i]);
            themeLabel.setStyle("-fx-background-color: rgba(0, 212, 255, 0.2); " +
                              "-fx-border-color: rgba(255, 255, 255, 0.3); " +
                              "-fx-border-width: 1; -fx-padding: 8; " +
                              "-fx-font-weight: bold;");
            themeLabel.setPrefWidth(150);
            GridPane.setConstraints(themeLabel, 0, row);
            
            // Gamer1 mot
            Label gamer1Word = createGridCell("---");
            gamer1Word.setId("gamer1-" + THEMES[i]);
            gamer1Word.setPrefWidth(150);
            GridPane.setConstraints(gamer1Word, 1, row);
            
            // Gamer1 points
            Label gamer1Pts = createGridCell("0");
            gamer1Pts.setId("gamer1-pts-" + THEMES[i]);
            gamer1Pts.setStyle("-fx-background-color: rgba(6, 255, 165, 0.2); " +
                             "-fx-border-color: rgba(255, 255, 255, 0.3); " +
                             "-fx-border-width: 1; -fx-padding: 8; " +
                             "-fx-font-weight: bold; -fx-text-fill: #06ffa5;");
            gamer1Pts.setPrefWidth(60);
            GridPane.setConstraints(gamer1Pts, 2, row);
            
            // Gamer2 mot
            Label gamer2Word = createGridCell("---");
            gamer2Word.setId("gamer2-" + THEMES[i]);
            gamer2Word.setPrefWidth(150);
            GridPane.setConstraints(gamer2Word, 3, row);
            
            // Gamer2 points
            Label gamer2Pts = createGridCell("0");
            gamer2Pts.setId("gamer2-pts-" + THEMES[i]);
            gamer2Pts.setStyle("-fx-background-color: rgba(255, 0, 110, 0.2); " +
                             "-fx-border-color: rgba(255, 255, 255, 0.3); " +
                             "-fx-border-width: 1; -fx-padding: 8; " +
                             "-fx-font-weight: bold; -fx-text-fill: #ff006e;");
            gamer2Pts.setPrefWidth(60);
            GridPane.setConstraints(gamer2Pts, 4, row);
            
            grid.getChildren().addAll(themeLabel, gamer1Word, gamer1Pts, gamer2Word, gamer2Pts);
        }

        return grid;
    }

    private Label createGridHeader(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#00d4ff"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPrefHeight(40);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); " +
                      "-fx-border-color: rgba(0, 212, 255, 0.5); " +
                      "-fx-border-width: 2; -fx-padding: 8;");
        return label;
    }

    private Label createGridCell(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        label.setTextFill(Color.WHITE);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPrefHeight(35);
        label.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); " +
                      "-fx-border-color: rgba(255, 255, 255, 0.3); " +
                      "-fx-border-width: 1; -fx-padding: 8;");
        return label;
    }

    private void updateGameGrid(String player, String theme, String word, int points) {
        Platform.runLater(() -> {
            // Mise à jour du mot
            Label wordLabel = (Label) gameGrid.lookup("#" + player.toLowerCase() + "-" + theme);
            if (wordLabel != null) {
                String displayWord = word.equals("---") ? "---" : word;
                wordLabel.setText(displayWord);
                
                // Animation seulement si un mot valide est ajouté
                if (!displayWord.equals("---")) {
                    wordLabel.setScaleX(1.2);
                    wordLabel.setScaleY(1.2);
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(300), 
                            new KeyValue(wordLabel.scaleXProperty(), 1.0),
                            new KeyValue(wordLabel.scaleYProperty(), 1.0))
                    );
                    timeline.play();
                }
            }
            
            // Mise à jour des points
            Label ptsLabel = (Label) gameGrid.lookup("#" + player.toLowerCase() + "-pts-" + theme);
            if (ptsLabel != null) {
                ptsLabel.setText(String.valueOf(points));
                
                // Animation des points
                if (points > 0) {
                    ptsLabel.setScaleX(1.5);
                    ptsLabel.setScaleY(1.5);
                    ptsLabel.setTextFill(Color.YELLOW);
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(400), 
                            new KeyValue(ptsLabel.scaleXProperty(), 1.0),
                            new KeyValue(ptsLabel.scaleYProperty(), 1.0))
                    );
                    timeline.setOnFinished(e -> {
                        if (player.equals("Gamer1")) {
                            ptsLabel.setTextFill(Color.web("#06ffa5"));
                        } else {
                            ptsLabel.setTextFill(Color.web("#ff006e"));
                        }
                    });
                    timeline.play();
                }
            }
        });
    }

    private void clearGameGrid() {
        Platform.runLater(() -> {
            for (String theme : THEMES) {
                // Reset Gamer1
                Label g1Word = (Label) gameGrid.lookup("#gamer1-" + theme);
                if (g1Word != null) g1Word.setText("---");
                
                Label g1Pts = (Label) gameGrid.lookup("#gamer1-pts-" + theme);
                if (g1Pts != null) {
                    g1Pts.setText("0");
                    g1Pts.setTextFill(Color.web("#06ffa5"));
                }
                
                // Reset Gamer2
                Label g2Word = (Label) gameGrid.lookup("#gamer2-" + theme);
                if (g2Word != null) g2Word.setText("---");
                
                Label g2Pts = (Label) gameGrid.lookup("#gamer2-pts-" + theme);
                if (g2Pts != null) {
                    g2Pts.setText("0");
                    g2Pts.setTextFill(Color.web("#ff006e"));
                }
            }
        });
    }

    private VBox createInfoCard(String title, String value) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                     "-fx-background-radius: 15; " +
                     "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                     "-fx-border-radius: 15; " +
                     "-fx-border-width: 2;");
        card.setPrefWidth(180);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setTextFill(Color.web("#a8dadc"));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.WHITE);

        VBox content = new VBox(3);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(titleLabel, valueLabel);
        
        card.getChildren().add(content);
        return card;
    }

    /*private VBox createPlayerCard(String name, String algo, Color accentColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); " +
                     "-fx-background-radius: 20; " +
                     "-fx-border-color: " + toRgbString(accentColor) + "; " +
                     "-fx-border-radius: 20; " +
                     "-fx-border-width: 3;");
        card.setPrefWidth(100);
        card.setPrefHeight(80);

        Label nameLabel = new Label("👤 " + name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setTextFill(accentColor);

        Label algoLabel = new Label("Algorithme: " + algo);
        algoLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        algoLabel.setTextFill(Color.web("#a8dadc"));

        Label scoreTitle = new Label("SCORE TOTAL");
        scoreTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        scoreTitle.setTextFill(Color.web("#a8dadc"));

        Label scoreValue = new Label("0");
        scoreValue.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        scoreValue.setTextFill(Color.WHITE);
        
        if (name.equals("Gamer1")) {
            score1Label = scoreValue;
        } else {
            score2Label = scoreValue;
        }

        Label statusIcon = new Label("⏸");
        statusIcon.setFont(Font.font("Arial", 24));

        card.getChildren().addAll(nameLabel, algoLabel, scoreTitle, scoreValue, statusIcon);
        return card;
    }*/
    private VBox createPlayerCard(String name, String algo, Color accentColor) {
        VBox card = new VBox(10); // Espacement augmenté
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15)); // Padding augmenté
        card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); " +
                     "-fx-background-radius: 20; " +
                     "-fx-border-color: " + toRgbString(accentColor) + "; " +
                     "-fx-border-radius: 20; " +
                     "-fx-border-width: 3;");
        card.setPrefWidth(150); // Largeur augmentée
        card.setPrefHeight(180); // Hauteur augmentée

        Label nameLabel = new Label("👤 " + name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18)); // Police plus grande
        nameLabel.setTextFill(accentColor);

        Label algoLabel = new Label("Algorithme: " + algo);
        algoLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14)); // Police plus grande
        algoLabel.setTextFill(Color.web("#a8dadc"));

        // Ajouter une ligne de séparation
        Separator separator = new Separator();
        separator.setPrefWidth(120);
        separator.setStyle("-fx-background-color: " + toRgbString(accentColor.brighter()) + ";");

        Label scoreTitle = new Label("SCORE TOTAL");
        scoreTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // Police plus grande
        scoreTitle.setTextFill(Color.web("#a8dadc"));

        Label scoreValue = new Label("0");
        scoreValue.setFont(Font.font("Arial", FontWeight.BOLD, 28)); // Police plus grande
        scoreValue.setTextFill(Color.WHITE);
        
        if (name.equals("Gamer1")) {
            score1Label = scoreValue;
        } else {
            score2Label = scoreValue;
        }

        // Ajouter une icône de statut
        Label statusIcon = new Label("⏸");
        statusIcon.setFont(Font.font("Arial", 24));
        statusIcon.setTextFill(accentColor);

        card.getChildren().addAll(nameLabel, algoLabel, separator, scoreTitle, scoreValue, statusIcon);
        return card;
    }

    private VBox createConsole() {
        VBox consoleBox = new VBox(5);
        consoleBox.setPadding(new Insets(5));
        consoleBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        Label consoleTitle = new Label("📋 Console des Agents");
        consoleTitle.setFont(Font.font("Arial", FontWeight.BOLD, 7));
        consoleTitle.setTextFill(Color.web("#00d4ff"));

        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(80);
        consoleArea.setStyle("-fx-control-inner-background: #1a1a1a; " +
                            "-fx-text-fill: #00ff00; " +
                            "-fx-font-family: 'Consolas', 'Monaco', monospace; " +
                            "-fx-font-size: 11px;");
        consoleArea.setWrapText(true);

        consoleBox.getChildren().addAll(consoleTitle, consoleArea);
        return consoleBox;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(200);
        panel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");

        Label controlTitle = new Label("🎮 CONTRÔLES");
        controlTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        controlTitle.setTextFill(Color.web("#00d4ff"));

        startButton = createStyledButton("▶ DÉMARRER", "#06ffa5");
        startButton.setOnAction(e -> startGame());

        stopButton = createStyledButton("⏹ ARRÊTER", "#ff006e");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopGame());

        Button resetButton = createStyledButton("🔄 RÉINITIALISER", "#ffd60a");
        resetButton.setOnAction(e -> resetGame());

        Separator separator = new Separator();
        separator.setPrefWidth(180);

        Label infoTitle = new Label("ℹ INFORMATIONS");
        infoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        infoTitle.setTextFill(Color.web("#a8dadc"));

        Label info1 = new Label("• 3 manches par partie");
        Label info2 = new Label("• 7 thèmes à remplir");
        Label info3 = new Label("• Bonus STOP: +20 pts");
        
        info1.setTextFill(Color.WHITE);
        info1.setFont(Font.font("Arial", 12));
        info2.setTextFill(Color.WHITE);
        info2.setFont(Font.font("Arial", 12));
        info3.setTextFill(Color.WHITE);
        info3.setFont(Font.font("Arial", 12));

        panel.getChildren().addAll(
            controlTitle, startButton, stopButton, resetButton,
            separator, infoTitle, info1, info2, info3
        );

        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefWidth(180);
        button.setPrefHeight(45);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setStyle("-fx-background-color: " + color + "; " +
                       "-fx-text-fill: white; " +
                       "-fx-background-radius: 10; " +
                       "-fx-cursor: hand;");
        
        button.setOnMouseEntered(e -> {
            button.setEffect(createGlowEffect());
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        
        button.setOnMouseExited(e -> {
            button.setEffect(null);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        return button;
    }

    private void startGame() {
        if (gameRunning) return;

        startButton.setDisable(true);
        stopButton.setDisable(false);
        gameRunning = true;
        clearGameGrid();
        gamer1TotalScore = 0;
        gamer2TotalScore = 0;

        updateStatus("🎮 Jeu en cours...", "#06ffa5");
        consoleArea.appendText("\n========================================\n");
        consoleArea.appendText("   🎮 DÉMARRAGE DU JEU\n");
        consoleArea.appendText("========================================\n\n");

        new Thread(() -> {
            try {
                Runtime rt = Runtime.instance();
                
                Profile p = new ProfileImpl();
                p.setParameter(Profile.MAIN_HOST, "localhost");
                p.setParameter(Profile.GUI, "false");
                p.setParameter(Profile.MAIN_PORT, "1099");
                p.setParameter(Profile.LOCAL_HOST, "localhost");
                
                Platform.runLater(() -> {
                    consoleArea.appendText("[INFO] Configuration JADE initialisée\n");
                });

                container = rt.createMainContainer(p);
                
                Platform.runLater(() -> {
                    consoleArea.appendText("[✓] Conteneur JADE créé\n\n");
                });

                Thread.sleep(500);

                Platform.runLater(() -> {
                    consoleArea.appendText("[INFO] Création des agents...\n");
                });
                
                AgentController arbitre = container.createNewAgent(
                    "ArbitreAgent",
                    "PetitBac.ArbitreAgent",
                    new Object[]{}
                );
                arbitre.start();
                
                Platform.runLater(() -> {
                    consoleArea.appendText("[✓] ArbitreAgent démarré\n");
                });

                Thread.sleep(800);

                AgentController joueur1 = container.createNewAgent(
                    "Gamer1",
                    "PetitBac.Joueur",
                    new Object[]{"BFS"}
                );
                joueur1.start();
                
                Platform.runLater(() -> {
                    consoleArea.appendText("[✓] Gamer1 démarré\n");
                });

                Thread.sleep(300);

                AgentController joueur2 = container.createNewAgent(
                    "Gamer2",
                    "PetitBac.Joueur2",
                    new Object[]{"ASTAR"}
                );
                joueur2.start();

                Platform.runLater(() -> {
                    consoleArea.appendText("[✓] Gamer2 démarré\n\n");
                    animateProgress();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    consoleArea.appendText("\n[ERREUR] " + e.getMessage() + "\n");
                    updateStatus("❌ Erreur de démarrage", "#ff006e");
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                    gameRunning = false;
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void stopGame() {
        if (!gameRunning) return;

        if (progressTimeline != null) {
            progressTimeline.stop();
        }

        try {
            if (container != null) {
                consoleArea.appendText("\n[INFO] Arrêt du conteneur JADE...\n");
                container.kill();
                consoleArea.appendText("[✓] Conteneur arrêté\n");
            }
        } catch (Exception e) {
            consoleArea.appendText("[WARN] Erreur: " + e.getMessage() + "\n");
        }

        gameRunning = false;
        startButton.setDisable(false);
        stopButton.setDisable(true);
        updateStatus("⏹ Jeu arrêté", "#ffd60a");
        progressBar.setProgress(0);
    }

    private void resetGame() {
        stopGame();
        consoleArea.clear();
        consoleArea.appendText("===========================================\n");
        consoleArea.appendText("  PETIT BAC - Réinitialisé\n");
        consoleArea.appendText("===========================================\n\n");
        roundLabel.setText("0/3");
        letterLabel.setText("?");
        score1Label.setText("0");
        score2Label.setText("0");
        gamer1TotalScore = 0;
        gamer2TotalScore = 0;
        progressBar.setProgress(0);
        clearGameGrid();
        updateStatus("⏸ En attente de démarrage", "#ffd60a");
    }

    private void updateStatus(String message, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.web(color));
        });
    }

    private void animateProgress() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
        
        progressTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
            new KeyFrame(Duration.seconds(30), new KeyValue(progressBar.progressProperty(), 1))
        );
        progressTimeline.setCycleCount(Timeline.INDEFINITE);
        progressTimeline.play();
    }

    private void redirectSystemOut() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) {
                buffer.append((char) b);
                if (b == '\n') {
                    flush();
                }
            }
            
            @Override
            public void write(byte[] b, int off, int len) {
                String text = new String(b, off, len);
                buffer.append(text);
                if (text.contains("\n")) {
                    flush();
                }
            }
            
            @Override
            public void flush() {
                if (buffer.length() > 0) {
                    final String text = buffer.toString();
                    buffer.setLength(0);
                    
                    Platform.runLater(() -> {
                        consoleArea.appendText(text);
                        consoleArea.setScrollTop(Double.MAX_VALUE);
                        updateUIFromConsole(text);
                    });
                }
            }
        }, true);
        
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void updateUIFromConsole(String text) {
        // Mettre à jour la manche
        if (text.contains("Manche")) {
            try {
                Pattern pattern = Pattern.compile("Manche\\s+(\\d+)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String roundNum = matcher.group(1);
                    Platform.runLater(() -> {
                        roundLabel.setText(roundNum + "/3");
                        if (roundNum.equals("1")) {
                            clearGameGrid(); // Nettoyer la grille pour la nouvelle manche
                        }
                    });
                }
            } catch (Exception e) {
                // Ignorer
            }
        }
        
        // Mettre à jour la lettre
        if (text.contains("Lettre :") || text.contains("Lettre:")) {
            try {
                Pattern pattern = Pattern.compile("Lettre\\s*:\\s*([A-Z])");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String letter = matcher.group(1);
                    Platform.runLater(() -> letterLabel.setText(letter));
                }
            } catch (Exception e) {
                // Ignorer
            }
        }

        // Parser les réponses des joueurs et les scores par thème
        // Format attendu: "Country | France | 10 | Italy | 5"
        if (text.contains("|") && (text.contains("Country") || text.contains("City") || 
            text.contains("GirlName") || text.contains("BoyName") || 
            text.contains("Fruit") || text.contains("Color") || text.contains("Object"))) {
            
            try {
                // Supprimer les caractères de tableau si présents
                String cleanText = text.replace("│", "|").replace("║", "|").replace("═", "").trim();
                
                // Diviser la ligne en parties
                String[] parts = cleanText.split("\\|");
                if (parts.length >= 5) {
                    String theme = parts[0].trim();
                    String gamer1Word = parts[1].trim();
                    String gamer1ScoreStr = parts[2].trim();
                    String gamer2Word = parts[3].trim();
                    String gamer2ScoreStr = parts[4].trim();
                    
                    // Vérifier si c'est un thème valide
                    boolean isValidTheme = false;
                    for (String validTheme : THEMES) {
                        if (validTheme.equals(theme)) {
                            isValidTheme = true;
                            break;
                        }
                    }
                    
                    if (isValidTheme) {
                        try {
                            int gamer1Score = Integer.parseInt(gamer1ScoreStr);
                            int gamer2Score = Integer.parseInt(gamer2ScoreStr);
                            
                            // Mettre à jour la grille
                            updateGameGrid("Gamer1", theme, gamer1Word, gamer1Score);
                            updateGameGrid("Gamer2", theme, gamer2Word, gamer2Score);
                            
                        } catch (NumberFormatException e) {
                            // Ignorer si les scores ne sont pas des nombres
                        }
                    }
                }
            } catch (Exception e) {
                // Ignorer les erreurs de parsing
            }
        }

        // Mettre à jour les scores totaux
        if (text.contains("Gamer1") && text.contains("points")) {
            try {
                Pattern pattern = Pattern.compile("Gamer1\\s*:\\s*(\\d+)\\s*points");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    final String score = matcher.group(1);
                    gamer1TotalScore = Integer.parseInt(score);
                    Platform.runLater(() -> {
                        score1Label.setText(score);
                        // Animation du score
                        score1Label.setScaleX(1.3);
                        score1Label.setScaleY(1.3);
                        Timeline timeline = new Timeline(
                            new KeyFrame(Duration.millis(300), 
                                new KeyValue(score1Label.scaleXProperty(), 1.0),
                                new KeyValue(score1Label.scaleYProperty(), 1.0))
                        );
                        timeline.play();
                    });
                }
            } catch (Exception e) {
                // Ignorer les erreurs de parsing
            }
        }
        
        if (text.contains("Gamer2") && text.contains("points")) {
            try {
                Pattern pattern = Pattern.compile("Gamer2\\s*:\\s*(\\d+)\\s*points");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    final String score = matcher.group(1);
                    gamer2TotalScore = Integer.parseInt(score);
                    Platform.runLater(() -> {
                        score2Label.setText(score);
                        // Animation du score
                        score2Label.setScaleX(1.3);
                        score2Label.setScaleY(1.3);
                        Timeline timeline = new Timeline(
                            new KeyFrame(Duration.millis(300), 
                                new KeyValue(score2Label.scaleXProperty(), 1.0),
                                new KeyValue(score2Label.scaleYProperty(), 1.0))
                        );
                        timeline.play();
                    });
                }
            } catch (Exception e) {
                // Ignorer les erreurs de parsing
            }
        }

        // Mettre à jour le score total dans les infos joueurs
        if (text.contains("Score total")) {
            try {
                Pattern pattern = Pattern.compile("Score total.*?(\\d+).*?(\\d+)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    final String score1 = matcher.group(1);
                    final String score2 = matcher.group(2);
                    Platform.runLater(() -> {
                        score1Label.setText(score1);
                        score2Label.setText(score2);
                    });
                }
            } catch (Exception e) {
                // Ignorer
            }
        }

        // Détecter le gagnant et afficher une animation
        if (text.contains("GAGNANT")) {
            Platform.runLater(() -> {
                if (text.contains("Gamer1")) {
                    updateStatus("🏆 GAGNANT : Gamer1", "#06ffa5");
                    animateWinner(player1Box);
                } else if (text.contains("Gamer2")) {
                    updateStatus("🏆 GAGNANT : Gamer2", "#ff006e");
                    animateWinner(player2Box);
                } else if (text.contains("ÉGALITÉ")) {
                    updateStatus("🤝 MATCH NUL", "#ffd60a");
                }
            });
        }
        
        // Détecter la fin du jeu
        if (text.contains("FIN DU JEU") || text.contains("Jeu terminé")) {
            Platform.runLater(() -> {
                if (progressTimeline != null) {
                    progressTimeline.stop();
                }
                progressBar.setProgress(1.0);
                updateStatus("🏁 Jeu terminé", "#ffd60a");
                stopButton.setDisable(true);
                startButton.setDisable(false);
                gameRunning = false;
            });
        }
    }
    
    private void animateWinner(VBox playerBox) {
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(playerBox.scaleXProperty(), 1.0),
                new KeyValue(playerBox.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(playerBox.scaleXProperty(), 1.1),
                new KeyValue(playerBox.scaleYProperty(), 1.1)),
            new KeyFrame(Duration.millis(400), 
                new KeyValue(playerBox.scaleXProperty(), 1.0),
                new KeyValue(playerBox.scaleYProperty(), 1.0))
        );
        flash.setCycleCount(3);
        flash.play();
    }

    private DropShadow createGlowEffect() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00d4ff"));
        glow.setRadius(20);
        glow.setSpread(0.5);
        return glow;
    }

    private String toRgbString(Color color) {
        return String.format("rgba(%d, %d, %d, %.2f)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255),
            color.getOpacity());
    }

    public static void main(String[] args) {
        launch(args);
    }
}