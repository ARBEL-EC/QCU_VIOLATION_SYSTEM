package ui.panels;

import db.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserManagementPanel extends BorderPane {

    private TableView<UserItem> table;
    private ObservableList<UserItem> userList;

    public UserManagementPanel() {
        setPadding(new Insets(30, 40, 40, 40));

        setTop(createHeader());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 0, 0, 0));
        
        table = new TableView<>();
        table.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<UserItem, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<UserItem, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        colRole.setCellFactory(column -> new TableCell<UserItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 11px;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        table.getColumns().addAll(colUser, colRole);
        
        content.getChildren().add(table);
        setCenter(content);

        userList = FXCollections.observableArrayList();
        table.setItems(userList);
        loadUsers();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("USER MANAGEMENT");
        title.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.BOLD, 28));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add User");
        btnAdd.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> openUserDialog(null));

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> {
            UserItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openUserDialog(selected);
            else ui.components.CustomDialog.showMessage("No Selection", "Please select a user to edit.", true);
        });

        Button btnDelete = new Button("Delete");
        btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> deleteSelectedUser());

        HBox btnBox = new HBox(10, btnAdd, btnEdit, btnDelete);
        header.getChildren().addAll(title, spacer, btnBox);
        return header;
    }

    private void loadUsers() {
        userList.clear();
        String sql = "SELECT UserID, Username, Role FROM Users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                userList.add(new UserItem(rs.getInt("UserID"), rs.getString("Username"), rs.getString("Role")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedUser() {
        UserItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (selected.getUsername().equalsIgnoreCase("admin") || selected.getUsername().equalsIgnoreCase("superadmin")) {
            ui.components.CustomDialog.showMessage("Access Denied", "You cannot delete the primary Super Admin.", true);
            return;
        }

        String sql = "DELETE FROM Users WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selected.getId());
            ps.executeUpdate();
            loadUsers();
            ui.components.CustomDialog.showMessage("Success", "User deleted successfully.", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openUserDialog(UserItem user) {
        boolean isEdit = (user != null);
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED); // FIX: Completely removes the title bar

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #d1d5db; -fx-border-width: 2; -fx-border-radius: 10;");
        
        // FIX: Wider layout box for Add User
        layout.setPrefWidth(450);
        layout.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Text title = new Text(isEdit ? "Edit User" : "Add New User");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 20));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");
        if (isEdit) txtUsername.setText(user.getUsername());

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText(isEdit ? "Leave blank to keep current password" : "Password");

        ComboBox<String> cmbRole = new ComboBox<>(FXCollections.observableArrayList("Super Admin", "Admin", "Staff"));
        cmbRole.setPromptText("Select Role");
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        if (isEdit) cmbRole.setValue(user.getRole());

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Save");
        btnSave.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String uName = txtUsername.getText().trim();
            String pass = txtPassword.getText();
            String role = cmbRole.getValue();

            if (uName.isEmpty() || role == null || (!isEdit && pass.isEmpty())) {
                ui.components.CustomDialog.showMessage("Error", "Username and Role are required.", true);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (isEdit) {
                    if (pass.isEmpty()) {
                        String sql = "UPDATE Users SET Username = ?, Role = ? WHERE UserID = ?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, uName);
                        ps.setString(2, role);
                        ps.setInt(3, user.getId());
                        ps.executeUpdate();
                    } else {
                        String sql = "UPDATE Users SET Username = ?, Password = ?, Role = ? WHERE UserID = ?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, uName);
                        ps.setString(2, hashPassword(pass));
                        ps.setString(3, role);
                        ps.setInt(4, user.getId());
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO Users (Username, Password, Role) VALUES (?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, uName);
                    ps.setString(2, hashPassword(pass));
                    ps.setString(3, role);
                    ps.executeUpdate();
                }
                loadUsers();
                dialog.close();
            } catch (Exception ex) {
                ui.components.CustomDialog.showMessage("Database Error", "Username might already exist.", true);
            }
        });

        btnBox.getChildren().addAll(btnCancel, btnSave);
        layout.getChildren().addAll(title, new Label("Username:"), txtUsername, new Label("Password:"), txtPassword, new Label("Role:"), cmbRole, btnBox);

        // --- DARK OVERLAY ---
        StackPane darkOverlay = new StackPane(layout);
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);"); 
        darkOverlay.setPadding(new Insets(20));

        Scene scene = new Scene(darkOverlay);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        javafx.stage.Window mainWindow = this.getScene().getWindow();
        if(mainWindow != null) {
            dialog.initOwner(mainWindow);
            dialog.setX(mainWindow.getX());
            dialog.setY(mainWindow.getY());
            dialog.setWidth(mainWindow.getWidth());
            dialog.setHeight(mainWindow.getHeight());
        } else {
            dialog.centerOnScreen();
        }

        dialog.showAndWait();
    }

    public static String hashPassword(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class UserItem {
        private final int id;
        private final String username;
        private final String role;

        public UserItem(int id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}