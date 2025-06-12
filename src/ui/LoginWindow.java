package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import Dao_db.AddUser;
import DBobject.DBmanager;
import adminUI.AdminWindow;
import adminUI.WorkerWindow;
import ui.ClientWindow;
import model.User;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;

    public LoginWindow() {
        setTitle("Вход - Склад-Мастер");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Имя пользователя: "));
        usernameField = new JTextField(15);
        panel.add(usernameField);

        panel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField(15);
        panel.add(passwordField);

        loginButton = new JButton("Вход");
        loginButton.addActionListener(e -> login());
        panel.add(loginButton);

        registerButton = new JButton("Регистрация");
        registerButton.addActionListener(e -> openRegistrationWindow());
        panel.add(registerButton);

        add(panel);
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        User user = authenticateUser(username, password);
        if (user != null) {
            MainWindow mainWindow = new MainWindow(user);
            if ("admin".equalsIgnoreCase(user.getRole())) {
                new AdminWindow(user, mainWindow).setVisible(true);
                mainWindow.setVisible(false);
            } else if ("client".equalsIgnoreCase(user.getRole())) {
                new ClientWindow(user, mainWindow).setVisible(true);
                mainWindow.setVisible(false);
            } else if ("worker".equalsIgnoreCase(user.getRole())) {
                try {
                    WorkerWindow workerWindow = new WorkerWindow(DBmanager.getConnection(), user.getUsername(), mainWindow);
                    workerWindow.setVisible(true);
                    mainWindow.setVisible(false);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при открытии окна работника: " + e.getMessage());
                }
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Неверное имя пользователя или пароль");
        }
    }

    private User authenticateUser(String username, String password) {
        String hashedPassword = hashPassword(password);

        try (AddUser userDao = new AddUser(DBmanager.getConnection())) {
            User user = userDao.findByUsername(username);
            if (user != null && user.getPassword().equals(hashedPassword)) {
                System.out.println("Вход успешен для пользователя: " + user.getUsername());
                System.out.println("Роль пользователя: " + user.getRole());
                return user;
            } else {
                return null;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + e.getMessage());
            return null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка при закрытии ресурса: " + e.getMessage());
            return null;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при хешировании пароля", e);
        }
    }

    private void openRegistrationWindow() {
        registration registrationWindow = new registration();
        registrationWindow.setVisible(true);
        dispose();
    }
}