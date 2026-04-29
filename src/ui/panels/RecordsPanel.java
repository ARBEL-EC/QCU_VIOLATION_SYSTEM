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
    private String currentUserRole; 
    private String currentUsername; // --- NEW: Stores the actual username for Audit Logs ---

    // --- UPDATED CONSTRUCTOR: Now accepts the username to fix the MainFrame error ---
    public RecordsPanel(String role, String username) {
        this.currentUserRole = role;
        this.currentUsername = username;

        // 1. MAIN LAYOUT SETUP
        setSpacing(20);
        setPadding(new Insets(10, 0, 0, 0));
        setAlignment(Pos.TOP_CENTER);

        // ==========================================
        // 2. TOP SECTION (SEARCH BAR)
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

        topArea.getChildren().addAll(searchContainer, topSpacer); // Add search bar to UI

        // ==========================================
        // 3. HEADER SECTION (TITLE & ACTION BUTTONS)
        // ==========================================
        HBox headerArea = new HBox(15);
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
        
        Button btnView = new Button("VIEW DETAILS");
        btnView.setPrefHeight(30);
        btnView.setStyle("-fx-background-color: #004aad; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15;");
        
        btnView.setOnAction(e -> {
            ViolationRecord selectedRecord = table.getSelectionModel().getSelectedItem();
            if (selectedRecord == null) {
                ui.components.CustomDialog.showMessage("No Selection", "Please select a record to view.", true);
            } else {
                openViewDetailsDialog(selectedRecord);
            }
        });

        Button btnEdit = new Button("EDIT");
        btnEdit.setPrefHeight(30);
        btnEdit.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15;");
        
        btnEdit.setOnAction(e -> {
            ViolationRecord selectedRecord = table.getSelectionModel().getSelectedItem();
            if (selectedRecord == null) {
                ui.components.CustomDialog.showMessage("No Selection", "Please select a record to edit.", true);
            } else {
                openEditDialog(selectedRecord);
            }
        });

        headerArea.getChildren().addAll(title, headerSpacer, btnView, btnEdit);

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

        getChildren().addAll(topArea, headerArea, tableCard);
        
        setupSearchFilter(searchField);
        loadDataFromDatabase();
    }

    // ==========================================
    // VIEW FULL DETAILS DIALOG
    // ==========================================
    private void openViewDetailsDialog(ViolationRecord record) {
        Stage popupStage = new Stage();
        // FIX: Change UNDECORATED to TRANSPARENT so the dark overlay works
        popupStage.initStyle(javafx.stage.StageStyle.TRANSPARENT); 
        popupStage.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #d1d5db; -fx-border-width: 2; -fx-border-radius: 15;");
        
        // FIX: Add these two lines to stop it from stretching to full screen
        layout.setMaxWidth(400); 
        layout.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Text title = new Text("Record Details");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        title.setFill(Color.web("#004aad"));

        GridPane grid = new GridPane();
        grid.setVgap(12);
        grid.setHgap(20);
        
        String styleLabel = "-fx-font-weight: bold; -fx-text-fill: #555555; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px;";
        String styleValue = "-fx-text-fill: #000000; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-size: 14px;";

        grid.addRow(0, createStyledLabel("Name:", styleLabel), createStyledLabel(record.getName(), styleValue));
        grid.addRow(1, createStyledLabel("Student ID:", styleLabel), createStyledLabel(record.getIdNo(), styleValue));
        grid.addRow(2, createStyledLabel("Course:", styleLabel), createStyledLabel(record.getCourse(), styleValue));
        grid.addRow(3, createStyledLabel("Date:", styleLabel), createStyledLabel(record.dateProperty().get(), styleValue));
        grid.addRow(4, createStyledLabel("Type:", styleLabel), createStyledLabel(record.typeProperty().get(), styleValue));
        grid.addRow(5, createStyledLabel("Sanction:", styleLabel), createStyledLabel(record.sanctionProperty().get(), styleValue));
        grid.addRow(6, createStyledLabel("Status:", styleLabel), createStyledLabel(record.getStatus(), styleValue));
        
        Label lblDesc = createStyledLabel("Description:", styleLabel);
        Label valDesc = createStyledLabel(record.getViolation(), styleValue);
        valDesc.setWrapText(true);
        valDesc.setMaxWidth(300);
        grid.addRow(7, lblDesc, valDesc);

        Button btnClose = new Button("Close");
        btnClose.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> popupStage.close());

        HBox btnBox = new HBox(btnClose);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        layout.getChildren().addAll(title, grid, btnBox);

        // Dark Overlay for modern popup look
        StackPane darkOverlay = new StackPane(layout);
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);"); 
        darkOverlay.setPadding(new Insets(20));

        Scene scene = new Scene(darkOverlay);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);
        
        javafx.stage.Window mainWindow = this.getScene().getWindow();
        if(mainWindow != null) {
            popupStage.initOwner(mainWindow);
            popupStage.setX(mainWindow.getX());
            popupStage.setY(mainWindow.getY());
            popupStage.setWidth(mainWindow.getWidth());
            popupStage.setHeight(mainWindow.getHeight());
        } else {
            popupStage.centerOnScreen();
        }

        popupStage.showAndWait();
    }
    
    private Label createStyledLabel(String text, String style) {
        Label lbl = new Label(text);
        lbl.setStyle(style);
        return lbl;
    }
  
    // ==========================================
    // EDIT DIALOG
    // ==========================================
    private void openEditDialog(ViolationRecord record) {
        Stage popupStage = new Stage();
        // FIX: Change UNDECORATED to TRANSPARENT here as well
        popupStage.initStyle(javafx.stage.StageStyle.TRANSPARENT); 
        popupStage.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));

        layout.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #d1d5db; -fx-border-radius: 15;");
        layout.setMaxWidth(400);
        layout.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Text title = new Text("Edit Sanction");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);

        Label lblStatus = new Label("Status:");
        lblStatus.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-text-fill: #555555;");
        ComboBox<String> cmbStatus = new ComboBox<>(FXCollections.observableArrayList("Pending", "Ongoing", "Resolved"));
        cmbStatus.setValue(record.getStatus()); 
        cmbStatus.setMaxWidth(Double.MAX_VALUE);
        cmbStatus.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #d1d5db; -fx-background-color: white;");

        Label lblViolation = new Label("Violation Description:");
        lblViolation.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-text-fill: #555555;");
        TextArea txtViolation = new TextArea(record.getViolation()); 
        txtViolation.setWrapText(true);
        txtViolation.setPrefRowCount(4);
        txtViolation.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #d1d5db; -fx-control-inner-background: white;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> popupStage.close());

        Button btnDelete = new Button("Delete Record");
        btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> deleteRecord(record, popupStage));

        Button btnSave = new Button("Save Changes");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #004aad, #cb6ce6); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnSave.setOnAction(e -> updateRecord(record, cmbStatus.getValue(), txtViolation.getText().trim(), popupStage));

        if ("Staff".equalsIgnoreCase(currentUserRole)) {
            btnDelete.setVisible(false); 
            txtViolation.setEditable(false); 
            txtViolation.setStyle("-fx-control-inner-background: #f3f4f6; -fx-text-fill: #9ca3af; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #d1d5db;");
        }

        buttonBox.getChildren().addAll(btnCancel, btnDelete, btnSave);
        layout.getChildren().addAll(title, lblStatus, cmbStatus, lblViolation, txtViolation, buttonBox);

        StackPane darkOverlay = new StackPane(layout);
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);"); 
        
        Scene scene = new Scene(darkOverlay);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);
        
        javafx.stage.Window mainWindow = this.getScene().getWindow();
        if(mainWindow != null) {
            popupStage.initOwner(mainWindow);
            popupStage.setX(mainWindow.getX());
            popupStage.setY(mainWindow.getY());
            popupStage.setWidth(mainWindow.getWidth());
            popupStage.setHeight(mainWindow.getHeight());
        } else {
            popupStage.centerOnScreen();
        }
        
        popupStage.showAndWait();
    }

    private void updateRecord(ViolationRecord record, String newStatus, String newViolation, Stage popupStage) {
        String updateSql = "UPDATE Violations SET Status = ?, Violation = ? WHERE ViolationID = ?";
        String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES (?, 'Edited Record', ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                psUpdate.setString(1, newStatus);
                psUpdate.setString(2, newViolation);
                psUpdate.setInt(3, record.getId()); 
                psUpdate.executeUpdate();

                String details = "Edited record for student " + record.getIdNo() + " (Status -> " + newStatus + ")";
                
                // --- FIX: Use real username in Audit Log ---
                psAudit.setString(1, currentUsername);
                psAudit.setString(2, details);
                psAudit.executeUpdate();

                conn.commit();
                
                ui.components.CustomDialog.showMessage("Success", "Record updated successfully.", false);
                popupStage.close();
                loadDataFromDatabase(); 

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            ui.components.CustomDialog.showMessage("Database Error", "Failed to update record: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void deleteRecord(ViolationRecord record, Stage popupStage) {
        boolean confirm = ui.components.CustomDialog.showConfirmation("Confirm Delete", "Are you sure you want to delete this violation record?");

        if (confirm) {
            // --- FIX: SOFT DELETE (ARCHIVE) INSTEAD OF HARD DELETE ---
            String archiveSql = "UPDATE Violations SET Status = 'Archived' WHERE ViolationID = ?";
            String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES (?, 'Archived Record', ?)";

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false); 

                try (PreparedStatement psArchive = conn.prepareStatement(archiveSql);
                     PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                    psArchive.setInt(1, record.getId());
                    psArchive.executeUpdate();

                    // --- FIX: Use real username in Audit Log ---
                    psAudit.setString(1, currentUsername);
                    psAudit.setString(2, "Archived violation record for student " + record.getIdNo());
                    psAudit.executeUpdate();

                    conn.commit();

                    ui.components.CustomDialog.showMessage("Success", "Record archived successfully.", false);
                    popupStage.close();
                    loadDataFromDatabase(); 

                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                ui.components.CustomDialog.showMessage("Database Error", "Failed to archive record: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    // --- DATABASE INTEGRATION ---
    private void loadDataFromDatabase() {
        if (masterData == null) {
            masterData = FXCollections.observableArrayList();
        }
        masterData.clear(); 

        // --- FIX: Filter out Archived records so they don't show up in the table ---
        String query = "SELECT ViolationID, Date, StudentName, StudentID, Course, Violation, Type, Sanction, Status FROM Violations WHERE Status != 'Archived' ORDER BY Date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ViolationID"); 
                String date = rs.getString("Date");
                String name = rs.getString("StudentName");
                String idNo = rs.getString("StudentID");
                String course = rs.getString("Course");
                String violation = rs.getString("Violation");
                String type = rs.getString("Type");
                String sanction = rs.getString("Sanction");
                String status = rs.getString("Status");

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

    // --- DATA MODEL ---
    public static class ViolationRecord {
        private final int id; 
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
        public String getCourse() { return course.get(); }
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