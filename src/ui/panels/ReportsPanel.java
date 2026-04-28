package ui.panels;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import db.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class ReportsPanel extends BorderPane {

    private DatePicker dpFrom;
    private DatePicker dpTo;
    private PieChart pieChart;
    private BarChart<String, Number> barChart;
    private VBox offendersList;

    public ReportsPanel() {
        setPadding(new Insets(30, 40, 40, 40));
        
        // 1. Header Area (Title + Buttons)
        setTop(createHeader());

        // 2. Main Content Area
        VBox content = new VBox(25);
        content.setPadding(new Insets(20, 0, 0, 0));
        
        content.getChildren().addAll(
            createDateFilters(),
            createChartsRow(),
            createOffendersSection()
        );

        setCenter(content);

        // Load Initial Data
        refreshDashboardData();
    }

    // ==========================================
    // UI BUILDERS
    // ==========================================
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("Reports");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 32));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Export Buttons
        Button btnPrintStudent = new Button("🖨 Print Student History");
        btnPrintStudent.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnPrintStudent.setOnAction(e -> promptForStudentReport());

        Button btnPrintMonthly = new Button("📥 Print Monthly Report");
        btnPrintMonthly.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnPrintMonthly.setOnAction(e -> promptForMonthlyReport(e));

        HBox btnBox = new HBox(15, btnPrintStudent, btnPrintMonthly);
        header.getChildren().addAll(title, spacer, btnBox);
        return header;
    }

    private HBox createDateFilters() {
        HBox filterBox = new HBox(20);
        
        VBox fromBox = new VBox(5);
        Label lblFrom = new Label("From");
        dpFrom = new DatePicker(LocalDate.now().minusMonths(1));
        fromBox.getChildren().addAll(lblFrom, dpFrom);

        VBox toBox = new VBox(5);
        Label lblTo = new Label("To");
        dpTo = new DatePicker(LocalDate.now());
        toBox.getChildren().addAll(lblTo, dpTo);
        
        Button btnApply = new Button("Apply");
        btnApply.setStyle("-fx-background-color: #e5e7eb; -fx-cursor: hand;");
        btnApply.setOnAction(e -> refreshDashboardData());
        VBox applyBox = new VBox(5, new Label(""), btnApply);

        filterBox.getChildren().addAll(fromBox, toBox, applyBox);
        return filterBox;
    }

    private HBox createChartsRow() {
        HBox chartsBox = new HBox(20);
        
        VBox pieCard = createWhiteCard("Incidents by Category");
        pieChart = new PieChart();
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);
        pieCard.getChildren().add(pieChart);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        VBox barCard = createWhiteCard("Monthly Trend");
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barCard.getChildren().add(barChart);
        HBox.setHgrow(barCard, Priority.ALWAYS);

        chartsBox.getChildren().addAll(pieCard, barCard);
        return chartsBox;
    }

    private VBox createOffendersSection() {
        VBox listCard = createWhiteCard("Top Repeat Offenders");
        offendersList = new VBox(10);
        listCard.getChildren().add(offendersList);
        return listCard;
    }

    private VBox createWhiteCard(String titleText) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-padding: 20;");
        Label title = new Label(titleText);
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        card.getChildren().add(title);
        return card;
    }

    // ==========================================
    // DASHBOARD DATA LOADERS
    // ==========================================
    private void refreshDashboardData() {
        String startDate = dpFrom.getValue().toString();
        String endDate = dpTo.getValue().toString();

        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Pie Chart
                String pieSql = "SELECT Type, COUNT(*) FROM Violations WHERE Date >= ? AND Date <= ? GROUP BY Type";
                PreparedStatement psPie = conn.prepareStatement(pieSql);
                psPie.setString(1, startDate);
                psPie.setString(2, endDate);
                ResultSet rsPie = psPie.executeQuery();
                
                Platform.runLater(() -> pieChart.getData().clear());
                while (rsPie.next()) {
                    String type = rsPie.getString(1);
                    int count = rsPie.getInt(2);
                    if(type != null) {
                        Platform.runLater(() -> pieChart.getData().add(new PieChart.Data(type, count)));
                    }
                }

                // Bar Chart
                String barSql = "SELECT strftime('%Y-%m', Date) as Month, COUNT(*) FROM Violations WHERE Date >= ? AND Date <= ? GROUP BY Month ORDER BY Month";
                PreparedStatement psBar = conn.prepareStatement(barSql);
                psBar.setString(1, startDate);
                psBar.setString(2, endDate);
                ResultSet rsBar = psBar.executeQuery();
                
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                while (rsBar.next()) {
                    series.getData().add(new XYChart.Data<>(rsBar.getString(1), rsBar.getInt(2)));
                }
                Platform.runLater(() -> {
                    barChart.getData().clear();
                    barChart.getData().add(series);
                });

                // Offenders
                String offSql = "SELECT StudentName, COUNT(*) as Incidents FROM Violations WHERE Date >= ? AND Date <= ? GROUP BY StudentName HAVING COUNT(*) > 1 ORDER BY Incidents DESC LIMIT 5";
                PreparedStatement psOff = conn.prepareStatement(offSql);
                psOff.setString(1, startDate);
                psOff.setString(2, endDate);
                ResultSet rsOff = psOff.executeQuery();
                
                Platform.runLater(() -> offendersList.getChildren().clear());
                while (rsOff.next()) {
                    String name = rsOff.getString("StudentName");
                    int incidents = rsOff.getInt("Incidents");
                    
                    HBox row = new HBox();
                    row.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0; -fx-padding: 10 0;");
                    Label lblName = new Label((name != null && !name.isEmpty()) ? name : "Unknown");
                    lblName.setStyle("-fx-font-weight: bold;");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    Label lblInc = new Label(incidents + " incident(s)");
                    lblInc.setStyle("-fx-text-fill: #6b7280;");
                    
                    row.getChildren().addAll(lblName, sp, lblInc);
                    Platform.runLater(() -> offendersList.getChildren().add(row));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ==========================================
    // ITEXT PDF GENERATION LOGIC
    // ==========================================
    private void promptForMonthlyReport(javafx.event.ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Monthly Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Monthly_Report.pdf");
        
        // Grab the exact window from the button you just clicked
        javafx.stage.Window ownerWindow = ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);
        
        if (file != null) {
            generateMonthlyReport(file, dpFrom.getValue().toString(), dpTo.getValue().toString());
        }
    }

    // --- CUSTOM STYLED SEARCHABLE DROPDOWN DIALOG ---
    private void promptForStudentReport() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED); // Removes default windows borders

        // Main Container matching CustomDialog style
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #d1d5db; -fx-border-width: 2; -fx-border-radius: 15;");
        layout.setPrefWidth(400);

        // Title
        Text title = new Text("Print Student Record");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 22));
        title.setFill(Color.web("#004aad"));

        // Search Label
        Label lblStudent = new Label("Search Student:");
        lblStudent.setStyle("-fx-font-family: 'ITC Avant Garde Gothic', sans-serif; -fx-text-fill: #6d6a6a; -fx-font-weight: bold; -fx-font-size: 13px;");

        // The EXACT ComboBox styling and logic from LogViolationPanel
        ComboBox<StudentItem> cmbStudent = new ComboBox<>();
        cmbStudent.setEditable(true);
        cmbStudent.setPromptText("Search by student name or ID...");
        cmbStudent.setMaxWidth(Double.MAX_VALUE);
        cmbStudent.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-padding: 2;");

        ObservableList<StudentItem> allStudents = FXCollections.observableArrayList();
        ObservableList<StudentItem> filteredStudents = FXCollections.observableArrayList();

        // Load data from DB
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT StudentID, Name FROM Students");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allStudents.add(new StudentItem(rs.getString("StudentID"), rs.getString("Name")));
            }
            filteredStudents.setAll(allStudents);
            cmbStudent.setItems(filteredStudents);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        // Filtering logic
        cmbStudent.setConverter(new javafx.util.StringConverter<StudentItem>() {
            @Override public String toString(StudentItem object) { return object == null ? "" : object.toString(); }
            @Override public StudentItem fromString(String string) {
                return cmbStudent.getItems().stream().filter(item -> item.toString().equals(string)).findFirst().orElse(null);
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

        VBox searchBox = new VBox(5, lblStudent, cmbStudent);

        // Buttons (Cancel & Generate)
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnGenerate = new Button("Generate PDF");
        btnGenerate.setStyle("-fx-background-color: linear-gradient(to right, #004aad, #cb6ce6); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        btnGenerate.setOnAction(e -> {
            StudentItem selected = cmbStudent.getValue();
            if (selected == null || cmbStudent.getEditor().getText().isEmpty()) {
                ui.components.CustomDialog.showMessage("Error", "Please select a valid student from the list.", true);
                return;
            }
            
            // NOTE: We REMOVED dialog.close() from here!
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Student Report");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fileChooser.setInitialFileName(selected.getId() + "_History.pdf");
            
            // Attach the save menu directly to the custom popup we just built
            File file = fileChooser.showSaveDialog(dialog);
            
            if (file != null) {
                generateStudentReport(file, selected.getId());
            }
            
            // NOW we close the popup safely after everything is finished!
            dialog.close(); 
        });

        buttonBox.getChildren().addAll(btnCancel, btnGenerate);
        layout.getChildren().addAll(title, searchBox, buttonBox);
        
        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    private void generateMonthlyReport(File file, String startDate, String endDate) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Document Header
            addPdfHeader(document, "Monthly Violation Report\nFrom: " + startDate + " To: " + endDate);

            // Create Table (8 Columns)
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(20f);
            
            String[] headers = {"Date", "Student", "ID", "Course", "Violation", "Type", "Sanction", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            int totalViolations = 0;

            // Fetch Data
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT * FROM Violations WHERE Date >= ? AND Date <= ? ORDER BY Date DESC";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, startDate);
                ps.setString(2, endDate);
                ResultSet rs = ps.executeQuery();

                com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
                while (rs.next()) {
                    totalViolations++;
                    table.addCell(new Phrase(rs.getString("Date"), dataFont));
                    table.addCell(new Phrase(rs.getString("StudentName"), dataFont));
                    table.addCell(new Phrase(rs.getString("StudentID"), dataFont));
                    table.addCell(new Phrase(rs.getString("Course"), dataFont));
                    table.addCell(new Phrase(rs.getString("Violation"), dataFont));
                    table.addCell(new Phrase(rs.getString("Type"), dataFont));
                    table.addCell(new Phrase(rs.getString("Sanction"), dataFont));
                    table.addCell(new Phrase(rs.getString("Status"), dataFont));
                }
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("Total Violations in Period: " + totalViolations, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            footer.setSpacingBefore(20f);
            document.add(footer);

            document.close();
            ui.components.CustomDialog.showMessage("Success", "Monthly Report successfully saved to PDF.", false);

        } catch (Exception e) {
            e.printStackTrace();
            ui.components.CustomDialog.showMessage("Export Error", "Failed to create PDF: " + e.getMessage(), true);
        }
    }

    private void generateStudentReport(File file, String studentId) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Setup Header variables
            String studentName = "Unknown";
            String course = "Unknown";
            int totalViolations = 0;

            // First Pass: Get Student Info
            try (Connection conn = DatabaseConnection.getConnection()) {
                String infoSql = "SELECT Name, Course FROM Students WHERE StudentID = ?";
                PreparedStatement psInfo = conn.prepareStatement(infoSql);
                psInfo.setString(1, studentId);
                ResultSet rsInfo = psInfo.executeQuery();
                if (rsInfo.next()) {
                    studentName = rsInfo.getString("Name");
                    course = rsInfo.getString("Course");
                }
            }

            // Document Header
            addPdfHeader(document, "Official Student Discipline Record");
            
            // Student Info Text
            com.itextpdf.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Student Name: " + studentName, infoFont));
            document.add(new Paragraph("Student ID: " + studentId, infoFont));
            document.add(new Paragraph("Course: " + course, infoFont));
            document.add(new Paragraph(" ")); // Blank Line

            // Create Table (5 Columns for individual view)
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            
            String[] headers = {"Date", "Violation", "Type", "Sanction", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Fetch Incident Data
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT * FROM Violations WHERE StudentID = ? ORDER BY Date DESC";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentId);
                ResultSet rs = ps.executeQuery();

                com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                while (rs.next()) {
                    totalViolations++;
                    table.addCell(new Phrase(rs.getString("Date"), dataFont));
                    table.addCell(new Phrase(rs.getString("Violation"), dataFont));
                    table.addCell(new Phrase(rs.getString("Type"), dataFont));
                    table.addCell(new Phrase(rs.getString("Sanction"), dataFont));
                    table.addCell(new Phrase(rs.getString("Status"), dataFont));
                }
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("Total Lifetime Incidents: " + totalViolations, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            footer.setSpacingBefore(20f);
            document.add(footer);

            document.close();
            ui.components.CustomDialog.showMessage("Success", "Student History successfully saved to PDF.", false);

        } catch (Exception e) {
            e.printStackTrace();
            ui.components.CustomDialog.showMessage("Export Error", "Failed to create PDF: " + e.getMessage(), true);
        }
    }

    private void addPdfHeader(Document document, String subTitle) throws Exception {
        
        // 1. Load and Add the QCU Logo
        try {
            URL logoUrl = getClass().getResource("/Icons/Logo.png");
            if (logoUrl != null) {
                com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(logoUrl);
                logo.scaleToFit(80, 80); 
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } else {
                System.out.println("Warning: Logo not found for PDF export.");
            }
        } catch (Exception e) {
            System.out.println("Error adding logo to PDF: " + e.getMessage());
        }

        // 2. Add the Text Headers
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        com.itextpdf.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        
        Paragraph title = new Paragraph("QUEZON CITY UNIVERSITY", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10f); // Adds breathing room under the logo
        document.add(title);
        
        Paragraph sub = new Paragraph("STUDENT DISCIPLINE SYSTEM\n" + subTitle, subTitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20f);
        document.add(sub);
    }

    // --- MISSING CLASS FIXED HERE ---
    private static class StudentItem {
        private final String id;
        private final String name;

        public StudentItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return id + " - " + name;
        }
    }
}