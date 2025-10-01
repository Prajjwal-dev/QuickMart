package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

public class CashierManagementController {

    @FXML
    private TableView<User> cashierTable;

    @FXML
    private TableColumn<User, String> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colPassword;

    @FXML
    private TableColumn<User, Void> colActions;
    @FXML
    private TextField searchField;

    @FXML
    private Button addCashierBtn;

    @FXML
    private Button updateCashierBtn;

    @FXML
    private Button deleteCashierBtn;

    @FXML
    private StackPane mainContent;

    @FXML
    private void initialize() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (mainContent != null && mainContent.getScene() != null) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) mainContent.getScene().getWindow();
                    stage.setMaximized(true);
                } else if (cashierTable != null && cashierTable.getScene() != null) {
                    // fallback if mainContent isn't present in this FXML
                    javafx.stage.Stage stage = (javafx.stage.Stage) cashierTable.getScene().getWindow();
                    stage.setMaximized(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Actions cell factory
        colActions.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button upd = new Button("Update");
            private final Button del = new Button("Delete");
            private final HBox box = new HBox(8, upd, del);
            {
                upd.setStyle("-fx-background-color: linear-gradient(to right,#7c4dff,#b388ff); -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 12; -fx-background-radius: 6;");
                del.setStyle("-fx-background-color: linear-gradient(to right,#ff6b6b,#ff8b8b); -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 12; -fx-background-radius: 6;");
                upd.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showUpdateDialog(u);
                });
                del.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    // confirm deletion
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    a.setHeaderText(null);
                    a.setContentText("Delete cashier '" + u.getUsername() + "'? This action cannot be undone.");
                    java.util.Optional<javafx.scene.control.ButtonType> res = a.showAndWait();
                    if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) deleteUser(u);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadUsers();
        updateTable(users);

        // suggestions for search
        javafx.scene.control.ContextMenu suggestions = new javafx.scene.control.ContextMenu();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                suggestions.getItems().clear();
                if (n == null || n.isEmpty()) { suggestions.hide(); updateTable(users); return; }
                for (User u : users) {
                    if (u.getUsername().toLowerCase().contains(n.toLowerCase())) {
                        javafx.scene.control.MenuItem it = new javafx.scene.control.MenuItem(u.getUsername());
                        it.setOnAction(ev -> { searchField.setText(u.getUsername()); updateTable(java.util.Collections.singletonList(u)); suggestions.hide(); });
                        suggestions.getItems().add(it);
                    }
                }
                if (!suggestions.getItems().isEmpty()) {
                    javafx.geometry.Bounds b = searchField.localToScreen(searchField.getBoundsInLocal());
                    suggestions.show(searchField, b.getMinX(), b.getMaxY());
                } else suggestions.hide();
            });
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                if (newV == null || newV.isEmpty()) updateTable(users);
                else {
                    List<User> filtered = new ArrayList<>();
                    for (User u : users) if (u.getUsername().toLowerCase().contains(newV.toLowerCase())) filtered.add(u);
                    updateTable(filtered);
                }
            });
        }
        if (addCashierBtn != null) {
            addCashierBtn.setOnAction(e -> showAddCashierDialog());
        }

    cashierTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
    // show friendly placeholder when there are no cashiers
    cashierTable.setPlaceholder(new javafx.scene.control.Label("No cashiers available"));
        colId.setMaxWidth(120);
        colUsername.setMaxWidth(400);
        colRole.setMaxWidth(150);
        // actions column will take remaining space
        colActions.setMaxWidth(200);
    }

    private List<User> users = new ArrayList<>();

    private void loadUsers() {
        users.clear();
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT id, username, password, role FROM users WHERE role = 'cashier'");
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getString("id"));
                u.setUsername(rs.getString("username"));
                // load password if present (may be null)
                try { u.setPassword(rs.getString("password")); } catch (Exception ignore) { u.setPassword(""); }
                u.setRole(rs.getString("role"));
                users.add(u);
            }
            System.out.println("Loaded cashiers: " + users.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateTable(List<User> list) {
        if (cashierTable != null) cashierTable.getItems().setAll(list);
    }

    private void showUpdateDialog(User u) {
        // Simple dialog to update username/role
        javafx.stage.Stage s = new javafx.stage.Stage();
        javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(10);
        javafx.scene.control.TextField username = new javafx.scene.control.TextField(u.getUsername());
    // Role cannot be changed to admin here; enforce cashier only
    javafx.scene.control.Label roleLabel = new javafx.scene.control.Label("Role: cashier");
    javafx.scene.control.PasswordField pwdField = new javafx.scene.control.PasswordField(); pwdField.setPromptText("New password (leave blank to keep)");
        javafx.scene.control.Button ok = new javafx.scene.control.Button("Save");
        ok.setOnAction(ev -> {
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                if (pwdField.getText() != null && !pwdField.getText().isEmpty()) {
                    java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE users SET username=?, password=? WHERE id=?");
                    ps.setString(1, username.getText()); ps.setString(2, pwdField.getText()); ps.setString(3, u.getId());
                    int r = ps.executeUpdate();
                    if (r>0) { loadUsers(); updateTable(users); s.close(); }
                } else {
                    java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE users SET username=? WHERE id=?");
                    ps.setString(1, username.getText()); ps.setString(2, u.getId());
                    int r = ps.executeUpdate();
                    if (r>0) { loadUsers(); updateTable(users); s.close(); }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    v.getChildren().addAll(new javafx.scene.control.Label("Username"), username, new javafx.scene.control.Label("Password"), pwdField, roleLabel, ok);
        s.setScene(new javafx.scene.Scene(v, 320, 240)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    private void deleteUser(User u) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?");
            ps.setString(1, u.getId());
            int r = ps.executeUpdate();
            if (r>0) { loadUsers(); updateTable(users); }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showAddCashierDialog() {
        javafx.stage.Stage s = new javafx.stage.Stage();
        javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(10);
        javafx.scene.control.TextField username = new javafx.scene.control.TextField(); username.setPromptText("Username");
        javafx.scene.control.PasswordField pwd = new javafx.scene.control.PasswordField(); pwd.setPromptText("Password");
        javafx.scene.control.Button ok = new javafx.scene.control.Button("Create Cashier");
        ok.setOnAction(ev -> {
            String un = username.getText(); String pw = pwd.getText();
            if (un == null || un.isEmpty() || pw == null || pw.isEmpty()) { return; }
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                // generate id C-XXXX
                java.sql.Statement st = conn.createStatement();
                java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) as c FROM users WHERE id LIKE 'C-%'");
                int count = 0; if (rs.next()) count = rs.getInt("c");
                String id = String.format("C-%04d", count+1);
                java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, 'cashier')");
                ps.setString(1, id); ps.setString(2, un); ps.setString(3, pw);
                int r = ps.executeUpdate();
                if (r>0) { loadUsers(); updateTable(users); s.close(); }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        v.getChildren().addAll(new javafx.scene.control.Label("Username"), username, new javafx.scene.control.Label("Password"), pwd, ok);
        s.setScene(new javafx.scene.Scene(v, 320, 220)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    // TODO: Implement methods for Add, Update, Delete, and Search functionalities

    public static class User {
        private String id;
        private String username;
    private String password;
        private String role;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    }
}
