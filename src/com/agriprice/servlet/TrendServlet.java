package com.agriprice.servlet;

import com.agriprice.dao.PriceDAO;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TrendServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PriceDAO priceDAO = new PriceDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) pathInfo = "/history";

        try {
            if ("/history".equalsIgnoreCase(pathInfo)) {
                String pIdStr = request.getParameter("productId");
                String mIdStr = request.getParameter("marketId");
                String daysStr = request.getParameter("days");

                int productId = (pIdStr != null && !pIdStr.isEmpty()) ? Integer.parseInt(pIdStr) : 1;
                Integer marketId = (mIdStr != null && !mIdStr.isEmpty()) ? Integer.parseInt(mIdStr) : null;
                int days = (daysStr != null && !daysStr.isEmpty()) ? Integer.parseInt(daysStr) : 14;
                if (productId <= 0 || (marketId != null && marketId <= 0) || days < 1 || days > 365) {
                    throw new IllegalArgumentException("Trend filters are outside their allowed range.");
                }

                List<Map<String, Object>> trendData = priceDAO.getHistoricalTrends(productId, marketId, days);
                out.print(gson.toJson(trendData));
                return;
            }

            if ("/comparison".equalsIgnoreCase(pathInfo)) {
                String pIdStr = request.getParameter("productId");
                String dateStr = request.getParameter("date");

                int productId = (pIdStr != null && !pIdStr.isEmpty()) ? Integer.parseInt(pIdStr) : 1;
                Date date = (dateStr != null && !dateStr.isEmpty()) ? Date.valueOf(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)) : null;
                if (productId <= 0) throw new IllegalArgumentException("Product identifier must be positive.");

                List<Map<String, Object>> compData = priceDAO.getMarketComparison(productId, date);
                out.print(gson.toJson(compData));
                return;
            }

            if ("/distribution".equalsIgnoreCase(pathInfo)) {
                List<Map<String, Object>> distData = priceDAO.getMarketSubmissionDistribution();
                out.print(gson.toJson(distData));
                return;
            }

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"success\":false,\"message\":\"Unknown trend endpoint.\"}");

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Trend query error: " + e.getMessage() + "\"}");
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid trend filter. Check the product, market, date and history range.\"}");
        }
    }
}
