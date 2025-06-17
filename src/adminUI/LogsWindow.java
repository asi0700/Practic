package adminUI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class LogsWindow extends JFrame {
    private JTable logsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public LogsWindow() {
        setTitle("Журнал действий");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Создаем таблицу
        String[] columnNames = {"Дата и время", "Пользователь", "Роль", "Действие"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logsTable = new JTable(tableModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        logsTable.setRowSorter(sorter);
        JScrollPane scrollPane = new JScrollPane(logsTable);

        // Панель поиска
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            if (searchText.length() == 0) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
            }
        });
        searchPanel.add(new JLabel("Поиск:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> loadLogs());
        JButton clearSearchButton = new JButton("Очистить поиск");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
        });
        buttonPanel.add(clearSearchButton);
        buttonPanel.add(refreshButton);

        // Добавляем компоненты на форму
        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем логи
        loadLogs();
    }

    private void loadLogs() {
        tableModel.setRowCount(0);
        try (BufferedReader reader = new BufferedReader(new FileReader("actions.log"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[")) {
                    // Формат: [timestamp] Пользователь username (role): action
                    String[] parts = line.split("] ", 2);
                    if (parts.length == 2) {
                        String timestamp = parts[0].substring(1);
                        String[] userAction = parts[1].split(": ", 2);
                        if (userAction.length == 2) {
                            String userInfo = userAction[0]; // "Пользователь username (role)"
                            String action = userAction[1];
                            
                            // Извлекаем username и role из userInfo
                            String username = "";
                            String role = "";
                            if (userInfo.contains("(") && userInfo.contains(")")) {
                                username = userInfo.split("Пользователь ")[1].split(" \\(")[0];
                                role = userInfo.split("\\(")[1].split("\\)")[0];
                            }
                            
                            tableModel.addRow(new Object[]{
                                timestamp,
                                username,
                                role,
                                action
                            });
                        }
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка при чтении логов: " + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 