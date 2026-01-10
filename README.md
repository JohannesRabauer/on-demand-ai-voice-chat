# On-Demand AI Voice Chat (MVP)

This Quarkus-based project runs in the background (Windows tray) and toggles microphone recording via a global hotkey (F8 by default). Recorded audio is saved to a temporary WAV and can be piped to an STT provider, then fed to langchain4j and synthesized back to TTS.

Quick start:
- Set `app.ai.apiKey` in `src/main/resources/application.properties`
- `mvn package` then `java -jar target/on-demand-ai-voice-0.1.0-SNAPSHOT-runner.jar`

Next steps:
- Implement provider adapters for streaming STT and TTS
- Add GUI config and runtime reload
- Create Windows installer via `jpackage`
