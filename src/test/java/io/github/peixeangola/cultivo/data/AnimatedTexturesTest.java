package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Texturas animadas (&lt;nome&gt;.png + &lt;nome&gt;.png.mcmeta, como a água vanilla): quadros quadrados empilhados,
 * frametime positivo, quantidade de quadros batendo com a altura e quadros que mudam de verdade.
 */
class AnimatedTexturesTest {
    /** As animações pedidas: brasa e baforada, água do bong, carvão do narguilé, maço balançando e peneira sacudindo. */
    private static final Map<String, Integer> EXPECTED_FRAMES = Map.of(
        "item/joint_lit", 4,
        "item/joint_filtered_lit", 4,
        "item/smoke_ember", 4,
        "item/model_water_bubbling", 8,
        "item/hookah_coal", 6,
        "block/drying_bundle_fresh", 4,
        "block/sieve_flowers", 4);

    /** Todas as texturas com .mcmeta: caminho relativo a textures/ (sem .png) → arquivo .mcmeta. */
    private static Map<String, Path> animated() throws IOException {
        Path root = Resources.assets("textures");
        Map<String, Path> out = new TreeMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path meta : files.filter(p -> p.toString().endsWith(".png.mcmeta")).toList()) {
                String rel = root.relativize(meta).toString().replace('\\', '/');
                out.put(rel.substring(0, rel.length() - ".png.mcmeta".length()), meta);
            }
        }
        return out;
    }

    @Test
    void theRequestedAnimationsExist() throws IOException {
        assertEquals(EXPECTED_FRAMES.keySet(), animated().keySet());
    }

    @Test
    void everyMcmetaMatchesItsStrip() throws IOException {
        for (Map.Entry<String, Path> entry : animated().entrySet()) {
            String name = entry.getKey();
            JsonObject mcmeta = Resources.json(entry.getValue());
            assertEquals(1, mcmeta.size(), name + ": só a seção animation");
            JsonObject animation = mcmeta.getAsJsonObject("animation");
            int frametime = animation.get("frametime").getAsInt();
            assertTrue(frametime > 0, name + " frametime " + frametime);
            assertFalse(animation.has("interpolate") && animation.get("interpolate").getAsBoolean(),
                name + ": interpolar criaria alfa parcial entre quadros");
            Png png = Png.read(Resources.assets("textures/" + name + ".png"));
            assertEquals(16, png.width(), name + " largura");
            assertEquals(0, png.height() % png.width(), name + ": quadros quadrados");
            int frames = png.height() / png.width();
            assertEquals(EXPECTED_FRAMES.get(name), frames, name + " quadros");
            if (animation.has("frames")) {
                for (JsonElement index : animation.getAsJsonArray("frames")) {
                    int i = index.isJsonObject() ? index.getAsJsonObject().get("index").getAsInt() : index.getAsInt();
                    assertTrue(i >= 0 && i < frames, name + " quadro " + i);
                }
            }
        }
    }

    /** Animação que anima: quadros seguidos diferentes e pelo menos dois quadros distintos. */
    @Test
    void consecutiveFramesChange() throws IOException {
        for (String name : animated().keySet()) {
            Png png = Png.read(Resources.assets("textures/" + name + ".png"));
            int frames = png.height() / 16;
            int changes = 0;
            for (int f = 0; f < frames; f++) {
                int[] a = Arrays.copyOfRange(png.argb(), f * 256, (f + 1) * 256);
                int[] b = Arrays.copyOfRange(png.argb(), ((f + 1) % frames) * 256, (((f + 1) % frames) + 1) * 256);
                if (!Arrays.equals(a, b)) {
                    changes++;
                }
            }
            assertTrue(changes >= frames / 2, name + ": só " + changes + " trocas em " + frames + " quadros");
        }
    }

    /** O maço balança só embaixo: o barbante e o topo (linhas 2–7) ficam parados, a ponta (linhas 8–9) se mexe. */
    @Test
    void bundleSwaysOnlyAtTheBottom() {
        Png png = Png.read(Resources.assets("textures/block/drying_bundle_fresh.png"));
        boolean bottomMoves = false;
        for (int f = 1; f < 4; f++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean same = png.argb()[y * 16 + x] == png.argb()[(f * 16 + y) * 16 + x];
                    if (y < 8) {
                        assertTrue(same, "topo do maço mexeu no quadro " + f + " em " + x + "," + y);
                    } else if (!same) {
                        bottomMoves = true;
                    }
                }
            }
        }
        assertTrue(bottomMoves, "a ponta do maço deveria balançar");
    }

    /** As bolhas da água do bong sobem: a bolha clara (#ffffff) de cada coluna fica 2 px mais alta no quadro seguinte. */
    @Test
    void bongBubblesRise() {
        Png png = Png.read(Resources.assets("textures/item/model_water_bubbling.png"));
        int column = 1;
        int previous = -1;
        for (int f = 0; f < 8; f++) {
            int y = -1;
            for (int row = 0; row < 16; row++) {
                if ((png.argb()[(f * 16 + row) * 16 + column] & 0xFFFFFF) == 0xFFFFFF) {
                    y = row;
                }
            }
            assertTrue(y >= 0, "sem bolha no quadro " + f);
            if (previous >= 0) {
                assertEquals(Math.floorMod(previous - 2, 16), y, "quadro " + f);
            }
            previous = y;
        }
    }
}
