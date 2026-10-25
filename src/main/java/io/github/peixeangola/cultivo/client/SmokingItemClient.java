package io.github.peixeangola.cultivo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.peixeangola.cultivo.core.SmokeAnimation;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.item.SmokingItem;
import io.github.peixeangola.cultivo.smoke.SmokeEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/**
 * Animação de fumar no cliente (este pacote nunca é carregado num servidor dedicado). Os números vêm de
 * {@link SmokeAnimation}, que é puro e testado.
 * <ul>
 *   <li><b>3ª pessoa:</b> enquanto o jogador fuma, o braço usa a pose {@link #ONE_HAND} ou {@link #TWO_HANDS} (bong e
 *       narguilé com a outra mão vazia), criadas como extensões de {@link HumanoidModel.ArmPose} do Forge. O braço sobe
 *       um passo por tick junto com os modelos k0..k4 do item e respira na boca.</li>
 *   <li><b>1ª pessoa:</b> {@link #applyForgeHandTransform} troca a transformação vanilla da mão pela subida suave até a
 *       boca, com respiração e inclinação de tragada ({@link SmokeAnimation#firstPersonMatrix}). O modelo aceso usa a
 *       exibição de quem segura, então não há salto quando o uso começa.</li>
 * </ul>
 */
public final class SmokingItemClient implements IClientItemExtensions {
    /** Pose de braço com o aparelho numa mão (baseado, cachimbo, chillum; bong e narguilé com a outra mão ocupada). */
    public static final HumanoidModel.ArmPose ONE_HAND =
        HumanoidModel.ArmPose.create("CULTIVO_SMOKE", false, false, SmokingItemClient::poseOneHand);
    /** Bong e narguilé com as duas mãos: a outra mão apoia embaixo (a pose cuida dos dois braços). */
    public static final HumanoidModel.ArmPose TWO_HANDS =
        HumanoidModel.ArmPose.create("CULTIVO_SMOKE_TWO_HANDS", true, true, SmokingItemClient::poseTwoHands);

    /**
     * Instância única usada por todos os aparelhos ({@link SmokingItem#initializeClient}). Criá-la carrega esta classe e
     * as poses acima já no registro dos itens, antes de qualquer modelo humanoide ser desenhado (o switch das poses do
     * vanilla guarda o tamanho do enum na primeira vez que roda).
     */
    public static final SmokingItemClient EXTENSIONS = new SmokingItemClient();

    private SmokingItemClient() {}

    /**
     * Garante que as poses existem antes do primeiro quadro (chamado no FMLClientSetupEvent, por garantia): chamar um
     * método estático já roda o inicializador da classe, que cria as poses; se o registro dos itens já fez isso, nada
     * acontece.
     */
    static void init() {
        // O inicializador estático desta classe cria as poses; aqui só falta avisar quem é a câmera em 1ª pessoa
        SmokeEffects.setFirstPersonViewer(entity -> {
            Minecraft minecraft = Minecraft.getInstance();
            return entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
        });
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof SmokingItem item) || !entity.isUsingItem() || entity.getUsedItemHand() != hand) {
            return HumanoidModel.ArmPose.ITEM;
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return SmokeAnimation.twoHanded(item.device()) && entity.getItemInHand(other).isEmpty() ? TWO_HANDS : ONE_HAND;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!(itemInHand.getItem() instanceof SmokingItem item) || !player.isUsingItem()) {
            return false;
        }
        InteractionHand used = player.getUsedItemHand();
        HumanoidArm usedArm = used == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        if (usedArm != arm) {
            return false;
        }
        SmokeDevice device = item.device();
        boolean left = arm == HumanoidArm.LEFT;
        // A mesma translação base do vanilla (applyItemArmTransform), com a subida do equipamento
        poseStack.translate((left ? -1.0F : 1.0F) * SmokeAnimation.HAND_X, SmokeAnimation.HAND_Y + equipProcess * -0.6F,
            SmokeAnimation.HAND_Z);
        float ticks = player.getTicksUsingItem(partialTick);
        poseStack.mulPose(new Matrix4f().set(SmokeAnimation.firstPersonMatrix(device, ticks, device.useTicks(), left)));
        return true;
    }

    private static void poseOneHand(HumanoidModel<? extends HumanoidRenderState> model, HumanoidRenderState state,
                                    HumanoidArm arm) {
        pose(model, state, arm, false);
    }

    private static void poseTwoHands(HumanoidModel<? extends HumanoidRenderState> model, HumanoidRenderState state,
                                     HumanoidArm arm) {
        pose(model, state, arm, true);
    }

    /**
     * Ergue o braço {@code arm} (o que segura) do jeito de segurar um item até a boca, um passo por tick
     * ({@link SmokeAnimation#thirdPersonRaise}), e, com as duas mãos, ergue o outro braço até a pose de apoio.
     */
    private static void pose(HumanoidModel<? extends HumanoidRenderState> model, HumanoidRenderState state, HumanoidArm arm,
                             boolean twoHands) {
        ModelPart holding = model.getArm(arm);
        ItemStack stack = state.getUseItemStackForArm(arm);
        if (!(stack.getItem() instanceof SmokingItem item)) {
            // Não deveria acontecer (a pose só é pedida para aparelhos em uso): fica como um item comum
            holding.xRot = holding.xRot * 0.5F + SmokeAnimation.ITEM_ARM_X;
            holding.yRot = 0.0F;
            return;
        }
        SmokeAnimation.Pose pose = SmokeAnimation.pose(item.device());
        float ticks = state.ticksUsingItem(arm);
        float raise = SmokeAnimation.thirdPersonRaise((int) Math.floor(ticks), item.device().useTicks());
        float breath = SmokeAnimation.breath(ticks);
        boolean mirrored = arm == HumanoidArm.LEFT;
        ModelPart head = model.getHead();
        float[] angles = SmokeAnimation.armAngles(pose.raised(), raise, holding.xRot * 0.5F + SmokeAnimation.ITEM_ARM_X,
            head.xRot, head.yRot, breath, mirrored);
        holding.xRot = angles[0];
        holding.yRot = angles[1];
        if (twoHands && pose.support() != null) {
            ModelPart other = model.getArm(arm.getOpposite());
            float[] support = SmokeAnimation.armAngles(pose.support(), raise, other.xRot, head.xRot, head.yRot, breath,
                mirrored);
            other.xRot = support[0];
            other.yRot = support[1];
        }
    }
}
