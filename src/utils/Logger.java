package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "actions.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String currentUser = null;
    private static String currentRole = null;

    public static void setCurrentUser(String username, String role) {
        currentUser = username;
        currentRole = role;
    }

    public static void log(String message) {
        writeToLog("INFO", message);
    }

    public static void logError(String message, Throwable e) {
        if (e != null) {
            writeToLog("ERROR", message + "\n" + e.getMessage());
            e.printStackTrace();
        } else {
            writeToLog("ERROR", message);
        }
    }

    private static synchronized void writeToLog(String level, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String userInfo = currentUser != null ? 
                String.format("Пользователь %s (%s)", currentUser, currentRole != null ? currentRole : "неизвестно") :
                "Система";
            writer.println(String.format("[%s] %s: %s", timestamp, userInfo, message));
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initialize() {
        // Очищаем информацию о текущем пользователе при инициализации
        currentUser = null;
        currentRole = null;
    }
}