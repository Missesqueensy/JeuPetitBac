package PetitBac;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.io.InputStreamReader;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Joueur extends Agent {

    private String gamerName;
    private int score;
    private HashMap<String, String> currentResponses = new HashMap<>();
    private GameState state;
    private AID aid;
    private Random rand = new Random();
    private SearchAlgorithm searchAlgorithm;
    private boolean hasSaidStop = false;

    // Dictionnaire : thème -> (lettre -> liste de mots)
    private HashMap<String, HashMap<Character, String[]>> personalDictionary;

    public enum GameState {
        WAITING,
        RUNNING,
        FINISHED
    }

    public enum SearchAlgorithm {
        BFS, DFS, UCS, ASTAR
    }
/*****//* Partie modifiée pour choisir un algorithme aléatoire à chaque manche*/
private SearchAlgorithm chooseRandomAlgorithm() {
    SearchAlgorithm[] algos = SearchAlgorithm.values();
    return algos[rand.nextInt(algos.length)];
}

    @Override
    protected void setup() {
        aid = getAID();
        gamerName = getLocalName();
        score = 0;
        state = GameState.WAITING;

        // Définir l'algorithme selon le nom de l'agent ou les arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            searchAlgorithm = SearchAlgorithm.valueOf(args[0].toString());
        } else { System.out.println("gamername")     ;       // Par défaut selon le nom
            if (gamerName.contains("sd")) {
                searchAlgorithm = SearchAlgorithm.ASTAR;
            } else {
                searchAlgorithm = SearchAlgorithm.BFS;
            }
        }

        /*System.out.println("🎮 Agent joueur créé : " + gamerName + 
                         " | Algorithme : " + searchAlgorithm);*/

        loadDictionary();
        
        // Envoyer signal "ready" à l'arbitre
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
        msg.setContent("ready");
        send(msg);
        System.out.println("✅ " + gamerName + " est prêt (signal envoyé)\n");

        addBehaviour(new GamerBehaviour(this));
    }

    private void loadDictionary() {
        try {
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("dictionary.json")
            );

            Type type = new TypeToken<HashMap<String, HashMap<Character, String[]>>>() {}.getType();
            personalDictionary = gson.fromJson(reader, type);

            reader.close();
            System.out.println("📚 Dictionnaire chargé pour " + gamerName);

        } catch (Exception e) {
            System.err.println("❌ Erreur de chargement du dictionnaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =============== ALGORITHMES DE RECHERCHE ===============

    private class SearchNode implements Comparable<SearchNode> {
        String theme;
        int depth;
        double cost;
        double heuristic;
        SearchNode parent;
        String word;

        SearchNode(String theme, int depth, double cost, SearchNode parent) {
            this.theme = theme;
            this.depth = depth;
            this.cost = cost;
            this.parent = parent;
            this.heuristic = calculateHeuristic(theme);
        }

        private double calculateHeuristic(String currentTheme) {
            String[] allThemes = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};
            int themesLeft = 0;
            boolean foundCurrent = false;
            for (String t : allThemes) {
                if (t.equals(currentTheme)) {
                    foundCurrent = true;
                }
                if (foundCurrent) {
                    themesLeft++;
                }
            }
            return themesLeft;
        }

        @Override
        public int compareTo(SearchNode other) {
            double f1 = this.cost + this.heuristic;
            double f2 = other.cost + other.heuristic;
            return Double.compare(f1, f2);
        }
    }

    // BFS - Recherche en largeur
    private HashMap<String, String> searchBFS(char firstLetter, String[] themes) {
        HashMap<String, String> results = new HashMap<>();
        Queue<SearchNode> queue = new LinkedList<>();
        
        System.out.println("🔍 " + gamerName + " utilise BFS");
        
        for (String theme : themes) {
            queue.offer(new SearchNode(theme, 0, 0, null));
        }

        while (!queue.isEmpty() && results.size() < themes.length) {
            SearchNode node = queue.poll();
            
            if (!results.containsKey(node.theme)) {
                String word = generateWord(node.theme, firstLetter);
                results.put(node.theme, word);
                
                try {
                    Thread.sleep(rand.nextInt(300) + 200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return results;
    }

    // DFS - Recherche en profondeur
    private HashMap<String, String> searchDFS(char firstLetter, String[] themes) {
        HashMap<String, String> results = new HashMap<>();
        Stack<SearchNode> stack = new Stack<>();
        
        System.out.println("🔍 " + gamerName + " utilise DFS");
        
        for (int i = themes.length - 1; i >= 0; i--) {
            stack.push(new SearchNode(themes[i], 0, 0, null));
        }

        while (!stack.isEmpty() && results.size() < themes.length) {
            SearchNode node = stack.pop();
            
            if (!results.containsKey(node.theme)) {
                String word = generateWord(node.theme, firstLetter);
                results.put(node.theme, word);
                
                try {
                    Thread.sleep(rand.nextInt(300) + 200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return results;
    }

    // UCS - Uniform Cost Search
    private HashMap<String, String> searchUCS(char firstLetter, String[] themes) {
        HashMap<String, String> results = new HashMap<>();
        PriorityQueue<SearchNode> pq = new PriorityQueue<>();
        
        System.out.println("🔍 " + gamerName + " utilise UCS");
        
        for (String theme : themes) {
            double cost = rand.nextDouble() * 5;
            pq.offer(new SearchNode(theme, 0, cost, null));
        }

        while (!pq.isEmpty() && results.size() < themes.length) {
            SearchNode node = pq.poll();
            
            if (!results.containsKey(node.theme)) {
                String word = generateWord(node.theme, firstLetter);
                results.put(node.theme, word);
                
                try {
                    Thread.sleep((long)(node.cost * 100) + 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return results;
    }

    // A* - A Star Search
    private HashMap<String, String> searchAStar(char firstLetter, String[] themes) {
        HashMap<String, String> results = new HashMap<>();
        PriorityQueue<SearchNode> pq = new PriorityQueue<>();
        
        System.out.println("🔍 " + gamerName + " utilise A*");
        
        for (int i = 0; i < themes.length; i++) {
            double cost = rand.nextDouble() * 3;
            SearchNode node = new SearchNode(themes[i], i, cost, null);
            pq.offer(node);
        }

        while (!pq.isEmpty() && results.size() < themes.length) {
            SearchNode node = pq.poll();
            
            if (!results.containsKey(node.theme)) {
                String word = generateWord(node.theme, firstLetter);
                results.put(node.theme, word);
                
                try {
                    Thread.sleep(rand.nextInt(200) + 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return results;
    }

    public String generateWord(String theme, char firstLetter) {
        if (!personalDictionary.containsKey(theme)) return firstLetter + "word";

        HashMap<Character, String[]> themeDict = personalDictionary.get(theme);
        if (!themeDict.containsKey(firstLetter)) return firstLetter + "word";

        String[] words = themeDict.get(firstLetter);
        return words[rand.nextInt(words.length)];
    }

    // ------------------- Comportement du joueur -------------------
    private class GamerBehaviour extends Behaviour {

        private Joueur agent;
        private boolean finished = false;
        private char firstLetter;
        private String[] themes = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};

        public GamerBehaviour(Joueur agent) {
            this.agent = agent;
        }

        @Override
        public void action() {
            switch (state) {
                case WAITING:
                    ACLMessage msgLetter = agent.receive();
                    if (msgLetter != null && msgLetter.getPerformative() == ACLMessage.INFORM) {
                        String content = msgLetter.getContent();
                        
                        if (content != null && content.length() == 1) {
                            firstLetter = content.charAt(0);
                            System.out.println("📩 " + gamerName + " a reçu la lettre : " + firstLetter);
                            hasSaidStop = false;
                            state = GameState.RUNNING;
                        }
                    } else {
                        block();
                    }
                    break;
                case RUNNING:
                    long startTime = System.currentTimeMillis();

                    // Choisir un nouvel algorithme à chaque manche
                    searchAlgorithm = chooseRandomAlgorithm();

                    // Exécuter la recherche
                    switch (searchAlgorithm) {
                        case BFS:
                            currentResponses = agent.searchBFS(firstLetter, themes);
                            break;
                        case DFS:
                            currentResponses = agent.searchDFS(firstLetter, themes);
                            break;
                        case UCS:
                            currentResponses = agent.searchUCS(firstLetter, themes);
                            break;
                        case ASTAR:
                            currentResponses = agent.searchAStar(firstLetter, themes);
                            break;
                    }

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // Dire STOP immédiatement après avoir trouvé ses réponses partielles
                    if (!hasSaidStop) {
                        ACLMessage stopMsg = new ACLMessage(ACLMessage.INFORM);
                        stopMsg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                        stopMsg.setContent("STOP");
                        agent.send(stopMsg);
                        hasSaidStop = true;
                        System.out.println("🛑 " + gamerName + " dit STOP ! (temps: " + duration + " ms)");
                    }

                    // Envoyer les réponses déjà remplies
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                    Gson gson = new Gson();

                    HashMap<String, Object> response = new HashMap<>();
                    response.put("responses", currentResponses);
                    response.put("algorithm", searchAlgorithm.toString());
                    response.put("time", duration);

                    msg.setContent(gson.toJson(response));
                    agent.send(msg);

                    System.out.println("📤 " + gamerName + " a envoyé ses réponses partielles\n");

                    state = GameState.WAITING;
                    break;


                /*case RUNNING:
                    long startTime = System.currentTimeMillis();
                    
                    // Utiliser l'algorithme approprié
                    switch (searchAlgorithm) {
                        case BFS:
                            currentResponses = agent.searchBFS(firstLetter, themes);
                            break;
                        case DFS:
                            currentResponses = agent.searchDFS(firstLetter, themes);
                            break;
                        case UCS:
                            currentResponses = agent.searchUCS(firstLetter, themes);
                            break;
                        case ASTAR:
                            currentResponses = agent.searchAStar(firstLetter, themes);
                            break;
                    }

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // ✅ DIRE STOP IMMÉDIATEMENT
                    if (!hasSaidStop) {
                        ACLMessage stopMsg = new ACLMessage(ACLMessage.INFORM);
                        stopMsg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                        stopMsg.setContent("STOP");
                        agent.send(stopMsg);
                        hasSaidStop = true;
                        System.out.println("🛑 " + gamerName + " dit STOP ! (temps: " + duration + " ms)");
                    }

                    // ✅ ENVOYER LES RÉPONSES AVEC MÉTADONNÉES
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                    Gson gson = new Gson();
                    
                    HashMap<String, Object> response = new HashMap<>();
                    response.put("responses", currentResponses);
                    response.put("algorithm", searchAlgorithm.toString());
                    response.put("time", duration);
                    
                    msg.setContent(gson.toJson(response));
                    agent.send(msg);

                    System.out.println("📤 " + gamerName + " a envoyé ses réponses\n");

                    state = GameState.WAITING;
                    break;*/

                case FINISHED:
                    finished = true;
                    break;
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}