# On-Demand AI Voice Chat

A Quarkus-based Windows tray application that records audio via global hotkey, streams it to OpenAI Realtime API for transcription, processes responses through a langchain4j adapter, and plays TTS audio back.

## Features ✅

- **Background Service**: Runs as Windows tray icon with Record/Exit menu
- **Global Hotkey**: F8 (configurable) toggles recording on/off
- **Streaming STT**: PCM16 audio streamed to OpenAI Realtime WebSocket API
- **LLM Integration**: Transcripts processed via langchain4j adapter (currently stub—add your chain)
- **TTS Playback**: Response synthesized via OpenAI TTS and played through speakers
- **Configuration**: External `application.properties` for API keys, voice, prompts

## Requirements

- Java 21 (Eclipse Temurin recommended)
- Maven 3.9+
- OpenAI API key with Realtime API access
- Microphone for audio input

## Quick Start

1. **Set API Key**:
   ```powershell
   $env:OPENAI_API_KEY="sk-..."
   ```
   Or edit [`src/main/resources/application.properties`](src/main/resources/application.properties):
   ```properties
   app.ai.apiKey=sk-...
   ```

2. **Build**:
   ```powershell
   mvn clean package
   ```

3. **Run**:
   ```powershell
   java -jar target/quarkus-app/quarkus-run.jar
   ```

4. **Test**:
   - Tray icon appears in system tray
   - Right-click → **Record** or press **F8**
   - Speak into microphone
   - Press **F8** again to stop
   - Watch logs for transcript and listen for TTS reply

## Configuration

Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
# Hotkey
app.hotkey=F8

# OpenAI
openai.api.key=${OPENAI_API_KEY}
openai.realtime.url=wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview
app.tts.url=https://api.openai.com/v1/audio/speech
app.voice=alloy

# AI
app.systemPrompt=You are a helpful assistant.

# Audio
app.audio.sampleRate=16000
app.audio.channels=1
app.audio.sampleSizeInBits=16
```

## Architecture

```
Hotkey Press (F8)
    ↓
AudioCaptureService
    ├─ TargetDataLine (capture PCM16)
    ├─ Stream to RealtimeOpenAIClient (WebSocket)
    └─ On stop: commit & request response
    
RealtimeOpenAIClient
    ├─ Append audio chunks (base64)
    ├─ Receive transcript events
    └─ Emit final transcript
    
LangchainAdapter
    └─ Process transcript (stub—add your chain)
    
TtsPlayer
    ├─ Request TTS WAV from OpenAI
    └─ Play via SourceDataLine
```

## Next Steps

- [ ] Replace `LangchainAdapter` stub with real langchain4j chains
- [ ] Add JNativeHook global hotkey (currently stub)
- [ ] Implement runtime config reload
- [ ] Package with `jpackage` for Windows installer
- [ ] Add streaming partial transcripts for live feedback
- [ ] Support other STT/TTS providers (Google, Azure, local)
- [ ] Secure API key storage (Windows Credential Manager)

## Troubleshooting

**No transcript received?**
- Check logs for WebSocket connection errors
- Verify `OPENAI_API_KEY` is set
- Ensure Realtime API access is enabled in your OpenAI account
- Check message schema matches current OpenAI Realtime format

**TTS playback fails?**
- Verify `app.tts.url` points to correct endpoint
- Check TTS response format (currently expects WAV)
- Inspect logs for HTTP status codes

**Hotkey not working?**
- JNativeHook integration is stubbed—currently use tray menu
- Implement full JNativeHook listener to enable F8 toggle

## License

MIT

