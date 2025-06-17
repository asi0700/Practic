package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
import adminUI.CommonMenuBar;
import model.User;
import workerUI.WorkerWindow;

public class Registration extends JFrame {
    private JTextField usernameField, nameField, phoneField, addressField;
    private JPasswordField passwordField;
    private JButton registerButton, loginButton;
    private AddUser userDao;

    public Registration() {
        setTitle("Регистрация - Склад-Мастер");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);

        try {
            DBmanager.initializeDatabase();
            userDao = new AddUser(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка подключения к базе данных: " + e.getMessage());
            System.exit(1);
        }

        JPanel inputPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        inputPanel.add(new JLabel("Имя пользователя:"));
        usernameField = new JTextField(15);
        inputPanel.add(usernameField);

        inputPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField(15);
        inputPanel.add(passwordField);

        inputPanel.add(new JLabel("Ваша настоящая имя:"));
        nameField = new JTextField(15);
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Телефон:"));
        phoneField = new JTextField(15);
        inputPanel.add(phoneField);

        inputPanel.add(new JLabel("Адрес:"));
        addressField = new JTextField(15);
        inputPanel.add(addressField);

        registerButton = new JButton("Регистрация");
        registerButton.addActionListener(e -> registerUser());
        inputPanel.add(registerButton);

        loginButton = new JButton("Войти в аккаунт");
        loginButton.addActionListener(e -> openLoginWindow());
        inputPanel.add(loginButton);

        setLayout(new BorderLayout(10, 10));
        add(inputPanel, BorderLayout.CENTER);
    }

    private void registerUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String name = nameField.getText();
        String phone = phoneField.getText().isEmpty() ? null : phoneField.getText();
        String address = addressField.getText().isEmpty() ? null : addressField.getText();

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String registrationDate = currentDate.format(formatter);

        if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните все обязательные поля!");
            return;
        }

        // Роль всегда 'client'
        String role = "client";
        try (AddUser userDao = new AddUser(DBmanager.getConnection())) {
            String hashedPassword = hashPassword(password);
            User newUser = new User(0, username, hashedPassword, role, name, phone, address, registrationDate, "", null);

            userDao.addUser(newUser);
            JOptionPane.showMessageDialog(this, "Регистрация успешна!");
            clearFields();
            User registeredUser = userDao.findByUsername(username);
            if (registeredUser != null) {
                dispose();
                System.out.println("Роль зарегистрированного пользователя: " + registeredUser.getRole());
                if ("admin".equalsIgnoreCase(registeredUser.getRole())) {
                    openAdminWindow(registeredUser, null);
                } else if ("worker".equalsIgnoreCase(registeredUser.getRole())) {
                    MainWindow mainWindow = new MainWindow(registeredUser);
                    WorkerWindow workerWindow = new WorkerWindow(DBmanager.getConnection(), registeredUser.getUsername(), mainWindow);
                    workerWindow.setVisible(true);
                    mainWindow.setVisible(true);
                } else {
                    openClientWindow(registeredUser, null);
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                JOptionPane.showMessageDialog(this, "Пользователь с таким именем уже существует!");
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка регистрации: " + e.getMessage());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка при закрытии ресурса: " + e.getMessage());
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

    private void openLoginWindow() {
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setVisible(true);
        dispose();
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        phoneField.setText("");
        addressField.setText("");
    }

    private void openAdminWindow(User newUser, MainWindow mainWindow) {
        AdminWindow adminWindow = new AdminWindow(newUser, mainWindow); 
        adminWindow.setVisible(true);
        dispose();
    }

    private void openClientWindow(User newUser, MainWindow mainWindow) {
        ClientWindow clientWindow = new ClientWindow(newUser, mainWindow);
        clientWindow.setVisible(true);
        dispose();
    }

    private void createMenuBar() {
        CommonMenuBar menuBar = new CommonMenuBar(
            this,
            (e) -> dispose(),
            (e) -> {},
            (e) -> {},
            (e) -> {},
            ""
        );
        setJMenuBar(menuBar);
    }
}
