package adminUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Dao_db.OrderDAO;
import DBobject.DBmanager;
import model.User;

public class OrdersWindow extends JFrame {
    private User user;
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable ordersTable;

    public OrdersWindow(User user) throws SQLException {
        this.user = user;
        initializeUI();
    }

    public void initializeUI() throws SQLException {
        setTitle("Управление заказами");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        cards.add(createOrdersPanel(), "ORDERS");

        add(cards, BorderLayout.CENTER);

        setJMenuBar(new CommonMenuBar(
                e -> dispose(),
                e -> openAdminWindow(),
                e -> cardLayout.show(cards, "ORDERS")
        ));
        cardLayout.show(cards, "ORDERS");
    }

    private JPanel createOrdersPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Управление заказами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


        OrderDAO orderDao = new OrderDAO(DBmanager.getConnection());
        List<Object[]> orders;
        try {
            orders = orderDao.getAllOrders();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки заказов: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            orders = new ArrayList<>();
        }

        //ryr
        String[] columnNames = {"ID заказа", "Клиент", "Дата", "Статус", "Общая стоимость"};
        Object[][] data = new Object[orders.size()][5];
        for (int i = 0; i < orders.size(); i++) {
            data[i][0] = orders.get(i)[0]; // order_id
            data[i][1] = orders.get(i)[1]; // client_name
            data[i][2] = orders.get(i)[2]; // order_date
            data[i][3] = orders.get(i)[3]; // status
            data[i][4] = orders.get(i)[4]; // total_cost
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(model);
        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton detailsButton = new JButton("Посмотреть детали");
        JButton updateStatusButton = new JButton("Изменить статус");


        detailsButton.addActionListener(e -> {
            int row = ordersTable.getSelectedRow();
            if (row >= 0) {
                int orderId = (int) ordersTable.getValueAt(row, 0);
                try {
                    OrderDAO dao = new OrderDAO(DBmanager.getConnection());
                    List<Object[]> items = dao.getOrderItems(orderId);
                    StringBuilder details = new StringBuilder("<html><b>Товары:</b><br>");
                    for (Object[] item : items) {
                        details.append(item[0]).append(" (Кол-во: ").append(item[1]).append(", Цена: ").append(item[2]).append(")<br>");
                    }
                    details.append("</html>");
                    JOptionPane.showMessageDialog(this, details.toString(), "Детали заказа: " + orderId, JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка загрузки деталей: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Выберите заказ!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            }
        });


        updateStatusButton.addActionListener(e -> {
            int row = ordersTable.getSelectedRow();
            if (row >= 0) {
                int orderId = (int) ordersTable.getValueAt(row, 0);
                String[] statuses = {"Новый", "В обработке", "Готов к выдаче", "Завершен"};
                String newStatus = (String) JOptionPane.showInputDialog(
                        this,
                        "Выберите новый статус:",
                        "Изменение статуса",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        statuses,
                        ordersTable.getValueAt(row, 3)
                );
                if (newStatus != null) {
                    try {
                        OrderDAO dao = new OrderDAO(DBmanager.getConnection());
                        dao.updateOrderStatus(orderId, newStatus);
                        model.setValueAt(newStatus, row, 3);
                        JOptionPane.showMessageDialog(this, "Статус обновлен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Ошибка обновления статуса: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Выберите заказ!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            }
        });

        buttonPanel.add(detailsButton);
        buttonPanel.add(updateStatusButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void openAdminWindow() {
        AdminWindow adminWindow = new AdminWindow(user);
        adminWindow.setVisible(true);
        dispose();
    }
}