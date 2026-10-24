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
            assertEquals(Refusal.NEEDS_LIGHTER, SmokeRules.check(device, false, true), device.name());
        }
    }

    @Test
    void onlyJointSkipsFuel() {
        assertEquals(Refusal.NONE, SmokeRules.check(SmokeDevice.JOINT, true, false));
        for (SmokeDevice device : List.of(SmokeDevice.PIPE, SmokeDevice.CHILLUM, SmokeDevice.BONG, SmokeDevice.HOOKAH)) {
            assertTrue(device.needsFuel());
            assertEquals(Refusal.NEEDS_FUEL, SmokeRules.check(device, true, false), device.name());
            assertEquals(Refusal.NONE, SmokeRules.check(device, true, true), device.name());
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
    void greenhouseQualityBeatsOutdoor() {
        // Estufa dá 4–5 e outdoor 2–3: a pior estufa ainda rende mais que o melhor outdoor
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
}
