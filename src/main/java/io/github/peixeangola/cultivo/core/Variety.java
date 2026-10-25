package io.github.peixeangola.cultivo.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tabela única das variedades (genéticas) do Cultivo: as 3 nativas do MVP e as 37 do catálogo, todas com nomes
 * de paródia. Classe pura: sem dependência do Minecraft, testável com JUnit. Os nomes exibidos (pt/en) ficam no
 * manifesto do gerador (tools/gen/manifest.json, seção "strains"), que um teste confere contra esta tabela coluna
 * por coluna e na mesma ordem; docs/strains-catalog.json é a fonte do dono para o catálogo.
 *
 * <p>Todos os atributos são rótulos de gameplay em faixas abstratas do jogo, sem números do mundo real:
 * <ul>
 *   <li>origem: nativa (planta silvestre, lavouras de aldeia e baús) ou catálogo (baús e mascate);</li>
 *   <li>raridade: peso nas fontes de sementes ({@link SeedSources});</li>
 *   <li>ciclo e genética: rótulos do tooltip; a genética (campo "genetics" no manifesto) escolhe o perfil de
 *       efeitos ao fumar ({@link SmokeRules});</li>
 *   <li>crescimento: rápida ×1,25, média ×1,0 ou lenta ×0,8 sobre a velocidade do trigo;</li>
 *   <li>potência 1–3: chance de +1 de qualidade na colheita (0,20 / 0,35 / 0,50);</li>
 *   <li>família de arte: paleta das texturas geradas (as nativas têm paleta própria).</li>
 * </ul>
 */
public enum Variety {
    //                 origem          raridade         ciclo                genética        crescimento    pot família
    // Nativas: os números do MVP não mudam
    BRISA(             Origin.NATIVE,  Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.SATIVA, Growth.FAST,   1, "brisa"),
    AURORA(            Origin.NATIVE,  Rarity.UNCOMMON, Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 2, "aurora"),
    ECLIPSE(           Origin.NATIVE,  Rarity.RARE,     Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.SLOW,   3, "eclipse"),
    // Catálogo, na ordem de docs/strains-catalog.json
    MANGA_GELADA(      Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 2, "dourado"),
    AMNESIA_NEBLINA(   Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.SATIVA, Growth.SLOW,   3, "limao"),
    COLA_DE_GORILA(    Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 3, "gelo"),
    OURO_COLOMBIANO(   Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.SATIVA, Growth.SLOW,   2, "dourado"),
    ULTRAVIOLETA(      Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 2, "roxo"),
    KUSH_ROXINHA(      Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "roxo"),
    NEBLINA_ROXA(      Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.SATIVA, Growth.FAST,   2, "roxo"),
    AK_48(             Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   2, "gelo"),
    JACK_HEROI(        Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.SATIVA, Growth.FAST,   2, "limao"),
    BRUNO_BANNER(      Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   3, "verde"),
    OURO_18K(          Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 2, "dourado"),
    KUSH_DO_BUBBA(     Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "verde_escuro"),
    KUSH_DE_MANGA(     Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "dourado"),
    MIRTILO(           Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "azul"),
    ANDARILHO_DO_CEU(  Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "azul"),
    SOCO_ROXO(         Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "roxo"),
    TRI_CAMPEA(        Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 2, "verde"),
    BOLO_DE_CASAMENTO( Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   3, "gelo"),
    CARAMELO_CRITICO(  Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "creme"),
    LUZES_DO_NORTE(    Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   1, "gelo"),
    BALA_SORTIDA(      Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   3, "rosa"),
    AZUL_ARROXEADO(    Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "azul"),
    CREME_DE_CARAMELO( Origin.CATALOG, Rarity.COMMON,   Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "creme"),
    CONFEITEZ(         Origin.CATALOG, Rarity.COMMON,   Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 2, "rosa"),
    GORILAO(           Origin.CATALOG, Rarity.UNCOMMON, Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   3, "verde_escuro"),
    VIUVA_BRANCA(      Origin.CATALOG, Rarity.UNCOMMON, Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   2, "gelo"),
    LIGEIRINHA(        Origin.CATALOG, Rarity.UNCOMMON, Cycle.AUTOFLOWERING, Lineage.SATIVA, Growth.FAST,   2, "limao"),
    DO_SI_DO(          Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "roxo"),
    VIAGEM_25(         Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   2, "verde"),
    CAO_QUIMICO(       Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   2, "limao"),
    MORANGO_COM_BANANA(Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST,   3, "rosa"),
    FRUTINHA_VELOZ(    Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   2, "rosa"),
    CHICLETE(          Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   2, "rosa"),
    VIAGEM_LISERGICA(  Origin.CATALOG, Rarity.RARE,     Cycle.PHOTOPERIOD,   Lineage.INDICA, Growth.MEDIUM, 3, "roxo"),
    GELATO_AZUL_42(    Origin.CATALOG, Rarity.RARE,     Cycle.PHOTOPERIOD,   Lineage.HYBRID, Growth.MEDIUM, 3, "azul"),
    MORANGO_AZEDO(     Origin.CATALOG, Rarity.RARE,     Cycle.PHOTOPERIOD,   Lineage.SATIVA, Growth.SLOW,   2, "rosa"),
    BANANONA(          Origin.CATALOG, Rarity.RARE,     Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST,   3, "dourado");

    /** Nativa (landrace do mundo: planta silvestre e lavouras de aldeia) ou do catálogo de paródias. */
    public enum Origin { NATIVE, CATALOG }

    public enum Rarity { COMMON, UNCOMMON, RARE }

    public enum Cycle { AUTOFLOWERING, PHOTOPERIOD }

    /** Genética dominante: sativa, híbrida ou índica. Define o perfil de efeitos ao fumar. */
    public enum Lineage { SATIVA, HYBRID, INDICA }

    /** Classe de crescimento: multiplicador sobre a velocidade vanilla do trigo. */
    public enum Growth {
        FAST(1.25F),
        MEDIUM(1.0F),
        SLOW(0.8F);

        private final float multiplier;

        Growth(float multiplier) {
            this.multiplier = multiplier;
        }

        public float multiplier() {
            return multiplier;
        }
    }

    public static final int MIN_POTENCY = 1;
    public static final int MAX_POTENCY = 3;

    private static final Map<String, Variety> BY_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(Variety::id, Function.identity()));

    private final Origin origin;
    private final Rarity rarity;
    private final Cycle cycle;
    private final Lineage lineage;
    private final Growth growth;
    private final int potency;
    private final String artFamily;

    Variety(Origin origin, Rarity rarity, Cycle cycle, Lineage lineage, Growth growth, int potency, String artFamily) {
        if (potency < MIN_POTENCY || potency > MAX_POTENCY) {
            throw new IllegalArgumentException("potência fora de 1–3: " + potency);
        }
        this.origin = origin;
        this.rarity = rarity;
        this.cycle = cycle;
        this.lineage = lineage;
        this.growth = growth;
        this.potency = potency;
        this.artFamily = artFamily;
    }

    /** Variedade pelo ID (brisa, manga_gelada, ...), se existir. */
    public static Optional<Variety> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** As nativas, na ordem da tabela. */
    public static List<Variety> natives() {
        return Arrays.stream(values()).filter(Variety::isNative).toList();
    }

    /** As do catálogo, na ordem da tabela. */
    public static List<Variety> catalog() {
        return Arrays.stream(values()).filter(v -> !v.isNative()).toList();
    }

    /** Agrupadas por origem (nativas primeiro) e depois raridade (comum, incomum, rara); empates na ordem da tabela. */
    public static List<Variety> byOriginThenRarity() {
        return Arrays.stream(values())
            .sorted(Comparator.comparing(Variety::origin).thenComparing(Variety::rarity))
            .toList();
    }

    /**
     * Chance de +1 de qualidade na colheita pela potência: 1 → 0,20, 2 → 0,35, 3 → 0,50 (os valores de Brisa,
     * Aurora e Eclipse no MVP).
     */
    public static double potencyBonusChance(int potency) {
        return switch (potency) {
            case 1 -> 0.20D;
            case 2 -> 0.35D;
            case 3 -> 0.50D;
            default -> throw new IllegalArgumentException("potência fora de 1–3: " + potency);
        };
    }

    /** Nome usado nos IDs de registro: brisa, manga_gelada, ak_48, ... */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Origin origin() {
        return origin;
    }

    public boolean isNative() {
        return origin == Origin.NATIVE;
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

    public Growth growth() {
        return growth;
    }

    /** Potência de 1 a 3 (estrelas no tooltip). */
    public int potency() {
        return potency;
    }

    /** Família da paleta das texturas (verde, roxo, gelo, ...; as nativas usam o próprio ID). */
    public String artFamily() {
        return artFamily;
    }

    /** Multiplicador sobre a velocidade de crescimento vanilla do trigo. */
    public float growthMultiplier() {
        return growth.multiplier();
    }

    /** Chance de +1 de qualidade na colheita. */
    public double qualityBonusChance() {
        return potencyBonusChance(potency);
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
