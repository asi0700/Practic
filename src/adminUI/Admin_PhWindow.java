package adminUI;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import model.*;
import DBobject.DBmanager;
import Dao_db.*;
import utils.*;
import ui.MainWindow;
import java.sql.SQLException;

public class Admin_PhWindow extends JFrame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final transient User currentUser;
    private final transient AddUser userDAO;
    private final transient CardLayout cardLayout;
    private final transient JPanel mainPanel;
    private transient JPanel photoPanel;
    private transient JPanel cameraPanel;
    private final transient Map<Integer, JButton> photoRequestButtons;
    private final transient Map<Integer, JButton> cameraAccessButtons;
    private final transient Map<Integer, Boolean> userConsentStatus;
    private final transient NotificationManager notificationManager;

    public Admin_PhWindow(User user, MainWindow mainWindow) {
        this.currentUser = user;
        try {
            this.userDAO = new AddUser(DBmanager.getConnection());
        } catch (SQLException e) {
            Logger.logError("Ошибка при инициализации DAO", e);
            throw new RuntimeException("Не удалось инициализировать DAO", e);
        }
        this.photoRequestButtons = new HashMap<>();
        this.cameraAccessButtons = new HashMap<>();
        this.userConsentStatus = new HashMap<>();
        this.notificationManager = NotificationManager.getInstance();

        setTitle("Панель администратора ф-доступа");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Инициализация панелей
        this.cardLayout = new CardLayout();
        this.mainPanel = new JPanel(cardLayout);
        initializePanels();

        // Добавление панелей в CardLayout
        mainPanel.add(photoPanel, "photo");
        mainPanel.add(cameraPanel, "camera");

        // Добавление меню
        setJMenuBar(createMenuBar());

        // Добавление основного контейнера
        add(mainPanel);

        // Обработчик закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                    Admin_PhWindow.this,
                    "Вы уверены, что хотите выйти?",
                    "Подтверждение выхода",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    Logger.log("Выход из системы пользователем " + currentUser.getUsername());
                    Logger.initialize();
                    dispose();
                    mainWindow.setVisible(true);
                }
            }
        });


        cardLayout.show(mainPanel, "ph");
    }

    private void initializePanels() {
        try {
            this.photoPanel = createPhotoPanel();
            this.cameraPanel = createCameraPanel();
        } catch (SQLException e) {
            Logger.logError("Ошибка при инициализации панелей", e);
            throw new RuntimeException("Не удалось инициализировать панели", e);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Меню "Аккаунт"
        JMenu accountMenu = new JMenu("Аккаунт");
        JMenuItem logoutItem = new JMenuItem("Выйти");
        logoutItem.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                Logger.log("Выход из системы пользователем " + currentUser.getUsername());
                Logger.initialize();
                dispose();
            }
        });
        accountMenu.add(logoutItem);
        menuBar.add(accountMenu);

        // Меню "Навигация"
        JMenu navigationMenu = new JMenu("Навигация");
        
        JMenuItem photoItem = new JMenuItem("Управление ph");
        photoItem.addActionListener(e -> cardLayout.show(mainPanel, "photo"));
        navigationMenu.add(photoItem);

        JMenuItem cameraItem = new JMenuItem("Управление камерой");
        cameraItem.addActionListener(e -> cardLayout.show(mainPanel, "camera"));
        navigationMenu.add(cameraItem);

        menuBar.add(navigationMenu);

        return menuBar;
    }

    private JPanel createPhotoPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Управление ф-запросами", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"ID", "Имя пользователя", "Роль", "Статус согласия"};
        List<User> users = userDAO.getAllUsers();
        Object[][] data = new Object[users.size()][4];
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            data[i][0] = user.getUserid();
            data[i][1] = user.getUsername();
            data[i][2] = user.getRole();
            boolean photoStatus = notificationManager.getPhotoRequestStatus(user.getUserid());
            String statusText = photoStatus ? "ф разрешено" : "Нет активных разрешений";
            data[i][3] = statusText;
        }
        JTable usersTable = new JTable(data, columnNames);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(usersTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton requestPhotoButton = new JButton("Запросить ");
        requestPhotoButton.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(panel, "Выберите пользователя в таблице.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int userId = (int) usersTable.getValueAt(selectedRow, 0);
            User selectedUser = users.stream().filter(u -> u.getUserid() == userId).findFirst().orElse(null);
            if (selectedUser != null) {
                try {
                    requestPhoto(selectedUser);
                } catch (SQLException ex) {
                    Logger.logError("Ошибка при отправке -запроса", ex);
                }
            }
        });
        buttonPanel.add(requestPhotoButton);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            try {
                refreshPhotoPanel();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        buttonPanel.add(refreshButton);

        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCameraPanel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Управление доступом к камере", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"ID", "Имя пользователя", "Роль", "Статус доступа"};
        List<User> users = userDAO.getAllUsers();
        Object[][] data = new Object[users.size()][4];
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            data[i][0] = user.getUserid();
            data[i][1] = user.getUsername();
            data[i][2] = user.getRole();
            boolean cameraStatus = notificationManager.getCameraAccessStatus(user.getUserid());
            String statusText = cameraStatus ? "Камера разрешена" : "Нет активных разрешений";
            data[i][3] = statusText;
        }
        JTable usersTable = new JTable(data, columnNames);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(usersTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton requestCameraButton = new JButton("Запросить доступ к камере");
        requestCameraButton.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(panel, "Выберите пользователя в таблице.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int userId = (int) usersTable.getValueAt(selectedRow, 0);
            User selectedUser = users.stream().filter(u -> u.getUserid() == userId).findFirst().orElse(null);
            if (selectedUser != null) {
                try {
                    requestCameraAccess(selectedUser);
                } catch (SQLException ex) {
                    Logger.logError("Ошибка при отправке запроса на камеру", ex);
                }
            }
        });
        buttonPanel.add(requestCameraButton);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> {
            try {
                refreshCameraPanel();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        buttonPanel.add(refreshButton);

        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private void requestPhoto(User user) throws SQLException {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Отправить запрос на  пользователю " + user.getUsername() + "?",
            "Подтверждение запроса",
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            notificationManager.sendPhotoRequest(user.getUserid(), currentUser.getUsername());
            Logger.log("Отправлен запрос на о пользователю " + user.getUsername());
            JOptionPane.showMessageDialog(
                this,
                "Запрос на у отправлен пользователю " + user.getUsername(),
                "Запрос отправлен",
                JOptionPane.INFORMATION_MESSAGE
            );
            refreshPhotoPanel();
        }
    }

    private void requestCameraAccess(User user) throws SQLException {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Отправить запрос на доступ к камере пользователю " + user.getUsername() + "?",
            "Подтверждение запроса",
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            notificationManager.sendCameraAccessRequest(user.getUserid(), currentUser.getUsername());
            Logger.log("Отправлен запрос на доступ к камере пользователю " + user.getUsername());
            
            // Запускаем таймер для проверки ответа пользователя
            javax.swing.Timer timer = new javax.swing.Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (notificationManager.getCameraAccessStatus(user.getUserid())) {
                        ((javax.swing.Timer)e.getSource()).stop();
                        SwingUtilities.invokeLater(() -> {
                            RemoteCameraWindow remoteWindow = new RemoteCameraWindow(user);
                            remoteWindow.setVisible(true);
                        });
                    }
                }
            });
            timer.start();
            
            JOptionPane.showMessageDialog(
                this,
                "Запрос на доступ к камере отправлен пользователю " + user.getUsername() + "\n" +
                "Ожидание ответа...",
                "Запрос отправлен",
                JOptionPane.INFORMATION_MESSAGE
            );
            refreshCameraPanel();
        }
    }

    private void refreshPhotoPanel() throws SQLException {
        cardLayout.show(mainPanel, "photo");

        JPanel newPhotoPanel = createPhotoPanel();
        mainPanel.remove(photoPanel);
        mainPanel.add(newPhotoPanel, "photo");
        photoPanel = newPhotoPanel;
        cardLayout.show(mainPanel, "photo");
    }

    private void refreshCameraPanel() throws SQLException {
        cardLayout.show(mainPanel, "camera");
        // Обновление данных панели
        JPanel newCameraPanel = createCameraPanel();
        mainPanel.remove(cameraPanel);
        mainPanel.add(newCameraPanel, "camera");
        cameraPanel = newCameraPanel;
        cardLayout.show(mainPanel, "camera");
    }
} 