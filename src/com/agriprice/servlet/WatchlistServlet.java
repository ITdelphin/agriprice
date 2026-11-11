package com.agriprice.servlet;

import com.agriprice.dao.WatchlistDAO;
import com.agriprice.model.Watchlist;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WatchlistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final WatchlistDAO watchlistDAO = new WatchlistDAO();
    private final Gson gson = new Gson();

    private String getOrCreateVisitorSessionId(HttpServletRequest request, HttpServletResponse response) {
        String visitorId = request.getParameter("visitorSessionId");
        if (visitorId != null && !visitorId.trim().isEmpty()) {
            return visitorId.trim();
        }

        // Check cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("agriprice_visitor".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                    return c.getValue();
                }
            }
        }

        // Generate new persistent visitor cookie
        String newVisitorId = "visitor-" + UUID.randomUUID().toString();
        Cookie visitorCookie = new Cookie("agriprice_visitor", newVisitorId);
        visitorCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
        visitorCookie.setPath("/");
        visitorCookie.setHttpOnly(false); // Accessible by client-side JS MVC
        response.addCookie(visitorCookie);
        return newVisitorId;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String visitorSessionId = getOrCreateVisitorSessionId(request, response);

        try {
            List<Watchlist> items = watchlistDAO.getBySession(visitorSessionId);
            Map<String, Object> result = new HashMap<>();
            result.put("visitorSessionId", visitorSessionId);
            result.put("items", items);
            long alertCount = items.stream().filter(Watchlist::isTargetReached).count();
            result.put("alertCount", alertCount);
            out.print(gson.toJson(result));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String visitorSessionId = getOrCreateVisitorSessionId(request, response);

        String action = "add";
        int watchlistId = 0;
        int productId = 0;
        Integer marketId = null;
        double targetPrice = 0.0;

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            try {
                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                if (json.has("action")) action = json.get("action").getAsString();
                if (json.has("watchlistId")) watchlistId = json.get("watchlistId").getAsInt();
                if (json.has("productId")) productId = json.get("productId").getAsInt();
                if (json.has("marketId") && !json.get("marketId").isJsonNull()) {
                    marketId = json.get("marketId").getAsInt();
                }
                if (json.has("targetPrice")) targetPrice = json.get("targetPrice").getAsDouble();
                if (json.has("visitorSessionId")) visitorSessionId = json.get("visitorSessionId").getAsString();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Malformed JSON payload.\"}");
                return;
            }
        } else {
            if (request.getParameter("action") != null) action = request.getParameter("action");
            if (request.getParameter("watchlistId") != null) watchlistId = Integer.parseInt(request.getParameter("watchlistId"));
            if (request.getParameter("productId") != null) productId = Integer.parseInt(request.getParameter("productId"));
            if (request.getParameter("marketId") != null && !request.getParameter("marketId").isEmpty()) {
                marketId = Integer.parseInt(request.getParameter("marketId"));
            }
            if (request.getParameter("targetPrice") != null) targetPrice = Double.parseDouble(request.getParameter("targetPrice"));
        }

        try {
            if ("delete".equalsIgnoreCase(action)) {
                boolean deleted = watchlistDAO.deleteItem(watchlistId, visitorSessionId);
                if (deleted) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Watchlist item removed.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Item not found.\"}");
                }
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                if (targetPrice <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Target price must be greater than zero.\"}");
                    return;
                }
                boolean updated = watchlistDAO.updateTargetPrice(watchlistId, visitorSessionId, targetPrice);
                if (updated) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Target price updated successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Item not found.\"}");
                }
                return;
            }

            // Action: ADD
            if (productId <= 0 || targetPrice <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Valid Product ID and Target Price (>0) are required.\"}");
                return;
            }

            Watchlist item = new Watchlist();
            item.setVisitorSessionId(visitorSessionId);
            item.setProductId(productId);
            item.setMarketId(marketId);
            item.setTargetPrice(targetPrice);

            int newId = watchlistDAO.addToWatchlist(item);
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Product added to watchlist!");
            resp.put("watchlistId", newId);
            resp.put("visitorSessionId", visitorSessionId);
            out.print(gson.toJson(resp));

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
