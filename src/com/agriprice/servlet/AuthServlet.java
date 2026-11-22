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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final long RESET_TOKEN_MINUTES = 30;
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

        JsonObject payload;
        try {
            payload = readPayload(request);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid JSON request format.\"}");
            out.flush();
            return;
        }

        if ("/forgot-password".equalsIgnoreCase(pathInfo)) {
            handleForgotPassword(response, out, getString(payload, "email"));
            return;
        }

        if ("/reset-password".equalsIgnoreCase(pathInfo)) {
            handleResetPassword(response, out, getString(payload, "token"), getString(payload, "newPassword"));
            return;
        }

        String email = getString(payload, "email");
        String password = getString(payload, "password");

        if (email == null || password == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Email and password are required.\"}");
            out.flush();
            return;
        }

        try {
            Agent agent = agentDAO.findByEmail(email);
            if (agent == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Invalid credentials. User not found.\"}");
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

            HttpSession session = request.getSession(true);
            request.changeSessionId();
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

    private void handleForgotPassword(HttpServletResponse response, PrintWriter out, String email) throws IOException {
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Enter your registered email address.\"}");
            out.flush();
            return;
        }

        try {
            Agent agent = agentDAO.findByEmail(email);
            if (agent == null) {
                Map<String, Object> data = new HashMap<>();
                data.put("success", true);
                data.put("message", "If the account exists, a reset token has been created.");
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(gson.toJson(data));
                out.flush();
                return;
            }

            String token = generateToken();
            String tokenHash = hashToken(token);
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + RESET_TOKEN_MINUTES * 60L * 1000L);
            agentDAO.createPasswordResetToken(agent.getAgentId(), tokenHash, expiresAt);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "Reset token created. It expires in " + RESET_TOKEN_MINUTES + " minutes.");
            data.put("token", token);
            data.put("email", agent.getEmail());
            data.put("resetUrl", "reset-password.html?token=" + token + "&email=" + agent.getEmail());
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(data));
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Could not create a reset token: " + e.getMessage() + "\"}");
            out.flush();
        }
    }

    private void handleResetPassword(HttpServletResponse response, PrintWriter out, String token, String newPassword) throws IOException {
        if (token == null || newPassword == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Reset token and new password are required.\"}");
            out.flush();
            return;
        }
        if (newPassword.length() < 8) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"New password must be at least 8 characters.\"}");
            out.flush();
            return;
        }

        try {
            String tokenHash = hashToken(token);
            Integer agentId = agentDAO.findAgentForResetToken(tokenHash, new Timestamp(System.currentTimeMillis()));
            if (agentId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Reset token is invalid or has expired.\"}");
                out.flush();
                return;
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(newPassword, salt);
            agentDAO.updatePassword(agentId, hash, salt);
            agentDAO.markResetTokenUsed(tokenHash);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "Password reset successfully. You can now sign in.");
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(data));
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Could not reset password: " + e.getMessage() + "\"}");
            out.flush();
        }
    }

    private JsonObject readPayload(HttpServletRequest request) throws IOException {
        JsonObject json = new JsonObject();
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        }
        addParameter(json, request, "email");
        addParameter(json, request, "password");
        addParameter(json, request, "token");
        addParameter(json, request, "newPassword");
        return json;
    }

    private void addParameter(JsonObject json, HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null) {
            json.addProperty(name, value);
        }
    }

    private String getString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        String value = json.get(key).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02x", value & 0xff));
        }
        return sb.toString();
    }
}
