package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
// javafx.stage.Stage import removed (unused)

public class CashierDashboardController {
    @FXML private Button newBillingBtn;
    @FXML private Button viewTransBtn;
    @FXML private Button exitBtn;
    @FXML private Button genBarcodeBtn;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button confirmBtn;
    @FXML private Button cancelTxnBtn;
    @FXML private TextField productSearchField;
    @FXML private Button scanBtn;
    @FXML private Button addToTxnBtn;
    @FXML private Label txnHeader;
    @FXML private TableView<TxnRow> txnTable;
    @FXML private StackPane rightStack;
    @FXML private VBox welcomePane;
    @FXML private VBox txnPane;
    @FXML private ImageView logoImageView;
    @FXML private ImageView profileImageView;
    @FXML private ImageView topProfileImageView;
    @FXML private Label topCashierName;
    @FXML private Button logoutBtn;
    private Runnable logoutHandler = null;

    // scanner as a controller-level field so we can stop it from other handlers (logout, page changes)
    private BarcodeScanner scanner = new BarcodeScanner();

    public void setLogoutHandler(Runnable r) { this.logoutHandler = r; }

    private ObservableList<TxnRow> txnRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // load logo and profile images if available
        try {
            if (logoImageView != null) logoImageView.setImage(new Image(getClass().getResource("/assets/logo.png").toExternalForm()));
            if (profileImageView != null) profileImageView.setImage(new Image(getClass().getResource("/assets/profile-icon-design-free-vector.jpg").toExternalForm()));
            if (topProfileImageView != null) topProfileImageView.setImage(new Image(getClass().getResource("/assets/profile-icon-design-free-vector.jpg").toExternalForm()));
            if (topCashierName != null) topCashierName.setText(System.getProperty("cashier.name", "Cashier"));
            // maximize cashier window for consistent full-screen layout
            try { javafx.stage.Stage st = (javafx.stage.Stage) topCashierName.getScene().getWindow(); st.setMaximized(true); } catch (Exception ignored) {}
        } catch (Exception ignore) {}
        // style navbar buttons
        newBillingBtn.setStyle("-fx-background-color: linear-gradient(to right,#7c4dff,#b388ff); -fx-text-fill: white;");
        viewTransBtn.setStyle("-fx-background-color: linear-gradient(to right,#4dd0e1,#26c6da); -fx-text-fill: white;");
        exitBtn.setStyle("-fx-background-color: linear-gradient(to right,#ff6b6b,#ff8b8b); -fx-text-fill: white;");

        // Add Guidelines / Privacy / About quick links in the navbar area (if sidebar container exists as parent)
        try {
            javafx.scene.layout.VBox parentSidebar = (javafx.scene.layout.VBox) newBillingBtn.getParent();
            if (parentSidebar != null) {
                // ensure exit button will remain last; remove and re-add later
                try {
                    if (exitBtn != null) { parentSidebar.getChildren().remove(exitBtn); }
                } catch (Exception ignored) {}
                Button guidelineBtn = new Button("Guidelines"); guidelineBtn.setStyle("-fx-background-color: linear-gradient(#8be58b,#45c754); -fx-text-fill: white; -fx-font-weight:bold; -fx-padding:10 12; -fx-background-radius:8");
                Button privacyBtn = new Button("Privacy Policy"); privacyBtn.setStyle("-fx-background-color: linear-gradient(#fff489,#ffd400); -fx-text-fill:#2b2b2b; -fx-font-weight:bold; -fx-padding:10 12; -fx-background-radius:8");
                Button aboutBtn = new Button("About Us"); aboutBtn.setStyle("-fx-background-color: linear-gradient(#c8a6ff,#8e6fff); -fx-text-fill: white; -fx-font-weight:bold; -fx-padding:10 12; -fx-background-radius:8");
                guidelineBtn.setOnAction(ev -> { setPosControlsVisible(false); setActiveSidebarButton(guidelineBtn); showGuidelinesPage(); });
                privacyBtn.setOnAction(ev -> { setPosControlsVisible(false); setActiveSidebarButton(privacyBtn); showPrivacyPage(); });
                aboutBtn.setOnAction(ev -> { setPosControlsVisible(false); setActiveSidebarButton(aboutBtn); showAboutPage(); });
                parentSidebar.getChildren().addAll(guidelineBtn, privacyBtn, aboutBtn);
                // re-add exit button as last
                try {
                    if (exitBtn != null) {
                        parentSidebar.getChildren().remove(exitBtn);
                        parentSidebar.getChildren().add(exitBtn);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

    // Transaction table columns: product_id, product_name, qty, rate(price), total, operation
    TableColumn<TxnRow, String> cProdId = new TableColumn<>("Product ID");
    cProdId.setCellValueFactory(new PropertyValueFactory<>("productId"));
    TableColumn<TxnRow, String> cProd = new TableColumn<>("Product");
    cProd.setCellValueFactory(new PropertyValueFactory<>("productName"));
    TableColumn<TxnRow, Integer> cQty = new TableColumn<>("Qty");
    cQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
    TableColumn<TxnRow, Double> cPrice = new TableColumn<>("Rate");
    cPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
    TableColumn<TxnRow, Double> cTotal = new TableColumn<>("Total");
    cTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        TableColumn<TxnRow, Void> cOps = new TableColumn<>("Operations");
        cOps.setCellFactory(new javafx.util.Callback<TableColumn<TxnRow, Void>, TableCell<TxnRow, Void>>() {
            @Override
            public TableCell<TxnRow, Void> call(TableColumn<TxnRow, Void> param) {
                return new TableCell<TxnRow, Void>() {
            private final Button rem = new Button("Remove");
            { rem.setStyle("-fx-background-color: linear-gradient(to right,#ff6b6b,#ff8b8b); -fx-text-fill: white;");
              rem.setOnAction(e -> {
                  TxnRow row = getTableView().getItems().get(getIndex());
                  // show admin auth dialog
                  Dialog<java.util.Map<String,String>> d = new Dialog<>();
                  d.setTitle("Authentication required");
                  javafx.scene.control.TextField userf = new javafx.scene.control.TextField(); userf.setPromptText("Username");
                  PasswordField pf = new PasswordField(); pf.setPromptText("Password");
                  d.getDialogPane().setContent(new javafx.scene.layout.VBox(8, userf, pf));
                  d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                  d.setResultConverter(bt -> {
                      if (bt == ButtonType.OK) {
                          java.util.Map<String,String> m = new java.util.HashMap<>();
                          m.put("u", userf.getText());
                          m.put("p", pf.getText());
                          return m;
                      }
                      return null;
                  });
                  d.showAndWait().ifPresent(map -> {
                      try {
                          boolean ok = database.DatabaseConnection.validateUser(map.get("u"), map.get("p"));
                          if (ok) txnRows.remove(row);
                          else new Alert(Alert.AlertType.ERROR, "Authentication failed").showAndWait();
                      } catch (Exception ex) { ex.printStackTrace(); }
                  });
              }); }
                    @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty?null:rem); }
                };
            }
        });

    // only keep the requested fields in this order
    txnTable.getColumns().addAll(java.util.Arrays.asList(cProdId, cProd, cQty, cPrice, cTotal, cOps));
        // avoid extra empty column by using constrained resize policy
        txnTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        txnTable.setItems(txnRows);

        // search suggestions
        ContextMenu suggestions = new ContextMenu();
        productSearchField.textProperty().addListener((obs, o, n) -> {
            suggestions.getItems().clear();
            if (n==null||n.isEmpty()) { suggestions.hide(); return; }
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT product_id, product_name, category, price, stock_quantity, unit FROM Products WHERE product_name LIKE ? LIMIT 10");
                ps.setString(1, "%"+n+"%");
                java.sql.ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String pid = rs.getString("product_id");
                    String pname = rs.getString("product_name");
                    MenuItem it = new MenuItem(pname + " ("+pid+")");
                    it.setOnAction(ev -> showProductDialog(pid));
                    suggestions.getItems().add(it);
                }
                if (!suggestions.getItems().isEmpty()) {
                    javafx.geometry.Bounds b = productSearchField.localToScreen(productSearchField.getBoundsInLocal());
                    suggestions.show(productSearchField, b.getMinX(), b.getMaxY());
                } else suggestions.hide();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // remove addToTxnBtn behavior (unused)
    if (addToTxnBtn != null) addToTxnBtn.setVisible(false);

        // New Billing shows the POS pane and ensures search/scan controls are visible
        newBillingBtn.setOnAction(e -> {
            // ensure scanner/search controls visible for POS
            setPosControlsVisible(true);
            try { rightStack.getChildren().setAll(createScrollable(txnPane)); } catch (Exception ignore) { rightStack.getChildren().clear(); rightStack.getChildren().add(createScrollable(txnPane)); }
            welcomePane.setVisible(false); welcomePane.setManaged(false);
            txnPane.setVisible(true); txnPane.setManaged(true);
            txnHeader.setText("New Billing / POS");
            try { setActiveSidebarButton(newBillingBtn); } catch (Exception ignored) {}
        });

        // Exit quits the app
        exitBtn.setOnAction(e -> javafx.application.Platform.exit());

    // (previous logout modal removed; logout now redirects to Login view below)

        // Scan button starts/stops scanner and handles result. Use controller-level scanner instance.
        scanBtn.setOnAction(e -> {
            if (scanBtn.getText().toLowerCase().contains("stop")) {
                scanner.stopScanning();
                scanBtn.setText("Scan Barcode");
                scanBtn.setDisable(false);
                return;
            }
            // Start scanning; allow manual stop via Stop button
            scanBtn.setText("Stop");
            scanner.startScanning(new BarcodeScanner.BarcodeScanCallback() {
                @Override public void onBarcodeScanned(String barcodeData) {
                    // Stop scanner immediately to release webcam
                    try { scanner.stopScanning(); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        scanBtn.setText("Scan Barcode");
                        scanBtn.setDisable(false);
                        showProductDialogByBarcode(barcodeData);
                    });
                }
                @Override public void onError(String errorMessage) {
                    // Ensure UI is restored and scanner stopped
                    try { scanner.stopScanning(); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        scanBtn.setText("Scan Barcode");
                        scanBtn.setDisable(false);
                        new Alert(Alert.AlertType.ERROR, "Scanner error: " + errorMessage + "\nIf you have another app using the webcam, close it and try again.").showAndWait();
                    });
                }
            });
        });
        // Generate Barcode quick access in cashier navbar (optional button)
        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> {
                try {
                    // stop scanner first
                    try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
                    // use same style as admin logout: show loading then invoke handler
                    StackPane loadingPane = new StackPane();
                    loadingPane.setStyle("-fx-background-color: #f4f4f9;");
                    javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
                    Label loadingLabel = new Label("Logging out...");
                    loadingLabel.setFont(javafx.scene.text.Font.font("Segoe UI", 18));
                    VBox box = new VBox(20, pi, loadingLabel);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    loadingPane.getChildren().add(box);
                    javafx.stage.Stage stage = (javafx.stage.Stage) logoutBtn.getScene().getWindow();
                    javafx.scene.Scene loadingScene = new javafx.scene.Scene(loadingPane, 420, 540);
                    stage.setScene(loadingScene);
                    new Thread(() -> {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(() -> { if (logoutHandler != null) logoutHandler.run(); else showLoginFallback(stage); });
                    }).start();
                } catch (Exception ex) { ex.printStackTrace(); javafx.application.Platform.exit(); }
            });
        }
        // POS navigation buttons
        if (prevBtn != null) {
            prevBtn.setOnAction(e -> {
                // go back to welcome
                welcomePane.setVisible(true); welcomePane.setManaged(true);
                txnPane.setVisible(false); txnPane.setManaged(false);
            });
        }
        if (nextBtn != null) {
            nextBtn.setOnAction(e -> {
                if (txnRows.isEmpty()) { new Alert(Alert.AlertType.INFORMATION, "No items in transaction").showAndWait(); return; }
                // only next in first POS page: show loyalty page
                showLoyaltyPage();
            });
        }
        // View Transactions button shows today's transactions for the logged-in cashier
        if (viewTransBtn != null) {
            viewTransBtn.setOnAction(e -> {
                // hide search/scan controls when viewing transactions
                setPosControlsVisible(false);
                try { setActiveSidebarButton(viewTransBtn); } catch (Exception ignored) {}
                showDailyTransactions();
            });
        }
        if (cancelTxnBtn != null) {
            cancelTxnBtn.setOnAction(e -> {
                txnRows.clear();
                welcomePane.setVisible(true); welcomePane.setManaged(true);
                txnPane.setVisible(false); txnPane.setManaged(false);
                txnHeader.setText("Welcome");
            });
        }
        if (confirmBtn != null) {
            confirmBtn.setOnAction(e -> showLoyaltyPage());
        }
    }

    // Loyalty page (in-place) after Confirm: choose registered/unregistered, lookup phone for registered
    private void showLoyaltyPage() {
    VBox loyaltyPage = new VBox(12);
    loyaltyPage.setStyle("-fx-padding: 12; -fx-background-color: #fff;");
    Label title = new Label("Loyalty Page"); title.setStyle("-fx-font-size:16; -fx-font-weight:bold;");
    ToggleGroup tg = new ToggleGroup();
    RadioButton rbReg = new RadioButton("Registered"); rbReg.setToggleGroup(tg);
    RadioButton rbUnreg = new RadioButton("Unregistered"); rbUnreg.setToggleGroup(tg);
    rbUnreg.setSelected(true);
    TextField phone = new TextField(); phone.setPromptText("Phone number (for registered users)"); phone.setDisable(true);
    Label customerName = new Label();
    Label loyaltyPointsLabel = new Label(); loyaltyPointsLabel.setStyle("-fx-font-size:12;");
    HBox choices = new HBox(12, rbReg, rbUnreg);
    VBox body = new VBox(10, title, choices, phone, customerName);
        // toggle phone enable and instant lookup when registered
        final String[] matchedCustomer = new String[1];
        tg.selectedToggleProperty().addListener((obs, o, n) -> { if (n==rbReg) phone.setDisable(false); else { phone.setDisable(true); customerName.setText(""); matchedCustomer[0]=null; } });
        phone.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().isEmpty() || !rbReg.isSelected()) { customerName.setText(""); loyaltyPointsLabel.setText(""); matchedCustomer[0] = null; return; }
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT c_id, customer_name, loyalty_points FROM customers WHERE phone_no = ? LIMIT 1");
                ps.setString(1, n.trim()); java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    matchedCustomer[0] = rs.getString("c_id");
                    customerName.setText(rs.getString("customer_name"));
                    // show loyalty points when matched
                    int lp = rs.getInt("loyalty_points");
                    loyaltyPointsLabel.setText("Loyalty Points: " + lp);
                } else { matchedCustomer[0] = null; customerName.setText(""); loyaltyPointsLabel.setText(""); }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

    Button prev = new Button("Prev"); Button next = new Button("Next");
    HBox nav = new HBox(10, prev, next);
    // include loyalty points label under customer name
    body.getChildren().add(loyaltyPointsLabel);
    loyaltyPage.getChildren().addAll(body, nav);
        prev.setOnAction(e -> {
            // go back to txn page
            rightStack.getChildren().setAll(txnPane);
            setPosControlsVisible(true);
            txnPane.setVisible(true); txnPane.setManaged(true);
        });
        next.setOnAction(e -> {
            if (rbReg.isSelected()) {
                String ph = phone.getText();
                if (ph==null || ph.trim().isEmpty()) { new Alert(Alert.AlertType.ERROR, "Enter phone number").showAndWait(); return; }
                if (matchedCustomer[0] != null) {
                    // proceed to payment with matched id
                    showPaymentDialog(matchedCustomer[0]);
                } else {
                    new Alert(Alert.AlertType.INFORMATION, "No customer with that phone number").showAndWait();
                }
            } else {
                // unregistered -> proceed to payment (null customer)
                showPaymentDialog(null);
            }
        });

    // loyalty page is not POS: hide search/scan controls and clear active sidebar highlight
    setPosControlsVisible(false);
    try { setActiveSidebarButton(null); } catch (Exception ignored) {}
    rightStack.getChildren().setAll(createScrollable(loyaltyPage));
    }

    // Fallback shown when logoutHandler is not provided: close the stage and return to login
    private void showLoginFallback(javafx.stage.Stage stage) {
        try {
            // attempt to close and exit; the App launcher will handle showing login if needed
            stage.close();
        } catch (Exception ignore) { javafx.application.Platform.exit(); }
    }
    // ...existing code...

    private void showProductDialog(String productId) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT product_id, product_name, category, price, stock_quantity, unit FROM Products WHERE product_id=?");
            ps.setString(1, productId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String pid = rs.getString("product_id");
                String pname = rs.getString("product_name");
                String cat = rs.getString("category");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock_quantity");
                String unit = rs.getString("unit");

                Dialog<Integer> d = new Dialog<>(); d.setTitle("Add to transaction");
                TextField qtyField = new TextField("1");
                qtyField.setPromptText("Quantity");
                // allow only digits
                qtyField.textProperty().addListener((obs, oldV, newV) -> {
                    if (!newV.matches("\\d*")) qtyField.setText(newV.replaceAll("[^\\d]", ""));
                });
                Label info = new Label(pname + "\nCategory: " + cat + "\nPrice: " + price + "\nStock: " + stock + " " + unit);
                Label err = new Label(); err.setStyle("-fx-text-fill: #b00020;");
                d.getDialogPane().setContent(new javafx.scene.layout.VBox(8, info, new javafx.scene.control.Label("Quantity"), qtyField, err));
                d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                final Button okBtn = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
                okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                    String t = qtyField.getText();
                    if (t==null || t.isEmpty()) { err.setText("Please enter a quantity"); ev.consume(); return; }
                    int q = Integer.parseInt(t);
                    if (q < 1) { err.setText("Quantity must be at least 1"); ev.consume(); return; }
                    if (q > stock) { err.setText("Quantity exceeds available stock: " + stock); ev.consume(); return; }
                    // valid
                });
                d.setResultConverter(bt -> {
                    if (bt == ButtonType.OK) {
                        String t = qtyField.getText(); if (t==null||t.isEmpty()) return null;
                        return Integer.parseInt(t);
                    }
                    return null;
                });
                d.showAndWait().ifPresent(qtyVal -> {
                    double total = price * qtyVal;
                    txnRows.add(new TxnRow(pid, pname, qtyVal, price, total));
                    txnHeader.setText("Transaction: " + txnRows.size() + " items");
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showProductDialogByBarcode(String barcode) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM Products WHERE barcode=? LIMIT 1");
            ps.setString(1, barcode);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) showProductDialog(rs.getString("product_id"));
            else new Alert(Alert.AlertType.INFORMATION, "No product found for barcode: " + barcode).showAndWait();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public static class TxnRow {
    public String productId, productName; public int qty; public double price, total;
    public TxnRow(String productId, String productName, int qty, double price, double total) { this.productId=productId; this.productName=productName; this.qty=qty; this.price=price; this.total=total; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
    public double getTotal() { return total; }
    }

    private void showPaymentDialog(String customerId) {
        // Replace rightStack content with Payment Page (full area)
        double totalForDisplay = txnRows.stream().mapToDouble(r -> r.total).sum();
        VBox paymentPage = new VBox(12);
        paymentPage.setStyle("-fx-padding: 18; -fx-background-color: #fff; -fx-background-radius: 8;");
        Label title = new Label("Payment"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        Label totalLabel = new Label("Total: " + String.format("%.2f", totalForDisplay));

        // Cash by Hand UI
        VBox cashHandBox = new VBox(8);
        cashHandBox.setStyle("-fx-border-color: transparent; -fx-padding: 8;");
        Label cashLabel = new Label("Cash by Hand");
        TextField receivedField = new TextField(); receivedField.setPromptText("Amount received");
        Label changeLabel = new Label("Change: 0.00");
        Button confirmCash = new Button("Confirm Cash");
        cashHandBox.getChildren().addAll(cashLabel, receivedField, changeLabel, confirmCash);

        // Cash by Payment Code
        VBox codeBox = new VBox(8);
        Label codeLabel = new Label("Cash by Payment Code");
        TextField codeField = new TextField(); codeField.setPromptText("Enter payment code");
        Button verifyCode = new Button("Verify Code");
        codeBox.getChildren().addAll(codeLabel, codeField, verifyCode);

    Button cancel = new Button("Cancel");
    Button sendCodeBtn = new Button("Send Code");
    Button backBtn = new Button("← Back");
    HBox actions = new HBox(12, backBtn, sendCodeBtn, cancel);

    paymentPage.getChildren().addAll(title, totalLabel, cashHandBox, codeBox, actions);
    rightStack.getChildren().setAll(createScrollable(paymentPage));

        // Live calculation of change
        receivedField.textProperty().addListener((obs, o, n) -> {
            try {
                double received = n==null||n.isEmpty()?0.0:Double.parseDouble(n);
                double change = received - totalForDisplay;
                changeLabel.setText(String.format("Change: %.2f", change >= 0 ? change : 0.0));
            } catch (NumberFormatException ex) { changeLabel.setText("Change: 0.00"); }
        });

    confirmCash.setOnAction(e -> {
            // ensure scanner is stopped before performing payment
            try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
            java.sql.Connection conn = null;
            try {
                conn = database.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);
                double itemsTotal = totalForDisplay;
                // fetch current settings (tax, discount as percentages)
                double taxPct = 0, discountPct = 0;
                try (java.sql.Statement st = conn.createStatement()) {
                    java.sql.ResultSet rs = st.executeQuery("SELECT tax, discount FROM settings ORDER BY updated_at DESC LIMIT 1");
                    if (rs.next()) { taxPct = rs.getDouble("tax"); discountPct = rs.getDouble("discount"); }
                }
                // calculate monetary tax and discount amounts (rounded to 2 decimals)
                java.math.BigDecimal itemsTotalBd = new java.math.BigDecimal(String.format("%.2f", itemsTotal));
                java.math.BigDecimal taxAmountBd = itemsTotalBd.multiply(new java.math.BigDecimal(taxPct)).divide(new java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal discountAmountBd = itemsTotalBd.multiply(new java.math.BigDecimal(discountPct)).divide(new java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP);
                // Per policy grand_total is sales_total + tax (gross total). We'll compute payable after discount cap/conversion below.
                java.math.BigDecimal grossTotalBd = itemsTotalBd.add(taxAmountBd).setScale(2, java.math.RoundingMode.HALF_UP);

                // Parse received amount (cash)
                double received = 0.0;
                try { received = Double.parseDouble(receivedField.getText().trim()); } catch (Exception ex) { received = 0.0; }
                java.math.BigDecimal receivedBd = new java.math.BigDecimal(String.format("%.2f", received));

                // Insert into main_sales (discount will be updated after applying cap/convert rules)
                // Include customer information when available
                java.sql.PreparedStatement ms = conn.prepareStatement("INSERT INTO main_sales (cashier_id, sales_total, tax, discount, customer_id, customer_name) VALUES (?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                String cashierId = System.getProperty("cashier.id", "unknown");
                ms.setString(1, cashierId);
                ms.setBigDecimal(2, itemsTotalBd);
                ms.setBigDecimal(3, taxAmountBd);
                ms.setBigDecimal(4, discountAmountBd);
                // set customer info (may be null)
                if (customerId != null) {
                    // try to read customer_name
                    String cname = null;
                    try (java.sql.PreparedStatement cst = conn.prepareStatement("SELECT customer_name FROM customers WHERE c_id=? LIMIT 1")) { cst.setString(1, customerId); java.sql.ResultSet cr = cst.executeQuery(); if (cr.next()) cname = cr.getString(1); }
                    ms.setString(5, customerId);
                    ms.setString(6, cname == null ? "" : cname);
                } else {
                    // unregistered customer -> write placeholders
                    ms.setString(5, "-");
                    ms.setString(6, "Unknown");
                }
                ms.executeUpdate();
                java.sql.ResultSet gk = ms.getGeneratedKeys();
                final long[] mainSalesIdArr = new long[] { -1 };
                if (gk.next()) mainSalesIdArr[0] = gk.getLong(1);

                // Loyalty handling: if registered customer, compute earned points, handle redemption, and convert any excess discount into loyalty points
                if (customerId != null) {
                    try {
                        // Fetch existing points
                        int currentPoints = 0;
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT loyalty_points FROM customers WHERE c_id = ? LIMIT 1")) {
                            cps.setString(1, customerId);
                            try (java.sql.ResultSet crr = cps.executeQuery()) { if (crr.next()) currentPoints = crr.getInt(1); }
                        }

                        // Points earned from this payment: 1 point = Rs 100 of items total (before tax/discount)
                        int earned = itemsTotalBd.divide(new java.math.BigDecimal(100), java.math.RoundingMode.FLOOR).intValue();
                        int newPoints = currentPoints + earned;

                        // Redemption: if currentPoints >= 500, apply immediate Rs 1500 discount (this increases discountAmountBd)
                        boolean redeemed = false;
                        if (currentPoints >= 500) {
                            java.math.BigDecimal redeemAmt = new java.math.BigDecimal("1500.00");
                            discountAmountBd = discountAmountBd.add(redeemAmt).setScale(2, java.math.RoundingMode.HALF_UP);
                            try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ?, caption = ? WHERE sales_id = ?")) {
                                upd.setBigDecimal(1, discountAmountBd);
                                upd.setString(2, "Loyalty Point redeemed!(Discount Added: Rs 1500)");
                                upd.setLong(3, mainSalesIdArr[0]);
                                upd.executeUpdate();
                            }
                            redeemed = true;
                        } else {
                            // No redemption: set a friendly caption using customer name if available
                            try {
                                String cnameForCaption = null;
                                try (java.sql.PreparedStatement cst2 = conn.prepareStatement("SELECT customer_name FROM customers WHERE c_id=? LIMIT 1")) {
                                    cst2.setString(1, customerId);
                                    try (java.sql.ResultSet rrr = cst2.executeQuery()) { if (rrr.next()) cnameForCaption = rrr.getString(1); }
                                }
                                String captionVal = (cnameForCaption == null || cnameForCaption.trim().isEmpty()) ? null : ("Customer: " + cnameForCaption.trim());
                                try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET caption = ? WHERE sales_id = ?")) {
                                    upd.setString(1, captionVal);
                                    upd.setLong(2, mainSalesIdArr[0]);
                                    upd.executeUpdate();
                                }
                            } catch (Exception _capEx) { _capEx.printStackTrace(); }
                        }

                        // Now apply the requested policy:
                        // grossTotal = sales_total + tax
                        // if discount < grossTotal -> appliedDiscount = discount, payable = grossTotal - discount
                        // if discount >= grossTotal -> appliedDiscount = grossTotal (payable 0), remaining = discount - grossTotal -> convert remaining to points
                        java.math.BigDecimal appliedDiscount = discountAmountBd.min(grossTotalBd).setScale(2, java.math.RoundingMode.HALF_UP);
                        java.math.BigDecimal remaining = discountAmountBd.subtract(appliedDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
                        java.math.BigDecimal payableBd = grossTotalBd.subtract(appliedDiscount).setScale(2, java.math.RoundingMode.HALF_UP);

                        // Guard against negative payable (shouldn't happen after min, but be safe)
                        if (payableBd.compareTo(java.math.BigDecimal.ZERO) < 0) {
                            java.math.BigDecimal diff = payableBd.abs();
                            remaining = remaining.add(diff).setScale(2, java.math.RoundingMode.HALF_UP);
                            payableBd = java.math.BigDecimal.ZERO;
                        }

                        // Update main_sales.discount to the applied discount and set caption if we converted excess
                        if (remaining.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ?, caption = ? WHERE sales_id = ?")) {
                                upd.setBigDecimal(1, appliedDiscount);
                                upd.setString(2, "Excess discount converted to loyalty points");
                                upd.setLong(3, mainSalesIdArr[0]);
                                upd.executeUpdate();
                            }
                            int convertedPoints = remaining.divide(new java.math.BigDecimal(100), java.math.RoundingMode.FLOOR).intValue();
                            int finalPoints = newPoints + convertedPoints;
                            if (redeemed) { finalPoints = finalPoints - 500; if (finalPoints < 0) finalPoints = 0; }
                            try (java.sql.PreparedStatement upc = conn.prepareStatement("UPDATE customers SET loyalty_points = ? WHERE c_id = ?")) {
                                upc.setInt(1, finalPoints);
                                upc.setString(2, customerId);
                                upc.executeUpdate();
                            }
                        } else {
                            // no conversion needed, just persist discount and points
                            try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ? WHERE sales_id = ?")) {
                                upd.setBigDecimal(1, appliedDiscount);
                                upd.setLong(2, mainSalesIdArr[0]);
                                upd.executeUpdate();
                            }
                            int finalPoints = newPoints;
                            if (redeemed) { finalPoints = newPoints - 500; if (finalPoints < 0) finalPoints = 0; }
                            try (java.sql.PreparedStatement upc = conn.prepareStatement("UPDATE customers SET loyalty_points = ? WHERE c_id = ?")) {
                                upc.setInt(1, finalPoints);
                                upc.setString(2, customerId);
                                upc.executeUpdate();
                            }
                        }
                        // At this point we have updated main_sales.discount and customers.loyalty_points as required.
                    } catch (Exception lpEx) { lpEx.printStackTrace(); }
                }

                // Insert sales detail rows and decrement product stock
                java.sql.PreparedStatement sps = conn.prepareStatement("INSERT INTO sales (sales_id, product_id, product_name, quantity_sold, sale_price, total) VALUES (?, ?, ?, ?, ?, ?)");
                java.sql.PreparedStatement updateStock = conn.prepareStatement("UPDATE Products SET stock_quantity = stock_quantity - ? WHERE product_id = ?");
                for (TxnRow r : txnRows) {
                    sps.setLong(1, mainSalesIdArr[0]);
                    sps.setString(2, r.productId);
                    sps.setString(3, r.productName);
                    sps.setInt(4, r.qty);
                    sps.setBigDecimal(5, new java.math.BigDecimal(String.format("%.2f", r.price)));
                    // total per item (qty * price)
                    sps.setBigDecimal(6, new java.math.BigDecimal(String.format("%.2f", r.total)));
                    sps.addBatch();

                    updateStock.setInt(1, r.qty);
                    updateStock.setString(2, r.productId);
                    updateStock.addBatch();
                }
                sps.executeBatch();
                updateStock.executeBatch();

                // Insert into Payments with required columns (sales_id, cashier_id, p_code)
                java.sql.PreparedStatement pay = conn.prepareStatement("INSERT INTO Payments (sales_id, cashier_id, p_code) VALUES (?, ?, ?)");
                pay.setLong(1, mainSalesIdArr[0]);
                pay.setString(2, cashierId);
                pay.setString(3, java.util.UUID.randomUUID().toString());
                pay.executeUpdate();

                conn.commit();

                // Ensure scanner is stopped and show compact success message; no upload proof in cashier flow
                try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
                // Recompute payable for final display: read discount from DB (main_sales.discount) to be safe
                java.math.BigDecimal payableForDisplay = java.math.BigDecimal.ZERO;
                try (java.sql.PreparedStatement rps = conn.prepareStatement("SELECT sales_total, tax, discount FROM main_sales WHERE sales_id = ? LIMIT 1")) {
                    rps.setLong(1, mainSalesIdArr[0]); try (java.sql.ResultSet rrs = rps.executeQuery()) {
                        if (rrs.next()) {
                            java.math.BigDecimal st = rrs.getBigDecimal("sales_total") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("sales_total");
                            java.math.BigDecimal tx = rrs.getBigDecimal("tax") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("tax");
                            java.math.BigDecimal dc = rrs.getBigDecimal("discount") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("discount");
                            payableForDisplay = st.add(tx).subtract(dc).setScale(2, java.math.RoundingMode.HALF_UP);
                            if (payableForDisplay.compareTo(java.math.BigDecimal.ZERO) < 0) payableForDisplay = java.math.BigDecimal.ZERO;
                        }
                    }
                } catch (Exception ignored) {}
                java.math.BigDecimal changeBd = receivedBd.subtract(payableForDisplay).setScale(2, java.math.RoundingMode.HALF_UP);
                // Include sale id so Payments/main_sales linkage is visible to the cashier
                Label ok = new Label("Payment Successful!\nSale ID: " + mainSalesIdArr[0] + "\nGrand Total: " + payableForDisplay.toPlainString() + "\nChange: " + changeBd.toPlainString());
                // optional success image placeholder from assets
                ImageView successImg = new ImageView(); try { java.io.InputStream is = getClass().getResourceAsStream("/assets/OIP.png"); if (is != null) successImg.setImage(new Image(is)); successImg.setFitWidth(160); successImg.setPreserveRatio(true); } catch (Exception ignored) {}
                Button done = new Button("Done");
                Button exportCsv = new Button("Export Invoice (CSV)");
                VBox success = new VBox(12, ok, successImg, new HBox(8, exportCsv, done)); success.setStyle("-fx-alignment:center; -fx-padding:20;");
                rightStack.getChildren().setAll(createScrollable(success));
                exportCsv.setOnAction(ev2 -> {
                    final long sidForExport = mainSalesIdArr[0];
                    String suffix = computeExportSuffixForSales(sidForExport);
                    exportInvoiceToCsv(sidForExport, "invoice_" + sidForExport + suffix + ".csv");
                });
                // also offer PDF/XLSX on payment success
                Button exportPdf = new Button("Export Invoice (PDF)");
                Button exportXlsx = new Button("Export Invoice (XLSX)");
                final long sidPdf = mainSalesIdArr[0];
                String suffixPdf = computeExportSuffixForSales(sidPdf);
                exportPdf.setOnAction(ev2 -> exportInvoiceToPdf(sidPdf, "invoice_" + sidPdf + suffixPdf + ".pdf"));
                final long sidX = mainSalesIdArr[0];
                String suffixX = computeExportSuffixForSales(sidX);
                exportXlsx.setOnAction(ev2 -> exportInvoiceToXlsx(sidX, "invoice_" + sidX + suffixX + ".xlsx"));
                ((HBox)success.getChildren().get(2)).getChildren().addAll(exportPdf, exportXlsx);
                done.setOnAction(ev -> {
                    // reset txn and restore welcome pane, ensure scanner is stopped and ready for next billing
                    txnRows.clear();
                    try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
                    // restore POS controls and highlight New Billing
                    try { setPosControlsVisible(true); } catch (Exception ignored) {}
                    try { setActiveSidebarButton(newBillingBtn); } catch (Exception ignored) {}
                    if (!rightStack.getChildren().contains(welcomePane)) rightStack.getChildren().setAll(welcomePane);
                    welcomePane.setVisible(true); welcomePane.setManaged(true);
                    txnPane.setVisible(false); txnPane.setManaged(false);
                    txnHeader.setText("Welcome");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                try { if (conn != null) conn.rollback(); } catch (Exception ignore) {}
                new Alert(Alert.AlertType.ERROR, "Payment failed: " + ex.getMessage()).showAndWait();
            } finally {
                try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ignore) {}
            }
        });

        // Send code behavior: create a Payments row with no sales_id and show code
        // Ensure we attach the current cashier's id so the Payments row references the cashier even before sales_id is known
        sendCodeBtn.setOnAction(e -> {
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                String code = java.util.UUID.randomUUID().toString();
                String cashierId = System.getProperty("cashier.id");
                java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO Payments (p_code, cashier_id) VALUES (?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, code);
                if (cashierId != null && !cashierId.trim().isEmpty()) {
                    ps.setString(2, cashierId);
                } else {
                    // fallback to NULL if cashier id is not available in system properties
                    ps.setNull(2, java.sql.Types.VARCHAR);
                }
                ps.executeUpdate();
                // Do NOT display the code in the UI. The system will send it to the customer externally.
                new Alert(Alert.AlertType.INFORMATION, "Payment code sent to customer.").showAndWait();
            } catch (Exception ex) { ex.printStackTrace(); new Alert(Alert.AlertType.ERROR, "Failed to send code").showAndWait(); }
        });

        // Back button should return to the loyalty page (customer lookup / loyalty flows)
        backBtn.setOnAction(e -> {
            try { showLoyaltyPage(); } catch (Exception ex) { // fallback to txn pane if loyalty page fails
                if (!rightStack.getChildren().contains(txnPane)) rightStack.getChildren().setAll(txnPane);
            }
        });

        verifyCode.setOnAction(e -> {
            // ensure scanner is stopped before performing payment
            try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
            String code = codeField.getText();
            if (code == null || code.trim().isEmpty()) { new Alert(Alert.AlertType.ERROR, "Enter payment code").showAndWait(); return; }
            java.sql.Connection conn = null;
            try {
                conn = database.DatabaseConnection.getConnection();
                // find unused payment code (payments row created previously as a token)
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT p_id FROM Payments WHERE p_code = ? AND sales_id IS NULL LIMIT 1");
                ps.setString(1, code.trim()); java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long payId = rs.getLong(1);
                    // proceed to create main_sales and sales rows and then update this Payments row to link
                    conn.setAutoCommit(false);
                    double itemsTotal = totalForDisplay;
                    double taxPct = 0, discountPct = 0;
                    try (java.sql.Statement st = conn.createStatement()) {
                        java.sql.ResultSet rs2 = st.executeQuery("SELECT tax, discount FROM settings ORDER BY updated_at DESC LIMIT 1");
                        if (rs2.next()) { taxPct = rs2.getDouble("tax"); discountPct = rs2.getDouble("discount"); }
                    }
                    java.math.BigDecimal itemsTotalBd = new java.math.BigDecimal(String.format("%.2f", itemsTotal));
                    java.math.BigDecimal taxAmountBd = itemsTotalBd.multiply(new java.math.BigDecimal(taxPct)).divide(new java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal discountAmountBd = itemsTotalBd.multiply(new java.math.BigDecimal(discountPct)).divide(new java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP);

                    java.sql.PreparedStatement ms = conn.prepareStatement("INSERT INTO main_sales (cashier_id, sales_total, tax, discount, customer_id, customer_name) VALUES (?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                    String cashierId = System.getProperty("cashier.id", "unknown");
                    ms.setString(1, cashierId);
                    ms.setBigDecimal(2, itemsTotalBd);
                    ms.setBigDecimal(3, taxAmountBd);
                    ms.setBigDecimal(4, discountAmountBd);
                    if (customerId != null) {
                        String cname = null;
                        try (java.sql.PreparedStatement cst = conn.prepareStatement("SELECT customer_name FROM customers WHERE c_id=? LIMIT 1")) { cst.setString(1, customerId); java.sql.ResultSet cr = cst.executeQuery(); if (cr.next()) cname = cr.getString(1); }
                        ms.setString(5, customerId);
                        ms.setString(6, cname == null ? "" : cname);
                    } else {
                        // Ensure placeholders for unregistered customers
                        ms.setString(5, "-");
                        ms.setString(6, "Unknown");
                    }
                    ms.executeUpdate(); java.sql.ResultSet gk = ms.getGeneratedKeys(); long mainSalesId = -1; if (gk.next()) mainSalesId = gk.getLong(1);

                        // Loyalty handling per requested policy: cap discount to gross total (sales_total + tax), convert any remaining discount into loyalty points
                        if (customerId != null) {
                            try {
                                int currentPoints = 0;
                                try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT loyalty_points FROM customers WHERE c_id = ? LIMIT 1")) {
                                    cps.setString(1, customerId);
                                    try (java.sql.ResultSet crr = cps.executeQuery()) { if (crr.next()) currentPoints = crr.getInt(1); }
                                }
                                int earned = itemsTotalBd.divide(new java.math.BigDecimal(100), java.math.RoundingMode.FLOOR).intValue();
                                int newPoints = currentPoints + earned;
                                boolean redeemed = false;
                                if (currentPoints >= 500) {
                                    java.math.BigDecimal redeemAmt = new java.math.BigDecimal("1500.00");
                                    discountAmountBd = discountAmountBd.add(redeemAmt).setScale(2, java.math.RoundingMode.HALF_UP);
                                    try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ?, caption = ? WHERE sales_id = ?")) {
                                        upd.setBigDecimal(1, discountAmountBd);
                                        upd.setString(2, "Loyalty Point redeemed!(Discount Added: Rs 1500)");
                                        upd.setLong(3, mainSalesId);
                                        upd.executeUpdate();
                                    }
                                    redeemed = true;
                                } else {
                                    try {
                                        String cnameForCaption = null;
                                        try (java.sql.PreparedStatement cst2 = conn.prepareStatement("SELECT customer_name FROM customers WHERE c_id=? LIMIT 1")) {
                                            cst2.setString(1, customerId);
                                            try (java.sql.ResultSet rrr = cst2.executeQuery()) { if (rrr.next()) cnameForCaption = rrr.getString(1); }
                                        }
                                        String captionVal = (cnameForCaption == null || cnameForCaption.trim().isEmpty()) ? null : ("Customer: " + cnameForCaption.trim());
                                        try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET caption = ? WHERE sales_id = ?")) { upd.setString(1, captionVal); upd.setLong(2, mainSalesId); upd.executeUpdate(); }
                                    } catch (Exception _capEx) { _capEx.printStackTrace(); }
                                }

                                // gross total = sales_total + tax
                                java.math.BigDecimal grossTotalBd = itemsTotalBd.add(taxAmountBd).setScale(2, java.math.RoundingMode.HALF_UP);
                                java.math.BigDecimal appliedDiscount = discountAmountBd.min(grossTotalBd).setScale(2, java.math.RoundingMode.HALF_UP);
                                java.math.BigDecimal remaining = discountAmountBd.subtract(appliedDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
                                java.math.BigDecimal payableBd = grossTotalBd.subtract(appliedDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
                                if (payableBd.compareTo(java.math.BigDecimal.ZERO) < 0) {
                                    java.math.BigDecimal diff = payableBd.abs();
                                    remaining = remaining.add(diff).setScale(2, java.math.RoundingMode.HALF_UP);
                                    payableBd = java.math.BigDecimal.ZERO;
                                }

                                if (remaining.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                    try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ?, caption = ? WHERE sales_id = ?")) {
                                        upd.setBigDecimal(1, appliedDiscount);
                                        upd.setString(2, "Excess discount converted to loyalty points");
                                        upd.setLong(3, mainSalesId);
                                        upd.executeUpdate();
                                    }
                                    int convertedPoints = remaining.divide(new java.math.BigDecimal(100), java.math.RoundingMode.FLOOR).intValue();
                                    int finalPoints = newPoints + convertedPoints;
                                    if (redeemed) { finalPoints = finalPoints - 500; if (finalPoints < 0) finalPoints = 0; }
                                    try (java.sql.PreparedStatement upc = conn.prepareStatement("UPDATE customers SET loyalty_points = ? WHERE c_id = ?")) { upc.setInt(1, finalPoints); upc.setString(2, customerId); upc.executeUpdate(); }
                                } else {
                                    try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE main_sales SET discount = ? WHERE sales_id = ?")) { upd.setBigDecimal(1, appliedDiscount); upd.setLong(2, mainSalesId); upd.executeUpdate(); }
                                    int finalPoints = newPoints; if (redeemed) { finalPoints = newPoints - 500; if (finalPoints < 0) finalPoints = 0; }
                                    try (java.sql.PreparedStatement upc = conn.prepareStatement("UPDATE customers SET loyalty_points = ? WHERE c_id = ?")) { upc.setInt(1, finalPoints); upc.setString(2, customerId); upc.executeUpdate(); }
                                }
                            } catch (Exception lpEx) { lpEx.printStackTrace(); }
                        }

                    java.sql.PreparedStatement sps = conn.prepareStatement("INSERT INTO sales (sales_id, product_id, product_name, quantity_sold, sale_price, total) VALUES (?, ?, ?, ?, ?, ?)");
                    java.sql.PreparedStatement updateStock = conn.prepareStatement("UPDATE Products SET stock_quantity = stock_quantity - ? WHERE product_id = ?");
                    for (TxnRow r : txnRows) {
                        sps.setLong(1, mainSalesId);
                        sps.setString(2, r.productId);
                        sps.setString(3, r.productName);
                        sps.setInt(4, r.qty);
                        sps.setBigDecimal(5, new java.math.BigDecimal(String.format("%.2f", r.price)));
                        sps.setBigDecimal(6, new java.math.BigDecimal(String.format("%.2f", r.total)));
                        sps.addBatch();

                        updateStock.setInt(1, r.qty);
                        updateStock.setString(2, r.productId);
                        updateStock.addBatch();
                    }
                    sps.executeBatch(); updateStock.executeBatch();

                    // link the existing payment code row to this sale
                    java.sql.PreparedStatement updPay = conn.prepareStatement("UPDATE Payments SET sales_id = ?, cashier_id = ? WHERE p_id = ?");
                    updPay.setLong(1, mainSalesId); updPay.setString(2, cashierId); updPay.setLong(3, payId);
                    updPay.executeUpdate();

                    conn.commit();
                    // stop scanner for privacy and show compact success view with grand total (recompute payable from DB to reflect any conversion)
                    try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
                    ImageView successImg = new ImageView(); try { java.io.InputStream is = getClass().getResourceAsStream("/assets/OIP.png"); if (is != null) successImg.setImage(new Image(is)); successImg.setFitWidth(160); successImg.setPreserveRatio(true); } catch (Exception ignored) {}
                    // show sale id so cashier can note the reference; query persisted payable (sales_total + tax - discount) to avoid negative displays
                    java.math.BigDecimal payableForDisplay = java.math.BigDecimal.ZERO;
                    try (java.sql.PreparedStatement rps = conn.prepareStatement("SELECT sales_total, tax, discount FROM main_sales WHERE sales_id = ? LIMIT 1")) {
                        rps.setLong(1, mainSalesId); try (java.sql.ResultSet rrs = rps.executeQuery()) {
                            if (rrs.next()) {
                                java.math.BigDecimal st = rrs.getBigDecimal("sales_total") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("sales_total");
                                java.math.BigDecimal tx = rrs.getBigDecimal("tax") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("tax");
                                java.math.BigDecimal dc = rrs.getBigDecimal("discount") == null ? java.math.BigDecimal.ZERO : rrs.getBigDecimal("discount");
                                payableForDisplay = st.add(tx).subtract(dc).setScale(2, java.math.RoundingMode.HALF_UP);
                                if (payableForDisplay.compareTo(java.math.BigDecimal.ZERO) < 0) payableForDisplay = java.math.BigDecimal.ZERO;
                            }
                        }
                    } catch (Exception ignored) {}
                    Label info = new Label("Payment completed via Code.\nSale ID: " + mainSalesId + "\nGrand Total: " + payableForDisplay.toPlainString());
                    Button okBtn = new Button("Done");
                    Button exportCsvBtn = new Button("Export Invoice (CSV)");
                    Button exportPdfBtn = new Button("Export Invoice (PDF)");
                    Button exportXlsxBtn = new Button("Export Invoice (XLSX)");
                    HBox btnRow = new HBox(8, exportCsvBtn, exportPdfBtn, exportXlsxBtn, okBtn);
                    VBox succ = new VBox(12, info, successImg, btnRow); succ.setStyle("-fx-alignment:center; -fx-padding:20;");
                    rightStack.getChildren().setAll(createScrollable(succ));
                    final long saleIdSaved = mainSalesId;
                    String sfxOk = computeExportSuffixForSales(saleIdSaved);
                    exportCsvBtn.setOnAction(ae2 -> exportInvoiceToCsv(saleIdSaved, "invoice_" + saleIdSaved + sfxOk + ".csv"));
                    exportPdfBtn.setOnAction(ae2 -> exportInvoiceToPdf(saleIdSaved, "invoice_" + saleIdSaved + sfxOk + ".pdf"));
                    exportXlsxBtn.setOnAction(ae2 -> exportInvoiceToXlsx(saleIdSaved, "invoice_" + saleIdSaved + sfxOk + ".xlsx"));
                    okBtn.setOnAction(ae -> {
                        // reset txn and restore welcome pane, ensure scanner is stopped and ready for next billing
                        txnRows.clear();
                        try { if (scanner != null) scanner.stopScanning(); } catch (Exception ignored) {}
                        // restore POS controls and highlight New Billing
                        try { setPosControlsVisible(true); } catch (Exception ignored) {}
                        try { setActiveSidebarButton(newBillingBtn); } catch (Exception ignored) {}
                        if (!rightStack.getChildren().contains(welcomePane)) rightStack.getChildren().setAll(welcomePane);
                        welcomePane.setVisible(true); welcomePane.setManaged(true);
                        txnPane.setVisible(false); txnPane.setManaged(false);
                        txnHeader.setText("Welcome");
                    });
                } else {
                    new Alert(Alert.AlertType.ERROR, "Invalid or used payment code").showAndWait();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                try { if (conn != null) conn.rollback(); } catch (Exception ignore) {}
                new Alert(Alert.AlertType.ERROR, "DB error: " + ex.getMessage()).showAndWait();
            } finally {
                try { if (conn != null) conn.setAutoCommit(true); if (conn != null) conn.close(); } catch (Exception ignore) {}
            }
        });

        cancel.setOnAction(e -> {
            // restore original rightStack content
            rightStack.getChildren().clear();
            rightStack.getChildren().addAll(welcomePane);
            welcomePane.setVisible(true); welcomePane.setManaged(true);
        });
    }

    // Daily transactions view for logged-in cashier
    private void showDailyTransactions() {
        VBox page = new VBox(12);
        page.setStyle("-fx-padding:12; -fx-background-color:#fff;");
        Label title = new Label("Today's Transactions"); title.setStyle("-fx-font-size:16; -fx-font-weight:bold;");

        TableView<DailyTxn> table = new TableView<>();
        TableColumn<DailyTxn, Long> cId = new TableColumn<>("Sale ID"); cId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        TableColumn<DailyTxn, String> cTime = new TableColumn<>("Time"); cTime.setCellValueFactory(new PropertyValueFactory<>("saleTime"));
        TableColumn<DailyTxn, String> cTotal = new TableColumn<>("Total"); cTotal.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        TableColumn<DailyTxn, String> cCustId = new TableColumn<>("Customer ID"); cCustId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        TableColumn<DailyTxn, String> cCustName = new TableColumn<>("Customer"); cCustName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
    table.getColumns().addAll(java.util.Arrays.asList(cId, cTime, cTotal, cCustId, cCustName));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        javafx.collections.ObservableList<DailyTxn> rows = FXCollections.observableArrayList();
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT sales_id, sale_time, sales_total, tax, discount, grand_total, customer_id, customer_name FROM main_sales WHERE cashier_id = ? AND DATE(sale_time) = CURRENT_DATE ORDER BY sale_time DESC");
            ps.setString(1, System.getProperty("cashier.id", "unknown"));
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("sales_id");
                java.sql.Timestamp ts = rs.getTimestamp("sale_time");
                String stime = ts == null ? "" : ts.toString();
                String grand;
                try { grand = rs.getBigDecimal("grand_total").setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); } catch (Exception ex) {
                    try { java.math.BigDecimal st = rs.getBigDecimal("sales_total"); java.math.BigDecimal tax = rs.getBigDecimal("tax"); java.math.BigDecimal disc = rs.getBigDecimal("discount"); grand = st.add(tax==null?java.math.BigDecimal.ZERO:tax).subtract(disc==null?java.math.BigDecimal.ZERO:disc).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); } catch (Exception ex2) { grand = ""; }
                }
                String cid = rs.getString("customer_id"); if (cid==null||cid.isEmpty()) cid = "-";
                String cname = rs.getString("customer_name"); if (cname==null||cname.isEmpty()) cname = "Unknown";
                rows.add(new DailyTxn(id, stime, grand, cid, cname));
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        table.setItems(rows);
    // export buttons
    HBox topActs = new HBox(8);
    Button expCsv = new Button("Export Selected Invoice (CSV)");
    Button expPdf = new Button("Export Selected Invoice (PDF)");
    Button expXlsx = new Button("Export Selected Invoice (XLSX)");
    topActs.getChildren().addAll(expCsv, expPdf, expXlsx);

    // double-click a row to show invoice details (drill-in)
        table.setRowFactory(tv -> {
            TableRow<DailyTxn> r = new TableRow<>();
            r.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2 && !r.isEmpty()) {
                    DailyTxn dt = r.getItem();
                    long sid = dt.getSaleId();
                    // build invoice view similar to admin
                    try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                        java.sql.PreparedStatement ps = conn.prepareStatement("SELECT ms.sales_id, ms.cashier_id, ms.customer_id, ms.customer_name, ms.sales_total, ms.tax, ms.discount, ms.grand_total, ms.sale_time, s.product_id, s.product_name, s.quantity_sold, s.sale_price, s.total FROM main_sales ms LEFT JOIN sales s ON s.sales_id = ms.sales_id WHERE ms.sales_id = ?");
                        ps.setLong(1, sid);
                        java.sql.ResultSet rs = ps.executeQuery();
                        VBox root = new VBox(8); root.setStyle("-fx-padding:12; -fx-background-color:#fff;");
                        Label header = new Label("Invoice #" + sid); header.setStyle("-fx-font-size:16; -fx-font-weight:bold;"); root.getChildren().add(header);
                        TableView<java.util.Map<String,Object>> tbl = new TableView<>(); tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                        javafx.collections.ObservableList<java.util.Map<String,Object>> rws = javafx.collections.FXCollections.observableArrayList();
                        while (rs.next()) {
                            java.util.Map<String,Object> m = new java.util.HashMap<>();
                            m.put("productId", rs.getString("product_id")); m.put("product", rs.getString("product_name")); m.put("qty", rs.getInt("quantity_sold")); m.put("price", rs.getDouble("sale_price")); m.put("total", rs.getDouble("total"));
                            rws.add(m);
                        }
                        TableColumn<java.util.Map<String,Object>, String> c0 = new TableColumn<>("Product ID"); c0.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty((String)cd.getValue().get("productId")));
                        TableColumn<java.util.Map<String,Object>, String> c1 = new TableColumn<>("Product"); c1.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty((String)cd.getValue().get("product")));
                        TableColumn<java.util.Map<String,Object>, String> c2 = new TableColumn<>("Qty"); c2.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().get("qty"))));
                        TableColumn<java.util.Map<String,Object>, String> c3 = new TableColumn<>("Unit Price"); c3.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cd.getValue().get("price"))));
                        TableColumn<java.util.Map<String,Object>, String> c4 = new TableColumn<>("Total"); c4.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cd.getValue().get("total"))));
                        tbl.getColumns().addAll(java.util.Arrays.asList(c0, c1, c2, c3, c4)); tbl.setItems(rws);
                        root.getChildren().add(tbl);
                        Button back = new Button("← Back"); back.setOnAction(ae -> {
                            // restore daily transactions page and controls
                            setPosControlsVisible(false);
                            showDailyTransactions();
                        });
                        root.getChildren().add(back);
                        rightStack.getChildren().setAll(createScrollable(root));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
            return r;
        });
        page.getChildren().addAll(topActs, title, table);
        rightStack.getChildren().setAll(createScrollable(page));

        expCsv.setOnAction(e -> {
            DailyTxn sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a transaction row first").showAndWait(); return; }
            String sfx = computeExportSuffixForSales(sel.getSaleId());
            String fname = "invoice_" + sel.getSaleId() + sfx + ".csv";
            String out = util.InvoiceExporter.resolveCashierExportPath(fname);
            util.InvoiceExporter.exportInvoiceToCsvToPath(sel.getSaleId(), out);
        });
        expPdf.setOnAction(e -> {
            DailyTxn sel = table.getSelectionModel().getSelectedItem(); if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a transaction row first").showAndWait(); return; }
            String sfxp = computeExportSuffixForSales(sel.getSaleId());
            String fnamep = "invoice_" + sel.getSaleId() + sfxp + ".pdf";
            String outp = util.InvoiceExporter.resolveCashierExportPath(fnamep);
            util.InvoiceExporter.exportInvoiceToPdfToPath(sel.getSaleId(), outp);
        });
        expXlsx.setOnAction(e -> {
            DailyTxn sel = table.getSelectionModel().getSelectedItem(); if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a transaction row first").showAndWait(); return; }
            String sfxx = computeExportSuffixForSales(sel.getSaleId());
            String fnamex = "invoice_" + sel.getSaleId() + sfxx + ".xlsx";
            String outx = util.InvoiceExporter.resolveCashierExportPath(fnamex);
            util.InvoiceExporter.exportInvoiceToXlsxToPath(sel.getSaleId(), outx);
        });
    }

    // Small helper to wrap a node in a ScrollPane
    private javafx.scene.control.ScrollPane createScrollable(javafx.scene.Node content) {
        // ensure the app window is maximized for consistent layout
        try { javafx.stage.Stage st = (javafx.stage.Stage) (content.getScene() != null ? content.getScene().getWindow() : null); if (st != null) st.setMaximized(true); } catch (Exception ignored) {}
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // append footer if not already present
        try {
            if (content instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane p = (javafx.scene.layout.Pane) content;
                boolean hasFooter = p.getChildrenUnmodifiable().stream().filter(n -> n instanceof javafx.scene.layout.HBox).map(n -> (javafx.scene.layout.HBox)n).anyMatch(h -> h.getChildren().stream().anyMatch(c -> c instanceof javafx.scene.control.Label && ((javafx.scene.control.Label)c).getText().contains("© QuickMart")));
                if (!hasFooter) p.getChildren().add(createCopyrightNodeCashier());
            }
        } catch (Exception ignored) {}
        return sp;
    }

    // Export invoice (main_sales + sales rows) to CSV
    private void exportInvoiceToCsv(long salesId, String filename) {
        // Delegate to shared exporter to avoid duplication
        util.InvoiceExporter.exportInvoiceToCsv(salesId, filename);
    }

    // PDF export for an invoice using iText
    private void exportInvoiceToPdf(long salesId, String filename) {
        // Delegate to shared exporter
        util.InvoiceExporter.exportInvoiceToPdf(salesId, filename);
    }

    // XLSX export for an invoice using Apache POI
    private void exportInvoiceToXlsx(long salesId, String filename) {
        // Delegate to shared exporter
        util.InvoiceExporter.exportInvoiceToXlsx(salesId, filename);
    }

    public static class DailyTxn {
        public long saleId; public String saleTime; public String grandTotal; public String customerId; public String customerName;
        public DailyTxn(long saleId, String saleTime, String grandTotal, String customerId, String customerName) { this.saleId=saleId; this.saleTime=saleTime; this.grandTotal=grandTotal; this.customerId=customerId; this.customerName=customerName; }
        public long getSaleId() { return saleId; }
        public String getSaleTime() { return saleTime; }
        public String getGrandTotal() { return grandTotal; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
    }
    
    // small helper alert used by cashier export helpers
    private void showAlert(String message) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // Compute a sanitized filename suffix for a sales record. Prefer customer name, else cashier username+role.
    private String computeExportSuffixForSales(long salesId) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT customer_name, cashier_id FROM main_sales WHERE sales_id = ? LIMIT 1");
            ps.setLong(1, salesId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String cname = rs.getString("customer_name");
                String cashierId = rs.getString("cashier_id");
                if (cname != null && !cname.trim().isEmpty() && !cname.equalsIgnoreCase("Unknown")) {
                    return "_" + sanitizeForFilename(cname.trim()) + "_Customer";
                }
                // fallback: lookup cashier username and role from users table
                if (cashierId != null && !cashierId.trim().isEmpty()) {
                    try (java.sql.PreparedStatement us = conn.prepareStatement("SELECT username, role FROM users WHERE id = ? LIMIT 1")) {
                        us.setString(1, cashierId);
                        java.sql.ResultSet urs = us.executeQuery();
                        if (urs.next()) {
                            String uname = urs.getString("username");
                            String role = urs.getString("role");
                            if (uname != null && !uname.trim().isEmpty()) {
                                String suf = "_" + sanitizeForFilename(uname.trim());
                                if (role != null && !role.trim().isEmpty()) suf += "_" + sanitizeForFilename(role);
                                return suf;
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception ignore) {}
        return "_Unknown_Customer";
    }

    // Centralized helper to show/hide POS controls (search field and scan button)
    private void setPosControlsVisible(boolean visible) {
        try {
            if (productSearchField != null) { productSearchField.setVisible(visible); productSearchField.setManaged(visible); }
            if (scanBtn != null) { scanBtn.setVisible(visible); scanBtn.setManaged(visible); }
        } catch (Exception ignored) {}
    }

    // Apply an active CSS class to the provided sidebar button and remove from siblings
    private void setActiveSidebarButton(javafx.scene.control.Button btn) {
        try {
            // find a parent that likely holds the sidebar buttons: rightStack's parent or scene root
            javafx.scene.Parent root = null;
            if (rightStack != null && rightStack.getScene() != null) root = rightStack.getScene().getRoot();
            if (root == null) return;
            // remove class from any button inside the root's scene graph matching style class
            for (javafx.scene.Node n : root.lookupAll("Button")) {
                try { if (n instanceof javafx.scene.control.Button) ((javafx.scene.control.Button)n).getStyleClass().removeAll("active-sidebar-btn"); } catch (Exception ignored) {}
            }
            if (btn != null) btn.getStyleClass().add("active-sidebar-btn");
        } catch (Exception ignored) {}
    }

    private String sanitizeForFilename(String s) {
        if (s == null) return "";
        // remove problematic chars, replace spaces with underscore
    // preserve original capitalization for readability in filenames, only remove illegal chars and collapse spaces
    return s.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", "_").trim();
    }

    // Pages: Guidelines, Privacy, About for cashier
    private void showGuidelinesPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("Loyalty & Guidelines"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label(); body.setWrapText(true);
    body.setText("Loyalty Points mechanism:\n\n1 point = Rs 100 of items total (before tax/discount). If a customer's existing loyalty points are >= 500 at the time of payment, the system will automatically redeem 500 points to apply an immediate Rs 1500 discount to the sale and record a caption \"Loyalty Point redeemed!(Discount Added: Rs 1500)\" on the invoice. Earned points from the current sale are computed as floor(items_total / 100) and added to the customer's balance after redemption is applied.\n\nDiscount overflow rule:\nIf a discount (manual or computed) exceeds the sum of the sale's items total plus tax (i.e., discount > sales_total + tax), the system caps the effective discount so the payable amount never goes negative. Any excess discount is converted into loyalty points at 1 point per Rs 100 and recorded on the invoice as a caption such as \"Excess discount converted to loyalty points\". This preserves customer value without creating negative payable amounts.");
        box.getChildren().addAll(title, body);
        box.getChildren().add(createCopyrightNodeCashier());
        rightStack.getChildren().setAll(createScrollable(box));
    }

    private void showPrivacyPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("Privacy Policy"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label(); body.setWrapText(true);
    body.setText("At QuickMart, we take your privacy seriously.\nWe are committed to securely handling your data and ensuring confidentiality.\n\nYour information is protected, and we continuously monitor our security measures to safeguard against unauthorized access, disclosure, or misuse.\n");
        box.getChildren().addAll(title, body);
        box.getChildren().add(createCopyrightNodeCashier());
        rightStack.getChildren().setAll(createScrollable(box));
    }

    private void showAboutPage() {
        VBox box = new VBox(12); box.setStyle("-fx-padding:20; -fx-background-color:#fff;");
        Label title = new Label("About Us"); title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        javafx.scene.control.Label body = new javafx.scene.control.Label(); body.setWrapText(true);
    body.setText("Welcome to QuickMart (TransferMaster in project description).\n\nOur Mission:\nRevolutionize retail point-of-sale experiences with a modern JavaFX desktop app.\n\nTechnologies used: Java, JavaFX, JDBC. Key libraries include ZXing (barcode generation), Apache POI (XLSX), and iText (PDF).\n\nTeam:\n- Prajjwal Maharjan (Lead Developer)\n- Rabin Pulami Magar\n- Durga Budha");
        box.getChildren().addAll(title, body);
        box.getChildren().add(createCopyrightNodeCashier());
        rightStack.getChildren().setAll(createScrollable(box));
    }

    private HBox createCopyrightNodeCashier() {
        HBox foot = new HBox(); foot.setStyle("-fx-alignment:center-right; -fx-padding:12 8 8 8;");
        Label copy = new Label("© QuickMart 2025. All rights reserved. Made by Prajjwal Maharjan Team");
        copy.setStyle("-fx-text-fill:#666; -fx-font-size:11;");
        foot.getChildren().add(copy);
        return foot;
    }

}
