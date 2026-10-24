package io.github.peixeangola.cultivo.core;

/**
 * Qualidade de colheita (1 a 5). Outdoor parte de 2 e estufa de 4; cada variedade tem uma chance de +1.
 * Resultado: outdoor 2–3, estufa 4–5.
 */
public final class QualityRules {
    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int OUTDOOR_BASE = 2;
    public static final int GREENHOUSE_BASE = 4;

    private QualityRules() {}

    public static int baseQuality(boolean greenhouse) {
        return greenhouse ? GREENHOUSE_BASE : OUTDOOR_BASE;
    }

    /**
     * @param roll número aleatório uniforme em [0, 1) sorteado no servidor
     */
    public static int harvestQuality(Variety variety, boolean greenhouse, double roll) {
        int quality = baseQuality(greenhouse);
        if (roll < variety.qualityBonusChance()) {
            quality++;
        }
        return clamp(quality);
    }

    public static int clamp(int quality) {
        return Math.max(MIN, Math.min(MAX, quality));
    }
}
