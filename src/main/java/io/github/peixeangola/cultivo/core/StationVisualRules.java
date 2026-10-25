package io.github.peixeangola.cultivo.core;

import java.util.Locale;

/**
 * Estado visual das estações (propriedade {@code contents} do blockstate): o modelo mostra se a estação está vazia,
 * parada com a entrada, trabalhando de verdade ou com a saída pronta. Classe pura, sem Minecraft; o bloco só copia o
 * resultado para o estado.
 *
 * <p>"Trabalhando" quer dizer que o ciclo anda (há receita para a entrada, com a quantidade mínima, e a saída aceita o
 * resultado), não só que há entrada: a prensa com 1 resina (precisa de 2) ou o suporte com a saída cheia de flores de
 * outra qualidade ficam parados, e o jogador precisa ver isso.
 */
public final class StationVisualRules {
    /** Valores da propriedade contents, na ordem do blockstate. */
    public enum Contents {
        /** Nada dentro: só a estação. */
        EMPTY,
        /** Só a entrada, parada (falta entrada para a receita): maços frescos, flor na tela, resina sob a placa erguida. */
        LOADED,
        /** O ciclo anda: maços balançando, flor tremendo na tela, placa da prensa subindo e descendo. */
        WORKING,
        /** Saída esperando e o ciclo parado: maços secos, resina na bandeja, barra de haxixe na base. */
        DONE;

        /** Nome no blockstate JSON: empty, loaded, working, done. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private StationVisualRules() {}

    /**
     * O ciclo anda → trabalhando (mesmo com saída esperando); parado com saída → pronta, para o jogador ver o produto e
     * retirar; parado só com entrada → carregada; nada → vazia.
     *
     * @param progressing o ciclo avança neste tick (receita para a entrada e saída que aceita o resultado)
     */
    public static Contents contents(boolean inputEmpty, boolean outputEmpty, boolean progressing) {
        if (progressing && !inputEmpty) {
            return Contents.WORKING;
        }
        if (!outputEmpty) {
            return Contents.DONE;
        }
        return inputEmpty ? Contents.EMPTY : Contents.LOADED;
    }
}
