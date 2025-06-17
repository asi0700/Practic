package ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import javax.swing.*;
import utils.CameraManager;
import utils.NotificationManager;

public class CameraStreamWindow extends JFrame implements Serializable, CameraManager.CameraListener {
    private static final long serialVersionUID = 1L;
    
    private transient JPanel cameraPanel;
    private transient JLabel statusLabel;
    private transient CameraManager cameraManager;
    private transient NotificationManager notificationManager;
    private final int userId;

    public CameraStreamWindow(int userId) {
        this.userId = userId;
        this.notificationManager = NotificationManager.getInstance();
        
        setTitle("Камера");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Инициализация компонентов
        statusLabel = new JLabel("Камера активна", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Основная панель
        cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.add(statusLabel, BorderLayout.CENTER);

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton stopButton = new JButton("Остановить камеру");

        stopButton.addActionListener(e -> {
            stopStreaming();
            dispose();
        });

        controlPanel.add(stopButton);
        cameraPanel.add(controlPanel, BorderLayout.SOUTH);

        // Добавление основной панели
        add(cameraPanel);

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopStreaming();
            }
        });

        // Начинаем стриминг
        startStreaming();
    }

    private void startStreaming() {
        cameraManager = CameraManager.getInstance();
        cameraManager.addListener(this);
        cameraManager.startStreaming();
    }

    private void stopStreaming() {
        if (cameraManager != null) {
            cameraManager.removeListener(this);
            cameraManager.stopStreaming();
            notificationManager.setCameraAccessStatus(userId, false);
        }
    }

    @Override
    public void onFrameCaptured(BufferedImage image) {
        // В режиме только передачи мы не отображаем изображение
        // Оно будет отправлено только слушателям (RemoteCameraWindow)
    }
} 