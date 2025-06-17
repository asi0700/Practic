package adminUI;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import javax.swing.*;
import utils.CameraManager;
import model.User;

public class RemoteCameraWindow extends JFrame implements Serializable, CameraManager.CameraListener {
    private static final long serialVersionUID = 1L;
    
    private final transient User targetUser;
    private transient JPanel cameraPanel;
    private transient JLabel videoLabel;
    private transient CameraManager cameraManager;
    private transient BufferedImage currentFrame;
    private transient JButton captureButton;
    private transient JButton stopButton;

    public RemoteCameraWindow(User targetUser) {
        this.targetUser = targetUser;
        
        setTitle("Камера пользователя: " + targetUser.getUsername());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Инициализация компонентов
        videoLabel = new JLabel();
        videoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        videoLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Основная панель
        cameraPanel = new JPanel(new BorderLayout());

        // Панель с видео
        JScrollPane scrollPane = new JScrollPane(videoLabel);
        cameraPanel.add(scrollPane, BorderLayout.CENTER);

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        captureButton = new JButton("Сделать снимок");
        stopButton = new JButton("Остановить просмотр");

        captureButton.addActionListener(e -> {
            if (currentFrame != null) {
                // TODO: Сохранить изображение
                JOptionPane.showMessageDialog(this, "Снимок сделан");
            }
        });

        stopButton.addActionListener(e -> {
            stopStreaming();
            dispose();
        });

        controlPanel.add(captureButton);
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

        // Начинаем просмотр
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
        }
    }

    @Override
    public void onFrameCaptured(BufferedImage image) {
        currentFrame = image;
        SwingUtilities.invokeLater(() -> {
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                videoLabel.setIcon(icon);
            }
        });
    }
} 