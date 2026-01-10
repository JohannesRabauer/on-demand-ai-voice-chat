package dev.rabauer.voice;

import io.quarkus.runtime.Quarkus;

public class Main {
    public static void main(String[] args) {
        // Run Quarkus in a background thread
        new Thread(() -> Quarkus.run(args)).start();
        
        // Keep main thread alive to allow SystemTray and AWT EDT to function
        try {
            Thread.sleep(500); // Give Quarkus time to initialize
            Thread.currentThread().join(); // Keep main thread alive indefinitely
        } catch (InterruptedException ignored) {}
    }
}
