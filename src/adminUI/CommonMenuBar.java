package adminUI;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.*;

public class CommonMenuBar extends JMenuBar {
    private ActionListener exitListener;
    private ActionListener productsListener;
    private ActionListener ordersListener;
    private ActionListener logsListener;
    private ActionListener workerTasksListener;
    private String userRole;

    public CommonMenuBar(ActionListener exitListener, ActionListener productsListener, ActionListener ordersListener, ActionListener logsListener, ActionListener workerTasksListener, String userRole) {
        this.exitListener = exitListener;
        this.productsListener = productsListener;
        this.ordersListener = ordersListener;
        this.logsListener = logsListener;
        this.workerTasksListener = workerTasksListener;
        this.userRole = userRole;
        initializeMenu();
    }

    private void initializeMenu() {

        JMenu navMenu = new JMenu("Навигация");
        JMenuItem productsItem = new JMenuItem("Товары");
        productsItem.addActionListener(productsListener);
        navMenu.add(productsItem);
        
        if (userRole.equals("admin")) {
            JMenuItem ordersItem = new JMenuItem("Заказы");
            ordersItem.addActionListener(ordersListener);
            navMenu.add(ordersItem);
            JMenuItem logsItem = new JMenuItem("Логи действий");
            logsItem.addActionListener(logsListener);
            navMenu.add(logsItem);
        } else if (userRole.equals("worker")) {
            JMenuItem tasksItem = new JMenuItem("Задачи работника");
            tasksItem.addActionListener(workerTasksListener);
            navMenu.add(tasksItem);
        }

        JMenu fileMenu = new JMenu("Аккаунт");
        JMenuItem logoutItem = new JMenuItem("Выйти из аккаунта");
        logoutItem.addActionListener(exitListener);
        fileMenu.add(logoutItem);

        add(navMenu);
        add(fileMenu);

        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> {
            if (productsListener != null) {
                productsListener.actionPerformed(null);
            }
        });
        searchPanel.add(new JLabel("Поиск товара:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        add(Box.createHorizontalGlue());
        add(searchPanel);
    }
}
