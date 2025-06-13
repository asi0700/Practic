package adminUI;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

import Dao_db.ProductDAO;
import Dao_db.AddUser;
import Dao_db.OrderDAO;
import DBobject.DBmanager;
import model.Order;
import model.Product;
import model.User;
import ui.LoginWindow;
import ui.MainWindow;
import adminUI.OrdersWindow; // Ensure OrdersWindow is correctly imported
import adminUI.LogsWindow; // Added import statement for LogsWindow
import utils.Logger;

public class AdminWindow extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTextField searchField;
    private JButton searchButton;
    private User user;
    private OrdersWindow ordersWindow;
    private JTable ordersTable;
    private User currentUser;
    private OrderDAO orderDAO;
    private MainWindow mainWindow;
    private ProductDAO productDAO; // Добавляем объявление переменной
    private int currentUserId;
    private String currentUsername;
    private AddUser userDAO;

    public AdminWindow(User user, MainWindow mainWindow) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        this.currentUser = user;
        this.mainWindow = mainWindow;
        this.currentUserId = user.getUserid();
        this.currentUsername = user.getUsername();
        
        try {
            System.out.println("Проверка таблицы Orders...");
            DBmanager.checkOrdersTable();
            productDAO = new ProductDAO(DBmanager.getConnection());
            orderDAO = new OrderDAO(DBmanager.getConnection());
            userDAO = new AddUser(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка подключения к базе данных: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.log("Ошибка подключения к базе данных: " + e.getMessage());
        }
        
        initializeUI();
        Logger.log("Открыто окно администратора для пользователя " + currentUsername);
    }

    private void initializeUI() {
        setTitle("Панель администратора");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создаем меню
        CommonMenuBar menuBar = new CommonMenuBar(
            this,
            e -> dispose(),
            e -> cardLayout.show(cards, "ORDERS"),
            e -> cardLayout.show(cards, "LOGS"),
            e -> cardLayout.show(cards, "PRODUCTS"),
            "admin"
        );
        setJMenuBar(menuBar);

        // Создаем панель с картами
        cards = new JPanel(new CardLayout());
        cardLayout = (CardLayout) cards.getLayout();

        // Добавляем панели
        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createOrdersPanel(), "ORDERS");
        cards.add(createLogsPanel(), "LOGS");

        // Добавляем панель с картами в окно
        add(cards);

        // Показываем панель товаров по умолчанию
        cardLayout.show(cards, "PRODUCTS");
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Главная панель", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statsPanel.add(createStatCard("Всего товаров", "0"));
        statsPanel.add(createStatCard("Последние поступления", "0"));

        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Заголовок
        JLabel title = new JLabel("Управление товарами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Таблица товаров
        String[] columnNames = {"ID", "Название", "Описание", "Цена", "Количество", "Поставщик", "Дата добавления", "Кто добавил", "Дата изменения", "Кто изменил"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(model);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Устанавливаем ширину столбцов
        productsTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Название
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Описание
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Цена
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Количество
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Поставщик
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Дата добавления
        productsTable.getColumnModel().getColumn(7).setPreferredWidth(150); // Кто добавил
        productsTable.getColumnModel().getColumn(8).setPreferredWidth(150); // Дата изменения
        productsTable.getColumnModel().getColumn(9).setPreferredWidth(150); // Кто изменил

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        JButton addButton = new JButton("Добавить товар");
        JButton editButton = new JButton("Редактировать");
        JButton deleteButton = new JButton("Удалить");

        refreshButton.addActionListener(e -> {
            loadProductsData();
            Logger.log("Обновлены данные товаров");
        });
        addButton.addActionListener(e -> showAddProductDialog());
        editButton.addActionListener(e -> showEditProductDialog());
        deleteButton.addActionListener(e -> deleteSelectedProduct());

        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные
        loadProductsData();

        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Заголовок
        JLabel title = new JLabel("Управление заказами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Таблица заказов
        String[] columnNames = {"ID", "ID клиента", "Дата заказа", "Сумма", "Статус", "Последнее обновление"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(model);
        ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Устанавливаем ширину столбцов
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // ID клиента
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Дата заказа
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Сумма
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Статус
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Последнее обновление

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        JButton addButton = new JButton("Добавить заказ");
        JButton editButton = new JButton("Редактировать");
        JButton deleteButton = new JButton("Удалить");

        refreshButton.addActionListener(e -> {
            loadOrdersData();
            Logger.log("Обновлены данные заказов");
        });
        addButton.addActionListener(e -> showAddOrderDialog());
        editButton.addActionListener(e -> showEditOrderDialog());
        deleteButton.addActionListener(e -> deleteSelectedOrder());

        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные
        loadOrdersData();

        return panel;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Заголовок
        JLabel title = new JLabel("Журнал действий", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Таблица логов
        String[] columnNames = {"Дата", "Действие", "Пользователь"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable logsTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(logsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        JButton exportButton = new JButton("Экспорт в файл");

        refreshButton.addActionListener(e -> loadLogsData(logsTable));
        exportButton.addActionListener(e -> exportLogsToFile());

        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные
        loadLogsData(logsTable);

        return panel;
    }

    private void loadLogsData(JTable logsTable) {
        try {
            File logFile = new File("actions.log");
            if (!logFile.exists()) {
                return;
            }

            DefaultTableModel model = (DefaultTableModel) logsTable.getModel();
            model.setRowCount(0);

            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        model.addRow(new Object[]{
                            parts[0], // Дата
                            parts[1], // Действие
                            parts[2]  // Пользователь
                        });
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения логов: " + e.getMessage());
        }
    }

    private void exportLogsToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить логи");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Текстовые файлы", "txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                File logFile = new File("actions.log");
                if (logFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "Логи успешно экспортированы в файл: " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка экспорта логов: " + e.getMessage());
            }
        }
    }

    private void loadProductsData() {
        DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
        model.setRowCount(0);
        
        try {
            List<Object[]> products = productDAO.getAllProducts();
            for (Object[] productData : products) {
                model.addRow(new Object[]{
                    productData[0], // ID
                    productData[1], // Название
                    productData[2], // Описание
                    productData[3], // Количество
                    productData[4], // Цена
                    productData[5], // Поставщик
                    productData[6], // Дата добавления
                    productData[7]  // Кто добавил
                });
            }
            Logger.log("Загружены данные товаров: " + products.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка загрузки данных товаров: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка загрузки данных товаров", e);
        }
    }

    private Object[][] loadProductsFromDB(String name, String startDate, String endDate, String addedBy, String minQuantity, String maxQuantity, String supplier) {
        try {
            List<Product> products = productDAO.getProductsByFilters(name, startDate, endDate, addedBy, minQuantity, maxQuantity, supplier);
            Object[][] data = new Object[products.size()][];
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                data[i] = new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getQuantity(),
                    p.getPrice(),
                    p.getSupplier() != null ? p.getSupplier() : "Не указано",
                    p.getAdded_date(), // Дата добавления
                    p.getAdded_by() // Кто добавил (ID)
                };
            }
            return data;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка фильтрации товаров: " + e.getMessage());
            return new Object[0][];
        }
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

    private void showAddProductDialog() {
        Logger.log("Открыт диалог добавления товара");
        JDialog dialog = new JDialog(this, "Добавить товар", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(7, 2, 5, 5));

        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField categoryField = new JTextField();

        dialog.add(new JLabel("Название:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Описание:"));
        dialog.add(descriptionField);
        dialog.add(new JLabel("Цена:"));
        dialog.add(priceField);
        dialog.add(new JLabel("Количество:"));
        dialog.add(quantityField);
        dialog.add(new JLabel("Категория:"));
        dialog.add(categoryField);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            try {
                String name = nameField.getText();
                String description = descriptionField.getText();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = categoryField.getText();

                if (name.isEmpty() || description.isEmpty() || category.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Все поля должны быть заполнены");
                    Logger.log("Попытка добавления товара с пустыми полями");
                    return;
                }

                Product product = new Product();
                product.setName(name);
                product.setDescription(description);
                product.setPrice(price);
                product.setQuantity(quantity);
                product.setCategory(category);

                productDAO.addProduct(product);
                Logger.log("Добавлен новый товар: " + name);
                loadProductsData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Цена и количество должны быть числами");
                Logger.log("Ошибка при добавлении товара: неверный формат чисел");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Ошибка при добавлении товара: " + ex.getMessage());
                Logger.logError("Ошибка при добавлении товара", ex);
            }
        });

        dialog.add(new JLabel()); // Пустая ячейка для выравнивания
        dialog.add(saveButton);

        dialog.setVisible(true);
    }

    private void showEditProductDialog() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите товар для редактирования");
            Logger.log("Попытка редактирования товара без выбора");
            return;
        }

        int productId = (int) productsTable.getValueAt(selectedRow, 0);
        try {
            Product product = productDAO.getProductById(productId);
            if (product == null) {
                JOptionPane.showMessageDialog(this, "Товар не найден");
                Logger.log("Попытка редактирования несуществующего товара #" + productId);
                return;
            }

            Logger.log("Открыт диалог редактирования товара #" + productId);
            JDialog dialog = new JDialog(this, "Редактировать товар", true);
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridLayout(7, 2, 5, 5));

            JTextField nameField = new JTextField(product.getName());
            JTextField descriptionField = new JTextField(product.getDescription());
            JTextField priceField = new JTextField(String.valueOf(product.getPrice()));
            JTextField quantityField = new JTextField(String.valueOf(product.getQuantity()));
            JTextField categoryField = new JTextField(product.getCategory());

            dialog.add(new JLabel("Название:"));
            dialog.add(nameField);
            dialog.add(new JLabel("Описание:"));
            dialog.add(descriptionField);
            dialog.add(new JLabel("Цена:"));
            dialog.add(priceField);
            dialog.add(new JLabel("Количество:"));
            dialog.add(quantityField);
            dialog.add(new JLabel("Категория:"));
            dialog.add(categoryField);

            JButton saveButton = new JButton("Сохранить");
            saveButton.addActionListener(e -> {
                try {
                    String name = nameField.getText();
                    String description = descriptionField.getText();
                    double price = Double.parseDouble(priceField.getText());
                    int quantity = Integer.parseInt(quantityField.getText());
                    String category = categoryField.getText();

                    if (name.isEmpty() || description.isEmpty() || category.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Все поля должны быть заполнены");
                        Logger.log("Попытка сохранения товара с пустыми полями");
                        return;
                    }

                    product.setName(name);
                    product.setDescription(description);
                    product.setPrice(price);
                    product.setQuantity(quantity);
                    product.setCategory(category);

                    productDAO.updateProduct(product);
                    Logger.log("Обновлен товар #" + productId + ": " + name);
                    loadProductsData();
                    dialog.dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Цена и количество должны быть числами");
                    Logger.log("Ошибка при обновлении товара: неверный формат чисел");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка при обновлении товара: " + ex.getMessage());
                    Logger.logError("Ошибка при обновлении товара #" + productId, ex);
                }
            });

            dialog.add(new JLabel());
            dialog.add(saveButton);

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при получении данных товара: " + ex.getMessage());
            Logger.logError("Ошибка при получении данных товара #" + productId, ex);
        }
    }

    private void deleteSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите товар для удаления");
            Logger.log("Попытка удаления товара без выбора");
            return;
        }

        int productId = (int) productsTable.getValueAt(selectedRow, 0);
        String productName = (String) productsTable.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Вы уверены, что хотите удалить товар \"" + productName + "\"?",
            "Подтверждение удаления",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                productDAO.deleteProduct(productId);
                Logger.log("Удален товар #" + productId + ": " + productName);
                loadProductsData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при удалении товара: " + ex.getMessage());
                Logger.logError("Ошибка при удалении товара #" + productId, ex);
            }
        } else {
            Logger.log("Отменено удаление товара #" + productId);
        }
    }

    private void returnToMainWindow() {
        if (mainWindow == null) {
            mainWindow = new MainWindow(currentUser);
        }
        mainWindow.setVisible(true);
        mainWindow.showNavigation();
        dispose();
        System.out.println("Returning to MainWindow with admin navigation shown");
    }

    private void openExitWindow() {
        dispose();
    }

    private void showFilterDialog() {
        JDialog dialog = new JDialog(this, "Фильтрация товаров", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 400);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(15);
        JTextField startDateField = new JTextField(10);
        JTextField endDateField = new JTextField(10);
        JTextField addedByField = new JTextField(15);
        JTextField minQuantityField = new JTextField(10);
        JTextField maxQuantityField = new JTextField(10);
        JTextField supplierField = new JTextField(15);

        String[] labels = {"Название:", "Дата добавления (с):", "Дата добавления (по):", "Кто добавил:", "Количество (с):", "Количество (по):", "Поставщик:"};
        JTextField[] fields = {nameField, startDateField, endDateField, addedByField, minQuantityField, maxQuantityField, supplierField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            dialog.add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            dialog.add(fields[i], gbc);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Применить");
        JButton cancelButton = new JButton("Отмена");

        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);

        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        applyButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String startDate = startDateField.getText().trim();
            String endDate = endDateField.getText().trim();
            String addedBy = addedByField.getText().trim();
            String minQuantity = minQuantityField.getText().trim();
            String maxQuantity = maxQuantityField.getText().trim();
            String supplier = supplierField.getText().trim();

            Object[][] filteredData = loadProductsFromDB(
                    name.isEmpty() ? null : name,
                    startDate.isEmpty() ? null : startDate,
                    endDate.isEmpty() ? null : endDate,
                    addedBy.isEmpty() ? null : addedBy,
                    minQuantity.isEmpty() ? null : minQuantity,
                    maxQuantity.isEmpty() ? null : maxQuantity,
                    supplier.isEmpty() ? null : supplier
            );
            DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
            model.setRowCount(0);
            for (Object[] row : filteredData) {
                model.addRow(row);
            }
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void loadOrdersData() {
        try {
            if (orderDAO == null) {
                System.out.println("Инициализация OrderDAO...");
                orderDAO = new OrderDAO(DBmanager.getConnection());
            }
            System.out.println("Получение списка заказов...");
            List<Object[]> orders = orderDAO.getAllOrders();
            System.out.println("Получено заказов: " + orders.size());
            
            DefaultTableModel model = (DefaultTableModel) ordersTable.getModel();
            model.setRowCount(0);
            
            for (Object[] order : orders) {
                if (order == null) {
                    System.out.println("Пропущен null заказ");
                    continue;
                }
                System.out.println("Добавление заказа: " + java.util.Arrays.toString(order));
                try {
                    model.addRow(order);
                } catch (Exception e) {
                    System.err.println("Ошибка при добавлении строки в таблицу: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            Logger.log("Загружены данные заказов: " + orders.size() + " записей");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Ошибка при загрузке заказов: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Ошибка при загрузке заказов: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.log("Ошибка при загрузке заказов: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Неожиданная ошибка: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Неожиданная ошибка: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.log("Неожиданная ошибка: " + e.getMessage());
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Войдите в свой аккаунт");
        dispose();
        new ui.LoginWindow().setVisible(true);
        logAction("Admin logged out: " + currentUser.getUsername());
    }

    private void logAction(String action) {
        String logMessage = String.format("Пользователь %s: %s", currentUser.getUsername(), action);
        System.out.println(logMessage);
        try {
            File logFile = new File("actions.log");
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter writer = new PrintWriter(fw)) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                writer.println(String.format("[%s] %s", timestamp, logMessage));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
        }
    }

    private void showLogsWindow() {
        LogsWindow logsWindow = new LogsWindow();
        logsWindow.setVisible(true);
        logAction("Открытие окна логов");
    }

    private void showLogsPanel() {
        JDialog logsDialog = new JDialog(this, "Логи действий", true);
        logsDialog.setSize(600, 400);
        logsDialog.setLayout(new BorderLayout());

        JTextArea logsArea = new JTextArea();
        logsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logsArea);
        logsDialog.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadLogs(logsArea));
        logsDialog.add(refreshButton, BorderLayout.SOUTH);

        loadLogs(logsArea);
        logsDialog.setLocationRelativeTo(this);
        logsDialog.setVisible(true);
    }

    private void loadLogs(JTextArea logsArea) {
        try {
            String logFilePath = "c:\\Users\\firem\\IdeaProjects\\PracticOsnova\\logs\\actions.log";
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(logFile));
                StringBuilder logs = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    logs.append(line).append("\n");
                }
                br.close();
                logsArea.setText(logs.toString());
            } else {
                logsArea.setText("Лог-файл не найден.");
            }
        } catch (IOException e) {
            logsArea.setText("Ошибка при чтении логов: " + e.getMessage());
            System.err.println("Ошибка при чтении логов: " + e.getMessage());
        }
    }

    private void showOrdersPanel() {
        cardLayout.show(cards, "Orders");
        loadOrdersData();
        logAction("Открыта вкладка заказов");
    }

    private void openOrdersWindow() {
        try {
            OrdersWindow ordersWindow = new OrdersWindow(DBmanager.getConnection(), currentUser.getUsername(), currentUser.getRole(), mainWindow);
            ordersWindow.setVisible(true);
            logAction("Открытие окна заказов");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при открытии окна заказов: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Ошибка при открытии окна заказов: " + e.getMessage());
        }
    }

    private void showAddOrderDialog() {
        logAction("Открыт диалог добавления заказа");
        JDialog dialog = new JDialog(this, "Добавить заказ", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField clientIdField = new JTextField();
        JTextField totalCostField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Новый", "В обработке", "Выполнен", "Отменен"});

        formPanel.add(new JLabel("ID клиента:"));
        formPanel.add(clientIdField);
        formPanel.add(new JLabel("Сумма:"));
        formPanel.add(totalCostField);
        formPanel.add(new JLabel("Статус:"));
        formPanel.add(statusCombo);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            try {
                int clientId = Integer.parseInt(clientIdField.getText());
                double totalCost = Double.parseDouble(totalCostField.getText());
                String status = (String) statusCombo.getSelectedItem();

                Order newOrder = new Order();
                newOrder.setClientId(clientId);
                newOrder.setTotalCost(totalCost);
                newOrder.setStatus(status);
                newOrder.setOrderDate(new Date());
                newOrder.setLastUpdated(new Date().toString());

                orderDAO.addOrder(newOrder);
                loadOrdersData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите корректные числовые значения");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Ошибка добавления заказа: " + ex.getMessage());
            }
        });

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(saveButton, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showEditOrderDialog() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Пожалуйста, выберите заказ для редактирования",
                "Предупреждение",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        String currentStatus = (String) ordersTable.getValueAt(selectedRow, 3);

        String[] statuses = {"Новый", "В обработке", "Выполнен", "Отменен"};
        String newStatus = (String) JOptionPane.showInputDialog(
            this,
            "Выберите новый статус заказа:",
            "Изменение статуса заказа",
            JOptionPane.QUESTION_MESSAGE,
            null,
            statuses,
            currentStatus
        );

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                Order updatedOrder = new Order();
                updatedOrder.setId(orderId);
                updatedOrder.setStatus(newStatus);
                updatedOrder.setClientId((int) ordersTable.getValueAt(selectedRow, 1));
                updatedOrder.setTotalCost((double) ordersTable.getValueAt(selectedRow, 2));
                
                orderDAO.updateOrder(updatedOrder, currentUserId);
                loadOrdersData();
                Logger.log("Обновлен статус заказа " + orderId + " на " + newStatus);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                    "Ошибка при обновлении заказа: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
                Logger.log("Ошибка при обновлении заказа: " + ex.getMessage());
            }
        }
    }

    private void deleteSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для удаления", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            logAction("Попытка удаления заказа без выбора");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Вы уверены, что хотите удалить заказ #" + orderId + "?",
            "Подтверждение удаления",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                orderDAO.deleteOrder(orderId);
                logAction("Удален заказ #" + orderId);
                loadOrdersData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при удалении заказа: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
                logAction("Ошибка при удалении заказа #" + orderId + ": " + ex.getMessage());
            }
        } else {
            logAction("Отменено удаление заказа #" + orderId);
        }
    }

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для просмотра деталей", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            logAction("Попытка просмотра деталей заказа без выбора");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        try {
            List<Object[]> items = orderDAO.getOrderItems(orderId);
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "В заказе нет товаров", 
                    "Информация", JOptionPane.INFORMATION_MESSAGE);
                logAction("Просмотр пустого заказа #" + orderId);
                return;
            }

            logAction("Просмотр деталей заказа #" + orderId + " (" + items.size() + " товаров)");
            // Создаем диалог с деталями заказа
            JDialog dialog = new JDialog(this, "Детали заказа #" + orderId, true);
            dialog.setLayout(new BorderLayout());

            // Создаем таблицу с товарами
            String[] columnNames = {"ID", "Товар", "Количество", "Цена за ед.", "Сумма"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable itemsTable = new JTable(model);
            double totalSum = 0.0;

            for (Object[] item : items) {
                double itemSum = (int)item[2] * (double)item[3]; // quantity * price
                totalSum += itemSum;
                model.addRow(new Object[]{
                    item[0], // ID
                    item[1], // Название товара
                    item[2], // Количество
                    String.format("%.2f ₽", (double)item[3]), // Цена за единицу
                    String.format("%.2f ₽", itemSum) // Общая сумма по товару
                });
            }

            JScrollPane scrollPane = new JScrollPane(itemsTable);
            dialog.add(scrollPane, BorderLayout.CENTER);

            // Добавляем итоговую сумму
            JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            summaryPanel.add(new JLabel(String.format("Итого: %.2f ₽", totalSum)));
            dialog.add(summaryPanel, BorderLayout.SOUTH);

            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при получении деталей заказа: " + ex.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Ошибка при получении деталей заказа #" + orderId + ": " + ex.getMessage());
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Меню "Файл"
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Меню "Справка"
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem aboutItem = new JMenuItem("О программе");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "Система управления складом\nВерсия 1.0",
                "О программе",
                JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);

        // Меню "Журнал"
        JMenu logMenu = new JMenu("Журнал");
        JMenuItem showLogItem = new JMenuItem("Показать журнал действий");
        showLogItem.addActionListener(e -> {
            LogsWindow logWindow = new LogsWindow();
            logWindow.setVisible(true);
        });
        logMenu.add(showLogItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        menuBar.add(logMenu);
        
        setJMenuBar(menuBar);
    }
}