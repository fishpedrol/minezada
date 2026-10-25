package io.github.peixeangola.cultivo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.peixeangola.cultivo.block.GrowTentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Planta dentro da estufinha: desenha o modelo da própria lavoura (a família de arte da variedade, no estágio da idade
 * sincronizada), sem arte nova. Só no cliente; a idade e a variedade chegam pela tag de atualização da BlockEntity.
 *
 * <p>O modelo da lavoura vai de y −1 a 15 (minecraft:block/crop): sobe 4 px para a base ficar dentro do vaso (topo em
 * y 4) e encolhe 1/8 na largura para as folhas não atravessarem as paredes. Madura, chega a y 19, 3 px dentro da
 * metade de cima (a caixa de desenho da BlockEntity cobre as duas metades).
 */
public final class GrowTentRenderer implements BlockEntityRenderer<GrowTentBlockEntity, GrowTentRenderer.State> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    /** Quanto a planta sobe (em blocos) e quanto encolhe na largura. */
    static final float LIFT = 4.0F / 16.0F;
    static final float WIDTH_SCALE = 0.875F;

    private final BlockModelResolver models;

    public GrowTentRenderer(BlockEntityRendererProvider.Context context) {
        this.models = context.blockModelResolver();
    }

    /** O que a estufinha desenha neste quadro: o modelo da lavoura, ou nada se estiver vazia. */
    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState plant = new BlockModelRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** Até a distância de renderização, como a placa da prensa: a planta não some antes da estufinha. */
    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public void extractRenderState(GrowTentBlockEntity blockEntity, State state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState plant = blockEntity.plantState();
        if (plant == null) {
            state.plant.clear();
            return;
        }
        models.update(state.plant, plant, DISPLAY_CONTEXT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.plant.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, LIFT, 0.5F);
        poseStack.scale(WIDTH_SCALE, 1.0F, WIDTH_SCALE);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        state.plant.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
