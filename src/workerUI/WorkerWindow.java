package workerUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.stream.Collectors;

import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import DBobject.DBmanager;
import model.Product;
import model.User;
import ui.MainWindow;
import adminUI.CommonMenuBar;
import utils.Logger;
import model.Order;
import model.OrderItem;
import Dao_db.UserDAO;
import Dao_db.AddUser;

public class WorkerWindow extends JFrame {
    private Connection conn;
    private String username;
    private MainWindow mainWindow;
    private DefaultTableModel tasksTableModel;
    private JTable tasksTable;
    private DefaultTableModel productsTableModel;
    private JTable productsTable;
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    private CardLayout cardLayout;
    private JPanel cards;
    private UserDAO userDAO;

    public WorkerWindow(Connection conn, String username, MainWindow mainWindow) {
        this.conn = conn;
        this.username = username;
        this.mainWindow = mainWindow;
        this.orderDAO = new OrderDAO(conn);
        this.productDAO = new ProductDAO(conn);
        this.userDAO = new UserDAO(conn);
        initializeUI();
        Logger.log("Окно работника открыто для пользователя " + username);
        checkPhotoRequest();
    }
    
    private void initializeUI() {
        setTitle("Окно работника - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // MENU BAR ТУТ
        CommonMenuBar menuBar = new CommonMenuBar(
            this,
            (e) -> dispose(),
            (e) -> {
                cardLayout.show(cards, "ORDERS");
                loadTasksData("");
                Logger.log("Работник " + username + " перешел на вкладку заказов.");
            },
            (e) -> {
                cardLayout.show(cards, "PRODUCTS");
                loadProductsData("");
                Logger.log("Работник " + username + " перешел на вкладку товаров.");
            },
            (e) -> {},
            "worker"
        );
        setJMenuBar(menuBar);
        
        // добавление пан
        cards.add(createOrdersPanel(), "ORDERS");
        cards.add(createProductsPanel(), "PRODUCTS");
        add(cards);

        // Загружаем данные по умолчанию
        cardLayout.show(cards, "ORDERS");
        loadTasksData("");
    }
    
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("Заказы", SwingConstants.CENTER), BorderLayout.NORTH);
        
        // Initialize ТАБЛ
        String[] columnNames = {"ID заказа", "Клиент", "Дата заказа", "Сумма", "Статус", "Город доставки", "Адрес доставки", "Товары"};
        tasksTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tasksTable = new JTable(tasksTableModel);
        tasksTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(tasksTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // ДОБАВЛЕНИЕ
        tasksTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = tasksTable.getSelectedRow();
                    if (selectedRow != -1) {
                        int orderId = (int) tasksTableModel.getValueAt(selectedRow, 0);
                        showOrderDetails(orderId);
                    }
                }
            }
        });
        

        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton updateStatusButton = new JButton("Изменить статус");
        updateStatusButton.addActionListener(e -> updateOrderStatus());

        JButton addOrderButton = new JButton("Добавить заказ");
        addOrderButton.addActionListener(e -> showAddOrderDialog());

        JButton removeOrderItemButton = new JButton("Удалить товар из заказа");
        removeOrderItemButton.addActionListener(e -> showRemoveOrderItemDialog());

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadTasksData(""));

        buttonPanel.add(addOrderButton);
        buttonPanel.add(updateStatusButton);
        buttonPanel.add(removeOrderItemButton);
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
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

            // Создаем таблицу для отображения элементов заказа
            String[] columnNames = {"ID", "Товар", "Количество", "Цена", "Сумма"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            JTable itemsTable = new JTable(model);
            double totalSum = 0;

            // Получаем элементы заказа
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

            // Добавляем информацию о заказе
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

            // Добавляем кнопку закрытия
            JButton closeButton = new JButton("Закрыть");
            closeButton.addActionListener(e -> dialog.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
            Logger.log("Работник " + username + " просмотрел детали заказа ID: " + orderId);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при получении деталей заказа: " + ex.getMessage(),
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка при просмотре деталей заказа: " + ex.getMessage(), ex);
        }
    }

    private void updateOrderStatus() {
        int selectedRow = tasksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для обновления статуса.");
            return;
        }

        int orderId = (int) tasksTableModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) tasksTableModel.getValueAt(selectedRow, 4);

        String[] statuses = {"В обработке", "Подтвержден", "В пути", "Доставлен", "Отменен"};
        String newStatus = (String) JOptionPane.showInputDialog(
            this,
            "Выберите новый статус заказа:",
            "Обновление статуса",
            JOptionPane.QUESTION_MESSAGE,
            null,
            statuses,
            currentStatus
        );

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                orderDAO.updateOrderStatus(orderId, newStatus);
                loadTasksData(""); // Обновляем таблицу
                JOptionPane.showMessageDialog(this, "Статус заказа успешно обновлен!");
                Logger.log("Работник " + username + " изменил статус заказа " + orderId + " на " + newStatus);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при обновлении статуса: " + e.getMessage());
            }
        }
    }

    private void loadTasksData(String filter) {
        try {
            List<Order> orders = orderDAO.getAllOrders();
            tasksTableModel.setRowCount(0);
            for (Order order : orders) {
                // Получаем информацию о клиенте
                User client = userDAO.getUserById(order.getClientId());
                String clientInfo = client != null ? client.getUsername() : "Неизвестный клиент";

                // Получаем детали заказа
                List<OrderItem> items = orderDAO.getOrderItems(order.getOrderId());
                StringBuilder itemsInfo = new StringBuilder();
                for (OrderItem item : items) {
                    itemsInfo.append("Товар ID: ").append(item.getProductId())
                            .append(" (Кол-во: ").append(item.getQuantity())
                            .append(", Цена: ").append(item.getPrice())
                            .append(")\n");
                }

                tasksTableModel.addRow(new Object[]{
                    order.getOrderId(),
                    clientInfo,
                    order.getOrderDate(),
                    String.format("%.2f ₽", order.getTotalCost()),
                    order.getStatus(),
                    order.getDeliveryCity(),
                    order.getDeliveryAddress(),
                    itemsInfo.toString()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке заказов: " + e.getMessage());
        }
    }

    private void loadProductsData(String searchText) {
        try {
            List<Object[]> productData = productDAO.getAllProducts();
            List<Product> products = new ArrayList<>();
            for (Object[] row : productData) {
                Product product = new Product();
                product.setId((int)row[0]);
                product.setName((String)row[1]);
                product.setDescription((String)row[2]);
                product.setQuantity((int)row[3]);
                product.setPrice((double)row[4]);
                product.setSupplier((String)row[5]);
                products.add(product);
            }

            // Фильтр.ия по поисковому запросу
            if (!searchText.isEmpty()) {
                products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                               p.getDescription().toLowerCase().contains(searchText.toLowerCase()) ||
                               p.getSupplier().toLowerCase().contains(searchText.toLowerCase()))
                    .collect(Collectors.toList());
            }

            productsTableModel.setRowCount(0);
            for (Product product : products) {
                productsTableModel.addRow(new Object[]{
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getQuantity(),
                    product.getPrice(),
                    product.getSupplier()
                });
            }
            Logger.log("Работник " + username + " загрузил данные товаров: " + products.size() + " записей");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки товаров: " + e.getMessage());
            Logger.logError("Ошибка загрузки товаров для работника: " + e.getMessage(), e);
        }
    }

    private void filterProductsTable(String query) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(productsTableModel);
        productsTable.setRowSorter(sorter);
        if (query.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
        Logger.log("Работник " + username + " выполнил поиск товаров по запросу: " + query);
    }

    private void showAddOrderDialog() {
        JTextField clientIdField = new JTextField(5);
        JTextField totalCostField = new JTextField(5);
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Новый", "В обработке", "Выполнен", "Отменен"});

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("ID клиента:"));
        panel.add(clientIdField);
        panel.add(new JLabel("Общая сумма:"));
        panel.add(totalCostField);
        panel.add(new JLabel("Статус:"));
        panel.add(statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Добавить новый заказ",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int clientId = Integer.parseInt(clientIdField.getText());
                double totalCost = Double.parseDouble(totalCostField.getText());
                String status = (String) statusCombo.getSelectedItem();

                Order newOrder = new Order();
                newOrder.setClientId(clientId);
                newOrder.setTotalCost(totalCost);
                newOrder.setStatus(status);
                newOrder.setOrderDate(new Date());
                newOrder.setLastUpdated(new Date());

                orderDAO.addOrder(newOrder);
                int orderId = newOrder.getOrderId();
                JOptionPane.showMessageDialog(this, "Заказ успешно добавлен.");
                Logger.log("Работник " + username + " добавил новый заказ для клиента ID " + clientId);
                loadTasksData(""); // Обновить данные
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числовые значения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                Logger.logError("Ошибка добавления заказа (неверный формат числа): " + e.getMessage(), e);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка базы данных при добавлении заказа: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                Logger.logError("Ошибка базы данных при добавлении заказа: " + e.getMessage(), e);
            }
        }
    }

    private void showAddProductToOrderDialog() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите товар для добавления в заказ.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productsTableModel.getValueAt(selectedRow, 0);
        String productName = (String) productsTableModel.getValueAt(selectedRow, 1);
        double productPrice = (double) productsTableModel.getValueAt(selectedRow, 4);
        int availableQuantity = (int) productsTableModel.getValueAt(selectedRow, 3);

        JTextField quantityField = new JTextField(5);
        JTextField orderIdField = new JTextField(5);
        JTextField clientIdField = new JTextField(5);
        JCheckBox newOrderCheckBox = new JCheckBox("Создать новый заказ");

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        formPanel.add(new JLabel("Товар:"));
        formPanel.add(new JLabel(productName + " (ID: " + productId + ")"));
        formPanel.add(new JLabel("Доступно:"));
        formPanel.add(new JLabel(String.valueOf(availableQuantity)));
        formPanel.add(new JLabel("Количество для заказа:"));
        formPanel.add(quantityField);
        formPanel.add(new JLabel("------------------"));
        formPanel.add(new JLabel("------------------"));
        formPanel.add(newOrderCheckBox);
        formPanel.add(new JLabel("")); // Пустая ячейка для выравнивания

        formPanel.add(new JLabel("ID существующего заказа (если не новый):"));
        formPanel.add(orderIdField);
        formPanel.add(new JLabel("ID клиента (для нового заказа):"));
        formPanel.add(clientIdField);

        // Логика переключения полей
        newOrderCheckBox.addActionListener(e -> {
            boolean isNewOrder = newOrderCheckBox.isSelected();
            orderIdField.setEnabled(!isNewOrder);
            clientIdField.setEnabled(isNewOrder);
        });
        newOrderCheckBox.setSelected(true); // По умолчанию - создать новый заказ
        orderIdField.setEnabled(false);

        int result = JOptionPane.showConfirmDialog(this, formPanel, "Добавить товар в заказ", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity <= 0 || quantity > availableQuantity) {
                    JOptionPane.showMessageDialog(this, "Некорректное количество. Доступно: " + availableQuantity, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int orderId;

                if (newOrderCheckBox.isSelected()) {
                    int clientId = Integer.parseInt(clientIdField.getText());
                    if (clientId <= 0) {
                        JOptionPane.showMessageDialog(this, "ID клиента для нового заказа должен быть больше 0.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Создаем новый заказ со статусом "В обработке" и нулевой стоимостью
                    Order newOrder = new Order();
                    newOrder.setClientId(clientId);
                    newOrder.setTotalCost(0.0);
                    newOrder.setStatus("В обработке");
                    newOrder.setOrderDate(new Date());
                    newOrder.setLastUpdated(new Date());
                    orderDAO.addOrder(newOrder);
                    orderId = newOrder.getOrderId();
                    Logger.log("Работник " + username + " создал новый заказ ID: " + orderId + " для клиента ID: " + clientId);
                } else {
                    orderId = Integer.parseInt(orderIdField.getText());
                    // Проверяем, существует ли заказ
                    if (orderDAO.getOrderById(orderId) == null) {
                        JOptionPane.showMessageDialog(this, "Заказ с указанным ID не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Logger.log("Работник " + username + " добавляет товар в существующий заказ ID: " + orderId);
                }

                // Добавляем товар в заказ
                orderDAO.addItemToOrder(orderId, productId, quantity);

                // Обновляем количество товара на складе
                productDAO.updateProductQuantity(productId, availableQuantity - quantity);

                JOptionPane.showMessageDialog(this, "Товар успешно добавлен в заказ и количество на складе обновлено.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                Logger.log("Работник " + username + " добавил товар ID: " + productId + " (Кол-во: " + quantity + ") в заказ ID: " + orderId);

                loadProductsData("");
                loadTasksData("");

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числовые значения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                Logger.logError("Ошибка добавления товара в заказ (неверный формат числа): " + e.getMessage(), e);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка базы данных при добавлении товара в заказ: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                Logger.logError("Ошибка базы данных при добавлении товара в заказ: " + e.getMessage(), e);
            }
        }
    }

    private void showRemoveOrderItemDialog() {
        int selectedOrderRow = tasksTable.getSelectedRow();
        if (selectedOrderRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для удаления товара.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId = (int) tasksTableModel.getValueAt(selectedOrderRow, 0);
        OrderDAO orderDAO = null;
        try {
            orderDAO = new OrderDAO(conn);
            List<OrderItem> orderItems = orderDAO.getOrderItems(orderId);

            if (orderItems.isEmpty()) {
                JOptionPane.showMessageDialog(this, "В этом заказе нет товаров.", "Информация", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] itemNames = new String[orderItems.size()];
            for (int i = 0; i < orderItems.size(); i++) {
                OrderItem item = orderItems.get(i);
                itemNames[i] = "Товар ID: " + item.getProductId() + " (Кол-во: " + item.getQuantity() + ", Цена: " + item.getPrice() + ")";
            }

            String selectedItemName = (String) JOptionPane.showInputDialog(
                this,
                "Выберите товар для удаления из заказа ID " + orderId + ":",
                "Удалить товар из заказа",
                JOptionPane.QUESTION_MESSAGE,
                null,
                itemNames,
                itemNames[0]
            );

            if (selectedItemName != null) {
                int selectedItemIndex = -1;
                for (int i = 0; i < itemNames.length; i++) {
                    if (itemNames[i].equals(selectedItemName)) {
                        selectedItemIndex = i;
                        break;
                    }
                }

                if (selectedItemIndex != -1) {
                    OrderItem selectedItem = orderItems.get(selectedItemIndex);
                    //int orderItemId = selectedItem.getId();
                    int productId = selectedItem.getProductId();
                    int currentQuantityInOrder = selectedItem.getQuantity();

                    String quantityToRemoveStr = JOptionPane.showInputDialog(this, "Введите количество для удаления (доступно: " + currentQuantityInOrder + "):", "Количество", JOptionPane.PLAIN_MESSAGE);
                    if (quantityToRemoveStr == null || quantityToRemoveStr.isEmpty()) return;

                    int quantityToRemove = Integer.parseInt(quantityToRemoveStr);

                    if (quantityToRemove <= 0 || quantityToRemove > currentQuantityInOrder) {
                        JOptionPane.showMessageDialog(this, "Некорректное количество для удаления.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (quantityToRemove == currentQuantityInOrder) {

                        orderDAO.deleteOrderItem(orderId, productId);
                        JOptionPane.showMessageDialog(this, "Товар полностью удален из заказа.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                        Logger.log("Работник " + username + " полностью удалил товар ID: " + productId + " из заказа ID: " + orderId);
                    } else {

                        int newQuantityInOrder = currentQuantityInOrder - quantityToRemove;
                        //orderDAO.updateOrderItemQuantity(orderItemId, newQuantityInOrder);
                        JOptionPane.showMessageDialog(this, "Количество товара в заказе обновлено.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                        Logger.log("Работник " + username + " изменил количество товара ID: " + productId + " в заказе ID: " + orderId + ", новое количество: " + newQuantityInOrder);
                    }


                    Product product = productDAO.getProductById(productId);
                    if (product != null) {
                        productDAO.updateProductQuantity(productId, product.getQuantity() + quantityToRemove);
                        Logger.log("Работник " + username + " вернул " + quantityToRemove + " ед. товара ID: " + productId + " на склад.");
                    }

                    loadTasksData("");
                    loadProductsData("");

                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректные числовые значения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка удаления товара из заказа (неверный формат числа): " + e.getMessage(), e);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка базы данных при удалении товара из заказа: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка базы данных при удалении товара из заказа: " + e.getMessage(), e);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Неизвестная ошибка: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            Logger.logError("Неизвестная ошибка при удалении товара из заказа: " + e.getMessage(), e);
        }
    }

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Товары", SwingConstants.CENTER), BorderLayout.NORTH);


        JPanel buttonPanel = new JPanel();
        JButton addProductButton = new JButton("Добавить товар");
        JButton refreshButton = new JButton("Обновить");
        buttonPanel.add(addProductButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Добавляем поиск
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Инициализация таблицы товаров
        String[] columnNames = {"ID", "Название", "Описание", "Количество", "Цена", "Поставщик"};
        productsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(productsTableModel);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Обработчик добавления товара
        addProductButton.addActionListener(e -> {
            JDialog dialog = new JDialog(this, "Добавить товар", true);
            dialog.setLayout(new BorderLayout());

            JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
            JTextField nameField = new JTextField();
            JTextField descField = new JTextField();
            JTextField quantityField = new JTextField();
            JTextField priceField = new JTextField();
            JTextField supplierField = new JTextField();

            inputPanel.add(new JLabel("Название:"));
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Описание:"));
            inputPanel.add(descField);
            inputPanel.add(new JLabel("Количество:"));
            inputPanel.add(quantityField);
            inputPanel.add(new JLabel("Цена:"));
            inputPanel.add(priceField);
            inputPanel.add(new JLabel("Поставщик:"));
            inputPanel.add(supplierField);

            JButton saveButton = new JButton("Сохранить");
            saveButton.addActionListener(ev -> {
                try {
                    String name = nameField.getText();
                    String description = descField.getText();
                    int quantity = Integer.parseInt(quantityField.getText());
                    double price = Double.parseDouble(priceField.getText());
                    String supplier = supplierField.getText();

                    Product product = new Product();
                    product.setName(name);
                    product.setDescription(description);
                    product.setQuantity(quantity);
                    product.setPrice(price);
                    product.setSupplier(supplier);

                    productDAO.addProduct(product);
                    loadProductsData("");
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Товар успешно добавлен");
                    Logger.log("Работник " + username + " добавил новый товар: " + name);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите корректные числовые значения");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка при добавлении товара: " + ex.getMessage());
                }
            });

            dialog.add(inputPanel, BorderLayout.CENTER);
            dialog.add(saveButton, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });


        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            loadProductsData(searchText);
        });

        // Обработчик обновления
        refreshButton.addActionListener(e -> loadProductsData(""));

        //  обработчик двойного клика для просмотра деталей (просто так)
        productsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = productsTable.getSelectedRow();
                    if (row != -1) {
                        int productId = (int) productsTableModel.getValueAt(row, 0);
                        String name = (String) productsTableModel.getValueAt(row, 1);
                        String description = (String) productsTableModel.getValueAt(row, 2);
                        int quantity = (int) productsTableModel.getValueAt(row, 3);
                        double price = (double) productsTableModel.getValueAt(row, 4);
                        String supplier = (String) productsTableModel.getValueAt(row, 5);

                        JDialog dialog = new JDialog(WorkerWindow.this, "Детали товара", true);
                        dialog.setLayout(new BorderLayout());

                        JPanel detailsPanel = new JPanel(new GridLayout(6, 2, 5, 5));
                        detailsPanel.add(new JLabel("ID:"));
                        detailsPanel.add(new JLabel(String.valueOf(productId)));
                        detailsPanel.add(new JLabel("Название:"));
                        detailsPanel.add(new JLabel(name));
                        detailsPanel.add(new JLabel("Описание:"));
                        detailsPanel.add(new JLabel(description));
                        detailsPanel.add(new JLabel("Количество:"));
                        detailsPanel.add(new JLabel(String.valueOf(quantity)));
                        detailsPanel.add(new JLabel("Цена:"));
                        detailsPanel.add(new JLabel(String.format("%.2f ₽", price)));
                        detailsPanel.add(new JLabel("Поставщик:"));
                        detailsPanel.add(new JLabel(supplier));

                        dialog.add(detailsPanel, BorderLayout.CENTER);
                        dialog.pack();
                        dialog.setLocationRelativeTo(WorkerWindow.this);
                        dialog.setVisible(true);
                    }
                }
            }
        });

        return panel;
    }

    private void checkPhotoRequest() {
        try (UserDAO userDAO = new UserDAO(DBmanager.getConnection())) {
            // Получаем userId работника по username
            User user = userDAO.getUserById(getCurrentUserId());
            if (user != null && userDAO.isPhotoRequest(user.getUserid())) {
                // Проверяем, что запрос был отправлен Admin_Ph
                String sql = "SELECT u1.role FROM Users u1 " +
                           "JOIN Users u2 ON u2.user_id = ? " +
                           "WHERE u1.role = 'admin_ph'";
                try (java.sql.PreparedStatement stmt = DBmanager.getConnection().prepareStatement(sql)) {
                    stmt.setInt(1, user.getUserid());
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
                                userDAO.updateUserPhoto(user.getUserid(), photoBytes);
                                userDAO.setPhotoRequest(user.getUserid(), false);
                                JOptionPane.showMessageDialog(this, "Спасибо за сотрудничество!");
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(this, "Ошибка при создании : " + e.getMessage());
                            }
                        } else {
                            userDAO.setPhotoRequest(user.getUserid(), false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке photo_request: " + e.getMessage());
        }
    }

    private int getCurrentUserId() {
        try (Dao_db.AddUser addUserDao = new Dao_db.AddUser(DBobject.DBmanager.getConnection())) {
            User user = addUserDao.findByUsername(username);
            if (user != null) return user.getUserid();
        } catch (Exception ignored) {}
        return -1;
    }
}
