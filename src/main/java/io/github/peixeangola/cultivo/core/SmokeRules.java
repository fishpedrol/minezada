package io.github.peixeangola.cultivo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Efeitos fictícios de fumar no jogo, calculados no servidor. Classe pura: sem Minecraft, testada por JUnit.
 *
 * <p>Duração base do efeito "chapado": {@link #BASE_SECONDS} s × força do artigo × fator de qualidade
 * (0,6 + 0,2 × qualidade, ou seja 1,0 na qualidade 2 e 1,6 na 5) × {@link #HASH_MULTIPLIER} se for haxixe.
 * Fumar de novo soma ao tempo que ainda resta, até {@link #MAX_HIGH_TICKS}; perto do limite o jogo recusa
 * ({@link Refusal#AT_MAX}) para não gastar itens à toa. Acima de {@link #HEAVY_THRESHOLD_TICKS} o efeito fica
 * "forte" (nível II) e a náusea pode durar mais. Fome acompanha o tempo total; náusea e o perfil da variedade
 * duram só em função da sessão atual (não do acumulado).
 *
 * <p>Perfis (só gameplay): sativa dá Velocidade, híbrida dá Regeneração curta, índica dá Lentidão + Regeneração,
 * e sem variedade conhecida (ex.: haxixe) dá Lentidão leve. Todos dão Fome ("larica") e um pouco de Náusea.
 */
public final class SmokeRules {
    public static final int TICKS_PER_SECOND = 20;
    public static final int BASE_SECONDS = 45;
    public static final float HASH_MULTIPLIER = 1.5F;
    public static final int DEFAULT_QUALITY = 2;
    public static final int MAX_HIGH_TICKS = 6 * 60 * TICKS_PER_SECOND;
    public static final int HEAVY_THRESHOLD_TICKS = 2 * 60 * TICKS_PER_SECOND;
    public static final int NAUSEA_LIGHT_MAX_TICKS = 8 * TICKS_PER_SECOND;
    public static final int NAUSEA_HEAVY_MAX_TICKS = 20 * TICKS_PER_SECOND;
    public static final int REGENERATION_TICKS = 5 * TICKS_PER_SECOND;
    /** Com menos que isto de espaço até o limite, fumar é recusado (nada seria ganho). */
    public static final int AT_MAX_MARGIN_TICKS = 30 * TICKS_PER_SECOND;
    public static final int LIGHTER_DAMAGE_PER_USE = 1;
    public static final int LIGHTER_DURABILITY = 64;

    /** Efeitos usados; o Java traduz para MobEffects (HIGH é o efeito próprio cultivo:high). */
    public enum EffectKind { HIGH, HUNGER, NAUSEA, SPEED, SLOWNESS, REGENERATION }

    /** Um efeito a aplicar: duração em ticks e nível (0 = I, 1 = II). */
    public record EffectSpec(EffectKind kind, int ticks, int amplifier) {}

    /**
     * O que o jogador fuma: variedade (null quando não se sabe, ex.: haxixe sem variedade), qualidade
     * (null = {@link #DEFAULT_QUALITY}) e se é haxixe.
     */
    public record Material(Variety variety, Integer quality, boolean hash) {}

    /** Motivo para não deixar fumar, com a chave da mensagem mostrada ao jogador. */
    public enum Refusal {
        NONE(null),
        NEEDS_LIGHTER("message.cultivo.need_lighter"),
        NEEDS_FUEL("message.cultivo.need_fuel"),
        AT_MAX("message.cultivo.too_high");

        private final String messageKey;

        Refusal(String messageKey) {
            this.messageKey = messageKey;
        }

        /** Chave de tradução da mensagem (null para NONE). */
        public String messageKey() {
            return messageKey;
        }
    }

    private SmokeRules() {}

    /**
     * Ordem das recusas: isqueiro, depois material, depois limite.
     *
     * @param remainingHighTicks quanto ainda resta do "chapado" atual (0 se nenhum)
     */
    public static Refusal check(SmokeDevice device, boolean hasLighter, boolean hasFuel, int remainingHighTicks) {
        if (!hasLighter) {
            return Refusal.NEEDS_LIGHTER;
        }
        if (device.needsFuel() && !hasFuel) {
            return Refusal.NEEDS_FUEL;
        }
        if (remainingHighTicks > MAX_HIGH_TICKS - AT_MAX_MARGIN_TICKS) {
            return Refusal.AT_MAX;
        }
        return Refusal.NONE;
    }

    /** Velocidade e Lentidão se anulam: aplicar uma remove a outra. Retorna o oposto, ou null. */
    public static EffectKind opposite(EffectKind kind) {
        return switch (kind) {
            case SPEED -> EffectKind.SLOWNESS;
            case SLOWNESS -> EffectKind.SPEED;
            default -> null;
        };
    }

    public static int effectiveQuality(Integer quality) {
        return QualityRules.clamp(quality == null ? DEFAULT_QUALITY : quality);
    }

    public static float qualityFactor(Integer quality) {
        return 0.6F + 0.2F * effectiveQuality(quality);
    }

    /** Duração desta sessão (sem somar o que já restava). */
    public static int sessionTicks(SmokeDevice device, Material material) {
        float seconds = BASE_SECONDS * device.strength() * qualityFactor(material.quality());
        if (material.hash()) {
            seconds *= HASH_MULTIPLIER;
        }
        return Math.round(seconds * TICKS_PER_SECOND);
    }

    /** Duração total do "chapado" depois de fumar, somando o que ainda restava e limitada ao máximo. */
    public static int totalHighTicks(int remainingHighTicks, int sessionTicks) {
        return Math.min(MAX_HIGH_TICKS, Math.max(0, remainingHighTicks) + sessionTicks);
    }

    public static boolean isHeavy(int totalHighTicks) {
        return totalHighTicks >= HEAVY_THRESHOLD_TICKS;
    }

    /** Lista de efeitos a aplicar. {@code remainingHighTicks} é quanto ainda resta do efeito anterior (0 se nenhum). */
    public static List<EffectSpec> effects(SmokeDevice device, Material material, int remainingHighTicks) {
        int session = sessionTicks(device, material);
        int total = totalHighTicks(remainingHighTicks, session);
        boolean heavy = isHeavy(total);
        List<EffectSpec> out = new ArrayList<>();
        out.add(new EffectSpec(EffectKind.HIGH, total, heavy ? 1 : 0));
        out.add(new EffectSpec(EffectKind.HUNGER, total, 0));
        int nauseaCap = heavy ? NAUSEA_HEAVY_MAX_TICKS : NAUSEA_LIGHT_MAX_TICKS;
        out.add(new EffectSpec(EffectKind.NAUSEA, Math.min(nauseaCap, Math.round(session * 0.3F)), 0));
        Variety.Lineage lineage = material.variety() == null ? null : material.variety().lineage();
        if (lineage == null) {
            out.add(new EffectSpec(EffectKind.SLOWNESS, Math.round(session * 0.3F), 0));
        } else {
            switch (lineage) {
                case SATIVA -> out.add(new EffectSpec(EffectKind.SPEED, Math.round(session * 0.5F), 0));
                case HYBRID -> out.add(new EffectSpec(EffectKind.REGENERATION, REGENERATION_TICKS, 0));
                case INDICA -> {
                    out.add(new EffectSpec(EffectKind.SLOWNESS, Math.round(session * 0.5F), 0));
                    out.add(new EffectSpec(EffectKind.REGENERATION, REGENERATION_TICKS * 2, 0));
                }
            }
        }
        return List.copyOf(out);
    }
}
