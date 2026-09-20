package eye.on.the.money.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public final class QrCodes {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private static final String FORMAT = "PNG";

    private static final int WHITE = 0xFFFFFF;

    private static final int BLACK = 0x000000;

    private QrCodes() {
    }

    public static String toPngDataUri(String content, int size) {
        BitMatrix matrix = encode(content, size);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, FORMAT, png);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the QR code image", e);
        }
        return DATA_URI_PREFIX + Base64.getEncoder().encodeToString(png.toByteArray());
    }

    private static BitMatrix encode(String content, int size) {
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalStateException("Could not build the QR code", e);
        }
    }
}
