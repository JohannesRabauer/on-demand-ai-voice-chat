package dev.rabauer.voice;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LangchainAdapter {
    private static final Logger log = LoggerFactory.getLogger(LangchainAdapter.class);

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "app.systemPrompt", defaultValue = "You are a helpful assistant.")
    String systemPrompt;

    @ConfigProperty(name = "app.llm.model", defaultValue = "gpt-4o")
    String model;

    @ConfigProperty(name = "app.contextMemory.enabled", defaultValue = "false")
    boolean contextMemoryEnabled;

    private ChatLanguageModel chatModel;
    private List<ChatMessage> conversationHistory;

    @PostConstruct
    public void init() {
        log.info("Initializing LangChain4j with model: {}", model);
        log.info("Context memory enabled: {}", contextMemoryEnabled);
        chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .build();
        
        if (contextMemoryEnabled) {
            conversationHistory = new ArrayList<>();
            conversationHistory.add(new SystemMessage(systemPrompt));
        }
    }

    public String processTranscript(String transcript) {
        try {
            log.info("Processing transcript with LLM: {}", transcript);
            
            String response;
            if (contextMemoryEnabled) {
                // Add user message to history
                conversationHistory.add(new UserMessage(transcript));
                
                // Generate response with full conversation history
                response = chatModel.generate(conversationHistory).content().text();
                
                // Add AI response to history
                conversationHistory.add(new AiMessage(response));
                
                log.info("Conversation history size: {} messages", conversationHistory.size());
            } else {
                // No memory - just use system prompt + current message
                String fullPrompt = systemPrompt + "\n\nUser: " + transcript;
                response = chatModel.generate(fullPrompt);
            }
            
            log.info("LLM response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to process transcript with LLM", e);
            return "Sorry, I encountered an error processing your request.";
        }
    }

    public void clearConversationHistory() {
        if (contextMemoryEnabled && conversationHistory != null) {
            conversationHistory.clear();
            conversationHistory.add(new SystemMessage(systemPrompt));
            log.info("Conversation history cleared");
        }
    }
}
