package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import ui.panels.DashboardPanel;
import ui.panels.LogViolationPanel;
import ui.panels.AuditPanel;
import ui.panels.RecordsPanel;

public class MainFrame extends Application {

    private StackPane mainContentArea;
    private List<Button> navButtons = new ArrayList<>();
    private String userRole; // Stores the current user's role

    // Constructor to accept role from LoginFrame
    public MainFrame(String role) {
        this.userRole = role;
    }

    // Default constructor for testing/direct launch
    public MainFrame() {
        this.userRole = "Admin"; // Fallback
    }

    @Override
    public void start(Stage primaryStage) {
        
        // --- ROOT LAYOUT ---
        BorderPane root = new BorderPane();
        // Light grey background for the main application area
        root.setStyle("-fx-background-color: #eef1f5; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif;");

        // ==========================================
        // 1. SIDEBAR (LEFT)
        // ==========================================
        VBox sidebar = new VBox(10); // 10px spacing between buttons
        sidebar.setPrefWidth(240);
        // White background for sidebar with subtle drop shadow
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 2, 0);");
        sidebar.setPadding(new Insets(40, 20, 30, 20)); // Top, Right, Bottom, Left padding

        // --- Logo Section ---
        ImageView logoView = loadImageView("/Icons/Logo.png", 100, 100);
        VBox logoContainer = new VBox(logoView);
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPadding(new Insets(0, 0, 40, 0)); // Spacing below the logo

        // --- Navigation Buttons ---
        Button btnDashboard = createNavButton("Dashboard", true);
        Button btnLogViolation = createNavButton("Log Violation", false);
        Button btnRecords = createNavButton("Student Records", false);
        Button btnAudit = createNavButton("Audit Logs", false);

        // Placeholder Click Events (Structure Ready)
        btnDashboard.setOnAction(e -> handleNavClick(btnDashboard, "Dashboard"));
        btnLogViolation.setOnAction(e -> handleNavClick(btnLogViolation, "LogViolation"));
        btnRecords.setOnAction(e -> handleNavClick(btnRecords, "Records"));
        btnAudit.setOnAction(e -> handleNavClick(btnAudit, "Audit"));

        // --- Spacer ---
        // This pushes the logout button to the absolute bottom of the VBox
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // --- Log Out Button ---
        Button btnLogout = new Button("Log Out");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #777777; -fx-font-size: 14px; -fx-cursor: hand;");
        
        // Load logout icon if it exists
        ImageView logoutIcon = loadImageView("/Icons/logout.png", 16, 16);
        if (logoutIcon.getImage() != null) {
            btnLogout.setGraphic(logoutIcon);
            btnLogout.setGraphicTextGap(10);
        }
        
        btnLogout.setOnAction(e -> {
            // Custom Confirmation Popup
            if (ui.components.CustomDialog.showConfirmation("Log Out", "Are you sure you want to log out of your account?")) {
                primaryStage.close();
                try {
                    new LoginFrame().start(new Stage());
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                }
            }
        });

        // --- Role-Based Sidebar Assembly ---
        sidebar.getChildren().addAll(logoContainer, btnDashboard, btnLogViolation, btnRecords);
        
        // Only add Audit Logs if user is Admin
        if ("Admin".equalsIgnoreCase(userRole)) {
            sidebar.getChildren().add(btnAudit);
        }
        
        sidebar.getChildren().addAll(spacer, btnLogout);

        root.setLeft(sidebar);

        // ==========================================
        // 2. MAIN AREA (CENTER)
        // ==========================================
        BorderPane contentPane = new BorderPane();

        // --- Dynamic Center Content Area ---
        mainContentArea = new StackPane();
        mainContentArea.setPadding(new Insets(30, 40, 40, 40));
        
        // Temporary placeholder text so you can see where the panels will go
        Label placeholderText = new Label("Select an option from the sidebar");
        placeholderText.setStyle("-fx-font-size: 18px; -fx-text-fill: #aaaaaa;");
        mainContentArea.getChildren().add(placeholderText);

        contentPane.setCenter(mainContentArea);

        // Add to root
        root.setCenter(contentPane);

        // ==========================================
        // 3. STAGE SETUP
        // ==========================================
        Scene scene = new Scene(root, 1100, 768);
        primaryStage.setTitle("Student Violation System - Dashboard");
        
        // --- Application Logo ---
        try {
            InputStream iconStream = getClass().getResourceAsStream("/Icons/Logo.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) { 
            System.out.println("Logo not found for stage icon."); 
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load default dashboard panel on startup
        handleNavClick(btnDashboard, "Dashboard");
    }

    // --- HELPER: Create Navigation Button ---
    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); // Stretch to fill width
        btn.setAlignment(Pos.CENTER);
        btn.setPrefHeight(45); // Thick enough to match image

        // Base styles applied to all buttons
        String baseStyle = "-fx-font-size: 15px; -fx-cursor: hand; -fx-background-radius: 25;";

        if (isActive) {
            btn.setStyle(baseStyle + " -fx-background-color: #b8b8b8; -fx-text-fill: #333333;");
        } else {
            btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #666666;");
        }

        // Hover Effect
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#b8b8b8")) { // Only hover effect if inactive
                btn.setStyle(baseStyle + " -fx-background-color: #f0f0f0; -fx-text-fill: #333333;");
            }
        });

        // Remove Hover Effect
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#b8b8b8")) { 
                btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #666666;");
            }
        });

        navButtons.add(btn);
        return btn;
    }

   // --- HELPER: Handle Click & Update Active State ---
    private void handleNavClick(Button clickedBtn, String panelName) {
        System.out.println("Navigating to: " + panelName);
        
        String baseStyle = "-fx-font-size: 15px; -fx-cursor: hand; -fx-background-radius: 25;";
        
        // 1. Loop through all buttons and reset styles
        for (Button btn : navButtons) {
            if (btn == clickedBtn) {
                // Active style
                btn.setStyle(baseStyle + " -fx-background-color: #b8b8b8; -fx-text-fill: #333333;");
            } else {
                // Inactive style
                btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #666666;");
            }
        }
        
        // 2. Clear the current content from the screen
        mainContentArea.getChildren().clear();

        // 3. Load the actual panel based on the button clicked
        switch (panelName) {
            case "Records":
                // Pass the role to RecordsPanel for Staff restrictions
                RecordsPanel recordsPanel = new RecordsPanel(userRole);
                mainContentArea.getChildren().add(recordsPanel);
                break;
                
            case "Dashboard":
                DashboardPanel dashboardPanel = new DashboardPanel();
                mainContentArea.getChildren().add(dashboardPanel);
                break;
                
            case "LogViolation":
                LogViolationPanel logViolationPanel = new LogViolationPanel();
                mainContentArea.getChildren().add(logViolationPanel);
                break;
                
            case "Audit":
                AuditPanel auditPanel = new AuditPanel();
                mainContentArea.getChildren().add(auditPanel);
                break;
                
            default:
                Label defaultTemp = new Label(panelName + " is empty.");
                defaultTemp.setStyle("-fx-font-size: 18px; -fx-text-fill: #aaaaaa;");
                mainContentArea.getChildren().add(defaultTemp);
                break;
        }
    }

    // --- HELPER: Safe Image Loading ---
    private ImageView loadImageView(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                iv.setImage(img);
            } else {
                System.out.println("WARNING: Could not find image at: " + path);
            }
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
        }
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        return iv;
    }

    public static void main(String[] args) {
        launch(args);
    }
}