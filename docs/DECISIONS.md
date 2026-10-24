# Decisões e questões em aberto

## Confirmado pelo usuário
- Mod de Minecraft Java para uso privado com um grupo de amigos; não há plano de divulgação pública.
- Tema de cultivo ficcional inspirado em variedades de cannabis, com etapas de crafting até haxixe e artigos de fumo.
- Deve ser possível craftar um baseado usando folha de seda e flor seca de cannabis do mod. Bong e variedade de artigos de fumo também fazem parte do conteúdo desejado.
- O cultivo deve ter duas rotas: outdoor em biomas apropriados e indoor em estufa; ambas funcionam, com qualidade superior na estufa e um pouco inferior ao ar livre.
- Plantio outdoor deve se comportar de modo familiar ao cultivo de trigo e ser possível em qualquer bioma/local adequado, sem restrição por bioma. A descoberta de sementes fica concentrada em baús de mais da metade dos tipos de aldeia existentes na versão escolhida e em plantações/campos nos biomas de planície e selva quando existirem; usar fonte equivalente de campo/loot natural se um desses biomas não gerar aldeias. Biomas adicionais podem entrar por necessidade de balanceamento.
- A escolha do loader ainda pode ser Forge ou NeoForge; a prioridade é escolher o melhor para o grupo e para o alvo.
- O site Santa Semente BR é referência para a origem/curadoria das variedades. Usar suas categorias e diversidade como inspiração, sem reproduzir seu catálogo.

## Recomendações de produto registradas
- Começar com três variedades fictícias, abrangendo perfis de ciclo e genética (automática/fotoperíodo; sativa/índica/híbrida). Brisa, Aurora e Eclipse seguem como nomes de trabalho.
- Cada variedade tem raridade que afeta a probabilidade de encontrá-la em baús/plantios de aldeia. Proposta provisória de tiers: Brisa comum, Aurora incomum, Eclipse rara. Raridade regula descoberta e não limita onde a semente pode ser plantada.
- Tratar qualidade como atributo simples persistente do produto colhido; a rota de estufa dá um bônus moderado. Rendimento e qualidade são variáveis distintas.
- Distribuir sementes em baús de mais da metade dos tipos de aldeia existentes na versão alvo, sem garantir item em cada instância. Além disso, disponibilizá-las em plantações/campos de planície e selva onde existam, ou fontes equivalentes de campo/loot natural nesses biomas. Mapear variantes e tabelas de loot durante implementação. Raridades controlam as chances relativas.

## Alvo de loader
- Recomendação atual: Forge para o primeiro MVP de 26.3, por ter release estável verificada; NeoForge 26.3 aparece como beta na pesquisa atual. Para um mod privado, reduzir instabilidade e facilitar reproduzir o mesmo setup entre amigos é prioritário.
- Reavaliar antes do build inicial se o grupo já depende de mods NeoForge. Escolher um loader e usá-lo em todos os clientes e no servidor; não prometer suporte duplo no MVP.
- Matriz técnica, data de verificação e fontes estão em [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md).

## Outras regras
- Crescimento, qualidade, inventário, processamento e drops são decididos no servidor.
- Todas as etapas de processamento e receitas dos artigos são abstrações de jogo, sem parâmetros ou instruções reais.
- Escopo inicial sugerido para artigos craftáveis: baseado, bong, cachimbo, narguilé, chillum, folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro. Outros artigos podem entrar conforme necessidade do grupo. No MVP os itens não tinham efeitos de consumo; a mecânica de fumar foi pedida depois (veja "Mecânica de fumar").
- Não adicionar checagem de conta nem alterar autenticação do jogo.
- Nomes de variedades e balanceamento exato continuam provisórios até protótipo.
- O agente que implementar o mod deve criar os assets originais completos (texturas, modelos, blockstates, ícones e outros recursos visuais/audiovisuais necessários), além do código e conteúdo.
- Cada incremento deve incluir testes automatizados apropriados e execução dos testes/build; atualizar TODO apenas com evidência dos resultados.

## Decisões de implementação (21/10/2026)
Estas decisões vêm de NEXT_SESSION.md, seção 4, e estão no código. As constantes ficam nas classes puras de
`io.github.peixeangola.cultivo.core`, e os testes JUnit conferem os JSONs contra elas.

- **Identidade:** modId `cultivo`, nome "Cultivo", pacote `io.github.peixeangola.cultivo`, versão 0.1.0.
- **Licença:** MIT para o código e os assets originais (`LICENSE`); a licença LGPL-2.1 do MDK do Forge fica em `FORGE-MDK-LICENSE.txt`. O jar não inclui arquivos do Minecraft.
- **Variedades** (`Variety`):
  | Variedade | Raridade | Ciclo/genética | Crescimento | Chance de +1 de qualidade |
  |---|---|---|---|---|
  | Brisa | comum | automática/sativa | ×1,25 | 0,20 |
  | Aurora | incomum | fotoperíodo/híbrida | ×1,0 | 0,35 |
  | Eclipse | rara | fotoperíodo/índica | ×0,8 | 0,50 |
- **Crescimento** (`GrowthRules`): igual ao trigo, com farmland, luz ≥ 9 e sem filtro de bioma. A cada random tick, a chance de crescer é `1/(floor(25/v)+1)`, sendo `v = velocidade vanilla × multiplicador da variedade × 1,25 se estiver em estufa`. A velocidade vanilla foi portada de `CropBlock#getGrowthSpeed`, e os hooks `ForgeHooks.onCropsGrowPre/Post` continuam valendo.
- **Fórmula de qualidade** (`QualityRules`): base 2 ao ar livre e 4 na estufa, mais +1 se `random < chance da variedade`, limitada a 1–5. Resultado: outdoor 2–3 e estufa 4–5. O valor é sorteado no servidor, na colheita, pela função de loot `cultivo:set_harvest_quality`, e gravado no componente `cultivo:quality`.
- **Regra da estufa** (`GreenhouseRules`):
  1. o primeiro bloco não-ar acima da planta, até 6 blocos, é da tag `cultivo:greenhouse_glass` (vidro, vidro fumê, painel de vidro, 16 vidros tingidos e 16 painéis tingidos);
  2. nas 4 direções horizontais há um bloco dessa tag a até 6 blocos, na altura da planta ou um bloco acima.

  Não exige luz artificial nem irrigação extra além da farmland.
- **Fontes de sementes** (`SeedSources`):
  - **Aldeias:** o 26.3 tem 5 tipos (plains, desert, savanna, snowy, taiga). Recebem sementes os baús `minecraft:chests/village/village_{plains,savanna,taiga,desert}_house`, ou seja, 4 de 5 tipos (80%); snowy fica de fora. O pool injetado tem 1 rolagem: vazio 50, Brisa 30 (1–3), Aurora 14 (1–2), Eclipse 6 (1). No 26.3 o `LootTableLoadEvent` não expõe o registro de loot tables, então o pool é montado em Java (`SeedLootInjector`) a partir de `SeedSources`; `loot_table/inject/village_seeds.json` espelha os mesmos pesos. As chances por baú são 30%, 14% e 6%.
  - **Lavouras de aldeia de planície:** `data/minecraft/worldgen/processor_list/farm_plains.json` é sobrescrito. As regras `random_block_match` sobre trigo entram **antes** das vanilla: Brisa 0,05, Aurora 0,025 e Eclipse 0,01, com saída no crop do mod com age 0. As regras vanilla (cenoura 0,3, batata 0,2, beterraba 0,1) foram mantidas.
  - **Selva:** não há aldeia de selva no 26.3. A fonte equivalente é o bloco `cultivo:wild_plant`, colocado pelo placed feature `wild_plant_jungle` (rarity 1/16) via biome modifier `forge:add_features` em `#minecraft:is_jungle`.
  - **Campo natural de planície:** `wild_plant_plains` (rarity 1/48) em `minecraft:plains` e `minecraft:meadow`.
  - Ao quebrar a planta silvestre, ela dá 1 semente sorteada com pesos Brisa 60, Aurora 30 e Eclipse 10.
- **Estações** (`StationKind`, `ProcessingRules`):
  - `drying_rack` (`cultivo:drying`, 600 ticks), `sieve` (`cultivo:sieving`, 200 ticks, +1 resina com qualidade ≥ 4) e `press` (`cultivo:pressing`, 300 ticks, 2 resinas → 1 haxixe);
  - todas usam uma única BlockEntity, com 1 entrada, 1 saída e progresso salvo em NBT;
  - a qualidade da entrada é copiada para a saída;
  - com a saída cheia ou de outra qualidade, o progresso fica congelado; sem receita válida, zera;
  - ao quebrar o bloco, os itens caem no chão.
- **Receitas de crafting** (`ArticleRecipes`, só itens vanilla, exceto o baseado):

  | Item | Receita |
  |---|---|
  | bong | `" G "/" G "/"GWG"`: vidro + balde d'água |
  | cachimbo | `"B  "/" BB"`: tijolo |
  | narguilé | `" I "/"SGS"/"IGI"`: ferro, linha, vidro |
  | chillum | `"T"/"T"`: terracota |
  | folha de seda ×4 | papel + cana-de-açúcar, sem forma |
  | piteira ×4 | papel + linha, sem forma |
  | dichavador | `"I"/"F"/"I"`: ferro + sílex |
  | isqueiro | `"F"/"N"/"N"`: sílex + pepitas de ferro |
  | bandeja | `"N N"/"NNN"`: pepitas de ferro |
  | pote | `"S"/"G"/"G"`: laje de madeira + vidro |
  | cinzeiro | `"T T"/"TTT"`: terracota |
  | baseado | `rolling_paper` + `#cultivo:dried_flowers`, sem forma |
  | suporte de secagem | `"SSS"/"T T"/"SSS"`: graveto + linha |
  | peneira | `"PTP"/"P P"`: tábuas + linha |
  | prensa | `"III"/" S "/"PPP"`: ferro, graveto, tábuas |

  O GameTest `article_recipes` confere que cada grade resolve para o item do mod.
- **Artigos e baseado:** baseado, cachimbo, chillum, bong e narguilé são fumáveis (seção "Mecânica de fumar"); o isqueiro é necessário e tem durabilidade; folha de seda, piteira, dichavador, bandeja, pote e cinzeiro continuam decorativos.
- **Assets:**
  - as texturas são pixel art original em ASCII, geradas por `tools/art/textures.mjs`;
  - blockstates, modelos, definições de item, lang, loot, receitas, tags, worldgen e test_instance são gerados por `tools/gen/generate.mjs` a partir de `tools/gen/manifest.json`;
  - a saída vai para `src/generated/resources` (não editar à mão);
  - sons e partículas são os vanilla, herdados das propriedades do trigo, das tábuas e do arbusto.

## Mecânica de fumar (pedida pelo usuário em 21/10/2026)
O usuário pediu explicitamente: "quero que implemente sim". A intensidade não foi escolhida por ele; o padrão adotado é **moderado** (atrapalha um pouco, sem impedir de jogar), e todos os números ficam em `core/SmokeRules.java` e `core/SmokeDevice.java`, com testes.

- **Como fumar:** segurar o botão direito com o item (animação de levar à boca, como a corneta). Precisa de um **isqueiro** em qualquer lugar do inventário; o isqueiro perde 1 de durabilidade por uso (64 usos).
- **Artigos** (`SmokeDevice`):

  | Artigo | Material | Força | Tempo de uso |
  |---|---|---|---|
  | baseado | ele mesmo (é consumido) | ×1,0 | 1,6 s |
  | cachimbo | 1 flor seca ou haxixe do inventário | ×1,0 | 1,6 s |
  | chillum | idem | ×1,2 | 1,6 s |
  | bong | idem | ×1,6 | 2,0 s |
  | narguilé | idem | ×0,8 | 2,4 s |

  O material da mão secundária é usado primeiro; senão, o primeiro do inventário. Em modo criativo nada é consumido.
- **Duração** (`SmokeRules`): 45 s × força × (0,6 + 0,2 × qualidade) × 1,5 se for haxixe. Sem qualidade conta como 2. Fumar de novo **soma** ao tempo restante, até 6 minutos. A partir de 2 minutos acumulados o "Chapado" vira nível II e a náusea pode durar mais.
- **Efeitos** (todos temporários):
  - `cultivo:high` ("Chapado"): só o indicador, com ícone próprio;
  - Fome ("larica"): mesma duração do Chapado;
  - Náusea: 30% da duração, no máximo 8 s (20 s no nível II);
  - perfil pela variedade: Brisa/sativa → Velocidade (50% da duração); Aurora/híbrida → Regeneração 5 s; Eclipse/índica → Lentidão (50%) + Regeneração 10 s; sem variedade conhecida (haxixe) → Lentidão (30%).
- **Variedade do baseado:** a receita virou 3 receitas `crafting_transmute` (flor seca de cada variedade + folha de seda). O transmute copia os componentes da flor (a **qualidade**) e grava a variedade no componente `cultivo:strain`. Haxixe e resina não guardam variedade.
- **Servidor:** `SmokeSession` revalida isqueiro e material no fim do uso (o cliente só mostra a animação) e é quem consome itens e aplica efeitos.

## Em aberto
- Ajuste fino de balanceamento (pesos, raridade da planta silvestre, velocidades) depois de jogar.
- Usos para os artigos ainda decorativos (dichavador, bandeja, pote, cinzeiro, piteira).
- Confirmar se os amigos usam Forge (a recomendação continua sendo Forge).
