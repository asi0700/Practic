package Dao_db;

import DBobject.DBmanager;
import model.Order;
import model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO implements AutoCloseable {
    private Connection connection;
    private OrderItemDAO orderItemDAO;

    public OrderDAO(Connection connection) {
        this.connection = connection;
        this.orderItemDAO = new OrderItemDAO(connection);
    }
    
    public OrderDAO() {
        try {
            this.connection = DBmanager.getConnection();
            this.orderItemDAO = new OrderItemDAO(connection);
        } catch (SQLException e) {
            System.err.println("Ошибка при создании OrderDAO: " + e.getMessage());
        }
    }

    public List<Order> getClientOrders(int clientId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM Orders WHERE client_id = ? ORDER BY order_date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("order_id"));
                    order.setClientId(rs.getInt("client_id"));
                    order.setOrderDate(rs.getTimestamp("order_date"));
                    order.setTotalCost(rs.getDouble("total_cost"));
                    order.setStatus(rs.getString("status"));
                    order.setLastUpdated(rs.getTimestamp("last_updated"));
                    order.setDeliveryCity(rs.getString("delivery_city"));
                    order.setDeliveryAddress(rs.getString("delivery_address"));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        return orderItemDAO.getOrderItems(orderId);
    }

    public void addOrder(Order order) throws SQLException {
        String sql = "INSERT INTO Orders (client_id, order_date, total_cost, status, last_updated, delivery_city, delivery_address) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getClientId());
            pstmt.setTimestamp(2, new Timestamp(order.getOrderDate().getTime()));
            pstmt.setDouble(3, order.getTotalCost());
            pstmt.setString(4, order.getStatus());
            pstmt.setTimestamp(5, new Timestamp(order.getLastUpdated().getTime()));
            pstmt.setString(6, order.getDeliveryCity());
            pstmt.setString(7, order.getDeliveryAddress());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Создание заказа не удалось, ни одна строка не была добавлена.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setOrderId(generatedKeys.getInt(1));
                    System.out.println("[DEBUG] Новый orderId: " + order.getOrderId());
                } else {
                    throw new SQLException("Создание заказа не удалось, не получен ID.");
                }
            }
        }
    }

    public void addOrderItem(int orderId, int productId, int quantity, double pricePerUnit) throws SQLException {
        String sql = "INSERT INTO Order_Items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, pricePerUnit);
            stmt.executeUpdate();
            updateOrderTotal(orderId);
            

            String updateProductSql = "UPDATE Products SET quantity = quantity - ? WHERE product_id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateProductSql)) {
                updateStmt.setInt(1, quantity);
                updateStmt.setInt(2, productId);
                updateStmt.executeUpdate();
            }
        }
    }

    public List<Order> getOrdersByStatus(String status) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM Orders WHERE status = ? ORDER BY order_date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("order_id"));
                    order.setClientId(rs.getInt("client_id"));
                    order.setOrderDate(rs.getTimestamp("order_date"));
                    order.setTotalCost(rs.getDouble("total_cost"));
                    order.setStatus(rs.getString("status"));
                    order.setLastUpdated(rs.getTimestamp("last_updated"));
                    order.setDeliveryCity(rs.getString("delivery_city"));
                    order.setDeliveryAddress(rs.getString("delivery_address"));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public void updateOrderStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE Orders SET status = ?, last_updated = CURRENT_TIMESTAMP WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Заказ с ID " + orderId + " не найден.");
            }
        }
    }

    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM Orders ORDER BY order_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getInt("order_id"));
                order.setClientId(rs.getInt("client_id"));
                order.setOrderDate(rs.getTimestamp("order_date"));
                order.setTotalCost(rs.getDouble("total_cost"));
                order.setStatus(rs.getString("status"));
                order.setLastUpdated(rs.getTimestamp("last_updated"));
                order.setDeliveryCity(rs.getString("delivery_city"));
                order.setDeliveryAddress(rs.getString("delivery_address"));
                orders.add(order);
            }
        }
        return orders;
    }

    public Order getOrderById(int orderId) throws SQLException {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("order_id"));
                    order.setClientId(rs.getInt("client_id"));
                    order.setOrderDate(rs.getTimestamp("order_date"));
                    order.setTotalCost(rs.getDouble("total_cost"));
                    order.setStatus(rs.getString("status"));
                    order.setLastUpdated(rs.getTimestamp("last_updated"));
                    order.setDeliveryCity(rs.getString("delivery_city"));
                    order.setDeliveryAddress(rs.getString("delivery_address"));
                    return order;
                }
            }
        }
        return null;
    }

    public void updateOrderItemQuantity(int itemId, int quantity) throws SQLException {
        String sql = "UPDATE Order_Items SET quantity = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Товар с ID " + itemId + " не найден в заказе.");
            }
            

            String orderIdSql = "SELECT order_id FROM Order_Items WHERE id = ?";
            try (PreparedStatement orderStmt = connection.prepareStatement(orderIdSql)) {
                orderStmt.setInt(1, itemId);
                try (ResultSet rs = orderStmt.executeQuery()) {
                    if (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        updateOrderTotal(orderId);
                    }
                }
            }
        }
    }

    public void addItemToOrder(int orderId, int productId, int quantity) throws SQLException {
        String priceSql = "SELECT price FROM Products WHERE product_id = ?";
        double pricePerUnit = 0.0;
        try (PreparedStatement priceStmt = connection.prepareStatement(priceSql)) {
            priceStmt.setInt(1, productId);
            try (ResultSet rs = priceStmt.executeQuery()) {
                if (rs.next()) {
                    pricePerUnit = rs.getDouble("price");
                } else {
                    throw new SQLException("Товар с ID " + productId + " не найден");
                }
            }
        }
        addOrderItem(orderId, productId, quantity, pricePerUnit);
    }

    private void updateOrderTotal(int orderId) throws SQLException {
        String sql = "UPDATE Orders o SET total_cost = (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM Order_Items WHERE order_id = ?) WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }

    public void deleteOrder(int orderId) throws SQLException {

        String deleteItemsSql = "DELETE FROM Order_Items WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteItemsSql)) {
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
        }
        

        String deleteOrderSql = "DELETE FROM Orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteOrderSql)) {
            stmt.setInt(1, orderId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Заказ с ID " + orderId + " не найден.");
            }
        }
    }

    public void updateOrder(Order order) throws SQLException {
        String sql = "UPDATE Orders SET client_id = ?, order_date = ?, total_cost = ?, status = ?, last_updated = ?, delivery_city = ?, delivery_address = ? WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, order.getClientId());
            stmt.setTimestamp(2, new Timestamp(order.getOrderDate().getTime()));
            stmt.setDouble(3, order.getTotalCost());
            stmt.setString(4, order.getStatus());
            stmt.setTimestamp(5, new Timestamp(order.getLastUpdated().getTime()));
            stmt.setString(6, order.getDeliveryCity());
            stmt.setString(7, order.getDeliveryAddress());
            stmt.setInt(8, order.getOrderId());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Заказ с ID " + order.getOrderId() + " не найден.");
            }
        }
    }

    public void deleteOrderItem(int orderId, int productId) throws SQLException {
        String sql = "DELETE FROM Order_Items WHERE order_id = ? AND product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Товар с ID " + productId + " не найден в заказе " + orderId);
            }
            updateOrderTotal(orderId);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
