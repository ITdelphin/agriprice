package com.agriprice.servlet;

import com.agriprice.dao.MarketDAO;
import com.agriprice.model.Market;
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MarketServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MarketDAO marketDAO = new MarketDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");
        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Market m = marketDAO.findById(id);
                if (m != null) {
                    out.print(gson.toJson(m));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Market not found.\"}");
                }
            } else {
                List<Market> list = marketDAO.getAll();
                out.print(gson.toJson(list));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database query error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Check authentication for modifications
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("agentId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Unauthorized: Only authenticated agents can manage markets.\"}");
            return;
        }

        String action = "create";
        int marketId = 0;
        String name = null;
        String district = null;
        String province = null;
        String desc = null;

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
                if (json.has("marketId")) marketId = json.get("marketId").getAsInt();
                if (json.has("marketName")) name = json.get("marketName").getAsString();
                if (json.has("district")) district = json.get("district").getAsString();
                if (json.has("province")) province = json.get("province").getAsString();
                if (json.has("locationDescription")) desc = json.get("locationDescription").getAsString();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Malformed JSON.\"}");
                return;
            }
        } else {
            if (request.getParameter("action") != null) action = request.getParameter("action");
            if (request.getParameter("marketId") != null) marketId = Integer.parseInt(request.getParameter("marketId"));
            name = request.getParameter("marketName");
            district = request.getParameter("district");
            province = request.getParameter("province");
            desc = request.getParameter("locationDescription");
        }

        try {
            if ("delete".equalsIgnoreCase(action)) {
                boolean deleted = marketDAO.delete(marketId);
                if (deleted) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Market deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Market not found or in use.\"}");
                }
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                Market m = new Market(marketId, name, district, province, desc, null);
                boolean updated = marketDAO.update(m);
                if (updated) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Market updated successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Market not found.\"}");
                }
                return;
            }

            // Create Market
            if (name == null || name.trim().isEmpty() || district == null || district.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Market name and district are required.\"}");
                return;
            }
            Market m = new Market(0, name, district, province != null ? province : "Western Province", desc, null);
            int newId = marketDAO.create(m);
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Market created successfully.");
            resp.put("marketId", newId);
            out.print(gson.toJson(resp));

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
