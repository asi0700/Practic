package adminUI;

import javax.swing.*;
import java.awt.event.ActionListener;

public class CommonMenuBar extends JMenuBar {
    private ActionListener exitListener;
    private ActionListener productsListener;
    private ActionListener ordersListener;

    public CommonMenuBar(ActionListener exitListener, ActionListener productsListener, ActionListener ordersListener) {
        this.exitListener = exitListener;
        this.productsListener = productsListener;
        this.ordersListener = ordersListener;

        initializeMenu();
    }

    private void initializeMenu() {

        JMenu fileMenu = new JMenu("Выход");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(exitListener);
        fileMenu.add(exitItem);


        JMenu navMenu = new JMenu("Навигация");
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(productsListener);
        JMenuItem ordersItem = new JMenuItem("Заказы");
        ordersItem.addActionListener(ordersListener);
        navMenu.add(productsItem);
        navMenu.add(ordersItem);

        add(fileMenu);
        add(navMenu);


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
