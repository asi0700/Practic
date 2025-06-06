package Dao_db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private Connection connection;

    public OrderDAO(Connection connection){
        this.connection = connection;
    }

    public List<Object[]> getClientOrders(int clientId) throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String sql = "SELECT order_id, order_date, status FROM Orders WHERE client_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] order = new Object[3];
                order[0] = rs.getInt("order_id");
                order[1] = rs.getString("order_date");
                order[2] = rs.getString("status");
                orders.add(order);
            }
        }
        return orders;
    }

    public List<Object[]> getOrderItems(int orderId) throws SQLException {
        List<Object[]> items = new ArrayList<>();
        String sql = "SELECT p.name, oi.quantity, oi.unit_price " +
                "FROM Order_Items oi " +
                "JOIN Products p ON oi.product_id = p.product_id " +
                "WHERE oi.order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] item = new Object[3];
                item[0] = rs.getString("name");
                item[1] = rs.getInt("quantity");
                item[2] = rs.getDouble("unit_price");
                items.add(item);
            }
        }
        return items;
    }

    public int addOrder(int clientId, double totalCost, String status) throws SQLException {
        String sql = "INSERT INTO Orders (client_id, order_date, total_cost, status, last_updated) " +
                "VALUES (?, DATETIME('now'), ?, ?, DATETIME('now'))";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, clientId);
            stmt.setDouble(2, totalCost);
            stmt.setString(3, status);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // Возвращаем сгенерированный order_id
            }
            throw new SQLException("Не удалось получить ID нового заказа");
        }
    }

    public void addOrderItem(int orderId, int productId, int quantity, double unitPrice) throws SQLException {
        String sql = "INSERT INTO Order_Items (order_id, product_id, quantity, unit_price) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, unitPrice);
            stmt.executeUpdate();
        }
    }


    public List<Object[]> getOrdersByStatus(String status) throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, u.name, o.order_date, o.status " +
                "FROM Orders o " +
                "JOIN Users u ON o.client_id = u.user_id " +
                "WHERE o.status = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] order = new Object[4];
                order[0] = rs.getInt("order_id");
                order[1] = rs.getString("name");
                order[2] = rs.getString("order_date");
                order[3] = rs.getString("status");
                orders.add(order);
            }
        }
        return orders;
    }

    public void updateOrderStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE Orders SET status = ?, last_updated = DATETIME('now') WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }

    public List<Object[]> getAllOrders() throws SQLException {
        List<Object[]> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, u.name, o.order_date, o.status, o.total_cost " +
                "FROM Orders o JOIN Users u ON o.client_id = u.user_id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] order = new Object[5];
                order[0] = rs.getInt("order_id");
                order[1] = rs.getString("name");
                order[2] = rs.getString("order_date");
                order[3] = rs.getString("status");
                order[4] = rs.getDouble("total_cost");
                orders.add(order);
            }
        }
        return orders;
    }


}


