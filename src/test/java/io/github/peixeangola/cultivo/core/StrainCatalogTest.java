package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.Variety.Cycle;
import io.github.peixeangola.cultivo.core.Variety.Growth;
import io.github.peixeangola.cultivo.core.Variety.Lineage;
import io.github.peixeangola.cultivo.core.Variety.Origin;
import io.github.peixeangola.cultivo.core.Variety.Rarity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Invariantes da tabela de variedades (3 nativas + 37 do catálogo) e da ordem das abas do criativo. */
class StrainCatalogTest {
    private static Map<Rarity, Integer> rarityCounts(List<Variety> varieties) {
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        varieties.forEach(v -> counts.merge(v.rarity(), 1, Integer::sum));
        return counts;
    }

    @Test
    void fortyStrainsThreeNativesAndThirtySevenFromTheCatalog() {
        assertEquals(40, Variety.values().length);
        assertEquals(List.of(Variety.BRISA, Variety.AURORA, Variety.ECLIPSE), Variety.natives());
        assertEquals(37, Variety.catalog().size());
        assertTrue(Variety.catalog().stream().noneMatch(Variety::isNative));
        assertEquals(Origin.CATALOG, Variety.MANGA_GELADA.origin());
        assertEquals(Origin.NATIVE, Variety.ECLIPSE.origin());
    }

    @Test
    void idsAreUniqueLowercaseAndResolve() {
        Set<String> ids = new HashSet<>();
        for (Variety v : Variety.values()) {
            assertTrue(v.id().matches("[a-z][a-z0-9_]*"), v.id());
            assertTrue(ids.add(v.id()), "ID repetido " + v.id());
            assertEquals(Optional.of(v), Variety.byId(v.id()));
        }
        assertEquals(Optional.of(Variety.AK_48), Variety.byId("ak_48"));
        assertEquals(Optional.of(Variety.OURO_18K), Variety.byId("ouro_18k"));
        assertEquals(Optional.empty(), Variety.byId("manga"));
        assertEquals("gelato_azul_42_seeds", Variety.GELATO_AZUL_42.seedsId());
        assertEquals("do_si_do_crop", Variety.DO_SI_DO.cropId());
    }

    @Test
    void nativesKeepTheMvpNumbers() {
        assertEquals(Rarity.COMMON, Variety.BRISA.rarity());
        assertEquals(Cycle.AUTOFLOWERING, Variety.BRISA.cycle());
        assertEquals(Lineage.SATIVA, Variety.BRISA.lineage());
        assertEquals(1.25F, Variety.BRISA.growthMultiplier());
        assertEquals(0.20, Variety.BRISA.qualityBonusChance(), 1e-12);
        assertEquals(Rarity.UNCOMMON, Variety.AURORA.rarity());
        assertEquals(1.0F, Variety.AURORA.growthMultiplier());
        assertEquals(0.35, Variety.AURORA.qualityBonusChance(), 1e-12);
        assertEquals(Rarity.RARE, Variety.ECLIPSE.rarity());
        assertEquals(Lineage.INDICA, Variety.ECLIPSE.lineage());
        assertEquals(0.8F, Variety.ECLIPSE.growthMultiplier());
        assertEquals(0.50, Variety.ECLIPSE.qualityBonusChance(), 1e-12);
        for (Variety v : Variety.natives()) {
            assertEquals(v.id(), v.artFamily(), "as nativas têm paleta própria");
        }
    }

    @Test
    void rarityCountsOverAllStrainsAndInTheCatalog() {
        assertEquals(Map.of(Rarity.COMMON, 25, Rarity.UNCOMMON, 4, Rarity.RARE, 11),
            rarityCounts(Arrays.asList(Variety.values())));
        assertEquals(Map.of(Rarity.COMMON, 24, Rarity.UNCOMMON, 3, Rarity.RARE, 10), rarityCounts(Variety.catalog()));
    }

    @Test
    void growthClassesAndPotency() {
        assertEquals(1.25F, Growth.FAST.multiplier());
        assertEquals(1.0F, Growth.MEDIUM.multiplier());
        assertEquals(0.8F, Growth.SLOW.multiplier());
        assertEquals(0.20, Variety.potencyBonusChance(1), 1e-12);
        assertEquals(0.35, Variety.potencyBonusChance(2), 1e-12);
        assertEquals(0.50, Variety.potencyBonusChance(3), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> Variety.potencyBonusChance(0));
        assertThrows(IllegalArgumentException.class, () -> Variety.potencyBonusChance(4));
        for (Variety v : Variety.values()) {
            assertTrue(v.potency() >= 1 && v.potency() <= 3, v.id());
            assertEquals(v.growth().multiplier(), v.growthMultiplier(), v.id());
        }
    }

    @Test
    void sampleCatalogRowsMatchTheOwnerCatalog() {
        assertEquals(List.of(Origin.CATALOG, Rarity.COMMON, Cycle.PHOTOPERIOD, Lineage.HYBRID, Growth.MEDIUM, 2, "dourado"),
            row(Variety.MANGA_GELADA));
        assertEquals(List.of(Origin.CATALOG, Rarity.COMMON, Cycle.PHOTOPERIOD, Lineage.SATIVA, Growth.SLOW, 3, "limao"),
            row(Variety.AMNESIA_NEBLINA));
        assertEquals(List.of(Origin.CATALOG, Rarity.COMMON, Cycle.AUTOFLOWERING, Lineage.INDICA, Growth.FAST, 1, "gelo"),
            row(Variety.LUZES_DO_NORTE));
        assertEquals(List.of(Origin.CATALOG, Rarity.UNCOMMON, Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST, 3, "verde_escuro"),
            row(Variety.GORILAO));
        assertEquals(List.of(Origin.CATALOG, Rarity.RARE, Cycle.PHOTOPERIOD, Lineage.SATIVA, Growth.SLOW, 2, "rosa"),
            row(Variety.MORANGO_AZEDO));
        assertEquals(List.of(Origin.CATALOG, Rarity.RARE, Cycle.AUTOFLOWERING, Lineage.HYBRID, Growth.FAST, 3, "dourado"),
            row(Variety.BANANONA));
    }

    private static List<Object> row(Variety v) {
        return List.of(v.origin(), v.rarity(), v.cycle(), v.lineage(), v.growth(), v.potency(), v.artFamily());
    }

    /** Regras do mapeamento do catálogo: automáticas crescem rápido; só sativas de floração longa são lentas. */
    @Test
    void catalogMappingRules() {
        for (Variety v : Variety.catalog()) {
            assertEquals(v.cycle() == Cycle.AUTOFLOWERING, v.growth() == Growth.FAST, v.id() + ": automática ⇔ rápida");
            if (v.growth() == Growth.SLOW) {
                assertEquals(Lineage.SATIVA, v.lineage(), v.id() + ": lenta só para sativa");
            }
        }
    }

    @Test
    void artFamiliesAreKnown() {
        Set<String> families = Set.of("verde", "verde_escuro", "limao", "dourado", "roxo", "azul", "rosa", "gelo", "creme");
        Set<String> used = new HashSet<>();
        for (Variety v : Variety.catalog()) {
            assertTrue(families.contains(v.artFamily()), v.id() + " família " + v.artFamily());
            used.add(v.artFamily());
        }
        assertEquals(families, used, "toda família tem ao menos uma variedade");
    }

    @Test
    void strainTabGroupsByOriginThenRarity() {
        List<Variety> order = Variety.byOriginThenRarity();
        assertEquals(40, order.size());
        assertEquals(List.of(Variety.BRISA, Variety.AURORA, Variety.ECLIPSE), order.subList(0, 3));
        assertEquals(Variety.MANGA_GELADA, order.get(3), "1ª comum do catálogo");
        assertEquals(Variety.CONFEITEZ, order.get(26), "última comum do catálogo");
        assertEquals(List.of(Variety.GORILAO, Variety.VIUVA_BRANCA, Variety.LIGEIRINHA), order.subList(27, 30));
        assertEquals(Variety.DO_SI_DO, order.get(30), "1ª rara do catálogo");
        assertEquals(Variety.BANANONA, order.get(39));
        for (int i = 1; i < order.size(); i++) {
            Variety a = order.get(i - 1);
            Variety b = order.get(i);
            boolean sorted = a.origin().compareTo(b.origin()) < 0
                || (a.origin() == b.origin() && a.rarity().compareTo(b.rarity()) <= 0);
            assertTrue(sorted, a.id() + " antes de " + b.id());
        }
    }

    @Test
    void creativeTabsSplitStrainItemsFromTheRest() {
        List<String> strainTab = ContentIds.strainTabItems();
        assertEquals(120, strainTab.size());
        assertEquals(List.of("brisa_seeds", "brisa_fresh_flower", "brisa_dried_flower"), strainTab.subList(0, 3));
        assertEquals("manga_gelada_seeds", strainTab.get(9));
        assertEquals("bananona_dried_flower", strainTab.get(119));
        assertEquals(List.of("drying_rack", "sieve", "press", "grow_tent", "wild_plant", "resin", "hash", "joint",
            "bong", "pipe", "hookah", "chillum", "rolling_paper", "filter_tip", "grinder", "lighter", "rolling_tray",
            "jar", "ashtray"), ContentIds.mainTabItems());
        List<String> both = new ArrayList<>(ContentIds.mainTabItems());
        both.addAll(strainTab);
        assertEquals(both.size(), new HashSet<>(both).size(), "um item em duas abas");
        assertEquals(new HashSet<>(ContentIds.allItems()), new HashSet<>(both), "todo item do mod em alguma aba");
    }

    @Test
    void contentListsCoverEveryStrain() {
        assertEquals(40, ContentIds.cropBlocks().size());
        assertEquals(40 * 2 + 2, ContentIds.qualityItems().size(), "flores fresca e seca de cada variedade + resina e haxixe");
        assertFalse(ContentIds.flatItems().contains("manga_gelada_crop"), "o crop é bloco, não item");
        assertTrue(ContentIds.flatItems().contains("bananona_seeds"));
    }
}
