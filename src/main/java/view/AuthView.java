package view;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class AuthView {
    private TextField usernameField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private Button loginButton = new Button("Login");
    private Button forgotPasswordButton = new Button("Forgot Password?");
    private VBox formBox = new VBox(15);
    private ImageView profileImageView = new ImageView();
    private ImageView logoView = new ImageView();

    public AuthView() {
        // Profile image placeholder (circle)
        profileImageView.setFitHeight(80);
        profileImageView.setFitWidth(80);
        profileImageView.setPreserveRatio(true);
        Circle clip = new Circle(40, 40, 40);
        profileImageView.setClip(clip);
        profileImageView.setStyle("-fx-background-color: #e0e0e0; -fx-border-radius: 50%;");
        // Default profile image (user can replace)
        try {
            profileImageView.setImage(new Image(getClass().getResource("/assets/profile.png").toExternalForm()));
        } catch (Exception e) {
            // fallback: blank
        }

        // Logo at top
        logoView.setFitHeight(60);
        logoView.setPreserveRatio(true);
        try {
            logoView.setImage(new Image(getClass().getResource("/assets/logo.png").toExternalForm()));
        } catch (Exception e) {
            // fallback: blank
        }
    }

    public Scene getScene() {
        // Welcome text
        Label welcome = new Label("Welcome to QuickMart");
        welcome.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        welcome.setTextFill(Color.web("#232946"));
        welcome.setPadding(new Insets(0, 0, 10, 0));

        // Username
        usernameField.setPromptText("User ID");
        usernameField.setPrefWidth(240);

        // Password
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(240);

        // Login button
        loginButton.setPrefWidth(240);
        loginButton.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-background-radius: 8;");
        loginButton.setFont(Font.font("Segoe UI", 16));

        // Forgot password button
        forgotPasswordButton.setPrefWidth(240);
        forgotPasswordButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b8c1ec; -fx-underline: true;");
        forgotPasswordButton.setFont(Font.font("Segoe UI", 13));

        // Form layout
        formBox.getChildren().setAll(profileImageView, usernameField, passwordField, loginButton, forgotPasswordButton);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(30, 30, 30, 30));
        formBox.setStyle("-fx-background-color: #fff; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, #b8c1ec, 10, 0, 0, 2);");

        VBox mainBox = new VBox(20, logoView, welcome, formBox);
        mainBox.setAlignment(Pos.TOP_CENTER);
        mainBox.setPadding(new Insets(40, 0, 0, 0));
        mainBox.setStyle("-fx-background-color: #f4f4f9;");

        StackPane root = new StackPane(mainBox);

        // Animation
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), mainBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return new Scene(root, 420, 540);
    }

    public void setProfileImage(String imagePath) {
    profileImageView.setImage(new Image(getClass().getResource(imagePath).toExternalForm()));
    }

    public TextField getUsernameField() { return usernameField; }
    public PasswordField getPasswordField() { return passwordField; }
    public Button getLoginButton() { return loginButton; }
    public Button getForgotPasswordButton() { return forgotPasswordButton; }
}