# Pesquisa técnica: loaders para o mod

**Pesquisa técnica revisada em 21/10/2026; situação atualizada em 25/10/2026.** A matriz abaixo está fixada e é a que o projeto usa: troque versões só a pedido do dono e, nesse caso, refaça a conferência nas páginas oficiais e no MDK baixado. As seções depois da matriz registram a pesquisa feita antes de implementar. Os pontos que ela deixava em aberto já foram resolvidos (veja "Itens que estavam abertos antes de implementar").

## Matriz exata do projeto (fixada pelo MDK baixado em 21/10/2026)

| Item | Valor | Onde está |
|---|---|---|
| Minecraft | 26.3 (mappings `26.3-20260918.230105`) | `mods.toml` (`[26.3,26.4)`) |
| Loader | Forge `26.3-66.0.13` | `build.gradle` (`net.minecraftforge:forge:26.3-66.0.13`, padrão de `-Pcultivo_forge_version`), `mods.toml` (`[66.0.2,)`, a menor versão testada; veja "Versões testadas e compatibilidade") |
| Plugin | `net.minecraftforge.gradle` `[7.0.17,8)` (ForgeGradle 7) | `build.gradle` |
| Gradle | wrapper 9.7.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Java | toolchain 25. Testado na nuvem com Temurin 25.0.1+8 (21/10/2026) e no Windows 11 do dono com Temurin 25.0.4.1+1 (24/10/2026) | `build.gradle` |
| Pack format | `min_format`/`max_format` 121 (do MDK) | `pack.mcmeta` |
| Testes | JUnit 5.12.2 (BOM) + Gson 2.13.1 | `build.gradle`, `tools/test-offline.sh` |
| Gerador de recursos | Node.js, sem dependências npm; testado com Node 24.19 no PC do dono | `tools/gen/generate.mjs` |

Essa matriz substitui a divergência entre 66.0.6 e 66.0.10 descrita abaixo, porque o MDK efetivamente baixado traz a 66.0.13.

### Versões testadas e compatibilidade (25/10/2026)
O build e o jar de release continuam no Forge 26.3-66.0.13 da matriz. O `mods.toml` aceita a partir do 66.0.2, a menor versão testada, que é a do launcher do dono. Evidências em TODO.md (seção 3e e tabela); regras em DECISIONS.md, "Compatibilidade: Serene Seasons e versões do Forge".

- **Forge 26.3-66.0.2 em desenvolvimento** (antes das tags do Serene Seasons):
  - `./gradlew build -Pcultivo_forge_version=26.3-66.0.2` deu `BUILD SUCCESSFUL`, com o log `Processing Minecraft dependency: net.minecraftforge:forge:26.3-66.0.2` e 311 testes JUnit, 0 falhas;
  - `runGameTestServer` com a mesma propriedade deu o log `Forge mod loading, version 66.0.2` e `All 62 required tests passed :)`.
- **Forge 26.3-66.0.2 em produção:** o servidor oficial (`forge-26.3-66.0.2-installer.jar --installServer`) com o jar compilado contra o 66.0.13 sobe (`Done (0.701s)!`). A sonda de console `growth.txt` viu as lavouras do Cultivo crescerem como o trigo: idades [6, 7, 7, 7] e [7, 7, 7, 7] em 30 s, com `random_tick_speed 300`.
- **Por que `[66.0.2,)`:** o 66.0.0 e o 66.0.1 não foram testados, e o 66.0.2 corrige "Fix players dropping items" (#10885), que o Cultivo usa para devolver sobras com `Player#drop`.
- **Changelog 66.0.0–66.0.13** ([changelog oficial](https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-changelog.txt)): depois da 66.0.0 ("26.3 - Initial update"), só correções e manutenção.
  - 66.0.1: afogamento dos jogadores; 66.0.2: jogadores soltando itens;
  - 66.0.3: build-logic do ForgeGradle; 66.0.4: patch de `BlockScanUtils`; 66.0.5: entidades empurradas por fluidos; 66.0.6: pipelines OIT dos `ForgeRenderTypes`;
  - 66.0.7: testes e MDK em subprojetos; 66.0.8: tarefas `checkLicense`; 66.0.9: recursos do MDK; 66.0.10: overlay de bloco no modo espectador;
  - 66.0.11: handlers de object holder por nome de registro; 66.0.12: proteção contra listas grandes de canais de rede; 66.0.13: filtro ao pôr itens em bundles.
  - O `maven-metadata.xml` do Maven do Forge lista 66.0.0, 66.0.2 a 66.0.6 e 66.0.8 a 66.0.13; a 66.0.1 e a 66.0.7 não aparecem.
  - Ainda em 25/10/2026, depois dos testes, o mesmo `maven-metadata.xml` já listava também a 26.3-66.1.0. Ela não foi testada, mas a faixa `[66.0.2,)` a aceita.
- **Serene Seasons:** 26.1.2.0.7 para Forge 26.3, com o GlitchCore 26.3.0.0.5, baixados do Modrinth (`api.modrinth.com`, `cdn.modrinth.com`), com o sha512 conferido contra o da API.
  - O `mods.toml` do Serene Seasons pede forge `[66.0.2,)` e glitchcore `[26.3.0.0.3,)`; o do GlitchCore, forge `[66.0.0,)`.
  - Não é dependência do Cultivo: o mod só traz as tags `data/sereneseasons/tags/{block,item}/{summer,autumn}_crops.json`, que ninguém lê sem o Serene Seasons.
  - Com os dois jars na pasta de mods, `runGameTestServer -Pcultivo_run_dir=run-compat-ss` deu `All 63 required tests passed :)` (62 do Cultivo + `minecraft:always_pass`), no 66.0.13 e com `-Pcultivo_forge_version=26.3-66.0.2`. O `serene_seasons_compat` confere a paridade com o trigo: os dois parados no início da primavera e crescendo sob um bloco de vidro e no meio do verão.
- **Servidor dedicado e `TagsUpdatedEvent`:** nas fontes do Forge 26.3 (66.0.2 e 66.0.13), `ForgeEventFactory.onTagsUpdated` só é chamado pelo `ClientPacketListener`. O servidor nunca dispara o `TagsUpdatedEvent` ao carregar os dados, e o Serene Seasons só monta as listas de lavouras nesse evento (`ModFertility.populate`, via GlitchCore).
  - Consequência: num servidor dedicado o Serene Seasons não afeta lavoura nenhuma, nem o trigo. Em single player e LAN o cliente do anfitrião dispara o evento na mesma JVM, e as estações funcionam.
  - Evidência: servidor oficial 66.0.2 + Serene Seasons + GlitchCore, `out_of_season_crop_behavior = 1`, `season set early_spring` e bioma plains: os trigos ao ar livre chegaram à idade 7 na primavera. O mixin estava aplicado (log de `-Dmixin.debug.export=true`).
  - O GameTest dispara o evento como o cliente do single player faria, então o resultado dele vale para single player e LAN.
- **Como repetir:** [tools/compat/README.md](../tools/compat/README.md) tem os comandos para Linux e para o Windows do dono (GameTests com `-Pcultivo_forge_version` e `-Pcultivo_run_dir`) e o teste de produção `tools/compat/forge-server-compat.sh` com as sondas de console (só Linux/macOS). Os GameTests não rodam num servidor de produção: `ForgeGameTestHooks.isGametestEnabled()` exige `!FMLLoader.isProduction()`.

### Rede necessária para o build
O ForgeGradle 7 baixa ferramentas e dependências de `maven.minecraftforge.net` (por exemplo `minecraft-mavenizer-0.5.21.jar`) e o jogo dos servidores da Mojang (`piston-meta.mojang.com`, `libraries.minecraft.net`). Na sessão na nuvem de 21/10/2026, esses hosts foram negados pela política de rede (403 no CONNECT), e `./gradlew build` falhou na configuração. Depois de liberar os domínios nas configurações de rede do ambiente, o build completou (`BUILD SUCCESSFUL`). O plugin em si vem do Gradle Plugin Portal, que estava acessível. Para builds na nuvem, libere esses domínios nas configurações de rede do ambiente.

Hosts que estavam acessíveis: `services.gradle.org`, `plugins.gradle.org`, `repo1.maven.org` e downloads de releases do GitHub (por onde veio o JDK Temurin). Se uma sessão nova não conseguir baixar o Forge, `tools/test-offline.sh` roda os testes JUnit sem o Minecraft. Os testes de bytecode do `ClientIsolationTest` precisam das classes de um build anterior em `build/sourceSets/main`.

### Formatos de dados 26.3 usados (conferidos contra o `client-extra-26.3` jar em 21/10/2026)
A pesquisa inicial veio das notas oficiais dos snapshots; os formatos finais foram copiados dos JSONs vanilla (detalhes em NEXT_SESSION.md, seção 6). Fontes ([26.3 Snapshot 4](https://www.minecraft.net/en-us/article/minecraft-26-3-snapshot-4), [notas do 26.1 Pre-Release 1](https://www.minecraft.net/en-us/article/minecraft-26-1-pre-release-1)):
- **Loot (26.3 Snapshot 4):** nas entradas, `conditions` virou `condition` e `functions` virou `modifier`, com valor único ou lista (lista = `minecraft:sequence`). Nos predicados e funções, o campo de tipo agora se chama `type` (antes `condition`/`function`). Uma lista de condições não equivale mais a `all_of` implícito.
- **Worldgen (26.1):** o tipo `random_patch` foi removido. Patches agora são `count` + `minecraft:offset` (com `x`/`y`/`z`, ex.: `trapezoid`) + `block_predicate_filter` no placed feature, e as features configuradas ficam em `worldgen/feature/`.
- **Receitas:** `crafting_shaped`/`crafting_shapeless` com ingrediente em string (`"ns:item"` ou `"#ns:tag"`) e `result: {id, count}`. `crafting_transmute` (usado no baseado): `input`, `material`, `result: {id, count, components}`; o resultado recebe o *patch* de componentes da entrada e depois os `components` do próprio resultado (`TransmuteRecipe`/`ItemStackTemplate#apply` no 26.3).
- **Efeitos:** `Registries.MOB_EFFECT`; `MobEffectInstance` recebe `Holder<MobEffect>` (pegue com `RegistryObject#getHolder()`); ícone em `assets/<ns>/textures/mob_effect/<id>.png`, 18×18 como os vanilla. Para substituir um efeito com nível menor sem criar "efeito escondido", use `LivingEntity#forceAddEffect`.
- **Formatos adicionados em 24/10/2026:**
  - avanços do livro de receitas em `advancement/recipes/misc/`;
  - definições de item com `minecraft:condition`, `minecraft:select`, `minecraft:range_dispatch` e `minecraft:composite`;
  - blockstates multipart;
  - `sounds.json` com `"type": "event"`;
  - texturas animadas com `.png.mcmeta`.

  Todos foram conferidos contra o código decompilado e os JSONs do 26.3, mas os de cliente só se confirmam de verdade num `runClient`, que ainda está pendente. Os detalhes e as armadilhas estão em NEXT_SESSION.md, seção 6.

## Alvo confirmado

- A versão estável atual do Minecraft Java indicada para esta pesquisa é **26.3**. A Mojang publicou o lançamento em 15/09/2026. [Notas oficiais do Minecraft Java 26.3](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)
- A página oficial de downloads do Forge lista a branch **26.3** e fornece o artefato **MDK**, mas os índices consultados divergiam quanto ao patch: a página de downloads apresentou **66.0.6**, enquanto o índice Maven/projeto listou uma atualização mais recente (**66.0.10**). Por isso a coordenada saiu do MDK efetivamente baixado, que trouxe a **66.0.13** (matriz no topo; histórico de 21/10/2026). [Downloads oficiais Forge 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/) · [Índice de projetos Forge](https://files.minecraftforge.net/project_index.html)
- O MDK 26.3 está baseado em **ForgeGradle 7**, conforme o template oficial, que declara `net.minecraftforge.gradle` na faixa `[7.0.17,8)`. O mesmo MDK define toolchain Java 25. [Template build.gradle da branch 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle)

## Java, Gradle e divergência nas instruções

- Para Minecraft 26.1 e posteriores, o próprio repositório oficial Forge lista **Java 25** como versão exigida. A documentação genérica “Getting Started” do site ainda menciona **JDK 21**, mas está defasada para este alvo. [Tabela Java da branch Forge 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/docs/README.md) · [Getting Started genérico do Forge](https://docs.minecraftforge.net/en/latest/gettingstarted/)
- O template específico do MDK 26.3 declara `java.toolchain.languageVersion = JavaLanguageVersion.of(25)`. Use **JDK 25 de 64 bits** para desenvolver, compilar e executar o wrapper nesse projeto. O runtime Java do Minecraft não deve ser confundido com o JDK usado para compilar; neste alvo ambos devem ser compatíveis com Java 25. [Template build.gradle da branch 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle) · [Requisitos Java Forge 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/docs/README.md)
- O MDK empacota **Gradle 9.7.1** no wrapper (valor `gradle` no catálogo `gradle/libs.versions.toml` da branch 26.3). Não é necessário instalar Gradle globalmente: use `gradlew.bat` no Windows ou `./gradlew` em macOS/Linux. [Catálogo oficial Forge 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/gradle/libs.versions.toml) · [Como funciona o Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- A documentação oficial do Gradle lista suporte para executar Gradle com Java 25 desde Gradle 9.1.0. O wrapper 9.7.1 do MDK supera esse mínimo. [Matriz oficial de compatibilidade Java/Gradle](https://docs.gradle.org/current/userguide/compatibility.html)
- A indicação de JDK 21 em `docs.minecraftforge.net/en/latest/gettingstarted/` não deve ser usada para configurar este projeto 26.3: para resolver o conflito, o template oficial produzido para a branch 26.3 e seu catálogo de versões são as evidências mais específicas e atualizadas.

## Preparação do workspace

1. Instalar um JDK 25 de 64 bits. Forge recomenda oficialmente Eclipse Temurin nas instruções gerais; a exigência de Java 25 para este ramo é confirmada na documentação Forge 26.3. [Requisitos Java Forge](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/docs/README.md) · [Getting Started](https://docs.minecraftforge.net/en/latest/gettingstarted/)
2. Baixar o **MDK** pelo seletor 26.3 da página oficial de downloads e extrair o arquivo em uma pasta vazia de projeto. O MDK inclui um exemplo de mod, wrapper Gradle e estrutura de recursos. [MDK no Forge Downloads](https://files.minecraftforge.net/net/minecraftforge/forge/) · [Instruções oficiais de setup](https://docs.minecraftforge.net/en/latest/gettingstarted/)
3. Importar/abrir o projeto como projeto Gradle em IntelliJ IDEA ou Eclipse (explicitamente suportados pelo Forge); VS Code também tem configuração de execução gerável. O primeiro sync baixa dependências do Forge/Minecraft. [Getting Started](https://docs.minecraftforge.net/en/latest/gettingstarted/)
4. Trocar os marcadores `examplemod` no código e `mods.toml`; definir um `modId` minúsculo estável, `group`, versão e nome do artefato. O ID em metadados precisa coincidir com o usado pelos registros, tarefas de data generation e namespace de recursos. [Personalização do MDK](https://docs.minecraftforge.net/en/latest/gettingstarted/) · [Metadados de mod](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
5. Reavaliar o projeto após alterar arquivos Gradle. Rodar tarefas pelo wrapper. A documentação e o template listam `build`, `runClient`, `runServer`, `runData` e `runGameTestServer`; no Windows, prefixar com `gradlew.bat`. [Getting Started](https://docs.minecraftforge.net/en/latest/gettingstarted/) · [MDK 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle)

Comandos usuais no PowerShell:

    .\gradlew.bat build
    .\gradlew.bat runClient
    .\gradlew.bat runServer
    .\gradlew.bat runData

Neste projeto os recursos de `src/generated/resources` vêm do gerador Node (`node tools/gen/generate.mjs`), e não do `runData`. Os testes dentro do jogo rodam com `.\gradlew.bat runGameTestServer`.

A tarefa `build` gera o JAR em `build/libs`. O teste com servidor dedicado é necessário para detectar referências acidentais a classes/clientes e validar o comportamento multiplayer. Ao primeiro iniciar o servidor de teste, aceite a EULA local no diretório de execução. [Build e teste em servidor](https://docs.minecraftforge.net/en/latest/gettingstarted/)

## Forge versus NeoForge

- Forge e NeoForge são loaders/projetos distintos, com MDKs, dependências, APIs e metadados próprios. Não presumir compatibilidade binária entre eles nem misturar plugins e namespaces. [Histórico do projeto NeoForged](https://neoforged.net/news/2023-retrospection/) · [MDKs oficiais NeoForge](https://github.com/NeoForgeMDKs)
- **Recomendação para este projeto privado:** Forge como primeiro alvo de implementação em Minecraft 26.3, porque as fontes consultadas apontam Forge 26.3 como release estável, enquanto os artefatos encontrados para NeoForge 26.3 têm sufixo `-beta`. Para um grupo pequeno, a estabilidade e a facilidade de todos reproduzirem a mesma configuração pesam mais que suporte a dois loaders. [Downloads Forge 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/) · [Artefatos NeoForge 26.3 no Maven oficial](https://maven.neoforged.net/releases/net/neoforged/neoforge/)
- **Exceção prevista (histórico):** se os amigos já usassem mods indispensáveis em NeoForge, a escolha mudaria. Isso não aconteceu.
  - Em 21/10/2026 ficou o Forge, sem dependência do grupo em NeoForge registrada.
  - Em 24/10/2026, o launcher do dono mostrou um perfil Forge 26.3-66.0.2 com outros mods Forge (Serene Seasons, entre outros), o que confirma o Forge.
  - Todos os jogadores e o servidor devem usar o mesmo Forge 26.3, na 66.0.2 ou mais nova (o `mods.toml` exige `[66.0.2,)`). O projeto compila contra a 66.0.13, que é a recomendada.
- As páginas consultadas nem sempre refletem o mesmo número de patch: a página de downloads do Forge e o índice Maven podem estar em momentos de atualização distintos. Por isso a coordenada do projeto veio do MDK baixado, e não do número "Latest" das páginas.

## Metadados e dependências

- O arquivo de metadados do Forge é TOML em `src/main/resources/META-INF/mods.toml`. Deve declarar o `modLoader` (`javafml`), `loaderVersion`, a licença escolhida pelo projeto e um bloco `[[mods]]` com `modId`, versão, nome, descrição e autoria. `loaderVersion` expressa a faixa aceitável da versão principal do Forge; se houver dependências, cada `[[dependencies.<modid>]]` declara `modId`, obrigatoriedade, faixa de versão, ordenação e lado (`CLIENT`, `SERVER` ou `BOTH`). [Especificação oficial mods.toml](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
- O MDK 26.3 usa `net.minecraftforge.gradle` e declara o Forge como `implementation minecraft.dependency('net.minecraftforge:forge:<minecraft>-<forge>')`. Para começar, a dependência funcional necessária é o próprio Forge; não há motivo para adicionar APIs externas até um requisito concreto existir. [Build do MDK 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle)
- Se futuramente depender de outro mod, declare a dependência no Gradle para compilar e no `mods.toml` para expressar e validar a dependência de runtime. ForgeGradle fornece `fg.deobf(...)` para resolver certos jars de mods em workspace de desenvolvimento; siga documentação compatível com a versão FG7, não snippets antigos. [Guia ForgeGradle de dependências](https://github.com/MinecraftForge/ForgeGradle/wiki/Dependencies) · [Metadados/dependências](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
- Evite incluir bibliotecas externas dentro do JAR sem necessidade; dependências embutidas podem duplicar classes e causar conflitos. Forge oferece Jar-in-Jar para casos que realmente precisem empacotar dependências. [Guia oficial de dependências ForgeGradle](https://github.com/MinecraftForge/ForgeGradle/wiki/Dependencies) · [Exemplo Jar-in-Jar mantido por Forge](https://github.com/MinecraftForge/MDKExamples/tree/master/jarjar-only)

## Cliente, servidor e dados sincronizados

- Minecraft distingue **lado físico** (programa cliente versus servidor dedicado) e **lado lógico** (simulação do mundo/autoridade de jogo versus apresentação/input). Single-player ainda tem servidor lógico rodando dentro do cliente físico. O artigo oficial Forge sobre lados explica que lógica do mundo deve rodar no lado lógico servidor (`Level#isClientSide == false`) e que código client-only em classes comuns pode derrubar um servidor dedicado. [Conceitos de lados Forge](https://docs.minecraftforge.net/en/1.21.x/concepts/sides/)
- Para o mod de cultivo, o servidor deve ser autoridade sobre crescimento, estágio da planta, drops, interações e transformações/crafting. O cliente apresenta modelos, texturas, animação/partículas e interface. Dados precisam seguir os mecanismos normais de estado de bloco e sincronização de inventário; não manter a verdade do jogo só em estáticos do cliente.
- A distribuição multiplayer do mod deve funcionar com o mesmo Forge/Minecraft nos clientes e no servidor quando os itens, blocos, receitas ou regras de jogo do mod forem usados no mundo. Ainda assim, prove compatibilidade executando o perfil `runServer` em servidor dedicado. Forge recomenda explicitamente esse teste. [Getting Started](https://docs.minecraftforge.net/en/latest/gettingstarted/) · [Conceitos de lados](https://docs.minecraftforge.net/en/1.21.x/concepts/sides/)

## Cultivo, crafting e geração de recursos

- Receitas comuns de transformação de itens são orientadas a dados em JSON no sistema de receitas vanilla; a documentação Forge aponta `RecipeProvider` para gerar receitas e avanços por data generation. Para as etapas do jogo, prefira itens/receitas de dados quando bastar combinar ingredientes, e adicione tipo/serializer de receita próprio apenas se houver uma máquina ou lógica de processamento que o formato vanilla não descreva. [Recipes](https://docs.minecraftforge.net/en/latest/resources/server/recipes/) · [Recipe data generation](https://docs.minecraftforge.net/en/latest/datagen/server/recipes/)
- O MDK 26.3 configura uma execução `data` que grava saídas em `src/generated/resources`. Itens, blocos, receitas, tags, loot tables, modelos, blockstates, traduções e avanços são recursos nomeados no namespace do `modId`; mantenha conteúdo gerado separado dos arquivos fonte manuais conforme o padrão do MDK. [Template de build MDK 26.3](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle) · [Data generation Forge](https://docs.minecraftforge.net/en/latest/datagen/)
- Recomendação de modelagem para este escopo: registrar cada variedade de semente/produto como IDs separados; implementar uma planta em estágios de crescimento, com drops definidos por estágio maduro; e representar secagem/extração como transformações explicitamente fictícias entre itens do mod. Receitas padrão bastam para uma cadeia simples; um bloco de trabalho próprio requer estado/inventário persistente e processamento autoritativo no servidor. O detalhe exato é decisão de design do projeto, não requisito imposto pelo Forge.
- A cadeia de resina/haxixe deve ser tratada como ficção de gameplay, sem tentar modelar instruções químicas reais. Isso mantém receitas, tempos, itens e efeitos no contexto de regras de Minecraft.

## Licenciamento e distribuição

- O código do Forge é licenciado sob **LGPL-2.1**; isso não escolhe automaticamente a licença do mod novo. Forge exige informar a licença do conteúdo do mod em `mods.toml`; o projeto deve escolher e registrar a própria licença antes de distribuir. [Licença/declaração no repositório Forge](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/build.gradle) · [Campo license de mods.toml](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
- As diretrizes oficiais de uso do Minecraft permitem distribuir o mod, mas dizem para distribuir **somente o mod**, sem uma versão modificada de Minecraft ou arquivos do jogo. Não incluir binários, assets ou instaladores do Minecraft no release. [Minecraft Usage Guidelines — mods e distribuição](https://www.minecraft.net/en-us/usage-guidelines) · [EULA Minecraft](https://www.minecraft.net/en-us/eula)
- Forge é um loader/build tool; a existência de um JAR do mod não comprova nem promete compatibilidade com launchers não oficiais, versões modificadas do jogo ou fluxos que contornem autenticação. O caminho de suporte e publicação documentável é Forge/Minecraft oficiais e distribuição do mod isolado.

## Itens que estavam abertos antes de implementar (resolvidos)

1. **Loader e versões:** Forge, com as versões tiradas do MDK baixado (matriz no topo; 21/10/2026).
2. **Identidade:** `modId` `cultivo`, group `io.github.peixeangola.cultivo`, versão 0.1.0, licença MIT (DECISIONS.md).
3. **Processamento:** estações com BlockEntity, inventário, progresso por ticks e sincronização dos slots, com receitas próprias `cultivo:drying`, `cultivo:sieving` e `cultivo:pressing` (DECISIONS.md, "Estações").
4. **Conteúdo de cultivo:** itens, estágios, colheita, drops, rendimento e receitas documentados em DECISIONS.md.
5. **Outdoor, aldeias e estufa:** outdoor em qualquer bioma; 4 de 5 tipos de aldeia; lavouras de planície; planta silvestre na selva e na planície; regra da estufa e bônus de qualidade (DECISIONS.md, "Fontes de sementes" e "Estufa").
6. **Validação:** `build`, `runGameTestServer` e `runServer` rodam a cada incremento. O `runClient` abriu no Windows em 24/10/2026 (código `5bded22`); a checagem visual e a conexão de um cliente ao servidor dedicado continuam pendentes (TODO.md). O multiplayer só fica pronto depois desse teste.

## Referência de produto consultada

- A Santa Semente BR apresenta seu catálogo como sementes colecionáveis e organiza variedades por tipo de semente/ciclo (como automática e fotoperíodo), predominância (sativa, índica e híbrida) e origem/marca. No começo, o mod aproveitou só essas dimensões como inspiração. [Loja Santa Semente BR](https://santasementebr.com/loja/) · [Categorias de variedades da marca Santa Semente](https://santasementebr.com/marca/santa-semente/)
- **24/10/2026:** o dono pediu as genéticas do filtro High THC ([catálogo High THC](https://santasementebr.com/loja/?canab%5B%5D=high-thc&catalogo=sementes)), com nomes reais ou de paródia.
  - O site respondeu HTTP 403 ao acesso automático (WebFetch e Invoke-WebRequest), então a lista saiu de trechos de busca das páginas de produto da própria loja: 37 genéticas.
  - Um snapshot antigo mostrava cerca de 58 produtos nesse filtro, então a lista pode estar incompleta.
  - O jogo usa nomes de paródia e faixas abstratas. Os nomes reais e os bancos ficam só em `docs/strains-catalog.json` (DECISIONS.md, "Catálogo de genéticas").

## Fontes oficiais consultadas

- [Minecraft Java 26.3 (Mojang)](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)
- [Downloads Forge 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/)
- [Maven do Forge: versões publicadas](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml) e [changelog 26.3 (66.0.0–66.0.13)](https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-changelog.txt)
- [Branch MinecraftForge 26.3: tabela Java](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/docs/README.md)
- [Branch MinecraftForge 26.3: MDK build template](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/forge-mdk/template/build.gradle)
- [Branch MinecraftForge 26.3: wrapper Gradle version](https://github.com/MinecraftForge/MinecraftForge/blob/26.3/gradle/libs.versions.toml)
- [Forge Getting Started](https://docs.minecraftforge.net/en/latest/gettingstarted/)
- [Forge mod metadata](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
- [Forge recipes e data generation](https://docs.minecraftforge.net/en/latest/resources/server/recipes/)
- [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines)
