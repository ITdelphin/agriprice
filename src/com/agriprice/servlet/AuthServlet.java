package com.agriprice.servlet;

import com.agriprice.dao.AgentDAO;
import com.agriprice.model.Agent;
import com.agriprice.util.PasswordUtil;
import com.agriprice.util.SmtpEmailUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
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
    private static final long RESET_TOKEN_MINUTES = 15;
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
            handleForgotPassword(request, response, out, getString(payload, "email"));
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

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response, PrintWriter out, String email) throws IOException {
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
                data.put("message", "If the account exists, a password reset link has been sent.");
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(gson.toJson(data));
                out.flush();
                return;
            }

            String token = generateToken();
            String tokenHash = hashToken(token);
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + RESET_TOKEN_MINUTES * 60L * 1000L);
            agentDAO.createPasswordResetToken(agent.getAgentId(), tokenHash, expiresAt);

            String resetUrl = buildPublicBaseUrl(request)
                    + "/reset-password.html?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8.name())
                    + "&email=" + URLEncoder.encode(agent.getEmail(), StandardCharsets.UTF_8.name());

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "If the account exists, a password reset link has been sent.");

            if (!SmtpEmailUtil.isConfigured() && !isPreviewEnabled()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                data.put("success", false);
                data.put("message", "Password reset email is not configured. Contact the administrator.");
                out.print(gson.toJson(data));
                out.flush();
                return;
            }

            if (SmtpEmailUtil.isConfigured()) {
                SmtpEmailUtil.sendPasswordResetEmail(agent.getEmail(), resetUrl, (int) RESET_TOKEN_MINUTES);
            } else {
                data.put("previewToken", token);
                data.put("previewUrl", resetUrl);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(data));
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Could not create a reset token: " + e.getMessage() + "\"}");
            out.flush();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.print("{\"success\":false,\"message\":\"Could not send the reset email. Try again later.\"}");
            out.flush();
        }
    }

    private String buildPublicBaseUrl(HttpServletRequest request) {
        String configured = setting("AGRIPRICE_PUBLIC_BASE_URL");
        if (configured != null) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        String scheme = first(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = first(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        if (host == null) {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }
        return scheme + "://" + host + request.getContextPath();
    }

    private boolean isPreviewEnabled() {
        return "true".equalsIgnoreCase(setting("AGRIPRICE_RESET_PREVIEW"));
    }

    private String setting(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(name);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String first(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary.trim();
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
