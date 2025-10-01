package controller;

import com.github.sarxos.webcam.Webcam; 
// import com.github.sarxos.webcam.WebcamResolution; // Unused import
import java.awt.Dimension;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BarcodeScanner {
    private Webcam webcam;
    private ScheduledExecutorService executor;
    private BarcodeScanCallback callback;

    public interface BarcodeScanCallback { void onBarcodeScanned(String barcodeData); void onError(String errorMessage); }

    public void startScanning(BarcodeScanCallback callback) {
        this.callback = callback;
        try {
            // ensure previous scanner is stopped first
            stopScanning();
            webcam = Webcam.getDefault();
            if (webcam == null) { callback.onError("No webcam found"); return; }
            // pick a sensible resolution (prefer >= 640 width)
            Dimension best = null;
            for (Dimension d : webcam.getViewSizes()) {
                if (d.width >= 1280) { best = d; break; }
                if (best == null || d.width > best.width) best = d;
            }
            try {
                if (!webcam.isOpen()) {
                    if (best != null) webcam.setViewSize(best);
                    webcam.open();
                }
            } catch (Exception e) {
                // if resolution change fails because webcam already in use, try opening without changing size
                try { if (!webcam.isOpen()) webcam.open(); } catch (Exception ex) { callback.onError("Cannot open webcam: " + ex.getMessage()); return; }
            }
            executor = Executors.newSingleThreadScheduledExecutor();
            // use scheduleWithFixedDelay to give camera some breathing room
            executor.scheduleWithFixedDelay(this::scanFrame, 0, 150, TimeUnit.MILLISECONDS);
        } catch (Exception ex) { callback.onError(ex.getMessage()); }
    }

    private void scanFrame() {
        try {
            BufferedImage img = webcam.getImage(); if (img==null) return;
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            if (result != null) { stopScanning(); try { controller.SoundUtils.beepShort(); } catch (Throwable ignored) {} callback.onBarcodeScanned(result.getText()); }
        } catch (NotFoundException e) { /* no barcode this frame */ } catch (Exception ex) { stopScanning(); callback.onError(ex.getMessage()); }
    }

    public void stopScanning() {
        try {
            if (executor != null) { try { executor.shutdownNow(); executor.awaitTermination(300, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {} executor = null; }
            if (webcam != null) {
                try { if (webcam.isOpen()) webcam.close(); } catch (Exception ignored) {}
                webcam = null;
            }
        } catch (Exception ignored) {}
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
}
