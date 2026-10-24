package io.github.peixeangola.cultivo.core;

import java.util.Locale;

/**
 * Estado visual das estações (propriedade {@code contents} do blockstate): o modelo mostra se a estação está vazia,
 * trabalhando ou com a saída pronta. Classe pura, sem Minecraft; o bloco só copia o resultado para o estado.
 */
public final class StationVisualRules {
    /** Valores da propriedade contents, na ordem do blockstate. */
    public enum Contents {
        /** Nada dentro: só a estação. */
        EMPTY,
        /** Há entrada (mesmo que já haja saída esperando): maços frescos, flor na tela, placa da prensa abaixada. */
        WORKING,
        /** Só a saída: maços secos, resina na bandeja, barra de haxixe na base. */
        DONE;

        /** Nome no blockstate JSON: empty, working, done. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private StationVisualRules() {}

    /** Entrada presente → trabalhando; só saída → pronta; nada → vazia. */
    public static Contents contents(boolean inputEmpty, boolean outputEmpty) {
        if (!inputEmpty) {
            return Contents.WORKING;
        }
        return outputEmpty ? Contents.EMPTY : Contents.DONE;
    }
}
