package dev.rabauer.voice;

import java.io.File;

public interface RecorderListener {
    void onRecordingStarted();
    void onRecordingStopped(File wavFile);
}
