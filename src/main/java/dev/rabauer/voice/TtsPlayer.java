package dev.rabauer.voice;

import jakarta.inject.Singleton;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Singleton
public class TtsPlayer {

    public byte[] requestTtsWav(String text, String voice, String apiKey, String ttsUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        // OpenAI TTS API requires: model, input, voice, and optionally response_format
        String payload = "{\"model\":\"tts-1\",\"input\":\"" + text.replace("\"", "\\\"") + "\",\"voice\":\"" + voice + "\",\"response_format\":\"wav\"}";
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
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
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
