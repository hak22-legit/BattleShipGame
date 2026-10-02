package com.battleship.view;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;

/** renders a string as a qr code image entirely offline (no network calls). */
public final class QrCodeUtil {

    private QrCodeUtil() { }

    public static WritableImage generate(String content, int pixelSize) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, pixelSize, pixelSize, hints);

            WritableImage image = new WritableImage(pixelSize, pixelSize);
            PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < pixelSize; y++) {
                for (int x = 0; x < pixelSize; x++) {
                    writer.setColor(x, y, matrix.get(x, y) ? Color.web("#081a2d") : Color.web("#f5f7fa"));
                }
            }
            return image;
        } catch (WriterException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
