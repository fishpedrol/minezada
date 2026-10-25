package io.github.peixeangola.cultivo.core;

/**
 * Estufinha (grow tent, pedido do dono de 25/10/2026): um bloco craftável de 2 de altura com luz roxa e um pé de planta
 * dentro, sem farmland, sem água e sem precisar de luz de fora. A qualidade da colheita (4–5) sai de
 * {@link QualityRules#harvestQuality(Variety, QualityRules.Route, double)} com a rota {@code TENT}.
 * <ul>
 *   <li>Crescimento: o mesmo sorteio do trigo ({@link GrowthRules#growthRollBound}) a cada random tick da metade de
 *       baixo, com velocidade {@link #SOIL_SPEED} × variedade × {@link #MULTIPLIER}: FAST 5, MEDIUM 6, SLOW 7 (ao ar
 *       livre, numa farmland hidratada sozinha, 6, 7 e 8).</li>
 *   <li>Clique direito: semente na estufinha vazia planta; a planta madura (idade 7) colhe e esvazia; o resto não faz
 *       nada ({@link #use}). O cliente prevê o clique com a mesma função e os dados sincronizados.</li>
 *   <li>Farinha de osso: +2 a +5 idades, até 7, como o {@code CropBlock} vanilla.</li>
 * </ul>
 * Classe pura, sem Minecraft: o bloco e os testes usam os mesmos números.
 */
public final class GrowTentRules {
    /** Base de "solo": a de uma farmland hidratada sozinha (1 + 3), como o trigo plantado isolado. */
    public static final float SOIL_SPEED = 4.0F;
    /** Multiplicador da estufinha (era o da estufa de vidro). */
    public static final float MULTIPLIER = 1.25F;
    /** Luz de bloco da lâmpada roxa, na metade de cima (cerca de 11 na metade de baixo). */
    public static final int LAMP_LIGHT = 12;
    /** Farinha de osso: o mesmo sorteio do {@code CropBlock#getBonemealAgeIncrease} vanilla (2 a 5). */
    public static final int BONE_MEAL_MIN = 2;
    public static final int BONE_MEAL_MAX = 5;

    /** O que um clique direito faz. */
    public enum Use { PLANT, HARVEST, NONE }

    private GrowTentRules() {}

    /** Velocidade de crescimento, multiplicada na mesma ordem de {@link GrowthRules#effectiveSpeed}. */
    public static float speed(Variety variety) {
        return SOIL_SPEED * variety.growthMultiplier() * MULTIPLIER;
    }

    /** Limite exclusivo do sorteio do random tick: cresce quando {@code random.nextInt(bound) == 0}. */
    public static int rollBound(Variety variety) {
        return GrowthRules.growthRollBound(speed(variety));
    }

    /** Ainda cresce: tem planta e não está madura. */
    public static boolean canGrow(boolean hasPlant, int age) {
        return hasPlant && age < GrowthRules.MAX_AGE;
    }

    /** A planta está madura e o clique colhe. */
    public static boolean isRipe(boolean hasPlant, int age) {
        return hasPlant && age >= GrowthRules.MAX_AGE;
    }

    /**
     * O clique direito: com semente na estufinha desocupada, planta (se o jogador pode construir ali: no modo aventura,
     * só com {@code can_place_on}); com a planta madura (com qualquer coisa na mão), colhe; senão nada (o vanilla segue,
     * por exemplo com a farinha de osso).
     *
     * @param occupied    tem planta ou um id de variedade desconhecido (estufinha inerte, que não planta nem colhe)
     * @param hasPlant    tem uma planta de variedade conhecida
     * @param mayPlace    o jogador pode construir na estufinha
     */
    public static Use use(boolean occupied, boolean hasPlant, int age, boolean holdingSeed, boolean mayPlace) {
        if (!occupied) {
            return holdingSeed && mayPlace ? Use.PLANT : Use.NONE;
        }
        return isRipe(hasPlant, age) ? Use.HARVEST : Use.NONE;
    }

    /** Idade depois da farinha de osso, com o bônus sorteado entre {@link #BONE_MEAL_MIN} e {@link #BONE_MEAL_MAX}. */
    public static int afterBoneMeal(int age, int bonus) {
        return Math.min(GrowthRules.MAX_AGE, age + bonus);
    }
}
