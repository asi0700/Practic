package DBobject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DBmanager  {
    private static final String URL = "jdbc:sqlite:sklad.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() throws SQLException {
        String[] sqlStatements = {
        """
        CREATE TABLE IF NOT EXISTS Users (
            user_id INTEGER PRIMARY KEY AUTOINCREMENT, 
            username TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            role TEXT NOT NULL,
            name TEXT NOT NULL,
            phone TEXT,
            address TEXT,
            registration_date TEXT NOT NULL
        )
        """,

        """
        CREATE TABLE IF NOT EXISTS Products (
            product_id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            description TEXT,
            quantity INTEGER NOT NULL DEFAULT 0,
            price REAL NOT NULL,
            supplier TEXT,
            added_by INTEGER,
            added_date TEXT,
            modified_by INTEGER,
            modified_date TEXT,
            FOREIGN KEY (added_by) REFERENCES Users(user_id) ON DELETE SET NULL,
            FOREIGN KEY (modified_by) REFERENCES Users(user_id) ON DELETE SET NULL
        )
        """,

        """
        CREATE TABLE IF NOT EXISTS Orders (
            order_id INTEGER PRIMARY KEY AUTOINCREMENT,
            client_id INTEGER NOT NULL,
            order_date DATETIME NOT NULL,
            total_cost REAL NOT NULL DEFAULT 0.0,
            status TEXT NOT NULL,
            last_updated DATETIME NOT NULL,
            FOREIGN KEY (client_id) REFERENCES Users(user_id)
        );
        
        CREATE TABLE IF NOT EXISTS Order_Items (
            order_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            PRIMARY KEY (order_id, product_id),
            FOREIGN KEY (order_id) REFERENCES Orders(order_id),
            FOREIGN KEY (product_id) REFERENCES Products(product_id)
        );
        """
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                stmt.execute(sql);
            }
            System.out.println("Все таблицы успешно созданы.");
        } catch (SQLException e) {
            System.err.println("Ошибка при создании таблиц: " + e.getMessage());
            throw e;
        }
    }
}
