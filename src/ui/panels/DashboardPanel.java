package ui.panels;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
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
import javafx.scene.chart.CategoryAxis;

import db.DatabaseConnection;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardPanel extends BorderPane {

    // Text nodes for our dynamic database values
    private Text totalViolationsText;
    private Text violationsTodayText;
    private AreaChart<String, Number> topViolationsChart;
    // Universal Gradient used across the dashboard
    private final LinearGradient mainGradient = new LinearGradient(
            0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#004aad")),
            new Stop(1, Color.web("#cb6ce6"))
    );

    public DashboardPanel() {
        // 1. MAIN LAYOUT SETUP
        setPadding(new Insets(10, 40, 40, 40)); 
        
        // Link to external CSS
        try {
            String cssPath = getClass().getResource("/ui/panels/dashboard-style.css").toExternalForm();
            getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("WARNING: Could not load dashboard-style.css.");
        }

        // 2. ASSEMBLE SECTIONS
        setTop(createHeader());

        VBox centerContent = new VBox(40); // 40px spacing between cards and chart
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(30, 0, 0, 0));
        centerContent.getChildren().addAll(createStatCards(), createChart());

        setCenter(centerContent);

        // 3. LOAD DATA
        loadDashboardData();
        loadTopViolations();
    }

    // ==========================================
    // UI BUILDER METHODS
    // ==========================================

    private HBox createHeader() {
        HBox headerArea = new HBox();
        headerArea.setAlignment(Pos.CENTER); 
        headerArea.setPadding(new Insets(0, 0, 20, 0));
        
        // Title text 
        Text title = new Text("DASHBOARD");
        title.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.BOLD, 60)); 
        title.setFill(mainGradient);
        
        // Spacer to push User Admin to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // User Admin Profile Section
        
        
        
        // Minor mock dropdown arrow matching reference image
        Label dropdownArrow = new Label("v");
        dropdownArrow.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        
       
        return headerArea;
    }

    private HBox createStatCards() {
        HBox cardsRow = new HBox(50); 
        cardsRow.setAlignment(Pos.CENTER);

        // Initialize our dynamic text nodes with placeholders
        totalViolationsText = new Text("00");
        violationsTodayText = new Text("00");

        VBox card1 = createSingleCard("TOTAL VIOLATIONS", totalViolationsText);
        VBox card2 = createSingleCard("VIOLATIONS TODAY", violationsTodayText);

        cardsRow.getChildren().addAll(card1, card2);
        return cardsRow;
    }

    private VBox createSingleCard(String labelText, Text valueText) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(350, 310); 
        card.setMaxSize(350, 310);
        card.getStyleClass().add("dashboard-card");

        // Format the enormous number text 
        valueText.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.BOLD, 140));
        valueText.setFill(mainGradient);

        // Format the label below it
        Text label = new Text(labelText);
        label.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.NORMAL, 18));
        label.setFill(mainGradient);

        // Spacer to push items slightly up to perfectly center them visually
        Region bottomPadding = new Region();
        bottomPadding.setPrefHeight(25);

        card.getChildren().addAll(valueText, label, bottomPadding);
        return card;
    }

    // ==========================================
    // UPDATED CHART BUILDER
    // ==========================================
    private VBox createChart() {
        VBox chartCard = new VBox(0);
        chartCard.setAlignment(Pos.TOP_CENTER);
        
        // --- UPDATED HEIGHT --- 
        chartCard.setPrefHeight(220); 
        chartCard.setMaxWidth(750); 
        
        // --- UPDATED PADDING ---
        chartCard.setPadding(new Insets(20, 20, 10, 20)); 
        chartCard.getStyleClass().add("dashboard-card");

        // Chart Title
        Text chartTitle = new Text("TOP VIOLATIONS");
        chartTitle.setFont(Font.font("ITC Avant Garde Gothic", FontWeight.BOLD, 28));
        chartTitle.setFill(mainGradient);

        // Chart Configuration (Hidden axes and grid)
        // CHANGED: X-Axis is now CategoryAxis to support text (Violation Names)
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setVisible(true); // Kept hidden as requested
        yAxis.setVisible(true); // Kept hidden as requested

        // Initialize the global chart variable
        topViolationsChart = new AreaChart<>(xAxis, yAxis);
        topViolationsChart.setHorizontalGridLinesVisible(false);
        topViolationsChart.setVerticalGridLinesVisible(false);
        topViolationsChart.setLegendVisible(false); // Hide the legend
        topViolationsChart.setCreateSymbols(false); // Hide data dots for a smooth look
        topViolationsChart.setAlternativeRowFillVisible(false);
        topViolationsChart.setAlternativeColumnFillVisible(false);

        // Add title and chart to the card (No dummy data anymore!)
        chartCard.getChildren().addAll(chartTitle, topViolationsChart);
        return chartCard;
    }

    // ==========================================
    // NEW: DYNAMIC TOP VIOLATIONS LOADER
    // ==========================================
    private void loadTopViolations() {
        // Run DB query on a background thread so the UI doesn't freeze
        new Thread(() -> {
            String query = "SELECT Violation, COUNT(*) AS Total FROM Violations GROUP BY Violation ORDER BY Total DESC LIMIT 5";
            
            // Create a single series to hold our dynamic data
            XYChart.Series<String, Number> series = new XYChart.Series<>();

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                // Loop through the database results
                while (rs.next()) {
                    String violationName = rs.getString("Violation");
                    int count = rs.getInt("Total");
                    
                    // Prevent null names from crashing the chart
                    if (violationName == null || violationName.isEmpty()) {
                        violationName = "Unknown";
                    }

                    // Add each row into our chart series
                    series.getData().add(new XYChart.Data<>(violationName, count));
                }

            } catch (SQLException e) {
                System.err.println("Top Violations DB Error: " + e.getMessage());
            }

            // Update UI safely on the main JavaFX thread
            Platform.runLater(() -> {
                topViolationsChart.getData().clear(); // Clear any old data
                
                // Only add the series if we actually found data in the database
                if (!series.getData().isEmpty()) {
                    topViolationsChart.getData().add(series);
                }
            });

        }).start();
    }

    // ==========================================
    // DATABASE LOGIC
    // ==========================================

    private void loadDashboardData() {
        // Run DB query on a background thread so the UI doesn't freeze
        new Thread(() -> {
            String queryTotal = "SELECT COUNT(*) FROM Violations";
            String queryToday = "SELECT COUNT(*) FROM Violations WHERE DATE(Date) = CURRENT_DATE";

            int total = 0;
            int today = 0;

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn != null) {
                    // Get Total Violations
                    try (PreparedStatement pstmt = conn.prepareStatement(queryTotal);
                         ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }

                    // Get Violations Today
                    try (PreparedStatement pstmt2 = conn.prepareStatement(queryToday);
                         ResultSet rs2 = pstmt2.executeQuery()) {
                        if (rs2.next()) today = rs2.getInt(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Dashboard DB Error: " + e.getMessage());
            }

            // Update UI safely on the main JavaFX thread
            final String finalTotal = String.format("%02d", total); // Format as "08" instead of "8"
            final String finalToday = String.format("%02d", today);

            Platform.runLater(() -> {
                totalViolationsText.setText(finalTotal);
                violationsTodayText.setText(finalToday);
            });

        }).start();
    }

    // --- HELPER: Safe Image Loading ---
    private ImageView loadImageView(String path, double w, double h) {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                iv.setImage(img);
            }
        } catch (Exception e) { /* Ignored */ }
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        return iv;
    }
}