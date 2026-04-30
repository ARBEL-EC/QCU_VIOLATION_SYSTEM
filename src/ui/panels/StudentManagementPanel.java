package ui.panels;

import db.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentManagementPanel extends BorderPane {

    private TableView<StudentModel> table;
    private ObservableList<StudentModel> studentList;

    public StudentManagementPanel() {
        setPadding(new Insets(30, 40, 40, 40));

        // 1. Header Area
        setTop(createHeader());

        // 2. Main Content Area (Search & Table)
        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 0, 0, 0));

        // Search Bar
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Student ID, Name, or Course...");
        searchField.setPrefWidth(400);
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #d1d5db; -fx-background-color: white; -fx-padding: 0 15 0 35; -fx-font-family: 'ITC Avant Garde Gothic', sans-serif;");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
        StackPane searchContainer = new StackPane(searchField, searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        StackPane.setMargin(searchIcon, new Insets(0, 0, 0, 12));
        
        HBox searchBox = new HBox(searchContainer);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Table Setup
        table = new TableView<>();
        table.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<StudentModel, String> colId = new TableColumn<>("Student ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(120);
        colId.setMaxWidth(150);

        TableColumn<StudentModel, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<StudentModel, String> colCourse = new TableColumn<>("Course");
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colCourse.setPrefWidth(150);
        colCourse.setMaxWidth(200);

        table.getColumns().addAll(colId, colName, colCourse);

        content.getChildren().addAll(searchBox, table);
        setCenter(content);

        studentList = FXCollections.observableArrayList();
        setupSearchFilter(searchField);
        loadStudents();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("STUDENT MANAGEMENT");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 42));

        LinearGradient titleGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#004aad")),
                new Stop(1, Color.web("#cb6ce6"))
        );
        title.setFill(titleGradient);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Student");
        btnAdd.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> openStudentDialog(null));

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> {
            StudentModel selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openStudentDialog(selected);
            else ui.components.CustomDialog.showMessage("No Selection", "Please select a student to edit.", true);
        });

        Button btnDelete = new Button("Delete");
        btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> deleteSelectedStudent());

        HBox btnBox = new HBox(10, btnAdd, btnEdit, btnDelete);
        header.getChildren().addAll(title, spacer, btnBox);

        return header;
    }

    private void loadStudents() {
        studentList.clear();
        String sql = "SELECT StudentID, Name, Course FROM Students";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                studentList.add(new StudentModel(rs.getString("StudentID"), rs.getString("Name"), rs.getString("Course")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter(TextField searchField) {
        FilteredList<StudentModel> filteredData = new FilteredList<>(studentList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(student -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filter = newValue.toLowerCase();
                return student.getName().toLowerCase().contains(filter)
                        || student.getId().toLowerCase().contains(filter)
                        || student.getCourse().toLowerCase().contains(filter);
            });
        });
        SortedList<StudentModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    private void deleteSelectedStudent() {
        StudentModel selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ui.components.CustomDialog.showMessage("No Selection", "Please select a student to delete.", true);
            return;
        }

        boolean confirm = ui.components.CustomDialog.showConfirmation("Confirm Delete", "Are you sure you want to delete " + selected.getName() + "?\n\n(Note: Their past violations will remain in the database for historical auditing).");
        if (confirm) {
            String sql = "DELETE FROM Students WHERE StudentID = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, selected.getId());
                ps.executeUpdate();
                loadStudents();
                ui.components.CustomDialog.showMessage("Success", "Student deleted successfully.", false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- CREATE / UPDATE DIALOG ---
    private void openStudentDialog(StudentModel student) {
        boolean isEdit = (student != null);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #d1d5db; -fx-border-width: 2; -fx-border-radius: 10;");
        layout.setPrefWidth(450);
        layout.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Text title = new Text(isEdit ? "Edit Student" : "Add New Student");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 20));
        title.setFill(Color.web("#004aad"));

        TextField txtId = new TextField();
        txtId.setPromptText("e.g. 26-0001");
        txtId.setStyle("-fx-pref-height: 35; -fx-background-radius: 5;");
        if (isEdit) {
            txtId.setText(student.getId());
            txtId.setEditable(false); // ID should not be changed once created to prevent data orphans
            txtId.setStyle("-fx-pref-height: 35; -fx-background-radius: 5; -fx-background-color: #f3f4f6; -fx-text-fill: #9ca3af;");
        }

        TextField txtName = new TextField();
        txtName.setPromptText("Last Name, First Name");
        txtName.setStyle("-fx-pref-height: 35; -fx-background-radius: 5;");
        if (isEdit) txtName.setText(student.getName());

        TextField txtCourse = new TextField();
        txtCourse.setPromptText("e.g. BSIT, BSCS, BSBA");
        txtCourse.setStyle("-fx-pref-height: 35; -fx-background-radius: 5;");
        if (isEdit) txtCourse.setText(student.getCourse());

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Save Student");
        btnSave.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");
        
        btnSave.setOnAction(e -> {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String course = txtCourse.getText().trim();

            if (id.isEmpty() || name.isEmpty() || course.isEmpty()) {
                ui.components.CustomDialog.showMessage("Error", "All fields are required.", true);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (isEdit) {
                    String sql = "UPDATE Students SET Name = ?, Course = ? WHERE StudentID = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, course);
                    ps.setString(3, id);
                    ps.executeUpdate();
                } else {
                    String sql = "INSERT INTO Students (StudentID, Name, Course) VALUES (?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, id);
                    ps.setString(2, name);
                    ps.setString(3, course);
                    ps.executeUpdate();
                }
                loadStudents();
                dialog.close();
                ui.components.CustomDialog.showMessage("Success", isEdit ? "Student updated successfully." : "Student added successfully.", false);
            } catch (Exception ex) {
                ui.components.CustomDialog.showMessage("Database Error", "Failed to save. That Student ID might already exist.", true);
            }
        });

        btnBox.getChildren().addAll(btnCancel, btnSave);
        layout.getChildren().addAll(title, 
            new Label("Student ID:"), txtId, 
            new Label("Full Name:"), txtName, 
            new Label("Course/Program:"), txtCourse, 
            btnBox);

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

    // --- DATA MODEL ---
    public static class StudentModel {
        private final String id;
        private final String name;
        private final String course;

        public StudentModel(String id, String name, String course) {
            this.id = id;
            this.name = name;
            this.course = course;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCourse() { return course; }
    }
}