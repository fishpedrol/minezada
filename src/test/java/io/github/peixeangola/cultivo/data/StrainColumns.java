package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.Variety;
import java.nio.file.Path;
import java.util.Map;

/**
 * Vocabulário das colunas de variedade usado no manifesto (tools/gen/manifest.json, seção "strains") e no catálogo
 * do dono (docs/strains-catalog.json), em português, e a conferência de uma linha contra o enum {@link Variety}.
 */
final class StrainColumns {
    static final Path MANIFEST = Path.of("tools", "gen", "manifest.json");
    static final Path OWNER_CATALOG = Path.of("docs", "strains-catalog.json");

    static final Map<String, Variety.Origin> ORIGINS = Map.of(
        "nativa", Variety.Origin.NATIVE, "catalogo", Variety.Origin.CATALOG);
    static final Map<String, Variety.Rarity> RARITIES = Map.of(
        "comum", Variety.Rarity.COMMON, "incomum", Variety.Rarity.UNCOMMON, "rara", Variety.Rarity.RARE);
    static final Map<String, Variety.Cycle> CYCLES = Map.of(
        "automatica", Variety.Cycle.AUTOFLOWERING, "fotoperiodo", Variety.Cycle.PHOTOPERIOD);
    static final Map<String, Variety.Lineage> GENETICS = Map.of(
        "sativa", Variety.Lineage.SATIVA, "hibrida", Variety.Lineage.HYBRID, "indica", Variety.Lineage.INDICA);
    static final Map<String, Variety.Growth> GROWTHS = Map.of(
        "rapida", Variety.Growth.FAST, "media", Variety.Growth.MEDIUM, "lenta", Variety.Growth.SLOW);

    private StrainColumns() {}

    private static <T> T lookup(Map<String, T> vocabulary, JsonObject row, String column, String where) {
        String value = row.get(column).getAsString();
        T mapped = vocabulary.get(value);
        assertTrue(mapped != null, where + ": valor desconhecido em " + column + ": " + value);
        return mapped;
    }

    /** Confere raridade, ciclo, genética, crescimento, potência e família (e a origem, se a linha tiver). */
    static void assertMatches(Variety expected, JsonObject row, String where) {
        assertEquals(expected.id(), row.get("id").getAsString(), where);
        if (row.has("origin")) {
            assertEquals(expected.origin(), lookup(ORIGINS, row, "origin", where), where + " origem");
        }
        assertEquals(expected.rarity(), lookup(RARITIES, row, "rarity", where), where + " raridade");
        assertEquals(expected.cycle(), lookup(CYCLES, row, "cycle", where), where + " ciclo");
        assertEquals(expected.lineage(), lookup(GENETICS, row, "genetics", where), where + " genética");
        assertEquals(expected.growth(), lookup(GROWTHS, row, "growth", where), where + " crescimento");
        assertEquals(expected.potency(), row.get("potency").getAsInt(), where + " potência");
        assertEquals(expected.artFamily(), row.get("family").getAsString(), where + " família");
    }
}
