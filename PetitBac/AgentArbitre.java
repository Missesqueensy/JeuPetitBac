package PetitBac;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AgentArbitre extends Agent {
    private Map<String, HashMap<Character, String[]>> dictionary;
    private final Gson gson = new Gson();
    private final CSVLogger logger = new CSVLogger("petitbac_benchmark.csv");
    private final Random rand = new Random();
    private final String[] players = {"Joueur1", "Joueur2"};
    private final String[] themes = {"Country","City","GirlName","BoyName","Fruit","Color","Object"};
    private int round = 0;
    private final int maxRounds = 5;

    @Override
    protected void setup() {
        System.out.println("Arbitre starting...");
        dictionary = DictionaryLoader.loadDictionary();
        addBehaviour(new RefereeBehaviour());
    }

    private class RefereeBehaviour extends Behaviour {
        private int step = 0;
        private int responsesReceived = 0;
        private final Map<String, PlayerBenchmark> received = new HashMap<>();
        private char letter; // current round letter
        // algorithmsList is no longer fixed; we will compute the union of algorithms sent by players each round

        @Override
        public void action() {
            switch (step) {
                case 0:
                    if (round >= maxRounds) {
                        System.out.println("All rounds completed.");
                        step = 3;
                        return;
                    }
                    round++;
                    letter = (char) ('A' + rand.nextInt(26));
                    System.out.println("Round " + round + " - letter: " + letter);
                    ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                    m.setContent(String.valueOf(letter));
                    for (String p : players) m.addReceiver(new AID(p, AID.ISLOCALNAME));
                    send(m);
                    responsesReceived = 0;
                    received.clear();
                    step = 1;
                    break;

                case 1:
                    ACLMessage reply = receive();
                    if (reply != null && reply.getPerformative() == ACLMessage.INFORM) {
                        String sender = reply.getSender().getLocalName();
                        String json = reply.getContent();
                        try {
                            PlayerBenchmark pb = gson.fromJson(json, PlayerBenchmark.class);
                            received.put(sender, pb);
                            responsesReceived++;
                            System.out.println("Got benchmark from " + sender + " (algorithms=" + (pb.algorithmResults == null ? 0 : pb.algorithmResults.size()) + ")");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        block();
                    }

                    if (responsesReceived >= players.length) {
                        // DEBUG: print exactly which algorithms each player reported this round
                        for (String p : players) {
                            PlayerBenchmark pb = received.get(p);
                            if (pb == null || pb.algorithmResults == null) {
                                System.out.println(p + " reported algorithms: []");
                            } else {
                                System.out.println(p + " reported algorithms: " + pb.algorithmResults.keySet());
                            }
                        }

                        // Build the union of algorithms provided by players this round
                        java.util.Set<String> algosToScore = new java.util.HashSet<>();
                        for (String p : players) {
                            PlayerBenchmark pb = received.get(p);
                            if (pb != null && pb.algorithmResults != null) algosToScore.addAll(pb.algorithmResults.keySet());
                        }

                        // For each algorithm present in at least one player's benchmark, compute scores
                        // and write CSV rows only for players that actually ran that algorithm.
                        for (String algo : algosToScore) {
                            Map<String, Integer> scores = computeScoresForAlgorithm(received, letter, algo);
                            for (String p : players) {
                                PlayerBenchmark pb = received.get(p);
                                // Skip logging and printing for this player if they didn't run this algorithm
                                if (pb == null || pb.algorithmResults == null || !pb.algorithmResults.containsKey(algo)) {
                                    continue;
                                }
                                SearchResult r = pb.algorithmResults.get(algo);
                                int score = scores.getOrDefault(p, 0);
                                String chosenWords = r == null ? "" : r.responses.toString();
                                long nodes = r == null ? 0 : r.nodesExpanded;
                                double timeMs = r == null ? 0.0 : (r.timeNs / 1_000_000.0);
                                logger.append(round, p, algo, nodes, timeMs, score, chosenWords);
                                System.out.println(String.format("%s [%s] scored %d (nodes=%d timeMs=%.3f)", p, algo, score, nodes, timeMs));
                            }
                        }

                        step = 0; // next round
                    }
                    break;

                case 3:
                    doDelete();
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 3;
        }
    }

    private Map<String,Integer> computeScoresForAlgorithm(Map<String, PlayerBenchmark> received, char letter, String algorithm) {
        Map<String,Integer> scores = new HashMap<>();
        // init
        for (String p : players) scores.put(p, 0);

        // For each theme, compare players' answers for this algorithm
        for (String theme : themes) {
            String a = safeGetForAlgorithm(received, players[0], theme, algorithm);
            String b = safeGetForAlgorithm(received, players[1], theme, algorithm);

            boolean aValid = isValidWord(theme, a, letter);
            boolean bValid = isValidWord(theme, b, letter);

            // retrieve per-player SearchResult for this algorithm
            SearchResult ra = null;
            SearchResult rb = null;
            PlayerBenchmark pba = received.get(players[0]);
            PlayerBenchmark pbb = received.get(players[1]);
            if (pba != null && pba.algorithmResults != null) ra = pba.algorithmResults.get(algorithm);
            if (pbb != null && pbb.algorithmResults != null) rb = pbb.algorithmResults.get(algorithm);

            if (aValid && bValid) {
                if (a.equalsIgnoreCase(b)) {
                    // same valid answer: small shared reward
                    int baseSame = 5;
                    scores.put(players[0], scores.get(players[0]) + baseSame);
                    scores.put(players[1], scores.get(players[1]) + baseSame);
                    System.out.println(String.format("Theme=%s alg=%s sameAnswer -> +%d each", theme, algorithm, baseSame));
                } else {
                    // aggressive ratio-based scoring
                    int baseDifferent = 25; // larger base so ratios matter

                    long nodesA = (ra == null || ra.nodesPerTheme == null) ? Long.MAX_VALUE : ra.nodesPerTheme.getOrDefault(theme, Long.MAX_VALUE);
                    long nodesB = (rb == null || rb.nodesPerTheme == null) ? Long.MAX_VALUE : rb.nodesPerTheme.getOrDefault(theme, Long.MAX_VALUE);
                    long tA = (ra == null || ra.timePerTheme == null) ? Long.MAX_VALUE : ra.timePerTheme.getOrDefault(theme, Long.MAX_VALUE);
                    long tB = (rb == null || rb.timePerTheme == null) ? Long.MAX_VALUE : rb.timePerTheme.getOrDefault(theme, Long.MAX_VALUE);

                    double nodeWeight = 2.5; // give strong importance to node efficiency
                    double timeWeight = 1.0; // time also matters

                    double ratioA_nodes = (nodesA == Long.MAX_VALUE) ? 1.0 : ((double)nodesB) / Math.max(1.0, (double)nodesA);
                    double ratioB_nodes = (nodesB == Long.MAX_VALUE) ? 1.0 : ((double)nodesA) / Math.max(1.0, (double)nodesB);

                    double ratioA_time = (tA == Long.MAX_VALUE) ? 1.0 : ((double)Math.max(1.0, (double)tB)) / Math.max(1.0, (double)tA);
                    double ratioB_time = (tB == Long.MAX_VALUE) ? 1.0 : ((double)Math.max(1.0, (double)tA)) / Math.max(1.0, (double)tB);

                    double effA = 1.0 + (ratioA_nodes - 1.0) * nodeWeight + (ratioA_time - 1.0) * timeWeight;
                    double effB = 1.0 + (ratioB_nodes - 1.0) * nodeWeight + (ratioB_time - 1.0) * timeWeight;

                    // amplify and clamp
                    effA = Math.max(0.1, Math.min(10.0, effA));
                    effB = Math.max(0.1, Math.min(10.0, effB));

                    int scoreA = (int)Math.round(baseDifferent * effA);
                    int scoreB = (int)Math.round(baseDifferent * effB);
                    scores.put(players[0], scores.get(players[0]) + scoreA);
                    scores.put(players[1], scores.get(players[1]) + scoreB);

                    System.out.println(String.format("Theme=%s alg=%s nodesA=%d nodesB=%d tA=%.3fms tB=%.3fms ratioNodesA=%.3f ratioTimeA=%.3f effA=%.3f scoreA=%d | ratioNodesB=%.3f ratioTimeB=%.3f effB=%.3f scoreB=%d",
                            theme, algorithm,
                            nodesA == Long.MAX_VALUE ? -1 : nodesA,
                            nodesB == Long.MAX_VALUE ? -1 : nodesB,
                            (tA == Long.MAX_VALUE ? -1.0 : tA / 1_000_000.0),
                            (tB == Long.MAX_VALUE ? -1.0 : tB / 1_000_000.0),
                            ratioA_nodes, ratioA_time, effA, scoreA,
                            ratioB_nodes, ratioB_time, effB, scoreB));
                }
            } else if (aValid) {
                // only A valid: give large score inversely proportional to its cost
                int baseOnly = 30;
                long nodesA = (ra == null || ra.nodesPerTheme == null) ? Long.MAX_VALUE : ra.nodesPerTheme.getOrDefault(theme, Long.MAX_VALUE);
                long tA = (ra == null || ra.timePerTheme == null) ? Long.MAX_VALUE : ra.timePerTheme.getOrDefault(theme, Long.MAX_VALUE);
                double nodeBonus = (nodesA == Long.MAX_VALUE) ? 1.0 : (1000.0 / Math.max(1.0, (double)nodesA));
                double timeMs = (tA == Long.MAX_VALUE) ? 1000.0 : (double)tA / 1_000_000.0;
                double timeBonus = 100.0 / Math.max(0.001, timeMs);
                int add = (int)Math.round(baseOnly + nodeBonus * 5.0 + timeBonus * 2.0);
                scores.put(players[0], scores.get(players[0]) + add);
                System.out.println(String.format("Theme=%s alg=%s onlyA valid nodesA=%d timeA=%.3fms add=%d", theme, algorithm, nodesA == Long.MAX_VALUE ? -1 : nodesA, timeMs, add));
            } else if (bValid) {
                int baseOnly = 30;
                long nodesB = (rb == null || rb.nodesPerTheme == null) ? Long.MAX_VALUE : rb.nodesPerTheme.getOrDefault(theme, Long.MAX_VALUE);
                long tB = (rb == null || rb.timePerTheme == null) ? Long.MAX_VALUE : rb.timePerTheme.getOrDefault(theme, Long.MAX_VALUE);
                double nodeBonus = (nodesB == Long.MAX_VALUE) ? 1.0 : (1000.0 / Math.max(1.0, (double)nodesB));
                double timeMs = (tB == Long.MAX_VALUE) ? 1000.0 : (double)tB / 1_000_000.0;
                double timeBonus = 100.0 / Math.max(0.001, timeMs);
                int add = (int)Math.round(baseOnly + nodeBonus * 5.0 + timeBonus * 2.0);
                scores.put(players[1], scores.get(players[1]) + add);
                System.out.println(String.format("Theme=%s alg=%s onlyB valid nodesB=%d timeB=%.3fms add=%d", theme, algorithm, nodesB == Long.MAX_VALUE ? -1 : nodesB, timeMs, add));
            }
        }

        return scores;
    }

    private String safeGetForAlgorithm(Map<String, PlayerBenchmark> map, String player, String theme, String algorithm) {
        PlayerBenchmark pb = map.get(player);
        if (pb == null || pb.algorithmResults == null) return "";
        SearchResult r = pb.algorithmResults.get(algorithm);
        if (r == null || r.responses == null) return "";
        String s = r.responses.get(theme);
        return s == null ? "" : s;
    }

    private boolean isValidWord(String theme, String word, char letter) {
        if (word == null || word.isEmpty()) return false;
        if (Character.toLowerCase(word.charAt(0)) != Character.toLowerCase(letter)) return false;
        if (!dictionary.containsKey(theme)) return false;
        HashMap<Character, String[]> themeMap = dictionary.get(theme);
        char key = Character.toLowerCase(word.charAt(0));
        for (Map.Entry<Character, String[]> e : themeMap.entrySet()) {
            // keys in dictionary might be upper/lower
            if (Character.toLowerCase(e.getKey()) == key) {
                for (String w : e.getValue()) if (w.equalsIgnoreCase(word)) return true;
            }
        }
        return false;
    }
}
