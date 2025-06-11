package adminUI;

import javax.swing.*;
import java.awt.event.ActionListener;

public class CommonMenuBar extends JMenuBar {
    private ActionListener exitListener;
    private ActionListener productsListener;
    private ActionListener ordersListener;
    private ActionListener logsListener;

    public CommonMenuBar(ActionListener exitListener, ActionListener productsListener, ActionListener ordersListener, ActionListener logsListener) {
        this.exitListener = exitListener;
        this.productsListener = productsListener;
        this.ordersListener = ordersListener;
        this.logsListener = logsListener;

        initializeMenu();
    }

    private void initializeMenu() {

        JMenu navMenu = new JMenu("Навигация");
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(productsListener);
        JMenuItem ordersItem = new JMenuItem("Заказы");
        ordersItem.addActionListener(ordersListener);
        JMenuItem logsItem = new JMenuItem("Логи действий");
        logsItem.addActionListener(logsListener);
        navMenu.add(productsItem);
        navMenu.add(ordersItem);
        navMenu.add(logsItem);

        JMenu fileMenu = new JMenu("Выход");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(exitListener);
        fileMenu.add(exitItem);

        add(navMenu);
        add(fileMenu);

        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> JOptionPane.showMessageDialog(null, "Поиск: " + searchField.getText()));
        searchPanel.add(new JLabel("Поиск товара:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        add(Box.createHorizontalGlue());
        add(searchPanel);
    }

}
