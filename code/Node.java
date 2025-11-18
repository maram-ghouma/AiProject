package code;

public class Node {
    public Object state;
    public Node parent;
    public String action;
    public double pathCost;
    public static int expandedCount = 0;

    public Node(Object state, Node parent, String action, double pathCost) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
    }
}
