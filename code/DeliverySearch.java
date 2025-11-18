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

    public static class State {
        public Coord pos;
        public Set<Coord> delivered; //les clients deja servis

        public State(Coord pos, Set<Coord> delivered) {
            this.pos = pos;
            this.delivered = new HashSet<>(delivered);
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof State)) return false;
            State other = (State)o;
            return pos.equals(other.pos) && delivered.equals(other.delivered);
        }

        @Override
        public int hashCode(){
            return Objects.hash(pos, delivered);
        }
    }

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

        return sb.toString();
    }
    @Override
    public boolean isGoal(Object stateObj){
        // TODO 
        return false;
    }

    @Override
    public List<Node> expand(Node node){
        // TODO
        return null;
    }

    @Override
    public double getStepCost(Object state, String action, Object nextState){
        // TODO
        return 0;
    }


    public String GenGrid() {
        Random rand = new Random();
        this.m = rand.nextInt(10) + 5;
        this.n = rand.nextInt(10) + 5;
        this.p = rand.nextInt(10) + 1;
        this.s = rand.nextInt(3) + 1;

        customers.clear();
        tunnels.clear();

        while (customers.size() < p) {
            Coord c = new Coord(rand.nextInt(n), rand.nextInt(m));
            customers.add(c);
        }

        int numTunnels = Math.max(1, p / 2);
        while (tunnels.size() < numTunnels) {
            Coord a = new Coord(rand.nextInt(n), rand.nextInt(m));
            Coord b = new Coord(rand.nextInt(n), rand.nextInt(m));
            tunnels.add(new Tunnel(a, b));
        }
        traffic.clear();
        //horizontal
        for (int x = 0; x < n - 1; x++) {
            for (int y = 0; y < m; y++) {
                Coord src = new Coord(x, y);
                Coord dst = new Coord(x + 1, y);
                traffic.put(new Edge(src, dst), rand.nextInt(4) + 1); 
                traffic.put(new Edge(dst, src), rand.nextInt(4) + 1); 
            }
        }
        //vertical
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m - 1; y++) {
                Coord src = new Coord(x, y);
                Coord dst = new Coord(x, y + 1);
                traffic.put(new Edge(src, dst), rand.nextInt(4) + 1);
                traffic.put(new Edge(dst, src), rand.nextInt(4) + 1);
            }
        }
        return this.getInitialState();
}


    public String path(Coord start, Coord destination, String strategy){
        // TODO
        return "NONE;0;0";
    }

    public String plan(String initialState, String trafficStr, String strategy, boolean visualize){
        // TODO
        return "";
    }
    public String solve(String initialState, String trafficStr, String strategy, boolean visualize){
        return plan(initialState, trafficStr, strategy, visualize);
    }
}
