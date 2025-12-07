package PetitBac;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomSearch implements SearchAlgorithm {
    private final Random rand = new Random();

    @Override
    public SearchResultSingle search(SearchProblem problem) {
        long start = System.nanoTime();
        List<SearchNode> nodes = problem.getInitialNodes();
        if (nodes.isEmpty()) {
            long timeNs = System.nanoTime() - start;
            return new SearchResultSingle("", 0, timeNs, "Random");
        }
        Collections.shuffle(nodes, rand);
        long nodesExpanded = 0;
        for (SearchNode n : nodes) {
            nodesExpanded++;
            if (problem.isGoal(n)) {
                long timeNs = System.nanoTime() - start;
                return new SearchResultSingle(n.value, nodesExpanded, timeNs, "Random");
            }
        }
        long timeNs = System.nanoTime() - start;
        return new SearchResultSingle("", nodesExpanded, timeNs, "Random");
    }
}
