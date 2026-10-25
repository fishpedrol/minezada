package io.github.peixeangola.cultivo.core;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Dicas de uso dos tooltips em linhas curtas, como as dicas vanilla: cada dica é a chave base com as linhas
 * {@code <chave>.1} … {@code <chave>.n} no lang (no manifesto, uma lista de textos). Uma frase comprida numa linha só
 * passava da largura da tela na escala de GUI automática, e o Forge a quebrava num lugar diferente a cada movimento do
 * mouse. Classe pura: o item lê daqui quantas linhas pôr; os testes conferem o lang e a largura de cada linha.
 */
public final class TooltipHints {
    /** Linhas de cada dica, pela chave base (sem o número). */
    public static final Map<String, Integer> LINES = Map.ofEntries(
        Map.entry("tooltip.cultivo.smoke.joint", 2),
        Map.entry("tooltip.cultivo.smoke.device", 3),
        Map.entry("tooltip.cultivo.lighter", 2),
        Map.entry("tooltip.cultivo.station.usage", 4),
        Map.entry("tooltip.cultivo.rolling_paper", 3),
        Map.entry("tooltip.cultivo.filter_tip", 2),
        Map.entry("tooltip.cultivo.grinder", 4),
        Map.entry("tooltip.cultivo.rolling_tray", 3),
        Map.entry("tooltip.cultivo.jar", 4),
        Map.entry("tooltip.cultivo.ashtray", 3),
        Map.entry("tooltip.cultivo.grow_tent", 4));

    private TooltipHints() {}

    /** Chaves das linhas de uma dica, na ordem: {@code <chave>.1} até {@code <chave>.n}. */
    public static List<String> lineKeys(String key) {
        Integer lines = LINES.get(key);
        if (lines == null) {
            throw new IllegalArgumentException("dica sem linhas registradas: " + key);
        }
        return IntStream.rangeClosed(1, lines).mapToObj(i -> key + "." + i).toList();
    }
}
