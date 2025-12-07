package PetitBac;

import java.util.List;

public interface SearchProblem {
    List<SearchNode> getInitialNodes();
    boolean isGoal(SearchNode node);
    List<SearchNode> expand(SearchNode node);
    double cost(SearchNode node);
    double heuristic(SearchNode node);
}
