package controller;

import javafx.scene.control.Alert;
import view.AuthView;

public class AuthController {
    private AuthView authView;

    public AuthController(AuthView authView) {
        this.authView = authView;
        addEventHandlers();
    }

    private void addEventHandlers() {
        authView.getLoginButton().setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        String username = authView.getUsernameField().getText();
        String password = authView.getPasswordField().getText();

        // Simple validation (replace with real authentication logic)
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Please fill all fields.");
            return;
        }

        // Example: hardcoded check (replace with DB or file check)
        if (("admin".equals(username) && "admin123".equals(password)) ||
            ("cashier".equals(username) && "cashier123".equals(password))) {
            showAlert("Login successful! (Implement scene switch here)");
            // TODO: Switch to main dashboard scene
        } else {
            showAlert("Invalid credentials. Try again.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}