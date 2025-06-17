package Dao_db;

import DBobject.DBmanager;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO implements AutoCloseable {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    public UserDAO() {
        try {
            this.connection = DBmanager.getConnection();
        } catch (SQLException e) {
            System.err.println("Ошибка при создании UserDAO: " + e.getMessage());
        }
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("registration_date"),
                        rs.getString("photo_path"),
                        rs.getBytes("photo")
                    );
                }
            }
        }
        return null;
    }

    public void updateUserPhotoPath(int userId, String photoPath) throws SQLException {
        String sql = "UPDATE Users SET photo_path = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, photoPath);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void updateUserPhoto(int userId, byte[] photo) throws SQLException {
        String sql = "UPDATE Users SET photo = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBytes(1, photo);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void setPhotoRequest(int userId, boolean request) throws SQLException {
        String sql = "UPDATE Users SET photo_request = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, request);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public boolean isPhotoRequest(int userId) throws SQLException {
        String sql = "SELECT photo_request FROM Users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("photo_request");
                }
            }
        }
        return false;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
} 