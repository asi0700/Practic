package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import DBobject.DBmanager;
import model.Order;
import model.Product;
import model.User;

public class ClientWindow extends JFrame {
    private User currentUser;
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable, ordersTable, cartTable;
    private JTextField searchField;
    private List<Object[]> cart = new ArrayList<>(); // Корзина: [product_id, name, quantity, price]
    private OrderDAO orderDAO;
    private MainWindow mainWindow;

    public ClientWindow(User user, MainWindow mainWindow) throws SQLException {
        this.currentUser = user;
        this.mainWindow = mainWindow;
        initializeUI();
    }

    public ClientWindow(User user) throws SQLException {
        this(user, null);
    }

    private void initializeUI() throws SQLException {
        setTitle("Склад (Пользовательский режим) - " + currentUser.getName());
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            orderDAO = new OrderDAO(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации OrderDAO: " + e.getMessage());
            System.err.println("Ошибка инициализации OrderDAO: " + e.getMessage());
        }

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        createMenuBar();

        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createOrdersPanel(), "ORDERS");
        cards.add(createCartPanel(), "CART");

        add(cards, BorderLayout.CENTER);

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu navMenu = new JMenu("Навигация");
        JMenuItem dashboardItem = new JMenuItem("Главная");
        dashboardItem.addActionListener(e -> cardLayout.show(cards, "DASHBOARD"));
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));
        JMenuItem ordersItem = new JMenuItem("Мои заказы");
        ordersItem.addActionListener(e -> {
            cardLayout.show(cards, "ORDERS");
            loadOrdersData();
        });
        JMenuItem cartItem = new JMenuItem("Корзина");
        cartItem.addActionListener(e -> cardLayout.show(cards, "CART"));

        navMenu.add(dashboardItem);
        navMenu.add(productsItem);
        navMenu.add(ordersItem);
        navMenu.add(cartItem);

        JMenuItem exitItem = new JMenuItem("Выйти в главное меню");
        exitItem.addActionListener(e -> returnToMainWindow());

        JMenuItem logoutItem = new JMenuItem("Выйти из аккаунта");
        logoutItem.addActionListener(e -> logout());

        menuBar.add(navMenu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(exitItem);
        menuBar.add(logoutItem);

        setJMenuBar(menuBar);
    }

    private JPanel createDashboardPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Главная панель", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Подсчет товаров и категорий
        ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
        int totalProducts = 0;
        try {
            totalProducts = productDao.getAllProducts().size();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка подсчета товаров: " + e.getMessage());
        }

        statsPanel.add(createStatCard("Всего товаров", String.valueOf(totalProducts)));
        statsPanel.add(createStatCard("Категории товаров", "8")); // Заменить на реальный подсчет категорий

        panel.add(statsPanel, BorderLayout.CENTER);

        JLabel warningLabel = new JLabel("Товары с низким остатком:", SwingConstants.CENTER);
        warningLabel.setForeground(Color.RED);
        warningLabel.setFont(new Font("Arial", Font.BOLD, 16));

        DefaultListModel<String> lowStockModel = new DefaultListModel<>();
        try {
            List<Product> lowStockProducts = productDao.getLowStockProducts(5);
            for (Product p : lowStockProducts) {
                lowStockModel.addElement(p.getName() + " (осталось: " + p.getQuantity() + ")");
            }
        } catch (SQLException e) {
            lowStockModel.addElement("Ошибка загрузки: " + e.getMessage());
        }

        JList<String> lowStockList = new JList<>(lowStockModel);
        lowStockList.setBackground(new Color(255, 240, 240));

        JPanel warningPanel = new JPanel(new BorderLayout());
        warningPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        warningPanel.add(warningLabel, BorderLayout.NORTH);
        warningPanel.add(new JScrollPane(lowStockList), BorderLayout.CENTER);

        panel.add(warningPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProductsPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Список товаров на складе", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Подключение к базе данных
        ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
        List<Product> products;
        try {
            products = productDao.getAllProducts();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки товаров: " + e.getMessage());
            products = new ArrayList<>();
        }

        String[] columnNames = {"ID", "Наименование", "Количество", "Цена", "Местоположение", "Категория"};
        Object[][] data = new Object[products.size()][6];
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            data[i][0] = p.getId(); // Changed from getProduct_id() to getId()
            data[i][1] = p.getName();
            data[i][2] = p.getQuantity();
            data[i][3] = p.getPrice();
            data[i][4] = p.getSupplier() != null ? p.getSupplier() : "Не указано";
            data[i][5] = "Общая";
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productsTable = new JTable(model);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        productsTable.setRowSorter(sorter);

        JPanel searchPanel = new JPanel();
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> filterTable());

        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            try {
                refreshData();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        searchPanel.add(refreshButton);

        JButton addToCartButton = new JButton("Добавить в корзину");
        addToCartButton.addActionListener(e -> {
            int row = productsTable.getSelectedRow();
            if (row >= 0) {
                int productId = (int) productsTable.getValueAt(row, 0);
                String name = (String) productsTable.getValueAt(row, 1);
                int quantityAvailable = (int) productsTable.getValueAt(row, 2);
                double price = (double) productsTable.getValueAt(row, 3);
                String quantityInput = JOptionPane.showInputDialog(this, "Введите количество (доступно: " + quantityAvailable + "):");
                if (quantityInput != null && !quantityInput.isEmpty()) {
                    try {
                        int quantity = Integer.parseInt(quantityInput);
                        if (quantity > 0 && quantity <= quantityAvailable) {
                            cart.add(new Object[]{productId, name, quantity, price});
                            JOptionPane.showMessageDialog(this, name + " добавлен в корзину!");
                        } else {
                            JOptionPane.showMessageDialog(this, "Недопустимое количество!");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Введите корректное число!");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Выберите товар!");
            }
        });
        searchPanel.add(addToCartButton);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(productsTable), BorderLayout.CENTER);

        productsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showProductDetails();
                }
            }
        });

        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Мои заказы", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        ordersTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadOrdersData());
        panel.add(refreshButton, BorderLayout.SOUTH);

        loadOrdersData();

        return panel;
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Корзина", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        String[] columnNames = {"Наименование", "Количество", "Цена", "Итог"};
        Object[][] cartData = new Object[cart.size()][4];
        double totalCost = 0;
        for (int i = 0; i < cart.size(); i++) {
            Object[] item = cart.get(i);
            cartData[i][0] = item[1]; // name
            cartData[i][1] = item[2]; // quantity
            cartData[i][2] = item[3]; // price
            double itemTotal = (int) item[2] * (double) item[3];
            cartData[i][3] = itemTotal;
            totalCost += itemTotal;
        }

        DefaultTableModel cartModel = new DefaultTableModel(cartData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cartTable = new JTable(cartModel);
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton checkoutButton = new JButton("Оформить заказ");
        double finalTotalCost = totalCost;
        checkoutButton.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Корзина пуста!");
                return;
            }
            OrderDAO orderDao = null;
            try {
                orderDao = new OrderDAO(DBmanager.getConnection());
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            try {
                int orderId = orderDao.addOrder(currentUser.getUserid(), finalTotalCost, "Новый");
                for (Object[] item : cart) {
                    orderDao.addOrderItem(orderId, (int) item[0], (int) item[2], (double) item[3]);
                }
                cart.clear();
                updateCartTable();
                JOptionPane.showMessageDialog(this, "Заказ успешно оформлен!");
                cardLayout.show(cards, "ORDERS");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка оформления: " + ex.getMessage());
            }
        });
        buttonPanel.add(checkoutButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateCartTable() {
        DefaultTableModel cartModel = (DefaultTableModel) cartTable.getModel();
        cartModel.setRowCount(0);
        double totalCost = 0;
        for (Object[] item : cart) {
            double itemTotal = (int) item[2] * (double) item[3];
            totalCost += itemTotal;
            cartModel.addRow(new Object[]{item[1], item[2], item[3], itemTotal});
        }
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(240, 248, 255));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void filterTable() {
        String query = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) productsTable.getRowSorter();
        if (query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void showProductDetails() {
        int row = productsTable.getSelectedRow();
        if (row >= 0) {
            String id = productsTable.getValueAt(row, 0).toString();
            String name = (String) productsTable.getValueAt(row, 1);
            String quantity = productsTable.getValueAt(row, 2).toString();
            String price = productsTable.getValueAt(row, 3).toString();
            String location = (String) productsTable.getValueAt(row, 4);
            String category = (String) productsTable.getValueAt(row, 5);

            String details = String.format(
                    "<html><b>ID:</b> %s<br>" +
                            "<b>Наименование:</b> %s<br>" +
                            "<b>Количество:</b> %s<br>" +
                            "<b>Цена:</b> %s<br>" +
                            "<b>Местоположение:</b> %s<br>" +
                            "<b>Категория:</b> %s</html>",
                    id, name, quantity, price, location, category
            );

            JOptionPane.showMessageDialog(this, details, "Детальная информация", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshData() throws SQLException {
        DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
        model.setRowCount(0);
        ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
        List<Product> products;
        try {
            products = productDao.getAllProducts();
            for (Product p : products) {
                model.addRow(new Object[]{
                        p.getId(), // Changed from getProduct_id() to getId()
                        p.getName(),
                        p.getQuantity(),
                        p.getPrice(),
                        p.getSupplier() != null ? p.getSupplier() : "Не указано",
                        "Общая" // Заменить на реальную категорию
                });
            }
            JOptionPane.showMessageDialog(this, "Данные успешно обновлены", "Обновление", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка обновления данных: " + e.getMessage());
        }
    }

    private void loadOrdersData() {
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
            // Загружаем заказы только для текущего пользователя
            List<Order> orders = orderDAO.getOrdersByClientId(currentUser.getUserid());
            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"ID", "Дата", "Статус"});

            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getId(),
                    order.getOrderDate(),
                    order.getStatus()
                });
            }

            ordersTable.setModel(model);
            System.out.println("Данные заказов клиента загружены: " + orders.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных заказов: " + e.getMessage());
            System.err.println("Ошибка загрузки данных заказов: " + e.getMessage());
        }
    }

    private void returnToMainWindow() {
        if (mainWindow == null) {
            mainWindow = new MainWindow(currentUser);
        }
        mainWindow.setVisible(true);
        mainWindow.showNavigation();
        dispose();
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Войдите в свой аккаунт");
        dispose();
        new LoginWindow().setVisible(true);
        logAction("Client logged out: " + currentUser.getUsername());
    }

    private void logAction(String action) {
        try {
            String logFilePath = "c:\\Users\\firem\\IdeaProjects\\PracticOsnova\\logs\\actions.log";
            java.io.File logFile = new java.io.File(logFilePath);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(new java.util.Date().toString() + " - " + action);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
        }
    }
}