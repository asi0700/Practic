package src;

import DBobject.DBmanager;
import ui.LoginWindow;
import utils.Logger;

import javax.swing.*;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            // Инициализация логгера
            Logger.initialize();
            Logger.log("Запуск приложения");

            DBmanager.initializeDatabase();
            Logger.log("База данных инициализирована");

            // Изменяю запуск приложения, чтобы начинать с окна логина
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
            Logger.log("Окно входа открыто");

        } catch (SQLException e) {
            Logger.logError("Ошибка инициализации базы данных", e);
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Ошибка инициализации базы данных: " + e.getMessage());
            System.exit(1);
        }
    }
}