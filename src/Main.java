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

        // Изменяю запуск приложения, чтобы начинать с окна логина
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setVisible(true);

    }
}