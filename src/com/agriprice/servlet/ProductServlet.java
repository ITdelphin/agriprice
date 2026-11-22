package com.agriprice.servlet;

import com.agriprice.dao.ProductDAO;
import com.agriprice.model.Product;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@MultipartConfig(maxFileSize = 2097152, maxRequestSize = 3145728)
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
            out.print("{\"success\":false,\"message\":\"Products are temporarily unavailable. Please try again.\"}");
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
            imageUrl = request.getParameter("imageUrl");
        }

        if (!"delete".equalsIgnoreCase(action)
                && (name == null || name.trim().isEmpty() || category == null || category.trim().isEmpty())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Product name and category are required.\"}");
            return;
        }
        if (!"kg".equals(unit) && !"piece".equals(unit) && !"sack".equals(unit) && !"bunch".equals(unit)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Choose a supported product unit.\"}");
            return;
        }

        try {
            if (!"delete".equalsIgnoreCase(action) && productDAO.existsByName(name, productId)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"A product with this name already exists. Choose a different name or edit the existing product.\"}");
                return;
            }
            Part imagePart = contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")
                    ? request.getPart("image") : null;
            if (imagePart != null && imagePart.getSize() > 0) {
                imageUrl = saveProductImage(request, imagePart);
            }
            if (imageUrl != null && !imageUrl.matches("assets/images/products/[a-f0-9-]+\\.(jpg|png|gif)")) {
                imageUrl = null;
            }

            if ("delete".equalsIgnoreCase(action)) {
                Product productToDelete = productDAO.findById(productId);
                if (productToDelete == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Product not found. Refresh the list and try again.\"}");
                    return;
                }
                if (productDAO.hasPriceReports(productId)) {
                    productDAO.archive(productId);
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\":true,\"message\":\"This product has saved price reports, so it was archived instead of deleted. Historical reports are preserved.\"}");
                    return;
                }
                boolean deleted = productDAO.delete(productId);
                if (deleted) {
                    if (productToDelete != null) deleteProductImage(request, productToDelete.getImageUrl());
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
                if (imageUrl == null) {
                    Product existing = productDAO.findById(productId);
                    if (existing != null) imageUrl = existing.getImageUrl();
                }
                p.setImageUrl(imageUrl);
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
            Product p = new Product(0, name, category, unit != null ? unit : "kg", desc, status);
            p.setImageUrl(imageUrl);
            int newId = productDAO.create(p);
            response.setStatus(HttpServletResponse.SC_CREATED);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Product created successfully.");
            resp.put("productId", newId);
            out.print(gson.toJson(resp));

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"A product with this name already exists. Choose a different name or edit the existing product.\"}");
                return;
            }
            if ((e.getErrorCode() == 1451 || "23000".equals(e.getSQLState())) && "delete".equalsIgnoreCase(action)) {
                try {
                    if (productDAO.archive(productId)) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        out.print("{\"success\":true,\"message\":\"This product is linked to saved market data, so it was archived instead of deleted. Historical data is preserved.\"}");
                        return;
                    }
                } catch (SQLException archiveError) {
                    getServletContext().log("Could not archive product " + productId, archiveError);
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            getServletContext().log("Product operation failed", e);
            out.print("{\"success\":false,\"message\":\"We could not save this product change. Please try again.\"}");
        } catch (IllegalStateException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Image upload is too large. Maximum size is 2 MB.\"}");
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Upload a valid JPEG, PNG or GIF image under 2 MB.\"}");
        }
    }

    private String saveProductImage(HttpServletRequest request, Part part) throws IOException {
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

        String relative = "assets/images/products/" + UUID.randomUUID().toString() + "." + extension;
        String root = getServletContext().getRealPath("/");
        if (root == null) throw new IOException("Product image storage is unavailable");
        Path target = Paths.get(root, relative.replace('/', java.io.File.separatorChar));
        Files.createDirectories(target.getParent());
        try (InputStream input = part.getInputStream()) { Files.copy(input, target); }
        return relative;
    }

    private void deleteProductImage(HttpServletRequest request, String imageUrl) {
        if (imageUrl == null || !imageUrl.matches("assets/images/products/[a-f0-9-]+\\.(jpg|png|gif)")) return;
        String root = getServletContext().getRealPath("/");
        if (root == null) return;
        Path base = Paths.get(root, "assets", "images", "products").normalize();
        Path image = Paths.get(root, imageUrl.replace('/', java.io.File.separatorChar)).normalize();
        if (image.startsWith(base)) {
            try { Files.deleteIfExists(image); } catch (IOException ignored) { /* Unused media may be cleaned up later. */ }
        }
    }
}
