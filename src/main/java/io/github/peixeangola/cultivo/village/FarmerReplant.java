package io.github.peixeangola.cultivo.village;

import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.VanillaGameEvent;

/**
 * Fazendeiros de aldeia e as lavouras do mod. O comportamento vanilla HarvestFarmland quebra qualquer CropBlock
 * maduro ({@code level.destroyBlock(pos, true, villager)}) e só replanta sementes da tag
 * minecraft:villager_plantable_seeds, então as plantações do mod nas fazendas de planície virariam trigo depois
 * da primeira colheita. Aqui, quando um aldeão quebra uma lavoura do mod madura, a mesma variedade é replantada
 * na idade 0, como o fazendeiro faz com o trigo: a plantação continua sendo uma fonte de sementes para o jogador
 * encontrar. A flor e as sementes da colheita caem no chão normalmente (aldeões não as recolhem).
 * Roda só no servidor, pelo evento de jogo BLOCK_DESTROY que o próprio destroyBlock emite.
 */
public final class FarmerReplant {
    private FarmerReplant() {}

    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent() != GameEvent.BLOCK_DESTROY.value()
            || !(event.getLevel() instanceof ServerLevel level)
            || !(event.getCause() instanceof Villager)) {
            return;
        }
        BlockState destroyed = event.getContext().affectedState();
        if (destroyed == null || !(destroyed.getBlock() instanceof CannabisCropBlock crop) || !crop.isMaxAge(destroyed)) {
            return;
        }
        BlockPos pos = BlockPos.containing(event.getEventPosition());
        BlockState seedling = crop.getStateForAge(0);
        if (level.getBlockState(pos).isAir() && seedling.canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, seedling);
        }
    }
}
