package test;
import code.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;

public class BenchmarkAll {
    
    private static final String[] STRATEGIES = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
    private static final int NUM_RUNS = 10;
    
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
    
    private static class RunStats {
        double totalCost = 0;
        int totalNodes = 0;
        long executionTime = 0;
        long cpuTime = 0;
        long memoryUsed = 0;
    }
    
    private static class StrategyAggregateStats {
        String strategyName;
        List<Double> costs = new ArrayList<>();
        List<Integer> nodes = new ArrayList<>();
        List<Long> executionTimes = new ArrayList<>();
        List<Long> cpuTimes = new ArrayList<>();
        List<Long> memoryUsages = new ArrayList<>();
        int successfulRuns = 0;
        int failedRuns = 0;
        
        void addRun(RunStats run) {
            costs.add(run.totalCost);
            nodes.add(run.totalNodes);
            executionTimes.add(run.executionTime);
            cpuTimes.add(run.cpuTime);
            memoryUsages.add(run.memoryUsed);
            successfulRuns++;
        }
        
        void addFailure() {
            failedRuns++;
        }
        
        // Statistical measures
        double getAvgCost() { return average(costs); }
        double getMinCost() { return min(costs); }
        double getMaxCost() { return max(costs); }
        double getStdDevCost() { return stdDev(costs); }
        
        double getAvgNodes() { return average(nodes); }
        double getAvgExecutionTime() { return average(executionTimes); }
        double getAvgCpuTime() { return average(cpuTimes) / 1_000_000.0; } // Convert to ms
        double getAvgMemory() { return average(memoryUsages) / (1024.0 * 1024.0); } // Convert to MB
        
        private double average(List<? extends Number> list) {
            if (list.isEmpty()) return 0;
            double sum = 0;
            for (Number n : list) sum += n.doubleValue();
            return sum / list.size();
        }
        
        private double min(List<? extends Number> list) {
            if (list.isEmpty()) return 0;
            double min = Double.MAX_VALUE;
            for (Number n : list) {
                min = Math.min(min, n.doubleValue());
            }
            return min;
        }
        
        private double max(List<? extends Number> list) {
            if (list.isEmpty()) return 0;
            double max = Double.MIN_VALUE;
            for (Number n : list) {
                max = Math.max(max, n.doubleValue());
            }
            return max;
        }
        
        private double stdDev(List<? extends Number> list) {
            if (list.size() < 2) return 0;
            double avg = average(list);
            double sumSquaredDiff = 0;
            for (Number n : list) {
                double diff = n.doubleValue() - avg;
                sumSquaredDiff += diff * diff;
            }
            return Math.sqrt(sumSquaredDiff / list.size());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║    COMPREHENSIVE DELIVERY STRATEGY BENCHMARK               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("Running " + NUM_RUNS + " iterations for each strategy...\n");
        
        // Store aggregate statistics for each strategy
        Map<String, StrategyAggregateStats> allStats = new LinkedHashMap<>();
        for (String strategy : STRATEGIES) {
            StrategyAggregateStats stats = new StrategyAggregateStats();
            stats.strategyName = strategy;
            allStats.put(strategy, stats);
        }
        
        // Run benchmarks
        for (int run = 1; run <= NUM_RUNS; run++) {
            System.out.println("═".repeat(60));
            System.out.println("                    RUN #" + run + " / " + NUM_RUNS);
            System.out.println("═".repeat(60));
            
            // Generate a NEW random grid for each run
            DeliverySearch search = new DeliverySearch();
            DeliveryPlanner planner = new DeliveryPlanner(search);
            String randomGrid = search.GenGrid();
            String traffic = generateCompleteTraffic(search);
            
            System.out.println("Grid: " + search.n + "x" + search.m + 
                             ", Customers: " + search.p + 
                             ", Trucks: " + search.s + 
                             ", Tunnels: " + search.tunnels.size());
            
            // Test each strategy on this grid
            for (String strategy : STRATEGIES) {
                try {
                    System.out.print("  Testing " + strategy + "... ");
                    RunStats runStats = testStrategy(planner, randomGrid, traffic, strategy);
                    allStats.get(strategy).addRun(runStats);
                    System.out.println("✓ Cost: " + String.format("%.1f", runStats.totalCost) + 
                                     ", Nodes: " + runStats.totalNodes +
                                     ", Time: " + runStats.executionTime + "ms");
                } catch (Exception e) {
                    allStats.get(strategy).addFailure();
                    System.out.println("✗ FAILED: " + e.getMessage());
                }
            }
            System.out.println();
        }
        
        // Print comprehensive results
        printComprehensiveResults(allStats);
    }
    
    private static RunStats testStrategy(DeliveryPlanner planner, String grid, 
                                        String traffic, String strategy) throws InterruptedException {
        System.gc();
        Thread.sleep(50); // Shorter delay for benchmarking
        
        RunStats stats = new RunStats();
        
        ResourceSnapshot start = ResourceSnapshot.take();
        long startTime = System.currentTimeMillis();
        
        String result = planner.plan(grid, traffic, strategy, false);
        
        long endTime = System.currentTimeMillis();
        ResourceSnapshot end = ResourceSnapshot.take();
        
        // Parse results
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
        
        stats.executionTime = endTime - startTime;
        stats.cpuTime = start.getCpuDiff(end);
        stats.memoryUsed = start.getMemoryDiff(end);
        
        return stats;
    }
    
    private static void printComprehensiveResults(Map<String, StrategyAggregateStats> allStats) {
        System.out.println("\n" + "═".repeat(100));
        System.out.println("                              COMPREHENSIVE BENCHMARK RESULTS");
        System.out.println("═".repeat(100));
        
        // Main comparison table
        System.out.println("\n=== AVERAGE PERFORMANCE METRICS (across " + NUM_RUNS + " runs) ===\n");
        System.out.println("┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ Strategy │ Avg Cost │ Min Cost │ Max Cost │ Std Dev  │ Avg Nodes│ Avg Time │");
        System.out.println("├──────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤");
        
        for (StrategyAggregateStats stats : allStats.values()) {
            if (stats.successfulRuns > 0) {
                System.out.printf("│   %-6s │ %8.1f │ %8.1f │ %8.1f │ %8.2f │ %8.0f │ %6.0f ms │%n",
                    stats.strategyName,
                    stats.getAvgCost(),
                    stats.getMinCost(),
                    stats.getMaxCost(),
                    stats.getStdDevCost(),
                    stats.getAvgNodes(),
                    stats.getAvgExecutionTime());
            } else {
                System.out.printf("│   %-6s │   FAILED - No successful runs                                │%n",
                    stats.strategyName);
            }
        }
        System.out.println("└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘");
        
        // Resource usage table
        System.out.println("\n=== RESOURCE USAGE ===\n");
        System.out.println("┌──────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ Strategy │   CPU Time   │    Memory    │  Success Rate│");
        System.out.println("├──────────┼──────────────┼──────────────┼──────────────┤");
        
        for (StrategyAggregateStats stats : allStats.values()) {
            if (stats.successfulRuns > 0) {
                double successRate = (stats.successfulRuns * 100.0) / (stats.successfulRuns + stats.failedRuns);
                System.out.printf("│   %-6s │   %8.1f ms │   %8.2f MB │    %6.1f%%   │%n",
                    stats.strategyName,
                    stats.getAvgCpuTime(),
                    stats.getAvgMemory(),
                    successRate);
            }
        }
        System.out.println("└──────────┴──────────────┴──────────────┴──────────────┘");
        
        // Rankings
        printRankings(allStats);
        
        // Overall winner
        determineWinner(allStats);
        
        // Detailed statistics
        printDetailedStatistics(allStats);
    }
    
    private static void printRankings(Map<String, StrategyAggregateStats> allStats) {
        System.out.println("\n=== RANKINGS ===\n");
        
        // Rank by cost
        List<StrategyAggregateStats> rankedByCost = new ArrayList<>(allStats.values());
        rankedByCost.removeIf(s -> s.successfulRuns == 0);
        rankedByCost.sort(Comparator.comparingDouble(StrategyAggregateStats::getAvgCost));
        
        System.out.println("🏆 By Solution Quality (Lower Cost = Better):");
        for (int i = 0; i < rankedByCost.size(); i++) {
            StrategyAggregateStats stats = rankedByCost.get(i);
            System.out.printf("   %d. %-6s - Avg Cost: %.1f (±%.2f)%n", 
                i + 1, stats.strategyName, stats.getAvgCost(), stats.getStdDevCost());
        }
        
        // Rank by speed
        List<StrategyAggregateStats> rankedBySpeed = new ArrayList<>(allStats.values());
        rankedBySpeed.removeIf(s -> s.successfulRuns == 0);
        rankedBySpeed.sort(Comparator.comparingDouble(StrategyAggregateStats::getAvgExecutionTime));
        
        System.out.println("\n⚡ By Speed (Faster = Better):");
        for (int i = 0; i < rankedBySpeed.size(); i++) {
            StrategyAggregateStats stats = rankedBySpeed.get(i);
            System.out.printf("   %d. %-6s - Avg Time: %.0f ms%n", 
                i + 1, stats.strategyName, stats.getAvgExecutionTime());
        }
        
        // Rank by efficiency (nodes expanded)
        List<StrategyAggregateStats> rankedByNodes = new ArrayList<>(allStats.values());
        rankedByNodes.removeIf(s -> s.successfulRuns == 0);
        rankedByNodes.sort(Comparator.comparingDouble(StrategyAggregateStats::getAvgNodes));
        
        System.out.println("\n🎯 By Search Efficiency (Fewer Nodes = Better):");
        for (int i = 0; i < rankedByNodes.size(); i++) {
            StrategyAggregateStats stats = rankedByNodes.get(i);
            System.out.printf("   %d. %-6s - Avg Nodes: %.0f%n", 
                i + 1, stats.strategyName, stats.getAvgNodes());
        }
    }
    
    private static void determineWinner(Map<String, StrategyAggregateStats> allStats) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("                    🏆 OVERALL WINNER 🏆");
        System.out.println("═".repeat(60));
        
        // Find best strategy by cost
        StrategyAggregateStats bestByCost = null;
        double bestCost = Double.MAX_VALUE;
        
        for (StrategyAggregateStats stats : allStats.values()) {
            if (stats.successfulRuns > 0 && stats.getAvgCost() < bestCost) {
                bestCost = stats.getAvgCost();
                bestByCost = stats;
            }
        }
        
        if (bestByCost != null) {
            System.out.println("\n🥇 BEST OVERALL STRATEGY: " + bestByCost.strategyName);
            System.out.println("\n   Why " + bestByCost.strategyName + " wins:");
            System.out.printf("   ✓ Best average cost: %.1f%n", bestByCost.getAvgCost());
            System.out.printf("   ✓ Cost consistency (std dev): %.2f%n", bestByCost.getStdDevCost());
            System.out.printf("   ✓ Best cost achieved: %.1f%n", bestByCost.getMinCost());
            System.out.printf("   ✓ Average execution time: %.0f ms%n", bestByCost.getAvgExecutionTime());
            System.out.printf("   ✓ Success rate: %.0f%%%n", 
                (bestByCost.successfulRuns * 100.0) / (bestByCost.successfulRuns + bestByCost.failedRuns));
            
            // Compare with second best
            List<StrategyAggregateStats> sorted = new ArrayList<>(allStats.values());
            sorted.removeIf(s -> s.successfulRuns == 0);
            sorted.sort(Comparator.comparingDouble(StrategyAggregateStats::getAvgCost));
            
            if (sorted.size() > 1) {
                StrategyAggregateStats secondBest = sorted.get(1);
                double improvement = ((secondBest.getAvgCost() - bestByCost.getAvgCost()) / secondBest.getAvgCost()) * 100;
                System.out.printf("\n   📊 Performance gap: %.1f%% better than %s (2nd place)%n", 
                    improvement, secondBest.strategyName);
            }
        }
    }
    
    private static void printDetailedStatistics(Map<String, StrategyAggregateStats> allStats) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("              DETAILED COST DISTRIBUTION");
        System.out.println("═".repeat(60));
        
        for (StrategyAggregateStats stats : allStats.values()) {
            if (stats.successfulRuns > 0) {
                System.out.println("\n" + stats.strategyName + ":");
                System.out.printf("  Costs: ");
                
                // Sort costs for display
                List<Double> sortedCosts = new ArrayList<>(stats.costs);
                Collections.sort(sortedCosts);
                
                for (int i = 0; i < Math.min(5, sortedCosts.size()); i++) {
                    System.out.printf("%.1f ", sortedCosts.get(i));
                }
                if (sortedCosts.size() > 5) {
                    System.out.print("...");
                }
                System.out.println();
            }
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
}