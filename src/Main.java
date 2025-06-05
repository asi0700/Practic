import DBobject.DBmanager;

import javax.swing.*;
import ui.registration;
import ui.LoginWindow;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            DBmanager.initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Ошибка инициализации базы данных: " + e.getMessage());
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            LoginWindow window = new LoginWindow();
            window.setVisible(true);
        });

    }
}