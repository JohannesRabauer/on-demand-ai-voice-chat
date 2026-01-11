package dev.rabauer.voice;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TrayService implements RecorderListener {
    private static final Logger log = LoggerFactory.getLogger(TrayService.class);

    @Inject
    HotkeyManager hotkeyManager;

    @Inject
    AudioCaptureService audioCaptureService;

    @Inject
    LangchainAdapter langchainAdapter;

    private TrayIcon trayIcon;
    private MenuItem recordMenuItem;

    void onStart(@Observes StartupEvent ev) {
        log.info("TrayService starting...");
        // Run on AWT Event Dispatch Thread
        EventQueue.invokeLater(this::initTray);
    }

    private void initTray() {
        log.info("Initializing system tray on EDT...");
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

            MenuItem clearHistory = new MenuItem("Clear History");
            clearHistory.addActionListener(e -> {
                langchainAdapter.clearConversationHistory();
            });

            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> {
                audioCaptureService.stopRecording();
                log.info("Exiting application via tray");
                System.exit(0);
            });

            popup.add(recordMenuItem);
            popup.add(clearHistory);
            popup.addSeparator();
            popup.add(exit);

            trayIcon = new TrayIcon(image, "OnDemand AI Voice", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            audioCaptureService.setListener(this);
            hotkeyManager.start();

            // Show startup notification with hotkey info
            trayIcon.displayMessage("OnDemand AI Voice", "Ready! Press F8 to start/stop recording", TrayIcon.MessageType.INFO);

            log.info("Tray initialized");
        } catch (Exception e) {
            log.error("Failed to init tray", e);
        }
    }

    private void toggleRecording() {
        if (audioCaptureService.isRecording()) {
            audioCaptureService.stopRecording();
        } else {
            audioCaptureService.startRecording();
        }
    }

    @Override
    public void onRecordingStarted() {
        if (recordMenuItem != null) {
            recordMenuItem.setLabel("Stop");
        }
    }

    @Override
    public void onRecordingStopped(File wavFile) {
        if (recordMenuItem != null) {
            recordMenuItem.setLabel("Record");
        }
    }
} 
