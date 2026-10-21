# Prompt para a sessão na nuvem

Copie o texto abaixo da linha e use-o como prompt inicial da sessão `claude --cloud`.

---

Retome o mod privado de Minecraft Java "Cultivo" neste repositório e leve-o até o MVP completo. Comece lendo, nesta ordem: CLAUDE.md, docs/NEXT_SESSION.md (resumo da sessão anterior), docs/PRODUCT_SPEC.md, docs/DECISIONS.md, docs/TECHNICAL_RESEARCH.md e docs/TODO.md. As decisões e verificações registradas em docs/NEXT_SESSION.md valem: não refaça a pesquisa nem rediscuta essas escolhas. Pesquise na web só para destravar uma dúvida concreta.

## Estado atual
- O repositório tem o MDK oficial do Forge ainda com `examplemod`, os docs e `tools/art/png.mjs` (encoder PNG em Node, sem dependências).
- Ainda não existe código do mod.
- Matriz fixada:
  - Minecraft 26.3;
  - Forge `26.3-66.0.13`;
  - ForgeGradle `[7.0.17,8)`;
  - Gradle wrapper 9.7.1;
  - Java 25.
- O build do MDK de exemplo já passou localmente. Mantenha essa versão.

## Preparar o ambiente na nuvem
1. Instale um JDK 25 (Temurin) e exporte `JAVA_HOME`. Rode `./gradlew build` uma vez para baixar as dependências.
2. Para consultar a API do 26.3 sem web, use os dois jars que o primeiro build gera:
   - `.gradle/mavenizer/repo/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-sources.jar`: fontes completas do Minecraft e do Forge. Extraia num diretório temporário e use grep.
   - `.gradle/mavenizer/repo/net/minecraft/client-extra/26.3-*/client-extra-*.jar`: JSONs vanilla (loot, receitas, worldgen, modelos). Leia com `unzip -p`.
   Copie os formatos JSON dos arquivos vanilla. O 26.3 mudou vários formatos; a seção 2 de NEXT_SESSION.md lista os que já foram confirmados.

## O que implementar (detalhes em NEXT_SESSION.md, seção 4)
1. **Renomear o MDK:**
   - modId `cultivo`, nome "Cultivo", pacote `io.github.peixeangola.cultivo`, versão 0.1.0;
   - atualize build.gradle (incluindo os args das runs e `forge.enabledGameTestNamespaces=cultivo`), settings.gradle e mods.toml;
   - escolha uma licença e registre-a;
   - apague `ExampleMod.java` e `Config.java`;
   - adicione JUnit 5 (`testImplementation` + `useJUnitPlatform()`).
2. **Regras puras com testes primeiro (TDD):**
   - `Variety`: Brisa comum/automática-sativa/mais rápida; Aurora incomum/fotoperíodo-híbrida; Eclipse rara/fotoperíodo-índica/mais lenta.
   - `QualityRules`, escala de 1 a 5:
     - outdoor: base 2; estufa: base 4;
     - +1 com chance por variedade: 0,20 / 0,35 / 0,50.
   - `GreenhouseRules`:
     - o primeiro bloco não-ar acima da planta, até 6 blocos, é da tag `cultivo:greenhouse_glass`;
     - há vidro dessa tag nas 4 direções horizontais, a até 6 blocos, na altura da planta ou um acima.
   - `ProcessingRules`: na peneira, qualidade ≥ 4 rende +1 resina.
   - `SeedSources`: alvos e pesos de loot.
3. **Conteúdo Java:**
   - registros (blocos, itens, componente `cultivo:quality`, block entities, tipos e serializers de receita, função de loot, creative tab);
   - `CannabisCropBlock extends CropBlock`:
     - funciona em farmland em qualquer bioma, com luz ≥ 9;
     - `randomTick` igual ao vanilla, com os hooks Forge e multiplicador de crescimento por variedade × 1,25 na estufa;
   - bloco `wild_plant`;
   - itens com tooltip de qualidade;
   - função de loot `cultivo:set_harvest_quality`, que calcula a rota no servidor na hora da colheita;
   - injeção em baús via `LootTableLoadEvent`.
4. **Estações de processamento por ticks** (`drying_rack`, `sieve`, `press`):
   - todas usam uma BlockEntity em comum, com 1 slot de entrada e 1 de saída, progresso salvo em NBT e lógica só no servidor;
   - receitas data-driven com os tipos `cultivo:drying` (600 ticks), `cultivo:sieving` (200 ticks) e `cultivo:pressing` (300 ticks; 2 resinas → 1 haxixe);
   - a qualidade da entrada é copiada para a saída;
   - com a saída cheia ou incompatível, o processo para, sem perda nem duplicação; os itens caem no chão quando o bloco é quebrado.
5. **Itens:**
   - por variedade: `<v>_seeds`, `<v>_crop`, `<v>_fresh_flower`, `<v>_dried_flower`;
   - genéricos: `resin`, `hash`, `joint`;
   - artigos: `bong`, `pipe`, `hookah`, `chillum`, `rolling_paper`, `filter_tip`, `grinder`, `lighter`, `rolling_tray`, `jar`, `ashtray`.
   Cada artigo e cada estação tem receita de crafting com itens vanilla. O baseado é shapeless: `rolling_paper` + `#cultivo:dried_flowers`. Efeitos de consumo ficam fora.
6. **Fontes de sementes:**
   - injeção nos baús `village_{plains,savanna,taiga,desert}_house` (4 de 5 tipos de aldeia; snowy fica de fora), com 1 rolagem e pesos vazio 50, brisa 30 (1–3), aurora 14 (1–2), eclipse 6 (1);
   - sobrescrita de `data/minecraft/worldgen/processor_list/farm_plains.json`, mantendo as regras vanilla e acrescentando **antes** delas regras `random_block_match` sobre trigo → crop: brisa 0,05, aurora 0,025, eclipse 0,01;
   - feature de worldgen `wild_plant` via biome modifier `forge:add_features` em `#minecraft:is_jungle` (a selva não tem aldeia) e em plains/meadow, com drops de sementes de pesos 60/30/10;
   - os crops entram nas tags `minecraft:maintains_farmland`, `crops` e `bee_growables`.
7. **Assets originais completos, sem placeholders:**
   - texturas 16×16 geradas por script Node em `tools/art/`, usando `png.mjs`; o crop tem 4 estágios por variedade (idades 0–1, 2–3, 4–5, 6–7);
   - blockstates, modelos de bloco e item, definições `assets/cultivo/items/*.json` e lang `en_us` e `pt_br`, de preferência gerados por `tools/gen/` a partir de um manifesto único;
   - sons e partículas são os vanilla.
8. **Testes:**
   - JUnit para as regras puras e para a completude (todo ID registrado tem blockstate, modelo, item model, textura e chave de tradução nas duas línguas; JSONs de receitas e loot batem com `SeedSources`);
   - GameTests registrados via DeferredRegister em `Registries.TEST_FUNCTION`, mais `data/cultivo/test_instance/*.json` com a estrutura `forge:empty9x9x9`, cobrindo:
     - plantio e crescimento outdoor;
     - estufa vs. outdoor com qualidade diferente;
     - colheita e drops;
     - as 3 estações, incluindo saída cheia e persistência após salvar/carregar a BE;
     - injeção de loot gerando sementes;
     - cada receita de artigo resolvendo para o item do mod, sem conflito com vanilla.

## Validação e fechamento
- Rode `./gradlew test`, `./gradlew runGameTestServer` e `./gradlew build`. Se possível, suba também `runServer` para testar o servidor dedicado (aceite a EULA em `run/`). Corrija as falhas antes de marcar qualquer item como concluído.
- Inspecione o jar: ele deve conter os assets e nenhum arquivo do Minecraft.
- Atualize docs/TODO.md **somente com evidência**, citando os comandos e os resultados reais.
- Registre as decisões em DECISIONS.md: variantes de aldeia cobertas, pesos, regra da estufa e fórmula de qualidade.
- Atualize a matriz exata de versões em TECHNICAL_RESEARCH.md.
- Faça commits incrementais em uma branch nova (não na main).
- Ao final, informe os arquivos alterados, a versão exata, os comandos de build e teste executados com seus resultados, e as pendências.
- Não inclua instruções reais de extração. Não mexa em autenticação ou licença do jogo.
