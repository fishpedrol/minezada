package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.peixeangola.cultivo.core.ContentIds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Toda função registrada em CultivoGameTests está em ContentIds.GAME_TESTS (e portanto tem test_instance). */
class GameTestRegistrationTest {
    @Test
    void registeredFunctionsMatchInstances() throws IOException {
        Path source = Resources.JAVA.resolve("io/github/peixeangola/cultivo/gametest/CultivoGameTests.java");
        Matcher m = Pattern.compile("TESTS\\.register\\(\"([a-z_]+)\"").matcher(Files.readString(source));
        List<String> registered = new ArrayList<>();
        while (m.find()) {
            registered.add(m.group(1));
        }
        assertEquals(ContentIds.GAME_TESTS, registered);
    }
}
