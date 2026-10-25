package io.github.peixeangola.cultivo.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Animação de fumar, só visual: as regras de jogo não dependem disto. O cliente usa estes números para mover o aparelho
 * e o braço, e o gerador (seção smoke_animation do manifesto) usa os mesmos números para os modelos da 3ª pessoa. Puro,
 * sem Minecraft.
 * <ul>
 *   <li><b>1ª pessoa</b> ({@link #firstPersonMatrix}): o aparelho sai do jeito de segurar e chega à boca em
 *       {@link #FP_RAISE_TICKS} ticks com aceleração suave, fica lá com um leve balanço de respiração e uma inclinação
 *       de tragada e volta nos últimos {@link #FP_LOWER_TICKS} ticks do uso.</li>
 *   <li><b>3ª pessoa</b> ({@link #thirdPersonRaise}, {@link #armAngles}): o braço sobe e desce em {@link #KEYFRAMES}
 *       passos de 1 tick, no ritmo dos modelos &lt;aparelho&gt;_in_hand_lit_k0..k4 (range_dispatch em
 *       minecraft:use_duration), para braço e item andarem juntos; no meio fica na pose da boca, respirando. Bong e
 *       narguilé, com a outra mão vazia, usam as duas mãos ({@link Pose#support()}).</li>
 * </ul>
 * Ângulos de braço em radianos (ModelPart xRot/yRot); exibições em graus e 1/16 de bloco, como nos JSON de modelo.
 */
public final class SmokeAnimation {
    /** Quadros da subida (e da descida) na 3ª pessoa, um por tick: k0..k4 (manifesto smoke_animation.keyframes). */
    public static final int KEYFRAMES = 5;
    /** Ticks da subida suave na 1ª pessoa. */
    public static final float FP_RAISE_TICKS = 6.0F;
    /** Ticks da descida suave no fim do uso, na 1ª pessoa. */
    public static final float FP_LOWER_TICKS = 5.0F;
    /** Período da respiração (ticks): o aparelho e o braço sobem e descem de leve enquanto o jogador traga. */
    public static final float BREATH_PERIOD_TICKS = 16.0F;
    /** Amplitude do balanço da respiração na 1ª pessoa (blocos). */
    public static final float BOB_BLOCKS = 0.012F;
    /** Amplitude da inclinação da respiração na 1ª pessoa (graus). */
    public static final float BOB_PITCH_DEGREES = 1.5F;
    /** Inclinação extra do aparelho para a boca ao longo da tragada, na 1ª pessoa (graus no fim da tragada). */
    public static final float INHALE_TILT_DEGREES = 4.0F;
    /** Amplitude da respiração no braço da 3ª pessoa (radianos). */
    public static final float ARM_BREATH_RADIANS = 0.04F;
    /** Quanto do olhar para cima/baixo o braço acompanha na 3ª pessoa (a corneta vanilla acompanha tudo). */
    public static final float HEAD_PITCH_FOLLOW = 0.5F;
    /** Limite do olhar acompanhado (radianos), para o braço não atravessar a cabeça olhando para cima. */
    public static final float MAX_HEAD_PITCH = 0.8F;
    /** Inclinação que o vanilla soma ao braço que segura um item comum (HumanoidModel, pose ITEM: -π/10). */
    public static final float ITEM_ARM_X = (float) (-Math.PI / 10.0D);
    /** Translação base da mão na 1ª pessoa (FirstPersonHandsAndItemsRenderer#applyItemArmTransform), em blocos. */
    public static final float HAND_X = 0.56F;
    public static final float HAND_Y = -0.52F;
    public static final float HAND_Z = -0.72F;

    /** Transformação de exibição como no JSON do modelo: rotação XYZ (graus), translação (1/16 de bloco), escala. */
    public record Display(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {}

    /** Ângulos de um braço (radianos), com a cabeça reta. */
    public record Arm(float x, float y) {}

    /**
     * Pose de um aparelho: como fica na mão na 1ª pessoa (espelho do firstperson_righthand de
     * models/item/&lt;aparelho&gt;_in_hand.json) e na boca; o braço que segura na boca e, para bong e narguilé, o braço
     * que apoia (null se o aparelho vai numa mão só).
     */
    public record Pose(Display idleFirstPerson, Display mouthFirstPerson, Arm raised, Arm support) {}

    private static final Map<SmokeDevice, Pose> POSES = new EnumMap<>(SmokeDevice.class);

    static {
        Arm oneHand = new Arm(-1.25F, -0.7F);
        POSES.put(SmokeDevice.JOINT, new Pose(
            new Display(0, 30, 40, 0, 5, 0, 0.6F), new Display(25, 65, 0, -8, 3, 2.5F, 0.6F), oneHand, null));
        POSES.put(SmokeDevice.PIPE, new Pose(
            new Display(10, -60, 0, -1, 6, -2, 0.65F), new Display(15, -90, 0, -9, 2.6F, 3, 0.65F), oneHand, null));
        POSES.put(SmokeDevice.CHILLUM, new Pose(
            new Display(0, -20, -20, 0, 5, -1, 0.6F), new Display(-55, 0, 0, -9, 3.2F, 2.5F, 0.6F), oneHand, null));
        POSES.put(SmokeDevice.BONG, new Pose(
            new Display(0, -25, 5, -1, 5, -2, 0.55F), new Display(15, 0, 0, -9, 1.5F, -1, 0.55F),
            new Arm(-1.3F, -0.6F), new Arm(-0.75F, 0.35F)));
        POSES.put(SmokeDevice.HOOKAH, new Pose(
            new Display(0, -25, 5, -1, 5, -2, 0.55F), new Display(15, 0, 0, -9, 1.5F, -1, 0.55F),
            new Arm(-0.95F, -0.5F), new Arm(-0.8F, 0.45F)));
    }

    private SmokeAnimation() {}

    public static Pose pose(SmokeDevice device) {
        return POSES.get(device);
    }

    /** O aparelho é segurado com as duas mãos na 3ª pessoa (se a outra mão estiver vazia). */
    public static boolean twoHanded(SmokeDevice device) {
        return POSES.get(device).support() != null;
    }

    /**
     * Quanto o braço da 3ª pessoa está erguido (0 segurando, 1 na boca), em passos de 1/{@link #KEYFRAMES} por tick:
     * sobe nos primeiros KEYFRAMES ticks, desce nos últimos e fica em 1 no meio. É a mesma escolha do range_dispatch
     * dos modelos do item: elapsed &lt; K escolhe k&lt;elapsed&gt;; depois, restante &lt; K escolhe k&lt;restante&gt;
     * (com restante 0 em k0).
     */
    public static float thirdPersonRaise(int elapsedTicks, int useTicks) {
        if (elapsedTicks <= 0) {
            return 0.0F;
        }
        if (elapsedTicks < KEYFRAMES) {
            return elapsedTicks / (float) KEYFRAMES;
        }
        int remaining = useTicks - elapsedTicks;
        if (remaining >= KEYFRAMES) {
            return 1.0F;
        }
        return remaining < 1 ? 0.0F : remaining / (float) KEYFRAMES;
    }

    /** Subida suave da 1ª pessoa (0 segurando, 1 na boca), com o tick fracionário do quadro. */
    public static float firstPersonRaise(float ticksUsing, int useTicks) {
        float up = smoothstep(ticksUsing / FP_RAISE_TICKS);
        float down = smoothstep((useTicks - ticksUsing) / FP_LOWER_TICKS);
        return Math.min(up, down);
    }

    /** Progresso da tragada entre a subida e a descida (0 a 1), para a inclinação crescer devagar. */
    public static float inhaleProgress(float ticksUsing, int useTicks) {
        float hold = useTicks - FP_RAISE_TICKS - FP_LOWER_TICKS;
        return hold <= 0 ? 0.0F : clamp01((ticksUsing - FP_RAISE_TICKS) / hold);
    }

    /** Respiração: seno de período {@link #BREATH_PERIOD_TICKS}, de -1 a 1. */
    public static float breath(float ticks) {
        return (float) Math.sin(2.0D * Math.PI * ticks / BREATH_PERIOD_TICKS);
    }

    /**
     * Ângulos (xRot, yRot) do braço que segura, na 3ª pessoa. {@code baseX} é o xRot que o braço teria segurando um item
     * comum (balanço de andar incluso); com {@code raise} 1 vai para a pose da boca, acompanhando metade do olhar para
     * cima/baixo e todo o giro da cabeça, mais a respiração. {@code mirrored} espelha para o braço esquerdo.
     */
    public static float[] armAngles(Arm raised, float raise, float baseX, float headPitch, float headYaw, float breath,
                                    boolean mirrored) {
        float pitch = Math.max(-MAX_HEAD_PITCH, Math.min(MAX_HEAD_PITCH, headPitch)) * HEAD_PITCH_FOLLOW;
        float targetX = raised.x() + pitch + ARM_BREATH_RADIANS * breath;
        float targetY = (mirrored ? -raised.y() : raised.y()) + headYaw;
        return new float[] {lerp(raise, baseX, targetX), lerp(raise, 0.0F, targetY)};
    }

    /**
     * Matriz 4x4 (coluna-maior, como o JOML espera) aplicada na 1ª pessoa depois da translação base da mão e antes da
     * exibição do modelo aceso (que é a de quem segura). Com a subida r ela vale
     * T(t_r)·R_r·S(s_r/s_0)·R_0⁻¹·T(-t_0), onde (t_0, R_0, s_0) é a exibição de segurar e (t_r, R_r, s_r) a interpolação
     * (translação e escala lineares, rotação por slerp) até a exibição da boca, com a inclinação da tragada e a
     * respiração. Assim a cadeia toda vira base·T(t_r)·R_r·S(s_r)·T(-0,5): o aparelho anda de um jeito ao outro sem
     * saltos, e em r = 0 a matriz é a identidade. No braço esquerdo a translação x é espelhada; a rotação fica igual, como
     * o ItemTransform do 26.3 faz com a mão esquerda.
     */
    public static float[] firstPersonMatrix(SmokeDevice device, float ticksUsing, int useTicks, boolean leftArm) {
        Pose pose = POSES.get(device);
        Display idle = pose.idleFirstPerson();
        Display mouth = pose.mouthFirstPerson();
        float r = firstPersonRaise(ticksUsing, useTicks);
        float breath = breath(ticksUsing);
        float tilt = INHALE_TILT_DEGREES * inhaleProgress(ticksUsing, useTicks) + BOB_PITCH_DEGREES * breath;
        float side = leftArm ? -1.0F : 1.0F;

        double[] q0 = quaternionXYZ(idle.rotX(), idle.rotY(), idle.rotZ());
        double[] q1 = quaternionXYZ(mouth.rotX() + tilt, mouth.rotY(), mouth.rotZ());
        double[] qr = slerp(q0, q1, r);
        double tx0 = side * idle.x() / 16.0D;
        double ty0 = idle.y() / 16.0D;
        double tz0 = idle.z() / 16.0D;
        double tx = side * lerp(r, idle.x(), mouth.x()) / 16.0D;
        double ty = lerp(r, idle.y(), mouth.y()) / 16.0D + BOB_BLOCKS * breath * r;
        double tz = lerp(r, idle.z(), mouth.z()) / 16.0D;
        double scale = lerp(r, idle.scale(), mouth.scale()) / idle.scale();

        double[] m = translation(tx, ty, tz);
        m = multiply(m, rotation(qr));
        m = multiply(m, scaling(scale));
        m = multiply(m, rotation(conjugate(q0)));
        m = multiply(m, translation(-tx0, -ty0, -tz0));
        float[] columnMajor = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                columnMajor[col * 4 + row] = (float) m[row * 4 + col];
            }
        }
        return columnMajor;
    }

    // ------------------------------------------------------------------ matemática (linha-maior por dentro)

    static float smoothstep(float x) {
        float t = clamp01(x);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp01(float x) {
        return Math.max(0.0F, Math.min(1.0F, x));
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    /** Quaternion (x, y, z, w) de rotationXYZ do JOML: Rx · Ry · Rz, ângulos em graus. */
    static double[] quaternionXYZ(double xDeg, double yDeg, double zDeg) {
        double hx = Math.toRadians(xDeg) / 2.0D;
        double hy = Math.toRadians(yDeg) / 2.0D;
        double hz = Math.toRadians(zDeg) / 2.0D;
        double[] qx = {Math.sin(hx), 0, 0, Math.cos(hx)};
        double[] qy = {0, Math.sin(hy), 0, Math.cos(hy)};
        double[] qz = {0, 0, Math.sin(hz), Math.cos(hz)};
        return quaternionProduct(quaternionProduct(qx, qy), qz);
    }

    private static double[] quaternionProduct(double[] a, double[] b) {
        return new double[] {
            a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1],
            a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
            a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3],
            a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2],
        };
    }

    private static double[] conjugate(double[] q) {
        return new double[] {-q[0], -q[1], -q[2], q[3]};
    }

    /** Interpolação esférica pelo caminho mais curto. */
    static double[] slerp(double[] a, double[] b, double t) {
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        double[] end = b.clone();
        if (dot < 0) {
            dot = -dot;
            for (int i = 0; i < 4; i++) {
                end[i] = -end[i];
            }
        }
        double wa;
        double wb;
        if (dot > 0.9995D) {
            wa = 1.0D - t;
            wb = t;
        } else {
            double theta = Math.acos(dot);
            double sin = Math.sin(theta);
            wa = Math.sin((1.0D - t) * theta) / sin;
            wb = Math.sin(t * theta) / sin;
        }
        double[] out = new double[4];
        double norm = 0;
        for (int i = 0; i < 4; i++) {
            out[i] = wa * a[i] + wb * end[i];
            norm += out[i] * out[i];
        }
        norm = Math.sqrt(norm);
        for (int i = 0; i < 4; i++) {
            out[i] /= norm;
        }
        return out;
    }

    private static double[] rotation(double[] q) {
        double x = q[0];
        double y = q[1];
        double z = q[2];
        double w = q[3];
        return new double[] {
            1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), 0,
            2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), 0,
            2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), 0,
            0, 0, 0, 1,
        };
    }

    private static double[] translation(double x, double y, double z) {
        return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
    }

    private static double[] scaling(double s) {
        return new double[] {s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1};
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] out = new double[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[i * 4 + k] * b[k * 4 + j];
                }
                out[i * 4 + j] = sum;
            }
        }
        return out;
    }
}
