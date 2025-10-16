package util;

// Shared invoice export helpers reused by Admin and Cashier controllers
public class InvoiceExporter {

    public static void exportInvoiceToCsv(long salesId, String filename) {
        try {
          String outPath = resolveExportPath(salesId, filename);
          try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(outPath));
              java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    pw.println("Invoice," + salesId);
                    pw.println("Cashier," + cashierId);
                    pw.println("Customer ID," + (rs.getString("customer_id") == null ? "-" : rs.getString("customer_id")));
                    pw.println("Customer Name," + (rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name")));
                    pw.println("Sale Time," + rs.getTimestamp("sale_time"));
                    pw.println();
                }
                pw.println("Product ID,Product,Qty,Unit Price,Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    pw.println(String.format("%s,%s,%d,%.2f,%.2f", rs2.getString("product_id"), rs2.getString("product_name").replaceAll(",", ""), rs2.getInt("quantity_sold"), rs2.getDouble("sale_price"), rs2.getDouble("total")));
                }
                try {
                    if (salesTotal != null) {
                        pw.println();
                        pw.println("Sales Total," + salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        pw.println("Tax," + (tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        pw.println("Discount," + (discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        pw.println("Grand Total," + (grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) pw.println("Caption," + cap.replaceAll(",", "")); } } }
                    }
                } catch (Exception ignore) {}
            }
            showAlert("Invoice CSV exported to " + new java.io.File(resolveExportPath(salesId, filename)).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("Export failed: " + ex.getMessage()); }
    }

    public static void exportInvoiceToPdf(long salesId, String filename) {
        try {
          String outPath = resolveExportPath(salesId, filename);
          try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outPath);
              java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
          com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
          com.itextpdf.text.pdf.PdfWriter.getInstance(doc, fos);
          doc.open();
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    doc.add(new com.itextpdf.text.Paragraph("Invoice " + salesId));
                    doc.add(new com.itextpdf.text.Paragraph("Cashier: " + cashierId));
                    doc.add(new com.itextpdf.text.Paragraph("Customer ID: " + (rs.getString("customer_id") == null ? "-" : rs.getString("customer_id"))));
                    doc.add(new com.itextpdf.text.Paragraph("Customer Name: " + (rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name"))));
                    doc.add(new com.itextpdf.text.Paragraph("Sale Time: " + rs.getTimestamp("sale_time")));
                    doc.add(new com.itextpdf.text.Paragraph(" "));
                }
                com.itextpdf.text.pdf.PdfPTable pt = new com.itextpdf.text.pdf.PdfPTable(5);
                pt.addCell("Product ID"); pt.addCell("Product"); pt.addCell("Qty"); pt.addCell("Unit Price"); pt.addCell("Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    pt.addCell(rs2.getString("product_id"));
                    pt.addCell(rs2.getString("product_name"));
                    pt.addCell(String.valueOf(rs2.getInt("quantity_sold")));
                    pt.addCell(String.format("%.2f", rs2.getDouble("sale_price")));
                    pt.addCell(String.format("%.2f", rs2.getDouble("total")));
                }
                doc.add(pt);
                try {
                    if (salesTotal != null) {
                        doc.add(new com.itextpdf.text.Paragraph(" "));
                        doc.add(new com.itextpdf.text.Paragraph("Sales Total: " + salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        doc.add(new com.itextpdf.text.Paragraph("Tax: " + (tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        doc.add(new com.itextpdf.text.Paragraph("Discount: " + (discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        doc.add(new com.itextpdf.text.Paragraph("Grand Total: " + (grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) doc.add(new com.itextpdf.text.Paragraph("Caption: " + cap)); } } }
                    }
                } catch (Exception ignore) {}
                // close document while fos is still open
                doc.close();
            }
            showAlert("Invoice PDF exported to " + new java.io.File(resolveExportPath(salesId, filename)).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("PDF export failed: " + ex.getMessage()); }
    }

    public static void exportInvoiceToXlsx(long salesId, String filename) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Invoice");
            int row = 0;
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    org.apache.poi.ss.usermodel.Row r0 = sheet.createRow(row++); r0.createCell(0).setCellValue("Invoice"); r0.createCell(1).setCellValue(String.valueOf(salesId));
                    org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(row++); r1.createCell(0).setCellValue("Cashier"); r1.createCell(1).setCellValue(cashierId);
                    org.apache.poi.ss.usermodel.Row r2 = sheet.createRow(row++); r2.createCell(0).setCellValue("Customer ID"); r2.createCell(1).setCellValue(rs.getString("customer_id") == null ? "-" : rs.getString("customer_id"));
                    org.apache.poi.ss.usermodel.Row r3 = sheet.createRow(row++); r3.createCell(0).setCellValue("Customer Name"); r3.createCell(1).setCellValue(rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name"));
                    row++;
                }
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(row++);
                header.createCell(0).setCellValue("Product ID"); header.createCell(1).setCellValue("Product"); header.createCell(2).setCellValue("Qty"); header.createCell(3).setCellValue("Unit Price"); header.createCell(4).setCellValue("Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    org.apache.poi.ss.usermodel.Row rr = sheet.createRow(row++);
                    rr.createCell(0).setCellValue(rs2.getString("product_id"));
                    rr.createCell(1).setCellValue(rs2.getString("product_name"));
                    rr.createCell(2).setCellValue(rs2.getInt("quantity_sold"));
                    rr.createCell(3).setCellValue(rs2.getDouble("sale_price"));
                    rr.createCell(4).setCellValue(rs2.getDouble("total"));
                }
                try {
                    if (salesTotal != null) {
                        row++;
                        org.apache.poi.ss.usermodel.Row s1 = sheet.createRow(row++); s1.createCell(0).setCellValue("Sales Total"); s1.createCell(1).setCellValue(salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s2 = sheet.createRow(row++); s2.createCell(0).setCellValue("Tax"); s2.createCell(1).setCellValue(tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s3 = sheet.createRow(row++); s3.createCell(0).setCellValue("Discount"); s3.createCell(1).setCellValue(discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s4 = sheet.createRow(row++); s4.createCell(0).setCellValue("Grand Total"); s4.createCell(1).setCellValue(grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) { org.apache.poi.ss.usermodel.Row sc = sheet.createRow(row++); sc.createCell(0).setCellValue("Caption"); sc.createCell(1).setCellValue(cap); } } } }
                    }
                } catch (Exception ignore) {}
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(resolveExportPath(salesId, filename))) { wb.write(fos); }
            showAlert("Invoice XLSX exported to " + new java.io.File(resolveExportPath(salesId, filename)).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("XLSX export failed: " + ex.getMessage()); }
    }

    // Variants that accept an explicit output path (used by UI contexts that must force a target folder)
    public static void exportInvoiceToCsvToPath(long salesId, String outPath) {
        try {
          try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(outPath));
              java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    pw.println("Invoice," + salesId);
                    pw.println("Cashier," + cashierId);
                    pw.println("Customer ID," + (rs.getString("customer_id") == null ? "-" : rs.getString("customer_id")));
                    pw.println("Customer Name," + (rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name")));
                    pw.println("Sale Time," + rs.getTimestamp("sale_time"));
                    pw.println();
                }
                pw.println("Product ID,Product,Qty,Unit Price,Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    pw.println(String.format("%s,%s,%d,%.2f,%.2f", rs2.getString("product_id"), rs2.getString("product_name").replaceAll(",", ""), rs2.getInt("quantity_sold"), rs2.getDouble("sale_price"), rs2.getDouble("total")));
                }
                try {
                    if (salesTotal != null) {
                        pw.println();
                        pw.println("Sales Total," + salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        pw.println("Tax," + (tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        pw.println("Discount," + (discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        pw.println("Grand Total," + (grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) pw.println("Caption," + cap.replaceAll(",", "")); } } }
                    }
                } catch (Exception ignore) {}
            }
            showAlert("Invoice CSV exported to " + new java.io.File(outPath).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("Export failed: " + ex.getMessage()); }
    }

    public static void exportInvoiceToPdfToPath(long salesId, String outPath) {
        try {
          try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outPath);
              java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
          com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
          com.itextpdf.text.pdf.PdfWriter.getInstance(doc, fos);
          doc.open();
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    doc.add(new com.itextpdf.text.Paragraph("Invoice " + salesId));
                    doc.add(new com.itextpdf.text.Paragraph("Cashier: " + cashierId));
                    doc.add(new com.itextpdf.text.Paragraph("Customer ID: " + (rs.getString("customer_id") == null ? "-" : rs.getString("customer_id"))));
                    doc.add(new com.itextpdf.text.Paragraph("Customer Name: " + (rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name"))));
                    doc.add(new com.itextpdf.text.Paragraph("Sale Time: " + rs.getTimestamp("sale_time")));
                    doc.add(new com.itextpdf.text.Paragraph(" "));
                }
                com.itextpdf.text.pdf.PdfPTable pt = new com.itextpdf.text.pdf.PdfPTable(5);
                pt.addCell("Product ID"); pt.addCell("Product"); pt.addCell("Qty"); pt.addCell("Unit Price"); pt.addCell("Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    pt.addCell(rs2.getString("product_id"));
                    pt.addCell(rs2.getString("product_name"));
                    pt.addCell(String.valueOf(rs2.getInt("quantity_sold")));
                    pt.addCell(String.format("%.2f", rs2.getDouble("sale_price")));
                    pt.addCell(String.format("%.2f", rs2.getDouble("total")));
                }
                doc.add(pt);
                try {
                    if (salesTotal != null) {
                        doc.add(new com.itextpdf.text.Paragraph(" "));
                        doc.add(new com.itextpdf.text.Paragraph("Sales Total: " + salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
                        doc.add(new com.itextpdf.text.Paragraph("Tax: " + (tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        doc.add(new com.itextpdf.text.Paragraph("Discount: " + (discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        doc.add(new com.itextpdf.text.Paragraph("Grand Total: " + (grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) doc.add(new com.itextpdf.text.Paragraph("Caption: " + cap)); } } }
                    }
                } catch (Exception ignore) {}
                doc.close();
            }
            showAlert("Invoice PDF exported to " + new java.io.File(outPath).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("PDF export failed: " + ex.getMessage()); }
    }

    public static void exportInvoiceToXlsxToPath(long salesId, String outPath) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Invoice");
            int row = 0;
            try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
                java.sql.PreparedStatement ms = conn.prepareStatement("SELECT cashier_id, customer_id, customer_name, sales_total, tax, discount, grand_total, sale_time FROM main_sales WHERE sales_id = ? LIMIT 1");
                ms.setLong(1, salesId);
                java.sql.ResultSet rs = ms.executeQuery();
                java.math.BigDecimal salesTotal = null, tax = null, discount = null, grandTotal = null;
                String cashierId = null;
                if (rs.next()) {
                    cashierId = rs.getString("cashier_id");
                    salesTotal = rs.getBigDecimal("sales_total");
                    tax = rs.getBigDecimal("tax");
                    discount = rs.getBigDecimal("discount");
                    grandTotal = rs.getBigDecimal("grand_total");
                    org.apache.poi.ss.usermodel.Row r0 = sheet.createRow(row++); r0.createCell(0).setCellValue("Invoice"); r0.createCell(1).setCellValue(String.valueOf(salesId));
                    org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(row++); r1.createCell(0).setCellValue("Cashier"); r1.createCell(1).setCellValue(cashierId);
                    org.apache.poi.ss.usermodel.Row r2 = sheet.createRow(row++); r2.createCell(0).setCellValue("Customer ID"); r2.createCell(1).setCellValue(rs.getString("customer_id") == null ? "-" : rs.getString("customer_id"));
                    org.apache.poi.ss.usermodel.Row r3 = sheet.createRow(row++); r3.createCell(0).setCellValue("Customer Name"); r3.createCell(1).setCellValue(rs.getString("customer_name") == null ? "Unknown" : rs.getString("customer_name"));
                    row++;
                }
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(row++);
                header.createCell(0).setCellValue("Product ID"); header.createCell(1).setCellValue("Product"); header.createCell(2).setCellValue("Qty"); header.createCell(3).setCellValue("Unit Price"); header.createCell(4).setCellValue("Total");
                java.sql.PreparedStatement sps = conn.prepareStatement("SELECT product_id, product_name, quantity_sold, sale_price, total FROM sales WHERE sales_id = ?");
                sps.setLong(1, salesId);
                java.sql.ResultSet rs2 = sps.executeQuery();
                while (rs2.next()) {
                    org.apache.poi.ss.usermodel.Row rr = sheet.createRow(row++);
                    rr.createCell(0).setCellValue(rs2.getString("product_id"));
                    rr.createCell(1).setCellValue(rs2.getString("product_name"));
                    rr.createCell(2).setCellValue(rs2.getInt("quantity_sold"));
                    rr.createCell(3).setCellValue(rs2.getDouble("sale_price"));
                    rr.createCell(4).setCellValue(rs2.getDouble("total"));
                }
                try {
                    if (salesTotal != null) {
                        row++;
                        org.apache.poi.ss.usermodel.Row s1 = sheet.createRow(row++); s1.createCell(0).setCellValue("Sales Total"); s1.createCell(1).setCellValue(salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s2 = sheet.createRow(row++); s2.createCell(0).setCellValue("Tax"); s2.createCell(1).setCellValue(tax == null ? "0.00" : tax.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s3 = sheet.createRow(row++); s3.createCell(0).setCellValue("Discount"); s3.createCell(1).setCellValue(discount == null ? "0.00" : discount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        org.apache.poi.ss.usermodel.Row s4 = sheet.createRow(row++); s4.createCell(0).setCellValue("Grand Total"); s4.createCell(1).setCellValue(grandTotal == null ? salesTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : grandTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                        try (java.sql.PreparedStatement cps = conn.prepareStatement("SELECT caption FROM main_sales WHERE sales_id = ? LIMIT 1")) { cps.setLong(1, salesId); try (java.sql.ResultSet rcap = cps.executeQuery()) { if (rcap.next()) { String cap = rcap.getString(1); if (cap != null && !cap.trim().isEmpty()) { org.apache.poi.ss.usermodel.Row sc = sheet.createRow(row++); sc.createCell(0).setCellValue("Caption"); sc.createCell(1).setCellValue(cap); } } } }
                    }
                } catch (Exception ignore) {}
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outPath)) { wb.write(fos); }
            showAlert("Invoice XLSX exported to " + new java.io.File(outPath).getAbsolutePath());
        } catch (Exception ex) { ex.printStackTrace(); showAlert("XLSX export failed: " + ex.getMessage()); }
    }

    // Resolve a destination path for an export based on the sale's customer/cashier and create directories as needed.
    private static String resolveExportPath(long salesId, String filename) {
        try (java.sql.Connection conn = database.DatabaseConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT customer_id, cashier_id FROM main_sales WHERE sales_id = ? LIMIT 1");
            ps.setLong(1, salesId);
            java.sql.ResultSet rs = ps.executeQuery();
            String customerId = null; String cashierId = null;
            if (rs.next()) { customerId = rs.getString("customer_id"); cashierId = rs.getString("cashier_id"); }
            java.io.File base;
            // If there's a customer id, place into customers folder. For unregistered sales customer_id is stored as "-"; treat those as Unknown customer folder.
            if (customerId != null && !customerId.trim().isEmpty()) {
                if (customerId.equals("-")) {
                    base = new java.io.File("exports" + java.io.File.separator + "customers" + java.io.File.separator + "Unknown");
                } else {
                    base = new java.io.File("exports" + java.io.File.separator + "customers" + java.io.File.separator + sanitize(customerId));
                }
            } else {
                // If no customer id, fallback to cashier or admin folders
                String currentCashier = System.getProperty("cashier.id", "");
                if (cashierId != null && !cashierId.trim().isEmpty() && cashierId.equals(currentCashier)) {
                    base = new java.io.File("exports" + java.io.File.separator + "cashier");
                } else {
                    base = new java.io.File("exports" + java.io.File.separator + "admin");
                }
            }
            if (!base.exists()) base.mkdirs();
            java.io.File out = new java.io.File(base, filename);
            return out.getAbsolutePath();
        } catch (Exception ex) { try { java.io.File fallback = new java.io.File("exports"); if (!fallback.exists()) fallback.mkdirs(); return new java.io.File(fallback, filename).getAbsolutePath(); } catch (Exception ignored) {} }
        return filename;
    }

    // Public helper for admin-level exports (generic reports / snapshots)
    public static String resolveAdminExportPath(String filename) {
        try {
            java.io.File base = new java.io.File("exports" + java.io.File.separator + "admin");
            if (!base.exists()) base.mkdirs();
            return new java.io.File(base, filename).getAbsolutePath();
        } catch (Exception ex) {
            try { java.io.File fallback = new java.io.File("exports"); if (!fallback.exists()) fallback.mkdirs(); return new java.io.File(fallback, filename).getAbsolutePath(); } catch (Exception ignored) {}
        }
        return filename;
    }

    // Public helper for cashier-level generic exports
    public static String resolveCashierExportPath(String filename) {
        try {
            java.io.File base = new java.io.File("exports" + java.io.File.separator + "cashier");
            if (!base.exists()) base.mkdirs();
            return new java.io.File(base, filename).getAbsolutePath();
        } catch (Exception ex) {
            try { java.io.File fallback = new java.io.File("exports"); if (!fallback.exists()) fallback.mkdirs(); return new java.io.File(fallback, filename).getAbsolutePath(); } catch (Exception ignored) {}
        }
        return filename;
    }

    private static String sanitize(String s) { if (s == null) return ""; return s.replaceAll("[^A-Za-z0-9_-]", "_"); }

    private static void showAlert(String message) {
        try {
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setHeaderText(null);
                a.setContentText(message);
                a.showAndWait();
            });
        } catch (Exception ignored) {}
    }
}
