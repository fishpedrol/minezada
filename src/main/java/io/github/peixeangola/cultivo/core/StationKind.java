package io.github.peixeangola.cultivo.core;

/** As três estações de processamento por ticks e seus tipos de receita. */
public enum StationKind {
    DRYING("drying_rack", "drying", 600),
    SIEVING("sieve", "sieving", 200),
    PRESSING("press", "pressing", 300);

    private final String blockId;
    private final String recipeTypeId;
    private final int defaultTicks;

    StationKind(String blockId, String recipeTypeId, int defaultTicks) {
        this.blockId = blockId;
        this.recipeTypeId = recipeTypeId;
        this.defaultTicks = defaultTicks;
    }

    public String blockId() {
        return blockId;
    }

    /** ID do tipo e do serializer de receita no namespace cultivo. */
    public String recipeTypeId() {
        return recipeTypeId;
    }

    /**
     * Tag de item (namespace cultivo) com tudo que a estação aceita na entrada. Espelha os ingredientes das
     * receitas desta estação; o cliente usa a tag para prever o clique, já que as receitas não chegam a ele.
     */
    public String inputTagId() {
        return blockId + "_inputs";
    }

    /** Duração documentada da receita padrão desta estação. */
    public int defaultTicks() {
        return defaultTicks;
    }
}
