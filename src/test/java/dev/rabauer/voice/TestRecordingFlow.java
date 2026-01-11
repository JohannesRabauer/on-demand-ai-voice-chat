package dev.rabauer.voice;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class TestRecordingFlow implements QuarkusApplication {

    @Inject
    AudioCaptureService audioCaptureService;

    @Override
    public int run(String... args) throws Exception {
        System.out.println("=== Testing Recording Flow ===");
        System.out.println("Starting 3-second recording...");
        
        audioCaptureService.startRecording();
        Thread.sleep(3000);
        audioCaptureService.stopRecording();
        
        System.out.println("Recording stopped. Waiting for transcript and TTS...");
        Thread.sleep(10000); // Give time for OpenAI response and playback
        
        System.out.println("=== Test Complete ===");
        return 0;
    }

    public static void main(String[] args) {
        Quarkus.run(TestRecordingFlow.class, args);
    }
}
