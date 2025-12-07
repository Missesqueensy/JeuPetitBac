package PetitBac;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    public final Map<Character, TrieNode> children = new HashMap<>();
    public boolean isWord = false;
    public String word = null; // full word if isWord
    // distance to nearest terminal (0 if this node is a word)
    public int minDepthToWord = Integer.MAX_VALUE;

    public void add(String w) {
        TrieNode node = this;
        for (char c : w.toCharArray()) {
            node = node.children.computeIfAbsent(Character.toLowerCase(c), k -> new TrieNode());
        }
        node.isWord = true;
        node.word = w;
    }

    // compute minDepthToWord for each node (post-order)
    public int computeMinDepth() {
        if (isWord) {
            minDepthToWord = 0;
        } else {
            minDepthToWord = Integer.MAX_VALUE;
        }
        for (TrieNode child : children.values()) {
            int d = child.computeMinDepth();
            if (d != Integer.MAX_VALUE) {
                minDepthToWord = Math.min(minDepthToWord, 1 + d);
            }
        }
        return minDepthToWord;
    }

}
