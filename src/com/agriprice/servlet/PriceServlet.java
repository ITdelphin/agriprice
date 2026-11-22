package com.agriprice.servlet;

import com.agriprice.dao.PriceDAO;
import com.agriprice.model.AuditLog;
import com.agriprice.model.PriceReport;
import com.agriprice.util.JsonFactory;
import com.agriprice.util.InvalidPriceException;
import com.agriprice.util.PriceOutlierException;
import com.agriprice.util.UnknownProductException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class PriceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PriceDAO priceDAO = new PriceDAO();
    private final Gson gson = JsonFactory.create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || "/list".equalsIgnoreCase(pathInfo)) {
            // Filtered or latest prices
            String prodParam = request.getParameter("productId");
            String marketParam = request.getParameter("marketId");
            String dateParam = request.getParameter("date");
            String search = request.getParameter("search");
            boolean includeAll = "true".equalsIgnoreCase(request.getParameter("all"));

            Integer productId;
            Integer marketId;
            Date reportDate;
            try {
                productId = (prodParam != null && !prodParam.isEmpty()) ? Integer.valueOf(prodParam) : null;
                marketId = (marketParam != null && !marketParam.isEmpty()) ? Integer.valueOf(marketParam) : null;
                reportDate = (dateParam != null && !dateParam.isEmpty()) ? parseIsoDate(dateParam) : null;
                if ((productId != null && productId <= 0) || (marketId != null && marketId <= 0)) {
                    throw new IllegalArgumentException("Identifiers must be positive.");
                }
            } catch (IllegalArgumentException | java.time.DateTimeException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid product, market or ISO date filter.\"}");
                return;
            }

            try {
                List<PriceReport> reports;
                if (includeAll || productId != null || marketId != null || reportDate != null || (search != null && !search.isEmpty())) {
                    reports = priceDAO.getFilteredPrices(productId, marketId, reportDate, search);
                } else {
                    reports = priceDAO.getLatestPrices(50);
                }
                out.print(gson.toJson(reports));
                out.flush();
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Database query error: " + e.getMessage() + "\"}");
                out.flush();
            }
            return;
        }

        if ("/agent".equalsIgnoreCase(pathInfo)) {
            // Returns reports submitted by authenticated agent
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("agentId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Agent authentication required.\"}");
                out.flush();
                return;
            }
            int agentId = (Integer) session.getAttribute("agentId");
            try {
                List<PriceReport> agentReports = priceDAO.getReportsByAgent(agentId, 50);
                out.print(gson.toJson(agentReports));
                out.flush();
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Error fetching agent reports: " + e.getMessage() + "\"}");
                out.flush();
            }
            return;
        }

        if ("/sql-injection-demo".equalsIgnoreCase(pathInfo)) {
            // Security demonstration
            String testInput = request.getParameter("payload");
            if (testInput == null) testInput = "' OR '1'='1";
            try {
                List<PriceReport> result = priceDAO.demonstratePreparedStatementDefense(testInput);
                Map<String, Object> demo = new HashMap<>();
                demo.put("payloadAttempted", testInput);
                demo.put("recordsReturned", result.size());
                demo.put("explanation", "PreparedStatement parameterization bound the malicious payload as literal text; no injection occurred.");
                out.print(gson.toJson(demo));
                out.flush();
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.print("{\"success\":false,\"message\":\"Resource not found.\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        PrintWriter out = response.getWriter();

        // 1. Check Authentication (Strict Rule: Agent must be authenticated)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("agentId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Unauthorized: Only authenticated agents may submit or modify prices.\"}");
            out.flush();
            return;
        }
        int agentId = (Integer) session.getAttribute("agentId");

        // Parse Request Body (JSON or Form)
        int productId = 0;
        int marketId = 0;
        double price = 0.0;
        String unit = "kg";
        Date reportDate = new Date(System.currentTimeMillis());
        int reportId = 0;
        String action = "create";

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            try {
                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                if (json.has("action")) action = json.get("action").getAsString();
                if (json.has("productId")) productId = json.get("productId").getAsInt();
                if (json.has("marketId")) marketId = json.get("marketId").getAsInt();
                if (json.has("price")) price = json.get("price").getAsDouble();
                if (json.has("unit")) unit = json.get("unit").getAsString();
                if (json.has("reportDate")) reportDate = parseIsoDate(json.get("reportDate").getAsString());
                if (json.has("reportId")) reportId = json.get("reportId").getAsInt();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Malformed JSON payload.\"}");
                out.flush();
                return;
            }
        } else {
            String act = request.getParameter("action");
            if (act != null) action = act;
            try {
                if (request.getParameter("productId") != null) productId = Integer.parseInt(request.getParameter("productId"));
                if (request.getParameter("marketId") != null) marketId = Integer.parseInt(request.getParameter("marketId"));
                if (request.getParameter("price") != null) price = Double.parseDouble(request.getParameter("price"));
                if (request.getParameter("unit") != null) unit = request.getParameter("unit");
                if (request.getParameter("reportDate") != null) reportDate = parseIsoDate(request.getParameter("reportDate"));
                if (request.getParameter("reportId") != null) reportId = Integer.parseInt(request.getParameter("reportId"));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid numeric or date parameter.\"}");
                out.flush();
                return;
            }
        }

        if (!java.util.Arrays.asList("kg", "piece", "sack", "bunch").contains(unit)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Unit must be kg, piece, sack or bunch.\"}");
            return;
        }

        // Action: UPDATE
        if ("update".equalsIgnoreCase(action) || (request.getPathInfo() != null && request.getPathInfo().startsWith("/update"))) {
            if (reportId <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Report ID required for update.\"}");
                return;
            }
            try {
                PriceReport rep = priceDAO.findById(reportId);
                if (rep == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Price report not found.\"}");
                    return;
                }
                if (rep.getAgentId() != agentId) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"success\":false,\"message\":\"Forbidden: Agents can only update their own price submissions.\"}");
                    return;
                }

                PriceReport updateReport = new PriceReport();
                updateReport.setReportId(reportId);
                updateReport.setAgentId(agentId);
                updateReport.setProductId(rep.getProductId());
                updateReport.setMarketId(rep.getMarketId());
                updateReport.setUnit(unit);
                updateReport.setPriceRwf(price);
                updateReport.setReportDate(rep.getReportDate());

                AuditLog audit = new AuditLog();
                audit.setAgentId(agentId);
                audit.setReportId(reportId);
                audit.setOldValue(String.format("{\"price_rwf\": %.2f, \"unit\": \"%s\"}", rep.getPriceRwf(), rep.getUnit()));
                audit.setNewValue(String.format("{\"price_rwf\": %.2f, \"unit\": \"%s\"}", price, unit));

                boolean updated = priceDAO.updatePriceWithAudit(updateReport, audit);
                if (updated) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Price report updated successfully with audit record.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"success\":false,\"message\":\"Failed to update price report.\"}");
                }
            } catch (InvalidPriceException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            } catch (PriceOutlierException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"outlier\":true,\"message\":\"" + e.getMessage() + "\"}");
            } catch (UnknownProductException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", e.getMessage());
                out.print(gson.toJson(err));
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Database update error: " + e.getMessage() + "\"}");
            }
            return;
        }

        // Action: DELETE
        if ("delete".equalsIgnoreCase(action) || (request.getPathInfo() != null && request.getPathInfo().startsWith("/delete"))) {
            if (reportId <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Report ID required for deletion.\"}");
                return;
            }
            try {
                PriceReport rep = priceDAO.findById(reportId);
                if (rep == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Price report not found.\"}");
                    return;
                }
                if (rep.getAgentId() != agentId) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"success\":false,\"message\":\"Forbidden: Agents can only delete their own submissions.\"}");
                    return;
                }
                AuditLog audit = new AuditLog();
                audit.setAgentId(agentId);
                audit.setReportId(reportId);
                audit.setOldValue(String.format("{\"price_rwf\": %.2f, \"product\": \"%s\"}", rep.getPriceRwf(), rep.getProductName()));
                audit.setNewValue("{\"status\": \"DELETED\"}");

                boolean deleted = priceDAO.deletePrice(reportId, agentId, audit);
                if (deleted) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Price report deleted successfully with audit record.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"success\":false,\"message\":\"Could not delete report.\"}");
                }
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
            }
            return;
        }

        // Action: CREATE
        PriceReport report = new PriceReport();
        report.setProductId(productId);
        report.setMarketId(marketId);
        report.setAgentId(agentId);
        report.setUnit(unit);
        report.setPriceRwf(price);
        report.setReportDate(reportDate);

        AuditLog audit = new AuditLog();
        audit.setAgentId(agentId);
        audit.setOldValue(null);
        audit.setNewValue(String.format("{\"product_id\":%d,\"market_id\":%d,\"price\":%.2f,\"date\":\"%s\"}",
            productId, marketId, price, reportDate.toString()));

        try {
            int newId = priceDAO.submitPriceWithAudit(report, audit);
            response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Price report submitted successfully!");
            resp.put("reportId", newId);
            out.print(gson.toJson(resp));
            out.flush();

        } catch (InvalidPriceException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            out.print("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        } catch (PriceOutlierException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request with Outlier Alert
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("outlier", true);
            err.put("message", e.getMessage());
            err.put("deviation", e.getDeviationPercentage());
            err.put("threshold", e.getThreshold());
            out.print(gson.toJson(err));
        } catch (UnknownProductException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            out.print(gson.toJson(err));
        } catch (SQLException e) {
            if ((e.getMessage() != null && e.getMessage().contains("Duplicate")) || e.getErrorCode() == 1062) {
                response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
                out.print("{\"success\":false,\"message\":\"A report already exists for this product, market and date.\"}");
            } else if (e.getErrorCode() == 1452) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Select a valid product and market before submitting.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
            }
        }
    }

    private Date parseIsoDate(String value) {
        return Date.valueOf(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
