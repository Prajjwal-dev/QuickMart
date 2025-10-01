package controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import javafx.scene.image.Image;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

public class BarcodeManager {
    public static boolean generateAndSaveBarcode(String productId) {
        try {
            String barcodeData = productId;
            String filePath = "barcodes/" + barcodeData + ".png";
            Path p = Paths.get(filePath);
            if (Files.exists(p)) return true; // don't overwrite existing barcode files
            BitMatrix matrix = new MultiFormatWriter().encode(barcodeData, BarcodeFormat.CODE_128, 300, 100);
            MatrixToImageWriter.writeToPath(matrix, "PNG", p);
            // DB update handled by callers if needed
            return true;
        } catch (Exception ex) { ex.printStackTrace(); return false; }
    }

    public static Image generateBarcodeImage(String barcodeData) {
        try {
            String tmp = "barcodes/" + barcodeData + ".png";
            Path p = Paths.get(tmp);
            if (!Files.exists(p)) {
                BitMatrix matrix = new MultiFormatWriter().encode(barcodeData, BarcodeFormat.CODE_128, 300, 100);
                MatrixToImageWriter.writeToPath(matrix, "PNG", p);
            }
            return new Image("file:" + tmp);
        } catch (Exception ex) { ex.printStackTrace(); return null; }
    }

    public static String getBarcodeFilePath(String barcodeData) {
        return "barcodes/" + barcodeData + ".png";
    }
}
