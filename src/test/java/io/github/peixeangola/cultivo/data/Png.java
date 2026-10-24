package io.github.peixeangola.cultivo.data;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Pixels ARGB de uma textura gerada (para os testes de arte lerem cor, alfa e silhueta). */
record Png(int width, int height, int[] argb) {
    static Png read(Path file) {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                throw new IOException("não é imagem: " + file);
            }
            int w = image.getWidth();
            int h = image.getHeight();
            return new Png(w, h, image.getRGB(0, 0, w, h, null, 0, w));
        } catch (IOException e) {
            throw new UncheckedIOException("textura faltando: " + file, e);
        }
    }

    int alpha(int x, int y) {
        return argb[y * width + x] >>> 24;
    }

    /** RGB sem alfa (0xRRGGBB). */
    int rgb(int x, int y) {
        return argb[y * width + x] & 0xFFFFFF;
    }

    /** Silhueta: true onde o pixel é visível. */
    boolean[] mask() {
        boolean[] mask = new boolean[argb.length];
        for (int i = 0; i < argb.length; i++) {
            mask[i] = (argb[i] >>> 24) != 0;
        }
        return mask;
    }

    /** Cor opaca mais frequente (0xRRGGBB). */
    int dominantColor() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int c : argb) {
            if ((c >>> 24) != 0) {
                counts.merge(c & 0xFFFFFF, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    static double distance(int rgbA, int rgbB) {
        int dr = ((rgbA >> 16) & 0xFF) - ((rgbB >> 16) & 0xFF);
        int dg = ((rgbA >> 8) & 0xFF) - ((rgbB >> 8) & 0xFF);
        int db = (rgbA & 0xFF) - (rgbB & 0xFF);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
