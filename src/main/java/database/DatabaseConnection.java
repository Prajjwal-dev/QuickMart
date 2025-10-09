package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnection {
    // Default DB URL: prefer the database created by database_setup.sql (QuickMartDB)
    private static final String URL = System.getProperty("DB_URL", System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/QuickMartDB"));
    private static final String USER = System.getProperty("DB_USER", System.getenv().getOrDefault("DB_USER", "root"));
    private static final String PASSWORD = System.getProperty("DB_PASS", System.getenv().getOrDefault("DB_PASS", ""));

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Please add mysql-connector-java to your classpath.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException ex) {
            System.err.println("Failed to connect to DB with URL=" + URL + " user=" + USER);
            throw ex;
        }
    }

    public static boolean validateUser(String username, String password) {
        // Use BINARY to enforce case-sensitive username comparison on MySQL
        String query = "SELECT * FROM users WHERE BINARY username = ? AND password = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next(); // Returns true if a matching user is found
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getUserRole(String username) {
        // Use BINARY so role lookup matches username case-sensitively
        String query = "SELECT role FROM users WHERE BINARY username = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Returns null if no role is found
    }
}
