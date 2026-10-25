package io.github.peixeangola.cultivo.core;

/**
 * Teto de vidro (pedido do dono de 25/10/2026): uma lavoura coberta por bloco de vidro colhe com qualidade 3–4
 * ({@link QualityRules.Route#GLASS_ROOF}). Só o teto conta, sem paredes, para valer numa lavoura grande:
 * subindo pela coluna da planta, até {@link #RANGE} blocos, o primeiro bloco com colisão é vidro. Blocos sem colisão
 * (ar, tocha, cipó) são pulados; qualquer outro bloco sólido antes do vidro cobre a planta e ela fica sem bônus.
 * O crescimento não muda. Classe pura, sem Minecraft: o detector e os testes usam a mesma regra.
 */
public final class GlassRoofRules {
    /** O mesmo alcance da estufa do Serene Seasons: de 1 a 16 blocos acima da planta. */
    public static final int RANGE = 16;

    /** O que há num bloco da coluna acima da planta. */
    public enum Cell { PASSABLE, GLASS, SOLID }

    /** Consulta o bloco {@code dy} blocos acima da planta. */
    @FunctionalInterface
    public interface Probe {
        Cell above(int dy);
    }

    private GlassRoofRules() {}

    public static boolean isCovered(Probe probe) {
        for (int dy = 1; dy <= RANGE; dy++) {
            Cell cell = probe.above(dy);
            if (cell != Cell.PASSABLE) {
                return cell == Cell.GLASS;
            }
        }
        return false;
    }
}
