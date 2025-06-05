package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import model.User;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTable productsTable;
    private JTextField searchField;
    private JButton searchButton;
    private User user;
    private User currentUser;

    public MainWindow(User currentUser) {
        this.currentUser = currentUser;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Управление складом");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);


        createMenuBar();


        cards.add(createDashboardPanel(), "DASHBOARD");
        cards.add(createProductsPanel(), "PRODUCTS");
        cards.add(createInventoryPanel(), "INVENTORY");

        add(cards, BorderLayout.CENTER);


        cardLayout.show(cards, "DASHBOARD");
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();


        JMenu fileMenu = new JMenu("Выход");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> openExitWindow());
        fileMenu.add(exitItem);


        JMenu navMenu = new JMenu("Навигация");
        JMenuItem dashboardItem = new JMenuItem("Главная");
        dashboardItem.addActionListener(e -> cardLayout.show(cards, "DASHBOARD"));
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(e -> cardLayout.show(cards, "PRODUCTS"));
        JMenuItem inventoryItem = new JMenuItem("Инвентаризация");
        inventoryItem.addActionListener(e -> cardLayout.show(cards, "INVENTORY"));

        navMenu.add(dashboardItem);
        navMenu.add(productsItem);
        navMenu.add(inventoryItem);

        menuBar.add(fileMenu);
        menuBar.add(navMenu);


        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        searchButton = new JButton("Поиск");
        searchButton.addActionListener(this::performSearch);

        searchPanel.add(new JLabel("Поиск товара:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(searchPanel);

        setJMenuBar(menuBar);
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());


        JLabel title = new JLabel("Главная панель", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JLabel welcomeLabel = new JLabel("Добро пожаловать, " + currentUser.getName() + "! Ваша роль: " + currentUser.getRole(), SwingConstants.CENTER);

        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(welcomeLabel, BorderLayout.CENTER);




        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statsPanel.add(createStatCard("Всего товаров", "0"));
        statsPanel.add(createStatCard("Низкий запас", "0"));
        statsPanel.add(createStatCard("Категории", "0"));
        statsPanel.add(createStatCard("Последние поступления", "0"));

        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Управление товарами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);


        String[] columnNames = {"ID", "Наименование", "Количество", "Цена"};
        Object[][] data = {};

        productsTable = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Инвентаризация", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.CENTER);
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



    private void openRegistrationWindow() {
        registration registrationWindow = new registration();
        registrationWindow.setVisible(true);
        dispose();
    }

    private void openExitWindow() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
        }

        //        ExitWindow exitWindow = new ExitWindow();
//        exitWindow.setVisible(true);
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            MainWindow window = new MainWindow();
//            window.setVisible(true);
//        });
//
//    }
}