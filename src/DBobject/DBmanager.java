package DBobject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBmanager {

    private static final String URL = "jdbc:mysql://sql8.freesqldatabase.com:3306/sql8784448?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&connectionCollation=utf8_unicode_ci";
    private static final String USER = "sql8784448";
    private static final String PASSWORD = "xDpyDqeiKQ";

    static {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL драйвер успешно загружен");
            

            try (Connection testConn = getConnection()) {
                System.out.println("Успешное подключение к базе данных MySQL");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки MySQL драйвера: " + e.getMessage());
            throw new RuntimeException("MySQL драйвер не найден", e);
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных MySQL: " + e.getMessage());
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET NAMES utf8mb4");
                stmt.execute("SET CHARACTER SET utf8mb4");
                stmt.execute("SET character_set_connection=utf8mb4");
                stmt.execute("SET collation_connection=utf8mb4_unicode_ci");
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("Ошибка при создании соединения с базой данных: " + e.getMessage());
            throw e;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {

            boolean usersTableExists = tableExists(conn, "Users");
            boolean productsTableExists = tableExists(conn, "Products");
            boolean ordersTableExists = tableExists(conn, "Orders");
            boolean orderItemsTableExists = tableExists(conn, "Order_Items");
            boolean logsTableExists = tableExists(conn, "Logs");

            if (!usersTableExists) {
                String createUsersTable = "CREATE TABLE Users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "address VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci, " +
                    "registration_date DATETIME NOT NULL, " +
                    "photo_path VARCHAR(255), " +
                    "photo LONGBLOB, " +
                    "photo_request BOOLEAN DEFAULT FALSE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci";
                executeSQL(conn, createUsersTable);
                System.out.println("Таблица Users создана");
            }

            if (!productsTableExists) {
                String createProductsTable = "CREATE TABLE Products (" +
                    "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "description TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci, " +
                    "quantity INT NOT NULL DEFAULT 0, " +
                    "price DECIMAL(10,2) NOT NULL, " +
                    "supplier VARCHAR(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci, " +
                    "added_by INT, " +
                    "added_date DATETIME, " +
                    "modified_by INT, " +
                    "modified_date DATETIME, " +
                    "FOREIGN KEY (added_by) REFERENCES Users(user_id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (modified_by) REFERENCES Users(user_id) ON DELETE SET NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci";
                executeSQL(conn, createProductsTable);
                System.out.println("Таблица Products создана");
            }

            if (!ordersTableExists || !orderItemsTableExists) {
                recreateOrdersTable();
            }

            if (!logsTableExists) {
                String createLogsTable = "CREATE TABLE Logs (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "action VARCHAR(255) NOT NULL, " +
                    "timestamp DATETIME NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci";
                executeSQL(conn, createLogsTable);
                System.out.println("Таблица Logs создана");
            }

            System.out.println("Все таблицы успешно проверены и созданы при необходимости.");
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Ошибка инициализации базы данных", e);
        }
    }

    public static void recreateOrdersTable() throws SQLException {
        try (Connection conn = getConnection()) {
            // Удаляю табл так как надо заново пересоздать!!!!!!
//            executeSQL(conn, "DROP TABLE IF EXISTS Order_Items");
//            executeSQL(conn, "DROP TABLE IF EXISTS Orders");


            String createOrdersTable = "CREATE TABLE Orders (" +
                "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "client_id INT NOT NULL, " +
                "order_date DATETIME NOT NULL, " +
                "total_cost DECIMAL(10,2) NOT NULL DEFAULT 0.0, " +
                "status VARCHAR(20) NOT NULL, " +
                "last_updated DATETIME NOT NULL, " +
                "delivery_city VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                "delivery_address VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                "modified_by INT, " +
                "FOREIGN KEY (client_id) REFERENCES Users(user_id), " +
                "FOREIGN KEY (modified_by) REFERENCES Users(user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            executeSQL(conn, createOrdersTable);


            String createOrderItemsTable = "CREATE TABLE Order_Items (" +
                "order_id INT NOT NULL, " +
                "product_id INT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "unit_price DECIMAL(10,2) NOT NULL, " +
                "PRIMARY KEY (order_id, product_id), " +
                "FOREIGN KEY (order_id) REFERENCES Orders(order_id), " +
                "FOREIGN KEY (product_id) REFERENCES Products(product_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            executeSQL(conn, createOrderItemsTable);

            System.out.println("Таблица Orders успешно пересоздана.");
        } catch (SQLException e) {
            System.err.println("Ошибка при пересоздании таблицы Orders: " + e.getMessage());
            throw e;
        }
    }

    public static void checkOrdersTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            

            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'Orders'");
            if (rs.next()) {
                System.out.println("Таблица Orders существует");
                

                rs = stmt.executeQuery("DESCRIBE Orders");
                System.out.println("Структура таблицы Orders:");
                while (rs.next()) {
                    String field = rs.getString("Field");
                    String type = rs.getString("Type");
                    System.out.println(field + " - " + type);
                }
            } else {
                System.out.println("Таблица Orders не существует");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке таблицы Orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSQL(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            System.out.println("Выполняется SQL: " + sql);
            stmt.execute(sql);
        }
    }
}
