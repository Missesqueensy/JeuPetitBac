package PetitBac;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 

public class AgentJoueur extends Agent {
    private Map<String, HashMap<Character, String[]>> personalDictionary;
    private Map<String, TrieDictionary> trieDictionary;
    
    private final String[] themes = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};
    private String[] algorithms = {"Random","BFS","DFS","UCS","A*"};
    private final Gson gson = new Gson();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " starting.");
        personalDictionary = DictionaryLoader.loadDictionary();
        trieDictionary = DictionaryLoader.loadTrieDictionary();
        // Read startup args for algorithm list (passed as String[])
        Object[] startup = getArguments();
        if (startup != null && startup.length > 0) {
            Object first = startup[0];
            if (first instanceof String[]) {
                algorithms = (String[]) first;
            } else if (first instanceof String) {
                // allow comma-separated list
                algorithms = ((String) first).split(",");
            }
        }
        System.out.println(getLocalName() + " algorithms=" + java.util.Arrays.toString(algorithms));
        addBehaviour(new WaitLetterBehaviour());
    }

    private class WaitLetterBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                String letterStr = msg.getContent();
                if (letterStr == null || letterStr.isEmpty()) return;
                char letter = letterStr.charAt(0);

                // Run all algorithms for this letter (benchmarking)
                Map<String, SearchResult> allResults = new HashMap<>();
                for (String algoName : algorithms) {
                    SearchAlgorithm algo = createAlgorithm(algoName);

                    Map<String,String> responses = new HashMap<>();
                    java.util.Map<String, Long> nodesPerTheme = new HashMap<>();
                    java.util.Map<String, Long> timePerTheme = new HashMap<>();
                    long totalNodes = 0;
                    long totalTimeNs = 0;

                    for (String theme : themes) {
                        // prefer trie-backed search problem for more discriminating algorithms
                        if (trieDictionary != null && trieDictionary.containsKey(theme)) {
                            TrieDictionary td = trieDictionary.get(theme);
                            TrieWordSearchProblem problem = new TrieWordSearchProblem(td, letter);
                            SearchResultSingle singleResult = algo.search(problem);
                            String chosen = singleResult.word == null ? "" : singleResult.word;
                            responses.put(theme, chosen);
                            nodesPerTheme.put(theme, singleResult.nodesExpanded);
                            timePerTheme.put(theme, singleResult.timeNs);
                            nodesPerTheme.put(theme, singleResult.nodesExpanded);
                            timePerTheme.put(theme, singleResult.timeNs);
                            totalNodes += singleResult.nodesExpanded;
                            totalTimeNs += singleResult.timeNs;
                        } else {
                            List<String> candidates = new ArrayList<>();
                            if (personalDictionary.containsKey(theme)) {
                                HashMap<Character, String[]> themeMap = personalDictionary.get(theme);
                                for (Map.Entry<Character, String[]> e : themeMap.entrySet()) {
                                    if (Character.toLowerCase(e.getKey()) == Character.toLowerCase(letter)) {
                                        for (String w : e.getValue()) candidates.add(w);
                                    }
                                }
                            }
                            WordSearchProblem problem = new WordSearchProblem(candidates, letter);
                            SearchResultSingle singleResult = algo.search(problem);
                            String chosen = singleResult.word == null ? "" : singleResult.word;
                            responses.put(theme, chosen);
                            nodesPerTheme.put(theme, singleResult.nodesExpanded);
                            timePerTheme.put(theme, singleResult.timeNs);
                            totalNodes += singleResult.nodesExpanded;
                            totalTimeNs += singleResult.timeNs;
                        }
                    }

                    SearchResult aggregated = new SearchResult(algoName, totalNodes, totalTimeNs, responses, nodesPerTheme, timePerTheme);
                    allResults.put(algoName, aggregated);
                }

                PlayerBenchmark pb = new PlayerBenchmark(allResults);
                String json = gson.toJson(pb);

                ACLMessage out = new ACLMessage(ACLMessage.INFORM);
                out.addReceiver(new AID("ArbitreAgent", AID.ISLOCALNAME));
                out.setContent(json);
                myAgent.send(out);
                System.out.println(getLocalName() + " sent benchmark results for letter " + letter);
            } else {
                block();
            }
        }
    }

    private SearchAlgorithm createAlgorithm(String name) {
        switch (name) {
            case "Random": return new RandomSearch();
            case "BFS": return new BFSSearch();
            case "DFS": return new DFSSearch();
            case "UCS": return new UCSSearch();
            case "A*": return new AStarSearch();
            default: return new RandomSearch();
        }
    }
}
