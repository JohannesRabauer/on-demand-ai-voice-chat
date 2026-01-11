package dev.rabauer.voice;

import jakarta.inject.Singleton;

@Singleton
public class LangchainAdapter {

    // TODO: Replace with langchain4j chain invocation
    public String processTranscript(String transcript) {
        // Basic placeholder logic for MVP
        return "You said: " + transcript;
    }
}
