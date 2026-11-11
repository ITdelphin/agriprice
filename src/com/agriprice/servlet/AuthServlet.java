package com.agriprice.servlet;

import com.agriprice.dao.AgentDAO;
import com.agriprice.model.Agent;
import com.agriprice.util.PasswordUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final AgentDAO agentDAO = new AgentDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        Map<String, Object> res = new HashMap<>();

        if (session != null && session.getAttribute("agentId") != null) {
            res.put("authenticated", true);
            res.put("agentId", session.getAttribute("agentId"));
            res.put("fullName", session.getAttribute("agentName"));
            res.put("email", session.getAttribute("agentEmail"));
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            res.put("authenticated", false);
            response.setStatus(HttpServletResponse.SC_OK);
        }
        out.print(gson.toJson(res));
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/login";
        }

        if ("/logout".equalsIgnoreCase(pathInfo)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Logged out successfully");
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();
            return;
        }

        // Login handling
        String email = null;
        String password = null;

        // Support both JSON body and form parameters
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
                if (json.has("email")) email = json.get("email").getAsString();
                if (json.has("password")) password = json.get("password").getAsString();
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Invalid JSON request format.\"}");
                out.flush();
                return;
            }
        } else {
            email = request.getParameter("email");
            password = request.getParameter("password");
        }

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Email and password are required.\"}");
            out.flush();
            return;
        }

        try {
            Agent agent = agentDAO.findByEmail(email.trim());
            if (agent == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Invalid credentials. User not found.\"}");
                out.flush();
                return;
            }

            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"Account is suspended or inactive. Please contact administration.\"}");
                out.flush();
                return;
            }

            boolean verified = PasswordUtil.verifyPassword(password, agent.getPasswordSalt(), agent.getPasswordHash());
            if (!verified) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Invalid credentials. Incorrect password.\"}");
                out.flush();
                return;
            }

            // Create Authenticated Session
            HttpSession session = request.getSession(true);
            session.setAttribute("agentId", agent.getAgentId());
            session.setAttribute("agentName", agent.getFullName());
            session.setAttribute("agentEmail", agent.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "Authentication successful.");
            data.put("agentId", agent.getAgentId());
            data.put("fullName", agent.getFullName());
            data.put("email", agent.getEmail());

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(data));
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error during authentication: " + e.getMessage() + "\"}");
            out.flush();
        }
    }
}
