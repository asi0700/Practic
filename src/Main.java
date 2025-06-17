import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import DBobject.DBmanager;
import ui.LoginWindow;
import utils.Logger;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {

                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                Logger.logError("Ошибка при установке Look and Feel", e);
            }


            Logger.initialize();
            Logger.log("Запуск приложения");

            try {
                DBmanager.initializeDatabase();
                Logger.log("База данных инициализирована");


                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Теперь это безопасно
                loginWindow.setVisible(true);
                Logger.log("Окно входа открыто");
            } catch (Exception e) {
                Logger.logError("Неожиданная ошибка при запуске", e);
                JOptionPane.showMessageDialog(null,
                    "Неожиданная ошибка при запуске: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}