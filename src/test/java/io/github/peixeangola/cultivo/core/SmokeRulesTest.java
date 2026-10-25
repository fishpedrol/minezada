package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.SmokeRules.EffectKind;
import io.github.peixeangola.cultivo.core.SmokeRules.EffectSpec;
import io.github.peixeangola.cultivo.core.SmokeRules.Material;
import io.github.peixeangola.cultivo.core.SmokeRules.Refusal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SmokeRulesTest {
    private static Optional<EffectSpec> find(List<EffectSpec> effects, EffectKind kind) {
        return effects.stream().filter(e -> e.kind() == kind).findFirst();
    }

    private static int high(List<EffectSpec> effects) {
        return find(effects, EffectKind.HIGH).orElseThrow().ticks();
    }

    @Test
    void lighterAlwaysRequired() {
        for (SmokeDevice device : SmokeDevice.values()) {
            assertEquals(Refusal.NEEDS_LIGHTER, SmokeRules.check(device, false, true, 0), device.name());
        }
    }

    @Test
    void onlyJointSkipsFuel() {
        assertEquals(Refusal.NONE, SmokeRules.check(SmokeDevice.JOINT, true, false, 0));
        for (SmokeDevice device : List.of(SmokeDevice.PIPE, SmokeDevice.CHILLUM, SmokeDevice.BONG, SmokeDevice.HOOKAH)) {
            assertTrue(device.needsFuel());
            assertEquals(Refusal.NEEDS_FUEL, SmokeRules.check(device, true, false, 0), device.name());
            assertEquals(Refusal.NONE, SmokeRules.check(device, true, true, 0), device.name());
        }
    }

    @Test
    void deviceIdsMatchItems() {
        for (SmokeDevice device : SmokeDevice.values()) {
            assertEquals(Optional.of(device), SmokeDevice.byItemId(device.itemId()));
            assertTrue(device == SmokeDevice.JOINT || ContentIds.ARTICLES.contains(device.itemId()), device.itemId());
        }
        assertEquals(Optional.empty(), SmokeDevice.byItemId("grinder"));
    }

    @Test
    void baseDurationIs45SecondsForJointQuality2() {
        assertEquals(45 * 20, SmokeRules.sessionTicks(SmokeDevice.JOINT, new Material(Variety.AURORA, 2, false)));
        assertEquals(45 * 20, SmokeRules.sessionTicks(SmokeDevice.JOINT, new Material(Variety.AURORA, null, false)),
            "sem qualidade conta como 2");
    }

    @Test
    void qualityHashAndDeviceScaleDuration() {
        Material q2 = new Material(Variety.BRISA, 2, false);
        Material q5 = new Material(Variety.BRISA, 5, false);
        Material hash = new Material(null, 2, true);
        assertEquals(Math.round(45 * 1.6F * 20), SmokeRules.sessionTicks(SmokeDevice.JOINT, q5));
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.PIPE, q5) > SmokeRules.sessionTicks(SmokeDevice.PIPE, q2));
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.PIPE, hash) > SmokeRules.sessionTicks(SmokeDevice.PIPE, q2));
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.BONG, q2) > SmokeRules.sessionTicks(SmokeDevice.CHILLUM, q2));
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.CHILLUM, q2) > SmokeRules.sessionTicks(SmokeDevice.PIPE, q2));
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.HOOKAH, q2) < SmokeRules.sessionTicks(SmokeDevice.PIPE, q2));
    }

    @Test
    void growTentQualityBeatsOutdoor() {
        // Estufinha dá 4–5 e outdoor 2–3: a pior estufinha ainda rende mais que o melhor outdoor
        assertTrue(SmokeRules.sessionTicks(SmokeDevice.JOINT, new Material(Variety.ECLIPSE, 4, false))
            > SmokeRules.sessionTicks(SmokeDevice.JOINT, new Material(Variety.ECLIPSE, 3, false)));
    }

    @Test
    void repeatedSmokingStacksUpToCap() {
        Material m = new Material(Variety.ECLIPSE, 5, true);
        int remaining = 0;
        for (int i = 0; i < 50; i++) {
            remaining = high(SmokeRules.effects(SmokeDevice.BONG, m, remaining));
            assertTrue(remaining <= SmokeRules.MAX_HIGH_TICKS);
        }
        assertEquals(SmokeRules.MAX_HIGH_TICKS, remaining);
        assertEquals(SmokeRules.MAX_HIGH_TICKS, SmokeRules.totalHighTicks(SmokeRules.MAX_HIGH_TICKS, 1));
        assertEquals(100, SmokeRules.totalHighTicks(-5, 100), "resto negativo é ignorado");
    }

    @Test
    void heavyAboveTwoMinutes() {
        Material m = new Material(Variety.AURORA, 2, false);
        List<EffectSpec> light = SmokeRules.effects(SmokeDevice.JOINT, m, 0);
        assertEquals(0, find(light, EffectKind.HIGH).orElseThrow().amplifier());
        assertTrue(find(light, EffectKind.NAUSEA).orElseThrow().ticks() <= SmokeRules.NAUSEA_LIGHT_MAX_TICKS);
        List<EffectSpec> heavy = SmokeRules.effects(SmokeDevice.JOINT, m, SmokeRules.HEAVY_THRESHOLD_TICKS);
        assertEquals(1, find(heavy, EffectKind.HIGH).orElseThrow().amplifier());
        assertTrue(find(heavy, EffectKind.NAUSEA).orElseThrow().ticks() > SmokeRules.NAUSEA_LIGHT_MAX_TICKS);
        assertTrue(find(heavy, EffectKind.NAUSEA).orElseThrow().ticks() <= SmokeRules.NAUSEA_HEAVY_MAX_TICKS);
    }

    @ParameterizedTest
    @EnumSource(SmokeDevice.class)
    void everySessionGivesHighHungerAndNausea(SmokeDevice device) {
        List<EffectSpec> effects = SmokeRules.effects(device, new Material(Variety.BRISA, 3, false), 0);
        int high = high(effects);
        assertTrue(high > 0);
        assertEquals(high, find(effects, EffectKind.HUNGER).orElseThrow().ticks(), "larica dura o mesmo tempo");
        assertTrue(find(effects, EffectKind.NAUSEA).orElseThrow().ticks() > 0);
        assertTrue(effects.stream().allMatch(e -> e.ticks() > 0 && e.amplifier() >= 0 && e.amplifier() <= 1));
    }

    @Test
    void lineageProfiles() {
        List<EffectSpec> sativa = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false), 0);
        assertTrue(find(sativa, EffectKind.SPEED).isPresent());
        assertFalse(find(sativa, EffectKind.SLOWNESS).isPresent());

        List<EffectSpec> hybrid = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.AURORA, 2, false), 0);
        assertEquals(SmokeRules.REGENERATION_TICKS, find(hybrid, EffectKind.REGENERATION).orElseThrow().ticks());
        assertFalse(find(hybrid, EffectKind.SPEED).isPresent());
        assertFalse(find(hybrid, EffectKind.SLOWNESS).isPresent());

        List<EffectSpec> indica = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.ECLIPSE, 2, false), 0);
        assertTrue(find(indica, EffectKind.SLOWNESS).isPresent());
        assertEquals(SmokeRules.REGENERATION_TICKS * 2, find(indica, EffectKind.REGENERATION).orElseThrow().ticks());

        List<EffectSpec> unknown = SmokeRules.effects(SmokeDevice.PIPE, new Material(null, 4, true), 0);
        assertTrue(find(unknown, EffectKind.SLOWNESS).isPresent());
        assertFalse(find(unknown, EffectKind.SPEED).isPresent());
        assertFalse(find(unknown, EffectKind.REGENERATION).isPresent());
    }

    /** As variedades do catálogo usam os perfis pela genética, com os mesmos números das nativas. */
    @Test
    void catalogStrainsReuseLineageProfiles() {
        List<EffectSpec> sativa = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.AMNESIA_NEBLINA, 2, false), 0);
        assertEquals(450, find(sativa, EffectKind.SPEED).orElseThrow().ticks(), "sativa: velocidade por metade da sessão");
        assertFalse(find(sativa, EffectKind.SLOWNESS).isPresent());

        List<EffectSpec> hybrid = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.BANANONA, 2, false), 0);
        assertEquals(100, find(hybrid, EffectKind.REGENERATION).orElseThrow().ticks(), "híbrida: 5 s de regeneração");
        assertFalse(find(hybrid, EffectKind.SPEED).isPresent());
        assertFalse(find(hybrid, EffectKind.SLOWNESS).isPresent());

        List<EffectSpec> indica = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.MIRTILO, 2, false), 0);
        assertEquals(450, find(indica, EffectKind.SLOWNESS).orElseThrow().ticks());
        assertEquals(200, find(indica, EffectKind.REGENERATION).orElseThrow().ticks(), "índica: 10 s de regeneração");

        // Potência e raridade não mudam a duração: só artigo, qualidade e haxixe
        assertEquals(900, high(sativa));
        assertEquals(900, high(hybrid));
        assertEquals(1260, SmokeRules.sessionTicks(SmokeDevice.PIPE, new Material(Variety.LUZES_DO_NORTE, 4, false)));
        for (Variety v : Variety.values()) {
            List<EffectSpec> effects = SmokeRules.effects(SmokeDevice.JOINT, new Material(v, 2, false), 0);
            List<EffectSpec> sameLineage = SmokeRules.effects(SmokeDevice.JOINT, new Material(switch (v.lineage()) {
                case SATIVA -> Variety.BRISA;
                case HYBRID -> Variety.AURORA;
                case INDICA -> Variety.ECLIPSE;
            }, 2, false), 0);
            assertEquals(sameLineage, effects, v.id() + " deveria ter o perfil da nativa da mesma genética");
        }
    }

    @Test
    void varietyById() {
        assertEquals(Optional.of(Variety.ECLIPSE), Variety.byId("eclipse"));
        assertEquals(Optional.empty(), Variety.byId("ECLIPSE"));
        assertEquals(Optional.empty(), Variety.byId(""));
    }

    @Test
    void qualityOutsideRangeIsClamped() {
        assertEquals(5, SmokeRules.effectiveQuality(9));
        assertEquals(1, SmokeRules.effectiveQuality(-1));
    }

    @Test
    void exactDurationsPerDeviceQualityAndHash() {
        Material q2 = new Material(Variety.BRISA, 2, false);
        assertEquals(900, SmokeRules.sessionTicks(SmokeDevice.JOINT, q2));
        assertEquals(900, SmokeRules.sessionTicks(SmokeDevice.PIPE, q2));
        assertEquals(1080, SmokeRules.sessionTicks(SmokeDevice.CHILLUM, q2));
        assertEquals(1440, SmokeRules.sessionTicks(SmokeDevice.BONG, q2));
        assertEquals(720, SmokeRules.sessionTicks(SmokeDevice.HOOKAH, q2));
        assertEquals(1350, SmokeRules.sessionTicks(SmokeDevice.PIPE, new Material(null, 2, true)), "haxixe ×1,5");
        assertEquals(1260, SmokeRules.sessionTicks(SmokeDevice.PIPE, new Material(Variety.BRISA, 4, false)), "q4 ×1,4");
        assertEquals(3456, SmokeRules.sessionTicks(SmokeDevice.BONG, new Material(null, 5, true)), "bong+haxixe q5");
    }

    @Test
    void useTimes() {
        assertEquals(32, SmokeDevice.JOINT.useTicks());
        assertEquals(40, SmokeDevice.BONG.useTicks());
        assertEquals(48, SmokeDevice.HOOKAH.useTicks());
    }

    @Test
    void lighterHas64UsesLosingOnePerUse() {
        assertEquals(64, SmokeRules.LIGHTER_DURABILITY);
        assertEquals(1, SmokeRules.LIGHTER_DAMAGE_PER_USE);
    }

    @Test
    void refusalOrderAndMessages() {
        for (SmokeDevice device : SmokeDevice.values()) {
            assertEquals(Refusal.NEEDS_LIGHTER, SmokeRules.check(device, false, false, SmokeRules.MAX_HIGH_TICKS));
        }
        assertEquals(Refusal.NEEDS_FUEL, SmokeRules.check(SmokeDevice.PIPE, true, false, SmokeRules.MAX_HIGH_TICKS));
        assertEquals(Refusal.AT_MAX, SmokeRules.check(SmokeDevice.PIPE, true, true, SmokeRules.MAX_HIGH_TICKS));
        assertEquals(null, Refusal.NONE.messageKey());
        assertEquals("message.cultivo.need_lighter", Refusal.NEEDS_LIGHTER.messageKey());
        assertEquals("message.cultivo.need_fuel", Refusal.NEEDS_FUEL.messageKey());
        assertEquals("message.cultivo.too_high", Refusal.AT_MAX.messageKey());
    }

    @Test
    void specConstants() {
        assertEquals(6 * 60 * 20, SmokeRules.MAX_HIGH_TICKS, "limite de 6 min");
        assertEquals(2 * 60 * 20, SmokeRules.HEAVY_THRESHOLD_TICKS, "nível II a partir de 2 min");
        assertEquals(30 * 20, SmokeRules.AT_MAX_MARGIN_TICKS, "margem de 30 s");
        assertEquals(8 * 20, SmokeRules.NAUSEA_LIGHT_MAX_TICKS);
        assertEquals(20 * 20, SmokeRules.NAUSEA_HEAVY_MAX_TICKS);
    }

    @Test
    void refusedOnlyWhenAlmostAtCap() {
        assertEquals(Refusal.NONE, SmokeRules.check(SmokeDevice.JOINT, true, false, 6600));
        assertEquals(Refusal.AT_MAX, SmokeRules.check(SmokeDevice.JOINT, true, false, 6601));
        assertEquals(Refusal.NONE, SmokeRules.check(SmokeDevice.JOINT, true, false, 0));
        assertEquals(Refusal.AT_MAX, SmokeRules.check(SmokeDevice.JOINT, true, false, -1), "infinito conta como no limite");
    }

    @Test
    void sideEffectsFollowTheSessionNotTheAccumulatedTotal() {
        Material sativa = new Material(Variety.BRISA, 2, false);
        int session = SmokeRules.sessionTicks(SmokeDevice.JOINT, sativa);
        List<EffectSpec> nearCap = SmokeRules.effects(SmokeDevice.JOINT, sativa, SmokeRules.MAX_HIGH_TICKS - session);
        assertEquals(SmokeRules.MAX_HIGH_TICKS, high(nearCap));
        assertEquals(SmokeRules.MAX_HIGH_TICKS, find(nearCap, EffectKind.HUNGER).orElseThrow().ticks(), "larica acompanha o total");
        assertEquals(Math.round(session * 0.5F), find(nearCap, EffectKind.SPEED).orElseThrow().ticks(), "velocidade só da sessão");
        assertEquals(Math.min(SmokeRules.NAUSEA_HEAVY_MAX_TICKS, Math.round(session * 0.3F)),
            find(nearCap, EffectKind.NAUSEA).orElseThrow().ticks());
        List<EffectSpec> indica = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.ECLIPSE, 2, false), 5000);
        assertEquals(450, find(indica, EffectKind.SLOWNESS).orElseThrow().ticks());
        List<EffectSpec> unknown = SmokeRules.effects(SmokeDevice.PIPE, new Material(null, 2, false), 5000);
        assertEquals(270, find(unknown, EffectKind.SLOWNESS).orElseThrow().ticks());
    }

    // ------------------------------------------------------------------ piteira (baseado com cultivo:filtered)

    @Test
    void filterConstants() {
        assertEquals(1.25F, SmokeRules.FILTER_DURATION_MULTIPLIER);
        assertEquals(2, SmokeRules.FILTER_NAUSEA_DIVISOR);
    }

    /** Brisa qualidade 2 no baseado: comum 900 / 160; com piteira 1125 / 80 (plano 1.2). */
    @Test
    void filteredJointLastsLongerWithHalfTheNausea() {
        List<EffectSpec> plain = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false), 0);
        assertEquals(900, high(plain));
        assertEquals(160, find(plain, EffectKind.NAUSEA).orElseThrow().ticks());

        List<EffectSpec> filtered = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false, true), 0);
        assertEquals(1125, SmokeRules.sessionTicks(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false, true)));
        assertEquals(1125, high(filtered));
        assertEquals(80, find(filtered, EffectKind.NAUSEA).orElseThrow().ticks());
        assertEquals(1125, find(filtered, EffectKind.HUNGER).orElseThrow().ticks(), "larica acompanha o total");
    }

    /**
     * O teto vem antes da divisão: sem isso o teto apagaria a diferença. Eclipse q5 rende 1440 (comum) e 1800 (com
     * piteira); 30% dá 432 e 540, os dois acima do teto leve de 160, então a náusea fica 160 e 80.
     */
    @Test
    void filterHalvesNauseaAfterTheCap() {
        Material plain = new Material(Variety.ECLIPSE, 5, false);
        Material filtered = new Material(Variety.ECLIPSE, 5, false, true);
        assertEquals(1440, SmokeRules.sessionTicks(SmokeDevice.JOINT, plain));
        assertEquals(1800, SmokeRules.sessionTicks(SmokeDevice.JOINT, filtered));
        assertEquals(160, find(SmokeRules.effects(SmokeDevice.JOINT, plain, 0), EffectKind.NAUSEA).orElseThrow().ticks());
        assertEquals(80, find(SmokeRules.effects(SmokeDevice.JOINT, filtered, 0), EffectKind.NAUSEA).orElseThrow().ticks());
        // Pesado (teto 400): 1800 × 0,3 = 540 → 400 → 200
        assertEquals(200, find(SmokeRules.effects(SmokeDevice.JOINT, filtered, SmokeRules.HEAVY_THRESHOLD_TICKS), EffectKind.NAUSEA)
            .orElseThrow().ticks());
    }

    /** A piteira só vale no baseado: cachimbo e bong ignoram a marca (duração e náusea iguais às do material comum). */
    @Test
    void filterIsIgnoredOutsideTheJoint() {
        for (SmokeDevice device : List.of(SmokeDevice.PIPE, SmokeDevice.BONG, SmokeDevice.CHILLUM, SmokeDevice.HOOKAH)) {
            Material plain = new Material(Variety.AURORA, 3, false);
            Material marked = new Material(Variety.AURORA, 3, false, true);
            assertFalse(SmokeRules.isFiltered(device, marked), device.name());
            assertEquals(SmokeRules.effects(device, plain, 0), SmokeRules.effects(device, marked, 0), device.name());
        }
        assertEquals(900, SmokeRules.sessionTicks(SmokeDevice.PIPE, new Material(Variety.BRISA, 2, false, true)));
        assertEquals(1440, SmokeRules.sessionTicks(SmokeDevice.BONG, new Material(Variety.BRISA, 2, false, true)));
        assertTrue(SmokeRules.isFiltered(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false, true)));
        assertFalse(SmokeRules.isFiltered(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false)));
    }

    /** O perfil da variedade acompanha a sessão mais longa: sativa com piteira dá Velocidade por metade de 1125. */
    @Test
    void filteredSessionDrivesTheProfile() {
        List<EffectSpec> sativa = SmokeRules.effects(SmokeDevice.JOINT, new Material(Variety.BRISA, 2, false, true), 0);
        assertEquals(563, find(sativa, EffectKind.SPEED).orElseThrow().ticks());
        List<EffectSpec> unknown = SmokeRules.effects(SmokeDevice.JOINT, new Material(null, 2, false, true), 0);
        assertEquals(338, find(unknown, EffectKind.SLOWNESS).orElseThrow().ticks());
    }

    @Test
    void heavyBoundaryIsExactlyTwoMinutes() {
        Material m = new Material(Variety.AURORA, 2, false);
        // Sessão de 900 ticks: 1499 + 900 = 2399 (nível I); 1500 + 900 = 2400 = 2 min (nível II)
        List<EffectSpec> below = SmokeRules.effects(SmokeDevice.JOINT, m, 1499);
        assertEquals(0, find(below, EffectKind.HIGH).orElseThrow().amplifier());
        List<EffectSpec> at = SmokeRules.effects(SmokeDevice.JOINT, m, 1500);
        assertEquals(1, find(at, EffectKind.HIGH).orElseThrow().amplifier());
    }
}
