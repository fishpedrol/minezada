package io.github.peixeangola.cultivo.core;

/**
 * Pote (cultivo:jar): guarda uma pilha e cura flores secas. Uma pilha de flores secas que fica {@link #CURE_TICKS}
 * ticks no pote sem o conteúdo mudar ganha +1 de qualidade (até {@link QualityRules#MAX}) e a marca cultivo:cured, uma
 * vez só. O contador zera sempre que o conteúdo muda. Com a cura, o outdoor vai de 2–3 para 3–4 e a estufinha de 4–5
 * para 5: a estufinha continua melhor. Também define o sinal de comparador do pote. Classe pura; mecânica fictícia de
 * jogo, em ticks.
 */
public final class CuringRules {
    /** Ticks sem mudança no conteúdo para curar (2 minutos de jogo). */
    public static final int CURE_TICKS = 2400;
    /** Sinal máximo de um comparador. */
    public static final int COMPARATOR_MAX = 15;

    /** Resultado de um tick: novo contador e se a cura terminou neste tick. */
    public record Tick(int progress, boolean completed) {}

    private CuringRules() {}

    /** Só flor seca ainda não curada cura (resina, haxixe, baseado e flor já curada não). */
    public static boolean canCure(boolean driedFlower, boolean alreadyCured) {
        return driedFlower && !alreadyCured;
    }

    /**
     * Qualidade depois da cura: +1, até 5. Sem qualidade conta como {@link SmokeRules#DEFAULT_QUALITY}, como ao
     * fumar.
     */
    public static int curedQuality(Integer quality) {
        return Math.min(QualityRules.MAX, SmokeRules.effectiveQuality(quality) + 1);
    }

    /**
     * Avança o contador um tick. Só conta enquanto a pilha pode ser curada (senão volta a 0); no tick
     * {@link #CURE_TICKS} a cura termina e o contador zera.
     */
    public static Tick advance(int progress, boolean curable) {
        if (!curable) {
            return new Tick(0, false);
        }
        int next = progress + 1;
        if (next >= CURE_TICKS) {
            return new Tick(0, true);
        }
        return new Tick(next, false);
    }

    /** Sinal do comparador do pote: ⌈15 × quantidade / pilha máxima⌉; pote vazio dá 0. */
    public static int comparatorSignal(int count, int maxStackSize) {
        if (count <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return Math.min(COMPARATOR_MAX, (COMPARATOR_MAX * count + maxStackSize - 1) / maxStackSize);
    }
}
