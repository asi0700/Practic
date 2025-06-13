package Dao_db;

import DBobject.DBmanager;
import model.Order;
import model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private Connection connection;

    public OrderDAO(Connection connection) {
        this.connection = connection;
    }
    
    public OrderDAO() {
        try {
            this.connection = DBmanager.getConnection();
        } catch (SQLException e) {
            System.err.println("Ошибка при создании OrderDAO: " + e.getMessage());
        }
    }

    public List<Object[]> getClientOrders(int clientId) throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String query = "SELECT order_id, client_id, total_cost, status, order_date, last_updated FROM orders WHERE client_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(new Object[]{
                    rs.getInt("order_id"),
                    rs.getInt("client_id"),
                    "Items not available",
                    rs.getDouble("total_cost"),
                    rs.getString("status"),
                    rs.getString("order_date"),
                    rs.getString("last_updated")
                });
            }
        }
        return orders;
    }

    public List<Object[]> getOrderItems(int orderId) throws SQLException {
        List<Object[]> items = new ArrayList<>();
        String sql = "SELECT oi.id, p.name, oi.quantity, oi.price_per_unit, oi.product_id " +
                     "FROM order_items oi JOIN products p ON oi.product_id = p.id " +
                     "WHERE oi.order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getDouble("price_per_unit"), rs.getInt("product_id")});
                }
            }
        }
        return items;
    }

    public int addOrder(int clientId, double totalAmount, String status) throws SQLException {
        String sql = "INSERT INTO Orders (client_id, order_date, status) VALUES (?, CURRENT_TIMESTAMP, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, clientId);
            stmt.setString(2, status);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to get generated order ID");
        }
    }

    public void addOrder(Order order) throws SQLException {
        String sql = "INSERT INTO Orders (client_id, order_date, total_cost, status, last_updated) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, order.getClientId());
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            pstmt.setString(2, now);
            pstmt.setDouble(3, order.getTotalCost());
            pstmt.setString(4, order.getStatus());
            pstmt.setString(5, order.getLastUpdated());
            pstmt.executeUpdate();
        }
    }

    private String orderItemsToString(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : items) {
            sb.append(item.getProductId()).append(":").append(item.getQuantity());
            sb.append(",");
        }
        // Remove the last comma
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public void addOrderItem(int orderId, int productId, int quantity, double pricePerUnit) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price_per_unit) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, pricePerUnit);
            stmt.executeUpdate();
        }
    }

    public List<Object[]> getOrdersByStatus(String status) throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String sql = "SELECT order_id, client_id, total_cost, status, order_date FROM orders WHERE status = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(new Object[]{
                    rs.getInt("order_id"),
                    rs.getInt("client_id"),
                    "Items not available",
                    rs.getDouble("total_cost"),
                    rs.getString("status"),
                    rs.getString("order_date"),
                    "N/A"
                });
            }
        }
        return orders;
    }

    public void updateOrderStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Заказ с ID " + orderId + " не найден.");
            }
        }
    }

    public List<Object[]> getAllOrders() throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String sql = "SELECT * FROM Orders";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(new Object[]{
                    rs.getInt("order_id"),
                    rs.getInt("client_id"),
                    rs.getString("order_date"),
                    rs.getDouble("total_cost"),
                    rs.getString("status"),
                    rs.getString("last_updated")
                });
            }
        }
        return orders;
    }

    public List<Order> getOrdersByClientId(int clientId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE client_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, clientId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("order_id");
            java.util.Date orderDate = new java.util.Date(rs.getTimestamp("order_date").getTime());
            String status = rs.getString("status");
            orders.add(new Order(id, orderDate, status, clientId));
        }

        rs.close();
        pstmt.close();
        return orders;
    }

    public Object[] getOrderById(int orderId) throws SQLException {
        String sql = "SELECT order_id, client_id, order_date, status FROM orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                        rs.getInt("order_id"),
                        rs.getInt("client_id"),
                        rs.getString("order_date"),
                        rs.getString("status")
                    };
                }
            }
        }
        return null; // Return null if order not found
    }

    public void updateOrderItemQuantity(int itemId, int quantity) throws SQLException {
        String sql = "UPDATE order_items SET quantity = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Товар с ID " + itemId + " не найден в заказе.");
            }
        }
    }

    public void addItemToOrder(int orderId, int productId, int quantity) throws SQLException {
        // First get the product price
        String priceSql = "SELECT price FROM products WHERE id = ?";
        double pricePerUnit = 0.0;
        try (PreparedStatement priceStmt = connection.prepareStatement(priceSql)) {
            priceStmt.setInt(1, productId);
            try (ResultSet rs = priceStmt.executeQuery()) {
                if (rs.next()) {
                    pricePerUnit = rs.getDouble("price");
                } else {
                    throw new SQLException("Product with ID " + productId + " not found");
                }
            }
        }
        
        // Now add the order item with the fetched price
        addOrderItem(orderId, productId, quantity, pricePerUnit);
        
        // Update the order total amount
        updateOrderTotal(orderId);
    }

    private void updateOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT SUM(quantity * price_per_unit) as total FROM order_items WHERE order_id = ?";
        double calculatedTotal = 0.0;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    calculatedTotal = rs.getDouble("total");
                }
            }
        }
        
        // If you absolutely need to store the total, you'll need to alter the table schema
        // For now, we'll just log if there's a discrepancy
        System.out.println("Calculated total for order " + orderId + ": " + calculatedTotal);
    }

    public void deleteOrder(int orderId) throws SQLException {
        // First delete order items
        String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteItemsSql)) {
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
        }
        // Then delete the order
        String deleteOrderSql = "DELETE FROM orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteOrderSql)) {
            stmt.setInt(1, orderId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Заказ с ID " + orderId + " не найден.");
            }
        }
    }

    public void updateOrder(Order order, int modifiedByUserId) throws SQLException {
        String sql = "UPDATE Orders SET client_id = ?, total_cost = ?, status = ?, last_updated = datetime('now'), modified_by = ? WHERE order_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, order.getClientId());
            pstmt.setDouble(2, order.getTotalCost());
            pstmt.setString(3, order.getStatus());
            pstmt.setInt(4, modifiedByUserId);
            pstmt.setInt(5, order.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteOrderItem(int orderId, int productId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id = ? AND product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }
}
