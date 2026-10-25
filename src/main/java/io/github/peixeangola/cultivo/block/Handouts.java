package io.github.peixeangola.cultivo.block;

import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** O que os blocos do Cultivo entregam na mão do jogador: a saída da estação, o pote, o dichavador e os baseados. */
final class Handouts {
    private Handouts() {}

    /**
     * Põe a pilha no inventário (completa pilhas iguais e depois os slots vazios) e derruba aos pés do jogador o que não
     * couber, também no criativo. Inventory#add não serve: com o jogador criativo e o inventário cheio, ele zera a pilha
     * e devolve true (Inventory.java do 26.3), e o que saiu do bloco sumiria.
     *
     * <p>O cliente já previu o clique (SUCCESS) e balançou o braço; por isso o derrubar usa PREDICTED: o servidor mostra
     * o balanço só para os outros jogadores, sem repetir para quem clicou. Sem o pacote de slot por slot: o inventário do
     * jogador sincroniza no fim do tick (AbstractContainerMenu#broadcastChanges), como no Inventory#add.
     */
    static void give(Player player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack, false, Prediction.PREDICTED);
    }
}
