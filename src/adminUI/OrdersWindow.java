package adminUI;

import model.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdersWindow extends JFrame {
    private User user;
    private DefaultTableModel tableModel;
    private JTable ordersTable;
    private JTextField searchField;

    public OrdersWindow(User user) {
        this.user = user;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Управление заказами");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        createMenuBar();
        createMainPanel();
        loadOrderData("");
    }

    private void createMenuBar() {
        setJMenuBar(new CommonMenuBar(
                e -> dispose(),
                e -> openAdminWindow(),
                e -> {}
        ));
    }

    private void createMainPanel() {

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        JButton searchButton = new JButton("Поиск");

        searchButton.addActionListener(e -> loadOrderData(searchField.getText()));
        searchField.addActionListener(e -> loadOrderData(searchField.getText()));

        searchPanel.add(new JLabel(" Поиск: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);


        String[] columnNames = {"ID заказа", "ID клиента", "Имя клиента", "Дата заказа",
                "Сумма", "Статус", "Последнее обновление"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: case 1: return Integer.class;
                    case 4: return Double.class;
                    case 3: case 6: return Timestamp.class;
                    default: return String.class;
                }
            }
        };

        ordersTable = new JTable(tableModel);
        ordersTable.setAutoCreateRowSorter(true);
        ordersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollPane = new JScrollPane(ordersTable);


        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("Обновить");
        JButton detailsButton = new JButton("Детали заказа");
        JButton statusButton = new JButton("Изменить статус");
        JButton closeButton = new JButton("Закрыть");

        refreshButton.addActionListener(e -> loadOrderData(""));
        detailsButton.addActionListener(e -> showOrderDetails());
        statusButton.addActionListener(e -> changeOrderStatus());
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(refreshButton);
        buttonPanel.add(detailsButton);
        buttonPanel.add(statusButton);
        buttonPanel.add(closeButton);


        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
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

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заказ для просмотра",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
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
    }

    private void changeOrderStatus() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заказ для изменения статуса",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 5);

        String[] statusOptions = {"В обработке", "Подтвержден", "Отправлен", "Доставлен", "Отменен"};
        String newStatus = (String) JOptionPane.showInputDialog(this,
                "Выберите новый статус:",
                "Изменение статуса заказа #" + orderId,
                JOptionPane.PLAIN_MESSAGE,
                null,
                statusOptions,
                currentStatus);

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            updateOrderStatus(orderId, newStatus);
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
                        "Статус заказа #" + orderId + " изменен на: " + newStatus,
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Заказ не найден",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка обновления статуса: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAdminWindow() {
        AdminWindow adminWindow = new AdminWindow(user);
        adminWindow.setVisible(true);
        dispose();
    }
}