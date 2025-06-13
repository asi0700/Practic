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
import java.util.Date;
import java.util.List;

import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import DBobject.DBmanager;
import adminUI.CommonMenuBar;
import model.Order;
import model.Product;
import model.User;
import utils.Logger;

public class ClientWindow extends JFrame {
    private User currentUser;
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable, ordersTable, cartTable;
    private JTextField searchField;
    private List<Object[]> cart = new ArrayList<>(); // Корзина: [product_id, name, quantity, price]
    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    private MainWindow mainWindow;

    public ClientWindow(User user, MainWindow mainWindow) {
        this.currentUser = user;
        this.mainWindow = mainWindow;
        try {
            initializeUI();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации: " + e.getMessage());
            System.err.println("Ошибка инициализации: " + e.getMessage());
        }
    }

    private void initializeUI() throws SQLException {
        setTitle("Склад (Пользовательский режим) - " + currentUser.getName());
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            orderDAO = new OrderDAO(DBmanager.getConnection());
            productDAO = new ProductDAO(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации DAO: " + e.getMessage());
            System.err.println("Ошибка инициализации DAO: " + e.getMessage());
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
        CommonMenuBar menuBar = new CommonMenuBar(
            this,
            (e) -> dispose(),
            (e) -> {
                cardLayout.show(cards, "ORDERS");
                loadOrdersData();
            },
            (e) -> cardLayout.show(cards, "CART"),
            (e) -> cardLayout.show(cards, "PRODUCTS"),
            "client"
        );
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
        int totalProducts = 0;
        try {
            totalProducts = productDAO.getAllProducts().size();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка подсчета товаров: " + e.getMessage());
        }

        statsPanel.add(createStatCard("Всего товаров", String.valueOf(totalProducts)));
        // Placeholder for another stat card if needed
        statsPanel.add(createStatCard("Ваши заказы", "0")); // This will be updated dynamically if possible

        panel.add(statsPanel, BorderLayout.CENTER);

        JButton browseProductsButton = new JButton("Просмотреть товары");
        browseProductsButton.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(browseProductsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProductsPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Товары на складе", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        // Панель поиска
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> filterTable());
        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Таблица товаров
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnIdentifiers(new Object[]{"ID", "Название", "Количество", "Цена", "Поставщик", "Кто добавил", "Дата добавления"});
        productsTable = new JTable(model);
        productsTable.setAutoCreateRowSorter(true);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Устанавливаем ширину столбцов
        productsTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Название
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Количество
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Цена
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Поставщик
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Кто добавил
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(150);  // Дата добавления

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Загрузка данных о товарах
        loadProductsData();

        // Кнопки действий
        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            try {
                refreshData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка обновления данных: " + ex.getMessage());
            }
        });
        JButton addToCartButton = new JButton("Добавить в корзину");
        addToCartButton.addActionListener(e -> {
            int selectedRow = productsTable.getSelectedRow();
            if (selectedRow != -1) {
                int quantity = Integer.parseInt(JOptionPane.showInputDialog(this, "Введите количество", 1));
                if (quantity > 0) {
                    Object[] product = new Object[]{
                        productsTable.getValueAt(selectedRow, 0),
                        productsTable.getValueAt(selectedRow, 1),
                        quantity,
                        productsTable.getValueAt(selectedRow, 3)
                    };
                    cart.add(product);
                    updateCartTable();
                    JOptionPane.showMessageDialog(this, "Товар добавлен в корзину");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Выберите товар для добавления в корзину");
            }
        });
        buttonPanel.add(refreshButton);
        buttonPanel.add(addToCartButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

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

    private void loadProductsData() {
        try {
            List<Object[]> products = productDAO.getAllProducts();
            DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
            model.setRowCount(0);
            for (Object[] p : products) {
                model.addRow(new Object[]{
                    p[0], // ID
                    p[1], // Название
                    p[3], // Количество
                    p[4], // Цена
                    p[5] != null ? p[5] : "Не указано", // Поставщик
                    p[7] != null ? p[7] : "Неизвестно", // Кто добавил
                    p[6] // Дата добавления
                });
            }
            Logger.log("Клиент " + currentUser.getUsername() + " загрузил данные товаров.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных товаров: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка загрузки данных товаров для клиента: " + e.getMessage(), e);
        }
    }

    private void refreshData() throws SQLException {
        loadProductsData();
        Logger.log("Клиент " + currentUser.getUsername() + " обновил данные товаров.");
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Ваши заказы", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        String[] columnNames = {"ID", "ID клиента", "Сумма", "Статус", "Дата заказа", "Последнее обновление"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(tableModel);
        ordersTable.setAutoCreateRowSorter(true);
        ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Устанавливаем ширину столбцов
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // ID клиента
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Сумма
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Статус
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Дата заказа
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Последнее обновление

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        loadOrdersData();

        ordersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = ordersTable.getSelectedRow();
                    if (selectedRow != -1) {
                        JOptionPane.showMessageDialog(ClientWindow.this, "Opening order details for ID: " + tableModel.getValueAt(selectedRow, 0));
                        Logger.log("Клиент " + currentUser.getUsername() + " просмотрел детали заказа ID: " + tableModel.getValueAt(selectedRow, 0));
                    }
                }
            }
        });

        return panel;
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
            List<Object[]> orders = orderDAO.getClientOrders(currentUser.getUserid());
            DefaultTableModel model = (DefaultTableModel) ordersTable.getModel();
            model.setRowCount(0); // Clear existing data
            for (Object[] order : orders) {
                model.addRow(new Object[]{
                    order[0], // ID заказа
                    order[1], // ID клиента
                    String.format("%.2f ₽", (double)order[3]), // Сумма
                    order[4], // Статус
                    order[5], // Дата заказа
                    order[6]  // Последнее обновление
                });
            }
            Logger.log("Клиент " + currentUser.getUsername() + " загрузил данные заказов: " + orders.size() + " записей.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных заказов: " + e.getMessage());
            System.err.println("Ошибка загрузки данных заказов: " + e.getMessage());
            Logger.logError("Ошибка загрузки данных заказов для клиента: " + e.getMessage(), e);
        }
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Корзина", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnIdentifiers(new Object[]{"ID", "Название", "Количество", "Цена", "Итого"});
        cartTable = new JTable(model);
        cartTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(cartTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton removeButton = new JButton("Удалить из корзины");
        removeButton.addActionListener(e -> {
            int selectedRow = cartTable.getSelectedRow();
            if (selectedRow != -1) {
                cart.remove(selectedRow);
                updateCartTable();
                JOptionPane.showMessageDialog(this, "Товар удален из корзины");
            } else {
                JOptionPane.showMessageDialog(this, "Выберите товар для удаления из корзины");
            }
        });
        JButton checkoutButton = new JButton("Оформить заказ");
        checkoutButton.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Корзина пуста. Добавьте товары перед оформлением заказа.");
                return;
            }
            // Placeholder for order processing logic
            JOptionPane.showMessageDialog(this, "Заказ успешно оформлен!");
            cart.clear();
            updateCartTable();
        });
        buttonPanel.add(removeButton);
        buttonPanel.add(checkoutButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        updateCartTable();

        return panel;
    }

    private void updateCartTable() {
        DefaultTableModel model = (DefaultTableModel) cartTable.getModel();
        model.setRowCount(0);
        for (Object[] item : cart) {
            double total = (int) item[2] * (double) item[3];
            model.addRow(new Object[]{item[0], item[1], item[2], item[3], total});
        }
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        card.setPreferredSize(new Dimension(200, 100));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void filterTable() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) productsTable.getModel());
        productsTable.setRowSorter(sorter);
        String text = searchField.getText();
        if (text.length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter(text));
        }
    }

    private void showProductDetails() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow != -1) {
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < productsTable.getColumnCount(); i++) {
                details.append(productsTable.getColumnName(i)).append(": ").append(productsTable.getValueAt(selectedRow, i)).append("\n");
            }
            JOptionPane.showMessageDialog(this, details.toString(), "Подробности о товаре", JOptionPane.INFORMATION_MESSAGE);
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
        dispose();
        new LoginWindow().setVisible(true);
    }

    private void logAction(String action) {
        Logger.log(action);
    }
}