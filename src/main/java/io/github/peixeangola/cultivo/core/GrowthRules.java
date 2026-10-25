package io.github.peixeangola.cultivo.core;

/**
 * Crescimento igual ao do trigo: a cada random tick com luz suficiente, cresce 1 idade com chance
 * 1 / (floor(25 / velocidade) + 1). A velocidade vanilla é multiplicada pela variedade. A estufa de vidro não dá
 * mais bônus (pedido do dono de 25/10/2026); a estufinha tem a regra própria em {@link GrowTentRules}.
 */
public final class GrowthRules {
    public static final int MIN_LIGHT = 9;
    public static final int MAX_AGE = 7;

    private GrowthRules() {}

    public static float effectiveSpeed(float vanillaSpeed, Variety variety) {
        return vanillaSpeed * variety.growthMultiplier();
    }

    /** Limite exclusivo do sorteio: cresce quando {@code random.nextInt(bound) == 0}. */
    public static int growthRollBound(float effectiveSpeed) {
        if (effectiveSpeed <= 0.0F) {
            throw new IllegalArgumentException("speed must be positive");
        }
        return (int) (25.0F / effectiveSpeed) + 1;
    }

    public static boolean hasEnoughLight(int rawBrightness) {
        return rawBrightness >= MIN_LIGHT;
    }

    /** Estágio visual (0–3) para a idade (0–7): 0–1, 2–3, 4–5, 6–7. */
    public static int visualStage(int age) {
        if (age < 0 || age > MAX_AGE) {
            throw new IllegalArgumentException("age " + age);
        }
        return age / 2;
    }
}
