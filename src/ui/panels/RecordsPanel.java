package ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import db.DatabaseConnection;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RecordsPanel extends VBox {

    private TableView<ViolationRecord> table;
    private ObservableList<ViolationRecord> masterData;

    public RecordsPanel() {
        // 1. MAIN LAYOUT SETUP
        setSpacing(20);
        setPadding(new Insets(10, 0, 0, 0));
        setAlignment(Pos.TOP_CENTER);

        // ==========================================
        // 2. TOP SECTION (SEARCH BAR & USER ADMIN)
        // ==========================================
        HBox topArea = new HBox();
        topArea.setAlignment(Pos.CENTER);
        topArea.setPadding(new Insets(0, 0, 15, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by student no. or name");
        searchField.setPrefWidth(400);
        searchField.setPrefHeight(45);
        searchField.setStyle("-fx-background-radius: 25; -fx-border-radius: 25; -fx-border-color: #333333; -fx-border-width: 1.5; -fx-background-color: transparent; -fx-padding: 0 15 0 40; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px;");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px;");
        StackPane searchContainer = new StackPane(searchField, searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        StackPane.setMargin(searchIcon, new Insets(0, 0, 0, 15));
        
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        
        HBox userAdminArea = new HBox(10);
        userAdminArea.setAlignment(Pos.CENTER_RIGHT);
        Label userLabel = new Label("USER ADMIN");
        userLabel.setStyle("-fx-text-fill: #777777; -fx-font-size: 14px; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif;");
        ImageView userIcon = loadImageView("/Icons/admin.png", 35, 35);
        
        userAdminArea.getChildren().addAll(userIcon, userLabel);
        topArea.getChildren().addAll(searchContainer, topSpacer, userAdminArea);

        // ==========================================
        // 3. HEADER SECTION (TITLE & EDIT BUTTON)
        // ==========================================
        HBox headerArea = new HBox();
        headerArea.setAlignment(Pos.BOTTOM_LEFT);
        
        Text title = new Text("STUDENT RECORDS");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 42));
        
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        // --- 🚀 NEW EDIT BUTTON LOGIC ---
        Button btnEdit = new Button("EDIT");
        btnEdit.setPrefWidth(80);
        btnEdit.setPrefHeight(30);
        btnEdit.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-cursor: hand;");
        
        btnEdit.setOnAction(e -> {
            ViolationRecord selectedRecord = table.getSelectionModel().getSelectedItem();
            if (selectedRecord == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a record first.");
            } else {
                openEditDialog(selectedRecord);
            }
        });
        // ---------------------------------

        headerArea.getChildren().addAll(title, headerSpacer, btnEdit);

        // ==========================================
        // 4. TABLE VIEW SETUP
        // ==========================================
        setupTable();

        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("table-container");
        tableCard.setPadding(new Insets(15, 20, 20, 20)); 
        VBox.setVgrow(table, Priority.ALWAYS); 
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        try {
            String cssPath = getClass().getResource("/ui/panels/table-style.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("WARNING: Could not load table-style.css.");
        }

        // ==========================================
        // 5. ASSEMBLE PANEL
        // ==========================================
        getChildren().addAll(topArea, headerArea, tableCard);
        setupSearchFilter(searchField);
        loadDataFromDatabase();
    }
  
    // ==========================================
    // 🚀 NEW: POPUP DIALOG LOGIC
    // ==========================================
    private void openEditDialog(ViolationRecord record) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Edit Record");

        // Clean white rounded container
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #d1d5db; -fx-border-radius: 15;");

        // Gradient Title
        Text title = new Text("Edit Sanction");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);

        // 1. Status Dropdown
        Label lblStatus = new Label("Status:");
        lblStatus.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-text-fill: #555555;");
        ComboBox<String> cmbStatus = new ComboBox<>(FXCollections.observableArrayList("Pending", "Ongoing", "Resolved"));
        cmbStatus.setValue(record.getStatus()); // Pre-select current status
        cmbStatus.setMaxWidth(Double.MAX_VALUE);
        cmbStatus.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #d1d5db; -fx-background-color: white;");

        // 2. Violation Description TextArea
        Label lblViolation = new Label("Violation Description:");
        lblViolation.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-text-fill: #555555;");
        TextArea txtViolation = new TextArea(record.getViolation()); // Pre-fill description
        txtViolation.setWrapText(true);
        txtViolation.setPrefRowCount(4);
        txtViolation.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #d1d5db; -fx-control-inner-background: white;");

        // Buttons Box
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDelete = new Button("Delete Record");
        btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> deleteRecord(record, popupStage));

        Button btnSave = new Button("Save Changes");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #004aad, #cb6ce6); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnSave.setOnAction(e -> updateRecord(record, cmbStatus.getValue(), txtViolation.getText().trim(), popupStage));

        buttonBox.getChildren().addAll(btnDelete, btnSave);

        // Add everything to layout
        layout.getChildren().addAll(title, lblStatus, cmbStatus, lblViolation, txtViolation, buttonBox);

        Scene scene = new Scene(layout, 400, 350);
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }

    private void updateRecord(ViolationRecord record, String newStatus, String newViolation, Stage popupStage) {
        String updateSql = "UPDATE Violations SET Status = ?, Violation = ? WHERE ViolationID = ?";
        String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES ('ADMIN', 'Edited Record', ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Enable transaction

            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                // Update row
                psUpdate.setString(1, newStatus);
                psUpdate.setString(2, newViolation);
                psUpdate.setInt(3, record.getId()); // Using strictly the DB ID
                psUpdate.executeUpdate();

                // Log audit
                String details = "Edited record for student " + record.getIdNo() + " (Status -> " + newStatus + ")";
                psAudit.setString(1, details);
                psAudit.executeUpdate();

                conn.commit();
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Record updated successfully.");
                popupStage.close();
                loadDataFromDatabase(); // Refresh table

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteRecord(ViolationRecord record, Stage popupStage) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this violation record?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            String deleteSql = "DELETE FROM Violations WHERE ViolationID = ?";
            String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES ('ADMIN', 'Deleted Record', ?)";

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false); // Enable transaction

                try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                     PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                    psDelete.setInt(1, record.getId());
                    psDelete.executeUpdate();

                    psAudit.setString(1, "Deleted violation record for student " + record.getIdNo());
                    psAudit.executeUpdate();

                    conn.commit();

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Record deleted successfully.");
                    popupStage.close();
                    loadDataFromDatabase(); // Refresh table

                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete record: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- DATABASE INTEGRATION ---
    private void loadDataFromDatabase() {
        if (masterData == null) {
            masterData = FXCollections.observableArrayList();
        }
        masterData.clear(); 

        // Query UPDATED: Now fetches ViolationID as well
        String query = "SELECT ViolationID, Date, StudentName, StudentID, Course, Violation, Type, Sanction, Status FROM Violations ORDER BY Date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ViolationID"); // Crucial for edits/deletes
                String date = rs.getString("Date");
                String name = rs.getString("StudentName");
                String idNo = rs.getString("StudentID");
                String course = rs.getString("Course");
                String violation = rs.getString("Violation");
                String type = rs.getString("Type");
                String sanction = rs.getString("Sanction");
                String status = rs.getString("Status");

                // Null safety
                date = (date != null) ? date : "";
                name = (name != null) ? name : "";
                idNo = (idNo != null) ? idNo : "";
                course = (course != null) ? course : "";
                violation = (violation != null) ? violation : "";
                type = (type != null) ? type : "";
                sanction = (sanction != null) ? sanction : "";
                status = (status != null) ? status : "Pending";

                masterData.add(new ViolationRecord(id, date, name, idNo, course, violation, type, sanction, status));
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL ERROR: Failed to load records from the database.");
            e.printStackTrace();
        }
    }
    
    // --- FIXED TABLE COLUMNS ---
    private void setupTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: transparent; -fx-padding: 0;"); 

        TableColumn<ViolationRecord, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> data.getValue().dateProperty());
        colDate.setPrefWidth(90);
        colDate.setMaxWidth(110);

        TableColumn<ViolationRecord, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colName.setPrefWidth(160); 

        TableColumn<ViolationRecord, String> colId = new TableColumn<>("ID No.");
        colId.setCellValueFactory(data -> data.getValue().idNoProperty());
        colId.setPrefWidth(100);
        colId.setMaxWidth(120);

        TableColumn<ViolationRecord, String> colCourse = new TableColumn<>("Course");
        colCourse.setCellValueFactory(data -> data.getValue().courseProperty());
        colCourse.setPrefWidth(70);
        colCourse.setMaxWidth(90);

        TableColumn<ViolationRecord, String> colViolation = new TableColumn<>("Violation");
        colViolation.setCellValueFactory(data -> data.getValue().violationProperty());
        colViolation.setPrefWidth(200); 

        TableColumn<ViolationRecord, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colType.setPrefWidth(80);
        colType.setMaxWidth(100);

        TableColumn<ViolationRecord, String> colSanction = new TableColumn<>("Sanction");
        colSanction.setCellValueFactory(data -> data.getValue().sanctionProperty());
        colSanction.setPrefWidth(140);

        TableColumn<ViolationRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colStatus.setPrefWidth(90);
        colStatus.setMaxWidth(110);

        table.getColumns().addAll(colDate, colName, colId, colCourse, colViolation, colType, colSanction, colStatus);
    }

    // --- SEARCH FILTER LOGIC ---
    private void setupSearchFilter(TextField searchField) {
        if (masterData == null) {
            masterData = FXCollections.observableArrayList();
        }
        
        FilteredList<ViolationRecord> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (record.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true; 
                } else if (record.getIdNo().toLowerCase().contains(lowerCaseFilter)) {
                    return true; 
                }
                return false; 
            });
        });

        SortedList<ViolationRecord> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    // --- HELPER: Safe Image Loading ---
    private ImageView loadImageView(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                iv.setImage(new Image(is));
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

    // --- DATA MODEL (UPDATED) ---
    public static class ViolationRecord {
        private final int id; // Added DB ID for updating/deleting
        private final javafx.beans.property.SimpleStringProperty date;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty idNo;
        private final javafx.beans.property.SimpleStringProperty course;
        private final javafx.beans.property.SimpleStringProperty violation;
        private final javafx.beans.property.SimpleStringProperty type;
        private final javafx.beans.property.SimpleStringProperty sanction;
        private final javafx.beans.property.SimpleStringProperty status;

        public ViolationRecord(int id, String date, String name, String idNo, String course, String violation, String type, String sanction, String status) {
            this.id = id;
            this.date = new javafx.beans.property.SimpleStringProperty(date);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.idNo = new javafx.beans.property.SimpleStringProperty(idNo);
            this.course = new javafx.beans.property.SimpleStringProperty(course);
            this.violation = new javafx.beans.property.SimpleStringProperty(violation);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.sanction = new javafx.beans.property.SimpleStringProperty(sanction);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public String getIdNo() { return idNo.get(); }
        public String getStatus() { return status.get(); }
        public String getViolation() { return violation.get(); }

        public javafx.beans.property.StringProperty dateProperty() { return date; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty idNoProperty() { return idNo; }
        public javafx.beans.property.StringProperty courseProperty() { return course; }
        public javafx.beans.property.StringProperty violationProperty() { return violation; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        public javafx.beans.property.StringProperty sanctionProperty() { return sanction; }
        public javafx.beans.property.StringProperty statusProperty() { return status; }
    }
}