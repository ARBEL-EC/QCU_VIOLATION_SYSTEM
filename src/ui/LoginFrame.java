package ui;

import db.DatabaseConnection; 
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static javafx.application.Application.launch;

public class LoginFrame extends Application {

    // --- Configuration ---
    private static final String FONT_CINZEL_URL = "https://fonts.gstatic.com/s/cinzel/v11/8vIJ7ww63mVu7gt78Uk.ttf";
    private static final String BG_COLOR = "#e5e8eb";
    private static final String INPUT_BORDER_COLOR = "#cccccc";

    @Override
    public void start(Stage primaryStage) {
        
        // 1. Setup the Gradient 
        LinearGradient titleGradient = new LinearGradient(
                0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000")),   // Bottom: Black
                new Stop(1, Color.web("#3432c7"))    // Top: Blue
        );

        // 2. Load Fonts
        Font cinzelFontLarge = loadFontSafe(FONT_CINZEL_URL, 32);
        Font cinzelFontSmall = loadFontSafe(FONT_CINZEL_URL, 20);

        // 3. Header Section (Logo + Text)
        ImageView logoView = loadImageView("/Icons/Logo.png", 80, 80);

        Text titleMain = new Text("QUEZON CITY UNIVERSITY");
        titleMain.setFont(cinzelFontLarge);
        titleMain.setFill(titleGradient); 

        Text titleSub = new Text("STUDENT VIOLATION SYSTEM");
        titleSub.setFont(cinzelFontSmall);
        titleSub.setFill(Color.BLACK); 

        VBox titleBox = new VBox(5, titleMain, titleSub);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox headerBox = new HBox(20, logoView, titleBox);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 30, 0)); 

        // 4. The Login Prompt 
        Text loginPrompt = new Text("Log in to your account.");
        loginPrompt.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.NORMAL, 18));
        loginPrompt.setFill(titleGradient);

        // 5. The Login Card Inputs
        TextField usernameField = createStyledTextField("Username");
        PasswordField passwordField = createStyledPasswordField("Password");
        
        // --- NEW: Pressing ENTER triggers login ---
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                processLogin(usernameField.getText(), passwordField.getText(), primaryStage);
            }
        });
        
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                processLogin(usernameField.getText(), passwordField.getText(), primaryStage);
            }
        });

        // Login Button Container
        StackPane loginBtnContainer = new StackPane();
        ImageView btnImageView = loadImageView("/Icons/login_btn.png", 150, 50);

        if (btnImageView.getImage() != null) {
            loginBtnContainer.getChildren().add(btnImageView);
            btnImageView.setPickOnBounds(true);
            btnImageView.setOnMouseEntered(e -> btnImageView.setOpacity(0.9));
            btnImageView.setOnMouseExited(e -> btnImageView.setOpacity(1.0));
            
            btnImageView.setOnMouseClicked(e -> processLogin(usernameField.getText(), passwordField.getText(), primaryStage));
            loginBtnContainer.setCursor(javafx.scene.Cursor.HAND);
        } else {
            // Fallback Button
            javafx.scene.control.Button fallbackBtn = new javafx.scene.control.Button("Log In");
            fallbackBtn.setStyle("-fx-background-color: #3432c7; -fx-text-fill: white; -fx-font-size: 14px; -fx-min-width: 150px;");
            fallbackBtn.setOnAction(e -> processLogin(usernameField.getText(), passwordField.getText(), primaryStage));
            loginBtnContainer.getChildren().add(fallbackBtn);
        }

        // Layout for the Card 
        VBox cardLayout = new VBox(25);
        cardLayout.setAlignment(Pos.CENTER);
        cardLayout.setPadding(new Insets(50, 50, 50, 50));
        cardLayout.getChildren().addAll(usernameField, passwordField, loginBtnContainer);

        // Card Styling
        StackPane cardPane = new StackPane(cardLayout);
        cardPane.setMaxWidth(400);
        cardPane.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-background-insets: 0;");
        
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(20.0);
        dropShadow.setOffsetY(5.0);
        dropShadow.setColor(Color.color(0.8, 0.8, 0.8, 0.5));
        cardPane.setEffect(dropShadow);

        // 6. Footer
        Label footerLabel = new Label("Accounts managed by system administrator.");
        footerLabel.setTextFill(Color.web("#555555"));
        footerLabel.setFont(Font.font("Arial", 12));
        
        VBox footerBox = new VBox(footerLabel);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setPadding(new Insets(20, 0, 0, 0));
        
        Region line = new Region();
        line.setStyle("-fx-background-color: #cccccc; -fx-min-height: 1px; -fx-max-height: 1px;");
        line.setMaxWidth(Double.MAX_VALUE);

        VBox bottomContainer = new VBox(10, line, footerBox);
        bottomContainer.setPadding(new Insets(20));

        // 7. Main Layout Assembly 
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");
        
        VBox centerContent = new VBox(20, headerBox, loginPrompt, cardPane); 
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(50, 0, 50, 0)); 

        root.setCenter(centerContent);
        root.setBottom(bottomContainer);

        // 8. Stage Setup
        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setTitle("Quezon City University - Student Violation");
        
        try {
            InputStream iconStream = getClass().getResourceAsStream("/Icons/Logo.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {}
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        root.requestFocus();
    }

    // --- SECURE DATABASE AUTHENTICATION LOGIC ---
    private void processLogin(String username, String password, Stage currentStage) {
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            ui.components.CustomDialog.showMessage("Validation Failed", "Username and password cannot be empty.", true);
            return;
        }

        // Hash the typed password before checking the DB
        String hashedInput = ui.panels.UserManagementPanel.hashPassword(password);
        String query = "SELECT Role FROM Users WHERE Username = ? AND Password = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                ui.components.CustomDialog.showMessage("Database Error", "Could not establish a connection to the database.", true);
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedInput); 

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String role = rs.getString("Role");
                        
                        // Close the current Login Window
                        currentStage.close();

                        // Open the Main Dashboard (Passing BOTH Username and Role!)
                        try {
                            MainFrame mainFrame = new MainFrame(username, role);
                            Stage mainStage = new Stage();
                            mainFrame.start(mainStage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        
                    } else {
                        ui.components.CustomDialog.showMessage("Login Failed", "Invalid username or password.", true);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ui.components.CustomDialog.showMessage("Database Error", "An issue occurred during the login process.", true);
        }
    }

    // --- Helper Methods ---
    private Font loadFontSafe(String url, double size) {
        try {
            Font f = Font.loadFont(url, size);
            if (f != null) return f;
        } catch (Exception e) { }
        return Font.font("Serif", FontWeight.BOLD, size);
    }

    private ImageView loadImageView(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                iv.setImage(img);
            }
        } catch (Exception e) { }
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        return iv;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(45);
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + INPUT_BORDER_COLOR + "; -fx-border-width: 1; -fx-background-color: white; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px; -fx-padding: 0 15 0 15;");
        return tf;
    }
    
    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(45);
        pf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + INPUT_BORDER_COLOR + "; -fx-border-width: 1; -fx-background-color: white; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px; -fx-padding: 0 15 0 15;");
        return pf;
    }

    public static void main(String[] args) {
        launch(args);
    }
}