package adminUI;

import DBobject.DBmanager;
import Dao_db.ProductDAO;
import Dao_db.OrderDAO;
import Dao_db.UserDAO;
import model.Product;
import model.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

// Этот класс больше не используется как отдельное окно, функционал перенесен в панель заказов в AdminWindow
public class OrdersWindow extends JFrame {
    private Connection connection;
    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private String currentUserRole;
    private String currentUsername;
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public OrdersWindow(Connection connection, String username, String role) {
        super("Управление заказами");
        this.connection = connection;
        this.orderDAO = new OrderDAO(connection);
        this.productDAO = new ProductDAO(connection);
        this.userDAO = new UserDAO(connection);
        this.currentUsername = username;
        this.currentUserRole = role;
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeUI();
        setupOrderManagement();
        logAction("Окно управления заказами открыто");
    }

    private void initializeUI() {
        setTitle("Управление заказами");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createMenuBar();
        createMainPanel();
        loadOrderData("");
    }

    private void createMenuBar() {
        CommonMenuBar menuBar = new CommonMenuBar(
            (e) -> dispose(),
            (e) -> openAdminWindow(),
            (e) -> {},
            (e) -> {}
        );
        setJMenuBar(menuBar);
    }

    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Создаем таблицу заказов
        String[] columnNames = {"ID заказа", "ID клиента", "Имя клиента", "Дата заказа",
                "Сумма", "Статус", "Последнее обновление"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(tableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollPane = new JScrollPane(ordersTable);

        // Панель с кнопками управления
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton changeStatusButton = new JButton("Изменить статус");
        JButton addProductButton = new JButton("Добавить товар");
        JButton editProductButton = new JButton("Редактировать товар");
        JButton refreshButton = new JButton("Обновить");
        JButton detailsButton = new JButton("Детали заказа");
        JButton closeButton = new JButton("Закрыть");

        changeStatusButton.addActionListener(e -> changeOrderStatus());
        addProductButton.addActionListener(e -> showAddProductDialog());
        editProductButton.addActionListener(e -> showEditProductDialog());
        refreshButton.addActionListener(e -> loadOrderData(""));
        detailsButton.addActionListener(e -> showOrderDetails());
        closeButton.addActionListener(e -> dispose());

        controlPanel.add(changeStatusButton);
        controlPanel.add(addProductButton);
        controlPanel.add(editProductButton);
        controlPanel.add(refreshButton);
        controlPanel.add(detailsButton);
        controlPanel.add(closeButton);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        JButton searchButton = new JButton("Поиск");

        searchButton.addActionListener(e -> searchOrders());
        searchField.addActionListener(e -> searchOrders());

        searchPanel.add(new JLabel(" Поиск: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadOrderData(String searchQuery) {
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> data = new ArrayList<>();
                String url = "jdbc:sqlite:sklad.db";
                String sql = """
                    SELECT o.order_id, o.client_id, u.name as client_name, 
                           o.order_date, o.total_cost, o.status, o.last_updated
                    FROM Orders o
                    LEFT JOIN Users u ON o.client_id = u.user_id
                    """;

                if (!searchQuery.isEmpty()) {
                    sql += " WHERE o.order_id LIKE ? OR u.name LIKE ? OR o.status LIKE ?";
                }
                sql += " ORDER BY o.order_date DESC";

                try (Connection conn = DriverManager.getConnection(url);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    if (!searchQuery.isEmpty()) {
                        String searchParam = "%" + searchQuery + "%";
                        stmt.setString(1, searchParam);
                        stmt.setString(2, searchParam);
                        stmt.setString(3, searchParam);
                    }

                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        data.add(new Object[]{
                                rs.getInt("order_id"),
                                rs.getInt("client_id"),
                                rs.getString("client_name"),
                                rs.getTimestamp("order_date"),
                                rs.getDouble("total_cost"),
                                rs.getString("status"),
                                rs.getTimestamp("last_updated")
                        });
                    }
                } catch (SQLException e) {
                    throw new Exception("Ошибка базы данных: " + e.getMessage());
                }
                return data;
            }

            @Override
            protected void done() {
                try {
                    tableModel.setRowCount(0);
                    List<Object[]> rows = get();
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }

                    // Auto-resize columns after data loaded
                    for (int i = 0; i < ordersTable.getColumnCount(); i++) {
                        ordersTable.getColumnModel().getColumn(i).setPreferredWidth(150);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(OrdersWindow.this,
                            "Ошибка загрузки данных: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void changeOrderStatus() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            String[] statuses = {"Новый", "В обработке", "Отправлен", "Доставлен", "Отменен"};
            String newStatus = (String) JOptionPane.showInputDialog(this, "Выберите новый статус:", "Изменить статус", JOptionPane.QUESTION_MESSAGE, null, statuses, statuses[0]);
            if (newStatus != null) {
                updateOrderStatus(orderId, newStatus);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для изменения статуса");
        }
    }

    private void updateOrderStatus(int orderId, String newStatus) {
        String url = "jdbc:sqlite:sklad.db";
        String sql = "UPDATE Orders SET status = ?, last_updated = CURRENT_TIMESTAMP WHERE order_id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                loadOrderData(searchField.getText());
                JOptionPane.showMessageDialog(this,
                        "Статус заказа обновлен",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
                logAction("Изменен статус заказа " + orderId + " на " + newStatus);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Заказ не найден",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка обновления статуса: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Ошибка при изменении статуса заказа " + orderId + ": " + e.getMessage());
        }
    }

    private void showAddProductDialog() {
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
            String name = nameField.getText();
            String description = descField.getText();
            try {
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = categoryField.getText();

                // Здесь должна быть логика добавления товара в базу данных
                JOptionPane.showMessageDialog(dialog, "Товар успешно добавлен");
                logAction("Добавлен новый товар: " + name);
                dialog.dispose();
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
        logAction("Открыт диалог добавления товара");
    }

    private void showEditProductDialog() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            JDialog dialog = new JDialog(this, "Редактировать товар", true);
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
                String name = nameField.getText();
                String description = descField.getText();
                try {
                    double price = Double.parseDouble(priceField.getText());
                    int quantity = Integer.parseInt(quantityField.getText());
                    String category = categoryField.getText();

                    // Здесь должна быть логика обновления товара в базе данных
                    JOptionPane.showMessageDialog(dialog, "Товар успешно обновлен");
                    logAction("Обновлен товар в заказе ID: " + orderId);
                    dialog.dispose();
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
            logAction("Открыт диалог редактирования товара в заказе " + orderId);
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для редактирования товара");
            logAction("Не выбран заказ для редактирования товара");
        }
    }

    private void logAction(String action) {
        try {
            FileWriter fw = new FileWriter("actions.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            out.println(timestamp + " - " + currentUsername + " - " + action);
            out.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
        }
    }

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заказ для просмотра",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            logAction("Не выбран заказ для просмотра");
            return;
        }

        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        int clientId = (int) tableModel.getValueAt(selectedRow, 1);
        String clientName = (String) tableModel.getValueAt(selectedRow, 2);
        Timestamp orderDate = (Timestamp) tableModel.getValueAt(selectedRow, 3);
        double totalCost = (double) tableModel.getValueAt(selectedRow, 4);
        String status = (String) tableModel.getValueAt(selectedRow, 5);
        Timestamp lastUpdated = (Timestamp) tableModel.getValueAt(selectedRow, 6);


        String message = String.format(
                "<html><b>Детали заказа #%d</b><br><br>" +
                        "<b>Клиент:</b> %s (ID: %d)<br>" +
                        "<b>Дата заказа:</b> %s<br>" +
                        "<b>Сумма:</b> %.2f руб.<br>" +
                        "<b>Статус:</b> %s<br>" +
                        "<b>Последнее обновление:</b> %s",
                orderId, clientName, clientId, orderDate, totalCost, status, lastUpdated
        );

        JOptionPane.showMessageDialog(this,
                message,
                "Детали заказа #" + orderId,
                JOptionPane.INFORMATION_MESSAGE);
        logAction("Просмотр деталей заказа " + orderId);
    }

    private void openAdminWindow() {
        dispose();
        logAction("Закрытие окна управления заказами");
    }

    private void setupOrderManagement() {
        JButton changeStatusButton = new JButton("Изменить статус");
        changeStatusButton.addActionListener(e -> changeOrderStatus());
        
        JButton viewDetailsButton = new JButton("Просмотреть детали");
        viewDetailsButton.addActionListener(e -> viewOrderDetails());
        
        JButton addOrderButton = new JButton("Добавить заказ");
        addOrderButton.addActionListener(e -> showAddOrderDialog());
        
        JButton addItemButton = new JButton("Добавить товар в заказ");
        addItemButton.addActionListener(e -> showAddItemDialog());
        
        JButton editItemButton = new JButton("Редактировать товар");
        editItemButton.addActionListener(e -> showEditItemDialog());
        
        JButton manageUsersButton = new JButton("Управление пользователями");
        manageUsersButton.addActionListener(e -> showManageUsersDialog());
        
        JButton deleteOrderButton = new JButton("Удалить заказ");
        deleteOrderButton.addActionListener(e -> deleteOrder());
        
        JButton processOrderButton = new JButton("Обработать заказ");
        processOrderButton.addActionListener(e -> processOrder());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 4, 5, 5));
        buttonPanel.add(changeStatusButton);
        buttonPanel.add(viewDetailsButton);
        buttonPanel.add(addOrderButton);
        buttonPanel.add(addItemButton);
        buttonPanel.add(editItemButton);
        buttonPanel.add(manageUsersButton);
        buttonPanel.add(deleteOrderButton);
        buttonPanel.add(processOrderButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void deleteOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите удалить заказ №" + orderId + "?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    orderDAO.deleteOrder(orderId);
                    loadOrderData("");
                    JOptionPane.showMessageDialog(this, "Заказ удален.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    logAction("Удален заказ №" + orderId);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении заказа: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    logAction("Ошибка при удалении заказа №" + orderId + ": " + ex.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для удаления.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Не выбран заказ для удаления");
        }
    }

    private void processOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            String currentStatus = (String) ordersTable.getValueAt(selectedRow, 5);
            if (currentStatus.equals("Обработан") || currentStatus.equals("Отправлен")) {
                JOptionPane.showMessageDialog(this, "Этот заказ уже обработан или отправлен.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                logAction("Ошибка при обработке заказа " + orderId + ": заказ уже обработан или отправлен");
                return;
            }
            JDialog dialog = new JDialog(this, "Обработать заказ " + orderId, true);
            dialog.setSize(400, 200);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridLayout(3, 2, 10, 10));
            
            JLabel statusLabel = new JLabel("Новый статус:");
            String[] statuses = {"В обработке", "Обработан", "Отправлен"};
            JComboBox<String> statusCombo = new JComboBox<>(statuses);
            
            JButton saveButton = new JButton("Сохранить");
            saveButton.addActionListener(e -> {
                String newStatus = (String) statusCombo.getSelectedItem();
                try {
                    orderDAO.updateOrderStatus(orderId, newStatus);
                    JOptionPane.showMessageDialog(dialog, "Статус заказа обновлен на " + newStatus, "Успех", JOptionPane.INFORMATION_MESSAGE);
                    logAction("Статус заказа " + orderId + " обновлен на " + newStatus);
                    loadOrdersData();
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка при обновлении статуса: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    logAction("Ошибка при обработке заказа " + orderId + ": " + ex.getMessage());
                }
            });
            
            dialog.add(statusLabel);
            dialog.add(statusCombo);
            dialog.add(new JLabel()); // Placeholder
            dialog.add(saveButton);
            
            dialog.setVisible(true);
            logAction("Открыт диалог обработки заказа " + orderId);
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для обработки.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Не выбран заказ для обработки");
        }
    }

    private void viewOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            try {
                List<Object[]> items = orderDAO.getOrderItems(orderId);
                if (items.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "У этого заказа нет товаров.", "Детали заказа", JOptionPane.INFORMATION_MESSAGE);
                    logAction("Просмотр деталей заказа " + orderId + ": нет товаров");
                    return;
                }
                
                StringBuilder details = new StringBuilder("Товары в заказе " + orderId + ":\n");
                for (Object[] item : items) {
                    details.append("ID товара: ").append(item[0]).append(", Название: ").append(item[1]).append(", Количество: ").append(item[2]).append("\n");
                }
                JOptionPane.showMessageDialog(this, details.toString(), "Детали заказа", JOptionPane.INFORMATION_MESSAGE);
                logAction("Просмотр деталей заказа " + orderId);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при получении деталей заказа: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                logAction("Ошибка при просмотре деталей заказа " + orderId + ": " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для просмотра деталей.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Не выбран заказ для просмотра деталей");
        }
    }

    private void showAddOrderDialog() {
        JDialog dialog = new JDialog(this, "Добавить заказ", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        
        JLabel clientIdLabel = new JLabel("ID клиента:");
        JTextField clientIdField = new JTextField();
        JLabel statusLabel = new JLabel("Статус:");
        JTextField statusField = new JTextField("Новый");
        JLabel totalLabel = new JLabel("Общая сумма:");
        JTextField totalField = new JTextField("0.0");
        
        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            try {
                int clientId = Integer.parseInt(clientIdField.getText());
                String status = statusField.getText();
                double total = Double.parseDouble(totalField.getText());
                if (status.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Статус не может быть пустым.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    logAction("Ошибка при добавлении заказа: статус пустой");
                    return;
                }
                orderDAO.addOrder(clientId, total, status);
                loadOrdersData();
                JOptionPane.showMessageDialog(dialog, "Заказ добавлен.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                logAction("Добавлен новый заказ для клиента " + clientId + " со статусом " + status);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "ID клиента и общая сумма должны быть числами.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                logAction("Ошибка при добавлении заказа: неверный формат чисел");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Ошибка при добавлении заказа: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                logAction("Ошибка при добавлении заказа: " + ex.getMessage());
            }
        });
        
        dialog.add(clientIdLabel);
        dialog.add(clientIdField);
        dialog.add(statusLabel);
        dialog.add(statusField);
        dialog.add(totalLabel);
        dialog.add(totalField);
        dialog.add(new JLabel()); // Placeholder
        dialog.add(saveButton);
        
        dialog.setVisible(true);
        logAction("Открыт диалог добавления заказа");
    }

    private void loadOrdersData() {
        try {
            tableModel.setRowCount(0);
            List<Object[]> orders = orderDAO.getAllOrders();
            for (Object[] order : orders) {
                tableModel.addRow(order);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке данных заказов: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Ошибка при загрузке данных заказов: " + ex.getMessage());
        }
    }

    private void searchOrders() {
        String query = searchField.getText().trim();
        try {
            tableModel.setRowCount(0);
            List<Object[]> orders;
            if (query.isEmpty()) {
                orders = orderDAO.getAllOrders();
            } else {
                // Assuming search by client ID or order ID
                try {
                    int id = Integer.parseInt(query);
                    orders = new ArrayList<>();
                    Object[] order = orderDAO.getOrderById(id);
                    if (order != null) {
                        orders.add(order);
                    } else {
                        orders = orderDAO.getClientOrders(id);
                    }
                } catch (NumberFormatException ex) {
                    // Search by status
                    orders = orderDAO.getOrdersByStatus(query);
                }
            }
            for (Object[] order : orders) {
                tableModel.addRow(order);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при поиске заказов: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logAction("Ошибка при поиске заказов: " + ex.getMessage());
        }
    }

    private void showAddItemDialog() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для добавления товара", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        
        JTextField productIdField = new JTextField(5);
        JTextField quantityField = new JTextField(5);
        
        JPanel myPanel = new JPanel();
        myPanel.add(new JLabel("ID товара:"));
        myPanel.add(productIdField);
        myPanel.add(Box.createHorizontalStrut(15));
        myPanel.add(new JLabel("Количество:"));
        myPanel.add(quantityField);
        
        int result = JOptionPane.showConfirmDialog(null, myPanel, 
                "Добавить товар в заказ #" + orderId, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int productId = Integer.parseInt(productIdField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                
                // Check if product exists
                Product product = productDAO.getProductById(productId);
                if (product == null) {
                    JOptionPane.showMessageDialog(this, "Товар с ID " + productId + " не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Количество должно быть больше 0", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Add item to order
                orderDAO.addItemToOrder(orderId, productId, quantity);
                JOptionPane.showMessageDialog(this, "Товар успешно добавлен в заказ", "Успех", JOptionPane.INFORMATION_MESSAGE);
                logAction("Добавлен товар " + productId + " в заказ " + orderId + " в количестве " + quantity);
                
                // Refresh order details
                loadOrderData("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числовые значения", "Ошибка", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditItemDialog() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для редактирования товара", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        
        // Get order items
        try {
            List<Object[]> items = orderDAO.getOrderItems(orderId);
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "В этом заказе нет товаров для редактирования", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create a dialog to select item to edit
            String[] itemDescriptions = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                Object[] item = items.get(i);
                itemDescriptions[i] = "ID: " + item[0] + ", Товар ID: " + item[1] + ", Количество: " + item[2];
            }
            
            String selectedItemDesc = (String) JOptionPane.showInputDialog(this, 
                    "Выберите товар для редактирования:", 
                    "Редактировать товар в заказе #" + orderId, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    itemDescriptions, 
                    itemDescriptions[0]);
            
            if (selectedItemDesc != null) {
                // Parse the selected item ID
                String itemIdStr = selectedItemDesc.split(",")[0].split(":")[1].trim();
                int itemId = Integer.parseInt(itemIdStr);
                
                // Find the item
                Object[] selectedItem = null;
                for (Object[] item : items) {
                    if ((int)item[0] == itemId) {
                        selectedItem = item;
                        break;
                    }
                }
                
                if (selectedItem != null) {
                    JTextField quantityField = new JTextField(5);
                    quantityField.setText(selectedItem[2].toString());
                    
                    JPanel myPanel = new JPanel();
                    myPanel.add(new JLabel("Новое количество:"));
                    myPanel.add(quantityField);
                    
                    int result = JOptionPane.showConfirmDialog(null, myPanel, 
                            "Редактировать товар ID " + selectedItem[1] + " в заказе #" + orderId, JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        try {
                            int newQuantity = Integer.parseInt(quantityField.getText());
                            if (newQuantity <= 0) {
                                JOptionPane.showMessageDialog(this, "Количество должно быть больше 0", "Ошибка", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            
                            orderDAO.updateOrderItemQuantity(itemId, newQuantity);
                            JOptionPane.showMessageDialog(this, "Количество товара обновлено", "Успех", JOptionPane.INFORMATION_MESSAGE);
                            logAction("Обновлено количество товара " + selectedItem[1] + " в заказе " + orderId + " на " + newQuantity);
                            loadOrderData("");
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректное числовое значение", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка базы данных: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showManageUsersDialog() {
        JOptionPane.showMessageDialog(this, "Функция управления пользователями пока недоступна в этом окне. Пожалуйста, используйте главное окно администратора.", "Информация", JOptionPane.INFORMATION_MESSAGE);
        logAction("Попытка открыть управление пользователями из окна заказов");
    }
}