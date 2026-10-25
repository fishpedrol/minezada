# Especificação do produto

## Objetivo

Mod privado para Minecraft: Java Edition, feito para jogar com amigos. O jogador encontra sementes de variedades fictícias inspiradas na diversidade de genéticas e ciclos catalogados por bancos de sementes, cultiva plantas, colhe flores e as processa por uma cadeia de crafting em etapas que termina em haxixe. Todo o cultivo e processamento são abstrações de gameplay com itens, receitas e ticks de Minecraft; não representam nem ensinam processos reais.

A Santa Semente BR serviu primeiro como referência de categorias e organização de variedades. Desde 24/10/2026, a pedido do dono, ela também é a fonte da lista de genéticas: as do filtro High THC da loja entram no jogo com **nomes de paródia** e atributos em faixas abstratas de jogo (raridade, ciclo, genética, crescimento, potência), sem números do mundo real. Os nomes reais ficam só em `docs/strains-catalog.json`.

## Alvo

Minecraft Java Edition 26.3 com Forge 26.3-66.0.13 e Java 25, com versões e toolchain fixadas em [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md). O Forge foi escolhido em 21/10/2026 (o grupo não depende de NeoForge, e o launcher do dono já usa Forge). O projeto é privado para um grupo de amigos e mantém um loader só.

Bedrock e NeoForge ficam fora do alvo.

## Ciclo principal

Semente(variedade) → planta com estágios de crescimento → flor fresca → suporte de secagem → flor seca → peneira de bancada → resina fictícia → prensa manual → haxixe. A flor seca também pode ser combinada com folha de seda para craftar um baseado. O conteúdo inclui bong e outros artigos de fumo como itens craftáveis.

O jogador obtém sementes em fontes de mundo, escolhe uma variedade e pode cultivar de duas formas: outdoor em qualquer bioma e local válido para plantio, com comportamento integrado ao sistema de lavoura do trigo; ou indoor numa estufinha craftada pelo jogador (desde 25/10/2026, no lugar da estufa de vidro: um bloco de 2 de altura com luz roxa e um pé de planta, sem farmland nem água). Ambas as rotas permitem crescer e colher. A estufinha oferece condições mais controladas e gera produto de qualidade superior; o cultivo outdoor é viável, mas tende a qualidade um pouco inferior. Qualidade é um atributo de gameplay persistente que influencia os resultados do processamento. A descoberta de sementes fica concentrada em baús de mais da metade dos tipos de aldeia existentes na versão escolhida e em plantações/campos nos biomas de planície e selva quando existirem. Se um desses biomas não gerar aldeias no jogo, incluir fonte equivalente de sementes em campos ou loot natural desse bioma. Nem todo baú ou aldeia precisa conter sementes. Biomas adicionais podem ser incluídos se balanceamento ou cobertura justificar. Suporte de secagem, peneira e prensa são estações que avançam em ticks (receitas fictícias `cultivo:drying`, `cultivo:sieving` e `cultivo:pressing`). O estado persiste após salvar/reabrir o mundo.

## MVP

Escopo do MVP de 21/10/2026, todo implementado; as extensões que vieram depois estão nas seções seguintes.

- Três variedades originais identificadas por semente, nome e aparência, cobrindo perfis de ciclo e genética inspirados nas categorias de referência (ex.: automática/fotoperíodo e sativa/índica/híbrida). Brisa, Aurora e Eclipse ficaram como as variedades "nativas" do mundo. Não copiar marcas ou cultivares comerciais: as genéticas do catálogo (24/10/2026) usam nomes de paródia.
- Sementes têm níveis de raridade que influenciam sua chance de aparecer em baús e plantações de aldeias. Proposta inicial: Brisa comum, Aurora incomum e Eclipse rara; raridade afeta descoberta, não deve bloquear plantio em biomas.
- Sementes com raridade e distribuição em baús/loot de aldeias e plantações de aldeias em planícies e selvas; cultivo outdoor em qualquer lugar válido e cultivo indoor na estufinha.
- Plantio, estágios visuais, colheita, flores, qualidade diferenciada pela rota e drops de sementes.
- Recursos completos de apresentação: texturas, modelos, blockstates, ícones, partículas/sons quando necessários, tooltips e traduções pt-BR/en-US; o agente de implementação deve criar os assets originais do mod.
- Secagem, peneiração e prensagem em estações/blocos.
- Cadeia de processamento até haxixe e receita abstrata de baseado usando folha de seda + flor seca.
- Artigos de fumo craftáveis, incluindo bong, cachimbo, narguilé, chillum, papel/folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro; itens decorativos/funcionais adicionais podem ser acrescentados sem simular instruções reais.
- Traduções pt-BR/en-US e IDs persistentes. Desde 24/10/2026 o jogo mostra o português também no en_us, a pedido do dono; o inglês continua no manifesto como fonte.
- Lógica autoritativa no servidor e cliente/servidor dedicados compatíveis com o loader escolhido.

## Fora do MVP

Bedrock e outros loaders; dependência/vício; crime ou comércio de drogas; simulação de técnicas ou parâmetros reais; integração obrigatória com outros mods. O mascate que vende sementes por esmeraldas, como já vende mudas no vanilla, é só uma fonte de sementes.

## Extensão pós-MVP (pedida pelo usuário em 21/10/2026)

Mecânica fictícia de fumar: baseado, cachimbo, chillum, bong e narguilé podem ser usados (segurar o botão direito) com um isqueiro no inventário; os artigos queimam flor seca ou haxixe. O servidor aplica efeitos de jogo temporários: o indicador "Chapado" (`cultivo:high`) mais efeitos vanilla (fome, náusea curta e um perfil por variedade). A duração depende do artigo, da qualidade e do material, soma ao fumar de novo e tem limite. São só mecânicas de Minecraft, sem informação do mundo real. Regras e números em DECISIONS.md.

## Extensões pós-MVP (24/10/2026)

- **Catálogo de genéticas.** Pedido do dono, com o link do filtro High THC da Santa Semente BR: "pode ser o nome deles mesmo, ou se preferir use um nome parodia".
  - Entraram 37 genéticas com nomes de paródia, somando 40 com as nativas.
  - As do catálogo aparecem em baús de aldeia e com o mascate; as nativas continuam na planta silvestre e nas lavouras de aldeia.
- **Polimento a partir da auditoria do projeto:**
  - lavoura com 8 estágios visuais e arte por família;
  - estações em 3D que mostram se estão vazias, carregadas, trabalhando ou prontas;
  - modelos 3D dos artigos na mão;
  - os aldeões fazendeiros replantam a lavoura do mod;
  - todas as receitas aparecem no livro de receitas.
- **Todo artigo com uso:**
  - piteira → baseado com piteira (dura mais, menos náusea);
  - dichavador → melhoria da peneira (+1 resina);
  - bandeja de enrolar (3 baseados com 2 flores);
  - pote de vidro (guarda uma pilha e cura flores secas: +1 de qualidade uma vez, e a estufinha continua superior);
  - cinzeiro (junta a cinza de quem fuma por perto e vira farinha de osso).
- **Português e animações.** Pedido do dono: "deixe tudo no mod em português, também crie as animações".
  - Todo o texto do mod aparece em português em qualquer idioma.
  - Fumar tem animação própria em 1ª e 3ª pessoa.
  - As estações e os artigos têm animações, partículas e 18 eventos de som com legenda (o áudio é o vanilla).
- **Estufinha (25/10/2026).** Pedido do dono: trocar a estufa de vidro por uma estufinha craftável com luz roxa, de 2 blocos de altura, que caiba um pé de planta no tamanho normal, sem farmland e sem água. Clique direito na planta madura colhe flores e sementes com qualidade 4–5 e deixa a estufinha vazia. O vidro deixa de dar bônus (ao ar livre continua 2–3).
- **Teto de vidro (25/10/2026).** Pedido do dono: a lavoura outdoor tampada no teto com bloco de vidro com qualidade parecida com a da estufinha. Escolha dele: 3–4 sob teto de bloco de vidro (sem paredes; vidraça não conta), abaixo da estufinha (4–5), que continua superior; o crescimento não muda.

Regras e números em DECISIONS.md.

## Critérios de aceite

1. Build compila e jogo inicia na combinação exata de Minecraft, loader e Java declarada.
2. Todas as variedades (as 3 nativas e as 37 do catálogo) plantam, crescem e produzem os drops esperados em outdoor e na estufinha; as duas rotas têm diferenças de qualidade visíveis e persistentes.
3. Sementes podem ser obtidas pelas fontes de mundo documentadas; plantio outdoor funciona em qualquer bioma/local que aceite as condições de plantio definidas; a estufinha oferece uma alternativa indoor funcional.
4. Sementes com raridades distintas aparecem em baús de mais da metade dos tipos de aldeia existentes e em plantações/campos de planície e selva ou fontes equivalentes nesses biomas; testes validam fontes e pesos configurados.
5. A cadeia de processamento funciona e persiste após reload; flor seca + folha de seda crafta o baseado conforme a receita do jogo.
6. Bong e demais artigos definidos no MVP podem ser obtidos pelas receitas documentadas.
7. Saída cheia, chunk unload/reload e interações repetidas não perdem nem duplicam itens.
8. Servidor dedicado valida interações, progresso e drops; cliente não pode forjar resultados.
9. O jar carrega numa instalação limpa do loader escolhido com dependências declaradas, sem arquivos do jogo empacotados.
10. Todos os recursos visuais/audiovisuais referenciados existem, são originais e carregam sem placeholders. Os sons são eventos do próprio mod que tocam áudio vanilla, sem arquivos de áudio no jar.
