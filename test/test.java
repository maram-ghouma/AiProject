package test;

import code.DeliverySearch;
public class test {
    public static void main(String[] args) {
        DeliverySearch search = new DeliverySearch();
        
        // Small grid
        search.m = 4;
        search.n = 4;
        search.p = 1;
        search.s = 1;
        
        search.stores.clear();
        search.stores.add(new DeliverySearch.Coord(0, 0));
        
        search.customers.clear();
        search.customers.add(new DeliverySearch.Coord(3, 3));
        
        // Simple traffic
        search.traffic.clear();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                search.traffic.put(new DeliverySearch.Edge(
                    new DeliverySearch.Coord(x, y), 
                    new DeliverySearch.Coord(x + 1, y)), 1);
                search.traffic.put(new DeliverySearch.Edge(
                    new DeliverySearch.Coord(x + 1, y), 
                    new DeliverySearch.Coord(x, y)), 1);
            }
        }
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                search.traffic.put(new DeliverySearch.Edge(
                    new DeliverySearch.Coord(x, y), 
                    new DeliverySearch.Coord(x, y + 1)), 1);
                search.traffic.put(new DeliverySearch.Edge(
                    new DeliverySearch.Coord(x, y + 1), 
                    new DeliverySearch.Coord(x, y)), 1);
            }
        }
        
        // Test path with visualization
        String result = search.path(
            search.stores.get(0), 
            search.customers.iterator().next(), 
            "BF"
        );
        
        System.out.println("Result: " + result);
        String[] parts = result.split(";");
        search.visualizePath(search.stores.get(0), search.customers.iterator().next(), parts[0]);
    }
}