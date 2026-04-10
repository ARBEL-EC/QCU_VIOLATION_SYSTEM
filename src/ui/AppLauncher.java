package ui;

public class AppLauncher {
    
    // Notice this class does NOT extend javafx.application.Application!
    
    public static void main(String[] args) {
        // This safely triggers the JavaFX initialization from a non-JavaFX class
        LoginFrame.main(args);
    }
}