package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Todo ID registrado tem blockstate, modelo, definição de item, textura 16x16 e tradução nas duas línguas. */
class AssetCompletenessTest {
    private static final List<String> LANGS = List.of("en_us", "pt_br");

    @Test
    void everyBlockHasBlockstateModelsAndTextures() {
        for (String block : ContentIds.allBlocks()) {
            JsonObject state = Resources.json(Resources.assets("blockstates/" + block + ".json"));
            JsonObject variants = state.getAsJsonObject("variants");
            assertTrue(variants != null && !variants.isEmpty(), block);
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                assertModelComplete(model);
            }
        }
    }

    @Test
    void cropsMapEightAgesToFourStages() {
        for (Variety v : Variety.values()) {
            JsonObject variants = Resources.json(Resources.assets("blockstates/" + v.cropId() + ".json"))
                .getAsJsonObject("variants");
            assertEquals(8, variants.size(), v.id());
            Set<String> models = new HashSet<>();
            for (int age = 0; age <= 7; age++) {
                String model = variants.getAsJsonObject("age=" + age).get("model").getAsString();
                assertEquals("cultivo:block/" + v.cropId() + "_stage" + age / 2, model);
                models.add(model);
            }
            assertEquals(4, models.size());
        }
    }

    @Test
    void everyItemHasItemDefinitionModelAndTexture() {
        for (String item : ContentIds.allItems()) {
            JsonObject def = Resources.json(Resources.assets("items/" + item + ".json")).getAsJsonObject("model");
            assertEquals("minecraft:model", def.get("type").getAsString(), item);
            assertModelComplete(def.get("model").getAsString());
        }
    }

    @Test
    void flatItemsHaveOwnTextures() {
        for (String item : ContentIds.flatItems()) {
            assertPng16(Resources.assets("textures/item/" + item + ".png"));
        }
    }

    @Test
    void everyIdIsTranslatedInBothLanguages() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (String item : ContentIds.flatItems()) {
                assertTranslated(t, "item.cultivo." + item, lang);
            }
            for (String block : ContentIds.allBlocks()) {
                assertTranslated(t, "block.cultivo." + block, lang);
            }
            assertTranslated(t, "itemGroup.cultivo", lang);
        }
    }

    @Test
    void languagesHaveSameKeys() {
        Set<String> en = Resources.json(Resources.assets("lang/en_us.json")).keySet();
        Set<String> pt = Resources.json(Resources.assets("lang/pt_br.json")).keySet();
        assertEquals(en, pt);
    }

    @Test
    void translationKeysUsedInJavaExist() throws IOException {
        Pattern literal = Pattern.compile("\"((?:tooltip|itemGroup|message|effect)\\.cultivo[a-z0-9_.]*)\"");
        Set<String> en = Resources.json(Resources.assets("lang/en_us.json")).keySet();
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = literal.matcher(Files.readString(file));
                while (m.find()) {
                    String key = m.group(1);
                    if (key.endsWith(".")) {
                        // prefixo concatenado em Java: precisa haver ao menos uma chave com ele
                        if (en.stream().noneMatch(k -> k.startsWith(key))) {
                            missing.add(key + "*");
                        }
                    } else if (!en.contains(key)) {
                        missing.add(key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "chaves sem tradução: " + missing);
    }

    @Test
    void qualityAndProfileTooltipKeysExist() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (int q = 1; q <= 5; q++) {
                assertTranslated(t, "tooltip.cultivo.quality." + q, lang);
            }
            for (Variety v : Variety.values()) {
                assertTranslated(t, "tooltip.cultivo.rarity." + v.rarity().name().toLowerCase(), lang);
                assertTranslated(t, "tooltip.cultivo.cycle." + v.cycle().name().toLowerCase(), lang);
                assertTranslated(t, "tooltip.cultivo.lineage." + v.lineage().name().toLowerCase(), lang);
            }
        }
    }

    @Test
    void everyEffectHasIconAndTranslation() {
        for (String effect : ContentIds.EFFECTS) {
            assertPng(Resources.assets("textures/mob_effect/" + effect + ".png"), 18);
            for (String lang : LANGS) {
                assertTranslated(Resources.json(Resources.assets("lang/" + lang + ".json")), "effect.cultivo." + effect, lang);
            }
        }
    }

    @Test
    void smokingMessagesAndVarietyNamesTranslated() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (io.github.peixeangola.cultivo.core.SmokeRules.Refusal refusal
                : io.github.peixeangola.cultivo.core.SmokeRules.Refusal.values()) {
                if (refusal.messageKey() != null) {
                    assertTranslated(t, refusal.messageKey(), lang);
                }
            }
            for (String key : List.of("message.cultivo.lighter_broke",
                "tooltip.cultivo.smoke.joint", "tooltip.cultivo.smoke.device", "tooltip.cultivo.lighter",
                "tooltip.cultivo.strain")) {
                assertTranslated(t, key, lang);
            }
            for (Variety v : Variety.values()) {
                assertTranslated(t, "tooltip.cultivo.variety." + v.id(), lang);
            }
        }
    }

    @Test
    void noOrphanTextures() throws IOException {
        Set<String> referenced = new HashSet<>();
        try (Stream<Path> models = Files.walk(Resources.assets("models"))) {
            for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject textures = Resources.json(model).getAsJsonObject("textures");
                if (textures != null) {
                    textures.entrySet().forEach(e -> referenced.add(e.getValue().getAsString()));
                }
            }
        }
        try (Stream<Path> pngs = Files.walk(Resources.assets("textures"))) {
            for (Path png : pngs.filter(p -> p.toString().endsWith(".png")).toList()) {
                String rel = Resources.assets("textures").relativize(png).toString().replace('\\', '/');
                String id = "cultivo:" + rel.substring(0, rel.length() - ".png".length());
                if (rel.startsWith("mob_effect/")) {
                    // Ícones de efeito são achados pelo ID do efeito, não por modelo
                    assertTrue(ContentIds.EFFECTS.contains(rel.substring("mob_effect/".length(), rel.length() - 4)),
                        "ícone de efeito sem efeito registrado: " + id);
                    continue;
                }
                assertTrue(referenced.contains(id), "textura sem uso: " + id);
            }
        }
    }

    private static void assertTranslated(JsonObject lang, String key, String langName) {
        JsonElement value = lang.get(key);
        assertTrue(value != null && !value.getAsString().isBlank(), langName + " sem " + key);
    }

    private static void assertModelComplete(String modelId) {
        if (!modelId.startsWith("cultivo:")) {
            fail("modelo fora do mod: " + modelId);
        }
        JsonObject model = Resources.json(Resources.cultivoResource(modelId, "models", ".json"));
        assertTrue(model.has("parent"), modelId);
        String parent = model.get("parent").getAsString();
        if (parent.startsWith("cultivo:")) {
            assertModelComplete(parent);
        }
        JsonObject textures = model.getAsJsonObject("textures");
        if (textures != null) {
            for (Map.Entry<String, JsonElement> tex : textures.entrySet()) {
                assertPng16(Resources.cultivoResource(tex.getValue().getAsString(), "textures", ".png"));
            }
        }
    }

    static void assertPng16(Path png) {
        assertPng(png, 16);
    }

    static void assertPng(Path png, int size) {
        try (InputStream in = Files.newInputStream(png)) {
            byte[] header = in.readNBytes(24);
            assertEquals(0x89, header[0] & 0xff, png + " não é PNG");
            assertEquals("IHDR", new String(header, 12, 4));
            ByteBuffer buf = ByteBuffer.wrap(header);
            assertEquals(size, buf.getInt(16), png + " largura");
            assertEquals(size, buf.getInt(20), png + " altura");
        } catch (IOException e) {
            fail("textura faltando: " + png);
        }
    }
}
