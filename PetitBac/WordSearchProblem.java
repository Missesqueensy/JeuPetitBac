package PetitBac;

import java.util.ArrayList;
import java.util.List;

public class WordSearchProblem implements SearchProblem {
    private final List<SearchNode> initialNodes;
    private final char letter;

    public WordSearchProblem(List<String> candidates, char letter) {
        this.letter = letter;
        this.initialNodes = new ArrayList<>();
        for (String w : candidates) {
            initialNodes.add(new SearchNode(w, null, w.length(), 0));
        }
    }

    @Override
    public List<SearchNode> getInitialNodes() {
        return initialNodes;
    }

    @Override
    public boolean isGoal(SearchNode node) {
        return node != null && node.value != null && !node.value.isEmpty() && Character.toLowerCase(node.value.charAt(0)) == Character.toLowerCase(letter);
    }

    @Override
    public List<SearchNode> expand(SearchNode node) {
        // Flat problem: no children beyond the initial candidates
        return new ArrayList<>();
    }

    @Override
    public double cost(SearchNode node) {
        return node == null ? Double.POSITIVE_INFINITY : node.cost;
    }

    @Override
    public double heuristic(SearchNode node) {
        // Simple heuristic: prefer shorter words -> 0 for now
        return 0;
    }
}
