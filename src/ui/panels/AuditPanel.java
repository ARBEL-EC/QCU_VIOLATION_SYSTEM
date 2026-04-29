package ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import db.DatabaseConnection;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuditPanel extends VBox {

    private TableView<AuditLog> table;
    private ObservableList<AuditLog> masterData;

    public AuditPanel() {
        // 1. MAIN LAYOUT SETUP
        setSpacing(20);
        setPadding(new Insets(10, 0, 0, 0)); // Clean padding at the top
        setAlignment(Pos.TOP_CENTER);

        // ==========================================
        // 2. TOP SECTION (SEARCH BAR & USER ADMIN)
        // ==========================================
        HBox topArea = new HBox();
        topArea.setAlignment(Pos.CENTER);
        topArea.setPadding(new Insets(0, 0, 15, 0));

        // Search Bar styling (Bigger, Rounded pill shape)
        TextField searchField = new TextField();
        searchField.setPromptText("Search Action or User...");
        searchField.setPrefWidth(400); 
        searchField.setPrefHeight(45); 
        searchField.setStyle("-fx-background-radius: 25; -fx-border-radius: 25; -fx-border-color: #333333; -fx-border-width: 1.5; -fx-background-color: transparent; -fx-padding: 0 15 0 40; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px;");
        
        // Custom StackPane to overlay a search icon inside the TextField
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px;");
        StackPane searchContainer = new StackPane(searchField, searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        StackPane.setMargin(searchIcon, new Insets(0, 0, 0, 15)); // Push icon slightly right
        
        // Spacer to push User Admin to the far right
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        
        
        

        // ==========================================
        // 3. HEADER SECTION (TITLE)
        // ==========================================
        HBox headerArea = new HBox();
        headerArea.setAlignment(Pos.BOTTOM_LEFT); 
        
        // Title text
        Text title = new Text("SYSTEM AUDIT LOG");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 42));
        
        // Gradient from #004aad to #cb6ce6
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);
        
        // Assemble Header (No edit button needed here)
        headerArea.getChildren().add(title);

        // ==========================================
        // 4. TABLE VIEW SETUP
        // ==========================================
        setupTable();

        // Wrap table in a VBox to apply the rounded white card CSS
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("table-container"); // Links to CSS file
        
        // --- FIXED PADDING AND V-GROW ---
        tableCard.setPadding(new Insets(15, 20, 20, 20)); // Gives breathing room inside the white card
        VBox.setVgrow(table, Priority.ALWAYS); // Table stretches vertically inside card
        VBox.setVgrow(tableCard, Priority.ALWAYS); // Card stretches vertically inside main layout

        // Apply external CSS
        try {
            String cssPath = getClass().getResource("/ui/panels/table-style.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("WARNING: Could not load table-style.css. Ensure it is in the correct folder.");
        }

        // ==========================================
        // 5. ASSEMBLE PANEL
        // ==========================================
        getChildren().addAll(topArea, headerArea, tableCard);

        // BIND SEARCH FUNCTIONALITY
        setupSearchFilter(searchField);
        
        // Fetch real data from the database
        loadAuditLogs();
    }
  
    // --- DATABASE INTEGRATION ---
    private void loadAuditLogs() {
        masterData.clear(); 

        String query = "SELECT Timestamp, User, Action, Details FROM AuditLogs ORDER BY Timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Could not connect to the database to load audit logs.");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String timestamp = rs.getString("Timestamp");
                    String user = rs.getString("User");
                    String action = rs.getString("Action");
                    String details = rs.getString("Details");

                    // Handle nulls defensively
                    timestamp = (timestamp != null) ? timestamp : "";
                    user = (user != null) ? user : "";
                    action = (action != null) ? action : "";
                    details = (details != null) ? details : "";

                    masterData.add(new AuditLog(timestamp, user, action, details));
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL ERROR: Failed to load audit logs from the database.");
            e.printStackTrace();
        }
    }
    
    // --- TABLE COLUMNS SETUP ---
    private void setupTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: transparent; -fx-padding: 0;"); // Remove default borders

        TableColumn<AuditLog, String> colTime = new TableColumn<>("Date & Time");
        colTime.setCellValueFactory(data -> data.getValue().timestampProperty());
        colTime.setPrefWidth(150);
        colTime.setMaxWidth(160);

        TableColumn<AuditLog, String> colUser = new TableColumn<>("User");
        colUser.setCellValueFactory(data -> data.getValue().userProperty());
        colUser.setPrefWidth(90);
        colUser.setMaxWidth(110);

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(data -> data.getValue().actionProperty());
        colAction.setPrefWidth(150);
        colAction.setMaxWidth(180);

        TableColumn<AuditLog, String> colDetails = new TableColumn<>("Details");
        colDetails.setCellValueFactory(data -> data.getValue().detailsProperty());
        colDetails.setPrefWidth(300); // Expanding column

        table.getColumns().addAll(colTime, colUser, colAction, colDetails);
    }

    // --- SEARCH FILTER LOGIC ---
    private void setupSearchFilter(TextField searchField) {
        masterData = FXCollections.observableArrayList();
        
        FilteredList<AuditLog> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(log -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (log.getUser().toLowerCase().contains(lowerCaseFilter)) {
                    return true; 
                } else if (log.getAction().toLowerCase().contains(lowerCaseFilter)) {
                    return true; 
                } else if (log.getDetails().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                return false; 
            });
        });

        SortedList<AuditLog> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());

        table.setItems(sortedData);
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

    // --- DATA MODEL ---
    public static class AuditLog {
        private final StringProperty timestamp;
        private final StringProperty user;
        private final StringProperty action;
        private final StringProperty details;

        public AuditLog(String timestamp, String user, String action, String details) {
            this.timestamp = new SimpleStringProperty(timestamp);
            this.user = new SimpleStringProperty(user);
            this.action = new SimpleStringProperty(action);
            this.details = new SimpleStringProperty(details);
        }

        public StringProperty timestampProperty() { return timestamp; }
        public StringProperty userProperty() { return user; }
        public StringProperty actionProperty() { return action; }
        public StringProperty detailsProperty() { return details; }
        
        public String getTimestamp() { return timestamp.get(); }
        public String getUser() { return user.get(); }
        public String getAction() { return action.get(); }
        public String getDetails() { return details.get(); }
    }
}