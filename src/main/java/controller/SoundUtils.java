package controller;

import javax.sound.sampled.*;

public class SoundUtils {
    // simple beep using system speaker via javax.sound.sampled
    public static void beepShort() {
        try {
            final float SAMPLE_RATE = 8000f;
            byte[] buf = new byte[1];
            AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine sdl = AudioSystem.getSourceDataLine(af)) {
                sdl.open(af);
                sdl.start();
                // generate 2 short beeps
                for (int k = 0; k < 2; k++) {
                    for (int i = 0; i < SAMPLE_RATE * 0.06; i++) {
                        double angle = i / (SAMPLE_RATE / 440.0) * 2.0 * Math.PI;
                        buf[0] = (byte)(Math.sin(angle) * 100);
                        sdl.write(buf, 0, 1);
                    }
                    try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                }
                sdl.drain();
                sdl.stop();
            }
        } catch (Exception ignored) {}
    }
}
