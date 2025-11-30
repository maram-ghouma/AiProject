package code;

public class main {
    public static void main(String[] args) {
        DeliverySearch ds = new DeliverySearch();

        String initialState = ds.GenGrid();
        System.out.println("InitialState: " + initialState);
        StringBuilder trafficStr = new StringBuilder();
        for (DeliverySearch.Edge e : ds.traffic.keySet()) {
            int cost = ds.traffic.get(e);
            trafficStr.append(e.src.x).append(",").append(e.src.y).append(",")
                      .append(e.dst.x).append(",").append(e.dst.y).append(",")
                      .append(cost).append(";");
        }
        if (trafficStr.length() > 0) trafficStr.setLength(trafficStr.length() - 1);

        DeliveryPlanner planner = new DeliveryPlanner(ds);
        String plan = planner.plan(initialState, trafficStr.toString(), "ID", true);

        System.out.println("=== PLAN DE LIVRAISON ===");
        System.out.println(plan);
    }
    
}
