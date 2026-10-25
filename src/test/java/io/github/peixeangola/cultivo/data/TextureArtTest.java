package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Regras de arte conferidas nos pixels das texturas geradas (plano de arte, seções 2.1–2.3 e 2.6): silhuetas,
 * cores de folha por família e transparência só de corte.
 */
class TextureArtTest {
    private static Png texture(String id) {
        return Png.read(Resources.assets("textures/" + id + ".png"));
    }

    /** A flor seca tem outra grade (menor, sem folhas de açúcar), não só outra paleta. */
    @Test
    void freshAndDriedFlowerSilhouettesDiffer() {
        for (Variety v : Variety.values()) {
            boolean[] fresh = texture("item/" + v.freshFlowerId()).mask();
            boolean[] dried = texture("item/" + v.driedFlowerId()).mask();
            assertFalse(Arrays.equals(fresh, dried), v.id());
            assertTrue(count(dried) < count(fresh), v.id() + ": a seca deveria ser menor");
        }
    }

    /**
     * A folha de cada família (cor mais frequente do estágio 4, só folhas e caule) fica a pelo menos 40 de distância
     * RGB das outras, para cada família se reconhecer no campo. As nativas usam as folhas do plano 2.1.
     */
    @Test
    void familyLeafColoursAreDistinct() {
        List<String> families = AssetCompletenessTest.families();
        assertEquals(12, families.size(), "3 nativas + 9 famílias do catálogo");
        int[] leaf = families.stream().mapToInt(f -> texture("block/" + f + "_crop_stage4").dominantColor()).toArray();
        for (int i = 0; i < leaf.length; i++) {
            for (int j = i + 1; j < leaf.length; j++) {
                double d = Png.distance(leaf[i], leaf[j]);
                assertTrue(d >= 40.0D, families.get(i) + " x " + families.get(j) + ": " + d);
            }
        }
        assertEquals(0x8fd14f, leaf[families.indexOf("brisa")]);
        assertEquals(0x4fa83c, leaf[families.indexOf("aurora")]);
        assertEquals(0x2f6a3a, leaf[families.indexOf("eclipse")]);
    }

    /** A planta silvestre tem grade própria: a silhueta não repete nenhum estágio da lavoura. */
    @Test
    void wildPlantDiffersFromEveryCropStage() {
        boolean[] wild = texture("block/wild_plant").mask();
        for (int stage = 0; stage < 8; stage++) {
            boolean[] crop = texture("block/brisa_crop_stage" + stage).mask();
            assertFalse(Arrays.equals(wild, crop), "planta silvestre igual ao estágio " + stage);
        }
        // Baixa: nada acima da linha 5
        Png png = texture("block/wild_plant");
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(0, png.alpha(x, y), "planta silvestre alta demais em " + x + "," + y);
            }
        }
    }

    /** Cada estágio da lavoura tem uma silhueta própria; a planta cresce (mais pixels) até o estágio 7. */
    @Test
    void cropStagesGrowStageByStage() {
        for (String family : AssetCompletenessTest.families()) {
            int previous = 0;
            for (int stage = 0; stage < 8; stage++) {
                int pixels = count(texture("block/" + family + "_crop_stage" + stage).mask());
                assertTrue(pixels > previous, family + " estágio " + stage + " não cresceu: " + pixels + " <= " + previous);
                previous = pixels;
            }
        }
    }

    /**
     * Texturas de bloco e item só têm alfa 0 ou 255: com alfa parcial o quad cai na camada translúcida
     * (ChunkSectionLayer.byTransparency) e se ordena mal com a tela, o vidro e os maços.
     */
    @Test
    void blockAndItemTexturesHaveNoPartialAlpha() throws IOException {
        for (String folder : List.of("block", "item")) {
            try (Stream<Path> pngs = Files.walk(Resources.assets("textures/" + folder))) {
                for (Path file : pngs.filter(p -> p.toString().endsWith(".png")).toList()) {
                    Png png = Png.read(file);
                    for (int y = 0; y < png.height(); y++) {
                        for (int x = 0; x < png.width(); x++) {
                            int a = png.alpha(x, y);
                            assertTrue(a == 0 || a == 255, file.getFileName() + " alfa " + a + " em " + x + "," + y);
                        }
                    }
                }
            }
        }
    }

    /** A tela da peneira é cutout de verdade: tem furos e arame. */
    @Test
    void sieveMeshIsCutout() {
        boolean[] mask = texture("block/sieve_mesh").mask();
        int solid = count(mask);
        assertTrue(solid > 0 && solid < mask.length, "a tela precisa de arame e de vãos: " + solid);
    }

    /**
     * O vidro do pote é cutout de verdade: na região da lateral (UV [5,8,11,16]) as colunas da borda são opacas e o
     * miolo tem furos para o conteúdo aparecer; fora das regiões usadas, tudo transparente.
     */
    @Test
    void jarGlassIsCutoutWithOpaqueEdges() {
        Png glass = texture("block/jar_glass");
        int holes = 0;
        for (int y = 8; y < 16; y++) {
            assertEquals(255, glass.alpha(5, y), "borda esquerda do vidro em y=" + y);
            assertEquals(255, glass.alpha(10, y), "borda direita do vidro em y=" + y);
            for (int x = 6; x < 10; x++) {
                if (glass.alpha(x, y) == 0) {
                    holes++;
                }
            }
        }
        assertTrue(holes >= 12, "o vidro precisa deixar ver o conteúdo: " + holes + " furos");
        for (int x = 0; x < 16; x++) {
            assertEquals(0, glass.alpha(x, 0), "linha 0 fora das regiões usadas");
            if (x < 5 || x > 10) {
                for (int y = 0; y < 16; y++) {
                    assertEquals(0, glass.alpha(x, y), "coluna " + x + " fora das regiões usadas");
                }
            }
        }
    }

    /**
     * O baseado com piteira tem o mesmo corpo do comum (linhas 0–11) e só muda a boca (linhas 12–14): a piteira cor de
     * papelão com o zigue-zague marrom sai 1 px além do papel. Vale apagado e aceso.
     */
    @Test
    void filteredJointOnlyChangesTheMouthEnd() {
        for (String suffix : List.of("", "_lit")) {
            Png plain = texture("item/joint" + suffix);
            Png filtered = texture("item/joint_filtered" + suffix);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(plain.argb()[y * 16 + x], filtered.argb()[y * 16 + x], suffix + " corpo diferente em " + x + "," + y);
                }
            }
            assertFalse(Arrays.equals(plain.mask(), filtered.mask()), suffix + ": a piteira deveria aparecer na silhueta");
            assertEquals(0, plain.alpha(1, 14), suffix + ": o baseado comum termina na coluna 2");
            assertEquals(255, filtered.alpha(1, 14), suffix + ": a piteira sai 1 px além");
            // Marrom do zigue-zague só na piteira
            assertFalse(hasColor(plain, 0x9a6a34), suffix + ": baseado comum com cor de piteira");
            assertTrue(hasColor(filtered, 0x9a6a34), suffix + ": piteira sem o zigue-zague marrom");
        }
    }

    /** A cinza do cinzeiro é cinza (a cor mais frequente não tem tom): não pode parecer terra ou areia. */
    @Test
    void ashLooksGrey() {
        int ash = texture("block/ashtray_ash").dominantColor();
        int r = (ash >> 16) & 0xFF;
        int g = (ash >> 8) & 0xFF;
        int b = ash & 0xFF;
        assertTrue(Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 8, String.format("cinza com tom: %06x", ash));
    }

    private static boolean hasColor(Png png, int rgb) {
        for (int y = 0; y < png.height(); y++) {
            for (int x = 0; x < png.width(); x++) {
                if (png.alpha(x, y) != 0 && png.rgb(x, y) == rgb) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int count(boolean[] mask) {
        int n = 0;
        for (boolean b : mask) {
            if (b) {
                n++;
            }
        }
        return n;
    }
}
