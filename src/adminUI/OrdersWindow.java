package adminUI;

import DBobject.DBmanager;
import Dao_db.ProductDAO;
import Dao_db.OrderDAO;
import Dao_db.AddUser;
import model.Product;
import model.User;
import model.Order;
import model.OrderItem;
import ui.MainWindow;
import utils.Logger;

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

public class OrdersWindow extends JFrame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient Connection connection;
    private transient OrderDAO orderDAO;
    private transient ProductDAO productDAO;
    private transient AddUser userDAO;
    private String currentUserRole;
    private String currentUsername;
    private transient MainWindow mainWindow;
    private transient JTable ordersTable;
    private transient DefaultTableModel tableModel;
    private transient JTextField searchField;

    public OrdersWindow(Connection connection, String username, String role, MainWindow mainWindow) {
        super("Управление заказами");
        this.connection = connection;
        this.orderDAO = new OrderDAO(connection);
        this.productDAO = new ProductDAO(connection);
        this.userDAO = new AddUser(connection);
        this.currentUsername = username;
        this.currentUserRole = role;
        this.mainWindow = mainWindow;
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeUI();
        setupOrderManagement();
        Logger.log("Окно управления заказами открыто");
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
            this,
            (e) -> dispose(),
            (e) -> {},
            (e) -> {},
            (e) -> {},
            currentUserRole
        );
        setJMenuBar(menuBar);
    }

    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());


        String[] columnNames = {"ID заказа", "ID клиента", "Дата заказа", "Сумма", "Статус", "Город доставки", "Адрес доставки"};
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


        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton changeStatusButton = new JButton("Изменить статус");
        JButton refreshButton = new JButton("Обновить");
        JButton detailsButton = new JButton("Детали заказа");
        JButton closeButton = new JButton("Закрыть");

        changeStatusButton.addActionListener(e -> changeOrderStatus());
        refreshButton.addActionListener(e -> loadOrderData(""));
        detailsButton.addActionListener(e -> showOrderDetails());
        closeButton.addActionListener(e -> dispose());

        controlPanel.add(changeStatusButton);
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
        try {
            tableModel.setRowCount(0);
            List<Order> orders;
            
            if (searchQuery.isEmpty()) {
                orders = orderDAO.getAllOrders();
            } else {
                try {
                    int id = Integer.parseInt(searchQuery);
                    Order order = orderDAO.getOrderById(id);
                    if (order != null) {
                        orders = new ArrayList<>();
                        orders.add(order);
                    } else {
                        orders = orderDAO.getClientOrders(id);
                    }
                } catch (NumberFormatException ex) {
                    orders = orderDAO.getOrdersByStatus(searchQuery);
                }
            }

            for (Order order : orders) {
                tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getClientId(),
                    order.getOrderDate(),
                    String.format("%.2f ₽", order.getTotalCost()),
                    order.getStatus(),
                    order.getDeliveryCity(),
                    order.getDeliveryAddress()
                });
            }

            // Auto-resize columns after data loaded
            for (int i = 0; i < ordersTable.getColumnCount(); i++) {
                ordersTable.getColumnModel().getColumn(i).setPreferredWidth(150);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка загрузки данных: " + e.getMessage(),
                "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeOrderStatus() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            String[] statuses = {"Новый", "В обработке", "Отправлен", "Доставлен", "Отменен"};
            String newStatus = (String) JOptionPane.showInputDialog(this, 
                "Выберите новый статус:", 
                "Изменить статус", 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                statuses, 
                statuses[0]);
            
            if (newStatus != null) {
                try {
                    Order order = orderDAO.getOrderById(orderId);
                    if (order != null) {
                        order.setStatus(newStatus);
                        orderDAO.updateOrder(order);
                        loadOrderData(searchField.getText());
                        JOptionPane.showMessageDialog(this,
                            "Статус заказа обновлен",
                            "Успех", 
                            JOptionPane.INFORMATION_MESSAGE);
                        logAction("Изменен статус заказа " + orderId + " на " + newStatus);
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Заказ не найден",
                            "Ошибка", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this,
                        "Ошибка обновления статуса: " + e.getMessage(),
                        "Ошибка", 
                        JOptionPane.ERROR_MESSAGE);
                    logAction("Ошибка при изменении статуса заказа " + orderId + ": " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для изменения статуса");
        }
    }

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            showOrderDetails(orderId);
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для просмотра деталей");
        }
    }

    private void showOrderDetails(int orderId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order == null) {
                JOptionPane.showMessageDialog(this, "Заказ не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog(this, "Детали заказа #" + orderId, true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);


            String[] columnNames = {"ID", "Товар", "Количество", "Цена", "Сумма"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            JTable itemsTable = new JTable(model);
            double totalSum = 0;


            List<OrderItem> items = orderDAO.getOrderItems(orderId);
            
            for (OrderItem item : items) {
                double itemSum = item.getQuantity() * item.getPrice();
                totalSum += itemSum;
                model.addRow(new Object[]{
                    //item.getId(),
                    item.getProductId(),
                    item.getQuantity(),
                    String.format("%.2f ₽", item.getPrice()),
                    String.format("%.2f ₽", itemSum)
                });
            }

            model.addRow(new Object[]{"", "", "", "Итого:", String.format("%.2f ₽", totalSum)});

            JScrollPane scrollPane = new JScrollPane(itemsTable);
            dialog.add(scrollPane, BorderLayout.CENTER);


            JPanel infoPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            infoPanel.add(new JLabel("Статус:"));
            infoPanel.add(new JLabel(order.getStatus()));
            
            infoPanel.add(new JLabel("Дата заказа:"));
            infoPanel.add(new JLabel(order.getOrderDate().toString()));
            
            infoPanel.add(new JLabel("Город доставки:"));
            infoPanel.add(new JLabel(order.getDeliveryCity()));
            
            infoPanel.add(new JLabel("Адрес доставки:"));
            infoPanel.add(new JLabel(order.getDeliveryAddress()));

            dialog.add(infoPanel, BorderLayout.SOUTH);

            JButton closeButton = new JButton("Закрыть");
            closeButton.addActionListener(e -> dialog.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при получении деталей заказа: " + ex.getMessage(),
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchOrders() {
        String searchText = searchField.getText().trim();
        searchOrders(searchText);
    }

    private void searchOrders(String searchText) {
        try {
            tableModel.setRowCount(0);
            List<Order> orders;
            
            if (searchText.isEmpty()) {
                orders = orderDAO.getAllOrders();
            } else {
                try {
                    int id = Integer.parseInt(searchText);
                    Order order = orderDAO.getOrderById(id);
                    if (order != null) {
                        orders = new ArrayList<>();
                        orders.add(order);
                    } else {
                        orders = orderDAO.getClientOrders(id);
                    }
                } catch (NumberFormatException ex) {
                    orders = orderDAO.getOrdersByStatus(searchText);
                }
            }

            for (Order order : orders) {
                tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getClientId(),
                    order.getOrderDate(),
                    String.format("%.2f ₽", order.getTotalCost()),
                    order.getStatus(),
                    order.getDeliveryCity(),
                    order.getDeliveryAddress()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при поиске заказов: " + ex.getMessage(),
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logAction(String action) {
        try {
            String sql = "INSERT INTO Logs (user_id, action, timestamp) VALUES (?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, currentUsername);
                stmt.setString(2, action);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при логировании: " + e.getMessage());
        }
    }

    private void setupOrderManagement() {

    }
}