package io.github.peixeangola.cultivo.core;

/** Regras puras das estações: rendimento, compatibilidade da saída e avanço do progresso. */
public final class ProcessingRules {
    public static final int SIEVE_BONUS_MIN_QUALITY = 4;
    /** Dichavador instalado na peneira: resina a mais por ciclo, somada ao bônus de qualidade. */
    public static final int GRINDER_BONUS = 1;

    private ProcessingRules() {}

    /** Quantidade produzida por ciclo sem dichavador (veja {@link #outputCount(StationKind, int, Integer, boolean)}). */
    public static int outputCount(StationKind kind, int baseCount, Integer quality) {
        return outputCount(kind, baseCount, quality, false);
    }

    /**
     * Quantidade produzida por ciclo. Só a peneira tem bônus: qualidade ≥ 4 rende +1 resina e o dichavador instalado
     * rende mais +1 (os dois somam). Secagem e prensa ignoram os dois.
     *
     * @param quality qualidade da entrada, ou {@code null} se a entrada não tem qualidade
     * @param grinder a peneira tem dichavador (blockstate grinder=true)
     */
    public static int outputCount(StationKind kind, int baseCount, Integer quality, boolean grinder) {
        if (kind != StationKind.SIEVING) {
            return baseCount;
        }
        int count = baseCount;
        if (quality != null && quality >= SIEVE_BONUS_MIN_QUALITY) {
            count++;
        }
        if (grinder) {
            count += GRINDER_BONUS;
        }
        return count;
    }

    /**
     * A saída aceita o resultado se estiver vazia, ou se for o mesmo item com os mesmos componentes
     * (mesma qualidade) e couber inteiro na pilha.
     */
    public static boolean outputAccepts(boolean outputEmpty, boolean sameItemAndComponents, int outputCount,
                                        int resultCount, int maxStackSize) {
        if (outputEmpty) {
            return resultCount <= maxStackSize;
        }
        return sameItemAndComponents && outputCount + resultCount <= maxStackSize;
    }

    /** Resultado de um tick: novo progresso e se o ciclo terminou. */
    public record Tick(int progress, boolean completed) {}

    /**
     * Avança o progresso. Se não há receita ou entrada suficiente, zera. Se a saída está bloqueada, mantém
     * o progresso parado (sem perda nem duplicação).
     */
    public static Tick advance(int progress, int processingTime, boolean hasRecipe, boolean outputAccepts) {
        if (!hasRecipe) {
            return new Tick(0, false);
        }
        if (!outputAccepts) {
            return new Tick(Math.min(progress, processingTime), false);
        }
        int next = progress + 1;
        if (next >= processingTime) {
            return new Tick(0, true);
        }
        return new Tick(next, false);
    }
}
