package Dao_db;

import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ProductDAO implements AutoCloseable {
    private final Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO Products (name, description, price, quantity, supplier, added_by, added_date, modified_by, modified_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getQuantity());
            pstmt.setString(5, product.getSupplier());
            pstmt.setInt(6, product.getAdded_by());
            pstmt.setTimestamp(7, new Timestamp(product.getAdded_date().getTime()));
            pstmt.setInt(8, product.getModified_by());
            pstmt.setTimestamp(9, new Timestamp(product.getModified_date().getTime()));
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE Products SET name = ?, description = ?, price = ?, quantity = ?, supplier = ?, modified_by = ?, modified_date = ? WHERE product_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getQuantity());
            pstmt.setString(5, product.getSupplier());
            pstmt.setInt(6, product.getModified_by());
            pstmt.setTimestamp(7, new Timestamp(product.getModified_date().getTime()));
            pstmt.setInt(8, product.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM Products WHERE product_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        }
    }

    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT * FROM Products WHERE product_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getInt("product_id"));
                    product.setName(rs.getString("name"));
                    product.setDescription(rs.getString("description"));
                    product.setPrice(rs.getDouble("price"));
                    product.setQuantity(rs.getInt("quantity"));
                    product.setSupplier(rs.getString("supplier"));
                    product.setAdded_by(rs.getInt("added_by"));
                    product.setModified_by(rs.getInt("modified_by"));
                    
                    Timestamp addedDate = rs.getTimestamp("added_date");
                    Timestamp modifiedDate = rs.getTimestamp("modified_date");
                    product.setAdded_date(addedDate != null ? new Date(addedDate.getTime()) : new Date());
                    product.setModified_date(modifiedDate != null ? new Date(modifiedDate.getTime()) : new Date());
                    
                    return product;
                }
            }
        }
        return null;
    }

    public List<Object[]> getAllProducts() throws SQLException {
        List<Object[]> products = new ArrayList<>();
        String sql = """
            SELECT p.product_id, p.name, p.description, p.quantity, p.price, 
                   p.supplier, p.added_date, u.name as added_by_name
            FROM Products p
            LEFT JOIN Users u ON p.added_by = u.user_id
            ORDER BY p.name ASC
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                products.add(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getString("supplier"),
                    rs.getString("added_date"),
                    rs.getString("added_by_name")
                });
            }
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
                        rs.getDouble("price"),
                        rs.getString("description"),
                        0, // Placeholder for categoryId
                        rs.getInt("quantity"),
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


    public List<Product> getLowStockProducts(int threshold) throws SQLException {
        List<Product> lowStockProducts = new ArrayList<>();
        String sql = "SELECT product_id, name, quantity, price, supplier FROM Products WHERE quantity < ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        "", // Placeholder for description as it's not in the result set
                        rs.getString("supplier")
                );
                lowStockProducts.add(p);
            }
        }
        return lowStockProducts;
    }


    public void updateProductQuantity(int productId, int newQuantity) throws SQLException {
        String sql = "UPDATE Products SET quantity = ? WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
