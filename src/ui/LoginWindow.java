package ui;

import java.awt.GridLayout;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import DBobject.DBmanager;
import Dao_db.AddUser;
import adminUI.AdminWindow;
import adminUI.Admin_PhWindow;
import model.User;
import utils.Logger;
import workerUI.WorkerWindow;

public class LoginWindow extends JFrame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient JTextField usernameField;
    private transient JPasswordField passwordField;
    private transient JButton loginButton, registerButton;
    private transient AddUser userDAO;
    private transient MainWindow mainWindow;

    public LoginWindow() {
        try {
            this.userDAO = new AddUser(DBmanager.getConnection());
            initializeUI();
        } catch (SQLException e) {
            Logger.logError("Ошибка при инициализации UserDAO", e);
            JOptionPane.showMessageDialog(this, "Ошибка при инициализации: " + e.getMessage());
        }
    }

    private void initializeUI() {
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
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, введите имя пользователя и пароль");
            return;
        }

        try {
            String hashedPassword = hashPassword(password);
            User user = userDAO.findByUsername(username);
            if (user != null && user.getPassword().equals(hashedPassword)) {
                Logger.log("Успешный вход пользователя: " + username);
                Logger.setCurrentUser(username, user.getRole());
                dispose();
                
                String role = user.getRole() != null ? user.getRole().toLowerCase().trim() : "";
                Logger.log("Роль пользователя: '" + role + "'");
                
                if (role.isEmpty()) {
                    Logger.logError("Роль пользователя не определена для: " + username, null);
                    JOptionPane.showMessageDialog(this, "Ошибка: роль пользователя не определена");
                    return;
                }

                this.setVisible(false);
                mainWindow = new MainWindow(user);

                try {
                    if (role.equals("admin")) {
                        Logger.log("Открытие окна администратора для: " + username);
                        AdminWindow adminWindow = new AdminWindow(user, mainWindow);
                        adminWindow.setVisible(true);
                    } else if (role.equals("admin_ph")) {
                        Logger.log("Открытие окна администратора ph для: " + username);
                        Admin_PhWindow adminPhWindow = new Admin_PhWindow(user, (MainWindow) null);
                        adminPhWindow.setVisible(true);
                    } else if (role.equals("worker")) {
                        Logger.log("Открытие окна работника для: " + username);
                        WorkerWindow workerWindow = new WorkerWindow(DBmanager.getConnection(), username, mainWindow);
                        workerWindow.setVisible(true);
                    } else if (role.equals("client")) {
                        Logger.log("Открытие окна клиента для: " + username);
                        ClientWindow clientWindow = new ClientWindow(user, mainWindow);
                        clientWindow.setVisible(true);
                    } else {
                        Logger.logError("Неизвестная роль пользователя: '" + role + "' для: " + username, null);
                        JOptionPane.showMessageDialog(this, "Ошибка: неизвестная роль пользователя");
                        this.setVisible(true);
                        return;
                    }
                } catch (Exception e) {
                    Logger.logError("Ошибка при создании окна для роли: " + role, e);
                    JOptionPane.showMessageDialog(this, "Ошибка при открытии окна: " + e.getMessage());
                    this.setVisible(true);
                    return;
                }
            } else {
                Logger.log("Неудачная попытка входа для пользователя: " + username);
                JOptionPane.showMessageDialog(this, "Неверное имя пользователя или пароль");
            }
        } catch (Exception e) {
            Logger.logError("Ошибка при входе пользователя: " + username, e);
            JOptionPane.showMessageDialog(this, "Ошибка при входе: " + e.getMessage());
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
        Registration registrationWindow = new Registration();
        registrationWindow.setVisible(true);
        dispose();
    }
}