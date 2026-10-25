package io.github.peixeangola.cultivo.core;

/**
 * Bandeja de enrolar (cultivo:rolling_tray): clique direito na bandeja com flores secas iguais na mão enrola
 * baseados usando folhas de seda do inventário. Rende {@link #JOINTS_PER_ROLL} baseados por {@link #FLOWERS_PER_ROLL}
 * flores (1,5 por flor; na bancada é 1 por flor), o que põe o baseado entre o chillum e o bong. Classe pura; tudo é
 * mecânica fictícia de jogo.
 */
public final class RollingRules {
    /** Flores secas da mesma pilha (mesmo item e mesmos componentes) gastas por vez. */
    public static final int FLOWERS_PER_ROLL = 2;
    /** Folhas de seda gastas por vez (de qualquer lugar do inventário). */
    public static final int PAPERS_PER_ROLL = 3;
    /** Baseados entregues por vez, com a qualidade e a variedade da flor. */
    public static final int JOINTS_PER_ROLL = 3;

    /** Motivo para a bandeja recusar, com a chave da mensagem mostrada acima da barra de itens. */
    public enum Refusal {
        NONE(null),
        NEEDS_FLOWERS("message.cultivo.tray_need_flowers"),
        NEEDS_PAPERS("message.cultivo.tray_need_papers");

        private final String messageKey;

        Refusal(String messageKey) {
            this.messageKey = messageKey;
        }

        /** Chave de tradução da mensagem (null para NONE). */
        public String messageKey() {
            return messageKey;
        }
    }

    private RollingRules() {}

    /**
     * Confere primeiro as flores (só as da pilha na mão contam: duas pilhas de qualidades diferentes não se juntam) e
     * depois as folhas de seda.
     *
     * @param flowersInHand quantas flores secas há na pilha da mão que clicou
     * @param papers quantas folhas de seda há no inventário todo
     */
    public static Refusal check(int flowersInHand, int papers) {
        if (flowersInHand < FLOWERS_PER_ROLL) {
            return Refusal.NEEDS_FLOWERS;
        }
        if (papers < PAPERS_PER_ROLL) {
            return Refusal.NEEDS_PAPERS;
        }
        return Refusal.NONE;
    }

    /**
     * O clique da mão principal passa para a mão secundária: a principal não tem flores bastantes e a secundária segura
     * flores secas. Então a principal devolve PASS (cliente e servidor decidem igual, com o que os dois sabem) e a
     * secundária enrola ou dá o próprio aviso; o aviso só aparece quando nenhuma das mãos consegue enrolar. Faltando
     * folhas de seda a outra mão não ajuda (as folhas são as mesmas), então o aviso sai já na principal.
     *
     * @param mainHand o clique é da mão principal (o vanilla tenta a principal e depois a secundária)
     * @param refusal o resultado de {@link #check} para a pilha da mão que clicou
     * @param offHandHoldsDriedFlowers a mão secundária segura flores secas
     */
    public static boolean defersToOffHand(boolean mainHand, Refusal refusal, boolean offHandHoldsDriedFlowers) {
        return mainHand && refusal == Refusal.NEEDS_FLOWERS && offHandHoldsDriedFlowers;
    }
}
