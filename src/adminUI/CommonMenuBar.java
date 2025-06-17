package adminUI;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import ui.ClientWindow;

public class CommonMenuBar extends JMenuBar {
    private ActionListener logoutListener;
    private ActionListener ordersListener;
    private ActionListener logsListener;
    private ActionListener productsListener;
    private ActionListener usersListener;
    private String role;

    public CommonMenuBar(JFrame parent, ActionListener logoutListener, ActionListener ordersListener, ActionListener logsListener, ActionListener productsListener, String role) {
        this.logoutListener = logoutListener;
        this.ordersListener = ordersListener;
        this.logsListener = logsListener;
        this.productsListener = productsListener;
        this.role = role;
        setBackground(new Color(240, 240, 240));
        
        JMenu accountMenu = new JMenu("Аккаунт");
        accountMenu.setMnemonic(KeyEvent.VK_A);
        
        JMenuItem logoutItem = new JMenuItem("Выйти", KeyEvent.VK_L);
        logoutItem.addActionListener(logoutListener);
        accountMenu.add(logoutItem);
        
        add(accountMenu);
        
        if (role.equals("admin") || role.equals("worker")) {
            JMenu navigationMenu = new JMenu("Навигация");
            navigationMenu.setMnemonic(KeyEvent.VK_N);
            
            if (role.equals("admin")) {
                JMenuItem dashboardItem = new JMenuItem("Главная", KeyEvent.VK_H);
                dashboardItem.addActionListener(e -> {
                    if (parent instanceof AdminWindow) {
                        ((AdminWindow) parent).showDashboard();
                    }
                });
                navigationMenu.add(dashboardItem);

                JMenuItem ordersItem = new JMenuItem("Заказы", KeyEvent.VK_O);
                ordersItem.addActionListener(ordersListener);
                navigationMenu.add(ordersItem);
                
                JMenuItem logsItem = new JMenuItem("Логи действий", KeyEvent.VK_G);
                logsItem.addActionListener(logsListener);
                navigationMenu.add(logsItem);
                
                JMenuItem productsItem = new JMenuItem("Товары", KeyEvent.VK_P);
                productsItem.addActionListener(productsListener);
                navigationMenu.add(productsItem);

                JMenuItem usersItem = new JMenuItem("Пользователи", KeyEvent.VK_U);
                usersItem.addActionListener(e -> {
                    if (parent instanceof AdminWindow) {
                        ((AdminWindow) parent).showUsersPanel();
                    }
                });
                navigationMenu.add(usersItem);
            } else if (role.equals("worker")) {
                JMenuItem tasksItem = new JMenuItem("Задачи", KeyEvent.VK_T);
                tasksItem.addActionListener(ordersListener);
                navigationMenu.add(tasksItem);
            }
            
            add(navigationMenu);
        } else {
            JMenu navigationMenu = new JMenu("Навигация");
            navigationMenu.setMnemonic(KeyEvent.VK_N);
            
            JMenuItem dashboardItem = new JMenuItem("Главная", KeyEvent.VK_H);
            dashboardItem.addActionListener(e -> {
                if (parent instanceof ClientWindow) {
                    ((ClientWindow) parent).showDashboard();
                }
            });
            navigationMenu.add(dashboardItem);
            
            JMenuItem productsItem = new JMenuItem("Товары", KeyEvent.VK_P);
            productsItem.addActionListener(productsListener);
            navigationMenu.add(productsItem);
            
            JMenuItem ordersItem = new JMenuItem("Заказы", KeyEvent.VK_O);
            ordersItem.addActionListener(ordersListener);
            navigationMenu.add(ordersItem);
            
            JMenuItem cartItem = new JMenuItem("Корзина", KeyEvent.VK_C);
            cartItem.addActionListener(logsListener);
            navigationMenu.add(cartItem);
            
            add(navigationMenu);
        }
    }
}
