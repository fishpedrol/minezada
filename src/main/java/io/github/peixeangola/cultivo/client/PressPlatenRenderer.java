package io.github.peixeangola.cultivo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.ProcessingStationBlock;
import io.github.peixeangola.cultivo.block.ProcessingStationBlockEntity;
import io.github.peixeangola.cultivo.block.StationContents;
import io.github.peixeangola.cultivo.core.StationAnimation;
import io.github.peixeangola.cultivo.core.StationKind;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Placa da prensa manual subindo e descendo enquanto ela trabalha (contents=working). Só no cliente e sem pacote nenhum:
 * a posição sai do tempo do mundo ({@link StationAnimation#pressOffsetPixels}), o mesmo relógio que o servidor usa para
 * tocar o som do aperto quando a placa chega embaixo.
 *
 * <p>O modelo da placa (models/block/press_platen_moving.json) é carregado como o estado sintético
 * {@code cultivo:press_platen} (só facing), do mesmo jeito que o vanilla carrega a moldura de item: o Forge deixa
 * registrar a StateDefinition em {@code ModelEvent.RegisterModelStateDefinitions} e o blockstates/press_platen.json gera
 * um modelo por facing. Com a prensa vazia ou pronta, a placa parada continua no modelo do bloco (platen_up); trabalhando,
 * o modelo do bloco só tem a resina e a placa vem daqui.
 */
public final class PressPlatenRenderer implements BlockEntityRenderer<ProcessingStationBlockEntity, PressPlatenRenderer.State> {
    /** Nome do estado sintético (blockstates/press_platen.json). */
    public static final Identifier PLATEN_STATES = Cultivo.id("press_platen");
    /** Estados sintéticos da placa: o dono é o ar, como a moldura de item vanilla; só existe o facing. */
    public static final StateDefinition<Block, BlockState> PLATEN_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
        .add(HorizontalDirectionalBlock.FACING)
        .create(Block::defaultBlockState, BlockState::new);
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver models;

    public PressPlatenRenderer(BlockEntityRendererProvider.Context context) {
        this.models = context.blockModelResolver();
    }

    /** O que a prensa desenha neste quadro: a placa (modelo do facing) e quanto ela desceu, em blocos. */
    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState platen = new BlockModelRenderState();
        boolean moving;
        float offset;
    }

    /** Estado sintético da placa para um facing da prensa. */
    static BlockState platenState(Direction facing) {
        return PLATEN_DEFINITION.any().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProcessingStationBlockEntity blockEntity, State state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.moving = blockState.getBlock() instanceof ProcessingStationBlock station
            && station.kind() == StationKind.PRESSING
            && blockState.getValue(ProcessingStationBlock.CONTENTS) == StationContents.WORKING;
        if (!state.moving) {
            state.platen.clear();
            return;
        }
        Level level = blockEntity.getLevel();
        double time = (level != null ? level.getGameTime() : 0L) + partialTicks;
        state.offset = StationAnimation.pressOffsetPixels(time, blockEntity.getBlockPos().asLong()) / 16.0F;
        models.update(state.platen, platenState(blockState.getValue(ProcessingStationBlock.FACING)), DISPLAY_CONTEXT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.moving || state.platen.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, state.offset, 0.0F);
        state.platen.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
