package dev.rabauer.voice;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LangchainAdapter {
    private static final Logger log = LoggerFactory.getLogger(LangchainAdapter.class);

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "app.systemPrompt", defaultValue = "You are a helpful assistant.")
    String systemPrompt;

    @ConfigProperty(name = "app.llm.model", defaultValue = "gpt-4o")
    String model;

    private ChatLanguageModel chatModel;

    @PostConstruct
    public void init() {
        log.info("Initializing LangChain4j with model: {}", model);
        chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .build();
    }

    public String processTranscript(String transcript) {
        try {
            log.info("Processing transcript with LLM: {}", transcript);
            String fullPrompt = systemPrompt + "\n\nUser: " + transcript;
            String response = chatModel.generate(fullPrompt);
            log.info("LLM response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to process transcript with LLM", e);
            return "Sorry, I encountered an error processing your request.";
        }
    }
}
