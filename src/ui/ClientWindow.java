package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import DBobject.DBmanager;
import model.Product;
import model.User;

public class ClientWindow extends JFrame {
    private User currentUser;
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable, ordersTable, cartTable;
    private JTextField searchField;
    private List<Object[]> cart = new ArrayList<>(); // Корзина: [product_id, name, quantity, price]

    public ClientWindow(User user) throws SQLException {
        this.currentUser = user;
        initializeUI();
    }

    private void initializeUI() throws SQLException {
        setTitle("Склад (Пользовательский режим) - " + currentUser.getName());
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

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
        ordersItem.addActionListener(e -> cardLayout.show(cards, "ORDERS"));
        JMenuItem cartItem = new JMenuItem("Корзина");
        cartItem.addActionListener(e -> cardLayout.show(cards, "CART"));

        navMenu.add(dashboardItem);
        navMenu.add(productsItem);
        navMenu.add(ordersItem);
        navMenu.add(cartItem);

        JMenuItem exitItem = new JMenuItem("Выйти в главное меню");
        exitItem.addActionListener(e -> {
            this.dispose();
            new ui.LoginWindow().setVisible(true);
        });

        menuBar.add(navMenu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(exitItem);

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
            data[i][0] = p.getProduct_id();
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

    private JPanel createOrdersPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Мои заказы", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        OrderDAO orderDao = new OrderDAO(DBmanager.getConnection());
        List<Object[]> orders;
        try {
            orders = orderDao.getClientOrders(currentUser.getUserid());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказов: " + e.getMessage());
            orders = new ArrayList<>();
        }

        String[] columnNames = {"ID заказа", "Дата", "Статус"};
        Object[][] data = new Object[orders.size()][3];
        for (int i = 0; i < orders.size(); i++) {
            data[i][0] = orders.get(i)[0]; // order_id
            data[i][1] = orders.get(i)[1]; // order_date
            data[i][2] = orders.get(i)[2]; // status
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = new JTable(model);
        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JButton detailsButton = new JButton("Посмотреть детали");
        detailsButton.addActionListener(e -> {
            int row = ordersTable.getSelectedRow();
            if (row >= 0) {
                int orderId = (int) ordersTable.getValueAt(row, 0);
                try {
                    List<Object[]> items = orderDao.getOrderItems(orderId);
                    StringBuilder details = new StringBuilder("<html><b>Товары в заказе:</b><br>");
                    for (Object[] item : items) {
                        details.append(item[0]).append(" (Кол-во: ").append(item[1]).append(", Цена: ").append(item[2]).append(")<br>");
                    }
                    details.append("</html>");
                    JOptionPane.showMessageDialog(this, details.toString(), "Детали заказа", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка загрузки деталей: " + ex.getMessage());
                }
            }
        });
        panel.add(detailsButton, BorderLayout.SOUTH);

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
                        p.getProduct_id(),
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
}