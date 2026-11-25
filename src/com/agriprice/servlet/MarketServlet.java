package com.agriprice.servlet;

import com.agriprice.dao.MarketDAO;
import com.agriprice.model.Market;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@MultipartConfig(maxFileSize = 2097152, maxRequestSize = 3145728)
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
            out.print("{\"success\":false,\"message\":\"Markets are temporarily unavailable. Please try again.\"}");
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
        String imageUrl = null;

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
                if (json.has("imageUrl")) imageUrl = json.get("imageUrl").getAsString();
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
            imageUrl = request.getParameter("imageUrl");
        }

        try {
            Part imagePart = contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")
                    ? request.getPart("image") : null;
            if (imagePart != null && imagePart.getSize() > 0) {
                imageUrl = saveMarketImage(request, imagePart);
            }
            if (imageUrl != null && !imageUrl.matches("assets/images/markets/[a-zA-Z0-9_-]+\\.(jpg|png|gif)")) {
                imageUrl = null;
            }

            if ("delete".equalsIgnoreCase(action)) {
                Market existing = marketDAO.findById(marketId);
                boolean deleted = marketDAO.delete(marketId);
                if (deleted) {
                    if (existing != null) deleteMarketImage(request, existing.getImageUrl());
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"Market deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Market not found or in use.\"}");
                }
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                Market existing = marketDAO.findById(marketId);
                if (imageUrl == null && existing != null) imageUrl = existing.getImageUrl();
                Market m = new Market(marketId, name, district, province, desc, null, imageUrl);
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
            Market m = new Market(0, name, district, province != null ? province : "Western Province", desc, null, imageUrl);
            int newId = marketDAO.create(m);
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Market created successfully.");
            resp.put("marketId", newId);
            out.print(gson.toJson(resp));

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"A market with this name already exists. Choose a different name.\"}");
                return;
            }
            if (e.getErrorCode() == 1451 || "23000".equals(e.getSQLState())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"This market is linked to saved price reports, so it cannot be deleted. The historical reports have been preserved.\"}");
                return;
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            getServletContext().log("Market operation failed", e);
            out.print("{\"success\":false,\"message\":\"We could not save this market change. Please try again.\"}");
        } catch (IllegalStateException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Image upload is too large. Maximum size is 2 MB.\"}");
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Upload a valid JPEG, PNG or GIF image under 2 MB.\"}");
        }
    }

    private String saveMarketImage(HttpServletRequest request, Part part) throws IOException {
        if (part.getSize() > 2097152) throw new IOException("Image exceeds size limit");
        byte[] header = new byte[8];
        try (InputStream input = part.getInputStream()) {
            if (input.read(header) < 3) throw new IOException("Invalid image");
        }
        String extension;
        if ((header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) extension = "jpg";
        else if ((header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47) extension = "png";
        else if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F') extension = "gif";
        else throw new IOException("Unsupported image format");

        String relative = "assets/images/markets/" + UUID.randomUUID().toString() + "." + extension;
        String root = getServletContext().getRealPath("/");
        if (root == null) throw new IOException("Market image storage is unavailable");
        Path target = Paths.get(root, relative.replace('/', java.io.File.separatorChar));
        Files.createDirectories(target.getParent());
        try (InputStream input = part.getInputStream()) { Files.copy(input, target); }
        return relative;
    }

    private void deleteMarketImage(HttpServletRequest request, String imageUrl) {
        if (imageUrl == null || !imageUrl.matches("assets/images/markets/[a-zA-Z0-9_-]+\\.(jpg|png|gif)")) return;
        String root = getServletContext().getRealPath("/");
        if (root == null) return;
        Path base = Paths.get(root, "assets", "images", "markets").normalize();
        Path image = Paths.get(root, imageUrl.replace('/', java.io.File.separatorChar)).normalize();
        if (image.startsWith(base)) {
            try { Files.deleteIfExists(image); } catch (IOException ignored) { }
        }
    }
}
