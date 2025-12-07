package PetitBac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class BFSSearch implements SearchAlgorithm {
    @Override
    public SearchResultSingle search(SearchProblem problem) {
        long start = System.nanoTime();
        Deque<SearchNode> queue = new ArrayDeque<>();
        List<SearchNode> initials = problem.getInitialNodes();
        for (SearchNode n : initials) queue.addLast(n);

        long nodesExpanded = 0;
        while (!queue.isEmpty()) {
            SearchNode cur = queue.removeFirst();
            nodesExpanded++;
            if (problem.isGoal(cur)) {
                long timeNs = System.nanoTime() - start;
                return new SearchResultSingle(cur.value, nodesExpanded, timeNs, "BFS");
            }
            for (SearchNode child : problem.expand(cur)) {
                queue.addLast(child);
            }
        }
        long timeNs = System.nanoTime() - start;
        return new SearchResultSingle("", nodesExpanded, timeNs, "BFS");
    }
}
