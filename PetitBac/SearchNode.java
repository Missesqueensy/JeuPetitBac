package PetitBac;

public class SearchNode {
    public final String value;
    public final SearchNode parent;
    public final double cost;
    public final int depth;

    public SearchNode(String value) {
        this(value, null, 0, 0);
    }

    public SearchNode(String value, SearchNode parent, double cost, int depth) {
        this.value = value;
        this.parent = parent;
        this.cost = cost;
        this.depth = depth;
    }
}
