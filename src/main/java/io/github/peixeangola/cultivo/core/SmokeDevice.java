package io.github.peixeangola.cultivo.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Artigos que dão para fumar no jogo. O baseado se consome; os demais são reutilizáveis e queimam uma flor
 * seca ou um haxixe do inventário a cada uso. Todos exigem um isqueiro no inventário. Tudo aqui é mecânica
 * de jogo fictícia.
 */
public enum SmokeDevice {
    /** Baseado: consome o próprio item. */
    JOINT(false, 1.0F, 32),
    PIPE(true, 1.0F, 32),
    CHILLUM(true, 1.2F, 32),
    /** Bong: o mais forte e o mais demorado. */
    BONG(true, 1.6F, 40),
    /** Narguilé: mais suave e mais demorado. */
    HOOKAH(true, 0.8F, 48);

    private final boolean needsFuel;
    private final float strength;
    private final int useTicks;

    SmokeDevice(boolean needsFuel, float strength, int useTicks) {
        this.needsFuel = needsFuel;
        this.strength = strength;
        this.useTicks = useTicks;
    }

    /** ID do item no namespace cultivo (joint, pipe, chillum, bong, hookah). */
    public String itemId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Precisa de flor seca ou haxixe no inventário (o baseado não precisa). */
    public boolean needsFuel() {
        return needsFuel;
    }

    /** Multiplicador da duração dos efeitos. */
    public float strength() {
        return strength;
    }

    /** Quanto tempo o jogador segura o botão até terminar de fumar (20 ticks = 1 segundo). */
    public int useTicks() {
        return useTicks;
    }

    public static Optional<SmokeDevice> byItemId(String id) {
        for (SmokeDevice device : values()) {
            if (device.itemId().equals(id)) {
                return Optional.of(device);
            }
        }
        return Optional.empty();
    }
}
