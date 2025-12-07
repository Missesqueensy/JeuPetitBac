package PetitBac;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AgentGamersd extends Agent {

    private String gamerName;
    private int score;
    private HashMap<String, String> currentResponses = new HashMap<>();
    private GameState state;
    private AID aid;

    private Random rand = new Random();

    // Dictionnaire : thème -> (lettre -> liste de mots)
    private HashMap<String, HashMap<Character, String[]>> personalDictionary;

    public enum GameState {
        WAITING,
        RUNNING,
        FINISHED
    }

    @Override
    protected void setup() {
        aid = getAID();
        gamerName = getLocalName();
        score = 0;
        state = GameState.WAITING;

        System.out.println("Création et initialisation de l'agent joueur : " + gamerName);

        loadDictionary();

        addBehaviour(new GamersdBehaviour(this));
    }

    // Lecture du dictionnaire JSON
    private void loadDictionary() {
        try {
            Gson gson = new Gson();
            Reader reader = new FileReader("dictionary1.json");

            Type type = new TypeToken<HashMap<String, HashMap<Character, String[]>>>() {}.getType();
            personalDictionary = gson.fromJson(reader, type);

            reader.close();
            System.out.println("Dictionary loaded for themes: " + personalDictionary.keySet());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Génération d'un mot pour un thème et une lettre
    public String generateWord(String theme, char firstLetter) {
        if (!personalDictionary.containsKey(theme)) return firstLetter + "word";

        HashMap<Character, String[]> themeDict = personalDictionary.get(theme);
        if (!themeDict.containsKey(firstLetter)) return firstLetter + "word";

        String[] words = themeDict.get(firstLetter);
        return words[rand.nextInt(words.length)];
    }

    // ------------------- Comportement du joueur -------------------
    private class GamersdBehaviour extends Behaviour {

        private AgentGamersd agent;
        private boolean finished = false;
        private char firstLetter;
        private String[] themes = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};

        public GamersdBehaviour(AgentGamersd agentGamersd) {
            this.agent = agentGamersd;
        }

        @Override
        public void action() {
            switch (state) {
                case WAITING:
                    // Attente de la lettre choisie par l'arbitre
                    ACLMessage msgLetter = agent.receive();
                    if (msgLetter != null && msgLetter.getPerformative() == ACLMessage.INFORM) {
                        String letter = msgLetter.getContent();
                        if (letter != null && !letter.isEmpty()) {
                            firstLetter = letter.charAt(0);
                            System.out.println(gamerName + " received letter: " + firstLetter);
                            state = GameState.RUNNING;
                        }
                    } else {
                        block(); // bloque le comportement jusqu'à réception d'un message
                    }
                    break;

                case RUNNING:
                    currentResponses.clear();
                    for (String theme : themes) {
                        String word = agent.generateWord(theme, firstLetter);
                        currentResponses.put(theme, word);

                        // Simuler temps de réponse aléatoire
                        try {
                            Thread.sleep(rand.nextInt(1000) + 500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Envoyer les réponses à l'arbitre
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                    msg.setContent(currentResponses.toString());
                    agent.send(msg);

                    System.out.println(gamerName + " sent responses: " + currentResponses);

                    finished = true;
                    state = GameState.FINISHED;
                    break;

                case FINISHED:
                    // Attente d'une nouvelle lettre ou score
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

