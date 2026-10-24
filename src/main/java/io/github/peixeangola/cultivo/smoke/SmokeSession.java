package io.github.peixeangola.cultivo.smoke;

import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules;
import io.github.peixeangola.cultivo.core.SmokeRules.EffectSpec;
import io.github.peixeangola.cultivo.core.SmokeRules.Material;
import io.github.peixeangola.cultivo.core.SmokeRules.Refusal;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModEffects;
import io.github.peixeangola.cultivo.registry.ModItems;
import io.github.peixeangola.cultivo.registry.ModTags;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Fumar no jogo, sempre decidido no servidor: confere isqueiro e material, consome o baseado ou 1 flor
 * seca/haxixe, gasta 1 de durabilidade do isqueiro, aplica os efeitos calculados por {@link SmokeRules} e solta
 * fumaça. O cliente só mostra a animação e as mensagens.
 */
public final class SmokeSession {
    private SmokeSession() {}

    /** Primeiro isqueiro do inventário (mãos incluídas), ou {@link ItemStack#EMPTY}. */
    public static ItemStack findLighter(Player player) {
        if (player.getOffhandItem().is(ModItems.LIGHTER.get())) {
            return player.getOffhandItem();
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.LIGHTER.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Uma flor seca (qualquer variedade) ou haxixe serve de material para cachimbo, chillum, bong e narguilé. */
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModTags.DRIED_FLOWERS) || stack.is(ModItems.HASH.get()));
    }

    /**
     * Primeiro material para fumar, ou {@link ItemStack#EMPTY}. A mão secundária vem primeiro, para o jogador
     * escolher qual flor ou haxixe quer usar.
     */
    public static ItemStack findFuel(Player player) {
        if (isFuel(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isFuel(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Quanto ainda resta do "chapado" atual, em ticks (0 se nenhum). */
    public static int remainingHigh(Player player) {
        MobEffectInstance current = player.getEffect(ModEffects.high());
        return current == null ? 0 : current.getDuration();
    }

    public static Refusal check(Player player, SmokeDevice device) {
        return SmokeRules.check(device, !findLighter(player).isEmpty(), !findFuel(player).isEmpty(), remainingHigh(player));
    }

    /** O que está sendo fumado: o próprio baseado ou o material do inventário. */
    public static Material materialOf(ItemStack smoked) {
        Integer quality = smoked.get(ModComponents.QUALITY.get());
        if (smoked.is(ModItems.HASH.get())) {
            return new Material(smoked.get(ModComponents.STRAIN.get()), quality, true);
        }
        Variety flower = ModItems.varietyOf(smoked);
        Variety variety = flower != null ? flower : smoked.get(ModComponents.STRAIN.get());
        return new Material(variety, quality, false);
    }

    static Holder<MobEffect> holder(SmokeRules.EffectKind kind) {
        return switch (kind) {
            case HIGH -> ModEffects.high();
            case HUNGER -> MobEffects.HUNGER;
            case NAUSEA -> MobEffects.NAUSEA;
            case SPEED -> MobEffects.SPEED;
            case SLOWNESS -> MobEffects.SLOWNESS;
            case REGENERATION -> MobEffects.REGENERATION;
        };
    }

    /**
     * Executa uma sessão. {@code held} é o item usado (baseado ou artigo). Revalida tudo (o jogador pode ter
     * largado o isqueiro durante a animação). Retorna os efeitos aplicados, ou vazio se não fumou.
     */
    public static Optional<List<EffectSpec>> smoke(ServerLevel level, Player player, ItemStack held, SmokeDevice device) {
        ItemStack lighter = findLighter(player);
        ItemStack fuel = device.needsFuel() ? findFuel(player) : ItemStack.EMPTY;
        int remaining = remainingHigh(player);
        if (held.isEmpty() || SmokeRules.check(device, !lighter.isEmpty(), !fuel.isEmpty(), remaining) != Refusal.NONE) {
            return Optional.empty();
        }
        ItemStack smoked = device.needsFuel() ? fuel : held;
        Material material = materialOf(smoked);

        List<EffectSpec> effects = SmokeRules.effects(device, material, remaining);
        for (EffectSpec spec : effects) {
            MobEffectInstance instance = new MobEffectInstance(holder(spec.kind()), spec.ticks(), spec.amplifier());
            if (spec.kind() == SmokeRules.EffectKind.HIGH) {
                // O total já inclui o que restava: substitui a instância inteira. Com addEffect, um nível I por
                // cima de um nível II viraria "efeito escondido" do vanilla e as sessões seguintes não somariam.
                player.forceAddEffect(instance, null);
                continue;
            }
            SmokeRules.EffectKind opposite = SmokeRules.opposite(spec.kind());
            if (opposite != null) {
                player.removeEffect(holder(opposite));
            }
            player.addEffect(instance);
        }

        if (!player.hasInfiniteMaterials()) {
            smoked.shrink(1);
            damageLighter(level, player, lighter);
        }

        Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
        level.sendParticles(ParticleTypes.WHITE_SMOKE, mouth.x, mouth.y - 0.1D, mouth.z, 6 + effects.size(),
            0.12D, 0.08D, 0.12D, 0.01D);
        level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.15F, 1.6F);
        return Optional.of(effects);
    }

    /**
     * Gasta 1 de durabilidade. Na mão, usa o caminho vanilla (som, partículas, estatística e evento do Forge ao
     * quebrar); em outro slot do inventário, faz o mesmo à mão. Avisa o jogador quando o isqueiro quebra.
     */
    static void damageLighter(ServerLevel level, Player player, ItemStack lighter) {
        if (lighter == player.getOffhandItem()) {
            lighter.hurtAndBreak(SmokeRules.LIGHTER_DAMAGE_PER_USE, player, EquipmentSlot.OFFHAND);
        } else if (lighter == player.getMainHandItem()) {
            lighter.hurtAndBreak(SmokeRules.LIGHTER_DAMAGE_PER_USE, player, EquipmentSlot.MAINHAND);
        } else {
            lighter.hurtAndBreak(SmokeRules.LIGHTER_DAMAGE_PER_USE, level,
                player instanceof ServerPlayer serverPlayer ? serverPlayer : null, broken -> {
                    Holder<SoundEvent> sound = broken.get(DataComponents.BREAK_SOUND);
                    if (sound != null) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.value(),
                            SoundSource.PLAYERS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                    }
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(broken)),
                        player.getX(), player.getEyeY() - 0.3D, player.getZ(), 5, 0.1D, 0.1D, 0.1D, 0.05D);
                    player.awardStat(Stats.ITEM_BROKEN.get(broken.getItem()));
                    ForgeEventFactory.onPlayerDestroyItem(player, broken, (EquipmentSlot) null);
                });
        }
        if (lighter.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.cultivo.lighter_broke"));
        }
    }
}
