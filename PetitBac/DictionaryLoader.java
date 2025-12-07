package PetitBac;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class DictionaryLoader {
    public static Map<String, HashMap<Character, String[]>> loadDictionary() {
        try {
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(DictionaryLoader.class.getClassLoader().getResourceAsStream("dictionary.json"));
            Type type = new TypeToken<Map<String, HashMap<Character, String[]>>>() {}.getType();
            Map<String, HashMap<Character, String[]>> map = gson.fromJson(reader, type);
            reader.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // Build a trie per theme and compute per-letter costs. Prefers dictionaryshuffled.json when available in classpath.
    public static java.util.Map<String, TrieDictionary> loadTrieDictionary() {
        try {
            Gson gson = new Gson();
            Reader reader = null;
            var cl = DictionaryLoader.class.getClassLoader();
            if (cl.getResourceAsStream("dictionaryshuffled.json") != null) {
                reader = new InputStreamReader(cl.getResourceAsStream("dictionaryshuffled.json"));
            } else if (cl.getResourceAsStream("dictionary.json") != null) {
                reader = new InputStreamReader(cl.getResourceAsStream("dictionary.json"));
            }
            if (reader == null) return new HashMap<>();
            Type type = new TypeToken<Map<String, HashMap<Character, String[]>>>() {}.getType();
            Map<String, HashMap<Character, String[]>> map = gson.fromJson(reader, type);
            reader.close();
            Map<String, TrieDictionary> result = new HashMap<>();
            for (var entry : map.entrySet()) {
                TrieNode root = new TrieNode();
                HashMap<Character, String[]> byChar = entry.getValue();
                java.util.Map<Character, Integer> counts = new HashMap<>();
                int maxCount = 0;
                for (var arr : byChar.values()) {
                    for (String w : arr) {
                        if (w == null || w.isEmpty()) continue;
                        root.add(w);
                        for (char c : w.toCharArray()) {
                            char lc = Character.toLowerCase(c);
                            int v = counts.getOrDefault(lc, 0) + 1;
                            counts.put(lc, v);
                            if (v > maxCount) maxCount = v;
                        }
                    }
                }
                // compute letter cost map: base 1.0, rarer letters slightly more expensive up to +1.0
                java.util.Map<Character, Double> letterCosts = new HashMap<>();
                if (maxCount == 0) maxCount = 1;
                for (var kv : counts.entrySet()) {
                    double cost = 1.0 + (double)(maxCount - kv.getValue()) / (double)maxCount; // in [1.0,2.0]
                    letterCosts.put(kv.getKey(), cost);
                }
                // ensure common letters have at least cost 1.0
                TrieDictionary td = new TrieDictionary(root, letterCosts);
                root.computeMinDepth();
                result.put(entry.getKey(), td);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
