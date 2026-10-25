package io.github.peixeangola.cultivo.core;

/**
 * Qualidade de colheita (1 a 5). A base sai da rota ({@link Route}): ao ar livre 2, sob teto de vidro
 * ({@link GlassRoofRules}) 3 e na estufinha ({@link GrowTentRules}) 4; cada variedade tem uma chance de +1. Resultado:
 * ar livre 2–3, teto de vidro 3–4, estufinha 4–5.
 */
public final class QualityRules {
    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int OUTDOOR_BASE = 2;
    /** Lavoura coberta por bloco de vidro (pedido do dono de 25/10/2026): perto da estufinha, mas abaixo dela. */
    public static final int GLASS_ROOF_BASE = 3;
    public static final int TENT_BASE = 4;

    /** Onde a planta foi colhida. */
    public enum Route { OUTDOOR, GLASS_ROOF, TENT }

    private QualityRules() {}

    public static int baseQuality(Route route) {
        return switch (route) {
            case OUTDOOR -> OUTDOOR_BASE;
            case GLASS_ROOF -> GLASS_ROOF_BASE;
            case TENT -> TENT_BASE;
        };
    }

    /**
     * @param route onde a planta foi colhida (decidido no servidor, na função de loot)
     * @param roll número aleatório uniforme em [0, 1) sorteado no servidor
     */
    public static int harvestQuality(Variety variety, Route route, double roll) {
        int quality = baseQuality(route);
        if (roll < variety.qualityBonusChance()) {
            quality++;
        }
        return clamp(quality);
    }

    public static int clamp(int quality) {
        return Math.max(MIN, Math.min(MAX, quality));
    }
}
