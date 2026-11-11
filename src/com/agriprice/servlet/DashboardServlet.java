package com.agriprice.servlet;

import com.agriprice.dao.AgentDAO;
import com.agriprice.dao.MarketDAO;
import com.agriprice.dao.PriceDAO;
import com.agriprice.dao.ProductDAO;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PriceDAO priceDAO = new PriceDAO();
    private final MarketDAO marketDAO = new MarketDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final AgentDAO agentDAO = new AgentDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalReports", priceDAO.getTotalReportsCount());
            stats.put("totalMarkets", marketDAO.getAll().size());
            stats.put("totalProducts", productDAO.getAll().size());
            stats.put("totalAgents", agentDAO.getAllAgents().size());
            stats.put("recentReports", priceDAO.getLatestPrices(10));
            stats.put("marketDistribution", priceDAO.getMarketSubmissionDistribution());

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(stats));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Dashboard stats error: " + e.getMessage() + "\"}");
        }
    }
}
