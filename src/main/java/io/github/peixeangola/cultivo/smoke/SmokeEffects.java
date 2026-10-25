package io.github.peixeangola.cultivo.smoke;

import io.github.peixeangola.cultivo.core.SmokeAnimation;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Enfeites de fumar: partículas e sons, sem efeito no jogo.
 * <ul>
 *   <li>Durante o uso, as partículas nascem no cliente de cada jogador que vê quem fuma: Item#onUseTick roda no cliente
 *       para toda entidade usando item, inclusive os outros jogadores. Brasa e fios de fumaça na ponta do baseado, no
 *       fornilho do cachimbo e no chillum; bolhas na câmara do bong; baforadas no carvão do narguilé; e a chama do
 *       isqueiro no primeiro tick.</li>
 *   <li>Os sons e a baforada final saem do servidor ({@code playSound(null, ...)} e sendParticles), então todo mundo
 *       por perto ouve e vê: isqueiro ao começar, tragada quando o aparelho chega à boca, borbulhar do bong e do
 *       narguilé, e a fumaça soprada no fim de uma sessão que deu certo.</li>
 * </ul>
 * As posições são aproximações à frente da boca, pelo olhar da entidade e pelo lado da mão que segura.
 */
public final class SmokeEffects {
    /** Brasa: ponto laranja pequeno que sobe devagar. */
    private static final ParticleOptions EMBER = new DustParticleOptions(0xFF8A2A, 0.45F);
    /** De quantos em quantos ticks o bong e o narguilé borbulham (som), depois de chegar à boca. */
    public static final int BUBBLE_SOUND_TICKS = 14;

    private SmokeEffects() {}

    /** Lado da mão que segura o aparelho: +1 direita, -1 esquerda. */
    private static double handSide(LivingEntity user) {
        HumanoidArm arm = user.getUsedItemHand() == InteractionHand.MAIN_HAND ? user.getMainArm() : user.getMainArm().getOpposite();
        return arm == HumanoidArm.RIGHT ? 1.0D : -1.0D;
    }

    /**
     * Ponto aceso de cada aparelho, um pouco à frente da boca: a ponta do baseado, o fornilho do cachimbo, a boca larga do
     * chillum, a câmara de água do bong e o carvão do narguilé.
     */
    static Vec3 glowPoint(LivingEntity user, SmokeDevice device) {
        Vec3 look = user.getLookAngle();
        Vec3 side = Entity.calculateViewVector(0.0F, user.getYRot() + 90.0F).scale(handSide(user));
        Vec3 mouth = user.getEyePosition().add(look.scale(0.22D)).add(0.0D, -0.12D, 0.0D);
        return switch (device) {
            case JOINT -> mouth.add(look.scale(0.42D)).add(side.scale(0.06D)).add(0.0D, 0.02D, 0.0D);
            case PIPE -> mouth.add(look.scale(0.35D)).add(side.scale(0.04D)).add(0.0D, 0.08D, 0.0D);
            case CHILLUM -> mouth.add(look.scale(0.3D)).add(0.0D, 0.22D, 0.0D);
            case BONG -> mouth.add(look.scale(0.3D)).add(side.scale(0.04D)).add(0.0D, -0.32D, 0.0D);
            case HOOKAH -> mouth.add(look.scale(0.35D)).add(0.0D, -0.25D, 0.0D);
        };
    }

    /**
     * Cliente: partículas de um tick de uso. {@code elapsed} é quantos ticks o jogador já segura o botão. A chama do
     * isqueiro aparece no tick 1; o resto só enquanto o aparelho está na boca.
     */
    public static void useTickParticles(Level level, LivingEntity user, SmokeDevice device, int elapsed) {
        if (!level.isClientSide()) {
            return;
        }
        RandomSource random = user.getRandom();
        Vec3 at = glowPoint(user, device);
        if (elapsed == 1) {
            level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 0.0D, 0.01D, 0.0D);
        }
        if (SmokeAnimation.thirdPersonRaise(elapsed, device.useTicks()) < 1.0F) {
            return;
        }
        double jx = (random.nextDouble() - 0.5D) * 0.06D;
        double jz = (random.nextDouble() - 0.5D) * 0.06D;
        switch (device) {
            case JOINT, PIPE, CHILLUM -> {
                if (elapsed % 2 == 0) {
                    level.addParticle(EMBER, at.x + jx, at.y, at.z + jz, 0.0D, 0.01D, 0.0D);
                }
                if (elapsed % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, at.x, at.y + 0.04D, at.z, jx * 0.1D, 0.025D, jz * 0.1D);
                }
            }
            case BONG -> {
                if (random.nextBoolean()) {
                    level.addParticle(ParticleTypes.BUBBLE_POP, at.x + jx, at.y + random.nextDouble() * 0.12D, at.z + jz,
                        0.0D, 0.02D, 0.0D);
                }
                if (elapsed % 4 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, at.x, at.y + 0.18D, at.z, 0.0D, 0.03D, 0.0D);
                }
            }
            case HOOKAH -> {
                if (elapsed % 4 == 0) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, at.x + jx, at.y + 0.1D, at.z + jz, 0.0D, 0.02D, 0.0D);
                }
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.BUBBLE_POP, at.x + jx, at.y - 0.12D, at.z + jz, 0.0D, 0.015D, 0.0D);
                }
            }
        }
    }

    /** Servidor: o isqueiro acende quando o uso começa. */
    public static void startSounds(ServerLevel level, LivingEntity user) {
        play(level, user, ModSounds.LIGHTER_CLICK.get(), 1.0F, 0.9F + user.getRandom().nextFloat() * 0.2F);
    }

    /**
     * Servidor: um tick de uso. A tragada toca quando o aparelho chega à boca; bong e narguilé borbulham a cada
     * {@link #BUBBLE_SOUND_TICKS} ticks depois disso (o narguilé mais grave).
     */
    public static void useTickSounds(ServerLevel level, LivingEntity user, SmokeDevice device, int elapsed) {
        if (elapsed == SmokeAnimation.KEYFRAMES) {
            play(level, user, ModSounds.SMOKE_INHALE.get(), 1.0F, 0.9F + user.getRandom().nextFloat() * 0.2F);
        }
        boolean bubbles = device == SmokeDevice.BONG || device == SmokeDevice.HOOKAH;
        if (bubbles && elapsed > SmokeAnimation.KEYFRAMES
            && (elapsed - SmokeAnimation.KEYFRAMES) % BUBBLE_SOUND_TICKS == 2
            && SmokeAnimation.thirdPersonRaise(elapsed, device.useTicks()) >= 1.0F) {
            play(level, user, ModSounds.BONG_BUBBLE.get(), 1.0F, device == SmokeDevice.HOOKAH ? 0.75F : 1.0F);
        }
    }

    /**
     * Servidor: baforada no fim de uma sessão que deu certo. {@code puffs} nuvens de fumaça branca saem da boca na
     * direção do olhar, mais uma lufada espalhada, e toca o som de soprar.
     */
    public static void exhale(ServerLevel level, LivingEntity user, int puffs) {
        RandomSource random = user.getRandom();
        Vec3 look = user.getLookAngle();
        Vec3 mouth = user.getEyePosition().add(look.scale(0.3D)).add(0.0D, -0.12D, 0.0D);
        for (int i = 0; i < puffs; i++) {
            double spread = 0.25D;
            // Com contagem 0 o pacote manda a velocidade exata: (dx, dy, dz) × speed
            level.sendParticles(ParticleTypes.WHITE_SMOKE, mouth.x, mouth.y, mouth.z, 0,
                look.x + random.nextGaussian() * spread, look.y + 0.15D + random.nextGaussian() * spread,
                look.z + random.nextGaussian() * spread, 0.06D);
        }
        level.sendParticles(ParticleTypes.CLOUD, mouth.x + look.x * 0.3D, mouth.y + look.y * 0.3D, mouth.z + look.z * 0.3D,
            2, 0.08D, 0.05D, 0.08D, 0.01D);
        play(level, user, ModSounds.SMOKE_EXHALE.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    private static void play(ServerLevel level, LivingEntity user, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, user.getX(), user.getEyeY(), user.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
