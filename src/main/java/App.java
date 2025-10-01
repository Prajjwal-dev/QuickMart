import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class App extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showWelcomePage();
    }

    private void showWelcomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/App.fxml"));
            Parent welcomeRoot = loader.load();
            Scene welcomeScene = new Scene(welcomeRoot);
            // apply global stylesheet
            welcomeScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("Welcome to QuickMart");
            primaryStage.setScene(welcomeScene);
            primaryStage.show();

                // Set logo image
                ImageView logoImageView = (ImageView) welcomeScene.lookup("#logoImageView");
                if (logoImageView != null) {
                    logoImageView.setImage(new Image(getClass().getResource("/assets/logo.png").toExternalForm()));
                }
                // Handle Get Started button click
                Button getStartedBtn = (Button) welcomeScene.lookup("#getStartedBtn");
                if (getStartedBtn != null) {
                    getStartedBtn.setOnAction(e -> showLoadingAndLogin(primaryStage));
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoginPage(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);
            // apply global stylesheet
            loginScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("Login - QuickMart");
            primaryStage.setScene(loginScene);
            primaryStage.show();

            // Handle login button click
            Button loginButton = (Button) loginScene.lookup("#loginButton");
            TextField usernameField = (TextField) loginScene.lookup("#usernameField");
            PasswordField passwordField = (PasswordField) loginScene.lookup("#passwordField");
            Button forgotPasswordButton = (Button) loginScene.lookup("#forgotPasswordButton");
            ImageView profileImageView = (ImageView) loginScene.lookup("#profileImageView");
            // Set profile photo from resources if view exists
            try {
                if (profileImageView != null) {
                    java.net.URL url = getClass().getResource("/assets/profile-icon-design-free-vector.jpg");
                    if (url != null) profileImageView.setImage(new Image(url.toExternalForm()));
                }
            } catch (Exception ignore) {}

            if (loginButton != null) {
                loginButton.setOnAction(e -> {
                    String username = usernameField.getText();
                    String password = passwordField.getText();
                    if (username.isEmpty() || password.isEmpty()) {
                        showAlert("Please fill all fields.");
                        return;
                    }
                    authenticateUser(username, password);
                });
            }

            // Forgot password logic
                            // Set logo image in navbar
                            ImageView logoImageView = (ImageView) loginScene.lookup("#logoImageView");
                            if (logoImageView != null) {
                                logoImageView.setImage(new Image(getClass().getResource("/assets/logo.png").toExternalForm()));
                            }
            if (forgotPasswordButton != null) {
                forgotPasswordButton.setOnAction(e -> {
                    String username = usernameField.getText();
                    if (username == null || username.trim().isEmpty()) {
                        showAlert("Please enter a username before resetting the password.");
                        return;
                    }
                    // Lookup user and role from DB instead of relying on hardcoded names
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String q = "SELECT id, role FROM Users WHERE username = ?";
                        PreparedStatement ps = conn.prepareStatement(q);
                        ps.setString(1, username);
                        ResultSet rs = ps.executeQuery();
                        if (!rs.next()) {
                            showAlert("No user found with that username.");
                            return;
                        }
                        String role = rs.getString("role");
                        String userId = rs.getString("id");
                        if ("admin".equalsIgnoreCase(role)) {
                            showAlert("Admin does not have the 'Forgot Password?' feature.");
                            return;
                        } else if ("cashier".equalsIgnoreCase(role)) {
                            TextInputDialog dialog = new TextInputDialog();
                            dialog.setTitle("Reset Password");
                            dialog.setHeaderText("Reset Cashier Password");
                            dialog.setContentText("Enter Cashier ID:");
                            dialog.showAndWait().ifPresent(cashierId -> {
                                // require the entered cashier ID to match the DB record for this username
                                if (cashierId != null && cashierId.equals(userId) && cashierId.matches("C-\\d{4}")) {
                                    TextInputDialog newPassDialog = new TextInputDialog();
                                    newPassDialog.setTitle("Set New Password");
                                    newPassDialog.setHeaderText("Enter New Password for Cashier");
                                    newPassDialog.setContentText("New Password:");
                                    newPassDialog.showAndWait().ifPresent(newPass -> {
                                        // Update password in DB
                                        try (Connection connection = DatabaseConnection.getConnection()) {
                                            String update = "UPDATE Users SET password = ? WHERE username = ? AND role = 'cashier'";
                                            PreparedStatement stmt = connection.prepareStatement(update);
                                            stmt.setString(1, newPass);
                                            stmt.setString(2, username);
                                            int rows = stmt.executeUpdate();
                                            if (rows > 0) {
                                                showAlert("Password reset successfully!");
                                            } else {
                                                showAlert("Failed to reset password. Please try again.");
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            showAlert("Database error. Please try again.");
                                        }
                                    });
                                } else {
                                    showAlert("Invalid Cashier ID. It must match the registered ID for this user and be in format C-4440.");
                                }
                            });
                        } else {
                            showAlert("Forgot password feature is not available for role: " + role);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Database error. Please try again.");
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Show loading animation before login page
    private void showLoadingAndLogin(Stage primaryStage) {
        StackPane loadingPane = new StackPane();
        loadingPane.setStyle("-fx-background-color: #f4f4f9;");
        javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
    Label loadingLabel = new Label("Loading...");
        loadingLabel.setFont(Font.font("Segoe UI", 18));
        VBox box = new VBox(20, pi, loadingLabel);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        loadingPane.getChildren().add(box);
        Scene loadingScene = new Scene(loadingPane, 420, 540);
        primaryStage.setScene(loadingScene);
        // Simulate loading
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> showLoginPage(primaryStage));
        }).start();
    }

    private void showDashboard(Stage primaryStage) {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(30, 10, 30, 10));
        sidebar.setStyle("-fx-background-color: #232946;");
        sidebar.setPrefWidth(200);

        Button btnBilling = createSidebarButton("Billing");
        Button btnInventory = createSidebarButton("Inventory");
        Button btnBarcode = createSidebarButton("Barcode");
        Button btnLoyalty = createSidebarButton("Loyalty Points");
        Button btnReports = createSidebarButton("Reports");
        Button btnSettings = createSidebarButton("Settings");

        sidebar.getChildren().addAll(
            btnBilling, btnInventory, btnBarcode, btnLoyalty, btnReports, btnSettings
        );

        StackPane mainContent = new StackPane();
        mainContent.setStyle("-fx-background-color: #f4f4f9;");
        Label welcomeLabel = new Label("Welcome to Supermarket System");
        welcomeLabel.setFont(Font.font("Segoe UI", 28));
        welcomeLabel.setTextFill(Color.web("#232946"));
        mainContent.getChildren().add(welcomeLabel);

        btnBilling.setOnAction(e -> setMainContent(mainContent, "Billing Section"));
        btnInventory.setOnAction(e -> setMainContent(mainContent, "Inventory Section"));
        btnBarcode.setOnAction(e -> setMainContent(mainContent, "Barcode Section"));
        btnLoyalty.setOnAction(e -> setMainContent(mainContent, "Loyalty Points Section"));
        btnReports.setOnAction(e -> setMainContent(mainContent, "Reports Section"));
        btnSettings.setOnAction(e -> setMainContent(mainContent, "Settings Section"));

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mainContent);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("Supermarket Billing & Inventory System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(180);
        btn.setFont(Font.font("Segoe UI", 16));
        btn.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-background-radius: 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-background-radius: 8;"));
        return btn;
    }

    // Helper to update main content area
    private void setMainContent(StackPane mainContent, String sectionTitle) {
        mainContent.getChildren().clear();
        Label sectionLabel = new Label(sectionTitle);
        sectionLabel.setFont(Font.font("Segoe UI", 24));
        sectionLabel.setTextFill(Color.web("#232946"));
        mainContent.getChildren().add(sectionLabel);
    }

    private void authenticateUser(String username, String password) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String role = resultSet.getString("role");
                // expose authenticated user id and name to controllers via system properties
                try { String id = resultSet.getString("id"); if (id != null) System.setProperty("cashier.id", id); } catch (Exception ignore) {}
                System.setProperty("cashier.name", username);
                showLoadingAndDashboardAfterAuth(primaryStage, username, role);
            } else {
                showAlert("Invalid credentials. Try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database connection error.");
        }
    }

    private void showAdminDashboard(Stage primaryStage, String username, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminDashboard.fxml"));
            Parent adminRoot = loader.load();
            Scene adminScene = new Scene(adminRoot, 1280, 720);
            adminScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setTitle("Admin Dashboard - QuickMart");
            primaryStage.setScene(adminScene);
            primaryStage.setMaximized(true); // Maximized windowed mode
            primaryStage.show();

            // Set profile photo, username, and role in the navbar
            controller.AdminDashboardController controller = loader.getController();
            controller.setUserDetails(username, role);
            controller.setLogoutHandler(() -> showLoginPage(primaryStage));
            // start at first admin page
            try { controller.getClass().getMethod("navigateAdminPageByIndex", int.class).invoke(controller, 0); } catch (Exception ignore) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCashierDashboard(Stage primaryStage, String username, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CashierDashboard.fxml"));
            Parent cashierRoot = loader.load();
            Scene cashierScene = new Scene(cashierRoot);
            cashierScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setTitle("Cashier Dashboard - QuickMart");
            primaryStage.setScene(cashierScene);
            primaryStage.show();
            // You can add more logic here for cashier dashboard
                try {
                controller.CashierDashboardController cctrl = loader.getController();
                // expose cashier username so controller can use it for payments/records
                try { cctrl.getClass().getMethod("setCashierName", String.class).invoke(cctrl, username); } catch (Exception ignore) {}
                // set logout handler so cashier logout redirects via App
                try { cctrl.getClass().getMethod("setLogoutHandler", Runnable.class).invoke(cctrl, (Runnable) (() -> showLoginPage(primaryStage))); } catch (Exception ignore) {}
            } catch (Exception ignore) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Show alert dialog
    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showLoadingAndDashboardAfterAuth(Stage primaryStage, String username, String role) {
        StackPane loadingPane = new StackPane();
        loadingPane.setStyle("-fx-background-color: #f4f4f9;");
        javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
    Label loadingLabel = new Label("Logging in...");
        loadingLabel.setFont(Font.font("Segoe UI", 18));
        VBox box = new VBox(20, pi, loadingLabel);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        loadingPane.getChildren().add(box);
        Scene loadingScene = new Scene(loadingPane, 420, 540);
        primaryStage.setScene(loadingScene);
        // Simulate loading
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                if ("admin".equals(role)) {
                    showAdminDashboard(primaryStage, username, role);
                } else if ("cashier".equals(role)) {
                    showCashierDashboard(primaryStage, username, role);
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}