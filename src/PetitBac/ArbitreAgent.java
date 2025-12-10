/*package PetitBac;

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

        private String firstToStop = null;
        private long stopTime = 0;
        private Map<String, Long> playerTimes = new HashMap<>();
        private Map<String, String> playerAlgorithms = new HashMap<>();

        public GameMasterBehaviour(int totalRounds) { this.totalRounds = totalRounds; }

        @Override
        public void action() {
            if (currentRound < totalRounds) {
                char letter = AVAILABLE_LETTERS.get(random.nextInt(AVAILABLE_LETTERS.size()));
                System.out.println("\n📨 Manche " + (currentRound+1) + " - Lettre : " + letter);

                firstToStop = null;
                stopTime = 0;
                playerTimes.clear();
                playerAlgorithms.clear();

                sendLetterToPlayers(letter);
                waitForStopAndResponses();

               // processResponses(letter);

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

       
        private void waitForStopAndResponses() {
            long start = System.currentTimeMillis();
            long timeout = 30000; // 30 sec
            Map<String, Map<String,String>> allResponses = new HashMap<>();
            firstToStop = null;

            while (System.currentTimeMillis() - start < timeout) {
                ACLMessage msg = receive();
                if (msg != null) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();

                    if ("STOP".equals(content) && firstToStop == null) {
                        firstToStop = sender;
                        stopTime = System.currentTimeMillis() - start;
                        System.out.println("🛑 " + firstToStop + " a dit STOP en premier ! ("+stopTime+" ms)");
                    } else if (!"STOP".equals(content) && (firstToStop == null || firstToStop.equals(sender))) {
                        // Recevoir uniquement les réponses avant le STOP
                        try {
                            Gson gson = new Gson();
                            Type mapType = new TypeToken<HashMap<String,Object>>(){}.getType();
                            HashMap<String,Object> data = gson.fromJson(content, mapType);
                            Map<String,String> responses = (Map<String,String>) data.get("responses");
                            allResponses.put(sender, responses);

                            playerAlgorithms.put(sender, (String)data.get("algorithm"));
                            playerTimes.put(sender, ((Number)data.get("time")).longValue());

                            System.out.println("✅ Réponses reçues de " + sender);

                        } catch(Exception e) {
                            System.err.println("❌ Erreur parsing " + sender + " : " + e.getMessage());
                        }
                    }
                } else {
                    block(100);
                }

                // Stop si tous les joueurs ont envoyé leurs réponses ou si STOP reçu
                if (firstToStop != null && allResponses.containsKey(firstToStop)) break;
            }

            // Calculer le score seulement avec les réponses disponibles
            calculateScores(allResponses, letter.charValue());

        }


        private void processResponses(char letter) {
            Map<String, Map<String, String>> allResponses = new HashMap<>();
            int received = 0;
            long start = System.currentTimeMillis();
            long timeout = 10000;

            System.out.println("📥 Réception des réponses...");

            while (received < PLAYERS.length && System.currentTimeMillis()-start < timeout) {
                ACLMessage msg = receive();
                if (msg != null && !"STOP".equals(msg.getContent())) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();

                    try {
                        Gson gson = new Gson();
                        Type mapType = new TypeToken<HashMap<String,Object>>(){}.getType();
                        HashMap<String,Object> data = gson.fromJson(content, mapType);

                        Map<String,String> responses = (Map<String,String>) data.get("responses");
                        allResponses.put(sender, responses);

                        // Stocker métadonnées
                        playerAlgorithms.put(sender, (String)data.get("algorithm"));
                        playerTimes.put(sender, ((Number)data.get("time")).longValue());

                        received++;
                        System.out.println("✅ Réponses reçues de " + sender + " ("+received+"/"+PLAYERS.length+")");

                    } catch(Exception e) {
                        System.err.println("❌ Erreur parsing " + sender + " : " + e.getMessage());
                    }
                } else block(100);
            }

            if (allResponses.size() == PLAYERS.length) calculateScores(allResponses, letter);
        }

        private void calculateScores(Map<String, Map<String,String>> allResponses, char letter) {
            Map<String,Integer> roundScores = new HashMap<>();
            for (String p : PLAYERS) roundScores.put(p, 0);

            System.out.println("\n📊 Résultats manche :");
            for (String theme : THEMES) {
                String w1 = allResponses.getOrDefault(PLAYERS[0], new HashMap<>()).getOrDefault(theme, "");
                String w2 = allResponses.getOrDefault(PLAYERS[1], new HashMap<>()).getOrDefault(theme, "");

                int s1=0, s2=0;
                boolean v1 = isValid(w1, letter);
                boolean v2 = isValid(w2, letter);

                if (v1 && v2) s1=s2=(w1.equalsIgnoreCase(w2)?1:2);
                else if (v1) s1=2;
                else if (v2) s2=2;

                roundScores.put(PLAYERS[0], roundScores.get(PLAYERS[0])+s1);
                roundScores.put(PLAYERS[1], roundScores.get(PLAYERS[1])+s2);

                System.out.println(theme + " : "+w1+"("+s1+") | "+w2+"("+s2+")");
            }

            if (firstToStop!=null) {
                roundScores.put(firstToStop, roundScores.get(firstToStop)+20);
                System.out.println("⭐ BONUS STOP : +20 points pour "+firstToStop);
            }

            for (String p : PLAYERS) {
                globalScores.put(p, globalScores.get(p)+roundScores.get(p));
            }
        }
     // Version plus robuste
        private boolean isValid(String word, char letter) {
            if(word==null || word.trim().isEmpty()) return false;
            // Vérifie que le mot ne contient QUE des lettres (et espaces optionnels, selon la règle)
            if(!word.matches("[a-zA-Z\\s]+")) return false; // Par exemple, autorise les espaces
            // Vérifie la première lettre
            return Character.toUpperCase(word.charAt(0)) == Character.toUpperCase(letter);
        }

        private void displayFinalResults() {
            System.out.println("\n🏆 FIN DU JEU 🏆");
            PLAYERS_SCORES:
            for(String p: PLAYERS) {
                System.out.println(p+" : "+globalScores.get(p)+" pts");
            }
        }
    }
}*/
/*package PetitBac;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ArbitreAgent extends Agent {
	private int roundNumber = 1;
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
                waitForStopAndResponses(letter);

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

        private void waitForStopAndResponses(char letter) {
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
                    //else if (!content.equals("STOP_SIGNAL")) {
                    else if (!"STOP_SIGNAL".equals(content) && !content.equals("STOP")) {

                        try {
                            Gson gson = new Gson();
                            Type mapType = new TypeToken<HashMap<String,Object>>(){}.getType();
                            HashMap<String,Object> data = gson.fromJson(content, mapType);
                            
                            Map<String,String> responses = (Map<String,String>) data.get("responses");
                            //allResponses.put(sender, responses);
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

            // Calculer les scores
            calculateScores(allResponses, letter, firstToStop);
        }

       /*1- private void calculateScores(Map<String, Map<String,String>> allResponses, char letter, String firstToStop) {
            Map<String,Integer> roundScores = new HashMap<>();
            for (String p : PLAYERS) roundScores.put(p, 0);

            System.out.println("\n📊 Résultats de la manche :");
            System.out.println("─────────────────────────────────────────────");
            
            for (String theme : THEMES) {
                String w1 = allResponses.getOrDefault(PLAYERS[0], new HashMap<>()).getOrDefault(theme, "");
                String w2 = allResponses.getOrDefault(PLAYERS[1], new HashMap<>()).getOrDefault(theme, "");

                int s1 = 0, s2 = 0;
                boolean v1 = isValid(w1, letter);
                boolean v2 = isValid(w2, letter);

                if (v1 && v2) {
                    s1 = s2 = (w1.equalsIgnoreCase(w2) ? 1 : 2);
                } else if (v1) {
                    s1 = 2;
                } else if (v2) {
                    s2 = 2;
                }

                roundScores.put(PLAYERS[0], roundScores.get(PLAYERS[0]) + s1);
                roundScores.put(PLAYERS[1], roundScores.get(PLAYERS[1]) + s2);

                /*System.out.printf("%-12s : %-15s (%d pts) | %-15s (%d pts)%n", 
                                theme, 
                                w1.isEmpty() ? "---" : w1, s1,
                                w2.isEmpty() ? "---" : w2, s2);
            }
                System.out.printf(
                	    "%-12s : %-7s → %-15s (%d pts) | %-7s → %-15s (%d pts)%n",
                	    theme,
                	    PLAYERS[0], (w1.isEmpty() ? "---" : w1), s1,
                	    PLAYERS[1], (w2.isEmpty() ? "---" : w2), s2
                	);


            // Bonus STOP
            if (firstToStop != null) {
                roundScores.put(firstToStop, roundScores.get(firstToStop) + 20);
                System.out.println("─────────────────────────────────────────────");
                System.out.println("⭐ BONUS STOP : +20 points pour " + firstToStop);
            }

            // Mise à jour scores globaux
            System.out.println("─────────────────────────────────────────────");
            System.out.println("Scores de la manche :");
            for (String p : PLAYERS) {
                globalScores.put(p, globalScores.get(p) + roundScores.get(p));
                System.out.println("  " + p + " : +" + roundScores.get(p) + " pts (Total: " + globalScores.get(p) + " pts)");
            }
        }
        }*
        private void calculateScores(Map<String, Map<String,String>> allResponses, char letter, String firstToStop) {
            Map<String,Integer> roundScores = new HashMap<>();
            for (String p : PLAYERS) roundScores.put(p, 0);

            // --- 1. Titre de la Manche ---
            System.out.println("\n" + "=".repeat(80));
            System.out.printf(" \u2694\uFE0F MANCHE %d - LETTRE : %c %s \n", roundNumber, letter, (firstToStop != null ? "(STOP: " + firstToStop + ")" : ""));
            System.out.println("=".repeat(80));
            
            // --- 2. En-tête du Tableau (Thèmes et Réponses) ---
            System.out.println("\u250C" + "\u2500".repeat(12) + "\u252C" + "\u2500".repeat(30) + "\u252C" + "\u2500".repeat(30) + "\u2510");
            System.out.printf("\u2502 %-10s \u2502 %-15s | %-7s \u2502 %-15s | %-7s \u2502\n", 
                              "THÈME", PLAYERS[0] + " (Rép.)", "(Pts)", PLAYERS[1] + " (Rép.)", "(Pts)");
            System.out.println("\u251C" + "\u2500".repeat(12) + "\u253C" + "\u2500".repeat(30) + "\u253C" + "\u2500".repeat(30) + "\u2524");
            
            int totalBaseScoreG1 = 0;
            int totalBaseScoreG2 = 0;

            // --- 3. Parcours des Thèmes et Calcul des Scores ---
            for (String theme : THEMES) {
                String w1 = allResponses.getOrDefault(PLAYERS[0], new HashMap<>()).getOrDefault(theme, "");
                String w2 = allResponses.getOrDefault(PLAYERS[1], new HashMap<>()).getOrDefault(theme, "");
                w1 = w1.isEmpty() ? "---" : w1;
                w2 = w2.isEmpty() ? "---" : w2;

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

                // Ligne de données formatée
                System.out.printf("\u2502 %-10s \u2502 %-15s | %-7d \u2502 %-15s | %-7d \u2502\n", 
                                  theme, w1, s1, w2, s2);
            }
            
            // --- 4. Pied de Tableau (Scores de Base) ---
            System.out.println("\u2514" + "\u2500".repeat(12) + "\u2534" + "\u2500".repeat(30) + "\u2534" + "\u2500".repeat(30) + "\u2518");
            System.out.printf("  SCORE DE BASE (Thèmes) : Gamer1 = %d pts | Gamer2 = %d pts%n", totalBaseScoreG1, totalBaseScoreG2);


            // --- 5. Détails du Score Final de la Manche ---
            int stopBonusG1 = 0;
            int stopBonusG2 = 0;
            
            if (firstToStop != null) {
                System.out.println("  " + "\u2500".repeat(78));
                System.out.printf(" \u2B50 BONUS STOP (+20 pts) : Attribué à %s%n", firstToStop);
                
                if (firstToStop.equals(PLAYERS[0])) {
                    roundScores.put(PLAYERS[0], roundScores.get(PLAYERS[0]) + 20);
                    stopBonusG1 = 20;
                } else {
                    roundScores.put(PLAYERS[1], roundScores.get(PLAYERS[1]) + 20);
                    stopBonusG2 = 20;
                }
            } else {
                System.out.println("  " + "\u2500".repeat(78));
                System.out.println(" \u2B50 BONUS STOP (+20 pts) : Non attribué.");
            }

            // --- 6. Tableau de Récapitulatif Global ---
            
            System.out.println("  " + "\u2500".repeat(78));
            System.out.println("  \uD83D\uDCC8 RÉCAPITULATIF DES SCORES :");
            System.out.println("  " + "\u2500".repeat(78));

            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "Joueur", "Score Base", "Bonus STOP", "Total Manche", "Total Global");
            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "---", "---", "---", "---", "---");

            // Ligne Gamer1
            int totalMancheG1 = totalBaseScoreG1 + stopBonusG1;
            globalScores.put(PLAYERS[0], globalScores.get(PLAYERS[0]) + totalMancheG1);
            System.out.printf("  | %-7s | %-12d | %-12d | **%-12d** | **%-12d** |\n", 
                              PLAYERS[0], totalBaseScoreG1, stopBonusG1, totalMancheG1, globalScores.get(PLAYERS[0])
            );
            
            // Ligne Gamer2
            int totalMancheG2 = totalBaseScoreG2 + stopBonusG2;
            globalScores.put(PLAYERS[1], globalScores.get(PLAYERS[1]) + totalMancheG2);
            System.out.printf("  | %-7s | %-12d | %-12d | **%-12d** | **%-12d** |\n", 
                              PLAYERS[1], totalBaseScoreG2, stopBonusG2, totalMancheG2, globalScores.get(PLAYERS[1])
            );
            System.out.println("  " + "\u2500".repeat(78));
        }
        private boolean isValid(String word, char letter) {
            if (word == null || word.trim().isEmpty()) return false;
            if (!word.matches("[a-zA-Z\\s]+")) return false;
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
            }
            System.out.println("═══════════════════════════════════════════════");
        }
    }
}*/
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

            // --- 1. Titre de la Manche ---
            System.out.println("\n" + "=".repeat(80));
            System.out.printf(" ⚔️ MANCHE %d - LETTRE : %c %s \n", roundNumber, letter, (firstToStop != null ? "(STOP: " + firstToStop + ")" : ""));
            System.out.println("=".repeat(80));
            
            // --- 2. En-tête du Tableau (Thèmes et Réponses) ---
            System.out.println("┌" + "─".repeat(12) + "┬" + "─".repeat(30) + "┬" + "─".repeat(30) + "┐");
            System.out.printf("│ %-10s │ %-15s | %-7s │ %-15s | %-7s │\n", 
                              "THÈME", PLAYERS[0] + " (Rép.)", "(Pts)", PLAYERS[1] + " (Rép.)", "(Pts)");
            System.out.println("├" + "─".repeat(12) + "┼" + "─".repeat(30) + "┼" + "─".repeat(30) + "┤");
            
            int totalBaseScoreG1 = 0;
            int totalBaseScoreG2 = 0;

            // --- 3. Parcours des Thèmes et Calcul des Scores ---
            for (String theme : THEMES) {
                String w1 = allResponses.getOrDefault(PLAYERS[0], new HashMap<>()).getOrDefault(theme, "");
                String w2 = allResponses.getOrDefault(PLAYERS[1], new HashMap<>()).getOrDefault(theme, "");
                w1 = w1.isEmpty() ? "---" : w1;
                w2 = w2.isEmpty() ? "---" : w2;

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

                // Ligne de données formatée
                System.out.printf("│ %-10s │ %-15s | %-7d │ %-15s | %-7d │\n", 
                                  theme, w1, s1, w2, s2);
            }
            
            // --- 4. Pied de Tableau (Scores de Base) ---
            System.out.println("└" + "─".repeat(12) + "┴" + "─".repeat(30) + "┴" + "─".repeat(30) + "┘");
            System.out.printf("  SCORE DE BASE (Thèmes) : Gamer1 = %d pts | Gamer2 = %d pts%n", totalBaseScoreG1, totalBaseScoreG2);


            // --- 5. Détails du Score Final de la Manche ---
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

            // --- 6. Tableau de Récapitulatif Global ---
            
            System.out.println("  " + "─".repeat(78));
            System.out.println("  📈 RÉCAPITULATIF DES SCORES :");
            System.out.println("  " + "─".repeat(78));

            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "Joueur", "Score Base", "Bonus STOP", "Total Manche", "Total Global");
            System.out.printf("  | %-7s | %-12s | %-12s | %-12s | %-12s |\n", 
                              "---", "---", "---", "---", "---");

            // Ligne Gamer1
            int totalMancheG1 = totalBaseScoreG1 + stopBonusG1;
            globalScores.put(PLAYERS[0], globalScores.get(PLAYERS[0]) + totalMancheG1);
            System.out.printf("  | %-7s | %-12d | %-12d | %-10d | %-10d |\n", 
                              PLAYERS[0], totalBaseScoreG1, stopBonusG1, totalMancheG1, globalScores.get(PLAYERS[0])
            );
            
            // Ligne Gamer2
            int totalMancheG2 = totalBaseScoreG2 + stopBonusG2;
            globalScores.put(PLAYERS[1], globalScores.get(PLAYERS[1]) + totalMancheG2);
            System.out.printf("  | %-7s | %-12d | %-12d | %-10d | %-10d |\n", 
                              PLAYERS[1], totalBaseScoreG2, stopBonusG2, totalMancheG2, globalScores.get(PLAYERS[1])
            );
            System.out.println("  " + "─".repeat(78));
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
            }
            System.out.println("═══════════════════════════════════════════════");
        }
    }
}

