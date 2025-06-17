package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import DBobject.DBmanager;
import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import Dao_db.UserDAO;
import adminUI.AdminWindow;
import adminUI.CommonMenuBar;
import model.Order;
import model.User;
import utils.Logger;
import workerUI.WorkerWindow;

public class MainWindow extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTable ordersTable;
    private JTextField searchField;
    private JButton searchButton;
    private User user;
    private User currentUser;
    private JMenuBar menuBar;
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    private Connection conn;

    public MainWindow(User currentUser) {
        this.currentUser = currentUser;
        try {
            this.conn = DBmanager.getConnection();
            this.orderDAO = new OrderDAO(conn);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации OrderDAO: " + e.getMessage());
            System.err.println("Ошибка инициализации OrderDAO: " + e.getMessage());
        }
        initializeUI();
        logAction("User logged in: " + currentUser.getUsername());
        checkPhotoRequest();
    }

    private void initializeUI() {
        setTitle("Управление складом");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Добавляем панели для различных вкладок
        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createInventoryPanel(), "INVENTORY");
        cards.add(createOrdersPanel(), "ORDERS");

        add(cards, BorderLayout.CENTER);

        createMenuBar();

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        this.menuBar = new CommonMenuBar(
            this,
            (e) -> dispose(),
            (e) -> cardLayout.show(cards, "ORDERS"),
            (e) -> {},
            (e) -> cardLayout.show(cards, "PRODUCTS"),
            currentUser != null ? currentUser.getRole() : ""
        );
        setJMenuBar(this.menuBar);
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Главная панель", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JLabel welcomeLabel = new JLabel("Добро пожаловать, " + currentUser.getName() + "! Ваша роль: " + currentUser.getRole(), SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(welcomeLabel, BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statsPanel.add(createStatCard("Всего товаров", "0"));
        statsPanel.add(createStatCard("Низкий запас", "0"));
        statsPanel.add(createStatCard("Категории", "0"));
        statsPanel.add(createStatCard("Последние поступления", "0"));

        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    public JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Управление товарами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Наименование", "Количество", "Цена"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        productsTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Инвентаризация", SwingConstants.CENTER); // вот тут че ?
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Заказы", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        // тут создание таблицы заказов (если надо меняй  формат таблицы!!!)
        String[] columnNames = {"ID заказа", "ID клиента", "Дата заказа", "Сумма", "Статус", "Город доставки", "Адрес доставки"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками (можешь поменять !! )
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadOrdersData(ordersTable));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные заказов при создании панели!!! (не )
        loadOrdersData(ordersTable);

        return panel;
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void performSearch(ActionEvent e) {
        String query = searchField.getText();

        JOptionPane.showMessageDialog(this, "Поиск: " + query);

    }

    private void openRegistrationWindow() {
        hideNavigation();
        registration registrationWindow = new registration();
        registrationWindow.setVisible(true);
        dispose();
    }

    private void openExitWindow() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
        }

        //        ExitWindow exitWindow = new ExitWindow();
//        exitWindow.setVisible(true); навремя оставим так потом посмотрим этот кусок (если не забуду!!!)
    }

    public void hideNavigation() {
        menuBar.setVisible(false);
        System.out.println("Navigation hidden");
    }

    public void showNavigation() {
        menuBar.setVisible(true);
        System.out.println("Navigation shown");
    }

    public void returnToMainWindow() {
        showNavigation();
        // Дополнительная логика для отображения MainWindow если хочешь добавь просто я не знаю еще что ннужна
    }

    private void loadOrdersData(JTable ordersTable) {
        if (orderDAO == null) {
            JOptionPane.showMessageDialog(this, "OrderDAO не инициализирован. Проверьте подключение к базе данных.");
            try {
                orderDAO = new OrderDAO(DBmanager.getConnection());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка повторной инициализации OrderDAO: " + ex.getMessage());
                System.err.println("Ошибка повторной инициализации OrderDAO: " + ex.getMessage());
            }
            return;
        }

        try {
            List<Order> orders = orderDAO.getAllOrders();
            DefaultTableModel model = (DefaultTableModel) ordersTable.getModel();
            model.setRowCount(0);

            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getOrderId(),
                    order.getClientId(),
                    order.getOrderDate(),
                    String.format("%.2f ₽", order.getTotalCost()),
                    order.getStatus(),
                    order.getDeliveryCity(),
                    order.getDeliveryAddress()
                });
            }

            System.out.println("Данные заказов загружены: " + orders.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных заказов: " + e.getMessage());
            System.err.println("Ошибка загрузки данных заказов: " + e.getMessage());
        }
    }

    private void logout() {
        logAction("User logged out: " + currentUser.getUsername());
        dispose();
    }

    public void logAction(String action) {
        try {
            String sql = "INSERT INTO Logs (user_id, action, timestamp) VALUES (?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getUserid());
                stmt.setString(2, action);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при логировании: " + e.getMessage());
        }
    }

    private void handleLoginSuccess(String username, String role) {
        Logger.log("Успешный вход в систему: " + username + " с ролью: " + role);
        

        User user = new User(0, username, "", role, username, "", "", "", "", null);

        MainWindow mainWindow = null;
        if (role.equals("admin")) {
            AdminWindow adminWindow = new AdminWindow(user, mainWindow);
            adminWindow.setVisible(true);
        } else if (role.equals("worker")) {
            WorkerWindow workerWindow = new WorkerWindow(conn, username, this);
            workerWindow.setVisible(true);
        } else {
            ClientWindow clientWindow = new ClientWindow(user, mainWindow);
            clientWindow.setVisible(true);
        }
        dispose();
    }

    private void checkPhotoRequest() {
        try (UserDAO userDAO = new UserDAO(DBmanager.getConnection())) {
            if (userDAO.isPhotoRequest(currentUser.getUserid())) {
               
                String sql = "SELECT u1.role FROM Users u1 " +
                           "JOIN Users u2 ON u2.user_id = ? " +
                           "WHERE u1.role = 'admin_ph'";
                try (PreparedStatement stmt = DBmanager.getConnection().prepareStatement(sql)) {
                    stmt.setInt(1, currentUser.getUserid());
                    java.sql.ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int result = JOptionPane.showConfirmDialog(
                                this,
                                "Администратор запрашивает ваше согласие на обработку ваших данных",
                                "Согласие на обработку ваших данных",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
                                webcam.open();
                                java.awt.image.BufferedImage image = webcam.getImage();
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                javax.imageio.ImageIO.write(image, "JPG", baos);
                                byte[] photoBytes = baos.toByteArray();
                                webcam.close();
                                userDAO.updateUserPhoto(currentUser.getUserid(), photoBytes);
                                userDAO.setPhotoRequest(currentUser.getUserid(), false);
                                JOptionPane.showMessageDialog(this, "Спасибо за сотрудничество!");
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(this, "Ошибка при создании о: " + e.getMessage());
                            }
                        } else {
                            userDAO.setPhotoRequest(currentUser.getUserid(), false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке photo_request: " + e.getMessage());
        }
    }
}