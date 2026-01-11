package dev.rabauer.voice;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.logging.Level;

/**
 * HotkeyManager handles global hotkey registration for toggle recording using JNativeHook.
 */
@Singleton
public class HotkeyManager implements NativeKeyListener {
    private static final Logger log = LoggerFactory.getLogger(HotkeyManager.class);

    @Inject
    AudioCaptureService audioCaptureService;

    @Inject
    @ConfigProperty(name = "app.hotkey", defaultValue = "F8")
    String hotkey;

    private int targetKeyCode;
    private boolean registered = false;

    @PostConstruct
    public void init() {
        // Disable JNativeHook's own logging (it's very verbose)
        java.util.logging.Logger jnhLogger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        jnhLogger.setLevel(Level.WARNING);
        jnhLogger.setUseParentHandlers(false);
        
        // Map hotkey name to key code
        targetKeyCode = mapHotkeyToKeyCode(hotkey);
    }

    public void start() {
        if (registered) {
            log.warn("HotkeyManager already started");
            return;
        }
        
        try {
            log.info("Registering global hotkey: {}", hotkey);
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            registered = true;
            log.info("Global hotkey {} registered successfully", hotkey);
        } catch (NativeHookException e) {
            log.error("Failed to register native hook", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (!registered) {
            return;
        }
        
        try {
            log.info("Unregistering global hotkey");
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            registered = false;
        } catch (NativeHookException e) {
            log.error("Failed to unregister native hook", e);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == targetKeyCode) {
            log.info("Hotkey {} pressed, toggling recording", hotkey);
            toggleRecording();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Not used
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used
    }

    private void toggleRecording() {
        if (audioCaptureService.isRecording()) {
            log.info("Stopping recording via hotkey");
            audioCaptureService.stopRecording();
        } else {
            log.info("Starting recording via hotkey");
            audioCaptureService.startRecording();
        }
    }

    private int mapHotkeyToKeyCode(String key) {
        // Map common function keys and keys to NativeKeyEvent codes
        switch (key.toUpperCase()) {
            case "F1": return NativeKeyEvent.VC_F1;
            case "F2": return NativeKeyEvent.VC_F2;
            case "F3": return NativeKeyEvent.VC_F3;
            case "F4": return NativeKeyEvent.VC_F4;
            case "F5": return NativeKeyEvent.VC_F5;
            case "F6": return NativeKeyEvent.VC_F6;
            case "F7": return NativeKeyEvent.VC_F7;
            case "F8": return NativeKeyEvent.VC_F8;
            case "F9": return NativeKeyEvent.VC_F9;
            case "F10": return NativeKeyEvent.VC_F10;
            case "F11": return NativeKeyEvent.VC_F11;
            case "F12": return NativeKeyEvent.VC_F12;
            case "SPACE": return NativeKeyEvent.VC_SPACE;
            case "CTRL": return NativeKeyEvent.VC_CONTROL;
            case "ALT": return NativeKeyEvent.VC_ALT;
            case "SHIFT": return NativeKeyEvent.VC_SHIFT;
            default:
                log.warn("Unknown hotkey '{}', defaulting to F8", key);
                return NativeKeyEvent.VC_F8;
        }
    }
}
