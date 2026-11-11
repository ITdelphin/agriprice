package com.agriprice.servlet;

import com.agriprice.dao.ProductDAO;
import com.agriprice.model.Product;
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

public class ProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ProductDAO productDAO = new ProductDAO();
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
                Product p = productDAO.findById(id);
                if (p != null) {
                    out.print(gson.toJson(p));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Product not found.\"}");
                }
            } else {
                List<Product> list = productDAO.getAll();
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

        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("agentId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Unauthorized: Only authenticated agents can manage products.\"}");
            return;
        }

        String action = "create";
        int productId = 0;
        String name = null;
        String category = null;
        String unit = "kg";
        String desc = null;
        String status = "ACTIVE";

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
                if (json.has("productId")) productId = json.get("productId").getAsInt();
                if (json.has("productName")) name = json.get("productName").getAsString();
                if (json.has("category")) category = json.get("category").getAsString();
                if (json.has("defaultUnit")) unit = json.get("defaultUnit").getAsString();
                if (json.has("description")) desc = json.get("description").getAsString();
                if (json.has("status")) status = json.get("status").getAsString();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Malformed JSON.\"}");
                return;
            }
        } else {
            if (request.getParameter("action") != null) action = request.getParameter("action");
            if (request.getParameter("productId") != null) productId = Integer.parseInt(request.getParameter("productId"));
            name = request.getParameter("productName");
            category = request.getParameter("category");
            unit = request.getParameter("defaultUnit");
            desc = request.getParameter("description");
            if (request.getParameter("status") != null) status = request.getParameter("status");
        }

        try {
            if ("delete".equalsIgnoreCase(action)) {
                boolean deleted = productDAO.delete(productId);
                if (deleted) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Product deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Product not found or in use.\"}");
                }
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                Product p = new Product(productId, name, category, unit, desc, status);
                boolean updated = productDAO.update(p);
                if (updated) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Product updated successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Product not found.\"}");
                }
                return;
            }

            // Create
            if (name == null || name.trim().isEmpty() || category == null || category.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Product name and category are required.\"}");
                return;
            }
            Product p = new Product(0, name, category, unit != null ? unit : "kg", desc, status);
            int newId = productDAO.create(p);
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Product created successfully.");
            resp.put("productId", newId);
            out.print(gson.toJson(resp));

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
