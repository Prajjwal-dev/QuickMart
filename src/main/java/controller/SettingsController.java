package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import java.util.prefs.Preferences;

public class SettingsController {
    @FXML private TextField taxField;
    @FXML private TextField discountField;
    @FXML private javafx.scene.control.CheckBox lowStockEnable;
    @FXML private Button saveBtn;
    @FXML private Button refreshBtn;

    @FXML
    private void initialize() {
        refreshBtn.setOnAction(e -> loadSettings());
        saveBtn.setOnAction(e -> saveSettings());
        loadSettings();
    }

    private void loadSettings() {
        // Load latest tax/discount from DB. Low-stock toggle is stored locally in Preferences.
        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT tax, discount FROM settings ORDER BY updated_at DESC LIMIT 1");
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                taxField.setText(String.format("%.2f", rs.getDouble("tax")));
                discountField.setText(String.format("%.2f", rs.getDouble("discount")));
            } else {
                taxField.setText(""); discountField.setText("");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        boolean lowEnabled = prefs.getBoolean("lowStockEnabled", false);
        lowStockEnable.setSelected(lowEnabled);
    }

    private void saveSettings() {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            // Defensive parsing: provide sensible defaults and validation
            String taxTxt = taxField.getText() == null ? "" : taxField.getText().trim();
            String discountTxt = discountField.getText() == null ? "" : discountField.getText().trim();

            double tax = 0.0;
            double discount = 0.0;

            try {
                if (!taxTxt.isEmpty()) tax = Double.parseDouble(taxTxt);
            } catch (NumberFormatException nfe) {
                new Alert(Alert.AlertType.ERROR, "Invalid tax value. Please enter a number like 7.5").showAndWait();
                return;
            }
            try {
                if (!discountTxt.isEmpty()) discount = Double.parseDouble(discountTxt);
            } catch (NumberFormatException nfe) {
                new Alert(Alert.AlertType.ERROR, "Invalid discount value. Please enter a number like 2.5").showAndWait();
                return;
            }

            // Normalize negative values
            if (tax < 0 || discount < 0) {
                new Alert(Alert.AlertType.ERROR, "Values cannot be negative.").showAndWait();
                return;
            }

            // Save tax & discount to DB. Low-stock enabled flag saved locally (Preferences)
            java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO settings (tax, discount) VALUES (?, ?)");
            ps.setDouble(1, tax);
            ps.setDouble(2, discount);
            ps.executeUpdate();

            Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
            prefs.putBoolean("lowStockEnabled", lowStockEnable.isSelected());

            new Alert(Alert.AlertType.INFORMATION, "Settings saved").showAndWait();
            // reload to reflect normalized values and clear inputs where applicable
            loadSettings();
        } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to save settings: " + ex.getMessage()).showAndWait(); }
    }
}
