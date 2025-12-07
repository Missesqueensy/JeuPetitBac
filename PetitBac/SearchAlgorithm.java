package PetitBac;

public interface SearchAlgorithm {
    // Search a single SearchProblem and return the chosen word result (nodes/time included)
    SearchResultSingle search(SearchProblem problem);
}

// Helper single-theme search result
class SearchResultSingle {
    public final String word;
    public final long nodesExpanded;
    public final long timeNs;
    public final String algorithmName;
    public SearchResultSingle(String word, long nodesExpanded, long timeNs, String algorithmName) {
        this.word = word;
        this.nodesExpanded = nodesExpanded;
        this.timeNs = timeNs;
        this.algorithmName = algorithmName;
    }
}
