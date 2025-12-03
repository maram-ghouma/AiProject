package code;
import java.util.*;


public class DeliverySearch extends GenericSearch {
    public int m; 
    public int n; 
    public int p; 
    public int s; 
    public Set<Tunnel> tunnels = new HashSet<>();
    public Set<Coord> customers = new HashSet<>();
    public Map<Edge, Integer> traffic = new HashMap<>();
    public List<Coord> stores = new ArrayList<>();

    public static class Coord {
        public int x, y;
        public Coord(int x, int y){ this.x = x; this.y = y; }
        @Override public boolean equals(Object o){
            if (!(o instanceof Coord)) return false;
            Coord c = (Coord)o; return x==c.x && y==c.y;
        }
        @Override public int hashCode(){ return Objects.hash(x,y); }
    }

    public static class Tunnel {
        public Coord a, b;
        public Tunnel(Coord a, Coord b){ this.a=a; this.b=b; }
    }

    public static class Edge {
        public Coord src, dst;
        public Edge(Coord s, Coord d){ this.src=s; this.dst=d; }
        @Override public boolean equals(Object o){
            if (!(o instanceof Edge)) return false;
            Edge e = (Edge)o;
            return src.equals(e.src) && dst.equals(e.dst);
        }
        @Override public int hashCode(){ return Objects.hash(src,dst); }
    }

    @Override
    public String getInitialState(){
    StringBuilder sb = new StringBuilder();
        sb.append(m).append(";").append(n).append(";").append(p).append(";").append(s).append(";");

        for (Coord c : customers) {
            sb.append(c.x).append(",").append(c.y).append(",");
        }
        if (!customers.isEmpty())
            sb.setLength(sb.length() - 1);
        sb.append(";");

        for (Tunnel t : tunnels) {
            sb.append(t.a.x).append(",").append(t.a.y).append(",")
            .append(t.b.x).append(",").append(t.b.y).append(",");
        }
        if (!tunnels.isEmpty())
            sb.setLength(sb.length() - 1);
        sb.append(";");

            for (Coord store : stores) {
        sb.append(store.x).append(",").append(store.y).append(",");
    }
        if (!stores.isEmpty())
            sb.setLength(sb.length() - 1);

        return sb.toString();
    }
@Override
public boolean isGoal(Object stateObj) {
    String state = (String) stateObj;
    String[] parts = state.split(";");
    if (parts.length < 2) return false;
    
    String deliveredStr = parts[1];
    if (deliveredStr.isEmpty()) return customers.isEmpty();
    
    String[] coords = deliveredStr.split(",");
    return (coords.length / 2) == customers.size();
}


    @Override
    public List<Node> expand(Node node) {
        List<Node> children = new ArrayList<>();

        // Parse the current state
        String[] parts = node.state.toString().split(";");
        String[] posParts = parts[0].split(",");
        int x = Integer.parseInt(posParts[0]);
        int y = Integer.parseInt(posParts[1]);

        Set<Coord> delivered = new HashSet<>();
        if (parts.length > 1 && !parts[1].isEmpty()) {
            String[] deliveredParts = parts[1].split(",");
            for (int i = 0; i < deliveredParts.length; i += 2) {
                delivered.add(new Coord(Integer.parseInt(deliveredParts[i]), Integer.parseInt(deliveredParts[i + 1])));
            }
        }

        // Possible moves: up, down, left, right
        int[][] moves = { {0,-1}, {0,1}, {-1,0}, {1,0} };
        String[] moveNames = { "up", "down", "left", "right" };

        for (int i = 0; i < moves.length; i++) {
            int nx = x + moves[i][0];
            int ny = y + moves[i][1];

            // Check bounds
            if (nx >= 0 && nx < n && ny >= 0 && ny < m) {
                Coord nextCoord = new Coord(nx, ny);
                Set<Coord> newDelivered = new HashSet<>(delivered);
                if (customers.contains(nextCoord)) newDelivered.add(nextCoord);

                
                Coord currentCoord = new Coord(x,y);
                Edge e = new Edge(currentCoord, nextCoord);
                Integer trafficLevel = traffic.get(e);
                if (trafficLevel != null && trafficLevel == 0) {
                    continue;
                }

                // Build new state string
                StringBuilder sb = new StringBuilder();
                sb.append(nx).append(",").append(ny).append(";");
                for (Coord c : newDelivered) sb.append(c.x).append(",").append(c.y).append(",");
                if (!newDelivered.isEmpty()) sb.setLength(sb.length() - 1);
                sb.append(";");

                // Create new Node
                Node child = new Node(sb.toString(), node, moveNames[i],
                        node.pathCost + getStepCost(node.state, moveNames[i], sb.toString()));
                children.add(child);
            }
        }

        // Handle tunnels
        for (Tunnel t : tunnels) {
            Coord entrance = null, exit = null;
            if (t.a.equals(new Coord(x, y))) { entrance = t.a; exit = t.b; }
            else if (t.b.equals(new Coord(x, y))) { entrance = t.b; exit = t.a; }
            if (entrance != null) {
                Set<Coord> newDelivered = new HashSet<>(delivered);
                if (customers.contains(exit)) newDelivered.add(exit);

                // Build new state string
                StringBuilder sb = new StringBuilder();
                sb.append(exit.x).append(",").append(exit.y).append(";");
                for (Coord c : newDelivered) sb.append(c.x).append(",").append(c.y).append(",");
                if (!newDelivered.isEmpty()) sb.setLength(sb.length() - 1);
                sb.append(";");

                Node child = new Node(sb.toString(), node, "tunnel",
                        node.pathCost + getStepCost(node.state, "tunnel", sb.toString()));
                children.add(child);
            }
        }

        return children;
    }


@Override
public double getStepCost(Object stateObj, String action, Object nextStateObj) {
    // Parse current position
    String state = (String) stateObj;
    String nextState = (String) nextStateObj;

    int x1 = Integer.parseInt(state.split(";")[0].split(",")[0]);
    int y1 = Integer.parseInt(state.split(";")[0].split(",")[1]);

    int x2 = Integer.parseInt(nextState.split(";")[0].split(",")[0]);
    int y2 = Integer.parseInt(nextState.split(";")[0].split(",")[1]);

    Coord src = new Coord(x1, y1);
    Coord dst = new Coord(x2, y2);

    if (action.equals("tunnel")) {
        // Manhattan distance for tunnels
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    } else {
        // Normal move: look up traffic
        Edge e = new Edge(src, dst);
        Integer trafficLevel = traffic.get(e);
        if (trafficLevel == null) {
            return 0;
        }
        return trafficLevel;
    }
}


    public String GenGrid() {
        Random rand = new Random();
        this.m = rand.nextInt(10) + 5;
        this.n = rand.nextInt(10) + 5;
        this.p = rand.nextInt(10) + 1;
        this.s = rand.nextInt(3) + 1;

        customers.clear();
        tunnels.clear();
        stores.clear();

        while (customers.size() < p) {
            Coord c = new Coord(rand.nextInt(n), rand.nextInt(m));
            customers.add(c);
        }

        // ADD THIS - Generate stores
        while (stores.size() < s) {
            Coord store = new Coord(rand.nextInt(n), rand.nextInt(m));
            if (!customers.contains(store) && !stores.contains(store)) {
                stores.add(store);
            }
        }

        int numTunnels = Math.max(1, p / 2);
        while (tunnels.size() < numTunnels) {
            Coord a = new Coord(rand.nextInt(n), rand.nextInt(m));
            Coord b = new Coord(rand.nextInt(n), rand.nextInt(m));
            tunnels.add(new Tunnel(a, b));
        }
        traffic.clear();
        double blockedChance = 0.1;
        //horizontal
        for (int x = 0; x < n - 1; x++) {
            for (int y = 0; y < m; y++) {
                Coord src = new Coord(x, y);
                Coord dst = new Coord(x + 1, y);
                traffic.put(new Edge(src, dst), (rand.nextDouble() < blockedChance) ? 0 : rand.nextInt(4) + 1); 
                traffic.put(new Edge(dst, src), (rand.nextDouble() < blockedChance) ? 0 : rand.nextInt(4) + 1); 
            }
        }
        //vertical
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m - 1; y++) {
                Coord src = new Coord(x, y);
                Coord dst = new Coord(x, y + 1);
                traffic.put(new Edge(src, dst), (rand.nextDouble() < blockedChance) ? 0 : rand.nextInt(4) + 1);
                traffic.put(new Edge(dst, src), (rand.nextDouble() < blockedChance) ? 0 : rand.nextInt(4) + 1);
            }
        }
        return this.getInitialState();
}

    @Override
    /*
        h1: Simple Manhattan distance to the goal customer
    */
    public double heuristic1(Object stateObj, Object destinationObj) {
        String state = (String) stateObj;
        Coord destination = (Coord) destinationObj;
        String[] parts = state.split(";");
        String[] posParts = parts[0].split(",");
        int x = Integer.parseInt(posParts[0]);
        int y = Integer.parseInt(posParts[1]);
        Coord currentPos = new Coord(x, y);

        // Manhattan distance
        return Math.abs(currentPos.x - destination.x) + 
               Math.abs(currentPos.y - destination.y);
    }   

    @Override
    /*
        h2: Manhattan distance accounting for tunnel shortcuts
    */
    public double heuristic2(Object stateObj, Object destionationObj) {
        String state = (String) stateObj;
        Coord destination = (Coord) destionationObj;
        String[] parts = state.split(";");
        String[] posParts = parts[0].split(",");
        int x = Integer.parseInt(posParts[0]);
        int y = Integer.parseInt(posParts[1]);
        Coord currentPos = new Coord(x, y);

        // Direct Manhattan distance
        double directDist = Math.abs(currentPos.x - destination.x) + 
                           Math.abs(currentPos.y - destination.y);

        // Check if any tunnel could provide a better lower bound
        double minCost = directDist;

        for (Tunnel tunnel : tunnels) {
            // Cost via tunnel: distance to entrance + tunnel cost + distance from exit to goal
            double viaTunnelA = Math.abs(currentPos.x - tunnel.a.x) + 
                               Math.abs(currentPos.y - tunnel.a.y) +
                               Math.abs(tunnel.a.x - tunnel.b.x) + 
                               Math.abs(tunnel.a.y - tunnel.b.y) +
                               Math.abs(tunnel.b.x - destination.x) + 
                               Math.abs(tunnel.b.y - destination.y);

            double viaTunnelB = Math.abs(currentPos.x - tunnel.b.x) + 
                               Math.abs(currentPos.y - tunnel.b.y) +
                               Math.abs(tunnel.b.x - tunnel.a.x) + 
                               Math.abs(tunnel.b.y - tunnel.a.y) +
                               Math.abs(tunnel.a.x - destination.x) + 
                               Math.abs(tunnel.a.y - destination.y);

            minCost = Math.min(minCost, Math.min(viaTunnelA, viaTunnelB));
        }

        return minCost;
}
 
public String path(Coord start, Coord destination, String strategy) {
    this.goal = destination;
    // Save current state
    Set<Coord> originalCustomers = new HashSet<>(customers);
    //List<Coord> originalStores = new ArrayList<>(stores);
    // Set up single-point path problem
    customers.clear();
    customers.add(destination);
    //stores.clear();
    //stores.add(start);
    
    // Reset expanded nodes counter
    Node.expandedCount = 0;
    
    // Create initial state with start position (NOT using getInitialState())
    String customInitialState = start.x + "," + start.y + ";;";
    
    // Manually run search with custom initial state
    Queue<Node> frontier = makeQueue(makeNode(customInitialState));
    Set<Object> explored = new HashSet<>();
    Node result = null;
    
    while (!frontier.isEmpty()) {
        Node node = removeFront(frontier);
        if (isGoal(node.state)) {
            result = node;
            break;
        }
        if (explored.contains(node.state)) continue; 
        explored.add(node.state);
        Node.expandedCount++;
        List<Node> children = expand(node);
        List<Node> filteredChildren = new ArrayList<>();
        for (Node child : children) {
            if (!explored.contains(child.state)) {
                filteredChildren.add(child);
            }
        }
        frontier = qingFun(strategy, frontier, filteredChildren);
    }

    // Restore original state
    customers = originalCustomers;
    //stores = originalStores;
    // Handle failure case
    if (result == null) {
        return "NONE;0;0";
    }
    // Build the action path
    List<String> actions = new ArrayList<>();
    Node current = result;
    while (current.parent != null) {
        actions.add(0, current.action);
        current = current.parent;
    }

    String pathStr = actions.isEmpty() ? "NONE" : String.join(",", actions);
    return pathStr + ";" + result.pathCost + ";" + Node.expandedCount;
}

public String plan(String initialState, String trafficStr, String strategy, boolean visualize) {
    // Parse initial state
    parseInitialState(initialState);
    
    // Parse traffic
    parseTraffic(trafficStr);

    List<Coord> originalStores = new ArrayList<>(stores);
    

    StringBuilder result = new StringBuilder();
    Set<Coord> remainingCustomers = new HashSet<>(customers);
    int totalNodes = 0;
    
    // For each customer, find the best truck to deliver
    while (!remainingCustomers.isEmpty()) {
        Coord product = remainingCustomers.iterator().next();
        double bestCost = Double.MAX_VALUE;
        String bestPlan = "NONE";
        Coord bestTruck = null;
        int bestNodes = 0;
        
        // Try each truck/store
        for (Coord truck : stores) {
            String pathResult = path(truck, product, strategy);
            String[] pathParts = pathResult.split(";");
            
            if (pathParts[0].equals("NONE")) continue;
            
            double cost = Double.parseDouble(pathParts[1]);
            int nodesExpanded = Integer.parseInt(pathParts[2]);
            
            if (cost < bestCost) {
                bestCost = cost;
                bestPlan = pathParts[0];
                bestTruck = truck;
                bestNodes = nodesExpanded;
            }
        }
        
        if (bestTruck == null) {
            return "No solution exists";
        }
        
        // Add to result
        if (result.length() > 0) result.append(";");
        result.append(bestTruck.x).append(",").append(bestTruck.y)
              .append(",").append(product.x).append(",").append(product.y)
              .append(",").append(bestPlan)
              .append(",").append((int)bestCost)
              .append(",").append(bestNodes);
        
        totalNodes += bestNodes;
        
        // Visualize if requested
        if (visualize) {
            visualizePath(bestTruck, product, bestPlan);
        }
        
        // Remove delivered customer
        remainingCustomers.remove(product);
    }
    
    return result.toString();
}

public void parseInitialState(String initialState) {
    String[] parts = initialState.split(";");
    
    this.m = Integer.parseInt(parts[0]);
    this.n = Integer.parseInt(parts[1]);
    this.p = Integer.parseInt(parts[2]);
    this.s = Integer.parseInt(parts[3]);
    
    // Parse customers
    customers.clear();
    if (!parts[4].isEmpty()) {
        String[] customerCoords = parts[4].split(",");
        for (int i = 0; i < customerCoords.length; i += 2) {
            customers.add(new Coord(
                Integer.parseInt(customerCoords[i]),
                Integer.parseInt(customerCoords[i + 1])
            ));
        }
    }
    
    // Parse tunnels
    tunnels.clear();
    if (parts.length > 5 && !parts[5].isEmpty()) {
        String[] tunnelCoords = parts[5].split(",");
        for (int i = 0; i < tunnelCoords.length; i += 4) {
            Coord a = new Coord(
                Integer.parseInt(tunnelCoords[i]),
                Integer.parseInt(tunnelCoords[i + 1])
            );
            Coord b = new Coord(
                Integer.parseInt(tunnelCoords[i + 2]),
                Integer.parseInt(tunnelCoords[i + 3])
            );
            tunnels.add(new Tunnel(a, b));
        }
    }
    
    // Parse stores (if provided, otherwise generate them)
    stores.clear();
    if (parts.length > 6 && !parts[6].isEmpty()) {
        String[] storeCoords = parts[6].split(",");
        for (int i = 0; i < storeCoords.length; i += 2) {
            stores.add(new Coord(
                Integer.parseInt(storeCoords[i]),
                Integer.parseInt(storeCoords[i + 1])
            ));
        }
    } else {
        // Generate random store locations
        Random rand = new Random();
        while (stores.size() < s) {
            Coord store = new Coord(rand.nextInt(n), rand.nextInt(m));
            if (!customers.contains(store) && !stores.contains(store)) {
                stores.add(store);
            }
        }
    }
}

public void parseTraffic(String trafficStr) {
    traffic.clear();
    if (trafficStr == null || trafficStr.isEmpty()) return;
    
    String[] segments = trafficStr.split(";");
    for (String segment : segments) {
        if (segment.isEmpty()) continue;
        
        String[] parts = segment.split(",");
        Coord src = new Coord(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1])
        );
        Coord dst = new Coord(
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3])
        );
        int trafficLevel = Integer.parseInt(parts[4]);
        
        if (trafficLevel == 0) {
            traffic.put(new Edge(src, dst), 0); // Blocked
        } else {
            traffic.put(new Edge(src, dst), trafficLevel);
        }
    }
}

public void visualizePath(Coord start, Coord end, String pathStr) {
    System.out.println("\n=== Delivery from (" + start.x + "," + start.y + 
                       ") to (" + end.x + "," + end.y + ") ===");
    
    String[] actions = pathStr.split(",");
    Coord current = new Coord(start.x, start.y);
    
    printGrid(current);
    
    for (String action : actions) {
        switch (action) {
            case "up": current = new Coord(current.x, current.y - 1); break;
            case "down": current = new Coord(current.x, current.y + 1); break;
            case "left": current = new Coord(current.x - 1, current.y); break;
            case "right": current = new Coord(current.x + 1, current.y); break;
            case "tunnel":
                // Find tunnel exit
                for (Tunnel t : tunnels) {
                    if (t.a.equals(current)) {
                        current = new Coord(t.b.x, t.b.y);
                        break;
                    } else if (t.b.equals(current)) {
                        current = new Coord(t.a.x, t.a.y);
                        break;
                    }
                }
                break;
        }
        System.out.println("Action: " + action);
        printGrid(current);
    }
}

private void printGrid(Coord truckPos) {
    for (int y = 0; y < m; y++) {
        for (int x = 0; x < n; x++) {
            Coord c = new Coord(x, y);
            if (c.equals(truckPos)) {
                System.out.print("T ");
            } else if (customers.contains(c)) {
                System.out.print("C ");
            } else if (stores.contains(c)) {
                System.out.print("S ");
            } else {
                boolean isTunnel = false;
                for (Tunnel t : tunnels) {
                    if (t.a.equals(c) || t.b.equals(c)) {
                        System.out.print("# ");
                        isTunnel = true;
                        break;
                    }
                }
                if (!isTunnel) System.out.print(". ");
            }
        }
        System.out.println();
    }
    System.out.println();
}

public String solve(String initialState, String trafficStr, String strategy, boolean visualize) {
    return plan(initialState, trafficStr, strategy, visualize);
}
}