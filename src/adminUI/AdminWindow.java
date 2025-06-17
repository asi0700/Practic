package adminUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import DBobject.DBmanager;
import Dao_db.AddUser;
import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import model.Order;
import model.OrderItem;
import model.Product;
import model.User;
import ui.MainWindow;
import utils.Logger;

public class AdminWindow extends JFrame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient JPanel cards;
    private transient CardLayout cardLayout;
    private transient JTable productsTable;
    private transient JTextField searchField;
    private transient JButton searchButton;
    private transient User user;
    private transient JTable ordersTable;
    private transient User currentUser;
    private transient OrderDAO orderDAO;
    private transient MainWindow mainWindow;
    private transient ProductDAO productDAO;
    private int currentUserId;
    private String currentUsername;
    private transient AddUser userDAO;

    public AdminWindow(User user, MainWindow mainWindow) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        this.currentUser = user;
        this.mainWindow = mainWindow;
        this.currentUserId = user.getUserid();
        this.currentUsername = user.getUsername();
        
        try {
            Logger.log("Проверка таблицы Orders...");
            DBmanager.checkOrdersTable();
            productDAO = new ProductDAO(DBmanager.getConnection());
            orderDAO = new OrderDAO(DBmanager.getConnection());
            userDAO = new AddUser(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка подключения к базе данных: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка подключения к базе данных", e);
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
        cards.add(createUsersPanel(), "USERS");

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
        

        JLabel title = new JLabel("Управление товарами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


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


        loadProductsData();

        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        

        JLabel title = new JLabel("Управление заказами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


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


        loadOrdersData();

        return panel;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        

        JLabel title = new JLabel("Журнал действий", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


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


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        JButton exportButton = new JButton("Экспорт в файл");

        refreshButton.addActionListener(e -> loadLogsData(logsTable));
        exportButton.addActionListener(e -> exportLogsToFile());

        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

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
                            parts[0],
                            parts[1],
                            parts[2]
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
                    productData[0],
                    productData[1],
                    productData[2],
                    productData[3],
                    productData[4],
                    productData[5],
                    productData[6],
                    productData[7]
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
                    p.getAdded_date(),
                    p.getAdded_by()
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
                product.setAdded_by(currentUser.getUserid());
                product.setModified_by(currentUser.getUserid());
                product.setAdded_date(new Date());
                product.setModified_date(new Date());

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

        dialog.add(new JLabel());
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
                    product.setModified_by(currentUser.getUserid());
                    product.setModified_date(new Date());

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
        Logger.log("Возврат в главное окно с отображением навигации администратора");
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
        DefaultTableModel model = (DefaultTableModel) ordersTable.getModel();
        model.setRowCount(0);
        
        try {
            List<Order> orders = orderDAO.getAllOrders();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            
            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getOrderId(),
                    order.getClientId(),
                    dateFormat.format(order.getOrderDate()),
                    String.format("%.2f", order.getTotalCost()),
                    order.getStatus(),
                    dateFormat.format(order.getLastUpdated()),
                    order.getDeliveryCity(),
                    order.getDeliveryAddress()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка при загрузке заказов: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.log("Ошибка при загрузке заказов: " + e.getMessage());
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Войдите в свой аккаунт");
        dispose();
        new ui.LoginWindow().setVisible(true);
        Logger.log("Выход администратора: " + currentUser.getUsername());
    }

    private void showLogsWindow() {
        LogsWindow logsWindow = new LogsWindow();
        logsWindow.setVisible(true);
        Logger.log("Открытие окна логов");
    }

    private void showOrdersPanel() {
        cardLayout.show(cards, "Orders");
        loadOrdersData();
        Logger.log("Открыта вкладка заказов");
    }

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для просмотра деталей");
            Logger.log("Попытка просмотра деталей заказа без выбора");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        try {
            List<OrderItem> items = orderDAO.getOrderItems(orderId);
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Заказ #" + orderId + " не содержит товаров");
                Logger.log("Просмотр пустого заказа #" + orderId);
                return;
            }

            Logger.log("Просмотр деталей заказа #" + orderId + " (" + items.size() + " товаров)");
            // Создаем диалог с деталями заказа
            JDialog dialog = new JDialog(this, "Детали заказа #" + orderId, true);
            dialog.setLayout(new BorderLayout());


            String[] columnNames = {"ID", "Товар", "Количество", "Цена за ед.", "Сумма"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable itemsTable = new JTable(model);
            double totalSum = 0;

            for (OrderItem item : items) {
                double itemSum = item.getQuantity() * item.getPrice();
                totalSum += itemSum;
                model.addRow(new Object[]{
                    item.getProductId(),
                    item.getQuantity(),
                    String.format("%.2f ₽", item.getPrice()),
                    String.format("%.2f ₽", itemSum)
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
            Logger.logError("Ошибка при получении деталей заказа #" + orderId, ex);
            JOptionPane.showMessageDialog(this,
                "Ошибка при получении деталей заказа: " + ex.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
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

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Пользователи", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Панель с кнопками
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createWorkerButton = new JButton("Создать аккаунт работника");
        createWorkerButton.addActionListener(e -> showCreateWorkerDialog());
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> refreshUsersTable());
        topPanel.add(refreshButton);
        topPanel.add(createWorkerButton);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Логин", "Имя", "Роль", "Телефон"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable usersTable = new JTable(model);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(usersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Загружаем пользователей при создании панели
        refreshUsersTable();

        return panel;
    }

    private void refreshUsersTable() {
        DefaultTableModel model = (DefaultTableModel) ((JTable)((JScrollPane)((JPanel)cards.getComponent(3)).getComponent(1)).getViewport().getView()).getModel();
        model.setRowCount(0);
        
        try {
            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                model.addRow(new Object[]{
                    user.getUserid(),
                    user.getUsername(),
                    user.getName(),
                    user.getRole(),
                    user.getPhone()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при обновлении списка пользователей: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка при обновлении списка пользователей", e);
        }
    }

    private void showCreateWorkerDialog() {
        JDialog dialog = new JDialog(this, "Создание аккаунта работника", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JTextField nameField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextField addressField = new JTextField(20);

        dialog.add(new JLabel("Имя пользователя:"));
        dialog.add(usernameField);
        dialog.add(new JLabel("Пароль:"));
        dialog.add(passwordField);
        dialog.add(new JLabel("Имя:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Телефон:"));
        dialog.add(phoneField);
        dialog.add(new JLabel("Адрес:"));
        dialog.add(addressField);

        JButton createButton = new JButton("Создать");
        JButton cancelButton = new JButton("Отмена");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel);

        createButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Пожалуйста, заполните все обязательные поля (имя пользователя, пароль, имя)",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {

                String hashedPassword = hashPassword(password);
                

                User newWorker = new User(0, username, hashedPassword, "worker", name, phone, address, 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "", null);
                

                userDAO.addUser(newWorker);
                
                JOptionPane.showMessageDialog(dialog, 
                    "Аккаунт работника успешно создан!\nИмя пользователя: " + username + "\nПароль: " + password,
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
                
                dialog.dispose();
                refreshUsersTable();
                
            } catch (SQLException ex) {
                if (ex.getMessage().contains("UNIQUE constraint failed")) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Пользователь с таким именем уже существует!",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Ошибка при создании аккаунта: " + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

    public void showUsersPanel() {
        cardLayout.show(cards, "USERS");
        refreshUsersTable(); // Обновляем таблицу при показе панели
    }

    public void showDashboard() {
        cardLayout.show(cards, "DASHBOARD");
    }

    private void showAddOrderDialog() {
        JDialog dialog = new JDialog(this, "Добавить заказ", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        JTextField clientIdField = new JTextField();
        JTextField deliveryCityField = new JTextField();
        JTextField deliveryAddressField = new JTextField();
        JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"Новый", "В обработке", "Отправлен", "Доставлен", "Отменен"});

        dialog.add(new JLabel("ID клиента:"));
        dialog.add(clientIdField);
        dialog.add(new JLabel("Город доставки:"));
        dialog.add(deliveryCityField);
        dialog.add(new JLabel("Адрес доставки:"));
        dialog.add(deliveryAddressField);
        dialog.add(new JLabel("Статус:"));
        dialog.add(statusComboBox);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            try {
                int clientId = Integer.parseInt(clientIdField.getText());
                String deliveryCity = deliveryCityField.getText();
                String deliveryAddress = deliveryAddressField.getText();
                String status = (String) statusComboBox.getSelectedItem();

                Order order = new Order();
                order.setClientId(clientId);
                order.setDeliveryCity(deliveryCity);
                order.setDeliveryAddress(deliveryAddress);
                order.setStatus(status);
                order.setOrderDate(new Date());
                order.setLastUpdated(new Date());

                orderDAO.addOrder(order);
                Logger.log("Добавлен новый заказ для клиента #" + clientId);
                loadOrdersData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "ID клиента должен быть числом");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Ошибка при добавлении заказа: " + ex.getMessage());
                Logger.logError("Ошибка при добавлении заказа", ex);
            }
        });

        dialog.add(new JLabel());
        dialog.add(saveButton);
        dialog.setVisible(true);
    }

    private void showEditOrderDialog() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для редактирования");
            Logger.log("Попытка редактирования заказа без выбора");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order == null) {
                JOptionPane.showMessageDialog(this, "Заказ не найден");
                Logger.log("Попытка редактирования несуществующего заказа #" + orderId);
                return;
            }

            JDialog dialog = new JDialog(this, "Редактировать заказ", true);
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridLayout(6, 2, 5, 5));

            JTextField clientIdField = new JTextField(String.valueOf(order.getClientId()));
            JTextField deliveryCityField = new JTextField(order.getDeliveryCity());
            JTextField deliveryAddressField = new JTextField(order.getDeliveryAddress());
            JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"Новый", "В обработке", "Отправлен", "Доставлен", "Отменен"});
            statusComboBox.setSelectedItem(order.getStatus());

            dialog.add(new JLabel("ID клиента:"));
            dialog.add(clientIdField);
            dialog.add(new JLabel("Город доставки:"));
            dialog.add(deliveryCityField);
            dialog.add(new JLabel("Адрес доставки:"));
            dialog.add(deliveryAddressField);
            dialog.add(new JLabel("Статус:"));
            dialog.add(statusComboBox);

            JButton saveButton = new JButton("Сохранить");
            saveButton.addActionListener(e -> {
                try {
                    int clientId = Integer.parseInt(clientIdField.getText());
                    String deliveryCity = deliveryCityField.getText();
                    String deliveryAddress = deliveryAddressField.getText();
                    String status = (String) statusComboBox.getSelectedItem();

                    order.setClientId(clientId);
                    order.setDeliveryCity(deliveryCity);
                    order.setDeliveryAddress(deliveryAddress);
                    order.setStatus(status);
                    order.setLastUpdated(new Date());

                    orderDAO.updateOrder(order);
                    Logger.log("Обновлен заказ #" + orderId);
                    loadOrdersData();
                    dialog.dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "ID клиента должен быть числом");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка при обновлении заказа: " + ex.getMessage());
                    Logger.logError("Ошибка при обновлении заказа #" + orderId, ex);
                }
            });

            dialog.add(new JLabel());
            dialog.add(saveButton);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при получении данных заказа: " + ex.getMessage());
            Logger.logError("Ошибка при получении данных заказа #" + orderId, ex);
        }
    }

    private void deleteSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для удаления");
            Logger.log("Попытка удаления заказа без выбора");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        int clientId = (int) ordersTable.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Вы уверены, что хотите удалить заказ #" + orderId + " для клиента #" + clientId + "?",
            "Подтверждение удаления",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                orderDAO.deleteOrder(orderId);
                Logger.log("Удален заказ #" + orderId);
                loadOrdersData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при удалении заказа: " + ex.getMessage());
                Logger.logError("Ошибка при удалении заказа #" + orderId, ex);
            }
        } else {
            Logger.log("Отменено удаление заказа #" + orderId);
        }
    }
}