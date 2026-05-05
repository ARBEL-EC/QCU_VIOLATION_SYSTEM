package ui.panels;

import db.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
import java.util.Arrays;
import java.util.List;

public class LogViolationPanel extends BorderPane {
    private String currentUsername;
    
    // Form Components
    private ComboBox<String> cmbViolation; // UPDATED: Consolidated Dropdown
    private ComboBox<StudentItem> cmbStudent;
    private ComboBox<String> cmbCategory; // This is the "Type" (Minor/Major)
    private DatePicker datePicker;
    private ComboBox<String> cmbSanction;

    // Data lists
    private ObservableList<StudentItem> allStudents = FXCollections.observableArrayList();
    private ObservableList<StudentItem> filteredStudents = FXCollections.observableArrayList();

    // Fonts & Colors
    private final String FONT_FAMILY = "'ITC Avant Garde Gothic', 'Segoe UI', sans-serif";
    private final String LABEL_COLOR = "#6d6a6a";
    private final String BORDER_STYLE = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-padding: 2;";
    
    public LogViolationPanel(String username) {
        this.currentUsername = username;
        
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
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label lblTitle = new Label("LOG VIOLATION");
        lblTitle.setStyle("-fx-font-family: " + FONT_FAMILY + "; "
                + "-fx-font-size: 60px; " 
                + "-fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #004aad, #cb6ce6);");

        header.getChildren().add(lblTitle);
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
        
        // --- ROW 1: Student Search (MOVED TO TOP) ---
        formFields.getChildren().add(createStudentSearchBox());

        // --- ROW 2: Violation Dropdown (MOVED BELOW STUDENT) ---
        VBox boxViolation = new VBox(5);
        Label lblViolation = createFormLabel("Violation");
        
        // Categorized Lists
        List<String> majorOffenses = Arrays.asList(
            "Cheating", "Plagiarism", "Vandalism", "Possession of weapons or explosives",
            "Major Academic Disruption", "Violence", "Substance abuse (Drugs/Alcohol)",
            "Damaging university’s reputation", "Stealing", "Bullying", "Smoking",
            "Falsifying official documents", "Gambling", "Illegal protests/rallies",
            "Criminal conviction", "Pornography", "PDA", "ID misuse", "Repeated offenses"
        );

        List<String> minorOffenses = Arrays.asList(
            "Conduct unbecoming", "Classroom Disturbance", 
            "Non-compliance with regulations", "Littering"
        );

        ObservableList<String> allViolations = FXCollections.observableArrayList();
        allViolations.add("--- MAJOR OFFENSES ---");
        allViolations.addAll(majorOffenses);
        allViolations.add("--- MINOR OFFENSES ---");
        allViolations.addAll(minorOffenses);

        cmbViolation = new ComboBox<>(allViolations);
        cmbViolation.setPromptText("Select Violation...");
        cmbViolation.setMaxWidth(Double.MAX_VALUE);
        cmbViolation.setStyle(BORDER_STYLE);
        
        boxViolation.getChildren().addAll(lblViolation, cmbViolation);
        formFields.getChildren().add(boxViolation);

        // --- ROW 3: Type (Auto-filled/Locked) and Date ---
        HBox row3 = new HBox(20);
        
        VBox boxCategory = new VBox(5);
        Label lblCategory = createFormLabel("Type");
        cmbCategory = new ComboBox<>(FXCollections.observableArrayList("MINOR", "MAJOR"));
        cmbCategory.setPromptText("Auto-filled...");
        cmbCategory.setMaxWidth(Double.MAX_VALUE);
        
        // LOCK THE TYPE FIELD
        cmbCategory.setMouseTransparent(true);
        cmbCategory.setFocusTraversable(false);
        cmbCategory.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 8; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-padding: 2; -fx-opacity: 1; -fx-font-weight: bold;");
        
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

        row3.getChildren().addAll(boxCategory, boxDate);
        formFields.getChildren().add(row3);

        // --- ROW 4: Sanction ---
        VBox boxSanction = new VBox(5);
        Label lblSanction = createFormLabel("Sanction");
        cmbSanction = new ComboBox<>(FXCollections.observableArrayList(
                "Warning", "Community Service", "Suspension", "Drop", "Call Parent"
        ));
        cmbSanction.setPromptText("Select Sanction...");
        cmbSanction.setMaxWidth(Double.MAX_VALUE);
        cmbSanction.setStyle(BORDER_STYLE);
        boxSanction.getChildren().addAll(lblSanction, cmbSanction);
        formFields.getChildren().add(boxSanction);

        // --- AUTO-FILL LOGIC FOR TYPE ---
        cmbViolation.setOnAction(e -> {
            String selected = cmbViolation.getValue();
            if (selected != null) {
                if (selected.startsWith("---")) {
                    // Prevent users from actually selecting the category headers
                    Platform.runLater(() -> cmbViolation.getSelectionModel().clearSelection());
                    cmbCategory.getSelectionModel().clearSelection();
                } else if (majorOffenses.contains(selected)) {
                    cmbCategory.setValue("MAJOR");
                } else if (minorOffenses.contains(selected)) {
                    cmbCategory.setValue("MINOR");
                }
            }
        });

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

    private boolean validateFields() {
        if (cmbViolation.getValue() == null || cmbViolation.getValue().startsWith("---")) return false;
        if (cmbStudent.getValue() == null || cmbStudent.getEditor().getText().isEmpty()) return false;
        if (cmbCategory.getValue() == null) return false;
        if (datePicker.getValue() == null) return false;
        if (cmbSanction.getValue() == null) return false;
        
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
            System.err.println("Warning: Could not load students.");
            e.printStackTrace();
        }
    }

    private void saveRecord() {
        if (!validateFields()) {
            ui.components.CustomDialog.showMessage("Validation Error", "Please fill up all required fields.", true);
            return;
        }

        StudentItem selectedStudent = cmbStudent.getValue();
        
        String sqlViolation = "INSERT INTO Violations (Date, StudentID, StudentName, Course, Location, Violation, Type, Sanction, Status) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pending');";
                              
        String sqlAudit = "INSERT INTO AuditLogs (User, Action, Details) VALUES (?, 'Logged Violation', ?);";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement psVio = conn.prepareStatement(sqlViolation);
                 PreparedStatement psAudit = conn.prepareStatement(sqlAudit)) {

                psVio.setString(1, datePicker.getValue().toString()); 
                psVio.setString(2, selectedStudent.getId());
                psVio.setString(3, selectedStudent.getName());
                psVio.setString(4, selectedStudent.getCourse());
                psVio.setString(5, ""); 
                
                String finalViolationText = cmbViolation.getValue();
                
                psVio.setString(6, finalViolationText); 
                psVio.setString(7, cmbCategory.getValue()); 
                psVio.setString(8, cmbSanction.getValue());
                psVio.executeUpdate();

                String auditDetails = "Logged " + cmbCategory.getValue() + " violation for " + selectedStudent.getId();
                
                psAudit.setString(1, currentUsername);
                psAudit.setString(2, auditDetails);
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
        cmbViolation.getSelectionModel().clearSelection();
        cmbStudent.getSelectionModel().clearSelection();
        cmbStudent.getEditor().clear();
        cmbCategory.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now());
        cmbSanction.getSelectionModel().clearSelection();
        
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