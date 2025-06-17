package utils;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationManager {
    private static NotificationManager instance;
    private final Map<Integer, List<Notification>> userNotifications;
    private final Map<Integer, Boolean> cameraAccessStatus;
    private final Map<Integer, Boolean> photoRequestStatus;

    private NotificationManager() {
        userNotifications = new ConcurrentHashMap<>();
        cameraAccessStatus = new ConcurrentHashMap<>();
        photoRequestStatus = new ConcurrentHashMap<>();
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void sendPhotoRequest(int userId, String fromUsername) {
        Notification notification = new Notification(
            "Запрос на зр",
            "Пользователь " + fromUsername + " запрашивает ваше ку",
            NotificationType.PHOTO_REQUEST
        );
        addNotification(userId, notification);
        photoRequestStatus.put(userId, false);
    }

    public void sendCameraAccessRequest(int userId, String fromUsername) {
        Notification notification = new Notification(
            "Запрос доступа к камере",
            "Пользователь " + fromUsername + " запрашивает доступ к вашей камере",
            NotificationType.CAMERA_ACCESS
        );
        addNotification(userId, notification);
        cameraAccessStatus.put(userId, false);
    }

    public void setCameraAccessStatus(int userId, boolean granted) {
        cameraAccessStatus.put(userId, granted);
        if (granted) {
            Logger.log("Пользователь ID " + userId + " предоставил доступ к камере");
        } else {
            Logger.log("Пользователь ID " + userId + " отклонил доступ к камере");
        }
    }

    public void setPhotoRequestStatus(int userId, boolean granted) {
        photoRequestStatus.put(userId, granted);
        if (granted) {
            Logger.log("Пользователь ID " + userId + " предоставил о");
        } else {
            Logger.log("Пользователь ID " + userId + " отклонил запрос на о");
        }
    }

    public boolean getCameraAccessStatus(int userId) {
        return cameraAccessStatus.getOrDefault(userId, false);
    }

    public boolean getPhotoRequestStatus(int userId) {
        return photoRequestStatus.getOrDefault(userId, false);
    }

    private void addNotification(int userId, Notification notification) {
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);
    }

    public List<Notification> getUserNotifications(int userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>());
    }

    public void clearNotifications(int userId) {
        userNotifications.remove(userId);
    }

    public static class Notification {
        private final String title;
        private final String message;
        private final NotificationType type;
        private final Date timestamp;

        public Notification(String title, String message, NotificationType type) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = new Date();
        }

        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public Date getTimestamp() { return timestamp; }
    }

    public enum NotificationType {
        PHOTO_REQUEST,
        CAMERA_ACCESS
    }
} 