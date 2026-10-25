package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * O catálogo do dono (docs/strains-catalog.json) é a fonte das variedades: a tabela Java e o manifesto seguem ele, e
 * os nomes reais (campos "original" e "breeder") ficam só nesse documento. No jogo só entram as paródias.
 */
class ParodyNamesTest {
    /** Pastas com arquivos que vão para o jogo ou geram o jogo (texto: Java, JSON, scripts, mcfunction). */
    private static final List<Path> GAME_SOURCES = List.of(
        Path.of("src", "main", "java"),
        Path.of("src", "main", "resources"),
        Path.of("src", "generated", "resources"),
        Path.of("tools", "gen"),
        Path.of("tools", "art"),
        Path.of("tools", "visual-probe"));
    private static final Set<String> TEXT_EXTENSIONS =
        Set.of(".java", ".json", ".mjs", ".js", ".toml", ".mcmeta", ".mcfunction", ".txt", ".cfg", ".ps1");

    private static JsonObject ownerCatalog() {
        return Resources.json(StrainColumns.OWNER_CATALOG);
    }

    private static List<JsonObject> rows(String section) {
        List<JsonObject> out = new ArrayList<>();
        ownerCatalog().getAsJsonArray(section).forEach(e -> out.add(e.getAsJsonObject()));
        return out;
    }

    @Test
    void javaTableFollowsTheOwnerCatalog() {
        List<JsonObject> originals = rows("originals");
        List<JsonObject> catalog = rows("catalog");
        assertEquals(3, originals.size());
        assertEquals(37, catalog.size());
        List<Variety> natives = Variety.natives();
        List<Variety> fromCatalog = Variety.catalog();
        assertEquals(originals.size(), natives.size());
        assertEquals(catalog.size(), fromCatalog.size());
        for (int i = 0; i < originals.size(); i++) {
            StrainColumns.assertMatches(natives.get(i), originals.get(i), "originals[" + i + "]");
        }
        for (int i = 0; i < catalog.size(); i++) {
            StrainColumns.assertMatches(fromCatalog.get(i), catalog.get(i), "catalog[" + i + "]");
        }
    }

    /** Os nomes pt/en do manifesto (que viram o lang) são as paródias do catálogo. */
    @Test
    void manifestUsesTheCatalogParodyNames() {
        Map<String, JsonObject> manifest = new HashMap<>();
        Resources.json(StrainColumns.MANIFEST).getAsJsonArray("strains")
            .forEach(e -> manifest.put(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject()));
        for (JsonObject row : rows("catalog")) {
            String id = row.get("id").getAsString();
            JsonObject strain = manifest.get(id);
            assertTrue(strain != null, "variedade fora do manifesto: " + id);
            assertEquals(row.get("pt").getAsString(), strain.get("pt").getAsString(), id + " pt");
            assertEquals(row.get("en").getAsString(), strain.get("en").getAsString(), id + " en");
        }
    }

    @Test
    void langShowsTheParodyNames() {
        JsonObject pt = Resources.json(Resources.assets("lang/pt_br.json"));
        JsonObject en = Resources.json(Resources.assets("lang/en_us.json"));
        assertEquals("Sementes de Manga Gelada", pt.get("item.cultivo.manga_gelada_seeds").getAsString());
        assertEquals("Flor Seca de Bananona", pt.get("item.cultivo.bananona_dried_flower").getAsString());
        assertEquals("Cola de Gorila #4", pt.get("tooltip.cultivo.variety.cola_de_gorila").getAsString());
        assertEquals("Plantio de Dó-Si-Dó", pt.get("block.cultivo.do_si_do_crop").getAsString());
        // O dono pediu o mod todo em português: o en_us.json (fallback de todos os idiomas) mostra as mesmas paródias
        // em português; as paródias em inglês continuam no manifesto (manifestUsesTheCatalogParodyNames)
        assertTrue(Resources.json(StrainColumns.MANIFEST).get("portuguese_everywhere").getAsBoolean());
        assertEquals("Sementes de Manga Gelada", en.get("item.cultivo.manga_gelada_seeds").getAsString());
        assertEquals("Cola de Gorila #4", en.get("tooltip.cultivo.variety.cola_de_gorila").getAsString());
        for (Variety v : Variety.values()) {
            for (JsonObject lang : List.of(pt, en)) {
                JsonElement name = lang.get("tooltip.cultivo.variety." + v.id());
                assertTrue(name != null && !name.getAsString().isBlank(), "sem nome para " + v.id());
            }
        }
    }

    /** Nomes reais de variedade e de banco de sementes (sem o sufixo "(auto)"), e "THC": nunca no jogo. */
    private static Set<String> forbiddenTerms() {
        Set<String> terms = new LinkedHashSet<>();
        for (JsonObject row : rows("catalog")) {
            terms.add(row.get("original").getAsString().replaceAll("\\s*\\(.*\\)\\s*$", "").trim());
            terms.add(row.get("breeder").getAsString().trim());
        }
        terms.add("THC");
        return terms;
    }

    private static boolean isWordChar(String text, int index) {
        return index >= 0 && index < text.length() && Character.isLetterOrDigit(text.charAt(index));
    }

    /** Ocorrências do termo, sem diferenciar maiúsculas, que não estejam coladas a letras ou dígitos. */
    static boolean containsTerm(String text, String term) {
        String haystack = text.toLowerCase(Locale.ROOT);
        String needle = term.toLowerCase(Locale.ROOT);
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            boolean startOk = !Character.isLetterOrDigit(needle.charAt(0)) || !isWordChar(haystack, at - 1);
            boolean endOk = !Character.isLetterOrDigit(needle.charAt(needle.length() - 1))
                || !isWordChar(haystack, at + needle.length());
            if (startOk && endOk) {
                return true;
            }
        }
        return false;
    }

    @Test
    void termMatchingIgnoresCaseAndRespectsWordEdges() {
        assertTrue(containsTerm("\"name\": \"Purple Kush\"", "purple kush"));
        assertTrue(containsTerm("x LSD-25 y", "LSD-25"));
        assertTrue(!containsTerm("Ultraviolet", "Ultra Violet"));
        assertTrue(!containsTerm("Mangoey Kushion", "Mango Kush"));
        assertTrue(!containsTerm("lsdx", "LSD"));
    }

    @Test
    void noRealStrainOrBreederNameInGameFiles() throws IOException {
        Set<String> terms = forbiddenTerms();
        assertTrue(terms.size() >= 37, "lista de nomes proibidos curta demais: " + terms.size());
        List<String> found = new ArrayList<>();
        int scanned = 0;
        for (Path root : GAME_SOURCES) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    int dot = name.lastIndexOf('.');
                    if (dot < 0 || !TEXT_EXTENSIONS.contains(name.substring(dot))) {
                        continue;
                    }
                    String text = Files.readString(file);
                    scanned++;
                    for (String term : terms) {
                        if (containsTerm(text, term)) {
                            found.add(file + ": " + term);
                        }
                    }
                }
            }
        }
        assertTrue(scanned > 700, "poucos arquivos lidos: " + scanned);
        assertTrue(found.isEmpty(), "nome real no jogo: " + found);
    }

    /** Cada linha do catálogo tem a paródia (pt/en) e, só para o dono, o nome real e o banco de sementes. */
    @Test
    void ownerCatalogRowsAreComplete() {
        for (JsonObject row : rows("catalog")) {
            String id = row.get("id").getAsString();
            for (String column : List.of("pt", "en", "original", "breeder")) {
                assertTrue(row.has(column) && !row.get(column).getAsString().isBlank(), id + " sem " + column);
            }
        }
    }
}
