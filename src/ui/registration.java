package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import Dao_db.AddUser;
import DBobject.DBmanager;
import adminUI.AdminWindow;
import adminUI.CommonMenuBar;
import workerUI.WorkerWindow;
import ui.ClientWindow;
import model.User;

public class registration extends JFrame {
    private JTextField usernameField, nameField, phoneField, addressField;
    private JPasswordField passwordField;
    private JTextField roleField;
    private JButton registerButton, loginButton;
    private AddUser userDao;

    public registration() {
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

        inputPanel.add(new JLabel("Роль (Client/Worker/Admin):"));
        roleField = new JTextField(15);
        inputPanel.add(roleField);

        inputPanel.add(new JLabel("Имя:"));
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
        String role = roleField.getText();
        String name = nameField.getText();
        String phone = phoneField.getText().isEmpty() ? null : phoneField.getText();
        String address = addressField.getText().isEmpty() ? null : addressField.getText();

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String registrationDate = currentDate.format(formatter);

        if (username.isEmpty() || password.isEmpty() || role.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните все обязательные поля!");
            return;
        }

        // Проверка на количество админов
        try (AddUser userDao = new AddUser(DBmanager.getConnection())) {
            if ("admin".equalsIgnoreCase(role)) {
                if (userDao.countAdmins() > 0) {
                    JOptionPane.showMessageDialog(this, "Администратор уже существует. Нельзя зарегистрировать более одного администратора.", "Ошибка регистрации", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String hashedPassword = hashPassword(password);
            User user = new User(0, username, hashedPassword, role, name, phone, address, registrationDate);

            userDao.addUser(user);
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
        roleField.setText("");
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
