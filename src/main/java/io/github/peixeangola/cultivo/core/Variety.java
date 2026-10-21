package io.github.peixeangola.cultivo.core;

import java.util.Locale;

/**
 * As três variedades fictícias do MVP. Classe pura: sem dependência do Minecraft, testável com JUnit.
 * Os perfis de ciclo/genética são só rótulos de gameplay inspirados em categorias de catálogo.
 */
public enum Variety {
    BRISA(Rarity.COMMON, Cycle.AUTOFLOWERING, Lineage.SATIVA, 1.25F, 0.20D),
    AURORA(Rarity.UNCOMMON, Cycle.PHOTOPERIOD, Lineage.HYBRID, 1.0F, 0.35D),
    ECLIPSE(Rarity.RARE, Cycle.PHOTOPERIOD, Lineage.INDICA, 0.8F, 0.50D);

    public enum Rarity { COMMON, UNCOMMON, RARE }

    public enum Cycle { AUTOFLOWERING, PHOTOPERIOD }

    public enum Lineage { SATIVA, HYBRID, INDICA }

    private final Rarity rarity;
    private final Cycle cycle;
    private final Lineage lineage;
    private final float growthMultiplier;
    private final double qualityBonusChance;

    Variety(Rarity rarity, Cycle cycle, Lineage lineage, float growthMultiplier, double qualityBonusChance) {
        this.rarity = rarity;
        this.cycle = cycle;
        this.lineage = lineage;
        this.growthMultiplier = growthMultiplier;
        this.qualityBonusChance = qualityBonusChance;
    }

    /** Nome usado nos IDs de registro: brisa, aurora, eclipse. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Rarity rarity() {
        return rarity;
    }

    public Cycle cycle() {
        return cycle;
    }

    public Lineage lineage() {
        return lineage;
    }

    /** Multiplicador sobre a velocidade de crescimento vanilla do trigo. */
    public float growthMultiplier() {
        return growthMultiplier;
    }

    /** Chance de +1 de qualidade na colheita. */
    public double qualityBonusChance() {
        return qualityBonusChance;
    }

    public String seedsId() {
        return id() + "_seeds";
    }

    public String cropId() {
        return id() + "_crop";
    }

    public String freshFlowerId() {
        return id() + "_fresh_flower";
    }

    public String driedFlowerId() {
        return id() + "_dried_flower";
    }
}
