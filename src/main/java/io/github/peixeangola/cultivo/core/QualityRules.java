package io.github.peixeangola.cultivo.core;

/**
 * Qualidade de colheita (1 a 5). Fora da estufinha (ao ar livre, sob vidro, em qualquer lugar) parte de 2 e na
 * estufinha ({@link GrowTentRules}) de 4; cada variedade tem uma chance de +1. Resultado: fora 2–3, estufinha 4–5.
 */
public final class QualityRules {
    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int OUTDOOR_BASE = 2;
    public static final int TENT_BASE = 4;

    private QualityRules() {}

    public static int baseQuality(boolean tent) {
        return tent ? TENT_BASE : OUTDOOR_BASE;
    }

    /**
     * @param tent a planta foi colhida na estufinha (decidido no servidor, na função de loot)
     * @param roll número aleatório uniforme em [0, 1) sorteado no servidor
     */
    public static int harvestQuality(Variety variety, boolean tent, double roll) {
        int quality = baseQuality(tent);
        if (roll < variety.qualityBonusChance()) {
            quality++;
        }
        return clamp(quality);
    }

    public static int clamp(int quality) {
        return Math.max(MIN, Math.min(MAX, quality));
    }
}
