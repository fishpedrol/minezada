package io.github.peixeangola.cultivo.core;

/**
 * Ritmo das estações trabalhando, só visual e sonoro (o progresso do processamento não depende disto). A placa da prensa
 * sobe e desce num ciclo de {@link #PRESS_PERIOD_TICKS} ticks, desenhada pelo cliente; o servidor toca o som do aperto
 * quando, pelo mesmo relógio (tempo do mundo), a placa chega embaixo. Cada estação tem a fase deslocada pela posição,
 * para várias prensas lado a lado não baterem juntas. Puro: sem Minecraft.
 */
public final class StationAnimation {
    /** Curso da placa da prensa (px): de y 7 (em cima) até y 4,5 (apertando a resina). Espelho de models.mjs. */
    public static final float PRESS_STROKE_PIXELS = 2.5F;
    /** Duração de um aperto completo da prensa (ticks). */
    public static final int PRESS_PERIOD_TICKS = 40;
    /** Fração do ciclo em que a placa termina de descer (desce devagar). */
    public static final float PRESS_DOWN_END = 0.55F;
    /** Fração do ciclo em que a placa começa a subir (antes disso fica apertando embaixo). */
    public static final float PRESS_UP_START = 0.75F;
    /** Tick do ciclo em que a placa chega embaixo: é quando o som do aperto toca. */
    public static final int PRESS_BOTTOM_TICK = 22;
    /** Intervalo do farfalhar do suporte de secagem trabalhando (ticks). */
    public static final int DRYING_SOUND_TICKS = 80;
    /** Intervalo do chacoalhar da peneira trabalhando (ticks). */
    public static final int SIEVE_SOUND_TICKS = 30;

    private StationAnimation() {}

    /** Fase própria da estação no ciclo, a partir de um número da posição (BlockPos#asLong). */
    public static int phaseOffset(long positionSeed, int period) {
        long mixed = positionSeed * 0x9E3779B97F4A7C15L;
        return (int) Math.floorMod(mixed ^ (mixed >>> 29), (long) period);
    }

    /**
     * Quanto a placa desceu (px, de 0 em cima até -{@link #PRESS_STROKE_PIXELS} embaixo) no tempo {@code ticks} do mundo
     * (com a fração do quadro): desce com aceleração suave até {@link #PRESS_DOWN_END} do ciclo, fica embaixo até
     * {@link #PRESS_UP_START} e sobe.
     */
    public static float pressOffsetPixels(double ticks, long positionSeed) {
        double local = ticks + phaseOffset(positionSeed, PRESS_PERIOD_TICKS);
        double phase = (((local % PRESS_PERIOD_TICKS) + PRESS_PERIOD_TICKS) % PRESS_PERIOD_TICKS) / PRESS_PERIOD_TICKS;
        float depth;
        if (phase < PRESS_DOWN_END) {
            depth = SmokeAnimation.smoothstep((float) (phase / PRESS_DOWN_END));
        } else if (phase < PRESS_UP_START) {
            depth = 1.0F;
        } else {
            depth = 1.0F - SmokeAnimation.smoothstep((float) ((phase - PRESS_UP_START) / (1.0F - PRESS_UP_START)));
        }
        return -PRESS_STROKE_PIXELS * depth;
    }

    /**
     * A estação trabalhando toca o som de trabalho neste tick do mundo: o suporte de secagem a cada
     * {@link #DRYING_SOUND_TICKS}, a peneira a cada {@link #SIEVE_SOUND_TICKS} e a prensa quando a placa chega embaixo
     * ({@link #PRESS_BOTTOM_TICK} do ciclo, no mesmo relógio da animação).
     */
    public static boolean playsWorkSound(StationKind kind, long gameTime, long positionSeed) {
        return switch (kind) {
            case DRYING -> onBeat(gameTime, positionSeed, DRYING_SOUND_TICKS, 0);
            case SIEVING -> onBeat(gameTime, positionSeed, SIEVE_SOUND_TICKS, 0);
            case PRESSING -> onBeat(gameTime, positionSeed, PRESS_PERIOD_TICKS, PRESS_BOTTOM_TICK);
        };
    }

    private static boolean onBeat(long gameTime, long positionSeed, int period, int beat) {
        return Math.floorMod(gameTime + phaseOffset(positionSeed, period), (long) period) == beat;
    }
}
