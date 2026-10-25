package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Compatibilidade com o ambiente do dono: o Forge mínimo declarado e o Serene Seasons (mod opcional de terceiros,
 * DECISIONS.md, "Compatibilidade: Serene Seasons"). Valores esperados escritos como literais da especificação.
 */
class CompatibilityTest {
    private static final Path SS = Resources.GENERATED.resolve("data").resolve("sereneseasons");

    /** IDs de um arquivo de tag cujas entradas são objetos {"id", "required": false}; confere replace=false. */
    private static List<String> optionalIds(Path file) {
        JsonObject tag = Resources.json(file);
        assertFalse(tag.get("replace").getAsBoolean(), file + ": replace deveria ser false");
        List<String> ids = new ArrayList<>();
        JsonArray values = tag.getAsJsonArray("values");
        for (JsonElement value : values) {
            assertTrue(value.isJsonObject(), file + ": entrada sem required:false derrubaria a tag inteira");
            JsonObject entry = value.getAsJsonObject();
            assertFalse(entry.get("required").getAsBoolean(), file + ": " + entry);
            ids.add(entry.get("id").getAsString());
        }
        return ids;
    }

    @Test
    void cropSeasonsMirrorWheat() {
        // Serene Seasons 26.1.2.0.7: minecraft:wheat só em summer_crops e autumn_crops (bloco e semente)
        assertEquals(List.of("summer", "autumn"), ContentIds.SERENE_SEASONS_CROP_SEASONS);
        JsonArray manifest = Resources.json(StrainColumns.MANIFEST).getAsJsonObject("compat")
            .getAsJsonObject("serene_seasons").getAsJsonArray("seasons");
        List<String> fromManifest = new ArrayList<>();
        manifest.forEach(e -> fromManifest.add(e.getAsString()));
        assertEquals(ContentIds.SERENE_SEASONS_CROP_SEASONS, fromManifest);
    }

    @Test
    void sereneSeasonsTagsListEveryCropAndSeed() {
        Set<String> crops = new LinkedHashSet<>();
        Set<String> seeds = new LinkedHashSet<>();
        for (Variety v : Variety.values()) {
            crops.add("cultivo:" + v.cropId());
            seeds.add("cultivo:" + v.seedsId());
        }
        assertEquals(40, crops.size());
        for (String season : List.of("summer", "autumn")) {
            List<String> blockIds = optionalIds(SS.resolve("tags/block/" + season + "_crops.json"));
            List<String> itemIds = optionalIds(SS.resolve("tags/item/" + season + "_crops.json"));
            assertEquals(crops, new LinkedHashSet<>(blockIds), season + " (bloco)");
            assertEquals(40, blockIds.size(), season + ": sem repetidos");
            assertEquals(seeds, new LinkedHashSet<>(itemIds), season + " (item)");
            assertEquals(40, itemIds.size(), season + ": sem repetidos");
            assertFalse(blockIds.contains("cultivo:wild_plant"), "a planta silvestre não tem random tick");
        }
    }

    @Test
    void onlyTheFourSereneSeasonsFilesExist() throws IOException {
        List<String> files;
        try (Stream<Path> walk = Files.walk(SS)) {
            files = walk.filter(Files::isRegularFile)
                .map(p -> SS.relativize(p).toString().replace('\\', '/'))
                .sorted()
                .toList();
        }
        // Nada de spring/winter/year_round, de unbreakable_infertile_crops ou de greenhouse_glass (mudaria o vanilla)
        assertEquals(List.of(
            "tags/block/autumn_crops.json",
            "tags/block/summer_crops.json",
            "tags/item/autumn_crops.json",
            "tags/item/summer_crops.json"), files);
    }

    @Test
    void sereneSeasonsGameTestHasSkyAccess() {
        assertTrue(ContentIds.GAME_TESTS.contains("serene_seasons_compat"));
        JsonObject instance = Resources.json(Resources.data("cultivo", "test_instance/serene_seasons_compat.json"));
        // Sem teto de barreira: abaixo de y=48 sem ver o céu, o Serene Seasons considera tudo fértil
        assertTrue(instance.get("sky_access").getAsBoolean());
    }

    @Test
    void forgeRangeStartsAtTheTestedVersion() throws IOException {
        // sem \r: num checkout do Windows com CRLF a comparação de várias linhas continua valendo
        String toml = Files.readString(Resources.MAIN.resolve("META-INF/mods.toml")).replace("\r", "");
        // Forge 26.3-66.0.2 é a menor versão testada (o launcher do dono); 66.0.13 é a da matriz
        assertTrue(toml.contains("modId=\"forge\"\n    mandatory=true\n    versionRange=\"[66.0.2,)\""), "faixa do Forge");
        assertTrue(toml.contains("versionRange=\"[26.3,26.4)\""), "faixa do Minecraft");
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("getOrElse('26.3-66.0.13')"), "o padrão do build continua o Forge da matriz");
    }
}
