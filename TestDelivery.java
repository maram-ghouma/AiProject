import code.*;
public class TestDelivery {
    public static void main(String[] args) {
        DeliverySearch search = new DeliverySearch();
        DeliveryPlanner planner = new DeliveryPlanner(search);
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         DELIVERY PLANNING SYSTEM TEST                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Generate random grid
        System.out.println("Generating random city grid...\n");
        String randomGrid = search.GenGrid();
        
        System.out.println("=== GENERATED GRID ===");
        System.out.println("Grid Size: " + search.n + " x " + search.m);
        System.out.println("Number of Customers: " + search.p);
        System.out.println("Number of Trucks: " + search.s);
        System.out.println("Number of Tunnels: " + search.tunnels.size());
        
        System.out.println("\nCustomer Locations:");
        int custNum = 0;
        for (DeliverySearch.Coord c : search.customers) {
            System.out.println("  Customer " + custNum + ": (" + c.x + "," + c.y + ")");
            custNum++;
        }
        
        System.out.println("\nTruck/Store Locations:");
        int truckNum = 0;
        for (DeliverySearch.Coord s : search.stores) {
            System.out.println("  Truck " + truckNum + ": (" + s.x + "," + s.y + ")");
            truckNum++;
        }
        
        System.out.println("\nTunnels:");
        int tunnelNum = 0;
        for (DeliverySearch.Tunnel t : search.tunnels) {
            System.out.println("  Tunnel " + tunnelNum + ": (" + t.a.x + "," + t.a.y + 
                             ") <-> (" + t.b.x + "," + t.b.y + ")");
            tunnelNum++;
        }
        System.out.println("\nTunnels:");

// ADD THIS SECTION:
System.out.println("\n=== VISUAL GRID ===");
printVisualGrid(search);

        // Generate complete traffic data
        String traffic = generateCompleteTraffic(search);
        System.out.println("\nTraffic edges generated: " + search.traffic.size());
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("          TESTING BF (BREADTH-FIRST) STRATEGY");
        System.out.println("=".repeat(60));
        
        long startTimeBF = System.currentTimeMillis();
        String resultBF = planner.plan(randomGrid, traffic, "BF", false);
        long endTimeBF = System.currentTimeMillis();
        
        System.out.println(resultBF);
        System.out.println("BF Execution Time: " + (endTimeBF - startTimeBF) + " ms");
        
        // Parse BF results
        BFDFComparison bfStats = parseResults(resultBF);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("          TESTING DF (DEPTH-FIRST) STRATEGY");
        System.out.println("=".repeat(60));
        
        long startTimeDF = System.currentTimeMillis();
        String resultDF = planner.plan(randomGrid, traffic, "DF", false);
        long endTimeDF = System.currentTimeMillis();
        
        System.out.println(resultDF);
        System.out.println("DF Execution Time: " + (endTimeDF - startTimeDF) + " ms");
        
        // Parse DF results
        BFDFComparison dfStats = parseResults(resultDF);
        
        // Print comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    COMPARISON SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("\n┌─────────────────────────┬──────────────┬──────────────┐");
        System.out.println("│ Metric                  │      BF      │      DF      │");
        System.out.println("├─────────────────────────┼──────────────┼──────────────┤");
        System.out.printf("│ Total Delivery Cost     │   %8.1f   │   %8.1f   │%n", 
                          bfStats.totalCost, dfStats.totalCost);
        System.out.printf("│ Total Nodes Expanded    │   %8d   │   %8d   │%n", 
                          bfStats.totalNodes, dfStats.totalNodes);
        System.out.printf("│ Execution Time (ms)     │   %8d   │   %8d   │%n", 
                          (endTimeBF - startTimeBF), (endTimeDF - startTimeDF));
        System.out.println("└─────────────────────────┴──────────────┴──────────────┘\n");
        
        // Determine winner
        System.out.println("🏆 WINNER: ");
        if (bfStats.totalCost < dfStats.totalCost) {
            double improvement = ((dfStats.totalCost - bfStats.totalCost) / dfStats.totalCost) * 100;
            System.out.println("   BF (Breadth-First) Strategy!");
            System.out.printf("   BF found %.1f%% better solution than DF%n", improvement);
        } else if (dfStats.totalCost < bfStats.totalCost) {
            double improvement = ((bfStats.totalCost - dfStats.totalCost) / bfStats.totalCost) * 100;
            System.out.println("   DF (Depth-First) Strategy!");
            System.out.printf("   DF found %.1f%% better solution than BF%n", improvement);
        } else {
            System.out.println("   TIE - Both strategies found same cost!");
        }
        
        System.out.println("\n📊 Analysis:");
        if (bfStats.totalNodes < dfStats.totalNodes) {
            System.out.println("   - BF explored fewer nodes (more efficient search)");
        } else {
            System.out.println("   - DF explored fewer nodes (more efficient search)");
        }
        
        System.out.println("\n✅ Test Complete!");
    }
    
    private static String generateCompleteTraffic(DeliverySearch search) {
        StringBuilder traffic = new StringBuilder();
        
        // Horizontal edges (both directions)
        for (int y = 0; y < search.m; y++) {
            for (int x = 0; x < search.n - 1; x++) {
                DeliverySearch.Coord src = new DeliverySearch.Coord(x, y);
                DeliverySearch.Coord dst = new DeliverySearch.Coord(x + 1, y);
                
                // Get cost from search.traffic (already generated by GenGrid)
                int cost1 = search.traffic.getOrDefault(new DeliverySearch.Edge(src, dst), 1);
                int cost2 = search.traffic.getOrDefault(new DeliverySearch.Edge(dst, src), 1);
                
                traffic.append(x).append(",").append(y).append(",")
                       .append(x + 1).append(",").append(y).append(",")
                       .append(cost1).append(";");
                
                traffic.append(x + 1).append(",").append(y).append(",")
                       .append(x).append(",").append(y).append(",")
                       .append(cost2).append(";");
            }
        }
        
        // Vertical edges (both directions)
        for (int x = 0; x < search.n; x++) {
            for (int y = 0; y < search.m - 1; y++) {
                DeliverySearch.Coord src = new DeliverySearch.Coord(x, y);
                DeliverySearch.Coord dst = new DeliverySearch.Coord(x, y + 1);
                
                int cost1 = search.traffic.getOrDefault(new DeliverySearch.Edge(src, dst), 1);
                int cost2 = search.traffic.getOrDefault(new DeliverySearch.Edge(dst, src), 1);
                
                traffic.append(x).append(",").append(y).append(",")
                       .append(x).append(",").append(y + 1).append(",")
                       .append(cost1).append(";");
                
                traffic.append(x).append(",").append(y + 1).append(",")
                       .append(x).append(",").append(y).append(",")
                       .append(cost2).append(";");
            }
        }
        
        return traffic.toString();
    }
    
    private static BFDFComparison parseResults(String result) {
        BFDFComparison stats = new BFDFComparison();
        
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.contains("Total Cost")) {
                String costStr = line.split(":")[1].trim();
                stats.totalCost += Double.parseDouble(costStr);
            } else if (line.contains("Nodes Expanded")) {
                String nodesStr = line.split(":")[1].trim();
                stats.totalNodes += Integer.parseInt(nodesStr);
            }
        }
        
        return stats;
    }
    
    static class BFDFComparison {
        double totalCost = 0;
        int totalNodes = 0;
    }

    private static void printVisualGrid(DeliverySearch search) {
    for (int y = 0; y < search.m; y++) {
        for (int x = 0; x < search.n; x++) {
            DeliverySearch.Coord pos = new DeliverySearch.Coord(x, y);
            boolean found = false;
            
            // Check if it's a truck/store
            for (int i = 0; i < search.stores.size(); i++) {
                if (search.stores.get(i).equals(pos)) {
                    System.out.print("T" + i + " ");
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                // Check if it's a customer
                int custIndex = 0;
                for (DeliverySearch.Coord c : search.customers) {
                    if (c.equals(pos)) {
                        System.out.print("C" + custIndex + " ");
                        found = true;
                        break;
                    }
                    custIndex++;
                }
            }
            
            if (!found) {
                // Check if it's a tunnel
                for (DeliverySearch.Tunnel t : search.tunnels) {
                    if (t.a.equals(pos) || t.b.equals(pos)) {
                        System.out.print("#  ");
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                System.out.print(".  ");
            }
        }
        System.out.println();
    }
    System.out.println("\nLegend: T0,T1=Trucks, C0-C4=Customers, #=Tunnel, .=Empty");
}
}