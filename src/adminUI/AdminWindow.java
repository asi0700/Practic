package adminUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

import Dao_db.ProductDAO;
import Dao_db.AddUser;
import DBobject.DBmanager;
import model.Product;
import model.User;

public class AdminWindow extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTextField searchField;
    private JButton searchButton;
    private User user;

    public AdminWindow(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        this.user = user;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Управление складом");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        createMenuBar();

        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createOrdersPanel(), "ORDERS");

        add(cards, BorderLayout.CENTER);

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        setJMenuBar(new CommonMenuBar(
                e -> dispose(), // Выход
                e -> cardLayout.show(cards, "PRODUCTS"), // Товары
                e -> cardLayout.show(cards, "ORDERS") // Заказы
        ));
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
                data[i][0] = product.getProduct_id();
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

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Управление заказами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        panel.add(new JLabel("Функционал заказов в разработке"), BorderLayout.CENTER);

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

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Добавить товар", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 550);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField[] fields = new JTextField[10];
        String[] labels = {"ID:", "Наименование:", "Описание:", "Количество:", "Цена:", "Поставщик:", "Дата добавления:", "Кто добавил:", "Дата изменения:", "Кто изменил:"};

        for (int i = 0; i < fields.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            dialog.add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            fields[i] = new JTextField(15);
            if (i == 0) fields[i].setText("0"); // ID будет сгенерирован
            if (i == 7 || i == 9) {
                fields[i].setText(user.getName());
                fields[i].setEditable(false);
            }
            dialog.add(fields[i], gbc);
        }

        JButton saveButton = new JButton("Сохранить");
        gbc.gridx = 1;
        gbc.gridy = fields.length;
        gbc.anchor = GridBagConstraints.EAST;
        dialog.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                Product product = new Product(
                        0,
                        fields[1].getText(), // name
                        fields[2].getText(), // description
                        Integer.parseInt(fields[3].getText()), // quantity
                        Double.parseDouble(fields[4].getText()), // price
                        fields[5].getText(), // supplier
                        user.getUserid(),
                        fields[6].getText(),
                        user.getUserid(),
                        fields[8].getText()
                );

                try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
                     AddUser addUser = new AddUser(DBmanager.getConnection())) {
                    productDao.addProduct(product);
                    User addedByUser = addUser.findById(product.getAdded_by());
                    User modifiedByUser = addUser.findById(product.getModified_by());
                    DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
                    model.addRow(new Object[]{
                            product.getProduct_id(),
                            product.getName(),
                            product.getDescription(),
                            product.getQuantity(),
                            product.getPrice(),
                            product.getSupplier(),
                            product.getAdded_date(),
                            (addedByUser != null) ? addedByUser.getName() : "Неизвестно",
                            product.getModified_date(),
                            (modifiedByUser != null) ? modifiedByUser.getName() : "Неизвестно"
                    });
                } catch (SQLException ex) {
                    System.err.println("Ошибка добавления товара: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Ошибка добавления товара: " + ex.getMessage());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите корректные числовые значения для количества и цены.");
            }
        });

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
                if (p.getProduct_id() == productId) {
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
                Product product = new Product(
                        Integer.parseInt(fields[0].getText()),
                        fields[1].getText(),
                        fields[2].getText(),
                        Integer.parseInt(fields[3].getText()),
                        Double.parseDouble(fields[4].getText()),
                        fields[5].getText(),
                        finalOriginalProduct != null ? finalOriginalProduct.getAdded_by() : user.getUserid(),
                        fields[6].getText(),
                        user.getUserid(),
                        fields[8].getText()
                );

                try (ProductDAO productDao = new ProductDAO(DBmanager.getConnection());
                     AddUser addUser = new AddUser(DBmanager.getConnection())) {
                    productDao.updateProduct(product);
                    User addedByUser = addUser.findById(product.getAdded_by());
                    User modifiedByUser = addUser.findById(product.getModified_by());
                    model.setValueAt(product.getProduct_id(), selectedRow, 0);
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
}