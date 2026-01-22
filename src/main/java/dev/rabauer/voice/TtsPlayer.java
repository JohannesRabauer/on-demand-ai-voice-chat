package dev.rabauer.voice;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Singleton
public class TtsPlayer {
    private static final Logger log = LoggerFactory.getLogger(TtsPlayer.class);

    @ConfigProperty(name = "app.audio.outputDevice")
    Optional<String> outputDeviceName;

    public void setOutputDevice(String deviceName) {
        this.outputDeviceName = Optional.ofNullable(deviceName);
        log.info("Output device changed to: {}", deviceName != null ? deviceName : "system default");
    }

    public Optional<String> getOutputDevice() {
        return outputDeviceName;
    }

    public byte[] requestTtsWav(String ttsUrl, String apiKey, String text, String voice) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        // OpenAI TTS API requires: model, input, voice, and optionally response_format
        // Use Jackson ObjectMapper for proper JSON encoding to handle newlines, quotes, and special characters
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payloadNode = mapper.createObjectNode();
        payloadNode.put("model", "tts-1");
        payloadNode.put("input", text);
        payloadNode.put("voice", voice);
        payloadNode.put("response_format", "wav");
        String payload = mapper.writeValueAsString(payloadNode);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ttsUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return resp.body();
        }
        throw new RuntimeException("TTS request failed: HTTP " + resp.statusCode());
    }

    public void playWav(byte[] wavBytes) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bais)) {
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            // Find configured output device or use default
            Mixer selectedMixer = null;
            if (outputDeviceName.isPresent()) {
                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().contains(outputDeviceName.get())) {
                        Mixer mixer = AudioSystem.getMixer(mixerInfo);
                        if (mixer.isLineSupported(info)) {
                            selectedMixer = mixer;
                            log.info("Using configured audio output device: {}", mixerInfo.getName());
                            break;
                        }
                    }
                }
                if (selectedMixer == null) {
                    log.warn("Configured output device '{}' not found or not supported, using system default", outputDeviceName.get());
                }
            }
            
            SourceDataLine line;
            if (selectedMixer != null) {
                line = (SourceDataLine) selectedMixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            
            try (line) {
                line.open(format);
                line.start();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = ais.read(buffer)) > 0) {
                    line.write(buffer, 0, n);
                }
                line.drain();
            }
        }
    }
}
