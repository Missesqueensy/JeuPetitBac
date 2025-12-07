package PetitBac;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class AgentArbitre extends Agent {

    private final String[] PLAYERS = {"AgentGamer", "AgentGamersd"}; // Noms locaux des agents joueurs
    private final String[] THEMES = {"Country", "City", "GirlName", "BoyName", "Fruit", "Color", "Object"};
    private final List<Character> AVAILABLE_LETTERS = Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    );
    private Random random = new Random();

    // Garder la trace des scores totaux
    private Map<String, Integer> globalScores = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println("Arbitre prêt. Initialisation des scores.");
        for (String player : PLAYERS) {
            globalScores.put(player, 0);
        }

        // Ajouter le comportement principal du jeu (manche par manche)
        addBehaviour(new GameMasterBehaviour(this, 3)); // Jouer 3 manches par exemple
    }

    // Comportement principal pour gérer le déroulement du jeu
    private class GameMasterBehaviour extends Behaviour {

        private final AgentArbitre arbitreAgent;
        private final int totalRounds;
        private int currentRound = 0;
        private boolean gameFinished = false;

        public GameMasterBehaviour(AgentArbitre agent, int totalRounds) {
            this.arbitreAgent = agent;
            this.totalRounds = totalRounds;
        }

        @Override
        public void action() {
            if (currentRound < totalRounds) {
                // Lancer une nouvelle manche
                char letter = chooseRandomLetter();
                System.out.println("--- DÉBUT DE LA MANCHE " + (currentRound + 1) + " (Lettre: " + letter + ") ---");

                // Envoyer la lettre à tous les joueurs
                sendLetterToPlayers(letter);

                // Attendre et traiter les réponses des joueurs
                processResponses(letter);

                currentRound++;
            } else {
                // Jeu terminé
                System.out.println("--- JEU TERMINÉ ---");
                System.out.println("Scores finaux:");
                for (Map.Entry<String, Integer> entry : globalScores.entrySet()) {
                    System.out.println("- " + entry.getKey() + ": " + entry.getValue() + " points");
                }
                gameFinished = true;
                // Arrêter l'agent Arbitre
                arbitreAgent.doDelete();
            }
        }

        @Override
        public boolean done() {
            return gameFinished;
        }

        // --- Logique d'une Manche ---

        private char chooseRandomLetter() {
            // Choisir une lettre aléatoire
            return AVAILABLE_LETTERS.get(random.nextInt(AVAILABLE_LETTERS.size()));
        }

        private void sendLetterToPlayers(char letter) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            for (String player : PLAYERS) {
                msg.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            msg.setContent(String.valueOf(letter));
            arbitreAgent.send(msg);
            System.out.println("Arbitre a envoyé la lettre: " + letter + " aux joueurs.");
        }

        private void processResponses(char letter) {
            Map<String, Map<String, String>> allResponses = new HashMap<>();
            int responsesReceived = 0;
            long startTime = System.currentTimeMillis();
            long timeout = 10000; // 10 secondes de délai max pour recevoir les 2 réponses

            // Attendre les réponses de tous les joueurs
            while (responsesReceived < PLAYERS.length && (System.currentTimeMillis() - startTime < timeout)) {
                // Filtrer les messages INFORM
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage response = arbitreAgent.receive(mt);

                if (response != null) {
                    String senderName = response.getSender().getLocalName();
                    String content = response.getContent();

                    try {
                        // Utiliser Gson pour parser la chaîne Map<String, String> reçue
                        Gson gson = new Gson();
                        Type type = new TypeToken<HashMap<String, String>>() {}.getType();
                        Map<String, String> playerResponses = gson.fromJson(content, type);

                        allResponses.put(senderName, playerResponses);
                        System.out.println("Arbitre a reçu les réponses de: " + senderName);
                        responsesReceived++;
                    } catch (Exception e) {
                        System.err.println("Erreur de parsing des réponses de " + senderName + ": " + e.getMessage());
                    }
                } else {
                    block(500); // Bloquer pendant 500ms si aucun message n'est reçu
                }
            }

            // S'assurer d'avoir au moins 2 joueurs qui ont répondu
            if (allResponses.size() == PLAYERS.length) {
                calculateScores(allResponses, letter);
            } else {
                System.out.println("Manche annulée : pas toutes les réponses reçues à temps.");
            }
        }

        private void calculateScores(Map<String, Map<String, String>> allResponses, char letter) {
            System.out.println("--- CALCUL DES SCORES POUR LA LETTRE " + letter + " ---");

            Map<String, Integer> roundScores = new HashMap<>();
            for (String player : PLAYERS) {
                roundScores.put(player, 0); // Initialiser le score de la manche
            }

            // Le premier joueur dans PLAYERS est J1, le second est J2 pour un jeu à 2
            String player1 = PLAYERS[0];
            String player2 = PLAYERS[1];

            // Parcourir chaque thème
            for (String theme : THEMES) {
                String word1 = allResponses.getOrDefault(player1, new HashMap<>()).getOrDefault(theme, "");
                String word2 = allResponses.getOrDefault(player2, new HashMap<>()).getOrDefault(theme, "");

                int scoreJ1 = 0;
                int scoreJ2 = 0;

                boolean validJ1 = isValidResponse(word1, letter);
                boolean validJ2 = isValidResponse(word2, letter);

                // Comparaison des mots
                if (validJ1 && validJ2) {
                    if (word1.equalsIgnoreCase(word2)) {
                        // Mots identiques : 1 point chacun
                        scoreJ1 = 1;
                        scoreJ2 = 1;
                    } else {
                        // Mots différents : 2 points chacun
                        scoreJ1 = 2;
                        scoreJ2 = 2;
                    }
                } else if (validJ1) {
                    // Seul J1 a un mot valide : 2 points
                    scoreJ1 = 2;
                    scoreJ2 = 0;
                } else if (validJ2) {
                    // Seul J2 a un mot valide : 2 points
                    scoreJ1 = 0;
                    scoreJ2 = 2;
                } else {
                    // Aucun mot valide : 0 point
                    scoreJ1 = 0;
                    scoreJ2 = 0;
                }

                // Mise à jour des scores de la manche
                roundScores.put(player1, roundScores.get(player1) + scoreJ1);
                roundScores.put(player2, roundScores.get(player2) + scoreJ2);

                System.out.printf("  - %s: %s (%d) | %s: %s (%d)%n",
                        player1, word1, scoreJ1, player2, word2, scoreJ2);
            }

            // Mise à jour des scores globaux et affichage du résultat de la manche
            System.out.println("--- RÉSULTATS DE LA MANCHE ---");
            for (String player : PLAYERS) {
                int score = roundScores.get(player);
                globalScores.put(player, globalScores.get(player) + score);
                System.out.println("  - " + player + " gagne " + score + " points (Total: " + globalScores.get(player) + ")");
            }
        }

        // Vérifie si la réponse est non vide et commence par la bonne lettre
        private boolean isValidResponse(String word, char letter) {
            if (word == null || word.trim().isEmpty() || word.equals(String.valueOf(letter) + "word")) {
                // "Xword" est la réponse par défaut si l'agent ne trouve pas de mot dans le dictionnaire
                return false;
            }
            return word.toUpperCase().charAt(0) == Character.toUpperCase(letter);
        }
    }
}