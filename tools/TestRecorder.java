import javax.sound.sampled.*;
import java.io.*;

public class TestRecorder {
    public static void main(String[] args) throws Exception {
        AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("TargetDataLine not supported with format: " + format);
            return;
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        System.out.println("Recording for 3 seconds...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long end = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < end) {
            int read = line.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.write(buffer, 0, read);
            }
        }

        line.stop();
        line.close();

        byte[] audioBytes = out.toByteArray();
        File tmp = File.createTempFile("test-recording-", ".wav");

        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
             AudioInputStream ais = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tmp);
        }

        System.out.println("Saved to: " + tmp.getAbsolutePath());
    }
}
