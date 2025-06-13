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

import Dao_db.OrderDAO;
import Dao_db.ProductDAO;
import DBobject.DBmanager;
import model.Product;
import model.User;
import ui.MainWindow;
import adminUI.CommonMenuBar;
import utils.Logger;
import model.Order;

public class WorkerWindow extends JFrame {
    private Connection conn;
    private String username;
    private MainWindow mainWindow;
    private DefaultTableModel tasksTableModel;
    private JTable tasksTable;
    private DefaultTableModel productsTableModel;
    private JTable productsTable;
    private ProductDAO productDAO;
    private CardLayout cardLayout;
    private JPanel cards;
    
    public WorkerWindow(Connection conn, String username, MainWindow mainWindow) {
        this.conn = conn;
        this.username = username;
        this.mainWindow = mainWindow;
        this.productDAO = new ProductDAO(conn);
        initializeUI();
        Logger.log("Окно работника открыто для пользователя " + username);
    }
    
    private void initializeUI() {
        setTitle("Окно работника - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Create menu bar
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
                loadProductsData();
                Logger.log("Работник " + username + " перешел на вкладку товаров.");
            },
            (e) -> {},
            "worker"
        );
        setJMenuBar(menuBar);
        
        // Добавляем панели
        try {
            cards.add(createOrdersPanel(), "ORDERS");
            cards.add(createProductsPanel(), "PRODUCTS");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при создании панелей: " + e.getMessage());
            Logger.logError("Ошибка при создании панелей в WorkerWindow: " + e.getMessage(), e);
        }
        add(cards);

        // Загружаем данные по умолчанию
        cardLayout.show(cards, "ORDERS");
        loadTasksData("");
    }
    
    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("Заказы", SwingConstants.CENTER), BorderLayout.NORTH);
        
        // Initialize table for tasks/orders
        String[] columnNames = {"ID", "ID клиента", "Дата заказа", "Сумма", "Статус", "Последнее обновление"};
        tasksTableModel = new DefaultTableModel(columnNames, 0);
        tasksTable = new JTable(tasksTableModel);
        tasksTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(tasksTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add mouse listener for row selection
        tasksTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = tasksTable.getSelectedRow();
                    if (selectedRow != -1) {
                        JOptionPane.showMessageDialog(WorkerWindow.this, "Opening order details for ID: " + tasksTableModel.getValueAt(selectedRow, 0));
                        Logger.log("Работник " + username + " просмотрел детали заказа ID: " + tasksTableModel.getValueAt(selectedRow, 0));
                    }
                }
            }
        });
        
        // Add button to update order status
        JButton updateStatusButton = new JButton("Update Status");
        updateStatusButton.addActionListener(e -> updateOrderStatus());

        JButton addOrderButton = new JButton("Добавить заказ");
        addOrderButton.addActionListener(e -> showAddOrderDialog());

        JButton removeOrderItemButton = new JButton("Удалить товар из заказа");
        removeOrderItemButton.addActionListener(e -> showRemoveOrderItemDialog());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addOrderButton);
        buttonPanel.add(updateStatusButton);
        buttonPanel.add(removeOrderItemButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createProductsPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Товары на складе", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.NORTH);

        // Панель поиска
        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> filterProductsTable(searchField.getText()));
        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Таблица товаров
        String[] columnNames = {"ID", "Название", "Описание", "Количество", "Цена", "Поставщик", "Дата добавления", "Кто добавил"};
        productsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(productsTableModel);
        productsTable.setAutoCreateRowSorter(true);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Устанавливаем ширину столбцов
        productsTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Название
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(200);  // Описание
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Количество
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Цена
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Поставщик
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(150);  // Дата добавления
        productsTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Кто добавил

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Кнопка обновления
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            loadProductsData();
            Logger.log("Работник " + username + " обновил данные товаров.");
        });
        JButton addProductToOrderButton = new JButton("Добавить в заказ");
        addProductToOrderButton.addActionListener(e -> showAddProductToOrderDialog());

        buttonPanel.add(refreshButton);
        buttonPanel.add(addProductToOrderButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        loadProductsData();
        return panel;
    }

    private void loadProductsData() {
        try {
            List<Object[]> products = productDAO.getAllProducts();
            productsTableModel.setRowCount(0);
            for (Object[] product : products) {
                productsTableModel.addRow(product);
            }
            Logger.log("Работник " + username + " загрузил данные товаров: " + products.size() + " записей.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки данных товаров: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            Logger.logError("Ошибка загрузки данных товаров для работника: " + e.getMessage(), e);
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

    private void loadTasksData(String filter) {
        try {
            OrderDAO orderDAO = new OrderDAO(conn);
            // Работник видит все заказы
            List<Object[]> orders = orderDAO.getAllOrders();
            tasksTableModel.setRowCount(0); // Clear existing data
            for (Object[] order : orders) {
                tasksTableModel.addRow(order);
            }
            Logger.log("Работник " + username + " загрузил данные заказов.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки задач: " + e.getMessage());
            Logger.logError("Ошибка загрузки задач для работника: " + e.getMessage(), e);
        }
    }
    
    private void updateOrderStatus() {
        int selectedRow = tasksTable.getSelectedRow();
        if (selectedRow != -1) {
            String orderId = tasksTableModel.getValueAt(selectedRow, 0).toString();
            String[] statusOptions = {"in progress", "completed", "cancelled"};
            String newStatus = (String) JOptionPane.showInputDialog(this, "Select new status:", "Update Order Status", JOptionPane.QUESTION_MESSAGE, null, statusOptions, "completed");
            if (newStatus != null) {
                OrderDAO orderDAO = new OrderDAO(conn);
                // Placeholder for actual update logic
                JOptionPane.showMessageDialog(this, "Status for order ID " + orderId + " updated to " + newStatus);
                loadTasksData(""); // Refresh data
            }
        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите заказ для обновления статуса.");
        }
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
                newOrder.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                newOrder.setLastUpdated(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));

                OrderDAO orderDAO = new OrderDAO(conn);
                orderDAO.addOrder(newOrder);
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
                OrderDAO orderDAO = new OrderDAO(conn);

                if (newOrderCheckBox.isSelected()) {
                    int clientId = Integer.parseInt(clientIdField.getText());
                    if (clientId <= 0) {
                        JOptionPane.showMessageDialog(this, "ID клиента для нового заказа должен быть больше 0.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Создаем новый заказ со статусом "В обработке" и нулевой стоимостью
                    orderId = orderDAO.addOrder(clientId, 0.0, "В обработке");
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

                loadProductsData(); // Обновляем данные товаров
                loadTasksData(""); // Обновляем данные заказов

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
            List<Object[]> orderItems = orderDAO.getOrderItems(orderId);

            if (orderItems.isEmpty()) {
                JOptionPane.showMessageDialog(this, "В этом заказе нет товаров.", "Информация", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] itemNames = new String[orderItems.size()];
            for (int i = 0; i < orderItems.size(); i++) {
                itemNames[i] = (String) orderItems.get(i)[1] + " (Кол-во: " + orderItems.get(i)[2] + ", Цена: " + orderItems.get(i)[3] + ")";
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
                    Object[] selectedItem = orderItems.get(selectedItemIndex);
                    int orderItemId = (int) selectedItem[0]; // ID элемента заказа
                    int productId = (int) selectedItem[4]; // ID продукта
                    int currentQuantityInOrder = (int) selectedItem[2]; // Текущее количество в заказе

                    String quantityToRemoveStr = JOptionPane.showInputDialog(this, "Введите количество для удаления (доступно: " + currentQuantityInOrder + "):", "Количество", JOptionPane.PLAIN_MESSAGE);
                    if (quantityToRemoveStr == null || quantityToRemoveStr.isEmpty()) return;

                    int quantityToRemove = Integer.parseInt(quantityToRemoveStr);

                    if (quantityToRemove <= 0 || quantityToRemove > currentQuantityInOrder) {
                        JOptionPane.showMessageDialog(this, "Некорректное количество для удаления.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (quantityToRemove == currentQuantityInOrder) {
                        // Удаляем весь элемент заказа
                        orderDAO.deleteOrderItem(orderId, productId);
                        JOptionPane.showMessageDialog(this, "Товар полностью удален из заказа.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                        Logger.log("Работник " + username + " полностью удалил товар ID: " + productId + " из заказа ID: " + orderId);
                    } else {
                        // Обновляем количество элемента заказа
                        int newQuantityInOrder = currentQuantityInOrder - quantityToRemove;
                        orderDAO.updateOrderItemQuantity(orderItemId, newQuantityInOrder);
                        JOptionPane.showMessageDialog(this, "Количество товара в заказе обновлено.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                        Logger.log("Работник " + username + " изменил количество товара ID: " + productId + " в заказе ID: " + orderId + ", новое количество: " + newQuantityInOrder);
                    }

                    // Возвращаем количество на склад
                    Product product = productDAO.getProductById(productId);
                    if (product != null) {
                        productDAO.updateProductQuantity(productId, product.getQuantity() + quantityToRemove);
                        Logger.log("Работник " + username + " вернул " + quantityToRemove + " ед. товара ID: " + productId + " на склад.");
                    }

                    loadTasksData(""); // Обновляем данные заказов
                    loadProductsData(); // Обновляем данные товаров

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
}
