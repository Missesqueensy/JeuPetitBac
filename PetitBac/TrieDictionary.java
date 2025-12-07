package PetitBac;

import java.util.Map;

public class TrieDictionary {
    public final TrieNode root;
    public final Map<Character, Double> letterCosts;
    public final double minEdgeCost;

    public TrieDictionary(TrieNode root, Map<Character, Double> letterCosts) {
        this.root = root;
        this.letterCosts = letterCosts;
        double min = Double.POSITIVE_INFINITY;
        for (double v : letterCosts.values()) if (v < min) min = v;
        if (Double.isInfinite(min)) min = 1.0;
        this.minEdgeCost = min;
    }
}
