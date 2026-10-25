package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tudo que é só visual do cliente fica no pacote {@code io.github.peixeangola.cultivo.client}, que um servidor dedicado
 * nunca carrega: o Forge só registra o CultivoClientEvents com Dist.CLIENT, e o código comum só cita o pacote client
 * dentro de Item#initializeClient (que o Forge só chama no cliente físico). O código comum não importa nem cita
 * net.minecraft.client nem com.mojang.blaze3d, no fonte e no bytecode compilado.
 */
class ClientIsolationTest {
    private static final String CLIENT_PACKAGE = "io/github/peixeangola/cultivo/client/";
    private static final Pattern CLIENT_ONLY = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.");

    private static List<Path> commonSources() throws IOException {
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().replace('\\', '/').contains("/cultivo/client/"))
                .toList();
        }
    }

    /** Linha de comentário (javadoc ou //): citar o nome de um pacote num comentário não carrega classe nenhuma. */
    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("*") || trimmed.startsWith("/*") || trimmed.startsWith("//");
    }

    @Test
    void commonSourcesNeverMentionClientOnlyPackages() throws IOException {
        List<Path> sources = commonSources();
        assertTrue(sources.size() > 50, "poucos fontes comuns: " + sources.size());
        List<String> found = new ArrayList<>();
        for (Path file : sources) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                if (!isComment(lines.get(i)) && CLIENT_ONLY.matcher(lines.get(i)).find()) {
                    found.add(file.getFileName() + ":" + (i + 1) + " " + lines.get(i).trim());
                }
            }
        }
        assertTrue(found.isEmpty(), "código comum citando classes só do cliente: " + found);
    }

    /**
     * O código comum só chega ao pacote client dentro de um initializeClient (ponte do Forge, chamada só no cliente
     * físico), e só importa a interface IClientItemExtensions do Forge nesses arquivos.
     */
    @Test
    void commonCodeReachesTheClientPackageOnlyFromInitializeClient() throws IOException {
        int bridges = 0;
        for (Path file : commonSources()) {
            List<String> lines = Files.readAllLines(file);
            String method = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) {
                    continue;
                }
                Matcher declaration = Pattern.compile("^    (public|protected|private)[^=;]*\\s(\\w+)\\(").matcher(line);
                if (declaration.find()) {
                    method = declaration.group(2);
                }
                if (line.contains("cultivo.client.")) {
                    assertFalse(line.trim().startsWith("import"), file.getFileName() + ": import do pacote client");
                    assertEquals("initializeClient", method, file.getFileName() + ":" + (i + 1) + " " + line.trim());
                    bridges++;
                }
                if (line.contains("net.minecraftforge.client.")) {
                    assertEquals("import net.minecraftforge.client.extensions.common.IClientItemExtensions;", line.trim(),
                        file.getFileName() + ":" + (i + 1));
                }
            }
        }
        assertEquals(1, bridges, "uma ponte só: SmokingItem#initializeClient");
    }

    /** O registro do cliente só acontece pela anotação com Dist.CLIENT (o código comum não cita CultivoClientEvents). */
    @Test
    void clientEventsAreRegisteredOnlyForTheClientDist() throws IOException {
        String events = Files.readString(Resources.JAVA.resolve("io/github/peixeangola/cultivo/client/CultivoClientEvents.java"));
        assertTrue(events.contains("@Mod.EventBusSubscriber(modid = Cultivo.MOD_ID, value = Dist.CLIENT)"));
        for (Path file : commonSources()) {
            assertFalse(Files.readString(file).contains("CultivoClientEvents"), file.getFileName().toString());
        }
    }

    /**
     * No bytecode compilado, nenhuma classe comum tem no pool de constantes um nome de
     * net/minecraft/client ou com/mojang/blaze3d: carregá-la num servidor dedicado nunca puxa uma classe do cliente.
     */
    /**
     * Pasta das classes compiladas do mod: o ForgeGradle 7 usa build/sourceSets/main (o plugin java puro,
     * build/classes/java/main). Acha pela classe principal, que o Gradle sempre compila antes do test.
     */
    static Path mainClasses() throws IOException {
        for (Path candidate : List.of(Path.of("build", "sourceSets", "main"), Path.of("build", "classes", "java", "main"))) {
            if (Files.exists(candidate.resolve("io/github/peixeangola/cultivo/Cultivo.class"))) {
                return candidate;
            }
        }
        throw new IOException("classes compiladas não encontradas em build/ (rode o compileJava)");
    }

    @Test
    void compiledCommonClassesHaveNoClientReferences() throws IOException {
        Path classes = mainClasses();
        List<String> found = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String rel = classes.relativize(file).toString().replace('\\', '/');
                if (rel.startsWith(CLIENT_PACKAGE)) {
                    continue;
                }
                scanned++;
                for (String constant : utf8Constants(file)) {
                    if (constant.contains("net/minecraft/client/") || constant.contains("com/mojang/blaze3d/")) {
                        found.add(rel + ": " + constant);
                    }
                }
            }
        }
        assertTrue(scanned > 50, "poucas classes comuns: " + scanned);
        assertTrue(found.isEmpty(), "classe comum com referência ao cliente: " + found);
    }

    /** Strings UTF-8 do pool de constantes de um .class (nomes de classe, descritores e assinaturas). */
    static List<String> utf8Constants(Path classFile) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream raw = Files.newInputStream(classFile); DataInputStream in = new DataInputStream(raw)) {
            assertEquals(0xCAFEBABE, in.readInt(), classFile + " não é .class");
            in.readUnsignedShort();
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> out.add(in.readUTF());
                    case 3, 4 -> in.skipNBytes(4);
                    case 5, 6 -> {
                        in.skipNBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> in.skipNBytes(2);
                    case 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                    case 15 -> in.skipNBytes(3);
                    default -> throw new IOException(classFile + ": tag desconhecida " + tag);
                }
            }
        }
        return out;
    }

    /**
     * A placa da prensa trabalhando aparece até a distância de renderização do cliente (como o farol vanilla), não só
     * até os 64 blocos padrão de BlockEntityRenderer: trabalhando, o modelo do bloco não tem placa, e de longe ficava um
     * vão. O renderer só roda no cliente, então a regressão é conferida no bytecode: ele declara getViewDistance e lê a
     * distância efetiva das opções.
     */
    @Test
    void pressPlatenIsDrawnUpToTheRenderDistance() throws IOException {
        List<String> constants = utf8Constants(mainClasses().resolve(CLIENT_PACKAGE + "PressPlatenRenderer.class"));
        assertTrue(constants.contains("getViewDistance"), "PressPlatenRenderer sem getViewDistance (fica no padrão de 64)");
        assertTrue(constants.contains("getEffectiveRenderDistance"), "a placa deveria seguir a distância de renderização");
        String source = Files.readString(Resources.JAVA.resolve(CLIENT_PACKAGE + "PressPlatenRenderer.java"));
        assertTrue(source.contains("options.getEffectiveRenderDistance() * 16"), "em blocos: chunks × 16");
    }

    /** O leitor do pool acharia a referência se ela existisse (a ponte cita o pacote client no bytecode). */
    @Test
    void constantPoolReaderSeesClassReferences() throws IOException {
        Path smokingItem = mainClasses().resolve("io/github/peixeangola/cultivo/item/SmokingItem.class");
        List<String> constants = utf8Constants(smokingItem);
        assertTrue(constants.stream().anyMatch(c -> c.contains(CLIENT_PACKAGE + "SmokingItemClient")), constants.toString());
    }
}
