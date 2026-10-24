package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.peixeangola.cultivo.core.StationVisualRules.Contents;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Estado visual das estações (propriedade contents do blockstate). */
class StationVisualRulesTest {
    @Test
    void nothingInsideIsEmpty() {
        assertEquals(Contents.EMPTY, StationVisualRules.contents(true, true));
    }

    @Test
    void inputPresentIsWorkingEvenWithOutputWaiting() {
        assertEquals(Contents.WORKING, StationVisualRules.contents(false, true));
        assertEquals(Contents.WORKING, StationVisualRules.contents(false, false));
    }

    @Test
    void onlyOutputIsDone() {
        assertEquals(Contents.DONE, StationVisualRules.contents(true, false));
    }

    @Test
    void blockstateValueNames() {
        // Os nomes vão para o blockstate JSON ("contents": "empty|working|done")
        assertEquals(List.of("empty", "working", "done"), Arrays.stream(Contents.values()).map(Contents::id).toList());
    }
}
