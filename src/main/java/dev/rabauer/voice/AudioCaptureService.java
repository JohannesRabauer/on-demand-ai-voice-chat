package dev.rabauer.voice;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AudioCaptureService {
    private static final Logger log = LoggerFactory.getLogger(AudioCaptureService.class);

    private final AtomicBoolean recording = new AtomicBoolean(false);
    private TargetDataLine line;
    private Thread captureThread;
    private RecorderListener listener;

    @Inject
    RealtimeOpenAIClient realtimeClient;

    @PostConstruct
    public void init() {
        // no-op for now
    }

    public boolean isRecording() {
        return recording.get();
    }

    public void setListener(RecorderListener listener) {
        this.listener = listener;
    }

    public synchronized void startRecording() {
        if (recording.get()) {
            log.warn("Already recording");
            return;
        }

        AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            recording.set(true);
            if (listener != null) {
                listener.onRecordingStarted();
            }
            // Connect to OpenAI Realtime and prepare to receive final transcript
            try {
                realtimeClient.connect().join();
                realtimeClient.setOnFinalTranscript(text -> {
                    log.info("Final transcript received: {}", text);
                    // Next step: hand to langchain4j adapter and TTS
                });
            } catch (Exception e) {
                log.error("Failed to connect Realtime client", e);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            captureThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                try {
                    while (recording.get()) {
                        int read = line.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            out.write(buffer, 0, read);
                            // Stream audio chunk to OpenAI Realtime
                            realtimeClient.appendPcm16(buffer, read);
                        }
                    }

                    // Commit the audio buffer and request a response
                    realtimeClient.commitAndCreateResponse();

                    // On stop: write to temp WAV file
                    byte[] audioBytes = out.toByteArray();
                    Path tmp = Files.createTempFile("recording-", ".wav");

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes)) {
                        AudioInputStream ais = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tmp.toFile());
                    }

                    log.info("Recording saved to {}", tmp);
                    if (listener != null) {
                        listener.onRecordingStopped(tmp.toFile());
                    }

                } catch (IOException e) {
                    log.error("Error while recording", e);
                } finally {
                    // Close realtime connection
                    try {
                        realtimeClient.close();
                    } catch (Exception ignored) {}
                }
            }, "audio-capture");

            captureThread.start();
            log.info("Recording started");
        } catch (LineUnavailableException e) {
            log.error("Failed to acquire audio line", e);
        }
    }

    public synchronized void stopRecording() {
        if (!recording.get()) {
            return;
        }
        recording.set(false);
        if (line != null) {
            line.stop();
            line.close();
        }
        try {
            if (captureThread != null) {
                captureThread.join(2000);
            }
        } catch (InterruptedException ignored) {}
        log.info("Recording stopped");
    }
}
