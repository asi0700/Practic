package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import DBobject.DBmanager;
import Dao_db.OrderDAO;
import Dao_db.OrderItemDAO;
import Dao_db.ProductDAO;
import Dao_db.UserDAO;
import adminUI.CommonMenuBar;
import model.Order;
import model.OrderItem;
import model.Product;
import model.User;
import utils.Logger;
import utils.NotificationManager;
import utils.CameraManager;

public class ClientWindow extends JFrame {
    private User currentUser;
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable, ordersTable, cartTable;
    private JTextField searchField;
    private List<Object[]> cart = new ArrayList<>(); 
    private OrderDAO orderDAO;
    private OrderItemDAO orderItemDAO;
    private ProductDAO productDAO;
    private MainWindow mainWindow;
    private DefaultTableModel ordersTableModel;
    private final NotificationManager notificationManager;
    private Timer notificationTimer;

    public ClientWindow(User user, MainWindow mainWindow) {
        this.currentUser = user;
        this.mainWindow = this.mainWindow;
        this.cart = new ArrayList<>();
        this.notificationManager = NotificationManager.getInstance();
        
        try {
            Connection conn = DBmanager.getConnection();
            this.orderDAO = new OrderDAO(conn);
            this.orderItemDAO = new OrderItemDAO(conn);
            this.productDAO = new ProductDAO(conn);
        } catch (SQLException e) {
            Logger.logError("Ошибка при инициализации DAO", e);
            JOptionPane.showMessageDialog(this, "Ошибка при инициализации: " + e.getMessage());
        }


        notificationTimer = new Timer();
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkNotifications();
            }
        }, 0, 5000);

        try {
            initializeUI();
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    int choice = JOptionPane.showConfirmDialog(
                        ClientWindow.this,
                        "Вы уверены, что хотите выйти?",
                        "Подтверждение выхода",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        try {

                            if (orderDAO != null) orderDAO.close();
                            if (orderItemDAO != null) orderItemDAO.close();
                            if (productDAO != null) productDAO.close();
                            Logger.log("Окно клиента закрыто для пользователя: " + currentUser.getUsername());
                            dispose();
                            System.exit(0);
                        } catch (Exception ex) {
                            Logger.logError("Ошибка при закрытии окна клиента", ex);
                        }
                    }
                }
            });
        } catch (SQLException e) {
            Logger.logError("Ошибка инициализации UI", e);
            JOptionPane.showMessageDialog(this, "Ошибка инициализации: " + e.getMessage());
            System.err.println("Ошибка инициализации: " + e.getMessage());
        }


        SwingUtilities.invokeLater(() -> {
            try {
                checkPhotoRequest();
            } catch (Exception e) {
                Logger.logError("Ошибка при проверке запроса на pkh", e);
            }
        });
    }

    private void initializeUI() throws SQLException {
        setTitle("Склад (Пользовательский режим) - " + currentUser.getName());
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);


        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                    ClientWindow.this,
                    "Вы уверены, что хотите выйти?",
                    "Подтверждение выхода",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (choice == JOptionPane.YES_OPTION) {
                    try {

                        if (orderDAO != null) orderDAO.close();
                        if (orderItemDAO != null) orderItemDAO.close();
                        if (productDAO != null) productDAO.close();
                        Logger.log("Окно клиента закрыто для пользователя: " + currentUser.getUsername());
                        dispose();
                        System.exit(0);
                    } catch (Exception ex) {
                        Logger.logError("Ошибка при закрытии окна клиента", ex);
                    }
                }
            }
        });

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
            (e) -> {
                int choice = JOptionPane.showConfirmDialog(
                    ClientWindow.this,
                    "Вы уверены, что хотите выйти из аккаунта?",
                    "Подтверждение выхода",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (choice == JOptionPane.YES_OPTION) {
                    try {

                        if (orderDAO != null) orderDAO.close();
                        if (orderItemDAO != null) orderItemDAO.close();
                        if (productDAO != null) productDAO.close();
                        Logger.log("Выход пользователя: " + currentUser.getUsername());
                        dispose();

                        SwingUtilities.invokeLater(() -> {
                            registration registration = new registration();
                            registration.setVisible(true);
                        });
                    } catch (Exception ex) {
                        Logger.logError("Ошибка при выходе", ex);
                    }
                }
            },
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

        JPanel totalProductsCard = createStatCard("Всего товаров", String.valueOf(getTotalProducts()));
        JPanel userOrdersCard = createStatCard("Ваши заказы", String.valueOf(getUserOrdersCount()));
        
        statsPanel.add(totalProductsCard);
        statsPanel.add(userOrdersCard);

        panel.add(statsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            try {
                ((JLabel)totalProductsCard.getComponent(1)).setText(String.valueOf(getTotalProducts()));
                ((JLabel)userOrdersCard.getComponent(1)).setText(String.valueOf(getUserOrdersCount()));
                Logger.log("Статистика на главной панели обновлена");
            } catch (SQLException ex) {
                Logger.logError("Ошибка при обновлении статистики", ex);
                JOptionPane.showMessageDialog(this, "Ошибка при обновлении статистики: " + ex.getMessage());
            }
        });

        JButton browseProductsButton = new JButton("Просмотреть товары");
        browseProductsButton.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));

        buttonPanel.add(refreshButton);
        buttonPanel.add(browseProductsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private int getTotalProducts() throws SQLException {
        return productDAO.getAllProducts().size();
    }

    private int getUserOrdersCount() throws SQLException {
        return orderDAO.getClientOrders(currentUser.getUserid()).size();
    }

    private JPanel createProductsPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Товары на складе", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> filterTable());
        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);

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

        // тут  ширина столбцов и да не убери комментарии тут
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

        loadProductsData();


        JPanel buttonPanel = new JPanel();
        JButton addToCartButton = new JButton("Добавить в корзину");
        addToCartButton.addActionListener(e -> {
            int selectedRow = productsTable.getSelectedRow();
            if (selectedRow != -1) {
                int quantity = Integer.parseInt(JOptionPane.showInputDialog(this, "Введите количество", 1));
                if (quantity > 0) {
                    int productId = Integer.parseInt(productsTable.getValueAt(selectedRow, 0).toString());
                    Object[] product = new Object[]{
                        productId,
                        productsTable.getValueAt(selectedRow, 1),
                        quantity,
                        productsTable.getValueAt(selectedRow, 3)
                    };
                    cart.add(product);
                    updateCartTable();
                }
            }
        });
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
        String[] columnNames = {"ID заказа", "Сумма", "Статус", "Дата заказа", "Последнее обновление", "Товары"};
        ordersTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ordersTable = new JTable(ordersTableModel);
        ordersTable.setRowHeight(60);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        loadOrdersData();
        return panel;
    }

    private void loadOrdersData() {
        try {
            List<Order> orders = orderDAO.getClientOrders(currentUser.getUserid());
            DefaultTableModel model = (DefaultTableModel) ordersTable.getModel();
            model.setRowCount(0);
            for (Order order : orders) {

                List<OrderItem> items = orderDAO.getOrderItems(order.getOrderId());
                StringBuilder itemsInfo = new StringBuilder();
                for (OrderItem item : items) {
                    itemsInfo.append("Товар ID: ").append(item.getProductId())
                            .append(" (Кол-во: ").append(item.getQuantity())
                            .append(", Цена: ").append(item.getPrice())
                            .append(")\n");
                }

                model.addRow(new Object[]{
                    order.getOrderId(),
                    String.format("%.2f ₽", order.getTotalCost()),
                    order.getStatus(),
                    order.getOrderDate(),
                    order.getLastUpdated(),
                    itemsInfo.toString()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке заказов: " + e.getMessage());
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
        checkoutButton.addActionListener(e -> checkout());
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
        JPanel card = new JPanel(new GridLayout(2, 1, 5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        
        card.add(titleLabel);
        card.add(valueLabel);
        
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

    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Корзина пуста!");
            return;
        }


        JDialog deliveryDialog = new JDialog(this, "Данные доставки", true);
        deliveryDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField cityField = new JTextField(20);
        JTextField addressField = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0;
        deliveryDialog.add(new JLabel("Город:"), gbc);
        gbc.gridx = 1;
        deliveryDialog.add(cityField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        deliveryDialog.add(new JLabel("Адрес:"), gbc);
        gbc.gridx = 1;
        deliveryDialog.add(addressField, gbc);

        JButton confirmButton = new JButton("Подтвердить");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        deliveryDialog.add(confirmButton, gbc);

        confirmButton.addActionListener(e -> {
            String city = cityField.getText().trim();
            String address = addressField.getText().trim();

            if (city.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(deliveryDialog, "Пожалуйста, заполните все поля!");
                return;
            }

            try {
                Connection conn = DBmanager.getConnection();
                conn.setAutoCommit(false);

                OrderDAO orderDAO = new OrderDAO(conn);
                OrderItemDAO orderItemDAO = new OrderItemDAO(conn);
                ProductDAO productDAO = new ProductDAO(conn);

                Order order = new Order();
                order.setClientId(currentUser.getUserid());
                order.setOrderDate(new Date());
                order.setTotalCost(calculateTotal());
                order.setStatus("Новый");
                order.setLastUpdated(new Date());
                order.setDeliveryCity(city);
                order.setDeliveryAddress(address);

                orderDAO.addOrder(order);

                for (Object[] item : cart) {
                    int productId = (Integer) item[0];
                    int quantity = (Integer) item[2];
                    double price = (Double) item[3];
                    
                    OrderItem orderItem = new OrderItem(
                        order.getOrderId(),
                        productId,
                        quantity,
                        price
                    );
                    orderItemDAO.addOrderItem(orderItem);

                    Product product = productDAO.getProductById(productId);
                    if (product != null) {
                        product.setQuantity(product.getQuantity() - quantity);
                        productDAO.updateProduct(product);
                    }
                }

                conn.commit();

                cart.clear();
                updateCartTable();

                JOptionPane.showMessageDialog(this, "Заказ успешно оформлен!");
                deliveryDialog.dispose();
            } catch (SQLException ex) {
                try {
                    Connection conn = DBmanager.getConnection();
                    conn.rollback();
                } catch (SQLException ignore) {}
                JOptionPane.showMessageDialog(this, "Ошибка при оформлении заказа: " + ex.getMessage());
            }
        });

        deliveryDialog.pack();
        deliveryDialog.setLocationRelativeTo(this);
        deliveryDialog.setVisible(true);
    }

    private double calculateTotal() {
        double total = 0.0;
        for (Object[] item : cart) {
            total += (int)item[2] * (double)item[3]; // quantity * price
        }
        return total;
    }

    private void checkPhotoRequest() {
        try (UserDAO userDAO = new UserDAO(DBmanager.getConnection())) {
            if (userDAO.isPhotoRequest(currentUser.getUserid())) {

                String sql = "SELECT u1.role FROM Users u1 " +
                           "JOIN Users u2 ON u2.user_id = ? " +
                           "WHERE u1.role = 'admin_ph'";
                try (java.sql.PreparedStatement stmt = DBmanager.getConnection().prepareStatement(sql)) {
                    stmt.setInt(1, currentUser.getUserid());
                    java.sql.ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int result = JOptionPane.showConfirmDialog(
                                this,
                                "Администратор запрашивает ваше согласие на обработку ваших данных",
                                "Согласие на обработку ваших данных",
                                JOptionPane.YES_NO_OPTION
                        );
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
                                webcam.open();
                                java.awt.image.BufferedImage image = webcam.getImage();
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                javax.imageio.ImageIO.write(image, "JPG", baos);
                                byte[] photoBytes = baos.toByteArray();
                                webcam.close();
                                userDAO.updateUserPhoto(currentUser.getUserid(), photoBytes);
                                userDAO.setPhotoRequest(currentUser.getUserid(), false);
                                JOptionPane.showMessageDialog(this, "Спасибо за сотрудничество!");
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(this, "Ошибка при создании : " + e.getMessage());
                            }
                        } else {
                            userDAO.setPhotoRequest(currentUser.getUserid(), false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке photo_request: " + e.getMessage());
        }
    }

    public void showDashboard() {
        cardLayout.show(cards, "DASHBOARD");
        try {
            JPanel dashboardPanel = createDashboardPanel();
            cards.remove(cards.getComponent(0));
            cards.add(dashboardPanel, "DASHBOARD", 0);
            cardLayout.show(cards, "DASHBOARD");
        } catch (SQLException e) {
            Logger.logError("Ошибка при обновлении главной панели", e);
            JOptionPane.showMessageDialog(this, "Ошибка при обновлении главной панели: " + e.getMessage());
        }
    }

    private void checkNotifications() {
        List<NotificationManager.Notification> notifications = 
            notificationManager.getUserNotifications(currentUser.getUserid());
        
        if (!notifications.isEmpty()) {
            for (NotificationManager.Notification notification : notifications) {
                SwingUtilities.invokeLater(() -> {
                    NotificationDialog dialog = new NotificationDialog(
                        this,
                        notification,
                        currentUser.getUserid()
                    );
                    dialog.setVisible(true);
                    
                    if (dialog.getResponse()) {
                        if (notification.getType() == NotificationManager.NotificationType.PHOTO_REQUEST) {
                            handlePhotoRequest();
                        } else if (notification.getType() == NotificationManager.NotificationType.CAMERA_ACCESS) {
                            handleCameraAccess();
                        }
                    }
                });
            }
            notificationManager.clearNotifications(currentUser.getUserid());
        }
    }

    private void handlePhotoRequest() {
        // TODO: Реализовать отправку ph
        JOptionPane.showMessageDialog(
            this,
            "Функция отправки ph будет реализована в следующем обновлении НАДЕЮСЬ!!!!",
            "Информация",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleCameraAccess() {
        if (notificationManager.getCameraAccessStatus(currentUser.getUserid())) {
            SwingUtilities.invokeLater(() -> {
                CameraStreamWindow cameraWindow = new CameraStreamWindow(currentUser.getUserid());
                cameraWindow.setVisible(true);
            });
        }
    }

    @Override
    public void dispose() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }

        CameraManager.getInstance().dispose();
        super.dispose();
    }
}