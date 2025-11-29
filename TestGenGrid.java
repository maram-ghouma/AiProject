import code.DeliverySearch;

public class TestGenGrid {
    public static void main(String[] args) {
        DeliverySearch search = new DeliverySearch();
        
        System.out.println("=== Testing GenGrid() ===\n");
        
        // Generate random grid
        String gridState = search.GenGrid();
        
        System.out.println("Generated State String:");
        System.out.println(gridState);
        System.out.println();
        
        System.out.println("Grid Properties:");
        System.out.println("  Dimensions: " + search.n + " x " + search.m);
        System.out.println("  Customers (p): " + search.p);
        System.out.println("  Stores (s): " + search.s);
        System.out.println();
        
        System.out.println("Stores (" + search.stores.size() + "):");
        for (DeliverySearch.Coord store : search.stores) {
            System.out.println("  (" + store.x + ", " + store.y + ")");
        }
        System.out.println();
        
        System.out.println("Customers (" + search.customers.size() + "):");
        for (DeliverySearch.Coord customer : search.customers) {
            System.out.println("  (" + customer.x + ", " + customer.y + ")");
        }
        System.out.println();
        
        System.out.println("Tunnels (" + search.tunnels.size() + "):");
        for (DeliverySearch.Tunnel tunnel : search.tunnels) {
            System.out.println("  (" + tunnel.a.x + ", " + tunnel.a.y + ") <-> " +
                             "(" + tunnel.b.x + ", " + tunnel.b.y + ")");
        }
        System.out.println();
        
        System.out.println("Traffic Map (" + search.traffic.size() + " edges):");
        System.out.println("  Sample edges (first 10):");
        int count = 0;
        for (java.util.Map.Entry<DeliverySearch.Edge, Integer> entry : search.traffic.entrySet()) {
            if (count++ < 10) {
                DeliverySearch.Edge edge = entry.getKey();
                System.out.println("    (" + edge.src.x + "," + edge.src.y + ") -> " +
                                 "(" + edge.dst.x + "," + edge.dst.y + ") : cost=" + entry.getValue());
            }
        }
        System.out.println();
        
        // Visual grid representation
        System.out.println("Grid Visualization:");
        printGrid(search);
        System.out.println();
        
        // Test that we can parse it back
        System.out.println("=== Testing Parse ===");
        DeliverySearch search2 = new DeliverySearch();
        search2.parseInitialState(gridState);
        
        System.out.println("Parsed successfully!");
        System.out.println("  m=" + search2.m + ", n=" + search2.n);
        System.out.println("  p=" + search2.p + ", s=" + search2.s);
        System.out.println("  Customers parsed: " + search2.customers.size());
        System.out.println("  Tunnels parsed: " + search2.tunnels.size());
        
        // Verify no overlaps
        System.out.println("\n=== Checking for Overlaps ===");
        boolean hasOverlap = false;
        for (DeliverySearch.Coord store : search.stores) {
            if (search.customers.contains(store)) {
                System.out.println("WARNING: Store at (" + store.x + "," + store.y + 
                                 ") overlaps with customer!");
                hasOverlap = true;
            }
        }
        if (!hasOverlap) {
            System.out.println("✓ No overlaps between stores and customers");
        }
        
        // Test path finding on generated grid
        System.out.println("\n=== Testing Path on Generated Grid ===");
        if (!search.stores.isEmpty() && !search.customers.isEmpty()) {
            DeliverySearch.Coord start = search.stores.get(0);
            DeliverySearch.Coord dest = search.customers.iterator().next();
            
            System.out.println("Finding path from store (" + start.x + "," + start.y + 
                             ") to customer (" + dest.x + "," + dest.y + ")");
            
            String result = search.path(start, dest, "BF");
            String[] parts = result.split(";");
            
            System.out.println("Path: " + parts[0]);
            System.out.println("Cost: " + parts[1]);
            System.out.println("Nodes Expanded: " + parts[2]);
        }
    }
    
    private static void printGrid(DeliverySearch search) {
        for (int y = 0; y < search.m; y++) {
            for (int x = 0; x < search.n; x++) {
                DeliverySearch.Coord c = new DeliverySearch.Coord(x, y);
                char symbol = '.';
                
                // Check stores
                for (DeliverySearch.Coord store : search.stores) {
                    if (store.equals(c)) {
                        symbol = 'S';
                        break;
                    }
                }
                
                // Check customers
                if (symbol == '.') {
                    for (DeliverySearch.Coord customer : search.customers) {
                        if (customer.equals(c)) {
                            symbol = 'C';
                            break;
                        }
                    }
                }
                
                // Check tunnels
                if (symbol == '.') {
                    for (DeliverySearch.Tunnel tunnel : search.tunnels) {
                        if (tunnel.a.equals(c) || tunnel.b.equals(c)) {
                            symbol = '#';
                            break;
                        }
                    }
                }
                
                System.out.print(symbol + " ");
            }
            System.out.println();
        }
    }
}