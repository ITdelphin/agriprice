package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO performing full CRUD operations on the 'products' table.
 */
public class ProductDAO {

    // 1. CREATE
    public int create(Product product) throws SQLException {
        String sql = "INSERT INTO products (product_name, category, default_unit, description, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getProductName().trim());
            ps.setString(2, product.getCategory().trim());
            ps.setString(3, product.getDefaultUnit().trim());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getStatus() != null ? product.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    product.setProductId(gk.getInt(1));
                    return product.getProductId();
                }
            }
        }
        return -1;
    }

    // 2. READ (Single)
    public Product findById(int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, category, default_unit, description, status "
                   + "FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // 2. READ (All)
    public List<Product> getAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT product_id, product_name, category, default_unit, description, status "
                   + "FROM products ORDER BY product_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // 3. UPDATE
    public boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET product_name = ?, category = ?, default_unit = ?, description = ?, status = ? "
                   + "WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName().trim());
            ps.setString(2, product.getCategory().trim());
            ps.setString(3, product.getDefaultUnit().trim());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getStatus());
            ps.setInt(6, product.getProductId());
            return ps.executeUpdate() > 0;
        }
    }

    // 4. DELETE
    public boolean delete(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setCategory(rs.getString("category"));
        p.setDefaultUnit(rs.getString("default_unit"));
        p.setDescription(rs.getString("description"));
        p.setStatus(rs.getString("status"));
        return p;
    }
}
