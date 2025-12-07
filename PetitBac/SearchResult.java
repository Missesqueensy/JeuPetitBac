package PetitBac;

import java.util.Map;

public class SearchResult {
    public String algorithmName;
    public long nodesExpanded;
    public long timeNs;
    public Map<String, String> responses; // theme -> chosen word
    public Map<String, Long> nodesPerTheme; // theme -> nodes expanded
    public Map<String, Long> timePerTheme; // theme -> time ns
    public boolean foundAny;

    public SearchResult() {}

    public SearchResult(String algorithmName, long nodesExpanded, long timeNs, Map<String, String> responses,
                        Map<String, Long> nodesPerTheme, Map<String, Long> timePerTheme) {
        this.algorithmName = algorithmName;
        this.nodesExpanded = nodesExpanded;
        this.timeNs = timeNs;
        this.responses = responses;
        this.nodesPerTheme = nodesPerTheme;
        this.timePerTheme = timePerTheme;
        this.foundAny = responses != null && !responses.isEmpty();
    }
}
