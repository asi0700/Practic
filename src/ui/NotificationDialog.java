package ui;

import utils.NotificationManager;
import utils.NotificationManager.Notification;
import utils.NotificationManager.NotificationType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;

public class NotificationDialog extends JDialog implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient JTextArea messageArea;
    private transient NotificationManager notificationManager;
    private final Notification notification;
    private final int userId;
    private boolean response = false;

    public NotificationDialog(Frame parent, Notification notification, int userId) {
        super(parent, "Уведомление", true);
        this.notification = notification;
        this.userId = userId;
        this.notificationManager = NotificationManager.getInstance();

        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(parent);

        // Заголовок
        JLabel titleLabel = new JLabel(notification.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Сообщение
        messageArea = new JTextArea(notification.getMessage());
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        if (notification.getType() == NotificationType.PHOTO_REQUEST) {
            JButton acceptButton = new JButton("Помочь с улучшением!");
            JButton rejectButton = new JButton("Потом помогу");
            
            acceptButton.addActionListener(e -> {
                response = true;
                notificationManager.setPhotoRequestStatus(userId, true);
                dispose();
            });
            
            rejectButton.addActionListener(e -> {
                response = false;
                notificationManager.setPhotoRequestStatus(userId, false);
                dispose();
            });
            
            buttonPanel.add(acceptButton);
            buttonPanel.add(rejectButton);
        } else if (notification.getType() == NotificationType.CAMERA_ACCESS) {
            JButton acceptButton = new JButton("Разрешить доступ");
            JButton rejectButton = new JButton("Отклонить");
            
            acceptButton.addActionListener(e -> {
                response = true;
                notificationManager.setCameraAccessStatus(userId, true);
                dispose();
            });
            
            rejectButton.addActionListener(e -> {
                response = false;
                notificationManager.setCameraAccessStatus(userId, false);
                dispose();
            });
            
            buttonPanel.add(acceptButton);
            buttonPanel.add(rejectButton);
        }

        add(buttonPanel, BorderLayout.SOUTH);

        // Обработка закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (notification.getType() == NotificationType.PHOTO_REQUEST) {
                    notificationManager.setPhotoRequestStatus(userId, false);
                } else if (notification.getType() == NotificationType.CAMERA_ACCESS) {
                    notificationManager.setCameraAccessStatus(userId, false);
                }
            }
        });
    }

    public boolean getResponse() {
        return response;
    }
} 