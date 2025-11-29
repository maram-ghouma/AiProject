package code;

import java.util.*;

public abstract class GenericSearch {
    //car chaque fils va le definir selon ses besoins
    public abstract String getInitialState();
    public abstract boolean isGoal(Object state);
    public abstract List<Node> expand(Node node);
    public abstract double getStepCost(Object state, String action, Object nextState);
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
/*
    protected Queue<Node> qingFun(String strategy, Queue<Node> frontier, List<Node> newNodes) {
        switch (strategy) {
            //todo: implement the queuing function depending on the strategy
        }
        return frontier;
    }*/
protected Queue<Node> qingFun(String strategy, Queue<Node> frontier, List<Node> newNodes) {
    // Simple BF implementation for testing
    if (strategy.equals("BF")) {
        for (Node node : newNodes) {
            frontier.add(node);
        }
    }
    return frontier;
}
}
