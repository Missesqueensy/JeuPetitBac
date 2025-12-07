package PetitBac;

import java.util.ArrayList;
import java.util.List;

public class TrieWordSearchProblem implements SearchProblem {
    private final TrieNode root;
    private final char startLetter;
    private final java.util.Map<Character, Double> letterCosts;
    private final double minEdgeCost;

    public TrieWordSearchProblem(TrieDictionary dict, char startLetter) {
        this.root = dict.root;
        this.letterCosts = dict.letterCosts;
        this.minEdgeCost = dict.minEdgeCost;
        this.startLetter = Character.toLowerCase(startLetter);
        // precompute heuristics
        root.computeMinDepth();
    }

    @Override
    public List<SearchNode> getInitialNodes() {
        List<SearchNode> list = new ArrayList<>();
        TrieNode child = root.children.get(startLetter);
        if (child != null) {
            String prefix = String.valueOf(startLetter);
            SearchNode node = new SearchNode(prefix, null, 0, 1);
            list.add(node);
        }
        return list;
    }

    @Override
    public boolean isGoal(SearchNode node) {
        // goal if the current prefix is a full word in the trie
        String prefix = node.value;
        TrieNode tn = follow(prefix);
        return tn != null && tn.isWord;
    }

    @Override
    public List<SearchNode> expand(SearchNode node) {
        List<SearchNode> next = new ArrayList<>();
        TrieNode tn = follow(node.value);
        if (tn == null) return next;
        for (var e : tn.children.entrySet()) {
            char c = e.getKey();
            String newPrefix = node.value + c;
            double edgeCost = 1.0;
            if (letterCosts != null && letterCosts.containsKey(Character.toLowerCase(c))) {
                edgeCost = letterCosts.get(Character.toLowerCase(c));
            }
            SearchNode child = new SearchNode(newPrefix, node, node.cost + edgeCost, node.depth + 1);
            next.add(child);
        }
        return next;
    }

    @Override
    public double cost(SearchNode node) {
        // return cumulative cost stored on the node
        return node.cost;
    }

    @Override
    public double heuristic(SearchNode node) {
        TrieNode tn = follow(node.value);
        if (tn == null) return Double.POSITIVE_INFINITY;
        // admissible heuristic = minimum remaining letters * cheapest per-letter cost
        return tn.minDepthToWord * Math.max(1.0, minEdgeCost);
    }

    private TrieNode follow(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(Character.toLowerCase(c));
            if (node == null) return null;
        }
        return node;
    }
}
