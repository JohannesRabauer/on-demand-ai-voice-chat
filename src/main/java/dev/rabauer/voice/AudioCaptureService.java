package dev.rabauer.voice;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
    @Inject
    LangchainAdapter langchainAdapter;
    @Inject
    TtsPlayer ttsPlayer;

    @ConfigProperty(name = "openai.api.key")
    String apiKey;
    @ConfigProperty(name = "app.voice", defaultValue = "alloy")
    String voice;
    @ConfigProperty(name = "app.tts.url", defaultValue = "https://api.openai.com/v1/audio/speech")
    String ttsUrl;

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

        AudioFormat format = new AudioFormat(24000f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // List all available mixers/devices
        log.info("Available audio input devices:");
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            if (targetLines.length > 0) {
                log.info("  - {} ({})", mixerInfo.getName(), mixerInfo.getDescription());
            }
        }

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            
            // Try to find which mixer this line came from
            Mixer.Info[] allMixers = AudioSystem.getMixerInfo();
            String mixerName = "Unknown";
            for (Mixer.Info mi : allMixers) {
                Mixer mixer = AudioSystem.getMixer(mi);
                if (mixer.isLineSupported(info)) {
                    try {
                        Line testLine = mixer.getLine(info);
                        if (testLine.equals(line) || testLine.getClass().equals(line.getClass())) {
                            mixerName = mi.getName() + " - " + mi.getDescription();
                            break;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            
            log.info("Using audio device: {}", mixerName);
            log.info("Line info: {}", line.getLineInfo());
            line.open(format);
            log.info("Line opened with buffer size: {} bytes", line.getBufferSize());
            line.start();
            log.info("Line started, capturing audio...");

            recording.set(true);
            if (listener != null) {
                listener.onRecordingStarted();
            }
            // Connect to OpenAI Realtime and prepare to receive final transcript
            try {
                realtimeClient.connect().join();
                // Wait for session to be fully initialized before sending audio
                Thread.sleep(500);
                log.info("Session initialized, ready to stream audio");
                realtimeClient.setOnFinalTranscript(text -> {
                    log.info("Final transcript received: {}", text);
                    // Hand transcript to langchain adapter
                    String reply = langchainAdapter.processTranscript(text);
                    // Request TTS as WAV and play
                    try {
                        byte[] wav = ttsPlayer.requestTtsWav(reply, voice, apiKey, ttsUrl);
                        ttsPlayer.playWav(wav);
                    } catch (Exception e1) {
                        log.error("TTS playback failed", e1);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to connect Realtime client", e);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            captureThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                int totalBytes = 0;
                int nonZeroChunks = 0;
                try {
                    while (recording.get()) {
                        int read = line.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            totalBytes += read;
                            out.write(buffer, 0, read);
                            
                            // Check if buffer contains non-zero data
                            boolean hasData = false;
                            for (int i = 0; i < read; i++) {
                                if (buffer[i] != 0) {
                                    hasData = true;
                                    break;
                                }
                            }
                            if (hasData) {
                                nonZeroChunks++;
                            }
                            
                            // Stream audio chunk to OpenAI Realtime
                            realtimeClient.appendPcm16(buffer, read);
                        }
                    }
                    log.info("Captured {} bytes total, {} chunks had non-zero audio data", totalBytes, nonZeroChunks);

                    // Server VAD may have already committed, but we call it anyway to ensure response is created
                    log.info("About to commit and request response...");
                    realtimeClient.commitAndCreateResponse();
                    
                    // Wait for transcript (with timeout)
                    log.info("Waiting for transcript...");
                    try {
                        String transcript = realtimeClient.waitForTranscript().get(10, java.util.concurrent.TimeUnit.SECONDS);
                        log.info("=== Received transcript from OpenAI: {} ===", transcript);
                    } catch (java.util.concurrent.TimeoutException e) {
                        log.warn("Timeout waiting for transcript from OpenAI");
                    } catch (Exception e) {
                        log.warn("Error waiting for transcript", e);
                    }

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
