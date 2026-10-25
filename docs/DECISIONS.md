# Decisões e questões em aberto

Atualizado em 24/10/2026, com o código do commit `319cf0b`. As constantes ficam nas classes puras de `io.github.peixeangola.cultivo.core` (e no `tools/gen/manifest.json`, quando também viram JSON); os testes JUnit conferem um contra o outro. A tabela completa das genéticas está em [GUIA_INICIANTE.md](GUIA_INICIANTE.md), seção 5.2.

## Pedidos e confirmações do dono
- Mod de Minecraft Java para uso privado com um grupo de amigos; não há plano de divulgação pública.
- Tema de cultivo ficcional inspirado em variedades de cannabis, com etapas de crafting até haxixe e artigos de fumo.
- **Baseado e artigos:** deve ser possível craftar um baseado usando folha de seda e flor seca do mod. Bong e variedade de artigos de fumo também fazem parte do conteúdo.
- **Duas rotas:** outdoor em biomas apropriados e indoor em estufa; ambas funcionam, com qualidade superior na estufa e um pouco inferior ao ar livre.
- **Plantio e descoberta:**
  - o plantio outdoor se comporta como o do trigo e funciona em qualquer bioma/local adequado, sem restrição por bioma;
  - as sementes ficam em baús de mais da metade dos tipos de aldeia da versão escolhida e em plantações/campos de planície e selva quando existirem, ou numa fonte equivalente de campo/loot natural se um desses biomas não gerar aldeias;
  - biomas adicionais podem entrar por necessidade de balanceamento.
- **Loader:** Forge, escolhido em 21/10/2026 (não há dependência do grupo em NeoForge; veja "Alvo de loader").
- **21/10/2026, mecânica de fumar:** "quero que implemente sim" (seção "Mecânica de fumar").
- **24/10/2026, catálogo de genéticas.** O dono pediu: "quero que tenha essas sementes e esses tipos de maconha, pode ser o nome deles mesmo, ou se preferir use um nome parodia", com o link do catálogo High THC da Santa Semente BR. Antes disso a loja era só referência de categorias. Seção "Catálogo de genéticas".
- **24/10/2026, português e animações:** "deixe tudo no mod em português, também crie as animações" (seção "Português e animações").

## Recomendações de produto registradas
- **Nativas:** Brisa, Aurora e Eclipse, as três variedades originais do MVP, continuam como as genéticas "nativas" do mundo, com ciclo e genética distintos (automática/fotoperíodo; sativa/índica/híbrida).
- **Raridade:** cada genética tem uma raridade (comum, incomum, rara) que regula a chance de encontrá-la e nunca limita onde ela pode ser plantada.
- **Qualidade:** atributo simples e persistente do produto colhido; a estufa dá um bônus moderado. Rendimento e qualidade são variáveis distintas.
- **Distribuição:** sementes em baús de mais da metade dos tipos de aldeia, sem garantir item em cada instância, mais plantações/campos de planície e selva (ou fontes equivalentes).

## Alvo de loader
- **Forge 26.3-66.0.13.** Em 21/10/2026 era a release estável; o NeoForge 26.3 só tinha artefatos beta. A matriz exata está em [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md); não há suporte duplo.
- **Mesma versão para todos:** cada jogador e o servidor usam o mesmo Forge e o mesmo jar do Cultivo.
- **Ambiente do dono (24/10/2026):** o launcher dele tem um perfil com Forge 26.3-66.0.2 e outros mods Forge, entre eles o Serene Seasons, o que confirma o Forge como loader do grupo. Desde 25/10/2026 esse Forge é suportado: o `mods.toml` aceita `[66.0.2,)`, e o Cultivo foi testado no 66.0.2 com o Serene Seasons (seção "Compatibilidade: Serene Seasons e versões do Forge").

## Outras regras
- Crescimento, qualidade, inventário, processamento, crafting, drops e fumo são decididos no servidor.
- Todas as etapas de processamento e receitas dos artigos são abstrações de jogo, sem parâmetros ou instruções reais. Nenhum número do mundo real (% de THC, semanas de cultivo) entra no jogo.
- **Artigos:** o escopo inclui baseado, bong, cachimbo, narguilé, chillum, folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro. Outros artigos podem entrar conforme a necessidade do grupo. No MVP os itens não tinham efeitos de consumo; a mecânica de fumar foi pedida depois.
- **Conta:** não adicionar checagem de conta nem alterar a autenticação do jogo.
- **Balanceamento:** os números são provisórios até o grupo jogar.
- **Assets:** o mod tem assets originais completos (texturas, modelos, blockstates, ícones, traduções e sons/partículas usados), sem placeholders.
- **Testes e evidência:** cada incremento tem testes automatizados e a execução de testes e build; o TODO só é atualizado com evidência dos resultados.

## Decisões de implementação

### Identidade e licença
- modId `cultivo`, nome "Cultivo", pacote `io.github.peixeangola.cultivo`, versão 0.1.0.
- MIT para o código e os assets originais (`LICENSE`); a licença LGPL-2.1 do MDK do Forge fica em `FORGE-MDK-LICENSE.txt`. O jar não inclui arquivos do Minecraft.

### Genéticas (`Variety`)
- Uma tabela única com 40 genéticas: Brisa, Aurora e Eclipse (nativas) e as 37 do catálogo, na ordem de `docs/strains-catalog.json`. IDs em minúsculas (`manga_gelada`, `ak_48`, `ouro_18k`, ...).
- **Itens por genética:** `<id>_seeds`, `<id>_crop` (bloco), `<id>_fresh_flower` e `<id>_dried_flower`.
- **Atributos:**
  - origem (nativa ou catálogo);
  - raridade;
  - ciclo (automática ou fotoperíodo);
  - genética (sativa, híbrida ou índica), que escolhe o perfil ao fumar;
  - crescimento: rápida ×1,25, média ×1,0 ou lenta ×0,8 sobre a velocidade do trigo;
  - potência de 1 a 3, que dá a chance de +1 de qualidade: 0,20, 0,35 ou 0,50;
  - família de arte.
- **Nativas:** mantêm os números do MVP.

  | Nativa | Raridade | Ciclo/genética | Crescimento | Chance de +1 de qualidade |
  |---|---|---|---|---|
  | Brisa | comum | automática/sativa | ×1,25 | 0,20 |
  | Aurora | incomum | fotoperíodo/híbrida | ×1,0 | 0,35 |
  | Eclipse | rara | fotoperíodo/índica | ×0,8 | 0,50 |
- **Tooltip** de sementes e flores: "Comum · Automática · Sativa · Potência ★☆☆", com a raridade colorida.

### Crescimento e lavoura (`GrowthRules`)
- **Regras do trigo:** farmland, luz ≥ 9, sem filtro de bioma. A cada random tick, a chance de crescer é `1/(floor(25/v)+1)`, sendo `v = velocidade vanilla × crescimento da genética × 1,25 se estiver em estufa`. A velocidade vanilla foi portada de `CropBlock#getGrowthSpeed`; os hooks `ForgeHooks.onCropsGrowPre/Post` continuam valendo.
- **8 estágios visuais**, um por idade (como o trigo).
  - A idade 6 tem uma cola pequena no topo, com pontinhos de pistilo (ainda verde).
  - Só a idade 7 mostra as colas grandes e coloridas: é o sinal de colher.
- **Arte por família:** as texturas da lavoura são por família de arte. São 12 famílias: as 3 nativas e verde, verde_escuro, limao, dourado, roxo, azul, rosa, gelo e creme. Sementes e flores têm ícone próprio por genética.
- **Aldeões:** o fazendeiro que colhe uma lavoura madura do mod replanta a mesma genética na idade 0 (`village/FarmerReplant`). A flor e as sementes caem no chão, e os aldeões não as recolhem. Isso mantém a lavoura da aldeia como fonte de sementes.

### Qualidade (`QualityRules`)
- Base 2 ao ar livre e 4 na estufa, mais +1 se `random < chance da genética`, limitada a 1–5. Resultado: outdoor 2–3 e estufa 4–5.
- Sorteada no servidor, na colheita, pela função de loot `cultivo:set_harvest_quality`, e gravada no componente `cultivo:quality`.
- **Cura no pote:** +1 uma vez (seção "Pote de vidro e cura"). Depois dela, outdoor fica em 3–4 e estufa em 5: a estufa continua superior.

### Estufa (`GreenhouseRules`)
1. O primeiro bloco não-ar acima da planta, até 6 blocos, é da tag `cultivo:greenhouse_glass`: vidro, vidro fosco, vidraça (painel de vidro), 16 vidros tingidos e 16 vidraças tingidas (35 blocos).
2. Nas 4 direções horizontais há um bloco dessa tag a até 6 blocos, na altura da planta ou um bloco acima.

Não exige luz artificial nem irrigação extra além da farmland.

### Fontes de sementes (`SeedSources`)
- **Baús das casas de aldeia:**
  - o 26.3 tem 5 tipos de aldeia (plains, desert, savanna, snowy, taiga); recebem sementes `minecraft:chests/village/village_{plains,savanna,taiga,desert}_house`, ou seja, 4 de 5 tipos (80%); snowy fica de fora;
  - o pool injetado tem 1 rolagem com **todas as 40 genéticas**, pelo peso da raridade: comum 4 (1–3 sementes), incomum 2 (1–2) e rara 1 (1);
  - o peso "vazio" é a soma dos pesos (119), então 50% dos baús rolam uma semente;
  - por baú: cada comum 4/238 (≈1,7%), cada incomum 2/238 (≈0,84%), cada rara 1/238 (≈0,42%); por classe, 100/238, 8/238 e 11/238;
  - isso substituiu a tabela antiga (vazio 50, Brisa 30, Aurora 14, Eclipse 6), então a chance de cada nativa caiu (Brisa: de 30% para ≈1,7%);
  - o pool é montado em Java (`SeedLootInjector`), porque o `LootTableLoadEvent` do 26.3 não expõe o registro de loot tables; `loot_table/inject/village_seeds.json` é um espelho conferido por teste.
- **Lavouras de aldeia de planície:** `data/minecraft/worldgen/processor_list/farm_plains.json` é sobrescrito.
  - As regras `random_block_match` sobre o trigo entram **antes** das vanilla: Brisa 0,05, Aurora 0,025, Eclipse 0,01. Saem como lavoura do mod na idade 0, só nativas.
  - As regras vanilla ficam como estão (cenoura 0,3, batata 0,2, beterraba 0,1).
  - As fazendas de `zombie_plains` não foram tocadas (nunca tiveram lavoura do mod).
- **Selva:** não há aldeia de selva no 26.3. A fonte equivalente é o bloco `cultivo:wild_plant`, do placed feature `wild_plant_jungle` (rarity 1/16, grupos de até 12), via biome modifier `forge:add_features` em `#minecraft:is_jungle`.
- **Campo natural de planície:** `wild_plant_plains` (rarity 1/48) em `minecraft:plains` e `minecraft:meadow`.
- **Planta silvestre:** ao ser quebrada, dá 1 semente com pesos Brisa 60, Aurora 30 e Eclipse 10 (só nativas).
- **Mascate** (o Mercador Ambulante do vanilla; `village/WanderingTraderSeeds`):
  - cada mascate novo ganha 2 ofertas a mais, depois das vanilla: spawn natural, ovo, `/summon` sem NBT, via `MobSpawnEvent.FinalizeSpawn`;
  - as duas são de genéticas diferentes do catálogo, sorteadas pelos pesos 4/2/1 sem repetição; a primeira sai comum em 96/112 (≈85,7%), incomum em 6/112 (≈5,4%) e rara em 10/112 (≈8,9%);
  - cada oferta vende 2–3 sementes por 2, 4 ou 8 esmeraldas conforme a raridade, com até 4 trocas, 1 de xp e multiplicador de preço 0,05;
  - as ofertas são salvas com o mascate, então recarregar o mundo não as duplica;
  - não é data-driven: o Forge 26.3 não tem evento de trocas do mascate; os números ficam em `SeedSources`. Mascates que já existiam ou criados por `/summon` com NBT não recebem ofertas.

### Estações (`StationKind`, `ProcessingRules`, `StationVisualRules`)
- **Receitas e tempos:**
  - `drying_rack`: `cultivo:drying`, 600 ticks; uma receita por genética, então a secagem mantém a genética;
  - `sieve`: `cultivo:sieving`, 200 ticks; +1 resina com qualidade ≥ 4 e +1 com o dichavador, e os dois somam;
  - `press`: `cultivo:pressing`, 300 ticks, 2 resinas → 1 haxixe.
- **Uma BlockEntity** (`processing_station`) para as três, com 1 entrada, 1 saída e progresso salvo em NBT.
  - A qualidade e a genética (`cultivo:strain`) da entrada passam para a saída.
  - Com a saída cheia ou com item de outra qualidade ou genética, o progresso congela; sem receita válida, zera.
  - Ao quebrar o bloco, os itens caem.
- **Cliques** (tudo decidido no servidor):
  1. Um item que entra vai para a entrada: a pilha inteira, ou o quanto couber.
  2. Um item que não entra (outro item, outra qualidade, entrada cheia) devolve PASS: o vanilla segue com o uso do item ou a colocação de bloco, e nada sai da estação.
  3. Mão principal vazia: tira só a saída pronta. Sem saída, PASS, e aí a mão secundária pode inserir. Segurar o botão ou o clique de outro jogador nunca puxa a entrada em andamento.
  4. Agachado e com as duas mãos vazias: devolve a entrada e zera o progresso. Na peneira o mesmo gesto devolve primeiro a entrada e, com a entrada vazia, o dichavador.

  O que sai vai para o inventário, e o que não couber cai aos pés do jogador, também no criativo (`block/Handouts`). O servidor responde SUCCESS, sem repetir o balanço do braço.
- **Previsão no cliente:** o cliente acerta o resultado do clique com as tags `cultivo:drying_rack_inputs` (`#cultivo:fresh_flowers`), `cultivo:sieve_inputs` (`#cultivo:dried_flowers`) e `cultivo:press_inputs` (`cultivo:resin`), que um teste mantém iguais aos ingredientes, e com os dois slots que a BlockEntity sincroniza quando o conteúdo muda. O progresso não vai para o cliente.
- **Blockstate:** `facing` (a frente olha para quem colocou) e `contents`, que só é gravado quando muda.
  - trabalhando (`working`): o ciclo anda;
  - pronta (`done`): parada com saída esperando;
  - carregada (`loaded`): parada só com entrada (por exemplo, a prensa com 1 resina);
  - vazia (`empty`).

  A peneira tem também `grinder`. Os modelos são multipart: a base gira com o `facing` e as partes de conteúdo seguem o `contents`.
- **Caixas** (frente para o norte, giradas pelo `facing`): suporte `box(0,0,6,16,15,10)`, peneira `box(0,0,0,16,13,16)` (mais `box(11,13,11,15,15,15)` com o dichavador) e prensa `box(1,0,1,15,16,15)`.
- **Ferramentas:** as três estações estão em `minecraft:mineable/axe`.
- **Dichavador na peneira** (`SieveBlock`):
  - clique direito com um dichavador numa peneira sem ele instala (gasta 1, exceto no criativo); um segundo dichavador devolve PASS;
  - tira com o gesto do item 4 dos cliques;
  - quebrar a peneira derruba o dichavador junto (pool extra no loot; no criativo não cai nada, como todo loot);
  - rende: qualidade 2 → 2 resinas, qualidade 4 → 3.

### Receitas de crafting (`ArticleRecipes`, só itens vanilla, exceto os baseados)

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
| bandeja de enrolar | `"N N"/"NNN"`: pepitas de ferro |
| pote de vidro | `"S"/"G"/"G"`: laje de madeira + vidro |
| cinzeiro | `"T T"/"TTT"`: terracota |
| baseado | 40 × `crafting_transmute` `joint_<genética>`: flor seca da genética (entrada) + folha de seda (material); copia a qualidade e grava `cultivo:strain` |
| baseado com piteira | `crafting_transmute` `joint_filtered`: baseado + piteira; grava `cultivo:filtered` e mantém genética e qualidade |
| suporte de secagem | `"SSS"/"T T"/"SSS"`: graveto + linha |
| peneira | `"PTP"/"P P"`: tábuas + linha |
| prensa | `"III"/" S "/"PPP"`: ferro, graveto, tábuas |

- **Por que um transmute por genética:** no 26.3 o `crafting_transmute` copia só o *patch* de componentes da entrada, então um transmute único com tag de entrada perderia a genética.
- **Total:** 55 receitas de crafting (11 artigos + 3 estações + 40 baseados + baseado com piteira). Os baseados ficam agrupados como `cultivo_joint` no livro de receitas.
- **Livro de receitas:** cada receita tem o avanço `data/cultivo/advancement/recipes/misc/<id>.json`, e qualquer um dos ingredientes a desbloqueia.
- O GameTest `article_recipes` confere que cada grade resolve para o item do mod; `joint_filter_recipe` confere o baseado com piteira para as 40 genéticas.

### Papéis dos artigos (`ContentIds`)
- **Fumáveis:** baseado, cachimbo, chillum, bong e narguilé (seção "Mecânica de fumar").
- **Isqueiro:** necessário para fumar, com 64 de durabilidade.
- **Ingredientes:** folha de seda (baseado e bandeja) e piteira (baseado com piteira).
- **Melhoria:** dichavador (peneira).
- **Colocáveis:** bandeja de enrolar, pote de vidro e cinzeiro. O item é um BlockItem com o mesmo ID e mantém o ícone plano, como o vaso de flor vanilla; quebrado, cada um derruba o próprio item.
- **Nenhum artigo é só decorativo:** `DecorativeItem` e a dica "Item decorativo" foram removidos.
- **Pilhas:** folha de seda e piteira empilham 64; baseado 16 (só com a mesma genética, qualidade e marcas); os outros artigos, 1.

### Baseado com piteira
- Componente `cultivo:filtered` (sem valor, `{}` no JSON).
- **Efeito:** só no baseado, a sessão dura ×1,25 (`FILTER_DURATION_MULTIPLIER`) e a náusea é dividida por 2 (`FILTER_NAUSEA_DIVISOR`), depois do teto.
  - Brisa q2: 900 ticks e náusea 160 → 1125 e 80.
  - Eclipse q5: 1440/160 → 1800/80.
- Um baseado que já tem piteira não aceita outra.
- **Arte:**
  - o baseado comum tem a ponta de papel torcido;
  - o com piteira tem a ponta creme, também no modelo 3D na mão;
  - os dois acendem enquanto são fumados, inclusive o ícone da barra de itens.

### Bandeja de enrolar (`RollingRules`, `RollingTrayBlock`)
- Colocada no chão (`facing`; caixa `box(2,0,3,14,2,13)`; picareta).
- **Clique direito:**
  - com pelo menos 2 flores secas iguais na pilha da mão que clicou e 3 folhas de seda em qualquer lugar do inventário, gasta as 2 flores e as 3 folhas e entrega 3 baseados: 1,5 baseado por flor, contra 1 na bancada;
  - confere primeiro as flores, depois as folhas; recusa com "Você precisa de 2 flores secas iguais na mão" ou "Você precisa de 3 folhas de seda no inventário", sem gastar nada;
  - se a mão principal não tem flores bastantes e a secundária segura flores secas, a secundária tenta; o aviso só aparece quando nenhuma das mãos consegue.
- **Baseados da bandeja:** levam todo o patch de componentes da flor (qualidade, cura) e a genética, então empilham com os baseados feitos na bancada com a mesma flor.
- No criativo não gasta nada.

### Pote de vidro e cura (`CuringRules`, `JarBlock`, `JarBlockEntity`)
- **Guarda uma pilha** da tag `cultivo:jar_storable`: flores secas das 40 genéticas, resina, haxixe e baseado (43 itens).
  - Clique direito com um item da tag põe o quanto couber, só no mesmo item com os mesmos componentes.
  - Um item que não entra devolve PASS.
  - A mão principal vazia tira a pilha inteira.
- **Bloco:** caixa `box(4.5,0,4.5,11.5,9.5,11.5)`; sem tag de ferramenta, como o vidro.
  - Comparador: ⌈15 × quantidade / pilha máxima⌉ (10 de 64 → 3; cheio → 15).
  - Quebrar derruba o conteúdo.
  - Sem funil (hopper).
- **Cura:** uma pilha de flores secas que fica 2400 ticks (2 minutos de jogo) sem o conteúdo mudar ganha +1 de qualidade (máximo 5; sem qualidade conta como 2 → 3) e a marca `cultivo:cured` ("Cura concluída").
  - Acontece uma vez só.
  - Qualquer mudança no conteúdo zera o contador.
  - Flores curadas não empilham com as não curadas.
  - Resina, haxixe e baseado não curam.
- **Bloco no mundo:** `filled` escolhe o modelo com conteúdo atrás do vidro. O tick acerta o valor de potes carregados de dados.

### Cinzeiro (`AshtrayRules`, `AshtrayBlock`)
- Bloco de terracota com `ash` de 0 a 4 (caixa `box(4,0,4,12,2.5,12)`; picareta).
- **Junta cinza:** cada sessão de fumo que termina com sucesso, também no criativo, põe +1 de cinza no cinzeiro mais próximo que não está cheio. O alcance é de até 4 blocos na horizontal e 2 na vertical do bloco do jogador, uma caixa de 405 posições, do mais perto para o mais longe.
- **Esvaziar:** cheio, só a mão principal vazia tira 1 farinha de osso e ele volta a 0; segurar qualquer item não esvazia.
- Comparador: cinza × 3 (cheio = 12).
- Quebrado, derruba só a si mesmo, e a cinza se perde.

### Assets
- **Gerador:**
  - texturas em pixel art original, desenhadas por grades ASCII em `tools/art/textures.mjs` (encoder `png.mjs`);
  - modelos 3D (estações, artigos colocados, artigos na mão) em `tools/gen/models.mjs`;
  - blockstates, definições de item, lang, sons, loot, receitas, avanços, tags, worldgen e test_instance em `tools/gen/generate.mjs`, a partir de `tools/gen/manifest.json`;
  - saída em `src/generated/resources`, que não se edita à mão.
- **Saída atual:** 285 texturas (7 animadas) e 786 JSONs.
- **Regra de textura:** alpha só 0 ou 255, para os quads ficarem sólidos ou cutout. Exceção: o ícone 18×18 do efeito `cultivo:high`.
- **Ícones por genética:**
  - as nativas mantêm os desenhos originais;
  - as do catálogo usam a paleta da família com variações determinísticas de forma e cor;
  - um teste exige que quaisquer duas genéticas difiram em pelo menos 16 pixels em cada ícone (semente, flor fresca, flor seca).
- **Haxixe:** o carimbo do haxixe tem a cor da família da genética, e um haxixe sem genética tem cor neutra.
- **Artigos de fumar e isqueiro:** ícone plano no inventário, no chão, na moldura e na estante, e modelo 3D na mão. Bong, narguilé, cachimbo, chillum e baseado têm também um modelo aceso.
- **Estações:** modelos 3D com partes por estado. A placa da prensa em movimento é desenhada no cliente (`client/PressPlatenRenderer`), e o ícone da prensa junta a base e a placa.

### Abas do criativo
- **"Cultivo"** (ícone: baseado; depois da aba "Ovos geradores"): estações, planta silvestre, resina, haxixe, baseado e os 11 artigos.
- **"Cultivo: Genéticas"** (ícone: flor fresca de Brisa; logo depois): semente, flor fresca e flor seca de cada genética, 120 itens. Ordem: nativas primeiro, depois catálogo comum, incomum e rara.

## Catálogo de genéticas (pedido de 24/10/2026)
- **Pedido:** "quero que tenha essas sementes e esses tipos de maconha, pode ser o nome deles mesmo, ou se preferir use um nome parodia", com o link <https://santasementebr.com/loja/?canab%5B%5D=high-thc&catalogo=sementes>.
- **Nomes de paródia** (o pedido permitia):
  - nomes de genética e de banco de sementes podem ser marcas registradas (a Gorilla Glue, por exemplo, passou a se chamar GG4 depois de um processo), e o jogo não exibe marcas de terceiros;
  - cada paródia lembra o original sem copiá-lo (Mango Gelato → Manga Gelada; Gorilla Glue #4 → Cola de Gorila #4; AK-47 → AK-48; 24k Gold → Ouro 18k);
  - os nomes reais e os bancos ficam só em `docs/strains-catalog.json`, para referência do dono;
  - `ParodyNamesTest` falha se um nome real, um banco ou a palavra "THC" aparecer nos arquivos do jogo (`src/main`, `src/generated`, `tools/gen`, `tools/art`, `tools/visual-probe`).
- **Fonte da lista:**
  - o site respondeu HTTP 403 ao acesso automático (WebFetch e Invoke-WebRequest);
  - a lista saiu de trechos de busca das próprias páginas de produto da loja, em 24/10/2026: 37 genéticas High THC, sem os produtos focados em CBD, CBG ou THCV;
  - um snapshot antigo do filtro High THC mostrava cerca de 58 produtos, então a lista pode estar incompleta; o dono pode colar a página para completá-la.
- **Tradução para faixas de jogo** (sem números reais):
  - raridade pela faixa de preço da loja: linha Santa Semente (cerca de R$175–185) = comum; Pyramid Seeds, Dutch Passion e Sweet Seeds (cerca de R$225–265) = incomum; Royal Queen Seeds, Fastbuds e Barney's Farm (cerca de R$350) = rara;
  - ciclo: automática ou fotoperíodo, pelo tipo de produto;
  - crescimento: rápida ×1,25 (automáticas), média ×1,0 ou lenta ×0,8 (sativas de floração longa). Os testes exigem automática ⇔ rápida e lenta ⇒ sativa no catálogo;
  - potência de 1 a 3 pela faixa de THC anunciada (baixa, média, alta), na mesma escala de chance de qualidade das nativas;
  - genética: a predominância sativa, híbrida ou índica, que escolhe o perfil ao fumar (os números de fumo não mudaram);
  - família de arte: verde, verde_escuro, limao, dourado, roxo, azul, rosa, gelo ou creme, com no máximo 6 genéticas por família (o gerador para com erro acima disso).
- **Contagem:** 25 comuns, 4 incomuns e 11 raras no total; só no catálogo, 24, 3 e 10.
- **Fontes:** as do catálogo vêm de baús de aldeia e do mascate. Planta silvestre e lavouras de aldeia continuam só com as nativas.
- **Onde mora cada coisa:**
  - `docs/strains-catalog.json` é a fonte do dono;
  - `core/Variety.java` é a tabela do jogo, na mesma ordem;
  - `tools/gen/manifest.json` → `strains` tem os nomes pt/en e os atributos para o gerador, com valores em português;
  - `ParodyNamesTest` confere o Java contra o catálogo, e `DataConsistencyTest` confere o manifesto contra o Java, coluna por coluna.
- **Nomes gerados** (`name_patterns` do manifesto): "Sementes de X", "Plantio de X", "Flor Fresca de X", "Flor Seca de X".

### Como adicionar uma genética
1. **Catálogo:** em `docs/strains-catalog.json` → `catalog`, acrescente a linha com `id` (minúsculas, sem acento, palavras separadas por `_`), `pt`, `en`, `original`, `breeder`, `cycle`, `genetics`, `growth`, `potency`, `rarity` e `family`.
2. **Java:** acrescente a mesma genética, na mesma posição relativa, no enum `core/Variety.java`. O ID é o nome do enum em minúsculas.
3. **Manifesto:** acrescente a linha em `tools/gen/manifest.json` → `strains`, na mesma ordem e com os mesmos valores.
4. **Família:** use uma família com menos de 6 genéticas.
5. **Verificar:** rode `node tools/gen/generate.mjs`, depois `build` e `runGameTestServer`.
   - Os testes que travam as contagens de hoje vão reclamar: 40 genéticas, 25/4/11, peso vazio 119, 41 entradas de baú, 41 receitas de baseado e 120 itens na aba Genéticas. Eles estão em `StrainCatalogTest`, `SeedSourcesTest`, `DataConsistencyTest` e nos GameTests `village_chest_seeds` e `creative_tabs_split`.
   - Atualize esses valores de propósito e acrescente a linha nova à tabela do guia (seção 5.2).

## Mecânica de fumar (pedida em 21/10/2026; atualizada em 24/10/2026)
O usuário pediu explicitamente: "quero que implemente sim". A intensidade não foi escolhida por ele; o padrão adotado é **moderado** (atrapalha um pouco, sem impedir de jogar). Os números ficam em `core/SmokeRules.java` e `core/SmokeDevice.java`, com testes.

- **Como fumar:** segurar o botão direito com o item, com uma animação própria (seção "Português e animações"). Precisa de um **isqueiro** em qualquer lugar do inventário; o isqueiro perde 1 de durabilidade por uso (64 usos).
- **Artigos** (`SmokeDevice`):

  | Artigo | Material | Força | Tempo de uso |
  |---|---|---|---|
  | baseado | ele mesmo (é consumido) | ×1,0 | 1,6 s |
  | cachimbo | 1 flor seca ou haxixe do inventário | ×1,0 | 1,6 s |
  | chillum | idem | ×1,2 | 1,6 s |
  | bong | idem | ×1,6 | 2,0 s |
  | narguilé | idem | ×0,8 | 2,4 s |

  O material da mão secundária é usado primeiro; senão, o primeiro do inventário. Em modo criativo nada é consumido.
- **Duração** (`SmokeRules`):
  - por sessão: 45 s × força × (0,6 + 0,2 × qualidade) × 1,5 se for haxixe × 1,25 se for baseado com piteira. Sem qualidade conta como 2;
  - fumar de novo **soma** ao tempo restante, até 6 minutos;
  - com menos de 30 s de espaço até o limite, o jogo **recusa** ("já está chapado ao máximo") e nada é gasto;
  - a partir de 2 minutos acumulados o "Chapado" vira nível II e a náusea pode durar mais;
  - o Chapado é aplicado com `forceAddEffect`. Com `addEffect`, um nível I por cima de um nível II viraria "efeito escondido" do vanilla, e as sessões seguintes não somariam (bug achado na revisão).
- **Efeitos** (todos temporários):
  - `cultivo:high` ("Chapado"): só o indicador, com ícone próprio;
  - Fome ("larica"): mesma duração do Chapado (o total acumulado);
  - Náusea: 30% da **sessão atual**, no máximo 8 s (20 s no nível II); metade disso com piteira;
  - perfil pela genética, também calculado só sobre a sessão atual: sativa → Velocidade (50%); híbrida → Regeneração 5 s; índica → Lentidão (50%) + Regeneração 10 s; sem genética conhecida (baseado de `/give`, haxixe antigo) → Lentidão (30%). As 37 do catálogo usam o perfil da genética delas;
  - os efeitos vanilla se juntam aos que o jogador já tem (regras do vanilla). Fumar não remove efeitos de poção, flecha ou sinalizador. Uma versão anterior removia o efeito oposto (Velocidade × Lentidão), mas isso apagava poções e virava exploit (por exemplo, tirar a Lentidão IV do Mestre Tartaruga); foi retirada na 2ª revisão;
  - um "Chapado" infinito (só por comando) conta como no limite: fumar é recusado.
- **Genética do baseado:** os transmutes da bancada copiam a qualidade da flor e gravam a genética em `cultivo:strain`. A peneira e a prensa passam a genética adiante, então resina e haxixe lembram de qual flor vieram. Resinas de genéticas diferentes não empilham, como já acontecia com qualidades diferentes.
- **Isqueiro:** perde 1 de durabilidade por uso (64 usos). Quando quebra, toca o som de item quebrado, conta a estatística e avisa o jogador ("Seu isqueiro acabou").
- **Servidor:** `SmokeSession` revalida isqueiro e material no fim do uso (o cliente só mostra a animação) e é quem consome itens e aplica efeitos. Se a revalidação falha no fim (o isqueiro ou o material sumiu durante o uso), aparece o mesmo aviso da recusa no começo e nada é gasto.
- **Depois de cada sessão bem-sucedida,** o cinzeiro mais próximo ganha cinza (seção "Cinzeiro").

## Português e animações (pedido de 24/10/2026)
O dono pediu: "deixe tudo no mod em português, também crie as animações".

- **Português em tudo:**
  - com `"portuguese_everywhere": true` no manifesto, o gerador escreve o `en_us.json` igual ao `pt_br.json`. Como o `en_us` é o idioma de reserva de todos os outros, qualquer idioma do cliente mostra o Cultivo em português;
  - o inglês continua no manifesto como fonte (`lang.en_us`, campos `en`, `name_patterns.*.en`); com `false`, o `en_us.json` volta a sair em inglês;
  - o gerador para se uma chave faltar em uma das línguas;
  - o metadado também está em português: `mods.toml` com autores "Colaboradores do Cultivo" e descrição em português, e `pack.mcmeta` com "Recursos do Cultivo".
- **Nomes revisados:** "Plantação de X" virou "Plantio de X" (como o vanilla "Plantio de Trigo"); "Bandeja" virou "Bandeja de Enrolar"; "Pote" virou "Pote de Vidro".
- **Dicas de tooltip:** linhas curtas numeradas (`core/TooltipHints`), de no máximo 220 px na escala de GUI, porque frases longas numa linha só passavam da tela. Elas dizem "clique com o botão direito" e o gesto exato. As estações, por exemplo, mostram:
  - "Clique com o botão direito:";
  - "com o ingrediente, coloca";
  - "de mão vazia, tira o que está pronto";
  - "agachado e de mãos vazias, tira a entrada".
- **Sons:** 16 eventos do mod (`cultivo:<id>`, `registry/ModSounds`), todos tocados no servidor.
  - Não há áudio próprio: cada evento reaproveita um evento vanilla, com volume e tom próprios definidos no manifesto, e tem legenda em português:

    | Evento | Áudio vanilla | Legenda |
    |---|---|---|
    | `smoke.inhale` | `entity.player.breath` | Tragando |
    | `smoke.exhale` | `entity.horse.breathe` | Fumaça soprada |
    | `bong.bubble` | `block.bubble_column.upwards_ambient` | Bong borbulha |
    | `hookah.bubble` | `block.bubble_column.upwards_ambient` | Narguilé borbulha |
    | `lighter.click` | `item.flintandsteel.use` | Isqueiro acende |
    | `drying_rack.rustle` | `block.azalea_leaves.step` | Maços farfalham |
    | `sieve.sift` | `item.brush.brushing.sand` | Peneira sacode |
    | `press.squeeze` | `block.piston.contract` | Prensa aperta |
    | `station.finish` | `block.composter.ready` | Processo concluído |
    | `tray.roll` | `item.book.page_turn` | Baseado enrolado |
    | `jar.open` / `jar.close` | `block.barrel.open` / `block.barrel.close` | Pote aberto / Pote fechado |
    | `jar.cured` | `block.amethyst_block.chime` | Flores curadas |
    | `grinder.grind` | `block.grindstone.use` | Dichavador mói |
    | `ashtray.ash` / `ashtray.empty` | `block.composter.fill` / `block.composter.empty` | Cinza cai no cinzeiro / Cinzeiro esvaziado |
  - O código de jogo não toca `SoundEvents` vanilla direto (`SoundsTest`).
- **Animação de fumar** (`core/SmokeAnimation`, `client/SmokingItemClient`):
  - o item usa a animação `NONE` do vanilla e o Cultivo desenha a sua;
  - **1ª pessoa:** o item sobe suave até a boca em 6 ticks, "respira" num ciclo de 16 ticks, inclina um pouco ao tragar e desce em 5 ticks no fim;
  - **3ª pessoa:** o braço sobe em 5 passos de 1 tick, segue metade da inclinação da cabeça e todo o giro dela, e respira. Bong e narguilé usam as duas mãos quando a outra mão está vazia;
  - os modelos aproximam o item da boca em 5 quadros-chave; a posição de cada artigo foi calculada para a boca, nas duas mãos;
  - a subida em 3ª pessoa é em degraus porque o Forge 26.3 não tem gancho por frame para o item na 3ª pessoa.
- **Partículas ao fumar** (desenhadas pelo cliente de cada jogador que está vendo, também para os outros jogadores):
  - chaminha do isqueiro no 1º tick;
  - com o item na boca: brasa e fiapos de fumaça (baseado, cachimbo, chillum), bolhas e fumaça (bong), lufadas de fumaça branca e bolhas (narguilé).
  - **na própria câmera em 1ª pessoa não saem os fiapos de fumaça** (25/10/2026): nascem a meio bloco do olho e, no cliente real, cobriam a tela com uma mancha preta. Brasa, bolhas, chama e a nuvem final continuam, e quem olha de fora vê tudo. O código comum não importa classes do cliente: o cliente registra no setup um `Predicate` em `SmokeEffects.setFirstPersonViewer` (no servidor fica sempre falso).
- **Sons e nuvem final, mandados pelo servidor:**
  - estalo do isqueiro ao começar;
  - som de tragar no 5º tick;
  - borbulhar a cada 14 ticks no bong e no narguilé;
  - no fim, uma nuvem de fumaça na direção do olhar, com o som de expirar.

  Isso substituiu a fumaça e o chiado antigos, que vinham direto do `SmokeSession`.
- **Texturas animadas:** brasa do baseado, brasa genérica, água borbulhando no bong aceso, carvão do narguilé, maço do suporte de secagem balançando e flores tremendo na peneira (7 no total).
- **Estações:**
  - só com o ciclo andando (`working`), o maço balança, as flores da peneira tremem e a placa da prensa sobe e desce. A placa faz um curso de 2,5 px num período de 40 ticks, defasado por posição para prensas vizinhas não andarem juntas;
  - partículas só no cliente: pó âmbar caindo da tela da peneira, resina pingando da prensa, farelo verde dos maços;
  - som de trabalho no ritmo da estação (secagem a cada 80 ticks, peneira a cada 30, prensa a cada 40, quando a placa chega embaixo) e o do dichavador na peneira que tem um;
  - cada ciclo concluído solta uma lufada e o som "Processo concluído".
- **Artigos:**
  - o cinzeiro solta cinza e fumaça e tem som próprio;
  - o pote soa ao guardar (o tom sobe com o enchimento) e ao tirar, e solta brilhos verdes com o som "Flores curadas" ao terminar a cura;
  - a bandeja soa ao enrolar;
  - o dichavador soa ao instalar e ao tirar.
- **Isolamento do cliente:** todo código só do cliente fica no pacote `client/`, e o código comum só chega lá por `SmokingItem.initializeClient`, que o Forge chama apenas no cliente físico. `ClientIsolationTest` confere as fontes e o bytecode, e o `runGameTestServer` (servidor dedicado) passa.

## Compatibilidade: Serene Seasons e versões do Forge
Em 25/10/2026 o Cultivo foi testado no ambiente do dono: Forge 26.3-66.0.2 com o Serene Seasons (SS) 26.1.2.0.7 para Forge 26.3 e o GlitchCore 26.3.0.0.5, baixados do Modrinth com o sha512 conferido. O SS pede forge `[66.0.2,)` e glitchcore `[26.3.0.0.3,)`.

- **Faixa do Forge:** o `mods.toml` passou de `[66,)` para `[66.0.2,)`.
  - O 66.0.2 é a menor versão testada: `build` e `runGameTestServer` passaram compilando contra ela, e o servidor oficial 66.0.2 rodou o jar compilado contra o 66.0.13 (as lavouras do Cultivo e o trigo cresceram).
  - O 66.0.0 e o 66.0.1 ficam de fora: não foram testados, e o 66.0.2 corrige "Fix players dropping items", que o Cultivo usa para devolver sobras com `Player#drop` (`block/Handouts`). O changelog do 66.0.x só tem correções.
  - O build continua no 66.0.13 da matriz (padrão do `build.gradle`). `-Pcultivo_forge_version` e `-Pcultivo_run_dir` servem só para testar outra versão ou outra pasta de mods.
- **Como o SS age nas lavouras:** um mixin no começo do `randomTick` de todo bloco (`BlockBehaviour$BlockStateBase`). Ele não passa pelos eventos do Forge (`CropGrowEvent`, `BonemealEvent`) e só age nos blocos das tags `sereneseasons:spring_crops`, `summer_crops`, `autumn_crops` e `winter_crops` (o que é de ano todo entra nas quatro); um bloco fora delas nunca é afetado.
  - Fora de estação (`out_of_season_crop_behavior`): 0 (padrão) cancela 5 de 6 random ticks, e a lavoura cresce ~1/6; 1 não cresce; 2 quebra a lavoura sem drop, exceto os blocos de `unbreakable_infertile_crops`.
  - Sempre férteis: abaixo de y=48 sem ver o céu, os biomas da lista negra (oceanos, rio, praia, cavernas...) e as dimensões fora da whitelist (por padrão só o overworld tem estações). Biomas tropicais (deserto, savanas, selvas, badlands, mangue...) deixam o que é de verão fértil o ano todo; biomas frios (temperatura < 0,15) só deixam as de inverno.
  - Farinha de osso do jogador numa lavoura fora de estação: o SS gasta o osso mesmo sem crescer (no modo 0 deixa passar 1 em 6). Dispensers e aldeões não passam pelo SS.
  - Calendário: um mundo novo começa no início da primavera; cada sub-estação tem 8 dias, e o ano, 96.
  - Antes desta mudança o Cultivo não estava em nenhuma tag do SS e ignorava as estações (em single player e em servidor), ao contrário do trigo.
- **Regra adotada: paridade com o trigo.**
  - No SS 26.1.2.0.7 o trigo, bloco e semente, só está em `summer_crops` e `autumn_crops`. As 40 lavouras e as 40 sementes do Cultivo entram nas mesmas tags: `data/sereneseasons/tags/block/{summer,autumn}_crops.json` e `data/sereneseasons/tags/item/{summer,autumn}_crops.json`, com `replace: false`.
  - Cada entrada é `{"id": ..., "required": false}`: uma entrada que não resolva é pulada, sem invalidar a tag inteira do SS (e com ela o trigo).
  - As estações vêm de `compat.serene_seasons.seasons` no manifesto (`ContentIds.SERENE_SEASONS_CROP_SEASONS` no Java).
  - A planta silvestre fica de fora: ela não tem random tick.
  - Nada em `spring_crops`, `winter_crops` ou `unbreakable_infertile_crops`, e a `greenhouse_glass` do SS fica como está (mudá-la mudaria o vanilla).
  - **Por quê:** a regra do projeto é "comportamento próximo à lavoura de trigo" (CLAUDE.md), e o dono pediu que o plantio outdoor se comporte como o do trigo (seção "Pedidos e confirmações do dono"). Com o SS instalado, uma lavoura do mod que ignorasse as estações ao lado de um trigo parado na primavera fugiria dessa regra.
  - Sem o SS instalado, ninguém lê essas tags: são inofensivas.
- **Estufa:**
  - A estufa do SS é qualquer bloco da tag `sereneseasons:greenhouse_glass` de 1 a 16 blocos acima, na mesma coluna, sem exigir paredes. A tag é `#c:glass_blocks` (vidro, 16 vidros tingidos e vidro fumê); vidraças (painéis) não contam. Ela só tira a penalidade de estação, sem bônus.
  - A estufa do Cultivo com teto de bloco de vidro também vale como estufa do SS: a lavoura cresce normalmente o ano todo.
  - Com teto de vidraça, só a regra do Cultivo vale: qualidade 4–5, mas sem proteção de estação.
  - A qualidade da colheita não muda com o SS: continua vindo da `GreenhouseRules` e da `QualityRules`.
- **Servidor dedicado (descoberta de 25/10/2026):**
  - No Forge 26.3 (66.0.2 e 66.0.13), `ForgeEventFactory.onTagsUpdated` só é chamado pelo `ClientPacketListener`: o servidor nunca dispara o `TagsUpdatedEvent` ao carregar os dados. O SS só monta as listas de lavouras (`ModFertility.populate`, via GlitchCore) nesse evento.
  - Consequência: num servidor dedicado o SS não afeta nenhuma lavoura, nem o trigo. Em single player e LAN o cliente do anfitrião dispara o evento na mesma JVM, e as estações funcionam.
  - Evidência: servidor oficial 66.0.2 + SS + GlitchCore, `out_of_season_crop_behavior = 1`, `season set early_spring` e bioma plains: os trigos ao ar livre chegaram à idade 7 na primavera. O mixin estava aplicado (log de `-Dmixin.debug.export=true`).
  - O Cultivo não contorna isso: no servidor dedicado ele segue o trigo, e os dois ignoram as estações.
- **Testes:** `CompatibilityTest` (JUnit) trava as tags, o manifesto, o `mods.toml` e o padrão do `build.gradle`. O GameTest `serene_seasons_compat` sempre confere as tags; com o SS carregado, simula a sincronização de tags do single player e confere que o Cultivo e o trigo não crescem no início da primavera e crescem sob um bloco de vidro e no meio do verão. O servidor de produção é testado com `tools/compat/forge-server-compat.sh` e as sondas de console de `tools/compat/probes/`, porque os GameTests não rodam em produção.
- **O que o dono pode mudar:**
  - a config do SS (`config/sereneseasons/fertility.toml`, por exemplo `out_of_season_crop_behavior`), que vale para o trigo e o Cultivo juntos;
  - outra regra de estações por datapack: um arquivo com o mesmo caminho de tag do SS e `"remove"` (extensão de tags do Forge) tira lavouras do Cultivo de uma estação, e um arquivo em `spring_crops` ou `winter_crops` as acrescenta. Nenhum desses datapacks foi testado;
  - para mudar a regra no próprio mod, trocar `compat.serene_seasons.seasons` no manifesto e `ContentIds.SERENE_SEASONS_CROP_SEASONS`, e atualizar `CompatibilityTest` e o GameTest, que travam verão e outono.

## Em aberto
- **Checagem visual no cliente:** feita pelo teste visual automático em 25/10/2026 (47 cenas, TODO.md). Faltam a mão esquerda, os artigos de todos os lados, ouvir os sons e o multiplayer do começo ao fim (o de 25/10 parou por falta de memória). Checklist: GUIA_INICIANTE.md, seção 6.
- **Serene Seasons** (seção "Compatibilidade: Serene Seasons e versões do Forge"). O dono decide:
  - manter a paridade com o trigo (verão e outono) ou adotar outra regra de estações;
  - se usar `out_of_season_crop_behavior = 2` (quebra fora de estação), pôr as lavouras do Cultivo em `unbreakable_infertile_crops`, para não perder sementes raras;
  - se vidraças (painéis) devem contar como estufa do SS, o que mudaria também o vanilla;
  - onde jogar: num servidor dedicado com Forge 26.3 o SS não age em lavoura nenhuma (o `TagsUpdatedEvent` só dispara no cliente), então as estações não afetam nenhuma plantação, nem o trigo; em single player e LAN, afetam.
  - além disso, falta ver no cliente, em single player com o SS: o tooltip "Estações férteis: Verão, Outono" nas sementes, a lavoura lenta na primavera e normal sob bloco de vidro.
- **Catálogo:** pode estar incompleto (37 de cerca de 58 produtos).
- **Balanceamento:** ajuste fino depois de jogar (pesos, raridade da planta silvestre, velocidades, preços do mascate).
- **Limites conhecidos:**
  - o pote sem funil (hopper);
  - nenhum áudio próprio;
  - a subida do braço em degraus na 3ª pessoa;
  - sem chama no item isqueiro enquanto se fuma;
  - bong e narguilé ficam parcialmente atrás dos braços em alguns ângulos da 3ª pessoa.
- **Amigos:** confirmar que todos (e o servidor, se houver) usam o mesmo Forge 26.3, 66.0.2 ou mais novo; o recomendado é o 66.0.13, contra o qual o projeto compila (a recomendação continua sendo o Forge).
