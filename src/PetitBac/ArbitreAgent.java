package PetitBac;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ArbitreAgent extends Agent {
    private final String[] PLAYERS = {"Gamer1", "Gamer2"};
    private final String[] THEMES = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};
    private final List<Character> AVAILABLE_LETTERS = Arrays.asList(
        'A','B','C','D','E','F','G','H','I','J','K','L','M',
        'N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
    );

    private Random random = new Random();
    private Map<String, Integer> globalScores = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println("🏁 ArbitreAgent prêt. Initialisation…");
        for (String player : PLAYERS) globalScores.put(player, 0);

        addBehaviour(new WaitPlayersReadyBehaviour());
    }

    // ================= ATTENTE DES JOUEURS =================
    private class WaitPlayersReadyBehaviour extends Behaviour {
        private int readyCount = 0;
        private boolean finished = false;

        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && "ready".equals(msg.getContent())) {
                readyCount++;
                System.out.println("✅ " + msg.getSender().getLocalName() + " est prêt ! (" +
                                   readyCount + "/" + PLAYERS.length + ")");
                if (readyCount == PLAYERS.length) {
                    System.out.println("\n🎮 Tous les joueurs sont prêts ! Lancement du jeu...\n");
                    addBehaviour(new GameMasterBehaviour(3)); // 3 manches
                    finished = true;
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() { return finished; }
    }

    // ================= GESTION DU JEU =================
    private class GameMasterBehaviour extends Behaviour {

        private final int totalRounds;
        private int currentRound = 0;
        private boolean gameFinished = false;

        public GameMasterBehaviour(int totalRounds) { this.totalRounds = totalRounds; }

        @Override
        public void action() {
            if (currentRound < totalRounds) {
                char letter = AVAILABLE_LETTERS.get(random.nextInt(AVAILABLE_LETTERS.size()));
                System.out.println("\n📨 Manche " + (currentRound+1) + " - Lettre : " + letter);

                sendLetterToPlayers(letter);
                waitForStopAndResponses(letter, currentRound + 1); // Passer le numéro de manche

                currentRound++;
            } else {
                displayFinalResults();
                gameFinished = true;
                doDelete();
            }
        }

        @Override
        public boolean done() { return gameFinished; }

        private void sendLetterToPlayers(char letter) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            for (String p : PLAYERS) msg.addReceiver(new AID(p, AID.ISLOCALNAME));
            msg.setContent(String.valueOf(letter));
            send(msg);
        }

        private void waitForStopAndResponses(char letter, int roundNumber) {
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30 secondes max
            
            String firstToStop = null;
            long stopTime = 0;
            Map<String, Map<String,String>> allResponses = new HashMap<>();
            Map<String, Long> playerTimes = new HashMap<>();
            Map<String, String> playerAlgorithms = new HashMap<>();
            
            boolean stopReceived = false;

            while (System.currentTimeMillis() - startTime < timeout) {
                ACLMessage msg = receive();
                
                if (msg != null) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();

                    // Gestion du STOP
                    if ("STOP".equals(content)) {
                        if (firstToStop == null) {
                            firstToStop = sender;
                            stopTime = System.currentTimeMillis() - startTime;
                            stopReceived = true;
                            System.out.println("🛑 " + firstToStop + " a dit STOP en premier ! (" + stopTime + " ms)");
                            
                            // Envoyer signal STOP à l'autre joueur pour l'interrompre
                            for (String player : PLAYERS) {
                                if (!player.equals(firstToStop)) {
                                    ACLMessage stopSignal = new ACLMessage(ACLMessage.INFORM);
                                    stopSignal.addReceiver(new AID(player, AID.ISLOCALNAME));
                                    stopSignal.setContent("STOP_SIGNAL");
                                    send(stopSignal);
                                    System.out.println("📢 Signal STOP envoyé à " + player);
                                }
                            }
                        }
                    }
                    // Gestion des réponses
                    else if (!"STOP_SIGNAL".equals(content) && !content.equals("STOP")) {

                        try {
                            Gson gson = new Gson();
                            Type mapType = new TypeToken<HashMap<String,Object>>(){}.getType();
                            HashMap<String,Object> data = gson.fromJson(content, mapType);
                            
                            Map<String,String> responses = (Map<String,String>) data.get("responses");
                            allResponses.putIfAbsent(sender, new HashMap<>());
                            allResponses.get(sender).putAll(responses);

                            playerAlgorithms.put(sender, (String)data.get("algorithm"));
                            playerTimes.put(sender, ((Number)data.get("time")).longValue());
                            
                            System.out.println("✅ Réponses reçues de " + sender + 
                                             " (" + responses.size() + " thèmes remplis)");
                            
                        } catch(Exception e) {
                            System.err.println("❌ Erreur parsing " + sender + " : " + e.getMessage());
                        }
                    }
                } else {
                    block(50);
                }

                // Sortir si on a reçu STOP et toutes les réponses
                if (stopReceived && allResponses.size() == PLAYERS.length) {
                    System.out.println("✓ STOP reçu et toutes les réponses collectées");
                    break;
                }
            }

            // Afficher les métadonnées
            System.out.println("\n📊 Métadonnées de la manche :");
            for (String player : PLAYERS) {
                if (playerAlgorithms.containsKey(player)) {
                    System.out.println("  " + player + " - Algorithme: " + playerAlgorithms.get(player) + 
                                     ", Temps: " + playerTimes.get(player) + " ms");
                }
            }

            // Calculer les scores avec le bon numéro de manche
            calculateScores(allResponses, letter, firstToStop, roundNumber);
        }

        private void calculateScores(Map<String, Map<String,String>> allResponses, char letter, String firstToStop, int roundNumber) {
            Map<String,Integer> roundScores = new HashMap<>();
            for (String p : PLAYERS) roundScores.put(p, 0);

            // ================= DÉBUT AFFICHAGE POUR GUI =================
            System.out.println("\n" + "=".repeat(80));
            System.out.printf(" ⚔️ MANCHE %d - LETTRE : %c %s \n", roundNumber, letter, (firstToStop != null ? "(STOP: " + firstToStop + ")" : ""));
            System.out.println("=".repeat(80));
            
            // === AFFICHAGE POUR LA GRILLE GUI (FORMAT SIMPLE) ===
            System.out.println("\n=== AFFICHAGE POUR LA GRILLE GUI ===");
            
            int totalBaseScoreG1 = 0;
            int totalBaseScoreG2 = 0;
            
            // ⭐ EN-TÊTE SIMPLE POUR LE PARSING DU GUI (POUR L'HISTORIQUE)
            System.out.println("THÈME | MOT_GAMER1 | POINTS_GAMER1 | MOT_GAMER2 | POINTS_GAMER2");
            
            // === AFFICHAGE DÉTAILLÉ POUR CONSOLE ===
            System.out.println("┌" + "─".repeat(12) + "┬" + "─".repeat(30) + "┬" + "─".repeat(30) + "┐");
            System.out.printf("│ %-10s │ %-15s | %-7s │ %-15s | %-7s │\n", 
                              "THÈME", PLAYERS[0] + " (Rép.)", "(Pts)", PLAYERS[1] + " (Rép.)", "(Pts)");
            System.out.println("├" + "─".repeat(12) + "┼" + "─".repeat(30) + "┼" + "─".repeat(30) + "┤");

            // Parcours des thèmes
            for (String theme : THEMES) {
                String w1 = allResponses.getOrDefault(PLAYERS[0], new HashMap<>()).getOrDefault(theme, "");
                String w2 = allResponses.getOrDefault(PLAYERS[1], new HashMap<>()).getOrDefault(theme, "");
                w1 = w1.isEmpty() ? "---" : w1;
                w2 = w2.isEmpty() ? "---" : w2;

                // Calcul des points
                int s1 = 0, s2 = 0;
                boolean v1 = isValid(w1, letter);
                boolean v2 = isValid(w2, letter);

                if (v1 && v2) {
                    s1 = s2 = (w1.equalsIgnoreCase(w2) ? 1 : 2); // 1 pt si identique, 2 pts sinon
                } else if (v1) {
                    s1 = 2; // 2 pts si valide et l'autre non
                } else if (v2) {
                    s2 = 2;
                }

                totalBaseScoreG1 += s1;
                totalBaseScoreG2 += s2;
                
                roundScores.put(PLAYERS[0], roundScores.get(PLAYERS[0]) + s1);
                roundScores.put(PLAYERS[1], roundScores.get(PLAYERS[1]) + s2);

                // Ligne formatée pour console
                System.out.printf("│ %-10s │ %-15s | %-7d │ %-15s | %-7d │\n", 
                                  theme, w1, s1, w2, s2);
                
                // ⭐⭐ LIGNE SIMPLE POUR LE PARSING DU GUI (POUR L'HISTORIQUE) ⭐⭐
                System.out.println(theme + " | " + w1 + " | " + s1 + " | " + w2 + " | " + s2);
            }
            
            System.out.println("└" + "─".repeat(12) + "┴" + "─".repeat(30) + "┴" + "─".repeat(30) + "┘");
            
            // Gestion du bonus STOP
            int stopBonusG1 = 0;
            int stopBonusG2 = 0;
            
            if (firstToStop != null) {
                System.out.println("  " + "─".repeat(78));
                System.out.printf(" ⭐ BONUS STOP (+20 pts) : Attribué à %s%n", firstToStop);
                
                if (firstToStop.equals(PLAYERS[0])) {
                    roundScores.put(PLAYERS[0], roundScores.get(PLAYERS[0]) + 20);
                    stopBonusG1 = 20;
                } else {
                    roundScores.put(PLAYERS[1], roundScores.get(PLAYERS[1]) + 20);
                    stopBonusG2 = 20;
                }
            } else {
                System.out.println("  " + "─".repeat(78));
                System.out.println(" ⭐ BONUS STOP (+20 pts) : Non attribué.");
            }

            // Mettre à jour les scores globaux
            int totalMancheG1 = totalBaseScoreG1 + stopBonusG1;
            int totalMancheG2 = totalBaseScoreG2 + stopBonusG2;
            
            globalScores.put(PLAYERS[0], globalScores.get(PLAYERS[0]) + totalMancheG1);
            globalScores.put(PLAYERS[1], globalScores.get(PLAYERS[1]) + totalMancheG2);
            
            // ⭐ AFFICHER LES SCORES POUR LE GUI (POUR L'HISTORIQUE)
            System.out.println("\n=== SCORES POUR GUI ===");
            System.out.println("Gamer1 : " + globalScores.get(PLAYERS[0]) + " points");
            System.out.println("Gamer2 : " + globalScores.get(PLAYERS[1]) + " points");
            System.out.println("=== FIN AFFICHAGE GRILLE ===\n");
            // ================= FIN AFFICHAGE POUR GUI =================

            // Tableau de Récapitulatif Global
            System.out.println("  " + "─".repeat(78));
            System.out.println("  📈 RÉCAPITULATIF DES SCORES :");
            System.out.println("  " + "─".repeat(78));

            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "Joueur", "Score Base", "Bonus STOP", "Total Manche", "Total Global");
            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "---", "---", "---", "---", "---");

            System.out.printf("  | %-7s | %-12d | %-12d | %-10d | %-10d |\n", 
                              PLAYERS[0], totalBaseScoreG1, stopBonusG1, totalMancheG1, globalScores.get(PLAYERS[0]));
            
            System.out.printf("  | %-7s | %-12d | %-12d | %-10d | %-10d |\n", 
                              PLAYERS[1], totalBaseScoreG2, stopBonusG2, totalMancheG2, globalScores.get(PLAYERS[1]));
            
            System.out.println("  " + "─".repeat(78));
            System.out.println();
        }
        
        private boolean isValid(String word, char letter) {
            if (word == null || word.trim().isEmpty() || word.equals("---")) return false;
            if (!word.matches("[a-zA-Z\\s\\(\\)]+")) return false;
            return Character.toUpperCase(word.charAt(0)) == Character.toUpperCase(letter);
        }

        private void displayFinalResults() {
            System.out.println("\n");
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("           🏆 FIN DU JEU 🏆");
            System.out.println("═══════════════════════════════════════════════");
            
            String winner = null;
            int maxScore = -1;
            
            for (String p : PLAYERS) {
                int score = globalScores.get(p);
                System.out.println("  " + p + " : " + score + " points");
                if (score > maxScore) {
                    maxScore = score;
                    winner = p;
                }
            }
            
            System.out.println("═══════════════════════════════════════════════");
            if (winner != null) {
                System.out.println("  🎉 GAGNANT : " + winner + " avec " + maxScore + " points ! 🎉");
                // ⭐ POUR LE PARSING DU GUI
                System.out.println("GAGNANT : " + winner);
            } else {
                System.out.println("🤝 MATCH NUL !");
                System.out.println("ÉGALITÉ");
            }
            System.out.println("═══════════════════════════════════════════════");
            
            // ⭐ AFFICHER LES SCORES FINAUX POUR LE GUI (POUR L'HISTORIQUE)
            System.out.println("\n=== FIN DU JEU - SCORES FINAUX ===");
            System.out.println("Gamer1 : " + globalScores.get(PLAYERS[0]) + " points");
            System.out.println("Gamer2 : " + globalScores.get(PLAYERS[1]) + " points");
            System.out.println("FIN DU JEU");
        }
    }
}