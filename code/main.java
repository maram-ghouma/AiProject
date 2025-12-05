package code;

import java.util.*;

public class main {
    
    private static final String[] ALL_STRATEGIES = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
    private static final String[] UNINFORMED = {"BF", "DF", "ID", "UC"};
    private static final String[] INFORMED = {"GR1", "GR2", "AS1", "AS2"};
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     DELIVERY PLANNING SYSTEM - COMPREHENSIVE ANALYSIS         ║");
        System.out.println("║              Comparing All Search Strategies                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Initialize the search system
        DeliverySearch search = new DeliverySearch();
        DeliveryPlanner planner = new DeliveryPlanner(search);
        
        // Generate a random test case
        System.out.println("📋 Generating random test case...\n");
        String grid = search.GenGrid();
        
        // Display grid information
        displayGridInfo(search);
        
        // Generate traffic data
        String traffic = generateCompleteTraffic(search);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    RUNNING ALL SEARCH STRATEGIES");
        System.out.println("=".repeat(70) + "\n");
        
        // Store results for all strategies
        Map<String, StrategyResult> results = new LinkedHashMap<>();
        
        // Test each strategy
        for (String strategy : ALL_STRATEGIES) {
            try {
                StrategyResult result = testStrategy(planner, grid, traffic, strategy);
                results.put(strategy, result);
                Thread.sleep(200); // Brief pause between tests
            } catch (Exception e) {
                System.err.println("❌ Error testing " + strategy + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Display comprehensive analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    COMPREHENSIVE ANALYSIS");
        System.out.println("=".repeat(70) + "\n");
        
        displayComparisonTable(results);
        displayCategoryComparison(results);
        displayOptimalityAnalysis(results);
        displayEfficiencyAnalysis(results);
        displayRecommendations(results);
        
        System.out.println("\n✅ All tests completed successfully!");
    }
    
    private static void displayGridInfo(DeliverySearch search) {
        System.out.println("=== GRID CONFIGURATION ===");
        System.out.println("Grid Size: " + search.n + " x " + search.m);
        System.out.println("Number of Customers: " + search.p);
        System.out.println("Number of Trucks: " + search.s);
        System.out.println("Number of Tunnels: " + search.tunnels.size());
        
        System.out.println("\n📍 Customer Locations:");
        int custIndex = 0;
        for (DeliverySearch.Coord c : search.customers) {
            System.out.println("   Customer " + custIndex + ": (" + c.x + ", " + c.y + ")");
            custIndex++;
        }
        
        System.out.println("\n🚚 Truck/Store Locations:");
        int storeIndex = 0;
        for (DeliverySearch.Coord s : search.stores) {
            System.out.println("   Truck " + storeIndex + ": (" + s.x + ", " + s.y + ")");
            storeIndex++;
        }
        
        if (!search.tunnels.isEmpty()) {
            System.out.println("\n🚇 Tunnels:");
            int tunnelIndex = 0;
            for (DeliverySearch.Tunnel t : search.tunnels) {
                System.out.println("   Tunnel " + tunnelIndex + ": (" + t.a.x + ", " + t.a.y + 
                                 ") ↔ (" + t.b.x + ", " + t.b.y + ")");
                tunnelIndex++;
            }
        }
        
        System.out.println("\n=== VISUAL GRID ===");
        printVisualGrid(search);
    }
    
    private static void printVisualGrid(DeliverySearch search) {
        for (int y = 0; y < search.m; y++) {
            for (int x = 0; x < search.n; x++) {
                DeliverySearch.Coord pos = new DeliverySearch.Coord(x, y);
                boolean found = false;
                
                // Check trucks
                int storeIdx = 0;
                for (DeliverySearch.Coord store : search.stores) {
                    if (store.equals(pos)) {
                        System.out.print(String.format("T%-2d", storeIdx));
                        found = true;
                        break;
                    }
                    storeIdx++;
                }
                
                if (!found) {
                    // Check customers
                    int custIdx = 0;
                    for (DeliverySearch.Coord cust : search.customers) {
                        if (cust.equals(pos)) {
                            System.out.print(String.format("C%-2d", custIdx));
                            found = true;
                            break;
                        }
                        custIdx++;
                    }
                }
                
                if (!found) {
                    // Check tunnels
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
        System.out.println("\n📖 Legend: Tn=Trucks, Cn=Customers, #=Tunnel, .=Empty");
    }
    
    private static StrategyResult testStrategy(DeliveryPlanner planner, String grid, 
                                               String traffic, String strategy) {
        System.out.println("\n" + "-".repeat(70));
        System.out.println("🔍 Testing: " + strategy + " (" + getStrategyName(strategy) + ")");
        System.out.println("-".repeat(70));
        
        // Force garbage collection
        System.gc();
        
        StrategyResult result = new StrategyResult();
        result.strategyCode = strategy;
        result.strategyName = getStrategyName(strategy);
        
        // Capture initial resources
        long startTime = System.nanoTime();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Run the strategy
        String output = planner.plan(grid, traffic, strategy, false);
        
        // Capture final resources
        long endTime = System.nanoTime();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        result.executionTimeMs = (endTime - startTime) / 1_000_000;
        result.memoryUsedMB = Math.max(0, endMemory - startMemory) / (1024.0 * 1024.0);
        result.output = output;
        
        // Parse the output
        parseOutput(output, result);
        
        // Display immediate results
        if (result.foundSolution) {
            System.out.println("\n" + output); // Show full output with plans
            System.out.println("\n" + "-".repeat(70));
            System.out.println("✓ Total Cost: " + result.totalCost);
            System.out.println("✓ Total Nodes Expanded: " + result.nodesExpanded);
            System.out.println("✓ Execution Time: " + result.executionTimeMs + " ms");
        } else {
            System.out.println("✗ No solution found");
        }
        
        return result;
    }
    
    private static void parseOutput(String output, StrategyResult result) {
        if (output == null || output.isEmpty() || output.contains("No solution")) {
            result.foundSolution = false;
            return;
        }
        
        // Parse the formatted output from DeliveryPlanner
        result.totalCost = 0;
        result.nodesExpanded = 0;
        
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // Look for "Total Cost : X.X" lines
            if (line.startsWith("Total Cost")) {
                try {
                    String costStr = line.split(":")[1].trim();
                    double cost = Double.parseDouble(costStr);
                    result.totalCost += cost;
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
            // Look for "Nodes Expanded : X" lines
            else if (line.startsWith("Nodes Expanded")) {
                try {
                    String nodesStr = line.split(":")[1].trim();
                    int nodes = Integer.parseInt(nodesStr);
                    result.nodesExpanded += nodes;
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
        }
        
        result.foundSolution = (result.totalCost > 0 || result.nodesExpanded > 0);
    }
    
    private static void displayComparisonTable(Map<String, StrategyResult> results) {
        System.out.println("📊 PERFORMANCE COMPARISON TABLE");
        System.out.println("┌──────────┬─────────────────────┬──────────┬───────────┬──────────┬─────────────┐");
        System.out.println("│ Strategy │ Name                │   Cost   │   Nodes   │ Time(ms) │  Memory(MB) │");
        System.out.println("├──────────┼─────────────────────┼──────────┼───────────┼──────────┼─────────────┤");
        
        for (String strategy : ALL_STRATEGIES) {
            StrategyResult r = results.get(strategy);
            if (r != null && r.foundSolution) {
                System.out.printf("│   %-6s │ %-19s │ %8.1f │ %9d │ %8d │ %11.2f │%n",
                    r.strategyCode,
                    r.strategyName,
                    r.totalCost,
                    r.nodesExpanded,
                    r.executionTimeMs,
                    r.memoryUsedMB);
            } else {
                System.out.printf("│   %-6s │ %-19s │ %8s │ %9s │ %8s │ %11s │%n",
                    strategy,
                    getStrategyName(strategy),
                    "NO SOL",
                    "N/A",
                    "N/A",
                    "N/A");
            }
        }
        System.out.println("└──────────┴─────────────────────┴──────────┴───────────┴──────────┴─────────────┘\n");
    }
    
    private static void displayCategoryComparison(Map<String, StrategyResult> results) {
        System.out.println("🔬 CATEGORY ANALYSIS\n");
        
        // Uninformed strategies
        System.out.println("📌 Uninformed Search Strategies:");
        displayCategoryStats(results, UNINFORMED);
        
        // Informed strategies
        System.out.println("\n📌 Informed Search Strategies:");
        displayCategoryStats(results, INFORMED);
    }
    
    private static void displayCategoryStats(Map<String, StrategyResult> results, String[] strategies) {
        double avgCost = 0, minCost = Double.MAX_VALUE, maxCost = 0;
        long avgNodes = 0, minNodes = Long.MAX_VALUE, maxNodes = 0;
        long avgTime = 0;
        int count = 0;
        
        for (String strategy : strategies) {
            StrategyResult r = results.get(strategy);
            if (r != null && r.foundSolution) {
                avgCost += r.totalCost;
                avgNodes += r.nodesExpanded;
                avgTime += r.executionTimeMs;
                minCost = Math.min(minCost, r.totalCost);
                maxCost = Math.max(maxCost, r.totalCost);
                minNodes = Math.min(minNodes, r.nodesExpanded);
                maxNodes = Math.max(maxNodes, r.nodesExpanded);
                count++;
            }
        }
        
        if (count > 0) {
            avgCost /= count;
            avgNodes /= count;
            avgTime /= count;
            
            System.out.printf("   Average Cost: %.2f (Range: %.2f - %.2f)%n", avgCost, minCost, maxCost);
            System.out.printf("   Average Nodes: %d (Range: %d - %d)%n", avgNodes, minNodes, maxNodes);
            System.out.printf("   Average Time: %d ms%n", avgTime);
        } else {
            System.out.println("   No solutions found in this category");
        }
    }
    
    private static void displayOptimalityAnalysis(Map<String, StrategyResult> results) {
        System.out.println("\n🎯 OPTIMALITY ANALYSIS\n");
        
        // Find optimal cost
        double optimalCost = Double.MAX_VALUE;
        List<String> optimalStrategies = new ArrayList<>();
        
        for (Map.Entry<String, StrategyResult> entry : results.entrySet()) {
            StrategyResult r = entry.getValue();
            if (r.foundSolution) {
                if (r.totalCost < optimalCost) {
                    optimalCost = r.totalCost;
                    optimalStrategies.clear();
                    optimalStrategies.add(entry.getKey());
                } else if (Math.abs(r.totalCost - optimalCost) < 0.01) {
                    optimalStrategies.add(entry.getKey());
                }
            }
        }
        
        if (optimalStrategies.isEmpty()) {
            System.out.println("❌ No solutions found by any strategy");
            return;
        }
        
        System.out.println("🏆 Optimal Solution Cost: " + optimalCost);
        System.out.println("🏆 Optimal Strategies: " + String.join(", ", optimalStrategies));
        
        // Show which strategies found optimal solution
        System.out.println("\n   Strategy Optimality Status:");
        for (String strategy : ALL_STRATEGIES) {
            StrategyResult r = results.get(strategy);
            if (r != null && r.foundSolution) {
                boolean isOptimal = Math.abs(r.totalCost - optimalCost) < 0.01;
                String status = isOptimal ? "✓ OPTIMAL" : "✗ Suboptimal (+" + 
                               String.format("%.1f%%", ((r.totalCost - optimalCost) / optimalCost * 100)) + ")";
                System.out.printf("   %-6s: %s%n", strategy, status);
            } else {
                System.out.printf("   %-6s: ✗ No solution%n", strategy);
            }
        }
    }
    
    private static void displayEfficiencyAnalysis(Map<String, StrategyResult> results) {
        System.out.println("\n⚡ EFFICIENCY ANALYSIS\n");
        
        // Find most efficient (fewest nodes)
        long minNodes = Long.MAX_VALUE;
        String mostEfficientStrategy = "";
        
        for (Map.Entry<String, StrategyResult> entry : results.entrySet()) {
            StrategyResult r = entry.getValue();
            if (r.foundSolution && r.nodesExpanded < minNodes) {
                minNodes = r.nodesExpanded;
                mostEfficientStrategy = entry.getKey();
            }
        }
        
        if (!mostEfficientStrategy.isEmpty()) {
            System.out.println("🚀 Most Efficient (Fewest Nodes): " + mostEfficientStrategy + 
                              " (" + minNodes + " nodes)");
        }
        
        // Find fastest
        long minTime = Long.MAX_VALUE;
        String fastestStrategy = "";
        
        for (Map.Entry<String, StrategyResult> entry : results.entrySet()) {
            StrategyResult r = entry.getValue();
            if (r.foundSolution && r.executionTimeMs < minTime) {
                minTime = r.executionTimeMs;
                fastestStrategy = entry.getKey();
            }
        }
        
        if (!fastestStrategy.isEmpty()) {
            System.out.println("⏱️  Fastest Execution: " + fastestStrategy + 
                              " (" + minTime + " ms)");
        }
    }
    
    private static void displayRecommendations(Map<String, StrategyResult> results) {
        System.out.println("\n💡 RECOMMENDATIONS\n");
        
        // Find best overall (optimal + efficient)
        String bestOverall = null;
        double optimalCost = Double.MAX_VALUE;
        long minNodesAmongOptimal = Long.MAX_VALUE;
        
        // First find optimal cost
        for (StrategyResult r : results.values()) {
            if (r.foundSolution && r.totalCost < optimalCost) {
                optimalCost = r.totalCost;
            }
        }
        
        // Then find most efficient among optimal
        for (Map.Entry<String, StrategyResult> entry : results.entrySet()) {
            StrategyResult r = entry.getValue();
            if (r.foundSolution && Math.abs(r.totalCost - optimalCost) < 0.01) {
                if (r.nodesExpanded < minNodesAmongOptimal) {
                    minNodesAmongOptimal = r.nodesExpanded;
                    bestOverall = entry.getKey();
                }
            }
        }
        
        if (bestOverall != null) {
            StrategyResult best = results.get(bestOverall);
            System.out.println("🥇 BEST OVERALL: " + bestOverall + " (" + getStrategyName(bestOverall) + ")");
            System.out.println("   Reasons:");
            System.out.println("   ✓ Finds optimal solution (cost: " + best.totalCost + ")");
            System.out.println("   ✓ Efficient search (" + best.nodesExpanded + " nodes)");
            System.out.println("   ✓ Fast execution (" + best.executionTimeMs + " ms)");
        }
        
        System.out.println("\n📝 Use Case Recommendations:");
        System.out.println("   • Need guaranteed optimal solution → Use UC, AS1, or AS2");
        System.out.println("   • Need fast solution (any quality) → Use DF or GR1");
        System.out.println("   • Limited memory → Use DF or ID");
        System.out.println("   • Balance of speed & quality → Use AS1 or AS2");
    }
    
    private static String getStrategyName(String code) {
        switch (code) {
            case "BF": return "Breadth-First";
            case "DF": return "Depth-First";
            case "ID": return "Iterative Deepening";
            case "UC": return "Uniform Cost";
            case "GR1": return "Greedy (H1)";
            case "GR2": return "Greedy (H2)";
            case "AS1": return "A* (H1)";
            case "AS2": return "A* (H2)";
            default: return "Unknown";
        }
    }
    
    private static String generateCompleteTraffic(DeliverySearch search) {
        StringBuilder traffic = new StringBuilder();
        
        // Horizontal edges
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
        
        // Vertical edges
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
    
    static class StrategyResult {
        String strategyCode;
        String strategyName;
        double totalCost = 0;
        int nodesExpanded = 0;
        long executionTimeMs = 0;
        double memoryUsedMB = 0;
        String output;
        boolean foundSolution = true;
    }
}