package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import model.User;
import java.util.ArrayList;
import java.util.List;
import Dao_db.OrderDAO;
import model.Order;
import java.sql.SQLException;
import java.sql.Connection;
import DBobject.DBmanager;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.util.Date;

public class MainWindow extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTextField searchField;
    private JButton searchButton;
    private User user;
    private User currentUser;
    private JMenuBar menuBar;
    private OrderDAO orderDAO;

    public MainWindow(User currentUser) {
        this.currentUser = currentUser;
        try {
            this.orderDAO = new OrderDAO(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации OrderDAO: " + e.getMessage());
            System.err.println("Ошибка инициализации OrderDAO: " + e.getMessage());
        }
        initializeUI();
        logAction("User logged in: " + currentUser.getUsername());
    }

    private void initializeUI() {
        setTitle("Управление складом");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Добавляем панели для различных вкладок
        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createInventoryPanel(), "INVENTORY");
        cards.add(createOrdersPanel(), "ORDERS"); // Добавляем панель заказов

        add(cards, BorderLayout.CENTER);

        createMenuBar();

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        JMenu navMenu = new JMenu("Навигация");

        JMenuItem dashboardItem = new JMenuItem("Главная");
        dashboardItem.addActionListener(e -> cardLayout.show(cards, "DASHBOARD"));
        navMenu.add(dashboardItem);

        if (currentUser != null) {
            String role = currentUser.getRole().toLowerCase();
            if (role.equals("admin")) {
                JMenuItem productsItem = new JMenuItem("Товары");
                productsItem.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));
                navMenu.add(productsItem);

                JMenuItem inventoryItem = new JMenuItem("Инвентарь");
                inventoryItem.addActionListener(e -> cardLayout.show(cards, "INVENTORY"));
                navMenu.add(inventoryItem);

                JMenuItem ordersItem = new JMenuItem("Заказы");
                ordersItem.addActionListener(e -> {
                    cardLayout.show(cards, "ORDERS");
                    loadOrdersData((JTable) ((JScrollPane) ((JPanel) cards.getComponent(3)).getComponent(1)).getViewport().getView());
                });
                navMenu.add(ordersItem);
            } else if (role.equals("client")) {
                JMenuItem productsItem = new JMenuItem("Товары");
                productsItem.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));
                navMenu.add(productsItem);

                JMenuItem ordersItem = new JMenuItem("Мои заказы");
                ordersItem.addActionListener(e -> {
                    cardLayout.show(cards, "ORDERS");
                    loadOrdersData((JTable) ((JScrollPane) ((JPanel) cards.getComponent(3)).getComponent(1)).getViewport().getView());
                });
                navMenu.add(ordersItem);
            } else if (role.equals("worker")) {
                JMenuItem ordersItem = new JMenuItem("Заказы на обработку");
                ordersItem.addActionListener(e -> {
                    cardLayout.show(cards, "ORDERS");
                    loadOrdersData((JTable) ((JScrollPane) ((JPanel) cards.getComponent(3)).getComponent(1)).getViewport().getView());
                });
                navMenu.add(ordersItem);
            }
        }

        JMenu fileMenu = new JMenu("Файл");
        JMenuItem logoutItem = new JMenuItem("Выйти из аккаунта");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);

        menuBar.add(navMenu);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
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

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Управление товарами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Наименование", "Количество", "Цена"};
        Object[][] data = {};

        productsTable = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Инвентаризация", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Заказы", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        // Здесь можно добавить таблицу заказов и другие элементы управления
        JTable ordersTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Кнопка обновления данных (опционально)
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadOrdersData(ordersTable));
        panel.add(refreshButton, BorderLayout.SOUTH);

        // Загружаем данные заказов при создании панели
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
//        exitWindow.setVisible(true);
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
        // Дополнительная логика для отображения MainWindow, если нужно
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
            List<Object[]> orders = orderDAO.getAllOrders();
            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"ID", "Дата", "Статус", "Клиент"});

            for (Object[] order : orders) {
                model.addRow(new Object[]{
                    order[0], // ID
                    order[1], // Дата
                    order[2], // Статус
                    order[3]  // Клиент
                });
            }

            ordersTable.setModel(model);
            System.out.println("Данные заказов загружены: " + orders.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных заказов: " + e.getMessage());
            System.err.println("Ошибка загрузки данных заказов: " + e.getMessage());
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Войдите в свой аккаунт");
        dispose();
        new LoginWindow().setVisible(true);
        logAction("User logged out: " + currentUser.getUsername());
    }

    private void logAction(String action) {
        try {
            String logFilePath = "c:\\Users\\firem\\IdeaProjects\\PracticOsnova\\logs\\actions.log";
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(new Date().toString() + " - " + action);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
        }
    }
}