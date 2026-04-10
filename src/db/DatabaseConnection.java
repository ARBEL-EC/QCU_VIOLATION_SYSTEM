package db;

import java.io.File;
import java.sql.*;

public class DatabaseConnection {
    
    // SINGLE SOURCE OF TRUTH for DB Name
    // FIXED: Now points to your actual database file
    private static final String DB_NAME = "school_violation.db"; 
    private static final String URL = "jdbc:sqlite:" + DB_NAME;

    public static Connection getConnection() {
        try {
            // FIXED: Prevent SQLite from creating a new file if it's missing
            File dbFile = new File(DB_NAME);
            if (!dbFile.exists()) {
                System.err.println("CRITICAL ERROR: Database file not found: " + dbFile.getAbsolutePath());
                System.err.println("Please ensure 'school_violation.db' is placed in the project root folder.");
                return null;
            }

            // Explicitly load driver to ensure it's found in the classpath
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("CRITICAL: Database Connection Failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initializes the database:
     * 1. Creates all required tables.
     * 2. Inserts the default Admin account if missing.
     */
    public static void initializeDatabase() {
        System.out.println("Initializing Database at: " + URL);
        
        String sqlStudents = "CREATE TABLE IF NOT EXISTS Students ("
                + "StudentID TEXT PRIMARY KEY, Name TEXT, Course TEXT);";

        String sqlViolations = "CREATE TABLE IF NOT EXISTS Violations ("
                + "ViolationID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Date TEXT, StudentID TEXT, StudentName TEXT, Course TEXT, "
                + "Violation TEXT, Type TEXT, Sanction TEXT, Location TEXT, "
                + "Status TEXT DEFAULT 'Pending');";

        String sqlAudit = "CREATE TABLE IF NOT EXISTS AuditLogs ("
                + "LogID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "User TEXT, Action TEXT, Details TEXT);";

        String sqlUsers = "CREATE TABLE IF NOT EXISTS Users ("
                + "UserID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Username TEXT UNIQUE, "
                + "Password TEXT, "
                + "Role TEXT);";

        // Insert Default Admin (Safe Insert)
        String sqlDefaultAdmin = "INSERT OR IGNORE INTO Users (Username, Password, Role) "
                + "VALUES ('admin', 'admin123', 'Admin');";

        try (Connection conn = getConnection()) {
            // Check if connection is valid bewfore creating statement
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sqlStudents);
                    stmt.execute(sqlViolations);
                    stmt.execute(sqlAudit);
                    stmt.execute(sqlUsers);
                    stmt.execute(sqlDefaultAdmin);
                    System.out.println("Database tables checked/created successfully.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}