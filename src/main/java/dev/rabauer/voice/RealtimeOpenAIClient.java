package dev.rabauer.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Singleton
public class RealtimeOpenAIClient {
    private static final Logger log = LoggerFactory.getLogger(RealtimeOpenAIClient.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "openai.realtime.url", defaultValue = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview")
    String realtimeUrl;

    private volatile WebSocket ws;
    private Consumer<String> onFinalTranscript = s -> {};
    private int audioChunkCount = 0;

    public CompletableFuture<Void> connect() {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Missing openai.api.key"));
        }
        audioChunkCount = 0;
        HttpClient client = HttpClient.newHttpClient();
        log.info("Connecting to OpenAI Realtime: {}", realtimeUrl);
        return client.newWebSocketBuilder()
                .header("Authorization", "Bearer " + apiKey)
                .buildAsync(URI.create(realtimeUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        ws = webSocket;
                        log.info("Realtime WebSocket opened, ready to receive audio");
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        String json = data.toString();
                        try {
                            JsonNode n = mapper.readTree(json);
                            log.info("Realtime event: {}", json);
                            
                            // Parse transcript events - with server VAD the transcript comes in response output
                            if (n.has("type")) {
                                String eventType = n.get("type").asText();
                                
                                // Handle response.output_item.added (contains transcription from user input)
                                if ("response.output_item.added".equals(eventType)) {
                                    JsonNode item = n.get("item");
                                    if (item != null && "message".equals(item.path("type").asText())) {
                                        JsonNode content = item.get("content");
                                        if (content != null && content.isArray()) {
                                            for (JsonNode c : content) {
                                                if ("input_text".equals(c.path("type").asText())) {
                                                    String text = c.path("text").asText();
                                                    if (!text.isEmpty()) {
                                                        log.info("User input transcript: {}", text);
                                                        onFinalTranscript.accept(text);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Also check for conversation.item.created with role=user
                                else if ("conversation.item.created".equals(eventType)) {
                                    JsonNode item = n.get("item");
                                    if (item != null && "user".equals(item.path("role").asText())) {
                                        JsonNode content = item.get("content");
                                        if (content != null && content.isArray()) {
                                            for (JsonNode c : content) {
                                                if ("input_audio".equals(c.path("type").asText())) {
                                                    String transcript = c.path("transcript").asText();
                                                    if (!transcript.isEmpty()) {
                                                        log.info("User audio transcript: {}", transcript);
                                                        onFinalTranscript.accept(transcript);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Speech detection events
                                else if ("input_audio_buffer.speech_started".equals(eventType)) {
                                    log.info("Speech detected in audio buffer");
                                }
                                else if ("input_audio_buffer.speech_stopped".equals(eventType)) {
                                    log.info("Speech stopped in audio buffer");
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse realtime message: {}", json, e);
                        }
                        webSocket.request(1);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        log.info("Realtime WebSocket closed: {} - {}", statusCode, reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("Realtime WebSocket error", error);
                    }
                }).thenAccept(w -> {});
    }

    public void setOnFinalTranscript(Consumer<String> consumer) {
        this.onFinalTranscript = consumer != null ? consumer : (s) -> {};
    }

    public void appendPcm16(byte[] buffer, int length) {
        WebSocket socket = this.ws;
        if (socket == null) {
            log.warn("WebSocket is null, cannot send audio");
            return;
        }
        byte[] copy = new byte[length];
        System.arraycopy(buffer, 0, copy, 0, length);
        String b64 = Base64.getEncoder().encodeToString(copy);
        try {
            String msg = mapper.createObjectNode()
                    .put("type", "input_audio_buffer.append")
                    .put("audio", b64)
                    .toString();
            
            // Log first message to verify format
            if (audioChunkCount == 0) {
                log.info("First audio chunk message (truncated): {}", msg.substring(0, Math.min(200, msg.length())));
                log.info("First 10 audio bytes: {}", java.util.Arrays.toString(java.util.Arrays.copyOf(copy, Math.min(10, length))));
            }
            audioChunkCount++;
            
            socket.sendText(msg, true);
            log.info("Sent {} bytes of audio (chunk #{})", length, audioChunkCount);
        } catch (Exception e) {
            log.warn("Failed to send audio chunk", e);
        }
    }

    public void commitAndCreateResponse() {
        WebSocket socket = this.ws;
        if (socket == null) return;
        try {
            String commit = mapper.createObjectNode().put("type", "input_audio_buffer.commit").toString();
            socket.sendText(commit, true);
            String create = mapper.createObjectNode().put("type", "response.create").toString();
            socket.sendText(create, true);
        } catch (Exception e) {
            log.warn("Failed to commit/create response", e);
        }
    }

    public void close() {
        WebSocket socket = this.ws;
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception ignored) {}
            this.ws = null;
        }
    }
}
