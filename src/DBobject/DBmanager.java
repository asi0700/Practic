package DBobject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;


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
        )
        """,
        
        """
        CREATE TABLE IF NOT EXISTS Order_Items (
            order_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            PRIMARY KEY (order_id, product_id),
            FOREIGN KEY (order_id) REFERENCES Orders(order_id),
            FOREIGN KEY (product_id) REFERENCES Products(product_id)
        )
        """
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                System.out.println("Выполняется SQL: " + sql);
                stmt.execute(sql);
            }
            System.out.println("Все таблицы успешно созданы.");
            
            // Проверяем существование таблиц
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
            System.out.println("Существующие таблицы:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при создании таблиц: " + e.getMessage());
            throw e;
        }
    }

    public static void recreateOrdersTable() throws SQLException {
        String[] sqlStatements = {
            "DROP TABLE IF EXISTS Order_Items;",
            "DROP TABLE IF EXISTS Orders;",
            """
            CREATE TABLE Orders (
                order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id INTEGER NOT NULL,
                order_date DATETIME NOT NULL,
                total_cost REAL NOT NULL DEFAULT 0.0,
                status TEXT NOT NULL,
                last_updated DATETIME NOT NULL,
                modified_by INTEGER,
                FOREIGN KEY (client_id) REFERENCES Users(user_id),
                FOREIGN KEY (modified_by) REFERENCES Users(user_id)
            );
            """,
            """
            CREATE TABLE Order_Items (
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
            System.out.println("Таблица Orders успешно пересоздана.");
        } catch (SQLException e) {
            System.err.println("Ошибка при пересоздании таблицы Orders: " + e.getMessage());
            throw e;
        }
    }

    public static boolean checkOrdersTable() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Orders'");
            if (rs.next()) {
                System.out.println("Таблица Orders существует");
                // Проверяем структуру таблицы
                rs = stmt.executeQuery("PRAGMA table_info(Orders)");
                System.out.println("Структура таблицы Orders:");
                while (rs.next()) {
                    System.out.println(rs.getString("name") + " - " + rs.getString("type"));
                }
                return true;
            } else {
                System.out.println("Таблица Orders не существует");
                return false;
            }
        }
    }
}
