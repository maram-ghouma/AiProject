package code;

import java.util.*;

public abstract class GenericSearch {
    //car chaque fils va le definir selon ses besoins
    public abstract String getInitialState();
    public abstract boolean isGoal(Object state);
    public abstract List<Node> expand(Node node);
    public abstract double getStepCost(Object state, String action, Object nextState);
    private int depthLimit = 10;
    public abstract double heuristic1(Object state, Object goal);
    public abstract double heuristic2(Object state, Object goal);
    protected Object goal = null;
    public void setDepthLimit(int d) { this.depthLimit = d; }
    public Node solve(String strategy) {
        Queue<Node> frontier = makeQueue(makeNode(getInitialState()));
        Set<Object> explored = new HashSet<>();
        while (!frontier.isEmpty()) {
            Node node = removeFront(frontier);
            if (isGoal(node.state)) {
                System.out.println("Goal found! Expanded nodes: " + Node.expandedCount);
                return node;
            }
            if (explored.contains(node.state)) continue; // ADD THIS
            explored.add(node.state);
            Node.expandedCount++;
            List<Node> children = expand(node);
            frontier = qingFun(strategy, frontier, children);
        }
        System.out.println("Failure: No solution found.");
        return null;
    }

    protected Node makeNode(Object state) {
        return new Node(state, null, null, 0);
    }

    protected Queue<Node> makeQueue(Node root) {
        Queue<Node> q = new LinkedList<>();
        q.add(root);
        return q;
    }
    protected Node removeFront(Queue<Node> frontier) {
        return frontier.poll();
    }

    protected Queue<Node> qingFun(String strategy, Queue<Node> frontier, List<Node> newNodes) {
        switch (strategy) {
            case "BF": // Breadth-First Search
                // Add to end of queue (FIFO)
                for (Node node : newNodes) {
                    frontier.add(node);
                }
                break;
                
            case "DF": // Depth-First Search
                // Add to front of queue (LIFO) - use stack behavior
                Queue<Node> newFrontier = new LinkedList<>();
                for (Node node : newNodes) {
                    newFrontier.add(node);
                }
                for (Node node : frontier) {
                    newFrontier.add(node);
                }
                return newFrontier;
            case "ID": // Iterative Deepening Search (Depth-Limited DFS)
                Queue<Node> idFrontier = new LinkedList<>();    

                for (Node node : newNodes) {
                    if (node.getDepth() <= depthLimit) {
                        // LIFO behavior (like DF)
                        idFrontier.add(node);
                    }
                }   

                for (Node node : frontier) {
                    idFrontier.add(node);
                }   

                return idFrontier;  

            case "UC": // Uniform-Cost Search
                for (Node node : newNodes) {
                    frontier.add(node);
                }   

                // Sort by path cost g(n)
                List<Node> sorted = new ArrayList<>(frontier);
                sorted.sort(Comparator.comparingDouble(n -> n.pathCost));   

                return new LinkedList<>(sorted);    

            case "GR1": // Greedy Search with h1
                for (Node node : newNodes) {
                    frontier.add(node);
                }   

                List<Node> gr1Sorted = new ArrayList<>(frontier);
                gr1Sorted.sort(Comparator.comparingDouble(n -> heuristic1(n.state, this.goal)));
                return new LinkedList<>(gr1Sorted); 

            case "GR2": // Greedy Search with h2
                for (Node node : newNodes) {
                    frontier.add(node);
                }   

                List<Node> gr2Sorted = new ArrayList<>(frontier);
                gr2Sorted.sort(Comparator.comparingDouble(n -> heuristic2(n.state, this.goal)));
                return new LinkedList<>(gr2Sorted); 

            case "AS1": // A* Search with h1
                for (Node node : newNodes) {
                    frontier.add(node);
                }   

                List<Node> as1Sorted = new ArrayList<>(frontier);
                as1Sorted.sort(Comparator.comparingDouble(n -> n.pathCost + heuristic1(n.state,this.goal)));
                return new LinkedList<>(as1Sorted); 

            case "AS2": // A* Search with h2
                for (Node node : newNodes) {
                    frontier.add(node);
                }   

                List<Node> as2Sorted = new ArrayList<>(frontier);
                as2Sorted.sort(Comparator.comparingDouble(n -> n.pathCost + heuristic2(n.state, this.goal)));
                return new LinkedList<>(as2Sorted); 

            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
        
        return frontier;
    }
}
