package PetitBac;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class DFSSearch implements SearchAlgorithm {
    @Override
    public SearchResultSingle search(SearchProblem problem) {
        long start = System.nanoTime();
        Deque<SearchNode> stack = new LinkedList<>();
        List<SearchNode> initials = problem.getInitialNodes();
        for (SearchNode n : initials) stack.push(n);

        long nodesExpanded = 0;
        while (!stack.isEmpty()) {
            SearchNode cur = stack.pop();
            nodesExpanded++;
            if (problem.isGoal(cur)) {
                long timeNs = System.nanoTime() - start;
                return new SearchResultSingle(cur.value, nodesExpanded, timeNs, "DFS");
            }
            for (SearchNode child : problem.expand(cur)) {
                stack.push(child);
            }
        }
        long timeNs = System.nanoTime() - start;
        return new SearchResultSingle("", nodesExpanded, timeNs, "DFS");
    }
}
