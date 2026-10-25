package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ContentIds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Sons do mod: cada evento usado no Java (ModSounds) está no sounds.json gerado, reaproveita um evento vanilla e tem
 * legenda traduzida em português; o código do jogo não toca som vanilla direto (a legenda sairia sem o nosso texto).
 */
class SoundsTest {
    private static final Path MOD_SOUNDS = Resources.JAVA.resolve("io/github/peixeangola/cultivo/registry/ModSounds.java");

    private static JsonObject soundsJson() {
        return Resources.json(Resources.assets("sounds.json"));
    }

    /** IDs registrados em ModSounds (register("...")), na ordem do arquivo. */
    private static List<String> registeredIds() throws IOException {
        Matcher m = Pattern.compile("register\\(\"([a-z_.]+)\"\\)").matcher(Files.readString(MOD_SOUNDS));
        List<String> ids = new ArrayList<>();
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    @Test
    void registryManifestAndSoundsJsonListTheSameEvents() throws IOException {
        assertEquals(16, ContentIds.SOUNDS.size());
        assertEquals(ContentIds.SOUNDS, registeredIds(), "ModSounds registra exatamente ContentIds.SOUNDS, na ordem");
        List<String> manifest = new ArrayList<>();
        Resources.json(StrainColumns.MANIFEST).getAsJsonArray("sounds")
            .forEach(s -> manifest.add(s.getAsJsonObject().get("id").getAsString()));
        assertEquals(ContentIds.SOUNDS, manifest);
        assertEquals(Set.copyOf(ContentIds.SOUNDS), soundsJson().keySet());
    }

    /**
     * Cada evento aponta para um evento vanilla ("type": "event"), com volume e tom razoáveis, e tem a legenda
     * subtitles.cultivo.&lt;id&gt; traduzida nas duas línguas.
     */
    @Test
    void everyEventReusesVanillaAudioAndHasTranslatedSubtitle() {
        JsonObject pt = Resources.json(Resources.assets("lang/pt_br.json"));
        JsonObject en = Resources.json(Resources.assets("lang/en_us.json"));
        for (String id : ContentIds.SOUNDS) {
            JsonObject entry = soundsJson().getAsJsonObject(id);
            String subtitle = entry.get("subtitle").getAsString();
            assertEquals("subtitles.cultivo." + id, subtitle);
            for (JsonObject lang : List.of(pt, en)) {
                assertTrue(lang.has(subtitle) && !lang.get(subtitle).getAsString().isBlank(), "legenda sem tradução: " + subtitle);
            }
            JsonArray sounds = entry.getAsJsonArray("sounds");
            assertFalse(sounds.isEmpty(), id);
            for (JsonElement e : sounds) {
                JsonObject sound = e.getAsJsonObject();
                assertEquals("event", sound.get("type").getAsString(), id + ": sem áudio próprio, só eventos vanilla");
                assertTrue(sound.get("name").getAsString().startsWith("minecraft:"), id + " " + sound);
                float volume = sound.get("volume").getAsFloat();
                float pitch = sound.get("pitch").getAsFloat();
                assertTrue(volume > 0.0F && volume <= 1.0F, id + " volume " + volume);
                assertTrue(pitch >= 0.5F && pitch <= 2.0F, id + " tom " + pitch);
            }
        }
        assertEquals("Isqueiro acende", pt.get("subtitles.cultivo.lighter.click").getAsString());
        assertEquals("Fumaça soprada", pt.get("subtitles.cultivo.smoke.exhale").getAsString());
        // O narguilé tem o próprio borbulhar: a legenda nomeia o aparelho que está sendo usado, não o bong
        assertEquals("Bong borbulha", pt.get("subtitles.cultivo.bong.bubble").getAsString());
        assertEquals("Narguilé borbulha", pt.get("subtitles.cultivo.hookah.bubble").getAsString());
        assertEquals("minecraft:block.grindstone.use",
            soundsJson().getAsJsonObject("grinder.grind").getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString());
    }

    /** Todo ModSounds.X citado no código existe em ModSounds (e portanto no sounds.json). */
    @Test
    void soundsUsedInJavaAreRegistered() throws IOException {
        String modSounds = Files.readString(MOD_SOUNDS);
        Set<String> declared = new HashSet<>();
        Matcher d = Pattern.compile("RegistryObject<SoundEvent> ([A-Z_]+) =").matcher(modSounds);
        while (d.find()) {
            declared.add(d.group(1));
        }
        assertEquals(ContentIds.SOUNDS.size(), declared.size());
        Set<String> used = new HashSet<>();
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher u = Pattern.compile("ModSounds\\.([A-Z_]+)\\.get\\(\\)").matcher(Files.readString(file));
                while (u.find()) {
                    used.add(u.group(1));
                }
            }
        }
        assertTrue(declared.containsAll(used), "som citado e não registrado: " + used);
        // Todos os eventos têm uso no jogo
        assertEquals(declared, used, "som registrado e nunca tocado");
    }

    /** O jogo só toca sons do mod (com legenda em português); os GameTests podem citar sons vanilla. */
    @Test
    void gameplayCodePlaysOnlyModSounds() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String path = file.toString().replace('\\', '/');
                if (!path.contains("/gametest/") && Files.readString(file).contains("SoundEvents.")) {
                    found.add(path);
                }
            }
        }
        assertTrue(found.isEmpty(), "som vanilla direto no jogo: " + found);
    }
}
