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
    private ComboBox<String> cmbSpecificViolation; // NEW: Predefined Dropdown
    private TextArea txtDescription; // Existing: Now used for custom/Other input

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
        header.setAlignment(Pos.CENTER); 
        header.setPadding(new Insets(0, 0, 20, 0)); 

        // Header Title
        Label lblTitle = new Label("LOG VIOLATION");
        lblTitle.setStyle("-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-size: 60px; " 
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #004aad, #cb6ce6);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User Admin Profile
        HBox userAdminBox = new HBox(10); 
        userAdminBox.setAlignment(Pos.CENTER_RIGHT);

        ImageView userIcon = new ImageView();
        try {
            String imgPath = getClass().getResource("/Icons/admin.png").toExternalForm();
            userIcon.setImage(new Image(imgPath));
            userIcon.setFitWidth(35);  
            userIcon.setFitHeight(35); 
            userIcon.setPreserveRatio(true);
            userIcon.setSmooth(true);
        } catch (NullPointerException e) {
            System.err.println("Warning: Icon not found at /Icons/admin.png");
        }

        Label lblUser = new Label("USER ADMIN");
        lblUser.setStyle("-fx-text-fill: #777777; -fx-font-size: 14px; -fx-font-family: " + FONT_FAMILY + ";"); 

        Label lblArrow = new Label("v"); 
        lblArrow.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;"); 

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

        Label lblIncident = new Label("INCIDENT DETAILS");
        lblIncident.setStyle("-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-size: 16px; "
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #004aad, #cb6ce6);");
        lblIncident.setPadding(new Insets(0, 0, 10, 0));

        VBox formFields = new VBox(15);
        
        // Row 1: Student Search
        formFields.getChildren().add(createStudentSearchBox());

        // Row 2: Category and Date
        HBox row2 = new HBox(20);
        
        VBox boxCategory = new VBox(5);
        Label lblCategory = createFormLabel("Type"); 
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
        formatDatePicker(datePicker); 
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

        // --- UPDATED Row 4: Violation Dropdown + Custom Text ---
        VBox boxViolation = new VBox(5);
        Label lblViolation = createFormLabel("Violation Type:");
        
        cmbSpecificViolation = new ComboBox<>(FXCollections.observableArrayList(
                "No ID", "Improper Uniform", "Late", "Absence", "Loitering", 
                "Disrespectful Behavior", "Use of Phone during class", "Smoking", "Vandalism", "Other"
        ));
        cmbSpecificViolation.setPromptText("Select Violation...");
        cmbSpecificViolation.setMaxWidth(Double.MAX_VALUE);
        cmbSpecificViolation.setStyle(BORDER_STYLE);

        Label lblDesc = createFormLabel("Custom Description (If 'Other'):");
        txtDescription = new TextArea();
        txtDescription.setPromptText("Enter custom violation details...");
        txtDescription.setPrefRowCount(3);
        txtDescription.setWrapText(true);
        txtDescription.setDisable(true); // Disabled by default
        txtDescription.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-control-inner-background: #f3f4f6;");

        // Logic to enable/disable Custom Text when "Other" is selected
        cmbSpecificViolation.setOnAction(e -> {
            boolean isOther = "Other".equals(cmbSpecificViolation.getValue());
            txtDescription.setDisable(!isOther);
            if (isOther) {
                txtDescription.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-control-inner-background: white;");
            } else {
                txtDescription.clear();
                txtDescription.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-control-inner-background: #f3f4f6;");
            }
        });

        boxViolation.getChildren().addAll(lblViolation, cmbSpecificViolation, lblDesc, txtDescription);
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
                cmbStudent.show(); 
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

    // --- UPDATED VALIDATION ---
    private boolean validateFields() {
        if (cmbStudent.getValue() == null || cmbStudent.getEditor().getText().isEmpty()) return false;
        if (cmbCategory.getValue() == null) return false;
        if (datePicker.getValue() == null) return false;
        if (cmbSanction.getValue() == null) return false;
        if (cmbSpecificViolation.getValue() == null) return false; // Prevent empty dropdown
        if ("Other".equals(cmbSpecificViolation.getValue()) && txtDescription.getText().trim().isEmpty()) return false; // Prevent empty "Other" text
        return true;
    }

    // ==========================================
    // 6. DATABASE LOGIC 
    // ==========================================
    private void loadStudentsFromDatabase() {
        allStudents.clear();
        String sql = "SELECT StudentID, Name, Course FROM Students;"; 
        
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

    // --- UPDATED SAVE RECORD ---
    private void saveRecord() {
        if (!validateFields()) {
            ui.components.CustomDialog.showMessage("Validation Error", "Please fill up all required fields.", true);
            return;
        }

        StudentItem selectedStudent = cmbStudent.getValue();
        
        String sqlViolation = "INSERT INTO Violations (Date, StudentID, StudentName, Course, Location, Violation, Type, Sanction, Status) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pending');";
                              
        String sqlAudit = "INSERT INTO AuditLogs (User, Action, Details) VALUES ('SYSTEM', 'Logged Violation', ?);";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement psVio = conn.prepareStatement(sqlViolation);
                 PreparedStatement psAudit = conn.prepareStatement(sqlAudit)) {

                psVio.setString(1, datePicker.getValue().toString()); 
                psVio.setString(2, selectedStudent.getId());
                psVio.setString(3, selectedStudent.getName());
                psVio.setString(4, selectedStudent.getCourse());
                psVio.setString(5, ""); 
                
                // Determine what text to save based on the dropdown selection
                String finalViolationText = "Other".equals(cmbSpecificViolation.getValue()) ? 
                                txtDescription.getText().trim() : 
                                cmbSpecificViolation.getValue();
                
                psVio.setString(6, finalViolationText); 
                psVio.setString(7, cmbCategory.getValue()); 
                psVio.setString(8, cmbSanction.getValue());
                psVio.executeUpdate();

                String auditDetails = "Logged " + cmbCategory.getValue() + " violation for " + selectedStudent.getId();
                psAudit.setString(1, auditDetails);
                psAudit.executeUpdate();

                conn.commit();
                
                ui.components.CustomDialog.showMessage("Success", "Record and Audit Log saved successfully.", false);
                clearForm();

            } catch (Exception ex) {
                conn.rollback(); 
                throw ex; 
            }

        } catch (Exception e) {
            ui.components.CustomDialog.showMessage("Database Error", "Failed to save record: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        cmbStudent.getSelectionModel().clearSelection();
        cmbStudent.getEditor().clear();
        cmbCategory.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now());
        cmbSanction.getSelectionModel().clearSelection();
        cmbSpecificViolation.getSelectionModel().clearSelection(); // Reset violation dropdown
        txtDescription.clear();
        txtDescription.setDisable(true); // Re-disable custom text box
        filteredStudents.setAll(allStudents); 
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