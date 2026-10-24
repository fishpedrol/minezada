package io.github.peixeangola.cultivo.core;

/**
 * Regra de estufa, pura e testável:
 * <ol>
 *   <li>o primeiro bloco não-ar acima da planta, até {@link #RANGE} blocos, é vidro de estufa;</li>
 *   <li>nas 4 direções horizontais há vidro de estufa a até {@link #RANGE} blocos, na altura da planta ou
 *       um bloco acima.</li>
 * </ol>
 */
public final class GreenhouseRules {
    public static final int RANGE = 6;

    public enum Cell { AIR, GLASS, OTHER }

    /** Consulta o tipo do bloco na posição relativa à planta. */
    @FunctionalInterface
    public interface Probe {
        Cell at(int dx, int dy, int dz);
    }

    private static final int[][] HORIZONTAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private GreenhouseRules() {}

    public static boolean isGreenhouse(Probe probe) {
        return hasGlassRoof(probe) && hasGlassWalls(probe);
    }

    public static boolean hasGlassRoof(Probe probe) {
        for (int dy = 1; dy <= RANGE; dy++) {
            Cell cell = probe.at(0, dy, 0);
            if (cell != Cell.AIR) {
                return cell == Cell.GLASS;
            }
        }
        return false;
    }

    public static boolean hasGlassWalls(Probe probe) {
        for (int[] dir : HORIZONTAL) {
            if (!hasGlassInDirection(probe, dir[0], dir[1])) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasGlassInDirection(Probe probe, int stepX, int stepZ) {
        for (int distance = 1; distance <= RANGE; distance++) {
            for (int dy = 0; dy <= 1; dy++) {
                if (probe.at(stepX * distance, dy, stepZ * distance) == Cell.GLASS) {
                    return true;
                }
            }
        }
        return false;
    }
}
