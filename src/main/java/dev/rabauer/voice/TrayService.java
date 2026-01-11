package dev.rabauer.voice;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

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

    @Inject
    TtsPlayer ttsPlayer;

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
            
            // Load icon image
            Image image;
            try {
                java.io.File iconFile = new java.io.File("Icon_cropped.png");
                if (iconFile.exists()) {
                    image = Toolkit.getDefaultToolkit().getImage("Icon_cropped.png");
                } else {
                    // Fallback to blank image if icon not found
                    image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    log.warn("Icon file not found, using blank image");
                }
            } catch (Exception e) {
                log.warn("Failed to load icon, using blank image", e);
                image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            }
            
            PopupMenu popup = new PopupMenu();

            recordMenuItem = new MenuItem("Record");
            recordMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleRecording();
                }
            });

            // Input device selection menu
            Menu inputDeviceMenu = new Menu("Input Device");
            addInputDeviceMenuItems(inputDeviceMenu);

            // Output device selection menu
            Menu outputDeviceMenu = new Menu("Output Device");
            addOutputDeviceMenuItems(outputDeviceMenu);

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
            popup.addSeparator();
            popup.add(inputDeviceMenu);
            popup.add(outputDeviceMenu);
            popup.addSeparator();
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

    private void addInputDeviceMenuItems(Menu menu) {
        // Add "System Default" option
        CheckboxMenuItem defaultItem = new CheckboxMenuItem("System Default");
        defaultItem.setState(!audioCaptureService.getInputDevice().isPresent());
        defaultItem.addItemListener(e -> {
            audioCaptureService.setInputDevice(null);
            trayIcon.displayMessage("Input Device Changed", "Using system default", TrayIcon.MessageType.INFO);
        });
        menu.add(defaultItem);
        menu.addSeparator();

        // List all available input devices
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            // Check if this mixer supports recording (has target lines)
            if (mixer.getTargetLineInfo().length > 0) {
                String deviceName = mixerInfo.getName();
                CheckboxMenuItem item = new CheckboxMenuItem(deviceName);
                
                // Check if this is the currently selected device
                item.setState(audioCaptureService.getInputDevice()
                    .map(current -> deviceName.contains(current))
                    .orElse(false));
                
                item.addItemListener(e -> {
                    audioCaptureService.setInputDevice(deviceName);
                    trayIcon.displayMessage("Input Device Changed", deviceName, TrayIcon.MessageType.INFO);
                });
                menu.add(item);
            }
        }
    }

    private void addOutputDeviceMenuItems(Menu menu) {
        // Add "System Default" option
        CheckboxMenuItem defaultItem = new CheckboxMenuItem("System Default");
        defaultItem.setState(!ttsPlayer.getOutputDevice().isPresent());
        defaultItem.addItemListener(e -> {
            ttsPlayer.setOutputDevice(null);
            trayIcon.displayMessage("Output Device Changed", "Using system default", TrayIcon.MessageType.INFO);
        });
        menu.add(defaultItem);
        menu.addSeparator();

        // List all available output devices
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            // Check if this mixer supports playback (has source lines)
            if (mixer.getSourceLineInfo().length > 0) {
                String deviceName = mixerInfo.getName();
                CheckboxMenuItem item = new CheckboxMenuItem(deviceName);
                
                // Check if this is the currently selected device
                item.setState(ttsPlayer.getOutputDevice()
                    .map(current -> deviceName.contains(current))
                    .orElse(false));
                
                item.addItemListener(e -> {
                    ttsPlayer.setOutputDevice(deviceName);
                    trayIcon.displayMessage("Output Device Changed", deviceName, TrayIcon.MessageType.INFO);
                });
                menu.add(item);
            }
        }
    }
} 
