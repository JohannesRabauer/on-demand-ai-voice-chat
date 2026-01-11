<p align="center">
  <img src="Icon_cropped.png" alt="On-Demand AI Voice Chat" width="200"/>
</p>

# On-Demand AI Voice Chat

A Quarkus-based Windows tray application that records audio via global hotkey, transcribes it using OpenAI Whisper API, processes responses through GPT-4 with langchain4j, and plays TTS audio back.

## 📥 Download

**Latest Release**: [Download OnDemandAIVoice-Portable-Windows.zip](../../releases/latest)

Pre-built portable package with bundled Java runtime - just extract and run! No installation required.

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
- **OpenAI API key** - Get one from [platform.openai.com](https://platform.openai.com/api-keys)
- Microphone for audio input

## Quick Start

**⚠️ Important: Set your OpenAI API key before running!**

1. **Set API Key** (choose one method):
   
   **Option A - Environment Variable** (recommended for development):
   ```powershell
   $env:OPENAI_API_KEY="sk-proj-your-actual-api-key-here"
   ```
   
   **Option B - Configuration File** (recommended for distribution):
   
   Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):
   ```properties
   app.ai.apiKey=sk-proj-your-actual-api-key-here
   ```
   
   Or for portable package: Edit `OnDemandAIVoice/app/application.properties`

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

## Building an Installer

You can create a portable Windows package using jpackage (included with JDK 21+):

### Portable Package (Recommended - No Installation Required)
```powershell
.\create-portable-package.ps1
```
Creates `OnDemandAIVoice-Portable-1.0.0.zip` (~70 MB) containing:
- Self-contained application with bundled Java runtime
- OnDemandAIVoice.exe launcher
- All dependencies included
- No installation needed - just extract and run!

**Users can:**
1. Extract the ZIP file anywhere
2. Double-click `OnDemandAIVoice.exe`
3. App starts in system tray - press F8 to use

### Windows Installer (Requires WiX Toolset)
```powershell
.\create-installer.ps1
```
Creates an MSI or EXE installer. **Note:** Requires [WiX Toolset 3.0+](https://wixtoolset.org) to be installed and added to PATH.

### Simple Launcher
If you don't need distribution, just run:
```cmd
run.bat
```
Requires Java 21+ installed on the system.

### Manual Installation
1. Copy the entire project folder to the target location
2. Ensure `Icon_cropped.png` is in the same directory
3. Set your OpenAI API key in `application.properties`
4. Run `java -jar target\quarkus-app\quarkus-run.jar`

## Next Steps

- [x] Global hotkey support (F8)
- [x] Whisper API transcription
- [x] LangChain4j integration with GPT-4
- [x] Context memory support
- [x] Windows installer scripts
- [ ] Auto-start on Windows login
- [ ] Secure API key storage (Windows Credential Manager)
- [ ] Support other STT/TTS providers (Google, Azure, local)

## Troubleshooting

**No transcript received?**
- Check logs for errors during Whisper API call
- Verify `OPENAI_API_KEY` is set correctly
- Ensure audio is being recorded (check temp WAV file)

**TTS playback fails?**
- Verify `app.tts.url` points to correct endpoint
- Check TTS response format (currently expects WAV)
- Inspect logs for HTTP status codes

**Hotkey not working?**
- JNativeHook requires accessibility permissions on some systems
- Check logs for "Global hotkey F8 registered successfully"
- Try running as administrator if needed

**Icon not showing in tray?**
- Ensure `Icon_cropped.png` is in the application directory
- Check logs for icon loading warnings

## License

MIT

