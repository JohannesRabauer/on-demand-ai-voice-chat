package dev.rabauer.voice;

/**
 * Manual test for AudioCaptureService (record for 3 seconds)
 */
public class TestAudioCapture {
    public static void main(String[] args) throws InterruptedException {
        AudioCaptureService service = new AudioCaptureService();
        service.init();

        RecorderListener listener = new RecorderListener() {
            @Override
            public void onRecordingStarted() {
                System.out.println("[TEST] Recording started");
            }

            @Override
            public void onRecordingStopped(java.io.File wavFile) {
                System.out.println("[TEST] Recording stopped, saved to: " + wavFile.getAbsolutePath());
            }

            @Override
            public void onProcessingStarted() {
                System.out.println("[TEST] Processing started");
            }

            @Override
            public void onProcessingFinished() {
                System.out.println("[TEST] Processing finished");
            }
        };

        service.setListener(listener);

        System.out.println("Starting recording test for 3 seconds...");
        service.startRecording();
        Thread.sleep(3000);
        service.stopRecording();

        System.out.println("Test complete!");
    }
}
