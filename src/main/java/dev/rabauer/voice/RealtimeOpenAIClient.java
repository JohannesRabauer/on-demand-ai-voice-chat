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
    private CompletableFuture<String> transcriptFuture = new CompletableFuture<>();
    private StringBuilder textBuffer = new StringBuilder();

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
                        log.info("Realtime WebSocket opened");
                        
                        // Update session to enable input audio transcription
                        try {
                            // According to docs, we need to specify type: "realtime" for conversation sessions
                            var formatConfig = mapper.createObjectNode();
                            formatConfig.put("type", "audio/pcm");
                            formatConfig.put("rate", 24000);
                            
                            var transcriptionConfig = mapper.createObjectNode();
                            transcriptionConfig.put("model", "whisper-1");
                            
                            var turnDetectionConfig = mapper.createObjectNode();
                            turnDetectionConfig.put("type", "server_vad");
                            turnDetectionConfig.put("threshold", 0.5);
                            turnDetectionConfig.put("prefix_padding_ms", 300);
                            turnDetectionConfig.put("silence_duration_ms", 500);
                            
                            var inputConfig = mapper.createObjectNode();
                            inputConfig.set("format", formatConfig);
                            inputConfig.set("transcription", transcriptionConfig);
                            inputConfig.set("turn_detection", turnDetectionConfig);
                            
                            var audioConfig = mapper.createObjectNode();
                            audioConfig.set("input", inputConfig);
                            
                            var session = mapper.createObjectNode();
                            session.put("type", "realtime");  // Explicitly set session type for conversation mode
                            session.set("audio", audioConfig);
                            
                            var sessionUpdate = mapper.createObjectNode();
                            sessionUpdate.put("type", "session.update");
                            sessionUpdate.set("session", session);
                            
                            webSocket.sendText(sessionUpdate.toString(), true);
                            log.info("Session update sent: enable input audio transcription (realtime conversation mode)");
                        } catch (Exception e) {
                            log.error("Failed to update session", e);
                        }
                        
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        // Accumulate text chunks
                        textBuffer.append(data);
                        
                        // Only process when we have the complete message
                        if (!last) {
                            webSocket.request(1);
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        String json = textBuffer.toString();
                        textBuffer.setLength(0); // Clear buffer for next message
                        
                        // Skip non-JSON messages (likely audio data sent as base64 text)
                        if (!json.startsWith("{")) {
                            log.debug("Skipping non-JSON message (likely audio data): {} chars", json.length());
                            webSocket.request(1);
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        try {
                            JsonNode n = mapper.readTree(json);
                            log.info("Realtime event: {}", json);
                            
                            if (n.has("type")) {
                                String eventType = n.get("type").asText();
                                
                                // User input audio transcription (what the user said)
                                if ("conversation.item.input_audio_transcription.completed".equals(eventType)) {
                                    String transcript = n.path("transcript").asText();
                                    if (!transcript.isEmpty()) {
                                        log.info("=== USER INPUT TRANSCRIPT ===");
                                        log.info("You said: {}", transcript);
                                        transcriptFuture.complete(transcript);
                                        onFinalTranscript.accept(transcript);
                                    }
                                }
                                // AI response transcript (what the AI is saying back)
                                else if ("response.output_audio_transcript.delta".equals(eventType)) {
                                    String delta = n.path("delta").asText();
                                    if (!delta.isEmpty()) {
                                        log.info("AI response delta: {}", delta);
                                    }
                                }
                                else if ("response.output_audio_transcript.done".equals(eventType)) {
                                    String transcript = n.path("transcript").asText();
                                    if (!transcript.isEmpty()) {
                                        log.info("=== AI RESPONSE TRANSCRIPT ===");
                                        log.info("AI said: {}", transcript);
                                    }
                                }
                                // Speech detection events
                                else if ("input_audio_buffer.speech_started".equals(eventType)) {
                                    log.info("Speech detected in audio buffer");
                                }
                                else if ("input_audio_buffer.speech_stopped".equals(eventType)) {
                                    log.info("Speech stopped in audio buffer");
                                }
                                // Session events
                                else if ("session.created".equals(eventType) || "session.updated".equals(eventType)) {
                                    // Log the session config to verify transcription is enabled
                                    JsonNode transcription = n.path("session").path("audio").path("input").path("transcription");
                                    log.info("Session {}: transcription config = {}", eventType, transcription);
                                }
                                // Error handling
                                else if ("error".equals(eventType)) {
                                    log.error("OpenAI error: {} - {}", n.path("code").asText(), n.path("message").asText());
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse realtime message: {}", json, e);
                        }
                        webSocket.request(1);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                        // OpenAI sends audio responses as binary data, we can ignore these for now
                        // (they're the AI's voice, not needed for transcription)
                        int bytes = data.remaining();
                        log.debug("Received {} bytes of binary audio data", bytes);
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
            // Reset the future for new recording
            transcriptFuture = new CompletableFuture<>();
            
            String commit = mapper.createObjectNode().put("type", "input_audio_buffer.commit").toString();
            socket.sendText(commit, true);
            String create = mapper.createObjectNode().put("type", "response.create").toString();
            socket.sendText(create, true);
            
            log.info("Committed audio and requested response, waiting for transcription...");
        } catch (Exception e) {
            log.warn("Failed to commit/create response", e);
        }
    }
    
    public CompletableFuture<String> waitForTranscript() {
        return transcriptFuture;
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
