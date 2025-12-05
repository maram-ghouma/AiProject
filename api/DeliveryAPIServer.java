package api;

import code.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class DeliveryAPIServer {
    private static final int PORT = 8080;
    private static DeliverySearch search = new DeliverySearch();
    private static DeliveryPlanner planner = new DeliveryPlanner(search);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // CORS handler wrapper
        HttpHandler corsHandler = (HttpExchange exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
        };

        // Endpoints
        server.createContext("/api/grid/generate", new GenerateGridHandler());
        server.createContext("/api/solve", new SolveHandler());
        server.createContext("/api/solve/all", new SolveAllHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         Delivery Planning API Server Started              ║");
        System.out.println("║              http://localhost:" + PORT + "                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("\nEndpoints:");
        System.out.println("  GET  /api/grid/generate  - Generate new grid");
        System.out.println("  POST /api/solve          - Solve with strategy");
        System.out.println("  POST /api/solve/all      - Solve with all strategies");
        System.out.println("\nPress Ctrl+C to stop the server.");
    }

    static class GenerateGridHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCORSHeaders(exchange);
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // Generate new grid
                search = new DeliverySearch();
                planner = new DeliveryPlanner(search);
                String grid = search.GenGrid();
                
                // Build JSON response
                JSONObject response = new JSONObject();
                response.put("n", search.n);
                response.put("m", search.m);
                response.put("p", search.p);
                response.put("s", search.s);
                
                // Customers
                JSONArray customers = new JSONArray();
                int custId = 0;
                for (DeliverySearch.Coord c : search.customers) {
                    JSONObject customer = new JSONObject();
                    customer.put("id", custId++);
                    customer.put("x", c.x);
                    customer.put("y", c.y);
                    customers.put(customer);
                }
                response.put("customers", customers);
                
                // Stores
                JSONArray stores = new JSONArray();
                int storeId = 0;
                for (DeliverySearch.Coord s : search.stores) {
                    JSONObject store = new JSONObject();
                    store.put("id", storeId++);
                    store.put("x", s.x);
                    store.put("y", s.y);
                    stores.put(store);
                }
                response.put("stores", stores);
                
                // Tunnels
                JSONArray tunnels = new JSONArray();
                for (DeliverySearch.Tunnel t : search.tunnels) {
                    JSONObject tunnel = new JSONObject();
                    JSONObject a = new JSONObject();
                    a.put("x", t.a.x);
                    a.put("y", t.a.y);
                    JSONObject b = new JSONObject();
                    b.put("x", t.b.x);
                    b.put("y", t.b.y);
                    tunnel.put("a", a);
                    tunnel.put("b", b);
                    tunnels.put(tunnel);
                }
                response.put("tunnels", tunnels);
                
                // Traffic
                JSONObject traffic = new JSONObject();
                for (Map.Entry<DeliverySearch.Edge, Integer> entry : search.traffic.entrySet()) {
                    DeliverySearch.Edge edge = entry.getKey();
                    String key = edge.src.x + "," + edge.src.y + "-" + 
                                edge.dst.x + "," + edge.dst.y;
                    traffic.put(key, entry.getValue());
                }
                response.put("traffic", traffic);
                
                // Grid string for backend
                response.put("gridString", grid);
                
                sendResponse(exchange, 200, response.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Error generating grid: " + e.getMessage());
            }
        }
    }

    static class SolveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCORSHeaders(exchange);
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // Read request body
                String requestBody = readRequestBody(exchange);
                JSONObject request = new JSONObject(requestBody);
                
                String strategy = request.getString("strategy");
                JSONObject gridData = request.getJSONObject("gridData");
                
                // Parse grid data
                String gridString = gridData.getString("gridString");
                String traffic = generateTrafficString(gridData.getJSONObject("traffic"));
                
                // Solve
                long startTime = System.currentTimeMillis();
                String planOutput = planner.plan(gridString, traffic, strategy, false);
                long endTime = System.currentTimeMillis();
                
                // Parse the plan output
                JSONObject result = parsePlanOutput(planOutput, endTime - startTime);
                result.put("strategy", strategy);
                
                sendResponse(exchange, 200, result.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Error solving: " + e.getMessage());
            }
        }
    }

    static class SolveAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCORSHeaders(exchange);
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                JSONObject request = new JSONObject(requestBody);
                JSONObject gridData = request.getJSONObject("gridData");
                
                String gridString = gridData.getString("gridString");
                String traffic = generateTrafficString(gridData.getJSONObject("traffic"));
                
                String[] strategies = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
                JSONObject allResults = new JSONObject();
                
                for (String strategy : strategies) {
                    try {
                        long startTime = System.currentTimeMillis();
                        String planOutput = planner.plan(gridString, traffic, strategy, false);
                        long endTime = System.currentTimeMillis();
                        
                        JSONObject result = parsePlanOutput(planOutput, endTime - startTime);
                        result.put("strategy", strategy);
                        allResults.put(strategy, result);
                        
                        System.out.println("Completed: " + strategy);
                    } catch (Exception e) {
                        System.err.println("Error with " + strategy + ": " + e.getMessage());
                    }
                }
                
                sendResponse(exchange, 200, allResults.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Error solving all: " + e.getMessage());
            }
        }
    }

    private static String generateTrafficString(JSONObject trafficObj) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = trafficObj.keys();
        
        while (keys.hasNext()) {
            String key = keys.next();
            int cost = trafficObj.getInt(key);
            
            // Parse key format: "x1,y1-x2,y2"
            String[] parts = key.split("-");
            String[] src = parts[0].split(",");
            String[] dst = parts[1].split(",");
            
            sb.append(src[0]).append(",").append(src[1]).append(",")
              .append(dst[0]).append(",").append(dst[1]).append(",")
              .append(cost).append(";");
        }
        
        return sb.toString();
    }

    private static JSONObject parsePlanOutput(String output, long timeMs) {
        JSONObject result = new JSONObject();
        JSONArray deliveries = new JSONArray();
        
        double totalCost = 0;
        int totalNodes = 0;
        
        String[] lines = output.split("\n");
        JSONObject currentDelivery = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("Truck") && line.contains("->")) {
                if (currentDelivery != null) {
                    deliveries.put(currentDelivery);
                }
                currentDelivery = new JSONObject();
                
                // Parse "Truck X -> Customer Y"
                String[] parts = line.split("->");
                int truckId = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                int customerId = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                
                currentDelivery.put("truckId", truckId);
                currentDelivery.put("customerId", customerId);
                
            } else if (line.startsWith("Plan :")) {
                String planStr = line.substring(7).trim();
                String[] actions = planStr.split(",");
                JSONArray actionsArray = new JSONArray();
                for (String action : actions) {
                    actionsArray.put(action.trim());
                }
                if (currentDelivery != null) {
                    currentDelivery.put("actions", actionsArray);
                    currentDelivery.put("path", generatePath(actions));
                }
                
            } else if (line.startsWith("Total Cost")) {
                String costStr = line.split(":")[1].trim();
                double cost = Double.parseDouble(costStr);
                if (currentDelivery != null) {
                    currentDelivery.put("cost", cost);
                }
                totalCost += cost;
                
            } else if (line.startsWith("Nodes Expanded")) {
                String nodesStr = line.split(":")[1].trim();
                int nodes = Integer.parseInt(nodesStr);
                if (currentDelivery != null) {
                    currentDelivery.put("nodes", nodes);
                }
                totalNodes += nodes;
            }
        }
        
        if (currentDelivery != null) {
            deliveries.put(currentDelivery);
        }
        
        result.put("deliveries", deliveries);
        result.put("totalCost", totalCost);
        result.put("nodesExpanded", totalNodes);
        result.put("timeMs", timeMs);
        
        return result;
    }

    private static JSONArray generatePath(String[] actions) {
        // This would need the actual grid state to generate the full path
        // For now, return a simple path representation
        JSONArray path = new JSONArray();
        // You'll need to track position through the grid based on actions
        // This is a placeholder
        return path;
    }

    private static void addCORSHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        sendResponse(exchange, statusCode, error.toString());
    }
}