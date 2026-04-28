package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import ui.panels.*;

public class MainFrame extends Application {

    private StackPane mainContentArea;
    private List<Button> navButtons = new ArrayList<>();
    private String userRole; 
    private String username; 

    public MainFrame(String username, String role) {
        this.username = username;
        this.userRole = role;
    }

    public MainFrame() {
        this.username = "Admin";
        this.userRole = "Super Admin"; 
    }

    @Override
    public void start(Stage primaryStage) {
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f3f4f6; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif;");

        // ==========================================
        // 1. SIDEBAR
        // ==========================================
        VBox sidebar = new VBox(5); 
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 2, 0);");
        sidebar.setPadding(new Insets(40, 20, 30, 20)); 

        ImageView logoView = loadImageView("/Icons/Logo.png", 100, 100);
        VBox logoContainer = new VBox(logoView);
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPadding(new Insets(0, 0, 30, 0)); 

        Button btnDashboard = createNavButton("Dashboard", true);
        Button btnLogViolation = createNavButton("Log Violation", false);
        Button btnRecords = createNavButton("Student Records", false);
        Button btnReports = createNavButton("Reports", false);
        
        sidebar.getChildren().addAll(logoContainer, btnDashboard, btnLogViolation, btnRecords, btnReports);

        // CATEGORY: ADMINISTRATION
        if ("Admin".equalsIgnoreCase(userRole) || "Super Admin".equalsIgnoreCase(userRole)) {
            sidebar.getChildren().add(createCategoryLabel("ADMINISTRATION"));
            Button btnAudit = createNavButton("Audit Logs", false);
            btnAudit.setOnAction(e -> handleNavClick(btnAudit, "Audit"));
            sidebar.getChildren().add(btnAudit);
        }
        
        // CATEGORY: SUPER ADMIN
        if ("Super Admin".equalsIgnoreCase(userRole)) {
            sidebar.getChildren().add(createCategoryLabel("SUPER ADMIN")); 
            Button btnUsers = createNavButton("User Management", false);
            btnUsers.setOnAction(e -> handleNavClick(btnUsers, "Users"));
            sidebar.getChildren().add(btnUsers);
        }

        // CATEGORY: SETTINGS
        sidebar.getChildren().add(createCategoryLabel("SETTINGS"));
        Button btnSettings = createNavButton("User Settings", false);
        btnSettings.setOnAction(e -> handleNavClick(btnSettings, "Settings"));
        sidebar.getChildren().add(btnSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Log Out Button
        Button btnLogout = new Button("Log Out");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #777777; -fx-font-size: 14px; -fx-cursor: hand;");
        ImageView logoutIcon = loadImageView("/Icons/logout.png", 16, 16);
        if (logoutIcon.getImage() != null) btnLogout.setGraphic(logoutIcon);
        
        btnLogout.setOnAction(e -> {
            if (ui.components.CustomDialog.showConfirmation("Log Out", "Are you sure you want to log out?")) {
                primaryStage.close();
                try { new LoginFrame().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        sidebar.getChildren().addAll(spacer, btnLogout);
        root.setLeft(sidebar);

        // ==========================================
        // 2. MAIN AREA (CENTER)
        // ==========================================
        BorderPane contentPane = new BorderPane();
        
        // --- DYNAMIC TOP HEADER ---
        HBox topHeader = new HBox(15);
        topHeader.setAlignment(Pos.CENTER_RIGHT);
        topHeader.setPadding(new Insets(20, 40, 10, 40));
        
        TextField searchBar = new TextField();
        searchBar.setPromptText("Search Action or User...");
        searchBar.setPrefWidth(300);
        searchBar.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; -fx-background-color: white; -fx-padding: 5 15;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        // User Profile Area
        ImageView profileIcon = loadImageView("/Icons/user_icon.png", 35, 35); // Optional: Add a user icon to your folder
        Text txtUsername = new Text(username.toUpperCase());
        txtUsername.setFont(Font.font("System", FontWeight.BOLD, 14));
        txtUsername.setFill(Color.web("#4b5563"));
        
        HBox userBox = new HBox(10, profileIcon, txtUsername);
        userBox.setAlignment(Pos.CENTER);
        
        topHeader.getChildren().addAll(searchBar, headerSpacer, userBox);
        contentPane.setTop(topHeader);

        // --- FULLSCREEN FIX ---
        mainContentArea = new StackPane();
        mainContentArea.setPadding(new Insets(20, 40, 40, 40));
        mainContentArea.setAlignment(Pos.TOP_LEFT); 
        contentPane.setCenter(mainContentArea);
        
        root.setCenter(contentPane);

        // ==========================================
        // 3. STAGE SETUP
        // ==========================================
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Student Violation System");
        
        try {
            InputStream iconStream = getClass().getResourceAsStream("/Icons/Logo.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {}

        primaryStage.setScene(scene);
        primaryStage.show();

        // Button Clicks
        btnDashboard.setOnAction(e -> handleNavClick(btnDashboard, "Dashboard"));
        btnLogViolation.setOnAction(e -> handleNavClick(btnLogViolation, "LogViolation"));
        btnRecords.setOnAction(e -> handleNavClick(btnRecords, "Records"));
        btnReports.setOnAction(e -> handleNavClick(btnReports, "Reports"));

        handleNavClick(btnDashboard, "Dashboard");
    }

    private Label createCategoryLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#9ca3af"));
        lbl.setPadding(new Insets(15, 0, 5, 10));
        return lbl;
    }

    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE); 
        btn.setAlignment(Pos.CENTER_LEFT); 
        btn.setPadding(new Insets(10, 10, 10, 20));
        btn.setPrefHeight(40); 

        String baseStyle = "-fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10;";

        if (isActive) {
            btn.setStyle(baseStyle + " -fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-weight: bold;");
        } else {
            btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #6b7280;");
        }

        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#e5e7eb")) btn.setStyle(baseStyle + " -fx-background-color: #f3f4f6; -fx-text-fill: #111827;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#e5e7eb")) btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #6b7280;");
        });

        navButtons.add(btn);
        return btn;
    }

    private void handleNavClick(Button clickedBtn, String panelName) {
        String baseStyle = "-fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10;";
        for (Button btn : navButtons) {
            if (btn == clickedBtn) {
                btn.setStyle(baseStyle + " -fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-weight: bold;");
            } else {
                btn.setStyle(baseStyle + " -fx-background-color: transparent; -fx-text-fill: #6b7280;");
            }
        }
        
        mainContentArea.getChildren().clear();

        Region activePanel = null;
        switch (panelName) {
            case "Dashboard": activePanel = new DashboardPanel(); break;
            case "LogViolation": activePanel = new LogViolationPanel(); break;
            case "Records": activePanel = new RecordsPanel(userRole); break;
            case "Reports": activePanel = new ReportsPanel(); break;    
            case "Audit": activePanel = new AuditPanel(); break;
            case "Users": activePanel = new UserManagementPanel(); break;
            case "Settings": activePanel = new UserSettingsPanel(username, userRole); break;
        }

        if (activePanel != null) {
            activePanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            mainContentArea.getChildren().add(activePanel);
        }
    }

    private ImageView loadImageView(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception e) {}
        iv.setFitWidth(w); iv.setFitHeight(h); iv.setPreserveRatio(true);
        return iv;
    }

    public static void main(String[] args) {
        launch(args);
    }
}