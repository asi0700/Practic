package Dao_db;

import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;

public class ProductDAO implements AutoCloseable {
    private final Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO Products (name, description, quantity, price, added_by, added_date, modified_by, modified_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setInt(3, product.getQuantity());
            stmt.setDouble(4, product.getPrice());
            stmt.setInt(5, product.getAdded_by());
            stmt.setString(6, product.getAdded_date());
            stmt.setInt(7, product.getModified_by());
            stmt.setString(8, product.getModified_date());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                product.setProduct_id(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении товара: " + e.getMessage());
            throw e;
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE Products SET name = ?, description = ?, quantity = ?, price = ?, supplier = ?, added_by = ?, added_date = ?, modified_by = ?, modified_date = ? WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setInt(3, product.getQuantity());
            stmt.setDouble(4, product.getPrice());
            stmt.setString(5, product.getSupplier());
            stmt.setInt(6, product.getAdded_by());
            stmt.setString(7, product.getAdded_date());
            stmt.setInt(8, product.getModified_by());
            stmt.setString(9, product.getModified_date());
            stmt.setInt(10, product.getProduct_id());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении товара: " + e.getMessage());
            throw e;
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM Products WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении товара: " + e.getMessage());
            throw e;
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("supplier"),
                        rs.getInt("added_by"),
                        rs.getString("added_date"),
                        rs.getInt("modified_by"),
                        rs.getString("modified_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении товаров: " + e.getMessage());
            throw e;
        }
        return products;
    }

    public List<Product> getProductsByFilters(String name, String startDate, String endDate, String addedBy, String minQuantity, String maxQuantity, String supplier) throws SQLException {
        String sql = "SELECT * FROM Products WHERE 1=1";
        if (name != null && !name.isEmpty()) sql += " AND name LIKE ?";
        if (startDate != null && !startDate.isEmpty()) sql += " AND added_date >= ?";
        if (endDate != null && !endDate.isEmpty()) sql += " AND added_date <= ?";
        if (addedBy != null && !addedBy.isEmpty()) sql += " AND added_by IN (SELECT user_id FROM Users WHERE name LIKE ?)";
        if (minQuantity != null && !minQuantity.isEmpty()) sql += " AND quantity >= ?";
        if (maxQuantity != null && !maxQuantity.isEmpty()) sql += " AND quantity <= ?";
        if (supplier != null && !supplier.isEmpty()) sql += " AND supplier LIKE ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (name != null && !name.isEmpty()) stmt.setString(paramIndex++, "%" + name + "%");
            if (startDate != null && !startDate.isEmpty()) stmt.setString(paramIndex++, startDate);
            if (endDate != null && !endDate.isEmpty()) stmt.setString(paramIndex++, endDate);
            if (addedBy != null && !addedBy.isEmpty()) stmt.setString(paramIndex++, "%" + addedBy + "%");
            if (minQuantity != null && !minQuantity.isEmpty()) stmt.setInt(paramIndex++, Integer.parseInt(minQuantity));
            if (maxQuantity != null && !maxQuantity.isEmpty()) stmt.setInt(paramIndex++, Integer.parseInt(maxQuantity));
            if (supplier != null && !supplier.isEmpty()) stmt.setString(paramIndex++, "%" + supplier + "%");

            ResultSet rs = stmt.executeQuery();
            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("supplier"),
                        rs.getInt("added_by"),
                        rs.getString("added_date"),
                        rs.getInt("modified_by"),
                        rs.getString("modified_date")
                ));
            }
            return products;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Соединение закрыто в ProductDAO");
        }
    }
}
