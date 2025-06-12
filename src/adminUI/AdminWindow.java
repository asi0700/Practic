package adminUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import Dao_db.ProductDAO;
import Dao_db.AddUser;
import Dao_db.OrderDAO;
import DBobject.DBmanager;
import model.Product;
import model.User;
import ui.LoginWindow;
import ui.MainWindow;
import adminUI.OrdersWindow; // Ensure OrdersWindow is correctly imported

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

    public AdminWindow(User user, MainWindow mainWindow) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        this.currentUser = user;
        this.mainWindow = mainWindow;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Администратор - Склад-Мастер");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Инициализация DAO для работы с заказами
        try {
            orderDAO = new OrderDAO(DBmanager.getConnection());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка инициализации OrderDAO: " + e.getMessage());
            System.err.println("Ошибка инициализации OrderDAO: " + e.getMessage());
        }

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Добавляем панели для различных вкладок
        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createOrdersPanel(), "ORDERS"); // Добавляем панель заказов

        add(cards);

        createMenuBar();

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        CommonMenuBar menuBar = new CommonMenuBar(
            (e) -> {
                dispose();
                logAction("Выход из аккаунта");
                new ui.LoginWindow().setVisible(true);
            },
            (e) -> cardLayout.show(cards, "PRODUCTS"), // Товары
            (e) -> openOrdersWindow(), // Заказы
            (e) -> showLogsWindow(), // Логи действий
            (e) -> {},
            "admin" // Explicitly set the role to admin
        );
        setJMenuBar(menuBar);
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

        String[] columnNames = {"ID", "Наименование", "Описание", "Количество", "Цена", "Поставщик", "Дата добавления", "Кто добавил", "Дата изменения", "Кто изменил"};
        DefaultTableModel model = new DefaultTableModel(loadProductsFromDB(), columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(model);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int i = 0; i < productsTable.getColumnCount(); i++) {
            productsTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        JScrollPane scrollPane = new JScrollPane(productsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Добавить");
        JButton editButton = new JButton("Редактировать");
        JButton deleteButton = new JButton("Удалить");
        JButton columnsButton = new JButton("Управление столбцами");
        JButton filterButton = new JButton("Фильтр");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteSelectedRow());
        columnsButton.addActionListener(e -> showColumnsManagementDialog());
        filterButton.addActionListener(e -> showFilterDialog());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(columnsButton);
        buttonPanel.add(filterButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Панель заказов", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        // Здесь можно добавить таблицу заказов и другие элементы управления
        ordersTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Кнопка возвращения к главному окну (опционально)
        JButton backButton = new JButton("Вернуться к главному меню");
        backButton.addActionListener(e -> returnToMainWindow());
        panel.add(backButton, BorderLayout.SOUTH);

        // Загрузка данных заказов (если есть метод загрузки)
        loadOrdersData();

        return panel;
    }

    private Object[][] loadProductsFromDB() {
        return loadProductsFromDB(null, null, null, null, null, null, null);
    }

    private Object[][] loadProductsFromDB(String name, String startDate, String endDate, String addedBy, String minQuantity, String maxQuantity, String supplier) {
        try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
             AddUser addUser = new AddUser(DBmanager.getConnection())) {
            List<Product> products = productDao.getProductsByFilters(name, startDate, endDate, addedBy, minQuantity, maxQuantity, supplier);
            Object[][] data = new Object[products.size()][10];
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                User addedByUser = addUser.findById(product.getAdded_by());
                User modifiedByUser = addUser.findById(product.getModified_by());
                data[i][0] = product.getId(); // Fix the getter method name for product ID
                data[i][1] = product.getName();
                data[i][2] = product.getDescription() != null ? product.getDescription() : "";
                data[i][3] = product.getQuantity();
                data[i][4] = product.getPrice();
                data[i][5] = product.getSupplier() != null ? product.getSupplier() : "";
                data[i][6] = product.getAdded_date() != null ? product.getAdded_date() : "";
                data[i][7] = (addedByUser != null) ? addedByUser.getName() : "Неизвестно";
                data[i][8] = product.getModified_date() != null ? product.getModified_date() : "";
                data[i][9] = (modifiedByUser != null) ? modifiedByUser.getName() : "Неизвестно";
            }
            return data;
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки товаров: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка загрузки товаров: " + e.getMessage());
            return new Object[][]{
                    {1, "Ноутбук Lenovo", "", 5, 45000.0, "ООО Техносила", "2023-10-15", "Иванов", "2023-10-15", "Иванов"},
                    {2, "Мышь Logitech", "", 20, 1200.0, "DNS", "2023-10-16", "Петров", "2023-10-16", "Петров"},
                    {3, "Клавиатура Razer", "", 8, 5600.0, "М.Видео", "2023-10-17", "Сидоров", "2023-10-17", "Сидоров"}
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Добавить товар", true);
        dialog.setSize(300, 400);
        dialog.setLayout(new GridLayout(6, 2));

        JLabel nameLabel = new JLabel("Название:");
        JTextField nameField = new JTextField();
        JLabel descLabel = new JLabel("Описание:");
        JTextField descField = new JTextField();
        JLabel priceLabel = new JLabel("Цена:");
        JTextField priceField = new JTextField();
        JLabel quantityLabel = new JLabel("Количество:");
        JTextField quantityField = new JTextField();
        JLabel categoryLabel = new JLabel("Категория:");
        JTextField categoryField = new JTextField();

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            // Validate product name is not empty
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Название товара не может быть пустым.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String name = nameField.getText();
            String description = descField.getText();
            try {
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = categoryField.getText();

                Product newProduct = new Product(0, name, price, description, "");
                try {
                    ProductDAO productDAO = new ProductDAO(DBmanager.getConnection());
                    productDAO.addProduct(newProduct);
                    JOptionPane.showMessageDialog(dialog, "Товар успешно добавлен");
                    loadProductsData();
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка при добавлении товара: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите корректные числовые значения для цены и количества");
            }
        });

        dialog.add(nameLabel);
        dialog.add(nameField);
        dialog.add(descLabel);
        dialog.add(descField);
        dialog.add(priceLabel);
        dialog.add(priceField);
        dialog.add(quantityLabel);
        dialog.add(quantityField);
        dialog.add(categoryLabel);
        dialog.add(categoryField);
        dialog.add(saveButton);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showEditDialog() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите строку для редактирования", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Редактировать товар", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 550);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField[] fields = new JTextField[10];
        String[] labels = {"ID:", "Наименование:", "Описание:", "Количество:", "Цена:", "Поставщик:", "Дата добавления:", "Кто добавил:", "Дата изменения:", "Кто изменил:"};
        DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
        int productId = (int) model.getValueAt(selectedRow, 0);

        Product originalProduct = null;
        try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection())) {
            List<Product> products = productDao.getAllProducts();
            for (Product p : products) {
                if (p.getId() == productId) { // Fix the getter method name for product ID
                    originalProduct = p;
                    break;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения продукта: " + e.getMessage());
            e.printStackTrace();
        }

        for (int i = 0; i < fields.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            dialog.add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            Object value = model.getValueAt(selectedRow, i);
            fields[i] = new JTextField(value != null ? value.toString() : "");
            if (i == 7 || i == 9) {
                fields[i].setEditable(false);
            }
            dialog.add(fields[i], gbc);
        }

        JButton saveButton = new JButton("Сохранить");
        gbc.gridx = 1;
        gbc.gridy = fields.length;
        gbc.anchor = GridBagConstraints.EAST;
        dialog.add(saveButton, gbc);

        Product finalOriginalProduct = originalProduct;
        saveButton.addActionListener(e -> {
            try {
                if (fields[1].getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Product name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Product product = new Product(
                        Integer.parseInt(fields[0].getText()),
                        fields[1].getText(),
                        Double.parseDouble(fields[4].getText()),
                        fields[2].getText(),
                        0, // Placeholder for categoryId
                        Integer.parseInt(fields[3].getText()),
                        fields[5].getText(),
                        finalOriginalProduct != null ? finalOriginalProduct.getAdded_by() : user.getUserid(),
                        fields[6].getText(),
                        user.getUserid(),
                        fields[7].getText()
                );

                try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
                     AddUser addUser = new AddUser(DBmanager.getConnection())) {
                    productDao.updateProduct(product);
                    User addedByUser = addUser.findById(product.getAdded_by());
                    User modifiedByUser = addUser.findById(product.getModified_by());
                    model.setValueAt(product.getId(), selectedRow, 0); // Fix the getter method name for product ID
                    model.setValueAt(product.getName(), selectedRow, 1);
                    model.setValueAt(product.getDescription(), selectedRow, 2);
                    model.setValueAt(product.getQuantity(), selectedRow, 3);
                    model.setValueAt(product.getPrice(), selectedRow, 4);
                    model.setValueAt(product.getSupplier(), selectedRow, 5);
                    model.setValueAt(product.getAdded_date(), selectedRow, 6);
                    model.setValueAt((addedByUser != null) ? addedByUser.getName() : "Неизвестно", selectedRow, 7);
                    model.setValueAt(product.getModified_date(), selectedRow, 8);
                    model.setValueAt((modifiedByUser != null) ? modifiedByUser.getName() : "Неизвестно", selectedRow, 9);
                } catch (SQLException ex) {
                    System.err.println("Ошибка редактирования товара: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Ошибка редактирования товара: " + ex.getMessage());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите корректные числовые значения для ID, количества и цены.");
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelectedRow() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите строку для удаления.");
            return;
        }

        int productId = Integer.parseInt(productsTable.getValueAt(selectedRow, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Удалить товар с ID " + productId + "?", "Подтверждение", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection())) {
                productDao.deleteProduct(productId);
                ((DefaultTableModel) productsTable.getModel()).removeRow(selectedRow);
            } catch (SQLException e) {
                System.err.println("Ошибка удаления товара: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ошибка удаления товара: " + e.getMessage());
            }
        }
    }

    private void showColumnsManagementDialog() {
        JDialog dialog = new JDialog(this, "Управление столбцами", true);
        dialog.setLayout(new BorderLayout());

        DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < model.getColumnCount(); i++) {
            listModel.addElement(model.getColumnName(i));
        }

        JList<String> columnsList = new JList<>(listModel);
        columnsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel controlPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        JButton addButton = new JButton("Добавить");
        JButton renameButton = new JButton("Переименовать");
        JButton removeButton = new JButton("Удалить");
        JButton upButton = new JButton("Вверх");
        JButton downButton = new JButton("Вниз");

        addButton.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(dialog, "Введите название нового столбца:");
            if (newName != null && !newName.trim().isEmpty()) {
                listModel.addElement(newName);
            }
        });

        renameButton.addActionListener(e -> {
            int selected = columnsList.getSelectedIndex();
            if (selected >= 0) {
                String newName = JOptionPane.showInputDialog(dialog, "Новое название:", listModel.getElementAt(selected));
                if (newName != null && !newName.trim().isEmpty()) {
                    listModel.set(selected, newName);
                }
            }
        });

        removeButton.addActionListener(e -> {
            int selected = columnsList.getSelectedIndex();
            if (selected >= 0 && listModel.size() > 1) {
                listModel.remove(selected);
            } else {
                JOptionPane.showMessageDialog(dialog, "Нельзя удалить последний столбец!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        upButton.addActionListener(e -> {
            int selected = columnsList.getSelectedIndex();
            if (selected > 0) {
                String item = listModel.remove(selected);
                listModel.add(selected - 1, item);
                columnsList.setSelectedIndex(selected - 1);
            }
        });

        downButton.addActionListener(e -> {
            int selected = columnsList.getSelectedIndex();
            if (selected >= 0 && selected < listModel.size() - 1) {
                String item = listModel.remove(selected);
                listModel.add(selected + 1, item);
                columnsList.setSelectedIndex(selected + 1);
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(e -> {
            updateTableColumns(listModel);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        controlPanel.add(addButton);
        controlPanel.add(renameButton);
        controlPanel.add(removeButton);
        controlPanel.add(upButton);
        controlPanel.add(downButton);

        bottomPanel.add(cancelButton);
        bottomPanel.add(saveButton);

        dialog.add(new JScrollPane(columnsList), BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.NORTH);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateTableColumns(DefaultListModel<String> newColumns) {
        DefaultTableModel oldModel = (DefaultTableModel) productsTable.getModel();
        DefaultTableModel newModel = new DefaultTableModel();

        for (int i = 0; i < newColumns.size(); i++) {
            newModel.addColumn(newColumns.getElementAt(i));
        }

        for (int row = 0; row < oldModel.getRowCount(); row++) {
            Object[] rowData = new Object[newColumns.size()];
            for (int col = 0; col < newColumns.size(); col++) {
                String colName = newColumns.getElementAt(col);
                int oldColIndex = -1;
                for (int i = 0; i < oldModel.getColumnCount(); i++) {
                    if (oldModel.getColumnName(i).equals(colName)) {
                        oldColIndex = i;
                        break;
                    }
                }
                rowData[col] = oldColIndex >= 0 ? oldModel.getValueAt(row, oldColIndex) : "";
            }
            newModel.addRow(rowData);
        }

        productsTable.setModel(newModel);

        for (int i = 0; i < productsTable.getColumnCount(); i++) {
            productsTable.getColumnModel().getColumn(i).setPreferredWidth(120);
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
        if (orderDAO == null) {
            JOptionPane.showMessageDialog(this, "OrderDAO не инициализирован. Проверьте подключение к базе данных.");
            try {
                orderDAO = new OrderDAO(DBmanager.getConnection());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка инициализации OrderDAO: " + e.getMessage());
                System.err.println("Ошибка инициализации OrderDAO: " + e.getMessage());
                return;
            }
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

    private void loadProductsData() {
        try {
            if (productDAO == null) {
                productDAO = new ProductDAO(DBmanager.getConnection());
            }
            List<Product> products = productDAO.getAllProducts();
            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"ID", "Название", "Описание", "Количество", "Цена"});

            for (Product product : products) {
                model.addRow(new Object[]{
                    product.getId(), // Fix the getter method name for product ID
                    product.getName(),
                    product.getDescription(),
                    product.getQuantity(),
                    product.getPrice()
                });
            }

            productsTable.setModel(model);
            System.out.println("Данные товаров загружены: " + products.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных товаров: " + e.getMessage());
            System.err.println("Ошибка загрузки данных товаров: " + e.getMessage());
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Войдите в свой аккаунт");
        dispose();
        new ui.LoginWindow().setVisible(true);
        logAction("Admin logged out: " + currentUser.getUsername());
    }

    private void logAction(String action) {
        try {
            mainWindow.logAction(action + " (Администратор: " + currentUser.getUsername() + ")");
        } catch (Exception e) {
            System.err.println("Ошибка логирования: " + e.getMessage());
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
}