package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraManager {
    private static CameraManager instance;
    private Webcam webcam;
    private final List<CameraListener> listeners;
    private final AtomicBoolean isStreaming;
    private Timer captureTimer;
    private Dimension resolution;
    private int fps = 30;

    private CameraManager() {
        listeners = new ArrayList<>();
        isStreaming = new AtomicBoolean(false);
        resolution = WebcamResolution.VGA.getSize();
    }

    public static synchronized CameraManager getInstance() {
        if (instance == null) {
            instance = new CameraManager();
        }
        return instance;
    }

    public void initialize() {
        if (webcam == null) {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(resolution);
                webcam.setCustomViewSizes(new Dimension[] { resolution });
            }
        }
    }

    public void startStreaming() {
        if (webcam == null) {
            initialize();
        }

        if (webcam != null && !isStreaming.get()) {
            try {
                webcam.open();
                isStreaming.set(true);
                
                captureTimer = new Timer(1000 / fps, e -> {
                    if (webcam.isOpen()) {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            notifyListeners(image);
                        }
                    }
                });
                captureTimer.start();
                
                Logger.log("Начало видеопотока с камеры");
            } catch (Exception e) {
                Logger.logError("Ошибка при запуске камеры", e);
                JOptionPane.showMessageDialog(null,
                    "Не удалось запустить камеру: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void stopStreaming() {
        if (isStreaming.get()) {
            if (captureTimer != null) {
                captureTimer.stop();
            }
            if (webcam != null) {
                webcam.close();
            }
            isStreaming.set(false);
            Logger.log("Остановка видеопотока с камеры");
        }
    }

    public void setResolution(Dimension newResolution) {
        if (webcam != null && webcam.isOpen()) {
            stopStreaming();
        }
        this.resolution = newResolution;
        if (webcam != null) {
            webcam.setViewSize(resolution);
        }
    }

    public void setFPS(int newFPS) {
        this.fps = newFPS;
        if (captureTimer != null && captureTimer.isRunning()) {
            captureTimer.setDelay(1000 / fps);
        }
    }

    public List<Dimension> getAvailableResolutions() {
        List<Dimension> resolutions = new ArrayList<>();
        if (webcam != null) {
            for (Dimension size : webcam.getDevice().getResolutions()) {
                resolutions.add(size);
            }
        }
        return resolutions;
    }

    public boolean isStreaming() {
        return isStreaming.get();
    }

    public void addListener(CameraListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CameraListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(BufferedImage image) {
        for (CameraListener listener : listeners) {
            listener.onFrameCaptured(image);
        }
    }

    public interface CameraListener {
        void onFrameCaptured(BufferedImage image);
    }

    public void dispose() {
        stopStreaming();
        if (webcam != null) {
            webcam.close();
            webcam = null;
        }
    }
} 