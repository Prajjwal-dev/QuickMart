package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminDashboard {
    private Stage stage;

    public AdminDashboard(Stage stage) {
        this.stage = stage;
    }

    public void showDashboard(String username, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            stage.setTitle("Admin Dashboard - QuickMart");
            stage.setScene(scene);
            stage.show();

            // Set profile photo, username, and role in the navbar
            AdminDashboardController controller = loader.getController();
            controller.setUserDetails(username, role);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
