package ui.panels;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import db.DatabaseConnection;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserSettingsPanel extends VBox {

    private String username;
    private String userRole;

    public UserSettingsPanel(String username, String userRole) {
        this.username = username;
        this.userRole = userRole;
        
        setSpacing(20);
        setPadding(new Insets(0)); 

        // Title
        Text title = new Text("USER SETTINGS");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 42)); // Increased size to match Dashboard
        
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);

        // Info Card
        VBox infoCard = createCard();
        Label lblUser = new Label("Logged in as: " + username);
        lblUser.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblRole = new Label("Role: " + userRole);
        lblRole.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
        infoCard.getChildren().addAll(lblUser, lblRole);

        // Change Password Card
        VBox passCard = createCard();
        Label passTitle = new Label("Change Password");
        passTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        passTitle.setPadding(new Insets(0, 0, 15, 0));

        PasswordField currentPass = createStyledPasswordField("Current Password");
        PasswordField newPass = createStyledPasswordField("New Password");
        PasswordField confirmPass = createStyledPasswordField("Confirm New Password");

        Button btnUpdate = new Button("Update Password");
        btnUpdate.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        
        btnUpdate.setOnAction(e -> {
            updatePassword(currentPass.getText(), newPass.getText(), confirmPass.getText());
            currentPass.clear(); newPass.clear(); confirmPass.clear();
        });

        VBox form = new VBox(15, currentPass, newPass, confirmPass, btnUpdate);
        form.setMaxWidth(400);
        passCard.getChildren().addAll(passTitle, form);

        getChildren().addAll(title, infoCard, passCard);
    }

    private void updatePassword(String current, String newP, String confirm) {
        if (current.isEmpty() || newP.isEmpty() || confirm.isEmpty()) {
            ui.components.CustomDialog.showMessage("Error", "All fields are required.", true);
            return;
        }
        if (!newP.equals(confirm)) {
            ui.components.CustomDialog.showMessage("Error", "New passwords do not match.", true);
            return;
        }

        // --- NEW: STRONG PASSWORD VALIDATION ---
        String passwordRegex = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$";
        if (!newP.matches(passwordRegex)) {
            ui.components.CustomDialog.showMessage("Weak Password", 
                "Password must be at least 8 characters long, contain 1 uppercase letter, 1 number, and 1 special character.", true);
            return;
        }
        
      

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT Password FROM Users WHERE Username = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, username);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next()) {
                String savedHash = rs.getString("Password");
                String currentHash = ui.panels.UserManagementPanel.hashPassword(current);
                
                if (!savedHash.equals(currentHash)) {
                    ui.components.CustomDialog.showMessage("Error", "Current password is incorrect.", true);
                    return;
                }
                
                String updateSql = "UPDATE Users SET Password = ? WHERE Username = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setString(1, ui.panels.UserManagementPanel.hashPassword(newP));
                updatePs.setString(2, username);
                updatePs.executeUpdate();
                
                ui.components.CustomDialog.showMessage("Success", "Password updated successfully!", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ui.components.CustomDialog.showMessage("Database Error", "Failed to update password.", true);
        }
    }

    private VBox createCard() {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-padding: 25;");
        return card;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(40);
        pf.setStyle("-fx-background-radius: 5; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 0 10;");
        return pf;
    }
}