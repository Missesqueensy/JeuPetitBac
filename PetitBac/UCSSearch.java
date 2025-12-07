package PetitBac;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class UCSSearch implements SearchAlgorithm {
    @Override
    public SearchResultSingle search(SearchProblem problem) {
        long start = System.nanoTime();
        PriorityQueue<SearchNode> pq = new PriorityQueue<>(Comparator.comparingDouble(problem::cost));
        List<SearchNode> initials = problem.getInitialNodes();
        for (SearchNode n : initials) pq.add(n);

        long nodesExpanded = 0;
        while (!pq.isEmpty()) {
            SearchNode cur = pq.poll();
            nodesExpanded++;
            if (problem.isGoal(cur)) {
                long timeNs = System.nanoTime() - start;
                return new SearchResultSingle(cur.value, nodesExpanded, timeNs, "UCS");
            }
            for (SearchNode child : problem.expand(cur)) {
                pq.add(child);
            }
        }
        long timeNs = System.nanoTime() - start;
        return new SearchResultSingle("", nodesExpanded, timeNs, "UCS");
    }
}
