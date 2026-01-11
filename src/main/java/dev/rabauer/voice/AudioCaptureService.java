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
    @ConfigProperty(name = "app.whisper.url", defaultValue = "https://api.openai.com/v1/audio/transcriptions")
    String whisperUrl;

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
                        }
                    }
                    log.info("Captured {} bytes total, {} chunks had non-zero audio data", totalBytes, nonZeroChunks);

                    // Save to WAV file first
                    byte[] audioBytes = out.toByteArray();
                    Path tmp = Files.createTempFile("recording-", ".wav");

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes)) {
                        AudioInputStream ais = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tmp.toFile());
                    }
                    log.info("Recording saved to {}", tmp);

                    try {
                        // Transcribe using Whisper API
                        log.info("Transcribing audio with Whisper API...");
                        String transcript = transcribeWithWhisper(tmp);
                        log.info("=== USER INPUT TRANSCRIPT ===");
                        log.info("You said: {}", transcript);

                        // Process with LangChain4j
                        String llmResponse = langchainAdapter.processTranscript(transcript);
                        
                        // Convert to speech and play
                        byte[] audioData = ttsPlayer.requestTtsWav(ttsUrl, apiKey, llmResponse, voice);
                        ttsPlayer.playWav(audioData);

                        if (listener != null) {
                            listener.onRecordingStopped(tmp.toFile());
                        }
                    } catch (Exception e) {
                        log.error("Error processing audio with Whisper/LLM/TTS", e);
                    }

                } catch (IOException e) {
                    log.error("Error while recording", e);
                }
            }, "audio-capture");

            captureThread.start();
            log.info("Recording started");
        } catch (LineUnavailableException e) {
            log.error("Failed to acquire audio line", e);
        }
    }

    private String transcribeWithWhisper(Path audioFile) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var boundary = "----Boundary" + System.currentTimeMillis();
            
            // Read audio file
            byte[] audioBytes = Files.readAllBytes(audioFile);
            
            // Build multipart form data
            var bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n");
            bodyBuilder.append("Content-Type: audio/wav\r\n\r\n");
            
            var headerBytes = bodyBuilder.toString().getBytes();
            var footerBytes = ("\r\n--" + boundary + "\r\n" +
                             "Content-Disposition: form-data; name=\"model\"\r\n\r\n" +
                             "whisper-1\r\n" +
                             "--" + boundary + "--\r\n").getBytes();
            
            // Combine all parts
            var bodyBytes = new byte[headerBytes.length + audioBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, bodyBytes, 0, headerBytes.length);
            System.arraycopy(audioBytes, 0, bodyBytes, headerBytes.length, audioBytes.length);
            System.arraycopy(footerBytes, 0, bodyBytes, headerBytes.length + audioBytes.length, footerBytes.length);
            
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(whisperUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();
            
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("Whisper API error: {} - {}", response.statusCode(), response.body());
                return "";
            }
            
            // Parse JSON response
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var jsonNode = mapper.readTree(response.body());
            return jsonNode.path("text").asText();
            
        } catch (Exception e) {
            log.error("Failed to transcribe with Whisper API", e);
            return "";
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
