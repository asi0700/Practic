package ui;

import ui.registration;
import ui.LoginWindow;
import javax.swing.*;
import java.awt.*;


public class ExitWindow extends JFrame {
    public ExitWindow() {
        setTitle("Выход");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        JLabel message = new JLabel("Вы уверены, что хотите выйти?", SwingConstants.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton yesButton = new JButton("Да");
        yesButton.addActionListener(e -> {
            registration registrationWindow = new registration();
            registrationWindow.setVisible(true);
            dispose();
            JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor((Component) e.getSource());
            if (mainFrame != null) {
                mainFrame.dispose();
            }

        });

        JButton noButton = new JButton("Нет");
        noButton.addActionListener(e -> dispose());

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        panel.add(message);
        panel.add(buttonPanel);

        add(panel);
    }
}
