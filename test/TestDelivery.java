package test;
import code.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

public class TestDelivery {

    private static final String[] VALID_STRATEGIES = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};

    private static class ResourceSnapshot {
        long cpuTime;
        long memoryUsed;
        
        static ResourceSnapshot take() {
            ResourceSnapshot snapshot = new ResourceSnapshot();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            snapshot.cpuTime = threadBean.getCurrentThreadCpuTime();
            snapshot.memoryUsed = memoryBean.getHeapMemoryUsage().getUsed();

            return snapshot;
        }

        long getCpuDiff(ResourceSnapshot end) {
            return end.cpuTime - this.cpuTime;
        }

        long getMemoryDiff(ResourceSnapshot end) {
            return end.memoryUsed - this.memoryUsed;
        }
    }

    static class StrategyStats {
        String strategyName;
        double totalCost = 0;
        int totalNodes = 0;
        long cpuTime = 0;
        long memoryUsed = 0;
        long executionTime = 0;
        String result;
    }

    public static void main(String[] args) {
        // Parse command-line arguments - now supports 1 or 2 strategies
        if (args.length < 1 || args.length > 2) {
            printUsage();
            return;
        }

        String strategy1 = args[0].toUpperCase();
        String strategy2 = args.length == 2 ? args[1].toUpperCase() : null;

        // Validate strategies
        if (!isValidStrategy(strategy1)) {
            System.err.println("❌ Error: Invalid strategy specified: " + strategy1);
            System.err.println("Valid strategies: " + String.join(", ", VALID_STRATEGIES));
            return;
        }
        
        if (strategy2 != null && !isValidStrategy(strategy2)) {
            System.err.println("❌ Error: Invalid strategy specified: " + strategy2);
            System.err.println("Valid strategies: " + String.join(", ", VALID_STRATEGIES));
            return;
        }

        try {
            if (strategy2 != null) {
                runComparison(strategy1, strategy2);
            } else {
                runSingleTest(strategy1);
            }
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java test.TestDelivery <strategy> [strategy2]");
        System.out.println("\nValid strategies: " + String.join(", ", VALID_STRATEGIES));
        System.out.println("\nExamples:");
        System.out.println("  Single strategy test:  java test.TestDelivery BF");
        System.out.println("  Comparison test:       java test.TestDelivery BF DF");
        System.out.println("                         java test.TestDelivery GR1 AS1");
    }

    private static boolean isValidStrategy(String strategy) {
        for (String valid : VALID_STRATEGIES) {
            if (valid.equals(strategy)) {
                return true;
            }
        }
        return false;
    }

    private static void runSingleTest(String strategy) throws InterruptedException {
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

        System.out.println("\n=== VISUAL GRID ===");
        printVisualGrid(search);

        // Generate complete traffic data
        String traffic = generateCompleteTraffic(search);
        System.out.println("\nTraffic edges generated: " + search.traffic.size());
        
        // Test Strategy
        StrategyStats stats = testStrategy(planner, randomGrid, traffic, strategy);
        
        // Print single strategy summary
        printSingleStrategySummary(stats);
    }

    private static void runComparison(String strategy1, String strategy2) throws InterruptedException {
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

        System.out.println("\n=== VISUAL GRID ===");
        printVisualGrid(search);

        // Generate complete traffic data
        String traffic = generateCompleteTraffic(search);
        System.out.println("\nTraffic edges generated: " + search.traffic.size());
        
        // Test Strategy 1
        StrategyStats stats1 = testStrategy(planner, randomGrid, traffic, strategy1);
        
        // Test Strategy 2
        StrategyStats stats2 = testStrategy(planner, randomGrid, traffic, strategy2);
        
        // Print comparison
        printComparison(stats1, stats2);
    }

    private static StrategyStats testStrategy(DeliveryPlanner planner, String grid, 
                                              String traffic, String strategy) throws InterruptedException {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("          TESTING " + strategy + " STRATEGY");
        System.out.println("=".repeat(60));
        
        System.gc();
        Thread.sleep(100);

        StrategyStats stats = new StrategyStats();
        stats.strategyName = strategy;

        ResourceSnapshot start = ResourceSnapshot.take();
        long startTime = System.currentTimeMillis();
        
        String result = planner.plan(grid, traffic, strategy, false);
        
        long endTime = System.currentTimeMillis();
        ResourceSnapshot end = ResourceSnapshot.take();
        
        System.out.println(result);
        System.out.println(strategy + " Execution Time: " + (endTime - startTime) + " ms");
        
        // Parse results
        stats.result = result;
        parseResults(result, stats);
        stats.cpuTime = start.getCpuDiff(end);
        stats.memoryUsed = start.getMemoryDiff(end);
        stats.executionTime = endTime - startTime;

        return stats;
    }

    private static void printSingleStrategySummary(StrategyStats stats) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    PERFORMANCE SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("\n┌─────────────────────────┬──────────────┐");
        System.out.printf("│ Metric                  │   %-8s   │%n", stats.strategyName);
        System.out.println("├─────────────────────────┼──────────────┤");
        System.out.printf("│ Total Delivery Cost     │   %8.1f   │%n", stats.totalCost);
        System.out.printf("│ Total Nodes Expanded    │   %8d   │%n", stats.totalNodes);
        System.out.printf("│ Execution Time (ms)     │   %8d   │%n", stats.executionTime);
        System.out.printf("│ CPU Time (ms)           │   %8d   │%n", stats.cpuTime / 1_000_000);
        System.out.printf("│ Memory Used (MB)        │   %8.2f   │%n", stats.memoryUsed / (1024.0 * 1024.0));
        System.out.println("└─────────────────────────┴──────────────┘\n");
        
        System.out.println("✅ Test Complete!");
        System.out.println("\n💡 Tip: Run with two strategies to compare performance:");
        System.out.println("   java test.TestDelivery " + stats.strategyName + " <other_strategy>");
    }

    private static void printComparison(StrategyStats stats1, StrategyStats stats2) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    COMPARISON SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("\n┌─────────────────────────┬──────────────┬──────────────┐");
        System.out.printf("│ Metric                  │   %-8s   │   %-8s   │%n", 
                          stats1.strategyName, stats2.strategyName);
        System.out.println("├─────────────────────────┼──────────────┼──────────────┤");
        System.out.printf("│ Total Delivery Cost     │   %8.1f   │   %8.1f   │%n", 
                          stats1.totalCost, stats2.totalCost);
        System.out.printf("│ Total Nodes Expanded    │   %8d   │   %8d   │%n", 
                          stats1.totalNodes, stats2.totalNodes);
        System.out.printf("│ Execution Time (ms)     │   %8d   │   %8d   │%n", 
                          stats1.executionTime, stats2.executionTime);
        System.out.printf("│ CPU Time (ms)           │   %8d   │   %8d   │%n", 
                          stats1.cpuTime / 1_000_000, stats2.cpuTime / 1_000_000);
        System.out.printf("│ Memory Used (MB)        │   %8.2f   │   %8.2f   │%n", 
                          stats1.memoryUsed / (1024.0 * 1024.0), stats2.memoryUsed / (1024.0 * 1024.0));
        System.out.println("└─────────────────────────┴──────────────┴──────────────┘\n");
        
        // Determine winner
        System.out.println("🏆 WINNER: ");
        if (stats1.totalCost < stats2.totalCost) {
            double improvement = ((stats2.totalCost - stats1.totalCost) / stats2.totalCost) * 100;
            System.out.println("   " + stats1.strategyName + " Strategy!");
            System.out.printf("   %s found %.1f%% better solution than %s%n", 
                            stats1.strategyName, improvement, stats2.strategyName);
        } else if (stats2.totalCost < stats1.totalCost) {
            double improvement = ((stats1.totalCost - stats2.totalCost) / stats1.totalCost) * 100;
            System.out.println("   " + stats2.strategyName + " Strategy!");
            System.out.printf("   %s found %.1f%% better solution than %s%n", 
                            stats2.strategyName, improvement, stats1.strategyName);
        } else {
            System.out.println("   TIE - Both strategies found same cost!");
        }
        
        System.out.println("\n📊 Analysis:");
        if (stats1.totalNodes < stats2.totalNodes) {
            System.out.println("   - " + stats1.strategyName + " explored fewer nodes (more efficient search)");
        } else if (stats2.totalNodes < stats1.totalNodes) {
            System.out.println("   - " + stats2.strategyName + " explored fewer nodes (more efficient search)");
        } else {
            System.out.println("   - Both strategies explored the same number of nodes");
        }
        
        if (stats1.executionTime < stats2.executionTime) {
            double speedup = ((double)stats2.executionTime / stats1.executionTime);
            System.out.printf("   - %s was %.2fx faster%n", stats1.strategyName, speedup);
        } else if (stats2.executionTime < stats1.executionTime) {
            double speedup = ((double)stats1.executionTime / stats2.executionTime);
            System.out.printf("   - %s was %.2fx faster%n", stats2.strategyName, speedup);
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
    
    private static void parseResults(String result, StrategyStats stats) {
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