package code;

import java.util.*;

public class DeliveryPlanner {

    private DeliverySearch search;

    public DeliveryPlanner(DeliverySearch search) {
        this.search = search;
    }

    public String plan(String initialState, String trafficStr, String strategy, boolean visualize) {
        String[] parts = initialState.split(";");
        int m = Integer.parseInt(parts[0]);
        int n = Integer.parseInt(parts[1]);
        int P = Integer.parseInt(parts[2]); 
        int S = Integer.parseInt(parts[3]); 

        List<DeliverySearch.Coord> products = new ArrayList<>();
        if(parts.length > 4 && !parts[4].isEmpty()) {
            String[] coords = parts[4].split(",");
            for(int i=0; i<coords.length; i+=2){
                products.add(new DeliverySearch.Coord(
                    Integer.parseInt(coords[i]), Integer.parseInt(coords[i+1])));
            }
        }

        search.tunnels.clear();
        if(parts.length > 5 && !parts[5].isEmpty()) {
            String[] tunnelCoords = parts[5].split(",");
            for(int i=0; i<tunnelCoords.length; i+=4){
                DeliverySearch.Coord a = new DeliverySearch.Coord(
                        Integer.parseInt(tunnelCoords[i]), Integer.parseInt(tunnelCoords[i+1]));
                DeliverySearch.Coord b = new DeliverySearch.Coord(
                        Integer.parseInt(tunnelCoords[i+2]), Integer.parseInt(tunnelCoords[i+3]));
                search.tunnels.add(new DeliverySearch.Tunnel(a,b));
            }
        }

        search.stores.clear();
        for(int i=0; i<S; i++){
            search.stores.add(new DeliverySearch.Coord(0,i)); // example: trucks at top row
        }

        search.customers.clear();
        search.customers.addAll(products);

        StringBuilder result = new StringBuilder();

        result.append("=== CITY GRID ===\n");
        result.append("Dimensions: ").append(n).append(" x ").append(m).append("\n");
        result.append("Customers: ").append(P).append(", Trucks: ").append(S).append("\n");

        result.append("Customer coordinates: ");
        for(DeliverySearch.Coord c : products){
            result.append("(").append(c.x).append(",").append(c.y).append(") ");
        }
        result.append("\n");

        result.append("Tunnels: ");
        for(DeliverySearch.Tunnel t : search.tunnels){
            result.append("(").append(t.a.x).append(",").append(t.a.y).append(") ↔ (")
                  .append(t.b.x).append(",").append(t.b.y).append(") ");
        }
        result.append("\n\n");

        result.append("=== DELIVERY PLAN ===\n");
        for(DeliverySearch.Coord product : products){
            double bestCost = Double.MAX_VALUE;
            String bestPlan = "";
            DeliverySearch.Coord bestTruck = null;
            int bestNodes = 0;

            for(DeliverySearch.Coord truck : search.stores){
                String pathResult = search.path(truck, product, strategy);
                String[] pathParts = pathResult.split(";");
                double cost = Double.parseDouble(pathParts[1]);
                int nodesExpanded = Integer.parseInt(pathParts[2]);

                if(cost < bestCost){
                    bestCost = cost;
                    bestPlan = pathParts[0];
                    bestTruck = truck;
                    bestNodes = nodesExpanded;
                }
            }

            result.append("Truck ").append(search.stores.indexOf(bestTruck))
                  .append(" -> Customer ").append(products.indexOf(product)).append("\n");
            result.append("  Plan : ").append(bestPlan).append("\n");
            result.append("  Total Cost : ").append(bestCost).append("\n");
            result.append("  Nodes Expanded : ").append(bestNodes).append("\n\n");
        }

        return result.toString();
    }
}
