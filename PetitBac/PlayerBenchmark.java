package PetitBac;

import java.util.Map;

public class PlayerBenchmark {
    public Map<String, SearchResult> algorithmResults; // algorithmName -> SearchResult

    public PlayerBenchmark() {}

    public PlayerBenchmark(Map<String, SearchResult> algorithmResults) {
        this.algorithmResults = algorithmResults;
    }
}
