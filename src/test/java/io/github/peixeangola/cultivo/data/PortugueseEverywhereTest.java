package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pedido do dono: o mod todo em português, seja qual for o idioma do cliente. Com portuguese_everywhere o en_us.json
 * (o fallback de todos os idiomas) sai igual ao pt_br.json; o inglês continua no manifesto como fonte completa, para dar
 * para voltar desligando a chave. Nenhuma palavra inglesa sobra nos textos do jogo.
 */
class PortugueseEverywhereTest {
    /**
     * Palavras que aparecem tanto nas fontes em inglês quanto nas em português e são legítimas nas duas: nomes próprios
     * (variedades, Cultivo), empréstimos (bong, chillum, sativa), "a" e "do" (português, e partes de paródias inglesas
     * como Do-Re-Mi), "s" de %s e "k" de 18k.
     */
    private static final Set<String> SHARED_WORDS = Set.of("a", "ak", "aurora", "banana", "banner", "bong", "brisa", "bruno",
        "bubba", "chillum", "cultivo", "do", "eclipse", "jack", "k", "s", "sativa");
    /** Palavras inglesas comuns que nunca entram num texto em português, mesmo fora das fontes do manifesto. */
    private static final Set<String> ENGLISH_STOPWORDS = Set.of("the", "and", "with", "of", "to", "your", "you", "for",
        "in", "on", "is", "are", "it", "this", "that", "seeds", "flower", "dried", "fresh", "crop", "joint");

    private static JsonObject manifest() {
        return Resources.json(StrainColumns.MANIFEST);
    }

    private static Map<String, String> lang(String name) {
        Map<String, String> out = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : Resources.json(Resources.assets("lang/" + name + ".json")).entrySet()) {
            out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        for (String w : text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /** Todas as fontes em inglês do manifesto: textos fixos, nomes, padrões de nome e legendas. */
    private static List<String> englishSources() {
        JsonObject m = manifest();
        List<String> out = new ArrayList<>();
        m.getAsJsonObject("lang").getAsJsonObject("en_us").entrySet().forEach(e -> out.add(e.getValue().getAsString()));
        for (String section : List.of("strains", "generic_items", "articles", "stations", "effects", "sounds")) {
            m.getAsJsonArray(section).forEach(row -> out.add(row.getAsJsonObject().get("en").getAsString()));
        }
        out.add(m.getAsJsonObject("wild_plant").get("en").getAsString());
        for (Map.Entry<String, JsonElement> p : m.getAsJsonObject("name_patterns").entrySet()) {
            if (p.getValue().isJsonObject()) {
                out.add(p.getValue().getAsJsonObject().get("en").getAsString());
            }
        }
        return out;
    }

    @Test
    void englishFallbackShowsPortuguese() {
        assertTrue(manifest().get("portuguese_everywhere").getAsBoolean(), "o dono pediu o mod todo em português");
        Map<String, String> pt = lang("pt_br");
        Map<String, String> en = lang("en_us");
        assertEquals(pt.keySet(), en.keySet(), "chaves diferentes entre en_us e pt_br");
        assertEquals(pt, en, "com portuguese_everywhere o en_us.json repete o pt_br.json");
        assertTrue(pt.size() > 250, "poucas traduções: " + pt.size());
    }

    /** Toda chave tem fonte nas duas línguas: desligar portuguese_everywhere devolve um en_us completo em inglês. */
    @Test
    void englishSourcesStayCompleteForEveryKey() {
        JsonObject m = manifest();
        JsonObject lang = m.getAsJsonObject("lang");
        assertEquals(lang.getAsJsonObject("pt_br").keySet(), lang.getAsJsonObject("en_us").keySet());
        for (String section : List.of("strains", "generic_items", "articles", "stations", "effects", "sounds")) {
            for (JsonElement row : m.getAsJsonArray(section)) {
                for (String column : List.of("pt", "en")) {
                    JsonElement value = row.getAsJsonObject().get(column);
                    assertTrue(value != null && !value.getAsString().isBlank(), section + " sem " + column + ": " + row);
                }
            }
        }
        for (Map.Entry<String, JsonElement> p : m.getAsJsonObject("name_patterns").entrySet()) {
            if (!p.getValue().isJsonObject()) {
                continue;
            }
            for (String column : List.of("pt", "en")) {
                assertTrue(p.getValue().getAsJsonObject().get(column).getAsString().contains("%s"), p.getKey() + " " + column);
            }
        }
    }

    /**
     * Nenhum texto do jogo (pt_br e en_us gerados) tem palavra que só existe nas fontes em inglês do manifesto, nem
     * palavras inglesas comuns.
     */
    @Test
    void noEnglishWordsLeftInGameTexts() {
        Set<String> english = new LinkedHashSet<>();
        englishSources().forEach(source -> english.addAll(words(source)));
        english.removeAll(SHARED_WORDS);
        english.addAll(ENGLISH_STOPWORDS);
        assertTrue(english.size() > 100, "vocabulário inglês curto demais: " + english.size());
        List<String> found = new ArrayList<>();
        for (String name : List.of("pt_br", "en_us")) {
            for (Map.Entry<String, String> e : lang(name).entrySet()) {
                for (String w : words(e.getValue())) {
                    if (english.contains(w)) {
                        found.add(name + " " + e.getKey() + ": " + w);
                    }
                }
            }
        }
        assertTrue(found.isEmpty(), "inglês nos textos do jogo: " + found);
    }

    /** O detector acharia uma palavra inglesa escondida num texto em português. */
    @Test
    void detectorCatchesEnglish() {
        Set<String> english = new LinkedHashSet<>();
        englishSources().forEach(source -> english.addAll(words(source)));
        english.removeAll(SHARED_WORDS);
        assertTrue(words("Clique com a empty hand").stream().anyMatch(english::contains));
        assertFalse(words("Clique com a mão vazia").stream().anyMatch(english::contains));
    }

    /** Nomes e frases revisados (acentos, termos do pt_br vanilla e do dono). */
    @Test
    void reviewedPortugueseNames() {
        Map<String, String> pt = lang("pt_br");
        assertEquals("Baseado", pt.get("item.cultivo.joint"));
        assertEquals("Suporte de Secagem", pt.get("block.cultivo.drying_rack"));
        assertEquals("Peneira de Bancada", pt.get("block.cultivo.sieve"));
        assertEquals("Prensa Manual", pt.get("block.cultivo.press"));
        assertEquals("Flor Seca de Brisa", pt.get("item.cultivo.brisa_dried_flower"));
        assertEquals("Flor Fresca de Aurora", pt.get("item.cultivo.aurora_fresh_flower"));
        assertEquals("Sementes de Eclipse", pt.get("item.cultivo.eclipse_seeds"));
        // Como o vanilla pt_br ("Plantio de Trigo")
        assertEquals("Plantio de Brisa", pt.get("block.cultivo.brisa_crop"));
        assertEquals("Bandeja de Enrolar", pt.get("item.cultivo.rolling_tray"));
        assertEquals("Bandeja de Enrolar", pt.get("block.cultivo.rolling_tray"));
        assertEquals("Pote de Vidro", pt.get("item.cultivo.jar"));
        assertEquals("Narguilé", pt.get("item.cultivo.hookah"));
        assertEquals("Chapado", pt.get("effect.cultivo.high"));
        assertEquals("Cultivo: Genéticas", pt.get("itemGroup.cultivo.strains"));
        assertEquals("Bong borbulha", pt.get("subtitles.cultivo.bong.bubble"));
        assertEquals("Dichavador mói", pt.get("subtitles.cultivo.grinder.grind"));
        assertEquals("Você precisa de 3 folhas de seda no inventário", pt.get("message.cultivo.tray_need_papers"));
    }

    /** Nome, autores e descrição do mod (lista de mods) e do pacote de recursos também em português. */
    @Test
    void modMetadataIsPortuguese() throws IOException {
        String toml = Files.readString(Resources.MAIN.resolve("META-INF/mods.toml"));
        assertTrue(toml.contains("displayName=\"Cultivo\""));
        assertTrue(toml.contains("authors=\"Colaboradores do Cultivo\""));
        assertTrue(toml.contains("tudo é mecânica de jogo"), "descrição em português");
        JsonObject pack = Resources.json(Resources.MAIN.resolve("pack.mcmeta")).getAsJsonObject("pack");
        assertEquals("Recursos do Cultivo", pack.get("description").getAsString());
        Set<String> english = new LinkedHashSet<>(ENGLISH_STOPWORDS);
        for (String line : toml.split("\\R")) {
            if (line.startsWith("description=") || line.startsWith("authors=") || line.startsWith("Mod privado")) {
                for (String w : words(line)) {
                    assertFalse(english.contains(w), "inglês nos metadados: " + line);
                }
            }
        }
    }

    /**
     * Texto que o jogador vê sai sempre de chave de tradução: nada de Component.literal no código do jogo. Só os
     * GameTests (diagnóstico de teste, nunca visto por jogador) usam literal.
     */
    @Test
    void gameCodeUsesTranslationKeysOnly() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String path = file.toString().replace('\\', '/');
                if (path.contains("/gametest/")) {
                    continue;
                }
                String text = Files.readString(file);
                if (text.contains("Component.literal(") || text.contains("Component.nullToEmpty(")) {
                    found.add(path);
                }
            }
        }
        assertTrue(found.isEmpty(), "texto fixo fora do lang: " + found);
    }
}
