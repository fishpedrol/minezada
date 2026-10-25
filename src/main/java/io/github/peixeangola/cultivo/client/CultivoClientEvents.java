package io.github.peixeangola.cultivo.client;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registros só do cliente. O Forge acha esta classe pela anotação e só a carrega com {@link Dist#CLIENT}; o código comum
 * nunca a cita, então um servidor dedicado nunca carrega nada de net.minecraft.client (ClientIsolationTest confere).
 */
@Mod.EventBusSubscriber(modid = Cultivo.MOD_ID, value = Dist.CLIENT)
public final class CultivoClientEvents {
    private CultivoClientEvents() {}

    /** As poses de braço de fumar precisam existir antes do primeiro modelo humanoide ser desenhado. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        SmokingItemClient.init();
    }

    /**
     * Placa da prensa animada (a BlockEntity das estações é uma só; o renderer só desenha a prensa trabalhando) e a
     * planta dentro da estufinha.
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PROCESSING_STATION.get(), PressPlatenRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GROW_TENT.get(), GrowTentRenderer::new);
    }

    /** Estado sintético cultivo:press_platen, para o jogo carregar e assar o modelo da placa (blockstates/press_platen.json). */
    @SubscribeEvent
    public static void onRegisterModelStates(ModelEvent.RegisterModelStateDefinitions event) {
        event.register(PressPlatenRenderer.PLATEN_STATES, PressPlatenRenderer.PLATEN_DEFINITION);
    }
}
