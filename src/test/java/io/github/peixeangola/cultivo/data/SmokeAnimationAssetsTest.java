package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.SmokeAnimation;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.StationAnimation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Os modelos gerados da animação de fumar e da prensa batem com as regras puras ({@link SmokeAnimation},
 * {@link StationAnimation}): exibição de segurar, quadros da 3ª pessoa no mesmo passo do braço, aparelho na boca com a
 * pose de braço do Java e, na 1ª pessoa, nunca um item gigante na frente da câmera.
 */
class SmokeAnimationAssetsTest {
    private static final double PX = 1.0D / 16.0D;
    private static final List<String> CONTEXTS = List.of("thirdperson_righthand", "firstperson_righthand");

    private static String id(SmokeDevice device) {
        return device.name().toLowerCase(Locale.ROOT);
    }

    private static JsonObject model(String name) {
        return Resources.json(Resources.assets("models/item/" + name + ".json"));
    }

    /** Exibição de um contexto, seguindo a cadeia de pais do mod (o filho sobrescreve contexto a contexto). */
    private static JsonObject display(String name, String context) {
        JsonObject m = model(name);
        if (m.has("display") && m.getAsJsonObject("display").has(context)) {
            return m.getAsJsonObject("display").getAsJsonObject(context);
        }
        String parent = m.get("parent").getAsString();
        assertTrue(parent.startsWith("cultivo:item/"), name + " sem exibição " + context);
        return display(parent.substring("cultivo:item/".length()), context);
    }

    private static double[] vec(JsonObject display, String key) {
        JsonArray a = display.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static JsonObject smokeAnimation() {
        return Resources.json(StrainColumns.MANIFEST).getAsJsonObject("smoke_animation");
    }

    /** O Java (SmokeAnimation) tem a mesma exibição de segurar na 1ª pessoa que os modelos gerados. */
    @Test
    void javaIdleDisplaysMatchTheModels() {
        for (SmokeDevice device : SmokeDevice.values()) {
            SmokeAnimation.Display idle = SmokeAnimation.pose(device).idleFirstPerson();
            JsonObject json = display(id(device) + "_in_hand", "firstperson_righthand");
            String where = id(device);
            assertEquals(List.of((double) idle.rotX(), (double) idle.rotY(), (double) idle.rotZ()), list(vec(json, "rotation")), where);
            assertEquals(List.of((double) idle.x(), (double) idle.y(), (double) idle.z()), list(vec(json, "translation")), where);
            double[] scale = vec(json, "scale");
            for (double s : scale) {
                assertEquals(idle.scale(), s, 1.0E-6, where);
            }
        }
    }

    /** O manifesto (que o gerador usa) e o Java (que o cliente usa) têm os mesmos quadros e a mesma pose de braço. */
    @Test
    void manifestPosesMatchJava() {
        JsonObject anim = smokeAnimation();
        assertEquals(SmokeAnimation.KEYFRAMES, anim.get("keyframes").getAsInt());
        JsonObject poses = anim.getAsJsonObject("poses");
        assertEquals(SmokeDevice.values().length, poses.size());
        for (SmokeDevice device : SmokeDevice.values()) {
            JsonObject pose = poses.getAsJsonObject(id(device));
            SmokeAnimation.Pose java = SmokeAnimation.pose(device);
            JsonArray arm = pose.getAsJsonArray("arm");
            assertEquals(java.raised().x(), arm.get(0).getAsFloat(), 1.0E-6F, id(device));
            assertEquals(java.raised().y(), arm.get(1).getAsFloat(), 1.0E-6F, id(device));
            assertEquals(java.support() != null, pose.has("support_arm"), id(device));
            if (java.support() != null) {
                JsonArray support = pose.getAsJsonArray("support_arm");
                assertEquals(java.support().x(), support.get(0).getAsFloat(), 1.0E-6F, id(device));
                assertEquals(java.support().y(), support.get(1).getAsFloat(), 1.0E-6F, id(device));
            }
        }
    }

    /**
     * Na 3ª pessoa, com o braço na pose da boca do Java (cabeça reta), o ponto de boca do aparelho cai no alvo do
     * manifesto (a boca, ou na frente do rosto para bong e narguilé), a menos de 0,05 px.
     */
    @Test
    void litModelsPutTheMouthpieceAtTheMouth() {
        JsonObject anim = smokeAnimation();
        JsonArray mouth = anim.getAsJsonArray("mouth");
        for (SmokeDevice device : SmokeDevice.values()) {
            JsonObject pose = anim.getAsJsonObject("poses").getAsJsonObject(id(device));
            JsonArray target = pose.has("target") ? pose.getAsJsonArray("target") : mouth;
            JsonArray point = pose.getAsJsonArray("mouth_point");
            SmokeAnimation.Arm arm = SmokeAnimation.pose(device).raised();
            JsonObject lit = display(id(device) + "_in_hand_lit", "thirdperson_righthand");
            double[] hand = M.chain(M.translate(-5 * PX, 2 * PX, 0), M.rotY(arm.y()), M.rotX(arm.x()), M.rotX(Math.toRadians(-90)),
                M.rotY(Math.toRadians(180)), M.translate(PX, 2 * PX, -10 * PX), displayMatrix(lit));
            double[] at = M.apply(hand, point.get(0).getAsDouble() / 16, point.get(1).getAsDouble() / 16, point.get(2).getAsDouble() / 16);
            for (int k = 0; k < 3; k++) {
                assertEquals(target.get(k).getAsDouble(), at[k] / PX, 0.05D, id(device) + " eixo " + k);
            }
        }
    }

    /**
     * Quadros da 3ª pessoa: k0 é a exibição de segurar, k&lt;i&gt; fica i/K do caminho até a boca; na 1ª pessoa todos
     * (e o modelo aceso) mantêm a exibição de segurar, porque lá quem anima é o cliente.
     */
    @Test
    void keyframesInterpolateBetweenHoldingAndTheMouth() {
        int k = SmokeAnimation.KEYFRAMES;
        for (SmokeDevice device : SmokeDevice.values()) {
            String base = id(device) + "_in_hand";
            JsonObject idle = display(base, "thirdperson_righthand");
            JsonObject lit = display(base + "_lit", "thirdperson_righthand");
            for (int i = 0; i < k; i++) {
                JsonObject frame = display(base + "_lit_k" + i, "thirdperson_righthand");
                for (String key : List.of("rotation", "translation", "scale")) {
                    double[] a = vec(idle, key);
                    double[] b = vec(lit, key);
                    double[] f = vec(frame, key);
                    for (int c = 0; c < 3; c++) {
                        assertEquals(a[c] + (b[c] - a[c]) * i / k, f[c], 0.002D, base + "_lit_k" + i + " " + key);
                    }
                }
            }
            JsonObject idleFirst = display(base, "firstperson_righthand");
            List<String> inUse = new ArrayList<>(List.of(base + "_lit"));
            for (int i = 0; i < k; i++) {
                inUse.add(base + "_lit_k" + i);
            }
            for (String name : inUse) {
                assertEquals(idleFirst, display(name, "firstperson_righthand"), name);
            }
        }
    }

    /**
     * O range_dispatch da 3ª pessoa escolhe, tick a tick, o quadro do mesmo passo que o braço do Java
     * ({@link SmokeAnimation#thirdPersonRaise}): k&lt;passo&gt; na subida e na descida, o modelo aceso no meio.
     */
    @Test
    void thirdPersonDispatchFollowsTheArm() {
        for (SmokeDevice device : SmokeDevice.values()) {
            String item = id(device);
            JsonObject root = Resources.json(Resources.assets("items/" + item + ".json")).getAsJsonObject("model");
            if (device == SmokeDevice.JOINT) {
                root = root.getAsJsonObject("on_false");
            }
            JsonObject inUse = root.getAsJsonObject("on_true");
            assertEquals("minecraft:using_item", root.get("property").getAsString(), item);
            JsonObject thirdPerson = inUse.getAsJsonObject("fallback");
            int use = device.useTicks();
            for (int elapsed = 0; elapsed <= use; elapsed++) {
                float raise = SmokeAnimation.thirdPersonRaise(elapsed, use);
                String expected = raise >= 1.0F
                    ? "cultivo:item/" + item + "_in_hand_lit"
                    : "cultivo:item/" + item + "_in_hand_lit_k" + Math.round(raise * SmokeAnimation.KEYFRAMES);
                assertEquals(expected, dispatch(thirdPerson, elapsed, use - elapsed), item + " tick " + elapsed);
            }
        }
    }

    /**
     * Nunca um item gigante na 1ª pessoa (o borrão da captura 08): em todo o uso, o aparelho fica a pelo menos 0,25
     * bloco da câmera e a parte na tela ocupa no máximo 35% da largura e 65% da altura (FOV 70, tela 16:9).
     */
    @Test
    void firstPersonAnimationNeverFillsTheScreen() {
        double f = 1.0D / Math.tan(Math.toRadians(35.0D));
        double aspect = 16.0D / 9.0D;
        for (SmokeDevice device : SmokeDevice.values()) {
            String base = id(device) + "_in_hand";
            JsonObject idle = display(base, "firstperson_righthand");
            List<double[]> corners = corners(model(base).getAsJsonArray("elements"));
            for (int step = 0; step <= device.useTicks() * 2; step++) {
                float t = step / 2.0F;
                for (boolean left : new boolean[] {false, true}) {
                    double[] chain = M.chain(M.translate((left ? -1 : 1) * SmokeAnimation.HAND_X, SmokeAnimation.HAND_Y,
                        SmokeAnimation.HAND_Z), M.fromColumnMajor(SmokeAnimation.firstPersonMatrix(device, t, device.useTicks(), left)),
                        displayMatrix(idle, left));
                    double nearest = -Double.MAX_VALUE;
                    double minX = 1;
                    double maxX = -1;
                    double minY = 1;
                    double maxY = -1;
                    for (double[] c : corners) {
                        double[] p = M.apply(chain, c[0], c[1], c[2]);
                        nearest = Math.max(nearest, p[2]);
                        double x = Math.max(-1, Math.min(1, p[0] * f / aspect / -p[2]));
                        double y = Math.max(-1, Math.min(1, p[1] * f / -p[2]));
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                    String where = base + (left ? " esquerda" : "") + " t=" + t;
                    assertTrue(nearest <= -0.25D, where + ": perto demais da câmera " + nearest);
                    assertTrue((maxX - minX) / 2 <= 0.35D, where + ": largura " + (maxX - minX) / 2);
                    assertTrue((maxY - minY) / 2 <= 0.65D, where + ": altura " + (maxY - minY) / 2);
                }
            }
        }
    }

    /**
     * A placa animada da prensa: o curso do Java é a distância entre a placa parada (platen_up, embaixo em y 7) e a
     * resina apertada (squeeze, em cima em y 4,5); o modelo que se mexe é a placa de platen_up com o parafuso até a
     * travessa; trabalhando, o modelo do bloco não tem placa (quem desenha é o cliente).
     */
    @Test
    void pressPlatenModelsMatchTheStroke() {
        JsonObject up = Resources.json(Resources.assets("models/block/press_platen_up.json"));
        JsonObject squeeze = Resources.json(Resources.assets("models/block/press_squeeze.json"));
        JsonObject moving = Resources.json(Resources.assets("models/block/press_platen_moving.json"));
        double platenBottom = up.getAsJsonArray("elements").get(1).getAsJsonObject().getAsJsonArray("from").get(1).getAsDouble();
        double resinTop = squeeze.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble();
        assertEquals(7.0D, platenBottom);
        assertEquals(StationAnimation.PRESS_STROKE_PIXELS, platenBottom - resinTop, 1.0E-6D);
        assertEquals(up.getAsJsonArray("elements").get(1), moving.getAsJsonArray("elements").get(1), "a mesma placa");
        double screwTop = moving.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble();
        assertTrue(screwTop - StationAnimation.PRESS_STROKE_PIXELS >= 12.0D, "o parafuso não sai da travessa (y 12–15)");
        JsonObject variants = Resources.json(Resources.assets("blockstates/press_platen.json")).getAsJsonObject("variants");
        Map<String, Integer> y = Map.of("north", 0, "east", 90, "south", 180, "west", 270);
        assertEquals(4, variants.size());
        for (Map.Entry<String, Integer> facing : y.entrySet()) {
            JsonObject v = variants.getAsJsonObject("facing=" + facing.getKey());
            assertEquals("cultivo:block/press_platen_moving", v.get("model").getAsString());
            assertEquals((int) facing.getValue(), v.has("y") ? v.get("y").getAsInt() : 0);
        }
        for (JsonElement part : Resources.json(Resources.assets("blockstates/press.json")).getAsJsonArray("multipart")) {
            JsonObject p = part.getAsJsonObject();
            String applied = p.getAsJsonObject("apply").get("model").getAsString();
            String contents = p.getAsJsonObject("when").has("contents") ? p.getAsJsonObject("when").get("contents").getAsString() : "";
            if (applied.equals("cultivo:block/press_platen_up")) {
                assertEquals("empty|done", contents);
            }
            if (contents.contains("working")) {
                assertEquals("cultivo:block/press_squeeze", applied);
            }
        }
    }

    // ------------------------------------------------------------------ apoio

    /** Escolha do range_dispatch do 26.3: maior limite ≤ valor; abaixo de todos, o fallback. */
    private static String dispatch(JsonObject node, int elapsed, int remaining) {
        String type = node.get("type").getAsString();
        if (type.equals("minecraft:model")) {
            return node.get("model").getAsString();
        }
        assertEquals("minecraft:range_dispatch", type);
        assertEquals("minecraft:use_duration", node.get("property").getAsString());
        boolean useRemaining = node.has("remaining") && node.get("remaining").getAsBoolean();
        double value = useRemaining ? remaining : elapsed;
        JsonObject chosen = node.getAsJsonObject("fallback");
        double best = Double.NEGATIVE_INFINITY;
        for (JsonElement e : node.getAsJsonArray("entries")) {
            double threshold = e.getAsJsonObject().get("threshold").getAsDouble();
            if (threshold <= value && threshold > best) {
                best = threshold;
                chosen = e.getAsJsonObject().getAsJsonObject("model");
            }
        }
        return dispatch(chosen, elapsed, remaining);
    }

    private static List<double[]> corners(JsonArray elements) {
        List<double[]> out = new ArrayList<>();
        for (JsonElement e : elements) {
            JsonArray from = e.getAsJsonObject().getAsJsonArray("from");
            JsonArray to = e.getAsJsonObject().getAsJsonArray("to");
            for (int mask = 0; mask < 8; mask++) {
                out.add(new double[] {
                    ((mask & 1) == 0 ? from : to).get(0).getAsDouble() / 16,
                    ((mask & 2) == 0 ? from : to).get(1).getAsDouble() / 16,
                    ((mask & 4) == 0 ? from : to).get(2).getAsDouble() / 16,
                });
            }
        }
        return out;
    }

    private static double[] displayMatrix(JsonObject display) {
        return displayMatrix(display, false);
    }

    /**
     * ItemTransform#apply do 26.3: translação, rotação XYZ, escala e -0,5. Na mão esquerda o jogo usa o
     * firstperson_lefthand (rotação Y e Z já negadas no JSON) e nega de novo Y, Z e a translação x: o efeito é a rotação
     * da direita com o x espelhado.
     */
    private static double[] displayMatrix(JsonObject display, boolean left) {
        double[] r = vec(display, "rotation");
        double[] t = vec(display, "translation");
        double[] s = vec(display, "scale");
        return M.chain(M.translate((left ? -1 : 1) * t[0] / 16, t[1] / 16, t[2] / 16), M.rotX(Math.toRadians(r[0])),
            M.rotY(Math.toRadians(r[1])), M.rotZ(Math.toRadians(r[2])), M.scale(s[0], s[1], s[2]),
            M.translate(-0.5, -0.5, -0.5));
    }

    private static List<Double> list(double[] v) {
        return List.of(v[0], v[1], v[2]);
    }

    /** Matrizes 4x4 linha-maior. */
    private static final class M {
        static double[] chain(double[]... ms) {
            double[] out = ms[0];
            for (int i = 1; i < ms.length; i++) {
                out = mul(out, ms[i]);
            }
            return out;
        }

        static double[] mul(double[] a, double[] b) {
            double[] out = new double[16];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < 4; k++) {
                        out[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
                    }
                }
            }
            return out;
        }

        static double[] translate(double x, double y, double z) {
            return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
        }

        static double[] scale(double x, double y, double z) {
            return new double[] {x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1};
        }

        static double[] rotX(double a) {
            return new double[] {1, 0, 0, 0, 0, Math.cos(a), -Math.sin(a), 0, 0, Math.sin(a), Math.cos(a), 0, 0, 0, 0, 1};
        }

        static double[] rotY(double a) {
            return new double[] {Math.cos(a), 0, Math.sin(a), 0, 0, 1, 0, 0, -Math.sin(a), 0, Math.cos(a), 0, 0, 0, 0, 1};
        }

        static double[] rotZ(double a) {
            return new double[] {Math.cos(a), -Math.sin(a), 0, 0, Math.sin(a), Math.cos(a), 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        }

        static double[] fromColumnMajor(float[] m) {
            double[] out = new double[16];
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    out[row * 4 + col] = m[col * 4 + row];
                }
            }
            return out;
        }

        static double[] apply(double[] m, double x, double y, double z) {
            return new double[] {
                m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11],
            };
        }
    }
}
