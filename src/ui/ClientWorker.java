package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class ClientWorker extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTextField searchField;

    public ClientWorker() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Склад (Пользовательский режим)");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        createMenuBar();

        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");

        add(cards, BorderLayout.CENTER);

        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu navMenu = new JMenu("Навигация");
        JMenuItem dashboardItem = new JMenuItem("Главная");
        dashboardItem.addActionListener(e -> cardLayout.show(cards, "DASHBOARD"));
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));

        navMenu.add(dashboardItem);
        navMenu.add(productsItem);

        JMenuItem exitItem = new JMenuItem("Выйти в главное меню");
        exitItem.addActionListener(e -> {
            this.dispose();
        });

        menuBar.add(navMenu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(exitItem);

        setJMenuBar(menuBar);
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Главная панель", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statsPanel.add(createStatCard("Всего товаров", "125"));
        statsPanel.add(createStatCard("Категории товаров", "8"));

        panel.add(statsPanel, BorderLayout.CENTER);

        JLabel warningLabel = new JLabel("Товары с низким остатком:", SwingConstants.CENTER);
        warningLabel.setForeground(Color.RED);
        warningLabel.setFont(new Font("Arial", Font.BOLD, 16));

        DefaultListModel<String> lowStockModel = new DefaultListModel<>();
        lowStockModel.addElement("Ноутбук ASUS (осталось: 2)");
        lowStockModel.addElement("Мышь Logitech (осталось: 3)");

        JList<String> lowStockList = new JList<>(lowStockModel);
        lowStockList.setBackground(new Color(255, 240, 240));

        JPanel warningPanel = new JPanel(new BorderLayout());
        warningPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        warningPanel.add(warningLabel, BorderLayout.NORTH);
        warningPanel.add(new JScrollPane(lowStockList), BorderLayout.CENTER);

        panel.add(warningPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Список товаров на складе", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


        String[] columnNames = {"Наименование", "Количество", "Местоположение", "Категория"};
        Object[][] data = {
                // интеграция с бд
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productsTable = new JTable(model);
        productsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        productsTable.setRowSorter(sorter);


        JPanel searchPanel = new JPanel();
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> filterTable());

        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> refreshData());
        searchPanel.add(refreshButton);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(productsTable), BorderLayout.CENTER);

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

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(240, 248, 255));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void filterTable() {
        String query = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter =
                (TableRowSorter<DefaultTableModel>) productsTable.getRowSorter();

        if (query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void showProductDetails() {
        int row = productsTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) productsTable.getValueAt(row, 0);
            String quantity = productsTable.getValueAt(row, 1).toString();
            String location = (String) productsTable.getValueAt(row, 2);
            String category = (String) productsTable.getValueAt(row, 3);

            String details = String.format(
                    "<html><b>Наименование:</b> %s<br>" +
                            "<b>Количество:</b> %s<br>" +
                            "<b>Местоположение:</b> %s<br>" +
                            "<b>Категория:</b> %s</html>",
                    name, quantity, location, category
            );

            JOptionPane.showMessageDialog(
                    this,
                    details,
                    "Детальная информация",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void refreshData() {
        JOptionPane.showMessageDialog(
                this,
                "Данные успешно обновлены",
                "Обновление",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientWorker window = new ClientWorker();
            window.setVisible(true);
        });
    }
}