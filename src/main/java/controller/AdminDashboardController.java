package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
// imports cleaned
// imports cleaned
import javafx.stage.Stage;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardController {
    @FXML
    private ImageView profileImageView;
    @FXML
    private ImageView logoImageView;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button hamburgerButton;
    @FXML
    private VBox sidebar;
    @FXML
    private Button inventoryBtn;
    @FXML
    private Button settingsBtn;
    @FXML
    private Button salesReportBtn;
    @FXML
    private Button cashierBtn;
    @FXML
    private Button customerBtn;
    @FXML
    private StackPane mainContent;

    private Runnable logoutHandler;
    // centralized admin page state
    private List<String> adminOrder = Arrays.asList("Inventory", "CustomerManagement", "CashierManagement");
    @SuppressWarnings("unused")
    private int currentAdminIndex = -1;
    private Button adminPrevBtn, adminNextBtn;

    // sales id list used for invoice navigation (prev/next)
    private java.util.List<Long> salesIds = new java.util.ArrayList<>();
    private int currentSalesIndex = -1;

    // hold low stock dialog so we can close it when settings change
    private javafx.scene.control.Dialog<Void> lowStockDialogRef = null;

    // Dashboard KPI nodes are created programmatically in showDashboardHome()

    public void setUserDetails(String username, String role) {
        if (logoImageView != null) {
            logoImageView.setImage(new Image(getClass().getResource("/assets/logo.png").toExternalForm()));
        }
        if (profileImageView != null) {
            profileImageView.setImage(new Image(getClass().getResource("/assets/profile-icon-design-free-vector.jpg").toExternalForm()));
        }
        usernameLabel.setText(username);
        roleLabel.setText("Role: " + role);
    }

    // Helper method to apply styles to all buttons recursively inside a parent
    private void applyButtonStyles(javafx.scene.Parent parent, String style, String hoverStyle) {
        if (parent == null) return;
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            try {
                if (node instanceof javafx.scene.control.Button) {
                    javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                    button.setStyle(style);
                    // Prevent OS/FX default focus blue outline appearing on click
                    button.setFocusTraversable(false);
                    button.setOnMouseEntered(e -> button.setStyle(style + hoverStyle));
                    button.setOnMouseExited(e -> button.setStyle(style));
                } else if (node instanceof javafx.scene.Parent) {
                    applyButtonStyles((javafx.scene.Parent) node, style, hoverStyle);
                }
            } catch (Exception ignored) {}
        }
    }

    // Ensure table view has a consistent white background and readable text
    private void applyTableStyling(javafx.scene.control.TableView<?> table) {
        if (table == null) return;
    // enforce white background, dark text and neutral selection colours
    table.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #333333; -fx-selection-bar: #e8e8e8; -fx-selection-bar-non-focused: #e8e8e8; -fx-table-cell-border-color: transparent;");
    // avoid focus ring / blue outline when clicking
    table.setFocusTraversable(false);
    }

    // Centralized pager used on admin pages
    private HBox createAdminPager() {
    HBox pager = new HBox(12);
    pager.setStyle("-fx-alignment: center; -fx-padding: 12;");
    // Admin-level pager (prev/next) intentionally hidden - navigation is done via sidebar
    return pager;
    }

    @SuppressWarnings("unused")
    private void navigateAdminPageByIndex(int idx) {
        if (idx < 0 || idx >= adminOrder.size()) return;
        String key = adminOrder.get(idx);
        switch (key) {
            case "Inventory": showInventoryManagement(); break;
            case "CustomerManagement": showCustomerManagement(); break;
            case "CashierManagement": showCashierManagement(); break;
        }
        // update pager buttons
        if (adminPrevBtn != null) adminPrevBtn.setDisable(idx<=0);
        if (adminNextBtn != null) adminNextBtn.setDisable(idx>=adminOrder.size()-1);
    }

    // Show loading animation on logout
    // Removed @Override annotation since no supertype method exists
    public void setLogoutHandler(Runnable handler) {
        this.logoutHandler = () -> {
            StackPane loadingPane = new StackPane();
            loadingPane.setStyle("-fx-background-color: #f4f4f9;");
            javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
            Label loadingLabel = new Label("Logging out...");
            loadingLabel.setFont(javafx.scene.text.Font.font("Segoe UI", 18));
            VBox box = new VBox(20, pi, loadingLabel);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            loadingPane.getChildren().add(box);
            javafx.scene.Scene loadingScene = new javafx.scene.Scene(loadingPane, 420, 540);
            javafx.stage.Stage stage = (javafx.stage.Stage) mainContent.getScene().getWindow();
            stage.setScene(loadingScene);
            new Thread(() -> {
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(handler);
            }).start();
        };
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) mainContent.getScene().getWindow();
            stage.setMaximized(true);
        });

        // Apply global style fixes on the FX thread to ensure nodes are present
        Platform.runLater(() -> {
            try {
                String buttonStyle = "-fx-background-color: #f5f5f5; -fx-text-fill: #333333; -fx-border-color: #dddddd; -fx-border-width: 1; -fx-border-radius: 4;";
                String buttonHoverStyle = "-fx-background-color: #e8e8e8;";
                if (mainContent != null) applyButtonStyles(mainContent, buttonStyle, buttonHoverStyle);
                if (sidebar != null) applyButtonStyles(sidebar, buttonStyle, buttonHoverStyle);
                // Explicitly style known sidebar buttons so they are visible on white background
                // Assign distinctive, attractive colors for each primary sidebar button
                try { if (inventoryBtn != null) { inventoryBtn.setStyle("-fx-background-color: linear-gradient(#8fe3a7, #67c58f); -fx-text-fill: #072227; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;"); inventoryBtn.setOnMouseEntered(e->inventoryBtn.setStyle("-fx-background-color: linear-gradient(#a7ecd0, #7fd69e); -fx-text-fill: #072227; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); inventoryBtn.setOnMouseExited(e->inventoryBtn.setStyle("-fx-background-color: linear-gradient(#8fe3a7, #67c58f); -fx-text-fill: #072227; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); inventoryBtn.setFocusTraversable(false); } } catch (Exception ignored) {}
                try { if (settingsBtn != null) { settingsBtn.setStyle("-fx-background-color: linear-gradient(#ffd9a6, #ffb86b); -fx-text-fill: #3b2f2f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;"); settingsBtn.setOnMouseEntered(e->settingsBtn.setStyle("-fx-background-color: linear-gradient(#ffe9bf, #ffc88b); -fx-text-fill: #3b2f2f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); settingsBtn.setOnMouseExited(e->settingsBtn.setStyle("-fx-background-color: linear-gradient(#ffd9a6, #ffb86b); -fx-text-fill: #3b2f2f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); settingsBtn.setFocusTraversable(false); } } catch (Exception ignored) {}
                try { if (salesReportBtn != null) { salesReportBtn.setStyle("-fx-background-color: linear-gradient(#9fc4ff, #6fa8ff); -fx-text-fill: #062a4f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;"); salesReportBtn.setOnMouseEntered(e->salesReportBtn.setStyle("-fx-background-color: linear-gradient(#bcdcff, #8fb8ff); -fx-text-fill: #062a4f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); salesReportBtn.setOnMouseExited(e->salesReportBtn.setStyle("-fx-background-color: linear-gradient(#9fc4ff, #6fa8ff); -fx-text-fill: #062a4f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); salesReportBtn.setFocusTraversable(false); } } catch (Exception ignored) {}
                try { if (cashierBtn != null) { cashierBtn.setStyle("-fx-background-color: linear-gradient(#f6a6ff, #d57bff); -fx-text-fill: #2b0636; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;"); cashierBtn.setOnMouseEntered(e->cashierBtn.setStyle("-fx-background-color: linear-gradient(#f9bdfb, #e69af6); -fx-text-fill: #2b0636; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); cashierBtn.setOnMouseExited(e->cashierBtn.setStyle("-fx-background-color: linear-gradient(#f6a6ff, #d57bff); -fx-text-fill: #2b0636; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); cashierBtn.setFocusTraversable(false); } } catch (Exception ignored) {}
                try { if (customerBtn != null) { customerBtn.setStyle("-fx-background-color: linear-gradient(#ffd1d1, #ff9b9b); -fx-text-fill: #3b0a0a; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;"); customerBtn.setOnMouseEntered(e->customerBtn.setStyle("-fx-background-color: linear-gradient(#ffe6e6, #ffbebe); -fx-text-fill: #3b0a0a; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); customerBtn.setOnMouseExited(e->customerBtn.setStyle("-fx-background-color: linear-gradient(#ffd1d1, #ff9b9b); -fx-text-fill: #3b0a0a; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;")); customerBtn.setFocusTraversable(false); customerBtn.setText("Customer Management"); } } catch (Exception ignored) {}
                // Optionally hide cashier management tile in admin (if we don't want it visible)
                try { if (cashierBtn != null) { cashierBtn.setVisible(false); cashierBtn.setManaged(false); } } catch (Exception ignored) {}

                // Active button highlight helper: add a style class to the active sidebar button and remove from others
                Runnable setActive = () -> {
                    try {
                        if (sidebar == null) return;
                        for (javafx.scene.Node n : sidebar.getChildren()) {
                            if (n instanceof Button) n.getStyleClass().removeAll("active-sidebar-btn");
                        }
                        // if salesReportBtn present, set it active initially
                        if (salesReportBtn != null) salesReportBtn.getStyleClass().add("active-sidebar-btn");
                    } catch (Exception ignored) {}
                };
                setActive.run();
                // Ensure the sidebar buttons use a palette of distinct, readable colors
                try {
                    if (sidebar != null) {
                        java.util.List<String> palette = java.util.Arrays.asList(
                            // green
                            "-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: #072227; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // yellow
                            "-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill: #2b2b2b; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // blue
                            "-fx-background-color: linear-gradient(#9fc4ff,#6fa8ff); -fx-text-fill: #062a4f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // purple
                            "-fx-background-color: linear-gradient(#f6a6ff,#d57bff); -fx-text-fill: #2b0636; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // red/pink
                            "-fx-background-color: linear-gradient(#ffd1d1,#ff9b9b); -fx-text-fill: #3b0a0a; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // amber
                            "-fx-background-color: linear-gradient(#ffd9a6,#ffb86b); -fx-text-fill: #3b2f2f; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // teal
                            "-fx-background-color: linear-gradient(#67e3c0,#3fbf9a); -fx-text-fill: #052524; -fx-padding: 10 14; -fx-font-weight: 600; -fx-background-radius: 8;",
                            // dark green (guidelines)
                            "-fx-background-color: linear-gradient(#6fdc9a,#3fb16d); -fx-text-fill:#0d2b20; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;",
                            // bright yellow (privacy)
                            "-fx-background-color: linear-gradient(#fff889,#ffe76a); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;",
                            // purple (about)
                            "-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill: #ffffff; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;",
                            // exit (red)
                            "-fx-background-color: linear-gradient(#ff6b6b,#ff8b8b); -fx-text-fill: #ffffff; -fx-padding:8 12; -fx-background-radius:8"
                        );

                        java.util.List<javafx.scene.control.Button> ordered = new java.util.ArrayList<>();
                        javafx.scene.control.Button dbb = (javafx.scene.control.Button) sidebar.lookup("#dashboardBtn"); if (dbb != null) ordered.add(dbb);
                        if (inventoryBtn != null) ordered.add(inventoryBtn);
                        if (salesReportBtn != null) ordered.add(salesReportBtn);
                        if (cashierBtn != null) ordered.add(cashierBtn);
                        if (customerBtn != null) ordered.add(customerBtn);
                        if (settingsBtn != null) ordered.add(settingsBtn);
                        javafx.scene.control.Button um = (javafx.scene.control.Button) sidebar.lookup("#userMgmtBtn"); if (um != null) ordered.add(um);
                        javafx.scene.control.Button gbtn = (javafx.scene.control.Button) sidebar.lookup("#guidelineBtn"); if (gbtn != null) ordered.add(gbtn);
                        javafx.scene.control.Button pbtn = (javafx.scene.control.Button) sidebar.lookup("#privacyBtn"); if (pbtn != null) ordered.add(pbtn);
                        javafx.scene.control.Button abtn = (javafx.scene.control.Button) sidebar.lookup("#aboutBtn"); if (abtn != null) ordered.add(abtn);
                        javafx.scene.control.Button exbtn = (javafx.scene.control.Button) sidebar.lookup("#exitBtn"); if (exbtn != null) ordered.add(exbtn);

                        int i = 0;
                        for (javafx.scene.control.Button b : ordered) {
                            try {
                                String style = palette.get(i % palette.size());
                                b.setStyle(style);
                                b.setFocusTraversable(false);
                                // subtle hover: slightly lighter gradient (reuse same style for safety)
                                final String s = style;
                                b.setOnMouseEntered(ev -> b.setStyle(s));
                                b.setOnMouseExited(ev -> b.setStyle(s));
                            } catch (Exception ignored2) {}
                            i++;
                        }
                    }
                } catch (Exception ignored) {}
                // style logout and hamburger to be subtle but visible
                try { if (logoutButton != null) { logoutButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #b23a48; -fx-border-color: transparent; -fx-padding: 8; -fx-background-radius: 8;"); logoutButton.setOnMouseEntered(e->logoutButton.setStyle("-fx-background-color: #fff5f6; -fx-text-fill: #b23a48;")); logoutButton.setOnMouseExited(e->logoutButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #b23a48;")); logoutButton.setFocusTraversable(false); } } catch (Exception ignored) {}
                try { if (hamburgerButton != null) { hamburgerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-padding: 8; -fx-background-radius: 6; -fx-font-size: 18px; -fx-font-weight: 700;"); hamburgerButton.setOnMouseEntered(e->hamburgerButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-font-size: 18px; -fx-font-weight: 700;")); hamburgerButton.setOnMouseExited(e->hamburgerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-font-size: 18px; -fx-font-weight: 700;")); hamburgerButton.setFocusTraversable(false); } } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });

        settingsBtn.setOnAction(e -> loadSettingsPage());
        salesReportBtn.setOnAction(e -> showSalesReport());

        // Sidebar toggle logic
        sidebar.setVisible(false);
        sidebar.setManaged(false);
        hamburgerButton.setOnAction(e -> {
            boolean show = !sidebar.isVisible();
            sidebar.setVisible(show);
            sidebar.setManaged(show);
        });

        // Logout button logic
        logoutButton.setOnAction(e -> {
            if (logoutHandler != null) {
                logoutHandler.run();
            }
        });

        // Sidebar button actions
        try {
            javafx.scene.control.Button db = (javafx.scene.control.Button) sidebar.lookup("#dashboardBtn");
            if (db != null) {
                db.setOnAction(e -> showDashboardHome());
            }
        } catch (Exception ignored) {}
        // Ensure exitBtn is the last item in the sidebar for consistent placement
        try {
            if (sidebar != null) {
                javafx.scene.control.Button exBtn = (javafx.scene.control.Button) sidebar.lookup("#exitBtn");
                if (exBtn != null) {
                    javafx.collections.ObservableList<javafx.scene.Node> nodes = sidebar.getChildren();
                    nodes.remove(exBtn);
                    nodes.add(exBtn);
                }
            }
        } catch (Exception ignored) {}
        try {
            javafx.scene.control.Button ex = (javafx.scene.control.Button) sidebar.lookup("#exitBtn");
            if (ex != null) {
                ex.setOnAction(e -> {
                    Stage s = (Stage) mainContent.getScene().getWindow();
                    s.close();
                });
            }
        } catch (Exception ignored) {}
        if (inventoryBtn != null) {
            inventoryBtn.setOnAction(e -> {
                try { setActiveSidebarButton(inventoryBtn); } catch (Exception ignored) {}
                showInventoryManagement();
            });
        }
        // Add User Management, Guidelines / Privacy / About buttons to sidebar if not present
        try {
            if (sidebar != null) {
                // User Management (restore previous button if missing)
                Button existingUserMgmt = (Button) sidebar.lookup("#userMgmtBtn");
                // Ensure exit button remains last: remove it before mutating children then re-add at the end
                javafx.scene.control.Button exitNode = (javafx.scene.control.Button) sidebar.lookup("#exitBtn");
                try { if (exitNode != null) sidebar.getChildren().remove(exitNode); } catch (Exception ignored) {}

                if (existingUserMgmt == null) {
                    Button userMgmtBtn = new Button("User Management"); userMgmtBtn.setId("userMgmtBtn");
                    userMgmtBtn.setStyle("-fx-background-color: linear-gradient(#d1ffd8,#a6f5b6); -fx-text-fill:#064; -fx-padding:8 12; -fx-background-radius:8");
                    final Button umBtn = userMgmtBtn;
                    umBtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(umBtn); } catch (Exception ignored) {} showUserManagement(); });
                    // insert user management directly under Customer Management (if present) for logical grouping
                    int insertIdx = sidebar.getChildren().indexOf(customerBtn);
                    if (insertIdx < 0) sidebar.getChildren().add(umBtn); else sidebar.getChildren().add(insertIdx + 1, umBtn);
                } else {
                    final Button umBtn = existingUserMgmt;
                    umBtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(umBtn); } catch (Exception ignored) {} showUserManagement(); });
                }
                // Guidelines button
                Button existingGuideline = (Button) sidebar.lookup("#guidelineBtn");
                    if (existingGuideline == null) {
                    Button guidelineBtn = new Button("Guidelines");
                    guidelineBtn.setId("guidelineBtn");
                    // Guidelines - use a strong green gradient; use dark text for better contrast on Windows
                    guidelineBtn.setStyle("-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10; -fx-border-color: #3b923b; -fx-border-radius:10");
                    final Button gbtn = guidelineBtn;
                    gbtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(gbtn); } catch (Exception ignored) {} showGuidelinesPage(); });
                    // ensure hover and focus behavior so color stays visible on Windows
                    gbtn.setFocusTraversable(false);
                    gbtn.setOnMouseEntered(e->gbtn.setStyle("-fx-background-color: linear-gradient(#a7ecd0, #7fd69e); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    gbtn.setOnMouseExited(e->gbtn.setStyle("-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    sidebar.getChildren().add(gbtn);
                } else {
                    final Button gbtn = existingGuideline;
                    gbtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(gbtn); } catch (Exception ignored) {} showGuidelinesPage(); });
                    // ensure style is applied at runtime
                    try { existingGuideline.setStyle("-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;"); existingGuideline.setFocusTraversable(false); existingGuideline.setOnMouseEntered(e->existingGuideline.setStyle("-fx-background-color: linear-gradient(#a7ecd0, #7fd69e); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;")); existingGuideline.setOnMouseExited(e->existingGuideline.setStyle("-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: #222222; -fx-font-weight: bold; -fx-padding:10 14; -fx-background-radius:10;")); } catch (Exception ignored) {}
                }

                // Privacy button
                Button existingPrivacy = (Button) sidebar.lookup("#privacyBtn");
                if (existingPrivacy == null) {
                    Button privacyBtn = new Button("Privacy Policy"); privacyBtn.setId("privacyBtn");
                    // Privacy - use a clear yellow gradient but with dark text to avoid white-on-light contrast issues
                    privacyBtn.setStyle("-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill:#222222; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10; -fx-border-color:#d6b300; -fx-border-radius:10");
                    final Button pbtn = privacyBtn;
                    pbtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(pbtn); } catch (Exception ignored) {} showPrivacyPage(); });
                    pbtn.setFocusTraversable(false);
                    pbtn.setOnMouseEntered(e->pbtn.setStyle("-fx-background-color: linear-gradient(#fff889,#ffe76a); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    pbtn.setOnMouseExited(e->pbtn.setStyle("-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    sidebar.getChildren().add(pbtn);
                } else {
                    final Button pbtn = existingPrivacy;
                    pbtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(pbtn); } catch (Exception ignored) {} showPrivacyPage(); });
                    try { existingPrivacy.setStyle("-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"); existingPrivacy.setFocusTraversable(false); existingPrivacy.setOnMouseEntered(e->existingPrivacy.setStyle("-fx-background-color: linear-gradient(#fff889,#ffe76a); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;")); existingPrivacy.setOnMouseExited(e->existingPrivacy.setStyle("-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;")); } catch (Exception ignored) {}
                }

                // About button
                Button existingAbout = (Button) sidebar.lookup("#aboutBtn");
                if (existingAbout == null) {
                    Button aboutBtn = new Button("About Us"); aboutBtn.setId("aboutBtn");
                    // About - purple gradient; keep white text for good contrast on purple
                    aboutBtn.setStyle("-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill: #ffffff; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10; -fx-border-color:#8a63d6; -fx-border-radius:10");
                    final Button abtn = aboutBtn;
                    abtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(abtn); } catch (Exception ignored) {} showAboutPage(); });
                    abtn.setFocusTraversable(false);
                    abtn.setOnMouseEntered(e->abtn.setStyle("-fx-background-color: linear-gradient(#e6cffb,#a98df6); -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    abtn.setOnMouseExited(e->abtn.setStyle("-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"));
                    sidebar.getChildren().add(abtn);
                } else {
                    final Button abtn = existingAbout;
                    abtn.setOnAction(ev -> { setPosControlsVisible(false); try { setActiveSidebarButton(abtn); } catch (Exception ignored) {} showAboutPage(); });
                    try { existingAbout.setStyle("-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill: white; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;"); existingAbout.setFocusTraversable(false); existingAbout.setOnMouseEntered(e->existingAbout.setStyle("-fx-background-color: linear-gradient(#e6cffb,#a98df6); -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;")); existingAbout.setOnMouseExited(e->existingAbout.setStyle("-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 14; -fx-background-radius:10;")); } catch (Exception ignored) {}
                }
                // Re-add exit button as last and style it consistently
                try {
                    if (exitNode != null) {
                        sidebar.getChildren().remove(exitNode);
                        exitNode.setStyle("-fx-background-color: linear-gradient(#ff6b6b,#ff8b8b); -fx-text-fill: #ffffff; -fx-padding:8 12; -fx-background-radius:8");
                        exitNode.setFocusTraversable(false);
                        sidebar.getChildren().add(exitNode);
                    }
                } catch (Exception ignored) {}
                // sidebar footer intentionally omitted here; page footers are injected into each page via createScrollable()
            }
        } catch (Exception ignored) {}
        if (cashierBtn != null) {
            cashierBtn.setOnAction(e -> {
                try { setActiveSidebarButton(cashierBtn); } catch (Exception ignored) {}
                showCashierManagement();
            });
            // ensure cashier management tile visibility policy: visible in admin if desired
            try { cashierBtn.setVisible(true); cashierBtn.setManaged(true); } catch (Exception ignored) {}
        }
        if (customerBtn != null) {
            customerBtn.setOnAction(e -> {
                try { setActiveSidebarButton(customerBtn); } catch (Exception ignored) {}
                showCustomerManagement();
            });
            try { customerBtn.setVisible(true); customerBtn.setManaged(true); } catch (Exception ignored) {}
        }

        // move settings button to be after cashier button in the sidebar children order
        try {
            javafx.collections.ObservableList<javafx.scene.Node> nodes = sidebar.getChildren();
            if (nodes.contains(cashierBtn) && nodes.contains(settingsBtn)) {
                nodes.remove(settingsBtn);
                int idx = nodes.indexOf(cashierBtn);
                nodes.add(idx + 1, settingsBtn);
            }
        } catch (Exception ignored) {}

        // listen for low stock preference changes so we can close popup when toggled off
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(controller.SettingsController.class);
            prefs.addPreferenceChangeListener(evt -> {
                if ("lowStockEnabled".equals(evt.getKey())) {
                    boolean enabled = Boolean.parseBoolean(evt.getNewValue());
                    if (!enabled) {
                        try { if (lowStockDialogRef != null && lowStockDialogRef.isShowing()) lowStockDialogRef.close(); } catch (Exception ignore) {}
                    }
                }
            });
        } catch (Exception ignored) {}
    // Ensure export helpers are referenced so static analysis doesn't mark them unused
    touchExportHelpers();
    }

    private void showCustomerManagement() {
        // inventory-like styling and search suggestions
        VBox inventoryBox = new VBox(20);
        inventoryBox.setStyle("-fx-padding: 40 30 30 30; -fx-background-color: #fff; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, #b8c1ec, 10, 0, 0, 2);");
    inventoryBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

    

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search customers...");
        searchField.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 15; -fx-pref-width: 260;");
        Button searchBtn = new Button("🔍");
        searchBtn.setStyle("-fx-font-size: 16; -fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
        searchBox.getChildren().addAll(searchField, searchBtn);

        Button addCustomerBtn = new Button("Add Customer");
        addCustomerBtn.setStyle("-fx-background-color: #4258d0; -fx-text-fill: #fff; -fx-font-family: 'Poppins'; -fx-font-size: 15; -fx-background-radius: 8; -fx-padding: 8 24 8 24;");

    HBox topActions = new HBox(20, searchBox, addCustomerBtn);
    topActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    // export buttons (handlers assigned after table created)
    Button custCsv = new Button("Export CSV");
    Button custPdf = new Button("Export PDF");
    Button custXlsx = new Button("Export XLSX");
    // add a Refresh button (circular '⟲') to reload customers and reset the search box
    Button refreshCust = new Button("⟲");
    refreshCust.setStyle("-fx-font-size: 16; -fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
    refreshCust.setOnAction(ev -> { try { searchField.clear(); searchField.requestFocus(); } catch (Exception ignored) {} showCustomerManagement(); });
    topActions.getChildren().addAll(custCsv, custPdf, custXlsx, refreshCust);

        TableView<Customer> table = new TableView<>();
    applyTableStyling(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new javafx.scene.control.Label("No customers found"));

        TableColumn<Customer, String> cid = new TableColumn<>("Customer ID"); cid.setCellValueFactory(new PropertyValueFactory<>("cId"));
        TableColumn<Customer, String> cname = new TableColumn<>("Name"); cname.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        TableColumn<Customer, String> phone = new TableColumn<>("Phone"); phone.setCellValueFactory(new PropertyValueFactory<>("phoneNo"));
        TableColumn<Customer, Integer> points = new TableColumn<>("Loyalty Points"); points.setCellValueFactory(new PropertyValueFactory<>("loyaltyPoints"));
        TableColumn<Customer, Void> ops = new TableColumn<>("Actions");
        ops.setCellFactory(p -> new TableCell<Customer, Void>() {
            private final Button upd = new Button("Update");
            private final Button del = new Button("Delete");
            private final HBox bx = new HBox(8, upd, del);
            {
                upd.setStyle("-fx-background-color: #b8c1ec; -fx-text-fill: #232946;");
                del.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946;");
                upd.setOnAction(e -> showUpdateCustomerDialog(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                    a.setHeaderText(null);
                    a.setContentText("Delete customer '" + c.customerName + "' (" + c.cId + ")?");
                    a.showAndWait().ifPresent(bt -> { if (bt == ButtonType.OK) deleteCustomer(c, table); });
                });
            }

            @Override
            protected void updateItem(Void it, boolean empty) {
                super.updateItem(it, empty);
                setGraphic(empty ? null : bx);
            }
        });
    // avoid creating a generic varargs array (type-safety) by using an ObservableList
    javafx.collections.ObservableList<javafx.scene.control.TableColumn<Customer, ?>> custCols = javafx.collections.FXCollections.observableArrayList();
    custCols.add(cid); custCols.add(cname); custCols.add(phone); custCols.add(points); custCols.add(ops);
    table.getColumns().setAll(custCols);

        inventoryBox.getChildren().setAll(topActions, table);
    mainContent.getChildren().setAll(createScrollable(inventoryBox));

    // pager removed from this view; invoice will contain navigation when needed

        addCustomerBtn.setOnAction(ev -> showAddCustomerDialog(table));

        // load all customers
        java.util.List<Customer> all = new java.util.ArrayList<>();
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.Statement st = conn.createStatement();
            java.sql.ResultSet rs = st.executeQuery("SELECT * FROM customers");
            while (rs.next()) {
                Customer c = new Customer();
                c.cId = rs.getString("c_id");
                c.customerName = rs.getString("customer_name");
                c.phoneNo = rs.getString("phone_no");
                c.loyaltyPoints = rs.getInt("loyalty_points");
                all.add(c);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        table.getItems().setAll(all);

        // Assign handlers to export buttons now that table exists
        custCsv.setOnAction(ev -> exportCustomerTableToCsv(table, "customers.csv"));
        custPdf.setOnAction(ev -> {
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("Customer ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("c_id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Name"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("customer_name"))));
            TableColumn<java.util.Map<String,Object>, String> c = new TableColumn<>("Phone"); c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("phone_no"))));
            TableColumn<java.util.Map<String,Object>, String> d = new TableColumn<>("Loyalty Points"); d.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("loyalty_points"))));
            javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> custMapCols = javafx.collections.FXCollections.observableArrayList();
            custMapCols.add(a); custMapCols.add(b); custMapCols.add(c); custMapCols.add(d);
            m.getColumns().setAll(custMapCols);
            javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
            for (Customer cu : table.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>(); mm.put("c_id", cu.cId); mm.put("customer_name", cu.customerName); mm.put("phone_no", cu.phoneNo); mm.put("loyalty_points", cu.loyaltyPoints); rows.add(mm);
            }
            m.setItems(rows);
            exportTableToPdf(m, "customers.pdf");
        });
        custXlsx.setOnAction(ev -> {
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("Customer ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("c_id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Name"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("customer_name"))));
            TableColumn<java.util.Map<String,Object>, String> c = new TableColumn<>("Phone"); c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("phone_no"))));
            TableColumn<java.util.Map<String,Object>, String> d = new TableColumn<>("Loyalty Points"); d.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("loyalty_points"))));
            javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> custMapCols2 = javafx.collections.FXCollections.observableArrayList();
            custMapCols2.add(a); custMapCols2.add(b); custMapCols2.add(c); custMapCols2.add(d);
            m.getColumns().setAll(custMapCols2);
            javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
            for (Customer cu : table.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>(); mm.put("c_id", cu.cId); mm.put("customer_name", cu.customerName); mm.put("phone_no", cu.phoneNo); mm.put("loyalty_points", cu.loyaltyPoints); rows.add(mm);
            }
            m.setItems(rows);
            exportTableToXlsx(m, "customers.xlsx");
        });

        // search suggestions similar to inventory
        javafx.scene.control.ContextMenu suggestions = new javafx.scene.control.ContextMenu();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            suggestions.getItems().clear();
            if (newVal == null || newVal.trim().isEmpty()) {
                table.getItems().setAll(all);
                suggestions.hide();
                return;
            }
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT c_id, customer_name, phone_no, loyalty_points FROM customers WHERE customer_name LIKE ? OR phone_no LIKE ? LIMIT 10");
                ps.setString(1, "%" + newVal + "%"); ps.setString(2, "%" + newVal + "%");
                java.sql.ResultSet rs = ps.executeQuery();
                java.util.List<Customer> filtered = new java.util.ArrayList<>();
                while (rs.next()) {
                    Customer c = new Customer(); c.cId = rs.getString("c_id"); c.customerName = rs.getString("customer_name"); c.phoneNo = rs.getString("phone_no"); c.loyaltyPoints = rs.getInt("loyalty_points");
                    filtered.add(c);
                    javafx.scene.control.MenuItem it = new javafx.scene.control.MenuItem(c.customerName + " (" + c.phoneNo + ")");
                    it.setOnAction(ev -> { searchField.setText(c.customerName); table.getItems().setAll(java.util.Collections.singletonList(c)); suggestions.hide(); });
                    suggestions.getItems().add(it);
                }
                if (!suggestions.getItems().isEmpty()) {
                    javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                    suggestions.show(searchField, bounds.getMinX(), bounds.getMaxY());
                }
                table.getItems().setAll(filtered);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

    
    }

    // User Management: list users from `users` table and allow simple update/delete operations
    private void showUserManagement() {
        ensureMaximized();
        VBox box = new VBox(20);
        box.setStyle("-fx-padding: 30; -fx-background-color: #fff; -fx-background-radius: 12;");
        box.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        HBox top = new HBox(12); top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    TextField q = new TextField(); q.setPromptText("Search users..."); q.setStyle("-fx-pref-width:260; -fx-font-size:14;");
    // autocomplete suggestions for username search
    javafx.scene.control.ContextMenu suggestions = new javafx.scene.control.ContextMenu();
        Button add = new Button("Add User"); add.setStyle("-fx-background-color:#6fa8ff; -fx-text-fill:#fff; -fx-background-radius:8;");
        Button expCsv = new Button("Export CSV"); Button expPdf = new Button("Export PDF"); Button expXlsx = new Button("Export XLSX");
    Button refreshUsers = new Button("⟲");
    refreshUsers.setStyle("-fx-font-size: 16; -fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
    refreshUsers.setOnAction(ev -> { try { q.clear(); q.requestFocus(); } catch (Exception ignored) {} showUserManagement(); });
        top.getChildren().addAll(q, add, refreshUsers, expCsv, expPdf, expXlsx);

        TableView<java.util.Map<String,Object>> table = new TableView<>(); applyTableStyling(table); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    TableColumn<java.util.Map<String,Object>, String> cid = new TableColumn<>("ID"); cid.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("id"))));
    TableColumn<java.util.Map<String,Object>, String> uname = new TableColumn<>("Username"); uname.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("username"))));
    TableColumn<java.util.Map<String,Object>, String> upwd = new TableColumn<>("Password"); upwd.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("password"))));
    TableColumn<java.util.Map<String,Object>, String> urole = new TableColumn<>("Role"); urole.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("role"))));
        TableColumn<java.util.Map<String,Object>, Void> ops = new TableColumn<>("Actions");
        ops.setCellFactory(p -> new TableCell<java.util.Map<String,Object>, Void>() {
            private final Button upd = new Button("Update");
            private final Button del = new Button("Delete");
            private final HBox hb = new HBox(8, upd, del);
            { upd.setStyle("-fx-background-color: #b8c1ec; -fx-text-fill: #232946;"); del.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946;");
              upd.setOnAction(e -> showUpdateUserDialog(getTableView().getItems().get(getIndex())));
              del.setOnAction(e -> {
                  java.util.Map<String,Object> m = getTableView().getItems().get(getIndex());
                  String uid = String.valueOf(m.get("id"));
                  Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setHeaderText(null); a.setContentText("Delete user '" + m.get("username") + "'?");
                  a.showAndWait().ifPresent(bt -> { if (bt == ButtonType.OK) {
                      try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                          java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?"); ps.setString(1, uid); ps.executeUpdate();
                          table.getItems().remove(m);
                      } catch (Exception ex) { ex.printStackTrace(); showAlert("Failed to delete user"); }
                  }});
              }); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty?null:hb); }
        });
    javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> colList = javafx.collections.FXCollections.observableArrayList();
    colList.add(cid); colList.add(uname); colList.add(upwd); colList.add(urole); colList.add(ops);
    table.getColumns().setAll(colList);

        box.getChildren().addAll(top, table);
        mainContent.getChildren().setAll(createScrollable(box));

        // load data (do not list admin accounts here)
        javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, username, password, role FROM users WHERE role <> 'admin'");
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", rs.getString("id"));
                m.put("username", rs.getString("username"));
                m.put("password", rs.getString("password"));
                m.put("role", rs.getString("role"));
                rows.add(m);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        table.setItems(rows);

        // search with suggestion dropdown
        q.textProperty().addListener((obs,o,n) -> {
            try {
                suggestions.getItems().clear();
                if (n == null || n.trim().isEmpty()) {
                    suggestions.hide();
                    table.setItems(rows);
                } else {
                    String term = n.toLowerCase();
                    javafx.collections.ObservableList<java.util.Map<String,Object>> filtered = javafx.collections.FXCollections.observableArrayList();
                    for (java.util.Map<String,Object> m : rows) {
                        String unameVal = m.get("username") == null ? "" : m.get("username").toString();
                        if (unameVal.toLowerCase().contains(term)) {
                            filtered.add(m);
                            // create a menu item for suggestion (show username and role)
                            javafx.scene.control.MenuItem it = new javafx.scene.control.MenuItem(unameVal + " (" + String.valueOf(m.get("role")) + ")");
                            it.setOnAction(ev -> {
                                q.setText(unameVal);
                                table.getItems().setAll(java.util.Collections.singletonList(m));
                                suggestions.hide();
                            });
                            suggestions.getItems().add(it);
                        }
                    }
                    // show suggestions anchored under the search field
                    if (!suggestions.getItems().isEmpty()) {
                        javafx.geometry.Bounds bounds = q.localToScreen(q.getBoundsInLocal());
                        suggestions.show(q, bounds.getMinX(), bounds.getMaxY());
                    } else {
                        suggestions.hide();
                    }
                    table.setItems(filtered);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // add user dialog
        add.setOnAction(ev -> showAddUserDialog(table));
            expCsv.setOnAction(ev -> {
            try {
                String out = util.InvoiceExporter.resolveAdminExportPath("users.csv");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(out))) {
                    pw.println("id,username,password,role");
                    for (java.util.Map<String,Object> m : table.getItems()) pw.println(String.format("%s,%s,%s,%s", m.get("id"), m.get("username"), m.get("password"), m.get("role")));
                }
                showAlert("CSV exported to " + new java.io.File(out).getAbsolutePath());
            } catch (Exception ex) { ex.printStackTrace(); showAlert("Export failed"); }
        });
            expPdf.setOnAction(ev -> {
            // Build a Map-backed table and reuse exportTableToPdf, forcing admin export path
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Username"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("username"))));
            TableColumn<java.util.Map<String,Object>, String> pwdCol = new TableColumn<>("Password"); pwdCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("password"))));
            TableColumn<java.util.Map<String,Object>, String> ccol = new TableColumn<>("Role"); ccol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("role"))));
            java.util.List<javafx.scene.control.TableColumn<java.util.Map<String,Object>, String>> cols = new java.util.ArrayList<>();
            cols.add(a); cols.add(b); cols.add(pwdCol); cols.add(ccol);
            m.getColumns().setAll(cols);
            javafx.collections.ObservableList<java.util.Map<String,Object>> mapRows = javafx.collections.FXCollections.observableArrayList();
            for (java.util.Map<String,Object> entry : table.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>();
                mm.put("id", entry.get("id")); mm.put("username", entry.get("username")); mm.put("password", entry.get("password")); mm.put("role", entry.get("role"));
                mapRows.add(mm);
            }
            m.setItems(mapRows);
            exportTableToPdf(m, util.InvoiceExporter.resolveAdminExportPath("users.pdf"));
        });
            expXlsx.setOnAction(ev -> {
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Username"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("username"))));
            TableColumn<java.util.Map<String,Object>, String> pwdCol2 = new TableColumn<>("Password"); pwdCol2.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("password"))));
            TableColumn<java.util.Map<String,Object>, String> ccol = new TableColumn<>("Role"); ccol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("role"))));
            java.util.List<javafx.scene.control.TableColumn<java.util.Map<String,Object>, String>> cols2 = new java.util.ArrayList<>();
            cols2.add(a); cols2.add(b); cols2.add(pwdCol2); cols2.add(ccol);
            m.getColumns().setAll(cols2);
            javafx.collections.ObservableList<java.util.Map<String,Object>> mapRows2 = javafx.collections.FXCollections.observableArrayList();
            for (java.util.Map<String,Object> entry : table.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>();
                mm.put("id", entry.get("id")); mm.put("username", entry.get("username")); mm.put("password", entry.get("password")); mm.put("role", entry.get("role"));
                mapRows2.add(mm);
            }
            m.setItems(mapRows2);
            exportTableToXlsx(m, util.InvoiceExporter.resolveAdminExportPath("users.xlsx"));
        });
    }

    private void showAddUserDialog(TableView<java.util.Map<String,Object>> table) {
        Stage s = new Stage(); s.setTitle("Add User"); VBox v = new VBox(8); v.setStyle("-fx-padding:12;");
        TextField user = new TextField(); user.setPromptText("Username");
        PasswordField pass = new PasswordField(); pass.setPromptText("Password");
    // disallow creating admin users from this UI
    javafx.scene.control.ComboBox<String> role = new javafx.scene.control.ComboBox<>(); role.getItems().addAll("cashier"); role.setValue("cashier");
        Button ok = new Button("Add"); Button cancel = new Button("Cancel");
        v.getChildren().addAll(new Label("Username"), user, new Label("Password"), pass, new Label("Role"), role, new HBox(8, ok, cancel));
        ok.setOnAction(e -> {
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                // Ensure username/password are unique among cashiers
                try (java.sql.PreparedStatement dup = conn.prepareStatement("SELECT id FROM users WHERE role = 'cashier' AND (username = ? OR password = ?) LIMIT 1")) {
                    dup.setString(1, user.getText() == null ? "" : user.getText().trim());
                    dup.setString(2, pass.getText() == null ? "" : pass.getText());
                    try (java.sql.ResultSet drr = dup.executeQuery()) {
                        if (drr.next()) {
                            new Alert(Alert.AlertType.ERROR, "Cashier username or password is already used.").showAndWait();
                            return;
                        }
                    }
                }
                // auto-generate a user id (U + zero-padded 5 digit number)
                String genId = "U" + String.format("%05d", new java.util.Random().nextInt(100000));
                java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)");
                ps.setString(1, genId); ps.setString(2, user.getText()); ps.setString(3, pass.getText()); ps.setString(4, role.getValue()); ps.executeUpdate();
                s.close(); showUserManagement();
            } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to add user").showAndWait(); }
        });
        cancel.setOnAction(e -> s.close());
        s.setScene(new javafx.scene.Scene(v, 360, 320)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    private void showUpdateUserDialog(java.util.Map<String,Object> m) {
        if (m == null) return; Stage s = new Stage(); s.setTitle("Update User"); VBox v = new VBox(8); v.setStyle("-fx-padding:12;");
        TextField uname = new TextField(String.valueOf(m.get("username")));
        PasswordField pwd = new PasswordField(); pwd.setPromptText("New password (leave blank to keep)");
    // disallow updating role to admin via this UI
    javafx.scene.control.ComboBox<String> role = new javafx.scene.control.ComboBox<>(); role.getItems().addAll("cashier"); role.setValue(String.valueOf(m.get("role")));
        Button ok = new Button("Update"); Button cancel = new Button("Cancel");
        v.getChildren().addAll(new Label("Username"), uname, new Label("Password"), pwd, new Label("Role"), role, new HBox(8, ok, cancel));
        ok.setOnAction(e -> {
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                String currentId = String.valueOf(m.get("id"));
                // Check for duplicates among cashiers (exclude current user)
                if (pwd.getText().trim().isEmpty()) {
                    try (java.sql.PreparedStatement dup = conn.prepareStatement("SELECT id FROM users WHERE role = 'cashier' AND username = ? AND id <> ? LIMIT 1")) {
                        dup.setString(1, uname.getText() == null ? "" : uname.getText().trim());
                        dup.setString(2, currentId);
                        try (java.sql.ResultSet dr = dup.executeQuery()) { if (dr.next()) { new Alert(Alert.AlertType.ERROR, "Username already used by another cashier").showAndWait(); return; } }
                    }
                } else {
                    try (java.sql.PreparedStatement dup = conn.prepareStatement("SELECT id FROM users WHERE role = 'cashier' AND (username = ? OR password = ?) AND id <> ? LIMIT 1")) {
                        dup.setString(1, uname.getText() == null ? "" : uname.getText().trim());
                        dup.setString(2, pwd.getText());
                        dup.setString(3, currentId);
                        try (java.sql.ResultSet dr = dup.executeQuery()) { if (dr.next()) { new Alert(Alert.AlertType.ERROR, "Cashier username or password is already used.").showAndWait(); return; } }
                    }
                }

                java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE users SET username = ?, role = ? " + (pwd.getText().trim().isEmpty()?"":" , password = ? ") + " WHERE id = ?");
                ps.setString(1, uname.getText());
                ps.setString(2, role.getValue());
                int idx = 3;
                if (!pwd.getText().trim().isEmpty()) { ps.setString(idx++, pwd.getText()); }
                ps.setString(idx, currentId);
                ps.executeUpdate(); s.close(); showUserManagement();
            } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to update").showAndWait(); }
        });
        cancel.setOnAction(e -> s.close());
        s.setScene(new javafx.scene.Scene(v, 360, 320)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    private void showAddCustomerDialog(TableView<Customer> table) {
        Stage s = new Stage(); s.setTitle("Add Customer");
        VBox v = new VBox(8); v.setStyle("-fx-padding:16;");
        TextField name = new TextField(); name.setPromptText("Customer Name");
        TextField phone = new TextField(); phone.setPromptText("Phone No");
        Button ok = new Button("Add"); Button cancel = new Button("Cancel");
        v.getChildren().addAll(new Label("Name"), name, new Label("Phone"), phone, new HBox(8, ok, cancel));
        ok.setOnAction(e -> {
            String phoneVal = phone.getText() == null ? "" : phone.getText().trim();
            if (!phoneVal.matches("\\d{10}")) {
                new Alert(Alert.AlertType.ERROR, "Phone number must be exactly 10 digits").showAndWait();
                try { phone.requestFocus(); } catch (Exception ignored) {}
                return;
            }
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                // Check for existing customer with same name or phone
                try (java.sql.PreparedStatement dup = conn.prepareStatement("SELECT c_id FROM customers WHERE customer_name = ? OR phone_no = ? LIMIT 1")) {
                    dup.setString(1, name.getText() == null ? "" : name.getText().trim());
                    dup.setString(2, phoneVal);
                    try (java.sql.ResultSet drr = dup.executeQuery()) {
                        if (drr.next()) {
                            new Alert(Alert.AlertType.ERROR, "Customer username or phone number already exists.").showAndWait();
                            return;
                        }
                    }
                }
                String id = "C" + String.format("%05d", new java.util.Random().nextInt(100000));
                java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO customers (c_id, customer_name, phone_no) VALUES (?, ?, ?)");
                ps.setString(1, id); ps.setString(2, name.getText()); ps.setString(3, phoneVal); ps.executeUpdate();
                s.close();
                showCustomerManagement();
            } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to add customer").showAndWait(); }
        });
        cancel.setOnAction(e -> s.close());
        s.setScene(new javafx.scene.Scene(v, 360, 240)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    // Render a small dashboard home with KPIs and low-stock alerts
    private void showDashboardHome() {
    ensureMaximized();
    VBox home = new VBox(12);
        // Use clean white background with proper padding and rounded corners
        home.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-background-radius: 8;");

        Label title = new Label("Overview");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

    // KPI rows (minimal styling, dark text)
    Label totalSalesTodayLbl = new Label("Total Sales today: Loading...");
    totalSalesTodayLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #333333;");
    Label productsSoldLbl = new Label("Products sold: Loading...");
    productsSoldLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #333333;");
    Label revenueGeneratedLbl = new Label("Revenue generated: Loading...");
    revenueGeneratedLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #333333;");

    // Additional KPIs
    Label totalProductsLbl = new Label("Total Products: Loading..."); totalProductsLbl.setStyle("-fx-font-size:16; -fx-text-fill: #333333;");
    Label salesPeakHourLbl = new Label("Sales Peak Hour: Loading..."); salesPeakHourLbl.setStyle("-fx-font-size:16; -fx-text-fill: #333333;");
    Label highestProductLbl = new Label("Highest Selling Product: Loading..."); highestProductLbl.setStyle("-fx-font-size:16; -fx-text-fill: #333333;");
    Label totalCustomersLbl = new Label("Total Customers: Loading..."); totalCustomersLbl.setStyle("-fx-font-size:16; -fx-text-fill: #333333;");
    Label topCustomerLbl = new Label("Top Customer (Points): Loading..."); topCustomerLbl.setStyle("-fx-font-size:16; -fx-text-fill: #333333;");

        // Low stock area (red) with image placeholder and details button
            HBox lowStockBox = new HBox(10);
            lowStockBox.setStyle("-fx-alignment: center-left;");
            ImageView lowStockImg = new ImageView();
            // increase size so any text within the image (if present) is readable in the dashboard
            lowStockImg.setFitWidth(64); lowStockImg.setFitHeight(64);
            lowStockImg.setPreserveRatio(true);
            // user can replace image later; keep placeholder transparent
            Label lowStockLabel = new Label("No low stock items.");
            lowStockLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #333333;");
            Button lowStockDetails = new Button("Details");
            lowStockDetails.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946;");
            lowStockBox.getChildren().addAll(lowStockImg, lowStockLabel, lowStockDetails);
            // title for the low-stock block so we can hide/show the whole section
            Label lowStockTitle = new Label("Low Stock Alert:"); lowStockTitle.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill: #333333;");

        // Total cashiers
    Label totalCashiersLbl = new Label("Total Cashiers: Loading...");
    totalCashiersLbl.setStyle("-fx-font-size: 16; -fx-text-fill: #333333;");

    // Charts placeholders
    BarChart<String, Number> bar = null;
    PieChart pie = null;
    // Line chart for customer loyalty (points over top customers)
    javafx.scene.chart.LineChart<String, Number> loyaltyLine = null;
        // additional KPI placeholders (declare here so they can be populated inside DB try-block)
    Label taxLbl = new Label("Default Tax: Loading..."); taxLbl.setStyle("-fx-text-fill: #333333;");
    Label discountLbl = new Label("Default Discount: Loading..."); discountLbl.setStyle("-fx-text-fill: #333333;");
    Label monthlySalesLbl = new Label("Monthly Sales: Loading..."); monthlySalesLbl.setStyle("-fx-text-fill: #333333;");
    Label annualSalesLbl = new Label("Annual Sales: Loading..."); annualSalesLbl.setStyle("-fx-text-fill: #333333;");
    Label customerGrowthLbl = new Label("Customer Growth: Loading..."); customerGrowthLbl.setStyle("-fx-text-fill: #333333;");
        VBox expensiveBox = new VBox(6);
    BarChart<String, Number> cashierBar = null;

    // start with title only; charts are added before KPI blocks so graphs appear first
    // add a small refresh button to reload dashboard KPIs
    HBox titleRow = new HBox(8);
    titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    Button refreshDash = new Button("⟲");
    refreshDash.setStyle("-fx-font-size: 16; -fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
    refreshDash.setOnAction(e -> showDashboardHome());
    titleRow.getChildren().addAll(title, refreshDash);
    home.getChildren().add(titleRow);

        // Query DB for values and charts
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            // Total sales today (sum of sales_total for today)
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(sales_total),0) AS total FROM main_sales WHERE DATE(sale_time)=CURDATE()")) {
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) totalSalesTodayLbl.setText("Total Sales today: Rs " + rs.getBigDecimal("total").toPlainString());
            }

            // Products sold today (sum of quantity_sold from sales joined to main_sales date)
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(s.quantity_sold),0) AS sold FROM sales s JOIN main_sales ms ON s.sales_id = ms.sales_id WHERE DATE(ms.sale_time)=CURDATE()")) {
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) productsSoldLbl.setText("Products sold: " + rs.getInt("sold"));
            }

            // Revenue generated today (formula: sum(sales_total) - sum(discount) + sum(tax))
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(sales_total),0) - IFNULL(SUM(discount),0) + IFNULL(SUM(tax),0) AS revenue FROM main_sales WHERE DATE(sale_time)=CURDATE()")) {
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) revenueGeneratedLbl.setText("Revenue generated: Rs " + rs.getBigDecimal("revenue").toPlainString());
            }

            // Low stock list: check local Preferences toggle (fixed threshold 10 when enabled)
            int threshold = Integer.MAX_VALUE;
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(controller.SettingsController.class);
                boolean enabled = prefs.getBoolean("lowStockEnabled", false);
                threshold = enabled ? 10 : Integer.MAX_VALUE; // when disabled threshold is very large -> no results
            } catch (Exception ignored) { threshold = Integer.MAX_VALUE; }

            java.util.List<String> lowItems = new java.util.ArrayList<>();
            try (java.sql.PreparedStatement ls = conn.prepareStatement("SELECT product_id, product_name, stock_quantity FROM Products WHERE stock_quantity <= ? ORDER BY stock_quantity ASC LIMIT 50")) {
                ls.setInt(1, threshold);
                java.sql.ResultSet rls = ls.executeQuery();
                while (rls.next()) {
                    lowItems.add(rls.getString("product_name") + " (" + rls.getInt("stock_quantity") + " left)");
                }
            }
            if (!lowItems.isEmpty()) {
                lowStockLabel.setText(lowItems.size() + " low-stock items");
                lowStockBox.setStyle("-fx-background-color: #fff2f2; -fx-border-color: #ff9b9b; -fx-padding: 8; -fx-alignment: center-left; -fx-border-radius: 6; -fx-background-radius: 6;");
                // try to load warning image from assets; ignore failures
                try {
                    // prefer the project's pngtree warning asset if available
                    java.io.InputStream is = getClass().getResourceAsStream("/assets/pngtree-warning-vector-sign-png-image_8930287.png");
                    if (is == null) {
                        // fallback to earlier filename if the preferred one is not present
                        is = getClass().getResourceAsStream("/assets/warning-sign-icon-transparent-background-png.png");
                    }
                    if (is != null) {
                        Image warn = new Image(is);
                        lowStockImg.setImage(warn);
                    }
                } catch (Exception ignored) {}

                // create a dialog to show low stock details
                javafx.scene.control.Dialog<Void> lowStockDialog = new javafx.scene.control.Dialog<>();
                // keep a reference so other parts (preference listener) can close it
                lowStockDialogRef = lowStockDialog;
                lowStockDialog.setTitle("Low Stock Details");
            VBox vb = new VBox(6);
            vb.setStyle("-fx-padding:12;");
            if (lowItems.isEmpty()) vb.getChildren().add(new Label("No low stock items."));
            else {
                for (String it : lowItems) vb.getChildren().add(new Label("• " + it));
            }
            lowStockDialog.getDialogPane().setContent(vb);
            lowStockDialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            lowStockDialog.initModality(javafx.stage.Modality.NONE);

                // Show/hide details button and the whole low-stock block depending on preference
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(controller.SettingsController.class);
                boolean enabled = prefs.getBoolean("lowStockEnabled", false);
                // hide or show the title and box entirely when the toggle is off
                lowStockDetails.setVisible(enabled);
                lowStockBox.setVisible(enabled);
                lowStockBox.setManaged(enabled);
                lowStockTitle.setVisible(enabled);
                lowStockTitle.setManaged(enabled);
                if (enabled && !lowItems.isEmpty()) {
                    // show non-blocking notification dialog
                    javafx.application.Platform.runLater(() -> {
                        try {
                            javafx.stage.Window owner = mainContent.getScene().getWindow();
                            lowStockDialog.initOwner(owner);
                            lowStockDialog.show();
                        } catch (Exception ignore) {}
                    });
                }
            } catch (Exception ignored) {}

            lowStockDetails.setOnAction(ev -> {
                if (!lowStockDialog.isShowing()) lowStockDialog.show();
                else try { lowStockDialog.getDialogPane().requestFocus(); } catch (Exception ignore) {}
            });

            }

            // Total cashiers
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM users WHERE role = 'Cashier'")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) totalCashiersLbl.setText("Total Cashiers: " + rs.getInt("total"));
            }

            // Total products
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM Products")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) totalProductsLbl.setText("Total Products: " + rs.getInt("total"));
            }

            // Sales peak hour (hour with most sales today) -> display in 12-hour format
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT HOUR(sale_time) AS hr, COUNT(*) AS cnt FROM main_sales WHERE DATE(sale_time)=CURDATE() GROUP BY hr ORDER BY cnt DESC LIMIT 1")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) {
                    int hr = rs.getInt("hr");
                    String ampm = hr >= 12 ? "PM" : "AM";
                    int hr12 = hr % 12; if (hr12 == 0) hr12 = 12;
                    salesPeakHourLbl.setText("Sales Peak Hour: " + hr12 + ":00 " + ampm);
                }
            }

            // Highest selling product
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT s.product_name, SUM(s.quantity_sold) AS sold FROM sales s GROUP BY s.product_name ORDER BY sold DESC LIMIT 1")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) highestProductLbl.setText("Highest Selling Product: " + rs.getString("product_name") + " (" + rs.getInt("sold") + ")");
            }

            // Total customers
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM customers")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) totalCustomersLbl.setText("Total Customers: " + rs.getInt("total"));
            }

            // Top customer by loyalty points
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT customer_name, loyalty_points FROM customers ORDER BY loyalty_points DESC LIMIT 1")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) topCustomerLbl.setText("Top Customer: " + rs.getString("customer_name") + " (" + rs.getInt("loyalty_points") + ")");
            }

            // Top 5 bar chart
            CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Product");
            NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Units Sold");
            bar = new BarChart<>(xAxis, yAxis);
            bar.setTitle("Top 5 Selling Products");
            // Ensure chart title and axis labels are visible on white
            bar.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
            xAxis.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
            yAxis.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
            xAxis.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
            yAxis.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Units Sold");
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT s.product_name, SUM(s.quantity_sold) AS sold FROM sales s GROUP BY s.product_name ORDER BY sold DESC LIMIT 5")) {
                java.sql.ResultSet rs = ps.executeQuery();
                while (rs.next()) series.getData().add(new XYChart.Data<>(rs.getString("product_name"), rs.getInt("sold")));
            }
            bar.getData().add(series);

            // Cashier performance bar chart (top cashiers by sales_total this month)
            CategoryAxis cashierX = new CategoryAxis(); NumberAxis cashierY = new NumberAxis();
            cashierBar = new BarChart<>(cashierX, cashierY);
            cashierBar.setTitle("Top Cashiers (This Month)");
            cashierBar.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
            cashierX.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
            cashierY.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
            cashierX.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
            cashierY.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
            XYChart.Series<String, Number> cashierSeries = new XYChart.Series<>();
            cashierSeries.setName("Sales");
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT cashier_id, SUM(sales_total) AS total FROM main_sales WHERE MONTH(sale_time)=MONTH(CURDATE()) AND YEAR(sale_time)=YEAR(CURDATE()) GROUP BY cashier_id ORDER BY total DESC LIMIT 5")) {
                java.sql.ResultSet rs = ps.executeQuery(); while (rs.next()) cashierSeries.getData().add(new XYChart.Data<>(rs.getString("cashier_id"), rs.getBigDecimal("total")));
            } catch (Exception ignored) {}
            cashierBar.getData().add(cashierSeries);

            // removed customer loyalty pie chart per request (we present loyalty as a line chart)
            // Customer loyalty LINE chart (trend/points for top customers) - useful alternate visualization
            try {
                CategoryAxis loyX = new CategoryAxis(); loyX.setLabel("Customer");
                NumberAxis loyY = new NumberAxis(); loyY.setLabel("Loyalty Points");
                loyaltyLine = new javafx.scene.chart.LineChart<>(loyX, loyY);
                loyaltyLine.setTitle("Top Customers - Loyalty Points");
                loyaltyLine.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
                loyX.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
                loyY.tickLabelFillProperty().set(javafx.scene.paint.Color.web("#333333"));
                loyX.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
                loyY.setStyle("-fx-text-fill: #333333; -fx-tick-label-fill: #333333;");
                XYChart.Series<String, Number> loySeries = new XYChart.Series<>();
                loySeries.setName("Points");
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT customer_name, loyalty_points FROM customers ORDER BY loyalty_points DESC LIMIT 10")) {
                    java.sql.ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        String name = rs.getString("customer_name");
                        int pts = rs.getInt("loyalty_points");
                        loySeries.getData().add(new XYChart.Data<>(name, pts));
                    }
                }
                loyaltyLine.getData().add(loySeries);
            } catch (Exception ignored) {}

            // Most expensive products (top 5 by price)
            expensiveBox = new VBox(6); expensiveBox.setStyle("-fx-padding:8;");
            expensiveBox.getChildren().add(new Label("Most Expensive Products:"));
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT product_name, price FROM Products ORDER BY price DESC LIMIT 5")) {
                java.sql.ResultSet rs = ps.executeQuery(); while (rs.next()) expensiveBox.getChildren().add(new Label(rs.getString("product_name") + " — Rs " + rs.getBigDecimal("price").toPlainString()));
            } catch (Exception ignored) {}

            // Monthly and annual sales totals
            // reuse placeholders declared earlier
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(sales_total),0) AS total FROM main_sales WHERE MONTH(sale_time)=MONTH(CURDATE()) AND YEAR(sale_time)=YEAR(CURDATE())")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) monthlySalesLbl.setText("Monthly Sales: Rs " + rs.getBigDecimal("total").toPlainString());
            } catch (Exception ignored) {}
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT IFNULL(SUM(sales_total),0) AS total FROM main_sales WHERE YEAR(sale_time)=YEAR(CURDATE())")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) annualSalesLbl.setText("Annual Sales: Rs " + rs.getBigDecimal("total").toPlainString());
            } catch (Exception ignored) {}

            // Customer growth: compare customers count month over month
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT (SELECT COUNT(*) FROM customers WHERE MONTH(created_at)=MONTH(CURDATE()) AND YEAR(created_at)=YEAR(CURDATE())) AS cur, (SELECT COUNT(*) FROM customers WHERE MONTH(created_at)=MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND YEAR(created_at)=YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))) AS prev")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) {
                    int cur = rs.getInt("cur"); int prev = rs.getInt("prev");
                    double pct = prev == 0 ? (cur>0?100.0:0.0) : ((cur - prev) * 100.0 / prev);
                    customerGrowthLbl.setText(String.format("Customer Growth: %d vs %d (%.1f%%)", cur, prev, pct));
                }
            } catch (Exception ignored) { customerGrowthLbl.setText("Customer Growth: N/A"); }

            // Current default tax/discount display
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT tax, discount FROM settings ORDER BY updated_at DESC LIMIT 1")) {
                java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) { taxLbl.setText("Default Tax: " + rs.getBigDecimal("tax").toPlainString() + "%"); discountLbl.setText("Default Discount: " + rs.getBigDecimal("discount").toPlainString() + "%"); }
            } catch (Exception ignored) {}

            // Category pie chart
            pie = new PieChart(); pie.setTitle("Sales by Category");
            pie.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT p.category, SUM(s.quantity_sold) AS sold FROM sales s JOIN Products p ON p.product_id = s.product_id GROUP BY p.category ORDER BY sold DESC")) {
                java.sql.ResultSet rs = ps.executeQuery();
                while (rs.next()) pie.getData().add(new PieChart.Data(rs.getString("category"), rs.getInt("sold")));
            } catch (Exception ignore) {}

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    // Charts row - show charts first so they appear at top of Overview
    // (KPI blocks will be added after the charts below)

    HBox mixedCharts = new HBox(12);
    mixedCharts.setStyle("-fx-padding:8; -fx-alignment: top-left;");
    // left column: top products bar and cashier performance
    VBox leftCol = new VBox(8);
    if (bar != null) leftCol.getChildren().add(bar);
    if (cashierBar != null) leftCol.getChildren().add(cashierBar);
    // right column: category pie, loyalty line, scatter and bubble charts
    VBox rightCol = new VBox(8);
    if (pie != null) rightCol.getChildren().add(pie);
    if (loyaltyLine != null) rightCol.getChildren().add(loyaltyLine);

    // ScatterChart: Price vs Quantity Sold
    javafx.scene.chart.ScatterChart<Number, Number> scatter = null;
    try {
        NumberAxis sx = new NumberAxis(); sx.setLabel("Price");
        NumberAxis sy = new NumberAxis(); sy.setLabel("Quantity Sold");
        scatter = new javafx.scene.chart.ScatterChart<>(sx, sy);
        scatter.setTitle("Price vs Quantity Sold");
        XYChart.Series<Number, Number> sseries = new XYChart.Series<>();
        sseries.setName("Products");
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT p.product_id, p.product_name, p.price, IFNULL(SUM(s.quantity_sold),0) AS sold FROM sales s JOIN Products p ON p.product_id = s.product_id GROUP BY p.product_id, p.product_name, p.price ORDER BY sold DESC LIMIT 200")) {
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double price = rs.getDouble("price");
                int sold = rs.getInt("sold");
                sseries.getData().add(new XYChart.Data<>(price, sold));
            }
        }
        scatter.getData().add(sseries);
    // ensure legend and axis labels are visible and styled and allow chart to expand
    scatter.setLegendVisible(true);
    scatter.setAnimated(false);
    scatter.setMaxWidth(Double.MAX_VALUE);
    try { javafx.scene.layout.VBox.setVgrow(scatter, javafx.scene.layout.Priority.ALWAYS); javafx.scene.layout.HBox.setHgrow(scatter, javafx.scene.layout.Priority.ALWAYS); } catch (Exception ignored) {}
    try { ((NumberAxis)scatter.getXAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    try { ((NumberAxis)scatter.getYAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    // force CSS/layout so lookups find nodes
    try { scatter.applyCss(); scatter.layout(); } catch (Exception ignored) {}
    scatter.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 13px;"));
    scatter.lookupAll(".chart-legend").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
    scatter.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;"));
    ensureLegendTextVisible(scatter);
    } catch (Exception ignored) {}

    // Category Analysis scatter (X = Product Category, Y = Total Revenue)
    javafx.scene.chart.ScatterChart<String, Number> scatterCategory = null;
    try {
        CategoryAxis scx = new CategoryAxis(); scx.setLabel("Product Category");
        NumberAxis scy = new NumberAxis(); scy.setLabel("Total Revenue");
        scatterCategory = new javafx.scene.chart.ScatterChart<>(scx, scy);
        scatterCategory.setTitle("Category Analysis");
        XYChart.Series<String, Number> scSeries = new XYChart.Series<>(); scSeries.setName("Revenue");
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT p.category, IFNULL(SUM(s.quantity_sold * s.sale_price),0) AS revenue FROM sales s JOIN Products p ON p.product_id = s.product_id GROUP BY p.category ORDER BY revenue DESC")) {
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) scSeries.getData().add(new XYChart.Data<>(rs.getString("category"), rs.getDouble("revenue")));
        }
        scatterCategory.getData().add(scSeries);
    scatterCategory.setLegendVisible(true);
    scatterCategory.setAnimated(false);
    scatterCategory.setMaxWidth(Double.MAX_VALUE);
    try { javafx.scene.layout.VBox.setVgrow(scatterCategory, javafx.scene.layout.Priority.ALWAYS); javafx.scene.layout.HBox.setHgrow(scatterCategory, javafx.scene.layout.Priority.ALWAYS); } catch (Exception ignored) {}
    try { ((CategoryAxis)scatterCategory.getXAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    try { ((NumberAxis)scatterCategory.getYAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    try { scatterCategory.applyCss(); scatterCategory.layout(); } catch (Exception ignored) {}
    scatterCategory.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 13px;"));
    scatterCategory.lookupAll(".chart-legend").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
    scatterCategory.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;"));
    ensureLegendTextVisible(scatterCategory);
    } catch (Exception ignored) {}

    // BubbleChart: Category Analysis (X = Category, Y = Total Revenue, Bubble size = #transactions)
    javafx.scene.chart.BubbleChart<String, Number> bubble = null;
    try {
        javafx.scene.chart.CategoryAxis bx = new javafx.scene.chart.CategoryAxis(); bx.setLabel("Category");
        NumberAxis by = new NumberAxis(); by.setLabel("Total Revenue");
        bubble = new javafx.scene.chart.BubbleChart<>(bx, by);
        bubble.setTitle("Category Analysis (Transactions)");
        XYChart.Series<String, Number> bseries = new XYChart.Series<>(); bseries.setName("Transactions");
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT p.category, IFNULL(SUM(s.quantity_sold * s.sale_price),0) AS revenue, COUNT(DISTINCT s.sales_id) AS txns FROM sales s JOIN Products p ON p.product_id = s.product_id GROUP BY p.category ORDER BY revenue DESC")) {
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String cat = rs.getString("category");
                double rev = rs.getDouble("revenue");
                int txns = rs.getInt("txns");
                XYChart.Data<String, Number> d = new XYChart.Data<>(cat, rev, txns);
                bseries.getData().add(d);
            }
        }
        bubble.getData().add(bseries);
    bubble.setLegendVisible(true);
    bubble.setAnimated(false);
    bubble.setMaxWidth(Double.MAX_VALUE);
    try { javafx.scene.layout.VBox.setVgrow(bubble, javafx.scene.layout.Priority.ALWAYS); javafx.scene.layout.HBox.setHgrow(bubble, javafx.scene.layout.Priority.ALWAYS); } catch (Exception ignored) {}
    try { ((CategoryAxis)bubble.getXAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    try { ((NumberAxis)bubble.getYAxis()).setTickLabelFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
    try { bubble.applyCss(); bubble.layout(); } catch (Exception ignored) {}
    bubble.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 13px;"));
    bubble.lookupAll(".chart-legend").forEach(n -> n.setStyle("-fx-text-fill: #333333;"));
    bubble.lookupAll(".axis-label").forEach(n -> n.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;"));
    ensureLegendTextVisible(bubble);
    } catch (Exception ignored) {}

    // Arrange right-column charts: place bubble to the left of category scatter (pair), then the price-vs-qty scatter below
    try {
        javafx.scene.layout.HBox chartPair = new javafx.scene.layout.HBox(12);
        chartPair.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        if (bubble != null) chartPair.getChildren().add(bubble);
        if (scatterCategory != null) chartPair.getChildren().add(scatterCategory);
        if (pie != null) rightCol.getChildren().add(pie);
        if (loyaltyLine != null) rightCol.getChildren().add(loyaltyLine);
        if (!chartPair.getChildren().isEmpty()) rightCol.getChildren().add(chartPair);
        if (scatter != null) rightCol.getChildren().add(scatter);
    } catch (Exception ignored) {}

    if (!leftCol.getChildren().isEmpty()) mixedCharts.getChildren().add(leftCol);
    if (!rightCol.getChildren().isEmpty()) mixedCharts.getChildren().add(rightCol);
    if (!mixedCharts.getChildren().isEmpty()) {
        // add charts first so they're visible at the top of the Overview
        home.getChildren().add(mixedCharts);
        // now add KPI blocks and other overview info after charts
        home.getChildren().addAll(totalSalesTodayLbl, productsSoldLbl, revenueGeneratedLbl,
            totalProductsLbl, salesPeakHourLbl, highestProductLbl,
            new Label(""), // spacer
            lowStockTitle, lowStockBox,
            totalCashiersLbl, totalCustomersLbl, topCustomerLbl,
            taxLbl, discountLbl, monthlySalesLbl, annualSalesLbl, customerGrowthLbl, expensiveBox);
    }

    // Export overview (PDF only) - top-right: snapshot the overview and write to PDF
    HBox overviewActions = new HBox(8);
    Button exportOverviewPdf = new Button("Export Overview (PDF)");
    overviewActions.getChildren().add(exportOverviewPdf);
    exportOverviewPdf.setOnAction(e -> {
        try {
            if (mainContent.getChildren().isEmpty()) { showAlert("Nothing to export"); return; }
            javafx.scene.Node node = mainContent.getChildren().get(0);
            javafx.scene.Node contentNode = node instanceof javafx.scene.control.ScrollPane ? ((javafx.scene.control.ScrollPane) node).getContent() : node;
            try { if (contentNode instanceof javafx.scene.Parent) { javafx.scene.Parent pnode = (javafx.scene.Parent) contentNode; pnode.applyCss(); pnode.layout(); } } catch (Exception ignored) {}
            javafx.scene.image.WritableImage img = contentNode.snapshot(new javafx.scene.SnapshotParameters(), null);
            javafx.scene.image.PixelReader pr = img.getPixelReader();
            int w = Math.max(1, (int) img.getWidth()); int h = Math.max(1, (int) img.getHeight());
            java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) bImg.setRGB(x, y, pr.getArgb(x, y));
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                javax.imageio.ImageIO.write(bImg, "png", baos);
                baos.flush();
                byte[] pngBytes = baos.toByteArray();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(util.InvoiceExporter.resolveAdminExportPath("overview_export.pdf"))) {
                    // Create an A4 PDF and scale the snapshot to fill the page while preserving aspect ratio
                    com.itextpdf.text.Rectangle a4 = com.itextpdf.text.PageSize.A4;
                    com.itextpdf.text.Document doc = new com.itextpdf.text.Document(a4, 18, 18, 18, 18);
                    com.itextpdf.text.pdf.PdfWriter.getInstance(doc, fos);
                    doc.open();
                    com.itextpdf.text.Image itImg = com.itextpdf.text.Image.getInstance(pngBytes);
                    // compute scale to fit within A4 minus margins
                    float maxW = a4.getWidth() - 36; float maxH = a4.getHeight() - 36;
                    float imgW = itImg.getWidth(); float imgH = itImg.getHeight();
                    float scale = Math.min(maxW / imgW, maxH / imgH);
                    itImg.scalePercent(scale * 100);
                    itImg.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    doc.add(itImg);
                    doc.close();
                }
            }
            showAlert("Overview exported to overview_export.pdf");
        } catch (Exception ex) { ex.printStackTrace(); showAlert("Overview PDF export failed: " + ex.getMessage()); }
    });
    // place actions at the very top
    home.getChildren().add(0, overviewActions);

    // render (invoice view will provide its own prev/next navigation) - no pager on Overview
    // Wrap the dashboard content in a white card container then a ScrollPane so long pages can be scrolled
    VBox card = new VBox(12);
    // ensure dark text so KPI labels, chart axis labels and other text are visible on white
    card.setStyle("-fx-padding: 24; -fx-background-color: #fff; -fx-background-radius: 12; -fx-text-fill: #232946;");
    card.getChildren().add(home);
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(card);
    sp.setFitToWidth(true);
    sp.setFitToHeight(false);
    sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
    sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
    sp.getStyleClass().add("dashboard-scrollpane");
    // ensure the ScrollPane grows to fill the mainContent area
    javafx.scene.layout.StackPane.setAlignment(sp, javafx.geometry.Pos.TOP_LEFT);
    // Use createScrollable so the global footer is appended consistently
    mainContent.getChildren().setAll(createScrollable(card));
    // After the dashboard is inserted into the scene, force CSS/layout and ensure legend text is visible
    Platform.runLater(() -> {
        try {
            if (mainContent == null || mainContent.getChildren().isEmpty()) return;
            javafx.scene.Node top = mainContent.getChildren().get(0);
            javafx.scene.Parent rootParent = null;
            if (top instanceof javafx.scene.control.ScrollPane) {
                javafx.scene.control.ScrollPane spc = (javafx.scene.control.ScrollPane) top;
                javafx.scene.Node content = spc.getContent();
                if (content instanceof javafx.scene.Parent) rootParent = (javafx.scene.Parent) content;
            } else if (top instanceof javafx.scene.Parent) {
                rootParent = (javafx.scene.Parent) top;
            }
            if (rootParent == null) return;
            try { rootParent.applyCss(); rootParent.layout(); } catch (Exception ignored) {}
            // find all chart nodes inside the rootParent and ensure legend text is visible
            for (javafx.scene.Node found : rootParent.lookupAll(".chart")) {
                try { if (found instanceof javafx.scene.Parent) { ((javafx.scene.Parent)found).applyCss(); ((javafx.scene.Parent)found).layout(); } } catch (Exception ignored) {}
                ensureLegendTextVisible(found);
            }
            // also check for chart-legend nodes if any remain
            for (javafx.scene.Node lg : rootParent.lookupAll(".chart-legend")) ensureLegendTextVisible(lg);
        } catch (Exception ignored) {}
    });
    }

    // Settings and reports are top-level admin actions
    @FXML
    private void loadSettingsPage() {
    ensureMaximized();
        try {
            javafx.fxml.FXMLLoader l = new javafx.fxml.FXMLLoader(getClass().getResource("/view/Settings.fxml"));
            javafx.scene.Parent p = l.load();
            // wrap settings FXML in a plain white container (minimal styling)
            VBox container = new VBox(12);
            // ensure dark text on white background so labels are visible
            container.setStyle("-fx-padding: 18; -fx-background-color: #ffffff; -fx-text-fill: #232946;");
            container.getChildren().add(p);
            mainContent.getChildren().setAll(createScrollable(container));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void showSalesReport() {
    ensureMaximized();
        try {
            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12);
            root.setStyle("-fx-padding: 18; -fx-background-color: #ffffff; -fx-text-fill: #232946;");
            Label title = new Label("Sales Report"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
            TableView<java.util.Map<String,Object>> table = new TableView<>();
            applyTableStyling(table);

            // Columns
            TableColumn<java.util.Map<String,Object>, String> cid = new TableColumn<>("Sale ID");
            cid.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("sales_id"))));
            TableColumn<java.util.Map<String,Object>, String> ctime = new TableColumn<>("Sale Time");
            ctime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("sale_time"))));
            TableColumn<java.util.Map<String,Object>, String> ccash = new TableColumn<>("Cashier");
            ccash.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("cashier_id"))));
            TableColumn<java.util.Map<String,Object>, String> csales = new TableColumn<>("Sales Total");
            csales.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(formatMoneyWithCurrency(cd.getValue().get("sales_total"))));
            TableColumn<java.util.Map<String,Object>, String> ctax = new TableColumn<>("Tax");
            ctax.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(formatMoneyWithCurrency(cd.getValue().get("tax"))));
            TableColumn<java.util.Map<String,Object>, String> cdisc = new TableColumn<>("Discount");
            cdisc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(formatMoneyWithCurrency(cd.getValue().get("discount"))));
            TableColumn<java.util.Map<String,Object>, String> ctotal = new TableColumn<>("Grand Total");
            ctotal.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(formatMoneyWithCurrency(cd.getValue().get("grand_total"))));
            TableColumn<java.util.Map<String,Object>, String> ccustId = new TableColumn<>("Customer ID");
            ccustId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("customer_id"))));
            TableColumn<java.util.Map<String,Object>, String> ccustName = new TableColumn<>("Customer Name");
            ccustName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("customer_name"))));
            TableColumn<java.util.Map<String,Object>, String> ccaption = new TableColumn<>("Caption");
            ccaption.setCellValueFactory(cd -> {
                Object v = cd.getValue().get("caption");
                String s = (v == null) ? "-" : String.valueOf(v);
                if (s == null || "null".equals(s) || s.trim().isEmpty()) s = "-";
                return new javafx.beans.property.SimpleStringProperty(s);
            });
            javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> salesCols = javafx.collections.FXCollections.observableArrayList();
            salesCols.add(cid); salesCols.add(ctime); salesCols.add(ccash); salesCols.add(csales); salesCols.add(ctax); salesCols.add(cdisc); salesCols.add(ctotal); salesCols.add(ccustId); salesCols.add(ccustName);
            salesCols.add(ccaption);
            table.getColumns().setAll(salesCols);

            // Load rows from main_sales
            javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT ms.sales_id, ms.sale_time, ms.cashier_id, ms.sales_total, ms.tax, ms.discount, ms.grand_total, ms.customer_id, ms.customer_name, ms.caption FROM main_sales ms ORDER BY ms.sale_time DESC LIMIT 500");
                java.sql.ResultSet rs = ps.executeQuery();
                salesIds.clear();
                while (rs.next()) {
                    long id = rs.getLong("sales_id");
                    salesIds.add(id);
                    java.util.Map<String,Object> m = new java.util.HashMap<>();
                    m.put("sales_id", id);
                    m.put("sale_time", rs.getTimestamp("sale_time"));
                    m.put("cashier_id", rs.getString("cashier_id"));
                    try { m.put("sales_total", rs.getBigDecimal("sales_total") == null ? 0.0 : rs.getBigDecimal("sales_total").doubleValue()); } catch (Exception ignored) { m.put("sales_total", safeGetDouble(rs, "sales_total")); }
                    try { m.put("tax", rs.getBigDecimal("tax") == null ? 0.0 : rs.getBigDecimal("tax").doubleValue()); } catch (Exception ignored) { m.put("tax", safeGetDouble(rs, "tax")); }
                    try { m.put("discount", rs.getBigDecimal("discount") == null ? 0.0 : rs.getBigDecimal("discount").doubleValue()); } catch (Exception ignored) { m.put("discount", safeGetDouble(rs, "discount")); }
                    try { m.put("grand_total", rs.getBigDecimal("grand_total") == null ? 0.0 : rs.getBigDecimal("grand_total").doubleValue()); } catch (Exception ignored) { m.put("grand_total", safeGetDouble(rs, "grand_total")); }
                    try { m.put("customer_id", safeGetString(rs, "customer_id")); } catch (Exception ignored) { m.put("customer_id", null); }
                    try { m.put("customer_name", safeGetString(rs, "customer_name")); } catch (Exception ignored) { m.put("customer_name", null); }
                    try { m.put("caption", safeGetString(rs, "caption")); } catch (Exception ignored) { m.put("caption", null); }
                    rows.add(m);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            table.setItems(rows);

            // Click row to open invoice
            table.setRowFactory(tv -> {
                TableRow<java.util.Map<String,Object>> r = new TableRow<>();
                r.setOnMouseClicked(me -> { if (me.getClickCount() == 2 && !r.isEmpty()) { long sid = (Long) r.getItem().get("sales_id"); currentSalesIndex = salesIds.indexOf(sid); showInvoice(sid); } });
                return r;
            });

            // Details box for selected sale
            VBox detailsBox = new VBox(6); detailsBox.setStyle("-fx-padding:8; -fx-border-color:#eee; -fx-background-color:#ffffff; -fx-text-fill:#333333; -fx-border-radius:6; -fx-background-radius:6;");
            detailsBox.getChildren().add(new Label("Select a sale to see details"));
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel == null) { detailsBox.getChildren().setAll(new Label("Select a sale to see details")); return; }
                Object sidObj = newSel.get("sales_id"); if (sidObj == null) { detailsBox.getChildren().setAll(new Label("No sale id present")); return; }
                long sid = (sidObj instanceof Number) ? ((Number)sidObj).longValue() : Long.parseLong(sidObj.toString());
                try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                    java.sql.PreparedStatement ps = conn.prepareStatement("SELECT cashier_id, sales_total, tax, discount, grand_total, sale_time, customer_id, customer_name FROM main_sales WHERE sales_id = ? LIMIT 1");
                    ps.setLong(1, sid); java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        detailsBox.getChildren().clear();
                        detailsBox.getChildren().addAll(
                            new Label("Sale ID: " + sid),
                            new Label("Cashier: " + safeGetString(rs, "cashier_id")),
                            new Label("Customer ID: " + (safeGetString(rs, "customer_id") == null ? "-" : safeGetString(rs, "customer_id"))),
                            new Label("Customer Name: " + (safeGetString(rs, "customer_name") == null ? "Unknown" : safeGetString(rs, "customer_name"))),
                            new Label(String.format("Sales Total: %s", formatMoneyWithCurrency(safeGetNumberForFormatting(rs, "sales_total")))),
                            new Label(String.format("Tax: %s", formatMoneyWithCurrency(safeGetNumberForFormatting(rs, "tax")))),
                            new Label(String.format("Discount: %s", formatMoneyWithCurrency(safeGetNumberForFormatting(rs, "discount")))),
                            new Label(String.format("Grand Total: %s", formatMoneyWithCurrency(safeGetNumberForFormatting(rs, "grand_total")))),
                            new Label("Sale Time: " + rs.getTimestamp("sale_time"))
                        );
                        Button viewInv = new Button("View Invoice"); viewInv.setOnAction(ae -> showInvoice(sid));
                        Button exportPdfBtn = new Button("Export Invoice (PDF)"); exportPdfBtn.setOnAction(ae -> {
                            String sfx = computeExportSuffixForSales(sid);
                            util.InvoiceExporter.exportInvoiceToPdf(sid, "invoice_" + sid + sfx + ".pdf");
                        });
                        Button exportXlsxBtn = new Button("Export Invoice (XLSX)"); exportXlsxBtn.setOnAction(ae -> {
                            String sfx = computeExportSuffixForSales(sid);
                            util.InvoiceExporter.exportInvoiceToXlsx(sid, "invoice_" + sid + sfx + ".xlsx");
                        });
                        HBox act = new HBox(8, viewInv, exportPdfBtn, exportXlsxBtn);
                        detailsBox.getChildren().add(act);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            // Footer with aggregate info
            HBox footer = new HBox(16); footer.setStyle("-fx-padding:8; -fx-alignment:center-left;");
            Label countLbl = new Label("Total sales: " + table.getItems().size());
            double totalSum = 0.0;
            for (java.util.Map<String,Object> mm : table.getItems()) {
                try {
                    Object gv = mm.get("grand_total");
                    if (gv instanceof Number) totalSum += ((Number)gv).doubleValue();
                    else if (gv instanceof String) totalSum += Double.parseDouble((String)gv);
                } catch (Exception ignored) {}
            }
            Label sumLbl = new Label(String.format("Aggregate Grand Total: Rs %.2f", totalSum));
            footer.getChildren().addAll(countLbl, sumLbl);

            root.getChildren().addAll(title, table, footer);
            // export buttons
            HBox actions = new HBox(8);
            Button expCsv = new Button("Export CSV");
            Button expPdf = new Button("Export PDF");
            Button expXlsx = new Button("Export XLSX");
            Button refresh = new Button("⟲"); refresh.setStyle("-fx-font-size: 16; -fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
            refresh.setOnAction(ev -> showSalesReport());
            actions.getChildren().addAll(expCsv, expPdf, expXlsx, refresh);
            root.getChildren().add(0, actions);
            // export filenames should include admin username and role, e.g. sales_report_Prajjwal_admin
            String adminName = null;
            try { if (usernameLabel != null) adminName = usernameLabel.getText(); } catch (Exception ignored) {}
            if (adminName == null || adminName.trim().isEmpty()) {
                // try to fetch username from users table if roleLabel indicates admin
                try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                    java.sql.PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE role = 'admin' LIMIT 1");
                    java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) adminName = rs.getString(1);
                } catch (Exception ignored) {}
            }
            final String adminSuffix = (adminName == null || adminName.trim().isEmpty()) ? "" : ("_" + sanitizeForFilename(adminName.trim()) + "_admin");
            expCsv.setOnAction(ev -> exportTableToCsv(table, "sales_report" + adminSuffix + ".csv"));
            expPdf.setOnAction(ev -> exportTableToPdf(table, "sales_report" + adminSuffix + ".pdf"));
            expXlsx.setOnAction(ev -> exportTableToXlsx(table, "sales_report" + adminSuffix + ".xlsx"));
            mainContent.getChildren().setAll(createScrollable(root));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showInvoice(long salesId) {
        try {
            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(8);
            root.setStyle("-fx-padding:12;");
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                // sales table does not have a stored `total` column; compute per-line total as quantity_sold * sale_price
                // do not request optional customer columns (may not exist in schema)
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT ms.sales_id, ms.cashier_id, ms.sales_total, ms.tax, ms.discount, ms.grand_total, ms.sale_time, s.product_id, s.product_name, s.quantity_sold, s.sale_price FROM main_sales ms LEFT JOIN sales s ON s.sales_id = ms.sales_id WHERE ms.sales_id = ?");
                ps.setLong(1, salesId);
                java.sql.ResultSet rs = ps.executeQuery();
                javafx.scene.control.Label header = new javafx.scene.control.Label("Invoice #" + salesId);
                root.getChildren().add(header);
                // create a placeholder for main_sales summary; we'll add it after the product table
                javafx.scene.layout.VBox mainSalesBox = new javafx.scene.layout.VBox(6);
                mainSalesBox.setStyle("-fx-padding:8; -fx-border-color:#eee; -fx-border-width:1; -fx-background-color:#ffffff; -fx-text-fill:#333333; -fx-border-radius:6; -fx-background-radius:6;");
                // placeholder labels; will be populated after reading rows and then inserted below the table
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Sales Total: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Tax: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Discount: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Grand Total: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Cashier: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Customer ID: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Customer Name: Loading..."));
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Sale Time: Loading..."));
                // optional caption field
                mainSalesBox.getChildren().add(new javafx.scene.control.Label("Caption: "));
                javafx.scene.control.TableView<java.util.Map<String,Object>> tbl = new javafx.scene.control.TableView<>();
                applyTableStyling(tbl);
                javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
                // track totals
                double salesTotal = 0.0; double tax = 0.0; double discount = 0.0; double grand = 0.0;
                while (rs.next()) {
                    java.util.Map<String,Object> m = new java.util.HashMap<>();
                    m.put("productId", rs.getString("product_id"));
                    m.put("product", rs.getString("product_name"));
                    m.put("qty", rs.getInt("quantity_sold"));
                    m.put("price", rs.getDouble("sale_price"));
                    // compute total per-line from quantity and unit price
                    int q = 0; double sp = 0.0; try { q = rs.getInt("quantity_sold"); } catch (Exception ignored) {}
                    try { sp = rs.getDouble("sale_price"); } catch (Exception ignored) {}
                    double lineTotal = q * sp;
                    m.put("total", lineTotal);
                    rows.add(m);
                    // try to read summary fields from the first row
                        if (salesTotal == 0.0) {
                            try { salesTotal = rs.getDouble("sales_total"); } catch (Exception ignored) {}
                            try { tax = rs.getDouble("tax"); } catch (Exception ignored) {}
                            try { discount = rs.getDouble("discount"); } catch (Exception ignored) {}
                            try { grand = rs.getDouble("grand_total"); } catch (Exception ignored) {}
                        }
                }
                javafx.scene.control.TableColumn<java.util.Map<String,Object>, String> c0 = new javafx.scene.control.TableColumn<>("Product ID");
                c0.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty((String)cd.getValue().get("productId")));
                javafx.scene.control.TableColumn<java.util.Map<String,Object>, String> c1 = new javafx.scene.control.TableColumn<>("Product");
                c1.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty((String)cd.getValue().get("product")));
                javafx.scene.control.TableColumn<java.util.Map<String,Object>, String> c2 = new javafx.scene.control.TableColumn<>("Qty");
                c2.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("qty"))));
                javafx.scene.control.TableColumn<java.util.Map<String,Object>, String> c3 = new javafx.scene.control.TableColumn<>("Unit Price");
                c3.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cd.getValue().get("price"))));
                javafx.scene.control.TableColumn<java.util.Map<String,Object>, String> c4 = new javafx.scene.control.TableColumn<>("Total");
                c4.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cd.getValue().get("total"))));
                javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> invCols = javafx.collections.FXCollections.observableArrayList();
                invCols.add(c0); invCols.add(c1); invCols.add(c2); invCols.add(c3); invCols.add(c4);
                tbl.getColumns().setAll(invCols);
                tbl.setItems(rows);
                // export actions for this invoice view (tbl is in scope here)
                HBox invActions = new HBox(8);
                Button invCsv = new Button("Export CSV");
                Button invPdf = new Button("Export PDF");
                Button invXlsx = new Button("Export XLSX");
                invActions.getChildren().addAll(invCsv, invPdf, invXlsx);
                invCsv.setOnAction(ev -> {
                    String sfx = computeExportSuffixForSales(salesId);
                    String fname = "invoice_" + salesId + sfx + ".csv";
                    String out = util.InvoiceExporter.resolveAdminExportPath(fname);
                    util.InvoiceExporter.exportInvoiceToCsvToPath(salesId, out);
                });
                invPdf.setOnAction(ev -> {
                    String sfx = computeExportSuffixForSales(salesId);
                    String fname = "invoice_" + salesId + sfx + ".pdf";
                    String out = util.InvoiceExporter.resolveAdminExportPath(fname);
                    util.InvoiceExporter.exportInvoiceToPdfToPath(salesId, out);
                });
                invXlsx.setOnAction(ev -> {
                    String sfx = computeExportSuffixForSales(salesId);
                    String fname = "invoice_" + salesId + sfx + ".xlsx";
                    String out = util.InvoiceExporter.resolveAdminExportPath(fname);
                    util.InvoiceExporter.exportInvoiceToXlsxToPath(salesId, out);
                });
                root.getChildren().addAll(invActions, tbl);
                // add main_sales summary below product table
                root.getChildren().add(mainSalesBox);
                // populate the mainSalesBox inserted earlier with computed totals
                try {
                    if (mainSalesBox != null && mainSalesBox.getChildren().size() >= 7) {
                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(0)).setText(String.format("Sales Total: %.2f", salesTotal));
                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(1)).setText(String.format("Tax: %.2f", tax));
                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(2)).setText(String.format("Discount: %.2f", discount));
                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(3)).setText(String.format("Grand Total: %.2f", grand));
                    }
                } catch (Exception ignored) {}
                // If summary values are still zero/unset, attempt to fetch the main_sales summary row directly
                if ((salesTotal == 0.0 && tax == 0.0 && discount == 0.0 && grand == 0.0) || mainSalesBox.getChildren().stream().anyMatch(n -> ((javafx.scene.control.Label)n).getText().contains("Loading"))) {
                    try (java.sql.PreparedStatement sps = conn.prepareStatement("SELECT sales_total, tax, discount, grand_total, cashier_id, customer_id, customer_name, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1")) {
                        sps.setLong(1, salesId);
                        java.sql.ResultSet rs3 = sps.executeQuery();
                        if (rs3.next()) {
                                    salesTotal = safeGetDouble(rs3, "sales_total");
                                    tax = safeGetDouble(rs3, "tax");
                                    discount = safeGetDouble(rs3, "discount");
                                    grand = safeGetDouble(rs3, "grand_total");
                                    String cashier = safeGetString(rs3, "cashier_id");
                                    String cid = safeGetString(rs3, "customer_id");
                                    String cname = safeGetString(rs3, "customer_name");
                                    java.sql.Timestamp st = null;
                                    try { st = rs3.getTimestamp("sale_time"); } catch (Exception ignored) {}
                                    String captionStr = null;
                                    try { captionStr = rs3.getString("caption"); } catch (Exception ignored) {}
                                    // extra fallback: if caption is empty, attempt a caption-only fetch to be defensive
                                    if (captionStr == null || captionStr.trim().isEmpty()) {
                                        try (java.sql.PreparedStatement capPs = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) {
                                            capPs.setLong(1, salesId);
                                            try (java.sql.ResultSet capRs = capPs.executeQuery()) {
                                                if (capRs.next()) {
                                                    try { String ctmp = capRs.getString("caption"); if (ctmp != null && !ctmp.trim().isEmpty()) captionStr = ctmp; } catch (Exception ignored) {}
                                                }
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                    try {
                                        if (mainSalesBox != null && mainSalesBox.getChildren().size() >= 8) {
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(0)).setText(String.format("Sales Total: %.2f", salesTotal));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(1)).setText(String.format("Tax: %.2f", tax));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(2)).setText(String.format("Discount: %.2f", discount));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(3)).setText(String.format("Grand Total: %.2f", grand));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(4)).setText("Cashier: " + (cashier == null ? "-" : cashier));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(5)).setText("Customer ID: " + (cid == null ? "-" : cid));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(6)).setText("Customer Name: " + (cname == null ? "Unknown" : cname));
                                            ((javafx.scene.control.Label)mainSalesBox.getChildren().get(7)).setText("Sale Time: " + (st == null ? "" : st.toString()));
                                            try {
                                                String capDisplay = (captionStr == null || captionStr.trim().isEmpty()) ? "—" : captionStr;
                                                ((javafx.scene.control.Label)mainSalesBox.getChildren().get(8)).setText("Caption: " + capDisplay);
                                            } catch (Exception ignored) {}
                                        }
                                    } catch (Exception ignored) {}
                                }
                    } catch (Exception ignored) {}
                }
                // Show main_sales meta info and populate mainSalesBox further
                // Try to populate cashier/customer/time but don't overwrite already-populated customer info
                try (java.sql.PreparedStatement infoPs = conn.prepareStatement("SELECT cashier_id, sale_time, customer_id, customer_name FROM main_sales WHERE sales_id = ?")) {
                    infoPs.setLong(1, salesId);
                    java.sql.ResultSet infoRs = infoPs.executeQuery();
                        if (infoRs.next()) {
                            String cashier = infoRs.getString("cashier_id"); if (cashier == null) cashier = "-";
                            String cidVal = null;
                            String cnameVal = null;
                            try { cidVal = infoRs.getString("customer_id"); } catch (Exception ignored) {}
                            try { cnameVal = infoRs.getString("customer_name"); } catch (Exception ignored) {}
                            try {
                                if (mainSalesBox != null && mainSalesBox.getChildren().size() >= 8) {
                                    ((javafx.scene.control.Label)mainSalesBox.getChildren().get(4)).setText("Cashier: " + cashier);
                                    // Only update customer labels if we actually have values from DB
                                    if (cidVal != null && !cidVal.trim().isEmpty()) {
                                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(5)).setText("Customer ID: " + cidVal);
                                    }
                                    if (cnameVal != null && !cnameVal.trim().isEmpty()) {
                                        ((javafx.scene.control.Label)mainSalesBox.getChildren().get(6)).setText("Customer Name: " + cnameVal);
                                    }
                                    java.sql.Timestamp ts = null;
                                    try { ts = infoRs.getTimestamp("sale_time"); } catch (Exception ignored) {}
                                    if (ts != null) ((javafx.scene.control.Label)mainSalesBox.getChildren().get(7)).setText("Sale Time: " + ts.toString());
                                }
                            } catch (Exception ignored) {}
                        }
                } catch (Exception ignored) {}
                // Back button to return to sales report
                Button back = new Button("← Back");
                back.setOnAction(e -> showSalesReport());
                root.getChildren().add(back);
                // Add prev/next navigation for invoices
                if (salesIds == null || salesIds.isEmpty()) {
                    // attempt to load salesIds
                    try (java.sql.Connection conn2 = database.DatabaseConnection.getConnection()) {
                        java.sql.PreparedStatement p2 = conn2.prepareStatement("SELECT sales_id FROM main_sales ORDER BY sale_time DESC");
                        java.sql.ResultSet r2 = p2.executeQuery();
                        salesIds.clear();
                        while (r2.next()) salesIds.add(r2.getLong("sales_id"));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
                currentSalesIndex = salesIds.indexOf(salesId);
                HBox nav = new HBox(8);
                Button prev = new Button("← Prev");
                Button next = new Button("Next →");
                prev.setDisable(currentSalesIndex <= 0);
                next.setDisable(currentSalesIndex < 0 || currentSalesIndex >= salesIds.size() - 1);
                prev.setOnAction(ev -> {
                    if (currentSalesIndex > 0) {
                        long sid = salesIds.get(currentSalesIndex - 1);
                        currentSalesIndex--;
                        showInvoice(sid);
                    }
                });
                next.setOnAction(ev -> {
                    if (currentSalesIndex >= 0 && currentSalesIndex < salesIds.size() - 1) {
                        long sid = salesIds.get(currentSalesIndex + 1);
                        currentSalesIndex++;
                        showInvoice(sid);
                    }
                });
                nav.getChildren().addAll(prev, next);
                root.getChildren().add(nav);
            }
            // ensure invoice view has the standard footer
            mainContent.getChildren().setAll(createScrollable(root));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showUpdateCustomerDialog(Customer c) {
        Stage s = new Stage(); s.setTitle("Update Customer");
        VBox v = new VBox(8); v.setStyle("-fx-padding:16;");
        TextField name = new TextField(c.customerName); name.setPromptText("Customer Name");
        TextField phone = new TextField(c.phoneNo); phone.setPromptText("Phone No");
        Button ok = new Button("Update"); Button cancel = new Button("Cancel");
        v.getChildren().addAll(new Label("Name"), name, new Label("Phone"), phone, new HBox(8, ok, cancel));
        ok.setOnAction(e -> {
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                // Check duplicate customer name or phone among other customers
                try (java.sql.PreparedStatement dup = conn.prepareStatement("SELECT c_id FROM customers WHERE (customer_name = ? OR phone_no = ?) AND c_id <> ? LIMIT 1")) {
                    dup.setString(1, name.getText() == null ? "" : name.getText().trim());
                    dup.setString(2, phone.getText() == null ? "" : phone.getText().trim());
                    dup.setString(3, c.cId);
                    try (java.sql.ResultSet drr = dup.executeQuery()) {
                        if (drr.next()) {
                            new Alert(Alert.AlertType.ERROR, "Customer username or phone number already exists.").showAndWait();
                            return;
                        }
                    }
                }

                java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE customers SET customer_name=?, phone_no=? WHERE c_id=?");
                ps.setString(1, name.getText()); ps.setString(2, phone.getText()); ps.setString(3, c.cId); ps.executeUpdate();
                s.close();
                showCustomerManagement();
            } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to update").showAndWait(); }
        });
        cancel.setOnAction(e -> s.close());
        s.setScene(new javafx.scene.Scene(v, 360, 240)); s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
    }

    private void deleteCustomer(Customer c, TableView<Customer> table) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM customers WHERE c_id=?");
            ps.setString(1, c.cId); ps.executeUpdate();
            table.getItems().remove(c);
        } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to delete").showAndWait(); }
    }

    // Simple Customer holder for table
    public static class Customer {
        public String cId, customerName, phoneNo; public int loyaltyPoints;
        public String getCId() { return cId; }
        public String getCustomerName() { return customerName; }
        public String getPhoneNo() { return phoneNo; }
        public int getLoyaltyPoints() { return loyaltyPoints; }
    }

    private void showCashierManagement() {
        try {
            javafx.scene.Parent p = javafx.fxml.FXMLLoader.load(getClass().getResource("/view/CashierManagement.fxml"));
            // wrap loaded view with pager so admin navigation remains consistent
            VBox container = new VBox(8);
            container.getChildren().addAll(p);
            mainContent.getChildren().setAll(container);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Failed to load Cashier Management view.");
        }
    }

    private TableView<Product> productTable;
    private java.util.List<Product> allProducts = new java.util.ArrayList<>();

    private void showInventoryManagement() {
        VBox inventoryBox = new VBox(20);
        inventoryBox.setStyle("-fx-padding: 40 30 30 30; -fx-background-color: #fff; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, #b8c1ec, 10, 0, 0, 2);");
        inventoryBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search products...");
        searchField.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 15; -fx-pref-width: 260;");
        Button searchBtn = new Button("🔍");
        searchBtn.setStyle("-fx-font-size: 16; -fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
        searchBox.getChildren().addAll(searchField, searchBtn);

        Button addProductBtn = new Button("Add Product");
        addProductBtn.setStyle("-fx-background-color: #4258d0; -fx-text-fill: #fff; -fx-font-family: 'Poppins'; -fx-font-size: 15; -fx-background-radius: 8; -fx-padding: 8 24 8 24;");
        addProductBtn.setOnAction(e -> showAddProductDialog());

        Button refreshBtn = new Button("⟲");
        refreshBtn.setStyle("-fx-font-size: 16; -fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-background-radius: 8; -fx-font-family: 'Poppins';");
        refreshBtn.setOnAction(e -> {
            // refresh product list and reset search box
            try {
                searchField.clear();
                searchField.requestFocus();
            } catch (Exception ignored) {}
            loadAllProducts();
            updateProductTable(allProducts);
        });

    HBox topActions = new HBox(20, searchBox, addProductBtn, refreshBtn);
    topActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    topActions.setStyle("-fx-padding: 0 0 20 0;");

    // product export buttons
    Button prodCsv = new Button("Export CSV");
    Button prodPdf = new Button("Export PDF");
    Button prodXlsx = new Button("Export XLSX");
    topActions.getChildren().addAll(prodCsv, prodPdf, prodXlsx);

        productTable = new TableView<>();
        productTable.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14;");
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPrefHeight(600);
    // allow the table to grow in its container
    javafx.scene.layout.VBox.setVgrow(productTable, javafx.scene.layout.Priority.ALWAYS);
    // Show a friendly placeholder when there are no products
    productTable.setPlaceholder(new javafx.scene.control.Label("No products available"));

        TableColumn<Product, String> colId = new TableColumn<>("Product ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        TableColumn<Product, String> colName = new TableColumn<>("Product Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<Product, String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
    TableColumn<Product, Double> colPrice = new TableColumn<>("Selling Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Product, Double> colTotal = new TableColumn<>("Total Amount");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        // Optional: format as currency with 2 decimal places
        colTotal.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", value));
                }
            }
        });
    TableColumn<Product, Double> colCost = new TableColumn<>("Cost Price");
        colCost.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
    TableColumn<Product, Integer> colStock = new TableColumn<>("Stock Qty");
    colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        TableColumn<Product, String> colUnit = new TableColumn<>("Unit");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Product, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final Button genBtn = new Button("Generate");
            private final HBox box = new HBox(8);

            {
                updateBtn.setStyle("-fx-background-color: #b8c1ec; -fx-text-fill: #232946; -fx-font-family: 'Poppins'; -fx-font-size: 13; -fx-background-radius: 8; -fx-padding: 4 12 4 12;");
                deleteBtn.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-font-family: 'Poppins'; -fx-font-size: 13; -fx-background-radius: 8; -fx-padding: 4 12 4 12;");
                genBtn.setStyle("-fx-background-color: #8fe3a7; -fx-text-fill: #232946; -fx-font-family: 'Poppins'; -fx-font-size: 13; -fx-background-radius: 8; -fx-padding: 4 12 4 12;");
                box.getChildren().addAll(updateBtn, deleteBtn);
                box.getChildren().add(2, genBtn);
                box.setStyle("-fx-alignment: CENTER_LEFT;");
                updateBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    // Open update dialog directly; any price-based confirmation will be shown inside the dialog when Confirm is clicked
                    showUpdateProductDialog(product);
                });
                deleteBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    a.setHeaderText(null);
                    a.setContentText("Delete product '" + product.productName + "' (" + product.productId + ")?");
                    java.util.Optional<javafx.scene.control.ButtonType> res = a.showAndWait();
                    if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK) deleteProduct(product);
                });
                genBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    try {
                        boolean ok = controller.BarcodeManager.generateAndSaveBarcode(product.productId);
                        if (ok) {
                            // update DB barcode column if empty
                            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                                String sql = "UPDATE Products SET barcode = ? WHERE product_id = ?";
                                java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                                ps.setString(1, product.productId);
                                ps.setString(2, product.productId);
                                ps.executeUpdate();
                            } catch (Exception ex) { ex.printStackTrace(); }
                            // show image in dialog
                            javafx.scene.image.Image img = controller.BarcodeManager.generateBarcodeImage(product.productId);
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                            iv.setPreserveRatio(true); iv.setFitWidth(360);
                            javafx.scene.control.Dialog<Void> d = new javafx.scene.control.Dialog<>();
                            d.setTitle("Barcode for " + product.productName);
                            d.getDialogPane().setContent(iv);
                            d.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
                            d.showAndWait();
                        } else showAlert("Failed to generate barcode.");
                    } catch (Exception ex) { ex.printStackTrace(); showAlert("Error generating barcode."); }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

    javafx.collections.ObservableList<javafx.scene.control.TableColumn<Product, ?>> prodCols = javafx.collections.FXCollections.observableArrayList();
    prodCols.add(colId); prodCols.add(colName); prodCols.add(colCategory); prodCols.add(colPrice); prodCols.add(colTotal); prodCols.add(colCost); prodCols.add(colStock); prodCols.add(colUnit); prodCols.add(colActions);
    productTable.getColumns().setAll(prodCols);

    inventoryBox.getChildren().setAll(topActions, productTable);
    // Wrap inventoryBox with createScrollable so the global footer is appended and layout is consistent
    mainContent.getChildren().setAll(createScrollable(inventoryBox));

        // Load all products from DB and show
        loadAllProducts();
        updateProductTable(allProducts);

        // wire product export handlers
        prodCsv.setOnAction(ev -> {
            try {
                String out = util.InvoiceExporter.resolveAdminExportPath("products.csv");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(out))) {
                    pw.println("Product ID,Product Name,Category,Price,Stock Qty,Unit,Total Amount");
                    for (Product p : productTable.getItems()) {
                        pw.println(String.format("%s,%s,%s,%.2f,%d,%s,%.2f", p.productId.replaceAll(",", ""), p.productName.replaceAll(",", ""), p.category.replaceAll(",", ""), p.price, p.stockQuantity, p.unit.replaceAll(",", ""), p.totalAmount));
                    }
                }
                showAlert("CSV exported to " + new java.io.File(out).getAbsolutePath());
            } catch (Exception ex) { ex.printStackTrace(); showAlert("CSV export failed: " + ex.getMessage()); }
        });
        prodPdf.setOnAction(ev -> {
            // convert productTable to Map backed table and reuse PDF exporter
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("Product ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("product_id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Product Name"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("product_name"))));
            TableColumn<java.util.Map<String,Object>, String> c = new TableColumn<>("Category"); c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("category"))));
            TableColumn<java.util.Map<String,Object>, String> d = new TableColumn<>("Price"); d.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("price"))));
            javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> prodMapCols = javafx.collections.FXCollections.observableArrayList();
            prodMapCols.add(a); prodMapCols.add(b); prodMapCols.add(c); prodMapCols.add(d);
            m.getColumns().setAll(prodMapCols);
            javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
            for (Product p : productTable.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>(); mm.put("product_id", p.productId); mm.put("product_name", p.productName); mm.put("category", p.category); mm.put("price", p.price); rows.add(mm);
            }
            m.setItems(rows);
            exportTableToPdf(m, util.InvoiceExporter.resolveAdminExportPath("products.pdf"));
        });
        prodXlsx.setOnAction(ev -> {
            TableView<java.util.Map<String,Object>> m = new TableView<>();
            TableColumn<java.util.Map<String,Object>, String> a = new TableColumn<>("Product ID"); a.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("product_id"))));
            TableColumn<java.util.Map<String,Object>, String> b = new TableColumn<>("Product Name"); b.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("product_name"))));
            TableColumn<java.util.Map<String,Object>, String> c = new TableColumn<>("Category"); c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("category"))));
            TableColumn<java.util.Map<String,Object>, String> d = new TableColumn<>("Price"); d.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("price"))));
            javafx.collections.ObservableList<javafx.scene.control.TableColumn<java.util.Map<String,Object>, ?>> prodMapCols2 = javafx.collections.FXCollections.observableArrayList();
            prodMapCols2.add(a); prodMapCols2.add(b); prodMapCols2.add(c); prodMapCols2.add(d);
            m.getColumns().setAll(prodMapCols2);
            javafx.collections.ObservableList<java.util.Map<String,Object>> rows = javafx.collections.FXCollections.observableArrayList();
            for (Product p : productTable.getItems()) {
                java.util.Map<String,Object> mm = new java.util.HashMap<>(); mm.put("product_id", p.productId); mm.put("product_name", p.productName); mm.put("category", p.category); mm.put("price", p.price); rows.add(mm);
            }
            m.setItems(rows);
            exportTableToXlsx(m, util.InvoiceExporter.resolveAdminExportPath("products.xlsx"));
        });

        // Search logic
        searchBtn.setOnAction(e -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                updateProductTable(allProducts);
            } else {
                java.util.List<Product> filtered = new java.util.ArrayList<>();
                for (Product p : allProducts) {
                    if (p.productName.toLowerCase().contains(query)) {
                        filtered.add(p);
                    }
                }
                updateProductTable(filtered);
            }
        });
        // Autocomplete suggestions below search box
        javafx.scene.control.ContextMenu suggestions = new javafx.scene.control.ContextMenu();
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            suggestions.getItems().clear();
            if (newVal.isEmpty()) {
                updateProductTable(allProducts);
                suggestions.hide();
            } else {
                java.util.List<Product> filtered = new java.util.ArrayList<>();
                for (Product p : allProducts) {
                    if (p.productName.toLowerCase().contains(newVal.toLowerCase())) {
                        filtered.add(p);
                        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(p.productName);
                        item.setOnAction(ev -> {
                            searchField.setText(p.productName);
                            updateProductTable(java.util.Collections.singletonList(p));
                            suggestions.hide();
                        });
                        suggestions.getItems().add(item);
                    }
                }
                if (!suggestions.getItems().isEmpty()) {
                    javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                    suggestions.show(searchField, bounds.getMinX(), bounds.getMaxY());
                } else {
                    suggestions.hide();
                }
                updateProductTable(filtered);
            }
        });
    }

    private void loadAllProducts() {
        allProducts.clear();
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM Products");
            while (rs.next()) {
                Product p = new Product();
                p.productId = rs.getString("product_id");
                p.productName = rs.getString("product_name");
                p.category = rs.getString("category");
                p.price = rs.getDouble("price");
                p.costPrice = rs.getDouble("cost_price");
                p.stockQuantity = rs.getInt("stock_quantity");
                p.unit = rs.getString("unit");
                p.barcode = rs.getString("barcode");
                // If the DB has a generated column total_amount, read it; otherwise compute
                try {
                    p.totalAmount = rs.getDouble("total_amount");
                } catch (Exception ex) {
                    p.totalAmount = p.price * p.stockQuantity;
                }
                allProducts.add(p);
            }
            System.out.println("Products loaded: " + allProducts.size()); // Log the number of products loaded
            // No fallback sample data; table will show the placeholder when empty
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateProductTable(java.util.List<Product> products) {
        productTable.getItems().setAll(products);
    }

    // Add this method for the Add Product dialog
    private void showAddProductDialog() {
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.setTitle("Add Product");
        VBox dialogVBox = new VBox(15);
        dialogVBox.setStyle("-fx-padding: 24; -fx-background-color: #fff; -fx-background-radius: 12;");
        dialogVBox.setAlignment(javafx.geometry.Pos.CENTER);

        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");

        javafx.scene.control.ComboBox<String> categoryBox = new javafx.scene.control.ComboBox<>();
        categoryBox.getItems().addAll(
            "Fruits & Vegetables",
            "Dairy & Eggs",
            "Bakery & Confectionery",
            "Meat, Poultry & Seafood",
            "Beverages",
            "Snacks & Packaged Foods",
            "Frozen & Ready-to-Eat Foods",
            "Grains, Pulses & Cereals",
            "Cooking Essentials",
            "Personal Care & Hygiene",
            "Health & Wellness",
            "Household Cleaning",
            "Baby & Kids Products",
            "Pet Care",
            "Electronics & Accessories",
            "Home & Kitchen Essentials",
            "Clothing & Apparel",
            "Stationery & Office Supplies",
            "Automotive & Tools",
            "Sports, Fitness & Outdoor"
        );
        categoryBox.setPromptText("Category");

        TextField sellingPriceField = new TextField();
        sellingPriceField.setPromptText("Selling Price (per unit)");
        TextField costPriceField = new TextField();
        costPriceField.setPromptText("Cost Price (per unit)");
        TextField stockQtyField = new TextField();
        stockQtyField.setPromptText("Stock Quantity");

        javafx.scene.control.ComboBox<String> unitBox = new javafx.scene.control.ComboBox<>();
        unitBox.getItems().addAll(
            "pcs (piece)",
            "kg (kilogram)",
            "g (gram)",
            "liter (L)",
            "ml (milliliter)",
            "pack",
            "bottle",
            "can",
            "tube",
            "set",
            "bunch",
            "sachet"
        );
        unitBox.setPromptText("Unit");

        Label barcodeLabel = new Label("Barcode: Will be auto-generated");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle("-fx-background-color: #4258d0; -fx-text-fill: #fff; -fx-font-family: 'Poppins'; -fx-font-size: 15; -fx-background-radius: 8;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #eebbc3; -fx-text-fill: #232946; -fx-font-family: 'Poppins'; -fx-font-size: 15; -fx-background-radius: 8;");
        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);

        confirmBtn.setOnAction(ev -> {
            String name = nameField.getText();
            String category = categoryBox.getValue();
            String sellingPrice = sellingPriceField.getText();
            String costPrice = costPriceField.getText();
            String stockQty = stockQtyField.getText();
            String unit = unitBox.getValue();
            if (name.isEmpty() || category == null || sellingPrice.isEmpty() || costPrice.isEmpty() || stockQty.isEmpty() || unit == null) {
                showAlert("Please fill all fields.");
                return;
            }
            // Confirmation: if cost price is greater than selling price, warn the user
            try {
                double sp = Double.parseDouble(sellingPrice);
                double cp = Double.parseDouble(costPrice);
                if (cp > sp) {
                    javafx.scene.control.Alert warn = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    warn.setHeaderText("Cost price is greater than selling price");
                    warn.setContentText("The cost price (" + cp + ") is higher than the selling price (" + sp + "). Do you want to continue?");
                    java.util.Optional<javafx.scene.control.ButtonType> r = warn.showAndWait();
                    if (!r.isPresent() || r.get() != javafx.scene.control.ButtonType.OK) {
                        return;
                    }
                }
            } catch (NumberFormatException ignored) {}
            try {
                String productId = "P" + String.format("%05d", new java.util.Random().nextInt(100000));
                String barcode = productId;
                java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
                // Insert into DB
                try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO Products (product_id, product_name, category, price, cost_price, stock_quantity, unit, barcode, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, productId);
                    stmt.setString(2, name);
                    stmt.setString(3, category);
                    stmt.setBigDecimal(4, new java.math.BigDecimal(sellingPrice));
                    stmt.setBigDecimal(5, new java.math.BigDecimal(costPrice));
                    stmt.setInt(6, Integer.parseInt(stockQty));
                    stmt.setString(7, unit);
                    stmt.setString(8, barcode);
                    stmt.setTimestamp(9, now);
                    stmt.setTimestamp(10, now);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        showAlert("Product added successfully!\nBarcode: " + barcode);
                        dialogStage.close();
                        loadAllProducts();
                        updateProductTable(allProducts);
                    } else {
                        showAlert("Failed to add product. Try again.");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error adding product. Please check your input and DB connection.");
            }
        });
        cancelBtn.setOnAction(ev -> dialogStage.close());

        dialogVBox.getChildren().addAll(
            new Label("Product Name:"), nameField,
            new Label("Category:"), categoryBox,
            new Label("Selling Price:"), sellingPriceField,
            new Label("Cost Price:"), costPriceField,
            new Label("Stock Quantity:"), stockQtyField,
            new Label("Unit:"), unitBox,
            barcodeLabel,
            buttonBox
        );

        javafx.scene.Scene dialogScene = new javafx.scene.Scene(dialogVBox, 340, 560);
        dialogStage.setScene(dialogScene);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.showAndWait();
    }

    private void deleteProduct(Product product) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM Products WHERE product_id = ?";
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, product.productId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                showAlert("Product deleted successfully!");
                loadAllProducts();
                updateProductTable(allProducts);
            } else {
                showAlert("Failed to delete product.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error deleting product.");
        }
    }

    private void showUpdateProductDialog(Product product) {
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.setTitle("Update Product");
        VBox dialogVBox = new VBox(15);
        dialogVBox.setStyle("-fx-padding: 24; -fx-background-color: #fff; -fx-background-radius: 12;");
        dialogVBox.setAlignment(javafx.geometry.Pos.CENTER);

        TextField nameField = new TextField(product.productName);
        javafx.scene.control.ComboBox<String> categoryBox = new javafx.scene.control.ComboBox<>();
        categoryBox.getItems().addAll(
            "Fruits & Vegetables",
            "Dairy & Eggs",
            "Bakery & Confectionery",
            "Meat, Poultry & Seafood",
            "Beverages",
            "Snacks & Packaged Foods",
            "Frozen & Ready-to-Eat Foods",
            "Grains, Pulses & Cereals",
            "Cooking Essentials",
            "Personal Care & Hygiene",
            "Health & Wellness",
            "Household Cleaning",
            "Baby & Kids Products",
            "Pet Care",
            "Electronics & Accessories",
            "Home & Kitchen Essentials",
            "Clothing & Apparel",
            "Stationery & Office Supplies",
            "Automotive & Tools",
            "Sports, Fitness & Outdoor"
        );
        categoryBox.setValue(product.category);
        TextField sellingPriceField = new TextField(String.valueOf(product.price));
        TextField costPriceField = new TextField(String.valueOf(product.costPrice));
        TextField stockQtyField = new TextField(String.valueOf(product.stockQuantity));
        javafx.scene.control.ComboBox<String> unitBox = new javafx.scene.control.ComboBox<>();
        unitBox.getItems().addAll(
            "pcs (piece)",
            "kg (kilogram)",
            "g (gram)",
            "liter (L)",
            "ml (milliliter)",
            "pack",
            "bottle",
            "can",
            "tube",
            "set",
            "bunch",
            "sachet"
        );
        unitBox.setValue(product.unit);
        Label barcodeLabel = new Label("Barcode: " + product.barcode);
        Button confirmBtn = new Button("Confirm");
        Button cancelBtn = new Button("Cancel");
        HBox buttonBox = new HBox(20, confirmBtn, cancelBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        confirmBtn.setOnAction(ev -> {
            String name = nameField.getText();
            String category = categoryBox.getValue();
            String sellingPrice = sellingPriceField.getText();
            String costPrice = costPriceField.getText();
            String stockQty = stockQtyField.getText();
            String unit = unitBox.getValue();
            if (name.isEmpty() || category == null || sellingPrice.isEmpty() || costPrice.isEmpty() || stockQty.isEmpty() || unit == null) {
                showAlert("Please fill all fields.");
                return;
            }
            // Post-edit confirmation: only warn if user changed either selling or cost price AND the new cost > new selling price
            boolean shouldWarn = false;
            try {
                double spNew = Double.parseDouble(sellingPrice);
                double cpNew = Double.parseDouble(costPrice);
                double spOld = product.price;
                double cpOld = product.costPrice;
                // consider changed when absolute difference > small epsilon to avoid floating point noise
                double eps = 1e-6;
                boolean changed = Math.abs(spNew - spOld) > eps || Math.abs(cpNew - cpOld) > eps;
                if (changed && cpNew > spNew) {
                    shouldWarn = true;
                }
            } catch (NumberFormatException ignored) {}

            if (shouldWarn) {
                javafx.scene.control.Alert warn = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                warn.setHeaderText("Cost price is greater than selling price");
                warn.setContentText("The new cost price you entered is higher than the new selling price. Do you want to continue and save these changes?");
                java.util.Optional<javafx.scene.control.ButtonType> r = warn.showAndWait();
                if (!r.isPresent() || r.get() != javafx.scene.control.ButtonType.OK) {
                    return;
                }
            }

            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                String sql = "UPDATE Products SET product_name=?, category=?, price=?, cost_price=?, stock_quantity=?, unit=?, barcode=? WHERE product_id=?";
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, category);
                stmt.setBigDecimal(3, new java.math.BigDecimal(sellingPrice));
                stmt.setBigDecimal(4, new java.math.BigDecimal(costPrice));
                stmt.setInt(5, Integer.parseInt(stockQty));
                stmt.setString(6, unit);
                stmt.setString(7, product.barcode);
                stmt.setString(8, product.productId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    showAlert("Product updated successfully!");
                    dialogStage.close();
                    loadAllProducts();
                    updateProductTable(allProducts);
                } else {
                    showAlert("Failed to update product.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error updating product.");
            }
        });
        cancelBtn.setOnAction(ev -> dialogStage.close());
        dialogVBox.getChildren().addAll(
            new Label("Product Name:"), nameField,
            new Label("Category:"), categoryBox,
            new Label("Selling Price:"), sellingPriceField,
            new Label("Cost Price:"), costPriceField,
            new Label("Stock Quantity:"), stockQtyField,
            new Label("Unit:"), unitBox,
            barcodeLabel,
            buttonBox
        );
        javafx.scene.Scene dialogScene = new javafx.scene.Scene(dialogVBox, 340, 560);
        dialogStage.setScene(dialogScene);
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.showAndWait();
    }

    // Show alert dialog
    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper: wrap any node in a vertical ScrollPane for consistent admin pages
    private javafx.scene.control.ScrollPane createScrollable(javafx.scene.Node content) {
        // ensure window is maximized for consistent full-screen layout
        ensureMaximized();
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
    // enforce white background and subtle border so inner content is readable
    sp.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff; -fx-border-color: #dddddd; -fx-border-width: 1;");
    sp.getStyleClass().add("dashboard-scrollpane");
        // append a global footer to every scrollable page so pages look professional and consistent
        try {
            if (content instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane p = (javafx.scene.layout.Pane) content;
                // avoid adding duplicate footer nodes
                boolean hasFooter = p.getChildrenUnmodifiable().stream().filter(n -> n instanceof javafx.scene.layout.HBox).map(n -> (javafx.scene.layout.HBox)n).anyMatch(h -> h.getChildren().stream().anyMatch(c -> c instanceof javafx.scene.control.Label && ((javafx.scene.control.Label)c).getText().contains("© QuickMart")));
                if (!hasFooter) p.getChildren().add(createCopyrightNode());
            }
        } catch (Exception ignored) {}
        return sp;
    }

    // Ensure legend label text inside a chart is readable (recursively sets Label textFill)
    private void ensureLegendTextVisible(javafx.scene.Node chartNode) {
        try {
            if (chartNode == null) return;
            if (chartNode instanceof javafx.scene.Parent) {
                javafx.scene.Parent p = (javafx.scene.Parent) chartNode;
                try { p.applyCss(); p.layout(); } catch (Exception ignored) {}
                for (javafx.scene.Node legend : p.lookupAll(".chart-legend")) {
                    setLabelTextColorRecursive(legend, javafx.scene.paint.Color.web("#333333"));
                    // also color any Text nodes inside legend items
                    for (javafx.scene.Node it : ((javafx.scene.Parent) legend).getChildrenUnmodifiable()) {
                        it.lookupAll("*").forEach(n -> {
                            try { if (n instanceof javafx.scene.text.Text) ((javafx.scene.text.Text)n).setFill(javafx.scene.paint.Color.web("#333333")); } catch (Exception ignored) {}
                        });
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void setLabelTextColorRecursive(javafx.scene.Node node, javafx.scene.paint.Color color) {
        try {
            if (node instanceof javafx.scene.control.Label) {
                ((javafx.scene.control.Label) node).setTextFill(color);
            }
            if (node instanceof javafx.scene.text.Text) {
                ((javafx.scene.text.Text) node).setFill(color);
            }
            if (node instanceof javafx.scene.Parent) {
                for (javafx.scene.Node c : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) setLabelTextColorRecursive(c, color);
            }
        } catch (Exception ignored) {}
    }

    // Ensure application window stays maximized (not fullscreen) when pages change
    private void ensureMaximized() {
        try {
            if (mainContent != null && mainContent.getScene() != null) {
                javafx.stage.Window w = mainContent.getScene().getWindow();
                if (w instanceof javafx.stage.Stage) {
                    ((javafx.stage.Stage) w).setMaximized(true);
                }
            }
        } catch (Exception ignored) {}
    }

    // Set the active sidebar button by toggling the CSS class on sidebar children
    private void setActiveSidebarButton(javafx.scene.control.Button btn) {
        try {
            if (sidebar == null) return;
            for (javafx.scene.Node n : sidebar.getChildren()) {
                if (n instanceof javafx.scene.control.Button) ((javafx.scene.control.Button) n).getStyleClass().removeAll("active-sidebar-btn");
            }
            if (btn != null) btn.getStyleClass().add("active-sidebar-btn");
        } catch (Exception ignored) {}
    }

    // Toggle POS-specific controls (product search and scan button) if present in the scene.
    // Admin doesn't have these controls directly, so we look them up in the scene by id and set visible/managed.
    private void setPosControlsVisible(boolean visible) {
        try {
            if (mainContent == null || mainContent.getScene() == null) return;
            javafx.scene.Parent root = mainContent.getScene().getRoot();
            if (root == null) return;
            try {
                javafx.scene.Node pf = root.lookup("#productSearchField");
                if (pf != null) { pf.setVisible(visible); pf.setManaged(visible); }
            } catch (Exception ignored) {}
            try {
                javafx.scene.Node sb = root.lookup("#scanBtn");
                if (sb != null) { sb.setVisible(visible); sb.setManaged(visible); }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    // CSV export with formatting and main_sales-aware column ordering
    private void exportTableToCsv(TableView<java.util.Map<String,Object>> table, String filename) {
        try {
            // If caller passed an absolute path, use it; otherwise resolve into admin exports dir
            java.io.File maybe = new java.io.File(filename);
            String outPath = maybe.isAbsolute() ? filename : util.InvoiceExporter.resolveAdminExportPath(filename);
            java.io.File f = new java.io.File(outPath);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(f)) {
                // detect if rows look like main_sales rows
                boolean isMainSales = false;
                if (!table.getItems().isEmpty()) {
                    // detect main_sales by scanning rows for summary keys (sales_total, grand_total, caption)
                    for (java.util.Map<String,Object> sample : table.getItems()) {
                        if (sample == null) continue;
                        if (sample.containsKey("sales_id") || sample.containsKey("grand_total") || sample.containsKey("sales_total") || sample.containsKey("caption")) { isMainSales = true; break; }
                    }
                }
                String[] headers;
                if (isMainSales) {
                    headers = new String[]{"sales_id","sale_time","cashier_id","sales_total","tax","discount","grand_total","customer_id","customer_name","caption"};
                } else {
                    // fallback to column texts
                    java.util.List<String> h = new java.util.ArrayList<>();
                    for (TableColumn<?,?> c : table.getColumns()) h.add(c.getText());
                    headers = h.toArray(new String[0]);
                }
                pw.println(String.join(",", headers));
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (java.util.Map<String,Object> row : table.getItems()) {
                    // skip rows that are completely empty (all headers map to null/empty)
                    boolean allEmpty = true;
                    java.util.List<String> vals = new java.util.ArrayList<>();
                    for (String h : headers) {
                        Object v = row.get(h);
                        if (v == null) v = getRowValue(row, h);
                        // if still null, try TableColumn.getCellData to pick up formatted cell values
                        if (v == null) {
                            for (TableColumn<?,?> col : table.getColumns()) {
                                if (h.equals(col.getText())) {
                                    try { v = col.getCellData(table.getItems().indexOf(row)); } catch (Exception ignored) { v = null; }
                                    break;
                                }
                            }
                        }
                        String out = "";
                        if (v == null) out = "";
                        else if (v instanceof Number) out = String.format("%.2f", ((Number)v).doubleValue());
                        else if (v instanceof java.sql.Timestamp) out = dtf.format(((java.sql.Timestamp)v).toLocalDateTime());
                        else out = v.toString();
                        // remove commas to keep CSV structure simple
                        out = out.replaceAll(",", "");
                        vals.add(out);
                        if (out != null && !out.trim().isEmpty()) allEmpty = false;
                    }
                    if (!allEmpty) pw.println(String.join(",", vals));
                }
                // If table contains a product list with a trailing summary map (sales_total/grand_total/caption), print summary lines
                try {
                    boolean hasSummary = false;
                    java.util.Map<String,Object> summaryRow = null;
                    for (java.util.Map<String,Object> row : table.getItems()) {
                        if (row == null) continue;
                        if (row.containsKey("sales_total") || row.containsKey("grand_total") || row.containsKey("caption")) { hasSummary = true; summaryRow = row; break; }
                    }
                    if (hasSummary && summaryRow != null) {
                        pw.println();
                        Object st = getRowValue(summaryRow, "sales_total"); if (st == null) st = summaryRow.get("sales_total");
                        Object tax = getRowValue(summaryRow, "tax"); if (tax == null) tax = summaryRow.get("tax");
                        Object disc = getRowValue(summaryRow, "discount"); if (disc == null) disc = summaryRow.get("discount");
                        Object gt = getRowValue(summaryRow, "grand_total"); if (gt == null) gt = summaryRow.get("grand_total");
                        Object cap = getRowValue(summaryRow, "caption"); if (cap == null) cap = summaryRow.get("caption");
                        pw.println("Sales Total," + (st == null ? "0.00" : formatMoney(st)));
                        pw.println("Tax," + (tax == null ? "0.00" : formatMoney(tax)));
                        pw.println("Discount," + (disc == null ? "0.00" : formatMoney(disc)));
                        pw.println("Grand Total," + (gt == null ? "0.00" : formatMoney(gt)));
                        if (cap != null && cap.toString().trim().length() > 0) pw.println("Caption," + cap.toString().replaceAll(",", ""));
                    }
                } catch (Exception ignored) {}
            }
            showAlert("CSV exported to " + f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("CSV export failed: " + ex.getMessage());
        }
    }

    // Helper to safely format money-like objects for table cells
    private static String formatMoney(Object o) {
        if (o == null) return "0.00";
        try {
            if (o instanceof Number) return String.format("%.2f", ((Number)o).doubleValue());
            if (o instanceof java.math.BigDecimal) return ((java.math.BigDecimal)o).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            return String.format("%.2f", Double.parseDouble(o.toString()));
        } catch (Exception ex) { return "0.00"; }
    }

    // formatMoney with currency prefix
    private static String formatMoneyWithCurrency(Object o) {
        String n = formatMoney(o);
        return "Rs " + n;
    }

    // Helper to read a double from ResultSet without throwing when column is null or missing
    private static double safeGetDouble(java.sql.ResultSet rs, String col) {
        try {
            double v = rs.getDouble(col);
            if (rs.wasNull()) return 0.0;
            return v;
        } catch (Exception ex) { return 0.0; }
    }

    // Helper to safely read a string column (returns null if column missing or value null)
    private static String safeGetString(java.sql.ResultSet rs, String col) {
        try {
            try {
                String v = rs.getString(col);
                if (rs.wasNull()) return null;
                return v;
            } catch (java.sql.SQLException sqle) {
                // column doesn't exist
                return null;
            }
        } catch (Exception ex) { return null; }
    }

    // Helper to return an object suitable for formatMoney() from a ResultSet column (BigDecimal/Double/0.0)
    private static Object safeGetNumberForFormatting(java.sql.ResultSet rs, String col) {
        try {
            try {
                java.math.BigDecimal bd = rs.getBigDecimal(col);
                if (bd != null) return bd;
            } catch (Exception ignored) {}
            double v = rs.getDouble(col);
            if (rs.wasNull()) return 0.0;
            return v;
        } catch (Exception ex) { return 0.0; }
    }

    // XLSX export using Apache POI with formatting and main_sales ordering
    private void exportTableToXlsx(TableView<java.util.Map<String,Object>> table, String filename) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Export");
            // detect main_sales
            boolean isMainSales = false;
            if (!table.getItems().isEmpty()) {
                java.util.Map<String,Object> sample = table.getItems().get(0);
                isMainSales = sample.containsKey("sales_id") || sample.containsKey("grand_total");
            }
            String[] headers;
                if (isMainSales) {
                    headers = new String[]{"sales_id","sale_time","cashier_id","sales_total","tax","discount","grand_total","customer_id","customer_name","caption"};
            } else {
                java.util.List<String> h = new java.util.ArrayList<>();
                for (TableColumn<?,?> c : table.getColumns()) h.add(c.getText());
                headers = h.toArray(new String[0]);
            }
            // header row
            org.apache.poi.ss.usermodel.Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hr.createCell(i).setCellValue(headers[i]);
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            org.apache.poi.ss.usermodel.CellStyle moneyStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat df = wb.createDataFormat();
            moneyStyle.setDataFormat(df.getFormat("#,##0.00"));
            int r = 1;
            for (java.util.Map<String,Object> row : table.getItems()) {
                org.apache.poi.ss.usermodel.Row xr = sheet.createRow(r++);
                    for (int c = 0; c < headers.length; c++) {
                    String h = headers[c];
                    Object v = row.get(h);
                    if (v == null) v = getRowValue(row, h);
                    // fallback to TableColumn.getCellData for non-map tables
                    if (v == null) {
                        for (TableColumn<?,?> col : table.getColumns()) {
                            if (h.equals(col.getText())) { try { v = col.getCellData(table.getItems().indexOf(row)); } catch (Exception ignored) { v = null; } break; }
                        }
                    }
                    org.apache.poi.ss.usermodel.Cell cell = xr.createCell(c);
                    if (v == null) { cell.setCellValue(""); }
                    else if (v instanceof Number) { cell.setCellValue(((Number)v).doubleValue()); cell.setCellStyle(moneyStyle); }
                    else if (v instanceof java.sql.Timestamp) { cell.setCellValue(dtf.format(((java.sql.Timestamp)v).toLocalDateTime())); }
                    else { cell.setCellValue(v.toString()); }
                }
            }
            // Append summary rows if a summary map exists in items
            try {
                java.util.Map<String,Object> summaryRow = null;
                for (java.util.Map<String,Object> row : table.getItems()) { if (row == null) continue; if (row.containsKey("sales_total") || row.containsKey("grand_total") || row.containsKey("caption")) { summaryRow = row; break; } }
                if (summaryRow != null) {
                    // leave an empty row
                    r++;
                    org.apache.poi.ss.usermodel.Row s1 = sheet.createRow(r++); s1.createCell(0).setCellValue("Sales Total"); s1.createCell(1).setCellValue(formatMoney(getRowValue(summaryRow, "sales_total")));
                    org.apache.poi.ss.usermodel.Row s2 = sheet.createRow(r++); s2.createCell(0).setCellValue("Tax"); s2.createCell(1).setCellValue(formatMoney(getRowValue(summaryRow, "tax")));
                    org.apache.poi.ss.usermodel.Row s3 = sheet.createRow(r++); s3.createCell(0).setCellValue("Discount"); s3.createCell(1).setCellValue(formatMoney(getRowValue(summaryRow, "discount")));
                    org.apache.poi.ss.usermodel.Row s4 = sheet.createRow(r++); s4.createCell(0).setCellValue("Grand Total"); s4.createCell(1).setCellValue(formatMoney(getRowValue(summaryRow, "grand_total")));
                    Object cap = getRowValue(summaryRow, "caption"); if (cap != null && cap.toString().trim().length() > 0) { org.apache.poi.ss.usermodel.Row sc = sheet.createRow(r++); sc.createCell(0).setCellValue("Caption"); sc.createCell(1).setCellValue(cap.toString()); }
                }
            } catch (Exception ignored) {}
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            java.io.File maybe = new java.io.File(filename);
            String outPath = maybe.isAbsolute() ? filename : util.InvoiceExporter.resolveAdminExportPath(filename);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outPath)) { wb.write(fos); }
            showAlert("XLSX exported to " + new java.io.File(outPath).getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("XLSX export failed: " + ex.getMessage());
        }
    }

    // PDF export using iText with formatted columns and main_sales awareness
    private void exportTableToPdf(TableView<java.util.Map<String,Object>> table, String filename) {
    java.io.File maybe = new java.io.File(filename);
    String outPath = maybe.isAbsolute() ? filename : util.InvoiceExporter.resolveAdminExportPath(filename);
    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outPath)) {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, fos);
            doc.open();
            boolean isMainSales = false;
            if (!table.getItems().isEmpty()) {
                java.util.Map<String,Object> sample = table.getItems().get(0);
                isMainSales = sample.containsKey("sales_id") || sample.containsKey("grand_total");
            }
            String[] headers;
            if (isMainSales) headers = new String[]{"sales_id","sale_time","cashier_id","sales_total","tax","discount","grand_total","customer_id","customer_name","caption"};
            else {
                java.util.List<String> h = new java.util.ArrayList<>();
                for (TableColumn<?,?> c : table.getColumns()) h.add(c.getText());
                headers = h.toArray(new String[0]);
            }
            com.itextpdf.text.pdf.PdfPTable pt = new com.itextpdf.text.pdf.PdfPTable(headers.length);
            for (String h : headers) pt.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(h)));
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (java.util.Map<String,Object> row : table.getItems()) {
                    for (String h : headers) {
                    Object v = row.get(h);
                    if (v == null) v = getRowValue(row, h);
                    if (v == null) {
                        for (TableColumn<?,?> col : table.getColumns()) {
                            if (h.equals(col.getText())) { try { v = col.getCellData(table.getItems().indexOf(row)); } catch (Exception ignored) { v = null; } break; }
                        }
                    }
                    String s = "";
                    if (v == null) s = "";
                    else if (v instanceof Number) s = String.format("%.2f", ((Number)v).doubleValue());
                    else if (v instanceof java.sql.Timestamp) s = dtf.format(((java.sql.Timestamp)v).toLocalDateTime());
                    else s = v.toString();
                    pt.addCell(new com.itextpdf.text.Phrase(s));
                }
            }
                doc.add(pt);
                // add summary totals if present in the table items
                try {
                    java.util.Map<String,Object> summaryRow = null;
                    for (java.util.Map<String,Object> row : table.getItems()) { if (row == null) continue; if (row.containsKey("sales_total") || row.containsKey("grand_total") || row.containsKey("caption")) { summaryRow = row; break; } }
                    if (summaryRow != null) {
                        doc.add(new com.itextpdf.text.Paragraph(" "));
                        doc.add(new com.itextpdf.text.Paragraph("Sales Total: " + formatMoney(getRowValue(summaryRow, "sales_total"))));
                        doc.add(new com.itextpdf.text.Paragraph("Tax: " + formatMoney(getRowValue(summaryRow, "tax"))));
                        doc.add(new com.itextpdf.text.Paragraph("Discount: " + formatMoney(getRowValue(summaryRow, "discount"))));
                        doc.add(new com.itextpdf.text.Paragraph("Grand Total: " + formatMoney(getRowValue(summaryRow, "grand_total"))));
                        Object cap = getRowValue(summaryRow, "caption"); if (cap != null && cap.toString().trim().length() > 0) doc.add(new com.itextpdf.text.Paragraph("Caption: " + cap.toString()));
                    }
                } catch (Exception ignored) {}
                doc.close();
            showAlert("PDF exported to " + new java.io.File(outPath).getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("PDF export failed: " + ex.getMessage());
        }
    }

    // Normalize helper to match header names to map keys robustly
    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private Object getRowValue(java.util.Map<String,Object> row, String header) {
        if (row == null) return null;
        // try direct header variations
        if (row.containsKey(header)) return row.get(header);
        String h1 = header.toLowerCase().replaceAll(" ", "_");
        if (row.containsKey(h1)) return row.get(h1);
        // normalized matching against all keys
        String nh = normalize(header);
        for (String k : row.keySet()) {
            if (normalize(k).equals(nh)) return row.get(k);
        }
        // last resort: try common names
        if (row.containsKey("sales_id")) return row.get("sales_id");
        if (row.containsKey("cashier_id")) return row.get("cashier_id");
        if (row.containsKey("sale_time")) return row.get("sale_time");
        return null;
    }

    private void exportCustomerTableToCsv(TableView<Customer> table, String filename) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(util.InvoiceExporter.resolveAdminExportPath(filename)))) {
            pw.println("Customer ID,Name,Phone,Loyalty Points");
            for (Customer c : table.getItems()) {
                pw.println(String.format("%s,%s,%s,%d", c.cId, c.customerName.replaceAll(",", ""), c.phoneNo.replaceAll(",", ""), c.loyaltyPoints));
            }
            showAlert("CSV exported to " + new java.io.File(util.InvoiceExporter.resolveAdminExportPath(filename)).getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("CSV export failed: " + ex.getMessage());
        }
    }

    // Invoice table builder moved to util.InvoiceExporter which reads main_sales and sales directly when exporting.

    // Build an export filename suffix for a sales invoice based on current role or main_sales cashier/customer
    private String computeExportSuffixForSales(long salesId) {
        try {
            // prefer explicit role label (admin)
            if (roleLabel != null && roleLabel.getText() != null) {
                String t = roleLabel.getText().toLowerCase();
                if (t.contains("admin")) return "_admin";
                if (t.contains("cashier")) {
                    // attempt to extract cashier name from main_sales
                    try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                        java.sql.PreparedStatement ps = conn.prepareStatement("SELECT cashier_id, customer_name FROM main_sales WHERE sales_id = ? LIMIT 1");
                        ps.setLong(1, salesId);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            String cid = safeGetString(rs, "cashier_id");
                            if (cid != null && !cid.isEmpty()) return "_" + sanitizeForFilename(cid);
                            String cname = safeGetString(rs, "customer_name"); if (cname != null && !cname.isEmpty()) return "_" + sanitizeForFilename(cname);
                        }
                    } catch (Exception ignored) {}
                    return "_cashier";
                }
            }
            // fallback: try to read customer_name from main_sales
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT customer_name, cashier_id FROM main_sales WHERE sales_id = ? LIMIT 1");
                ps.setLong(1, salesId);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String cname = safeGetString(rs, "customer_name"); if (cname != null && !cname.isEmpty()) return "_" + sanitizeForFilename(cname);
                    String cid = safeGetString(rs, "cashier_id"); if (cid != null && !cid.isEmpty()) return "_" + sanitizeForFilename(cid);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return "";
    }

    private String sanitizeForFilename(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9_\\-]", "_").trim();
    }

    // Dummy Product class for table structure
    public static class Product {
    private String productId, productName, category, unit, barcode;
    private double price, costPrice;
    private int stockQuantity;
    private double totalAmount;
    // Removed unused fields createdAt, updatedAt

        // Public getters required by PropertyValueFactory
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategory() { return category; }
        public String getUnit() { return unit; }
        public String getBarcode() { return barcode; }
        public double getPrice() { return price; }
        public double getCostPrice() { return costPrice; }
        public int getStockQuantity() { return stockQuantity; }
    public double getTotalAmount() { return totalAmount; }
        // Default constructor
        public Product() {}
    }

    // loadDashboardData removed: dashboard content is generated in showDashboardHome()

    // Quietly reference export helpers so static analysis marks them as used
    private void touchExportHelpers() {
        try {
            Runnable r1 = () -> exportTableToCsv(new TableView<>(), "");
            Runnable r2 = () -> exportCustomerTableToCsv(new TableView<>(), "");
            // reference pager creation
            HBox p = createAdminPager();
            // no-op use
            if (r1.hashCode() == r2.hashCode() || p.hashCode() == r1.hashCode()) System.out.println();
        } catch (Exception ignored) {}
    }

    // Helpful pages: Guidelines (loyalty rules), Privacy Policy and About Us
    private void showGuidelinesPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("Loyalty & Guidelines"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label();
        body.setWrapText(true);
    body.setText("Loyalty Points mechanism:\n\n1 point = Rs 100 of items total (before tax/discount).\nIf a customer's existing loyalty points are >= 500 at the time of payment, the system will automatically redeem 500 points to apply an immediate Rs 1500 discount to the sale and record a caption \"Loyalty Point redeemed!(Discount Added: Rs 1500)\" on the invoice.\nEarned points from the current sale are computed as floor(items_total / 100) and added to the customer's balance after redemption is applied.\n\nDiscount overflow rule:\nIf a discount (manual or computed) exceeds the sum of the sale's items total plus tax (i.e., discount > sales_total + tax), the system will cap the effective discount so the payable amount never becomes negative. Any excess discount amount (discount - (sales_total + tax)) will be converted into loyalty points at the standard earn rate (1 point per Rs 100) and recorded on the invoice as a caption such as \"Excess discount converted to loyalty points\". This ensures payable amounts remain non-negative while preserving customer value via loyalty points.\n\nPlease ensure customers are registered with a valid phone number to accrue or redeem points.");
        box.getChildren().addAll(title, body);
        // Footer credit
        box.getChildren().add(createCopyrightNode());
        mainContent.getChildren().setAll(createScrollable(box));
    }

    private void showPrivacyPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("Privacy Policy"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label();
        body.setWrapText(true);
    body.setText("At QuickMart, we take your privacy seriously.\nWe are committed to securely handling your data and ensuring confidentiality.\n\nYour information is protected, and we continuously monitor our security measures to safeguard against unauthorized access, disclosure, or misuse.\n");
        box.getChildren().addAll(title, body);
        box.getChildren().add(createCopyrightNode());
        mainContent.getChildren().setAll(createScrollable(box));
    }

    private void showAboutPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("About Us"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label();
        body.setWrapText(true);
    body.setText("Welcome to QuickMart.\n\nOur Mission:\nRevolutionize retail point-of-sale experiences with a modern JavaFX desktop app, leveraging JDBC for persistence, barcode scanning, PDF/XLSX/CSV export, and a simple loyalty program.\n\nTechnologies used: Java, JavaFX, JDBC. Key libraries include ZXing (barcode generation), Apache POI (XLSX), and iText (PDF).\n\nTeam:\n- Prajjwal Maharjan (Lead Developer)\n- Rabin Pulami Magar\n- Durga Budha\n");
        box.getChildren().addAll(title, body);
        box.getChildren().add(createCopyrightNode());
        mainContent.getChildren().setAll(createScrollable(box));
    }

    private HBox createCopyrightNode() {
        HBox foot = new HBox(); foot.setStyle("-fx-alignment:center-right; -fx-padding:12 8 8 8;");
        Label copy = new Label("© QuickMart 2025. All rights reserved. Made by Prajjwal Maharjan Team");
        copy.setStyle("-fx-text-fill:#666; -fx-font-size:11;");
        foot.getChildren().add(copy);
        return foot;
    }
}
