package io.github.peixeangola.cultivo.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Acesso aos recursos gerados (src/generated/resources) e manuais (src/main/resources) a partir da raiz. */
final class Resources {
    static final Path GENERATED = Path.of("src", "generated", "resources");
    static final Path MAIN = Path.of("src", "main", "resources");
    static final Path JAVA = Path.of("src", "main", "java");

    private Resources() {}

    static Path assets(String path) {
        return GENERATED.resolve("assets").resolve("cultivo").resolve(path);
    }

    static Path data(String namespace, String path) {
        return GENERATED.resolve("data").resolve(namespace).resolve(path);
    }

    static JsonObject json(Path file) {
        try {
            JsonElement element = JsonParser.parseString(Files.readString(file));
            return element.getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("faltando: " + file, e);
        }
    }

    /** Converte "cultivo:block/x" em textures/block/x.png (ou models/...). */
    static Path cultivoResource(String id, String folder, String extension) {
        if (!id.startsWith("cultivo:")) {
            throw new IllegalArgumentException("não é cultivo: " + id);
        }
        return assets(folder + "/" + id.substring("cultivo:".length()) + extension);
    }
}
