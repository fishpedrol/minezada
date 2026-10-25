package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.peixeangola.cultivo.core.StationVisualRules.Contents;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Estado visual das estações (propriedade contents do blockstate): "trabalhando" só com o ciclo andando de verdade.
 * Argumentos: entrada vazia, saída vazia, o ciclo anda.
 */
class StationVisualRulesTest {
    @Test
    void nothingInsideIsEmpty() {
        assertEquals(Contents.EMPTY, StationVisualRules.contents(true, true, false));
    }

    @Test
    void progressingIsWorkingEvenWithOutputWaiting() {
        assertEquals(Contents.WORKING, StationVisualRules.contents(false, true, true));
        assertEquals(Contents.WORKING, StationVisualRules.contents(false, false, true));
    }

    @Test
    void onlyOutputIsDone() {
        assertEquals(Contents.DONE, StationVisualRules.contents(true, false, false));
    }

    /**
     * Regressão: a prensa com 3 resinas faz 1 haxixe e sobra 1 resina (precisa de 2). Parada com a saída, mostra o haxixe
     * pronto, não a placa apertando para sempre. Vale também para a saída bloqueada (flores de outra qualidade).
     */
    @Test
    void stalledWithOutputIsDoneNotWorking() {
        assertEquals(Contents.DONE, StationVisualRules.contents(false, false, false));
    }

    /** Regressão: 1 resina sozinha na prensa não anda; aparece parada (carregada), sem movimento. */
    @Test
    void stalledInputWithoutOutputIsLoaded() {
        assertEquals(Contents.LOADED, StationVisualRules.contents(false, true, false));
    }

    /** Sem entrada não há trabalho, mesmo que a chamada diga que sim. */
    @Test
    void noInputIsNeverWorking() {
        assertEquals(Contents.EMPTY, StationVisualRules.contents(true, true, true));
        assertEquals(Contents.DONE, StationVisualRules.contents(true, false, true));
    }

    @Test
    void blockstateValueNames() {
        // Os nomes vão para o blockstate JSON ("contents": "empty|loaded|working|done")
        assertEquals(List.of("empty", "loaded", "working", "done"),
            Arrays.stream(Contents.values()).map(Contents::id).toList());
    }
}
