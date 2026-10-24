package io.github.peixeangola.cultivo.core;

/**
 * Cinzeiro (cultivo:ashtray), no estilo da composteira: cada sessão de fumo que termina perto dele (até
 * {@link #RADIUS_XZ} blocos na horizontal e {@link #RADIUS_Y} na vertical) junta 1 de cinza, até {@link #MAX_ASH}.
 * Cheio, a mão vazia tira {@link #BONE_MEAL_PER_EMPTYING} farinha de osso e ele volta a 0, o que devolve o resto da
 * sessão para a lavoura. Classe pura; mecânica fictícia de jogo.
 */
public final class AshtrayRules {
    /** Nível máximo da propriedade ash do blockstate (0–4). */
    public static final int MAX_ASH = 4;
    /** Alcance horizontal (x e z) a partir do bloco onde o jogador está. */
    public static final int RADIUS_XZ = 4;
    /** Alcance vertical a partir do bloco onde o jogador está. */
    public static final int RADIUS_Y = 2;
    /** Sinal do comparador por nível de cinza (cheio = 12). */
    public static final int COMPARATOR_PER_ASH = 3;
    /** Farinha de osso por esvaziada. */
    public static final int BONE_MEAL_PER_EMPTYING = 1;

    private AshtrayRules() {}

    /** Cinza depois de uma sessão por perto: +1, sem passar do máximo. */
    public static int next(int ash) {
        return Math.min(MAX_ASH, ash + 1);
    }

    /** Cheio: não junta mais cinza e a mão vazia pode esvaziar. */
    public static boolean isFull(int ash) {
        return ash >= MAX_ASH;
    }

    /** O cinzeiro na distância (dx, dy, dz) do jogador pega a cinza da sessão. */
    public static boolean inRange(int dx, int dy, int dz) {
        return Math.abs(dx) <= RADIUS_XZ && Math.abs(dy) <= RADIUS_Y && Math.abs(dz) <= RADIUS_XZ;
    }

    /** Posições examinadas a cada sessão: (2×4+1)² × (2×2+1) = 405. */
    public static int searchVolume() {
        int side = 2 * RADIUS_XZ + 1;
        return side * side * (2 * RADIUS_Y + 1);
    }

    public static int comparatorSignal(int ash) {
        return ash * COMPARATOR_PER_ASH;
    }
}
