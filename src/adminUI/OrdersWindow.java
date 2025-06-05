package adminUI;

import adminUI.AdminWindow;
import model.User;

import javax.swing.*;
import java.awt.*;


public class OrdersWindow  extends JFrame{
    private User user;
    private JPanel cards;
    private CardLayout cardLayout;

    public void initializeUI(){
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


    private JPanel createOrdersPanel(){
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Управление заказами", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        panel.add(new JLabel("Функционал заказов в разработке"), BorderLayout.CENTER);

        return panel;
    }

    private void openAdminWindow() {
        AdminWindow adminWindow = new AdminWindow(user);
        adminWindow.setVisible(true);
    }

}
