package dev.rabauer.voice;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * HotkeyManager handles global hotkey registration for toggle recording.
 * Currently a stub; JNativeHook integration pending.
 */
@Singleton
public class HotkeyManager {
    private static final Logger log = LoggerFactory.getLogger(HotkeyManager.class);

    @Inject
    AudioCaptureService audioCaptureService;

    @Inject
    @ConfigProperty(name = "app.hotkey", defaultValue = "F8")
    String hotkey;

    @PostConstruct
    public void init() {
        // no-op for now
    }

    public void start() {
        log.info("HotkeyManager starting (hotkey={}). JNativeHook integration pending.", hotkey);
        // TODO: Register JNativeHook and listen for hotkey presses
    }

    @PreDestroy
    public void stop() {
        log.info("HotkeyManager stopping");
    }
}
