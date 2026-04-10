package ui.panels;

import db.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LogViolationPanel extends BorderPane {

    // Form Components
    private ComboBox<StudentItem> cmbStudent;
    private ComboBox<String> cmbCategory;
    private DatePicker datePicker;
    private ComboBox<String> cmbSanction;
    private TextArea txtDescription;

    // Data lists
    private ObservableList<StudentItem> allStudents = FXCollections.observableArrayList();
    private ObservableList<StudentItem> filteredStudents = FXCollections.observableArrayList();

    // Fonts & Colors
    private final String FONT_FAMILY = "'ITC Avant Garde Gothic', 'Segoe UI', sans-serif";
    private final String LABEL_COLOR = "#6d6a6a";
    private final String BORDER_STYLE = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-padding: 2;";

    public LogViolationPanel() {
        setPadding(new Insets(30, 40, 40, 40));
        
        // Build Layout
        setTop(createHeader());
        
        StackPane centerWrapper = new StackPane(createIncidentCard());
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        centerWrapper.setPadding(new Insets(10, 0, 0, 0));
        setCenter(centerWrapper);

        // Load Initial Data
        loadStudentsFromDatabase();
    }

  // ==========================================
    // 1. TOP HEADER SECTION
    // ==========================================
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER); // Changed from CENTER_LEFT to CENTER to match Dashboard
        header.setPadding(new Insets(0, 0, 20, 0)); // Changed bottom padding from 25 to 20

        // Header Title
        Label lblTitle = new Label("LOG VIOLATION");
        lblTitle.setStyle("-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-size: 60px; " // Increased from 48px to 60px to exactly match Dashboard
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #004aad, #cb6ce6);");

        // Spacer pushes title left and profile right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User Admin Profile
        HBox userAdminBox = new HBox(10); // Kept at 10px spacing
        userAdminBox.setAlignment(Pos.CENTER_RIGHT);

        // Custom Admin Avatar
        ImageView userIcon = new ImageView();
        try {
            // Updated to capital "/Icons/" to prevent the .jar missing image issue
            String imgPath = getClass().getResource("/Icons/admin.png").toExternalForm();
            userIcon.setImage(new Image(imgPath));
            userIcon.setFitWidth(35);  // Increased from 32 to 35 to match Dashboard
            userIcon.setFitHeight(35); // Increased from 32 to 35 to match Dashboard
            userIcon.setPreserveRatio(true);
            userIcon.setSmooth(true);
        } catch (NullPointerException e) {
            System.err.println("Warning: Icon not found at /Icons/admin.png");
        }

        Label lblUser = new Label("USER ADMIN");
        lblUser.setStyle("-fx-text-fill: #777777; -fx-font-size: 14px; -fx-font-family: " + FONT_FAMILY + ";"); // Matched 14px and removed extra bolding

        Label lblArrow = new Label("v"); // Changed from "˅" to "v" to match Dashboard
        lblArrow.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;"); // Matched 12px size

        userAdminBox.getChildren().addAll(userIcon, lblUser, lblArrow);

        header.getChildren().addAll(lblTitle, spacer, userAdminBox);
        return header;
    }

    // ==========================================
    // 2. INCIDENT CARD CONTAINER
    // ==========================================
    private VBox createIncidentCard() {
        VBox card = new VBox(20);
        card.setMaxWidth(650);
        card.setStyle("-fx-background-color: #ffffff; "
                + "-fx-background-radius: 20; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        card.setPadding(new Insets(30));

        // Card Title
        Label lblIncident = new Label("INCIDENT DETAILS");
        lblIncident.setStyle("-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-size: 16px; "
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #004aad, #cb6ce6);");
        lblIncident.setPadding(new Insets(0, 0, 10, 0));

        // Form Fields
        VBox formFields = new VBox(15);
        
        // Row 1: Student Search
        formFields.getChildren().add(createStudentSearchBox());

        // Row 2: Category and Date
        HBox row2 = new HBox(20);
        
        VBox boxCategory = new VBox(5);
        Label lblCategory = createFormLabel("Type"); // Changed to match DB column "Type"
        cmbCategory = new ComboBox<>(FXCollections.observableArrayList("MINOR", "MAJOR"));
        cmbCategory.setPromptText("MINOR");
        cmbCategory.setMaxWidth(Double.MAX_VALUE);
        cmbCategory.setStyle(BORDER_STYLE);
        HBox.setHgrow(boxCategory, Priority.ALWAYS);
        boxCategory.getChildren().addAll(lblCategory, cmbCategory);

        VBox boxDate = new VBox(5);
        Label lblDate = createFormLabel("Date");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle(BORDER_STYLE);
        formatDatePicker(datePicker); // Apply MM/dd/yyyy visual format
        HBox.setHgrow(boxDate, Priority.ALWAYS);
        boxDate.getChildren().addAll(lblDate, datePicker);

        row2.getChildren().addAll(boxCategory, boxDate);
        formFields.getChildren().add(row2);

        // Row 3: Sanction
        VBox boxSanction = new VBox(5);
        Label lblSanction = createFormLabel("Sanction:");
        cmbSanction = new ComboBox<>(FXCollections.observableArrayList(
                "Warning", "Community Service", "Suspension", "Drop", "Call Parent"
        ));
        cmbSanction.setPromptText("Warning");
        cmbSanction.setMaxWidth(Double.MAX_VALUE);
        cmbSanction.setStyle(BORDER_STYLE);
        boxSanction.getChildren().addAll(lblSanction, cmbSanction);
        formFields.getChildren().add(boxSanction);

        // Row 4: Violation Description
        VBox boxViolation = new VBox(5);
        Label lblViolation = createFormLabel("Violation"); // Matches DB column
        txtDescription = new TextArea();
        txtDescription.setPromptText("Describe the incident....");
        txtDescription.setPrefRowCount(4);
        txtDescription.setWrapText(true);
        txtDescription.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-control-inner-background: white;");
        boxViolation.getChildren().addAll(lblViolation, txtDescription);
        formFields.getChildren().add(boxViolation);

        // Add everything to card
        card.getChildren().addAll(lblIncident, formFields, createButtons());
        return card;
    }

    // ==========================================
    // 3. SEARCHABLE STUDENT DROPDOWN
    // ==========================================
    private VBox createStudentSearchBox() {
        VBox box = new VBox(5);
        Label lblStudent = createFormLabel("Student");

        cmbStudent = new ComboBox<>(filteredStudents);
        cmbStudent.setEditable(true);
        cmbStudent.setPromptText("Search by student name or ID...");
        cmbStudent.setMaxWidth(Double.MAX_VALUE);
        cmbStudent.setStyle(BORDER_STYLE);
        
        // Handle Object to String display
        cmbStudent.setConverter(new StringConverter<StudentItem>() {
            @Override
            public String toString(StudentItem object) {
                return object == null ? "" : object.toString();
            }
            @Override
            public StudentItem fromString(String string) {
                return cmbStudent.getItems().stream().filter(item -> 
                    item.toString().equals(string)).findFirst().orElse(null);
            }
        });

        // Search/Filter logic
        cmbStudent.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                filteredStudents.setAll(allStudents);
            } else {
                StudentItem selected = cmbStudent.getSelectionModel().getSelectedItem();
                if (selected != null && selected.toString().equals(newText)) return;

                ObservableList<StudentItem> temp = FXCollections.observableArrayList();
                for (StudentItem student : allStudents) {
                    if (student.toString().toLowerCase().contains(newText.toLowerCase())) {
                        temp.add(student);
                    }
                }
                filteredStudents.setAll(temp);
                cmbStudent.show(); // Auto-drop matching results
            }
        });

        box.getChildren().addAll(lblStudent, cmbStudent);
        return box;
    }

    // ==========================================
    // 4. BOTTOM ACTION BUTTONS
    // ==========================================
    private HBox createButtons() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button btnSave = new Button("Save Record");
        btnSave.setStyle("-fx-background-color: #ef4444; "
                + "-fx-text-fill: white; "
                + "-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 20; "
                + "-fx-padding: 8 20 8 20; "
                + "-fx-cursor: hand;");
        btnSave.setOnAction(e -> saveRecord());

        Button btnClear = new Button("Clear Form");
        btnClear.setStyle("-fx-background-color: #e5e7eb; "
                + "-fx-text-fill: #6b7280; "
                + "-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-weight: bold; "
                + "-fx-background-radius: 20; "
                + "-fx-padding: 8 20 8 20; "
                + "-fx-cursor: hand;");
        btnClear.setOnAction(e -> clearForm());

        buttonBox.getChildren().addAll(btnSave, btnClear);
        return buttonBox;
    }

    // ==========================================
    // 5. HELPER METHODS & VALIDATION
    // ==========================================
    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: " + FONT_FAMILY + "; -fx-text-fill: " + LABEL_COLOR + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        return lbl;
    }

    private void formatDatePicker(DatePicker picker) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? formatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, formatter) : null;
            }
        });
    }

    private boolean validateFields() {
        if (cmbStudent.getValue() == null || cmbStudent.getEditor().getText().isEmpty()) return false;
        if (cmbCategory.getValue() == null) return false;
        if (datePicker.getValue() == null) return false;
        if (cmbSanction.getValue() == null) return false;
        if (txtDescription.getText().trim().isEmpty()) return false;
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==========================================
    // 6. DATABASE LOGIC (Updated to match schema)
    // ==========================================
    private void loadStudentsFromDatabase() {
        allStudents.clear();
        String sql = "SELECT StudentID, Name, Course FROM Students;"; // Matched with your DB schema
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                StudentItem student = new StudentItem(
                        rs.getString("StudentID"),
                        rs.getString("Name"),
                        rs.getString("Course")
                );
                allStudents.add(student);
            }
            filteredStudents.setAll(allStudents);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not load students. Ensure 'Students' table exists with columns StudentID, Name, Course.");
            e.printStackTrace();
        }
    }

// ==========================================
    // 6. DATABASE LOGIC (Updated to include Audit Logs)
    // ==========================================
    private void saveRecord() {
        if (!validateFields()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill up all required fields.");
            return;
        }

        StudentItem selectedStudent = cmbStudent.getValue();
        
        // Query 1: Save the Violation
        String sqlViolation = "INSERT INTO Violations (Date, StudentID, StudentName, Course, Location, Violation, Type, Sanction, Status) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pending');";
                              
        // Query 2: Save the Audit Log
        String sqlAudit = "INSERT INTO AuditLogs (User, Action, Details) VALUES ('ADMIN', 'Logged Violation', ?);";

        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // Turn off auto-commit to ensure BOTH queries run successfully together
            conn.setAutoCommit(false);

            try (PreparedStatement psVio = conn.prepareStatement(sqlViolation);
                 PreparedStatement psAudit = conn.prepareStatement(sqlAudit)) {

                // 1. Execute Violation Insert
                psVio.setString(1, datePicker.getValue().toString()); 
                psVio.setString(2, selectedStudent.getId());
                psVio.setString(3, selectedStudent.getName());
                psVio.setString(4, selectedStudent.getCourse());
                psVio.setString(5, ""); // Location (Empty since we removed it from UI)
                psVio.setString(6, txtDescription.getText().trim()); 
                psVio.setString(7, cmbCategory.getValue()); 
                psVio.setString(8, cmbSanction.getValue());
                psVio.executeUpdate();

                // 2. Execute Audit Log Insert
                // Example format: "Logged MINOR violation for 25-0010"
                String auditDetails = "Logged " + cmbCategory.getValue() + " violation for " + selectedStudent.getId();
                psAudit.setString(1, auditDetails);
                psAudit.executeUpdate();

                // 3. Commit the transaction (Save both permanently)
                conn.commit();
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Record and Audit Log saved successfully.");
                clearForm();

            } catch (Exception ex) {
                // If anything goes wrong, rollback so we don't get half-saved data
                conn.rollback(); 
                throw ex; // Pass to the outer catch block to show the error
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearForm() {
        cmbStudent.getSelectionModel().clearSelection();
        cmbStudent.getEditor().clear();
        cmbCategory.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now());
        cmbSanction.getSelectionModel().clearSelection();
        txtDescription.clear();
        filteredStudents.setAll(allStudents); // Reset dropdown list
    }

    // ==========================================
    // 7. INTERNAL DATA CLASS
    // ==========================================
    private static class StudentItem {
        private final String id;
        private final String name;
        private final String course;

        public StudentItem(String id, String name, String course) {
            this.id = id;
            this.name = name;
            this.course = course;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCourse() { return course; }

        @Override
        public String toString() {
            return id + " - " + name + " (" + course + ")";
        }
    }
}