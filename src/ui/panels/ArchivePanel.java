package ui.panels;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArchivePanel extends VBox {

    private TableView<ui.panels.RecordsPanel.ViolationRecord> table;
    private ObservableList<ui.panels.RecordsPanel.ViolationRecord> masterData;
    private String currentUsername;

    public ArchivePanel(String username) {
        this.currentUsername = username;

        // 1. MAIN LAYOUT SETUP
        setSpacing(20);
        setPadding(new Insets(10, 0, 0, 0));
        setAlignment(Pos.TOP_CENTER);

        // 2. TOP SECTION (SEARCH BAR)
        HBox topArea = new HBox();
        topArea.setAlignment(Pos.CENTER);
        topArea.setPadding(new Insets(0, 0, 15, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("Search archived records...");
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
        topArea.getChildren().addAll(searchContainer, topSpacer);

        // 3. HEADER SECTION (TITLE & ACTION BUTTONS)
        HBox headerArea = new HBox(15);
        headerArea.setAlignment(Pos.BOTTOM_LEFT);

        Text title = new Text("ARCHIVED RECORDS");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 42));

        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")), // Standard Blue!
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button btnRestore = new Button("RESTORE");
        btnRestore.setPrefHeight(30);
        btnRestore.setStyle("-fx-background-color: #004aad; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15;");
        btnRestore.setOnAction(e -> restoreSelectedRecord());

        Button btnPermanentDelete = new Button("PERMANENT DELETE");
        btnPermanentDelete.setPrefHeight(30);
        btnPermanentDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15;");
        btnPermanentDelete.setOnAction(e -> permanentDeleteRecord());

        headerArea.getChildren().addAll(title, headerSpacer, btnRestore, btnPermanentDelete);

        // 4. TABLE VIEW SETUP
        setupTable();
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("table-container");
        tableCard.setPadding(new Insets(15, 20, 20, 20));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        try {
            String cssPath = getClass().getResource("/ui/panels/table-style.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception e) {}

        getChildren().addAll(topArea, headerArea, tableCard);

        setupSearchFilter(searchField);
        loadArchivedData();
    }

    private void restoreSelectedRecord() {
        ui.panels.RecordsPanel.ViolationRecord record = table.getSelectionModel().getSelectedItem();
        if (record == null) {
            ui.components.CustomDialog.showMessage("No Selection", "Please select a record to restore.", true);
            return;
        }

        String restoreSql = "UPDATE Violations SET Status = 'Pending' WHERE ViolationID = ?";
        String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES (?, 'Restored Record', ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psRestore = conn.prepareStatement(restoreSql);
                 PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                psRestore.setInt(1, record.getId());
                psRestore.executeUpdate();

                psAudit.setString(1, currentUsername);
                psAudit.setString(2, "Restored archived violation for student " + record.getIdNo());
                psAudit.executeUpdate();

                conn.commit();
                ui.components.CustomDialog.showMessage("Success", "Record restored successfully to Pending status.", false);
                loadArchivedData();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            ui.components.CustomDialog.showMessage("Database Error", "Failed to restore record: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void permanentDeleteRecord() {
        ui.panels.RecordsPanel.ViolationRecord record = table.getSelectionModel().getSelectedItem();
        if (record == null) {
            ui.components.CustomDialog.showMessage("No Selection", "Please select a record to permanently delete.", true);
            return;
        }

        boolean confirm = ui.components.CustomDialog.showConfirmation("CRITICAL WARNING", 
            "This action cannot be undone. Are you sure you want to permanently delete this record from the database?");

        if (confirm) {
            String deleteSql = "DELETE FROM Violations WHERE ViolationID = ?";
            String auditSql = "INSERT INTO AuditLogs (User, Action, Details) VALUES (?, 'Permanent Delete', ?)";

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement psDelete = conn.prepareStatement(deleteSql);
                     PreparedStatement psAudit = conn.prepareStatement(auditSql)) {

                    psDelete.setInt(1, record.getId());
                    psDelete.executeUpdate();

                    psAudit.setString(1, currentUsername);
                    psAudit.setString(2, "Permanently deleted violation record for student " + record.getIdNo());
                    psAudit.executeUpdate();

                    conn.commit();
                    ui.components.CustomDialog.showMessage("Success", "Record permanently deleted.", false);
                    loadArchivedData();
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException e) {
                ui.components.CustomDialog.showMessage("Database Error", "Failed to delete record: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    private void loadArchivedData() {
        if (masterData == null) masterData = FXCollections.observableArrayList();
        masterData.clear();

        // ONLY fetch 'Archived' records
        String query = "SELECT ViolationID, Date, StudentName, StudentID, Course, Violation, Type, Sanction, Status FROM Violations WHERE Status = 'Archived' ORDER BY Date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                masterData.add(new ui.panels.RecordsPanel.ViolationRecord(
                        rs.getInt("ViolationID"),
                        rs.getString("Date"),
                        rs.getString("StudentName"),
                        rs.getString("StudentID"),
                        rs.getString("Course"),
                        rs.getString("Violation"),
                        rs.getString("Type"),
                        rs.getString("Sanction"),
                        rs.getString("Status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        TableColumn<ui.panels.RecordsPanel.ViolationRecord, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> data.getValue().dateProperty());

        TableColumn<ui.panels.RecordsPanel.ViolationRecord, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<ui.panels.RecordsPanel.ViolationRecord, String> colId = new TableColumn<>("ID No.");
        colId.setCellValueFactory(data -> data.getValue().idNoProperty());

        TableColumn<ui.panels.RecordsPanel.ViolationRecord, String> colViolation = new TableColumn<>("Violation");
        colViolation.setCellValueFactory(data -> data.getValue().violationProperty());

        table.getColumns().addAll(colDate, colName, colId, colViolation);
    }

    private void setupSearchFilter(TextField searchField) {
        if (masterData == null) masterData = FXCollections.observableArrayList();
        FilteredList<ui.panels.RecordsPanel.ViolationRecord> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return record.getName().toLowerCase().contains(lowerCaseFilter) || 
                       record.getIdNo().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<ui.panels.RecordsPanel.ViolationRecord> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }
}