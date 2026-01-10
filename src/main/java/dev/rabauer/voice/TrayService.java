package dev.rabauer.voice;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TrayService implements RecorderListener {
    private static final Logger log = LoggerFactory.getLogger(TrayService.class);

    @Inject
    HotkeyManager hotkeyManager;

    @Inject
    AudioCaptureService audioCaptureService;

    private TrayIcon trayIcon;
    private MenuItem recordMenuItem;

    @PostConstruct
    public void init() {
        if (!Boolean.parseBoolean(System.getProperty("enable.tray", "true"))) {
            log.info("Tray disabled via system property");
            return;
        }

        if (!SystemTray.isSupported()) {
            log.warn("SystemTray not supported on this platform");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            PopupMenu popup = new PopupMenu();

            recordMenuItem = new MenuItem("Record");
            recordMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleRecording();
                }
            });

            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> {
                audioCaptureService.stopRecording();
                log.info("Exiting application via tray");
                System.exit(0);
            });

            popup.add(recordMenuItem);
            popup.addSeparator();
            popup.add(exit);

            trayIcon = new TrayIcon(image, "OnDemand AI Voice", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            audioCaptureService.setListener(this);
            hotkeyManager.start();

            log.info("Tray initialized");
        } catch (Exception e) {
            log.error("Failed to init tray", e);
        }
    }

    private void toggleRecording() {
        if (audioCaptureService.isRecording()) {
            audioCaptureService.stopRecording();
            trayIcon.displayMessage("Recording", "Stopped", TrayIcon.MessageType.INFO);
        } else {
            audioCaptureService.startRecording();
            trayIcon.displayMessage("Recording", "Started", TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void onRecordingStarted() {
        if (recordMenuItem != null) {
            recordMenuItem.setLabel("Stop");
        }
        if (trayIcon != null) {
            trayIcon.displayMessage("Recording", "Started", TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void onRecordingStopped(File wavFile) {
        if (recordMenuItem != null) {
            recordMenuItem.setLabel("Record");
        }
        if (trayIcon != null) {
            trayIcon.displayMessage("Recording", "Stopped. Saved to: " + wavFile.getName(), TrayIcon.MessageType.INFO);
        }
    }
} 
