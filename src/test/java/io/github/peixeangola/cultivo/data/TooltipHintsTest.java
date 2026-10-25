package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.TooltipHints;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Dicas de uso dos tooltips em linhas curtas (core/TooltipHints): o lang tem uma chave por linha, cada linha cabe na tela
 * na escala de GUI automática (1920×1080 → 480 px de largura) e o texto diz o botão e o gesto de cada interação.
 */
class TooltipHintsTest {
    /** Largura máxima de uma linha de tooltip (px de GUI), como as dicas vanilla (o achado mediu linhas de ~600 px). */
    static final int MAX_LINE_PIXELS = 220;

    /**
     * Largura aproximada de um texto na fonte padrão do jogo, em px de GUI com o espaço de 1 px entre letras: a maioria
     * das letras tem 6, i/l/t/f/k e a pontuação são mais estreitos, o espaço tem 4, e as estrelas da potência vêm da
     * fonte unicode (~9). Acentos não mudam a largura.
     */
    static int guiWidth(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        int width = 0;
        for (char c : plain.toCharArray()) {
            width += switch (c) {
                case 'i', '!', '.', ',', ':', ';', '\'', '|', '·' -> 2;
                case 'l', '`' -> 3;
                case 'I', 't', '[', ']', ' ' -> 4;
                case 'f', 'k', '(', ')', '{', '}', '<', '>', '*', '"' -> 5;
                case '@', '~' -> 7;
                case '★', '☆' -> 9;
                default -> 6;
            };
        }
        return width;
    }

    private static JsonObject generated(String lang) {
        return Resources.json(Resources.assets("lang/" + lang + ".json"));
    }

    private static JsonObject manifestLang(String lang) {
        return Resources.json(StrainColumns.MANIFEST).getAsJsonObject("lang").getAsJsonObject(lang);
    }

    /** As linhas da dica, na ordem, como o jogo as mostra (lang gerado). */
    private static List<String> lines(JsonObject lang, String key) {
        List<String> out = new ArrayList<>();
        for (String line : TooltipHints.lineKeys(key)) {
            JsonElement value = lang.get(line);
            assertTrue(value != null, "sem " + line);
            out.add(value.getAsString());
        }
        return out;
    }

    /** O medidor reconhece uma linha comprida do jeito antigo e aceita uma curta. */
    @Test
    void widthModelFlagsTheOldSingleLineHints() {
        String old = "Clique com o botão direito segurando o ingrediente para colocar"
            + " · a mão vazia retira (o que está pronto sai primeiro)";
        assertTrue(guiWidth(old) > 2 * MAX_LINE_PIXELS, "a dica antiga media " + guiWidth(old));
        assertEquals(6 * 3 + 4 + 2 * 3, guiWidth("abc ii."));
        assertTrue(guiWidth("Segure o botão direito para fumar") <= MAX_LINE_PIXELS);
    }

    /**
     * Cada dica tem exatamente as linhas que o Java mostra (chave.1 … chave.n), nas duas línguas do lang gerado e nas
     * duas fontes do manifesto (listas do mesmo tamanho), e nenhuma chave base solta sobrou.
     */
    @Test
    void everyHintHasItsLinesInBothLanguages() {
        for (Map.Entry<String, Integer> hint : TooltipHints.LINES.entrySet()) {
            String key = hint.getKey();
            int n = hint.getValue();
            for (String lang : List.of("en_us", "pt_br")) {
                JsonObject t = generated(lang);
                assertFalse(t.has(key), lang + ": chave base solta " + key);
                assertFalse(t.has(key + "." + (n + 1)), lang + ": linha a mais em " + key);
                assertEquals(n, lines(t, key).size());
                JsonElement source = manifestLang(lang).get(key);
                assertTrue(source != null && source.isJsonArray() && source.getAsJsonArray().size() == n,
                    lang + ": o manifesto deveria ter " + n + " linhas em " + key + ": " + source);
            }
        }
    }

    /**
     * Toda linha de tooltip cabe na largura (dicas e linhas fixas, no lang gerado e na fonte em inglês do manifesto), e
     * nenhuma dica junta várias frases com " · " numa linha só. Os formatos com %s (perfil, qualidade, variedade) ficam de
     * fora: a largura depende do valor.
     */
    @Test
    void tooltipLinesFitTheScreen() {
        List<String> tooLong = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : generated("pt_br").entrySet()) {
            String value = e.getValue().getAsString();
            if (e.getKey().startsWith("tooltip.cultivo.") && !value.contains("%") && guiWidth(value) > MAX_LINE_PIXELS) {
                tooLong.add(e.getKey() + " (" + guiWidth(value) + " px): " + value);
            }
        }
        for (Map.Entry<String, JsonElement> e : manifestLang("en_us").entrySet()) {
            List<String> values = new ArrayList<>();
            if (e.getValue().isJsonArray()) {
                e.getValue().getAsJsonArray().forEach(v -> values.add(v.getAsString()));
            } else {
                values.add(e.getValue().getAsString());
            }
            for (String value : values) {
                if (e.getKey().startsWith("tooltip.cultivo.") && !value.contains("%") && guiWidth(value) > MAX_LINE_PIXELS) {
                    tooLong.add("en " + e.getKey() + " (" + guiWidth(value) + " px): " + value);
                }
            }
        }
        assertTrue(tooLong.isEmpty(), "linhas largas demais: " + tooLong);
        for (String key : TooltipHints.LINES.keySet()) {
            for (String line : lines(generated("pt_br"), key)) {
                assertFalse(line.contains(" · "), key + " junta frases numa linha: " + line);
            }
        }
    }

    /**
     * As dicas dizem o botão (clique esquerdo quebra o bloco), o gesto de cada interação e onde a piteira entra; nomes de
     * item no meio da frase em minúsculas.
     */
    @Test
    void hintsNameTheButtonAndTheGesture() {
        JsonObject pt = generated("pt_br");
        String grinder = String.join(" ", lines(pt, "tooltip.cultivo.grinder"));
        String tray = String.join(" ", lines(pt, "tooltip.cultivo.rolling_tray"));
        String jar = String.join(" ", lines(pt, "tooltip.cultivo.jar"));
        String station = String.join(" ", lines(pt, "tooltip.cultivo.station.usage"));
        String filter = String.join(" ", lines(pt, "tooltip.cultivo.filter_tip"));
        String ashtray = String.join(" ", lines(pt, "tooltip.cultivo.ashtray"));
        for (String hint : List.of(grinder, tray, jar, station, ashtray)) {
            assertTrue(hint.contains("botão direito"), "sem o botão: " + hint);
        }
        assertTrue(grinder.contains("peneira de bancada") && !grinder.contains("Peneira de Bancada"), grinder);
        assertTrue(grinder.contains("agachado") && grinder.contains("mãos vazias"), grinder);
        assertTrue(jar.contains("guarda") && jar.contains("mão vazia") && jar.contains("retira"), jar);
        assertTrue(station.contains("mão vazia") && station.contains("agachado") && station.contains("entrada"), station);
        assertTrue(filter.contains("bancada") && filter.contains("efeito dura mais"), filter);
        assertEquals(List.of("Clique com o botão direito:", "com o ingrediente, coloca", "de mão vazia, tira o que está pronto",
            "agachado e de mãos vazias, tira a entrada"), lines(pt, "tooltip.cultivo.station.usage"));
    }
}
