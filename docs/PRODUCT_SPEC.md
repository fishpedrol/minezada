# Especificação do produto

## Objetivo

Mod privado para Minecraft: Java Edition, feito para jogar com amigos. O jogador encontra sementes de variedades fictícias inspiradas na diversidade de genéticas e ciclos catalogados por bancos de sementes, cultiva plantas, colhe flores e as processa por uma cadeia de crafting em etapas que termina em haxixe. Todo o cultivo e processamento são abstrações de gameplay com itens, receitas e ticks de Minecraft; não representam nem ensinam processos reais. A Santa Semente BR serve como referência de categorias e organização de variedades, não como catálogo a ser copiado.

## Alvo

Primeiro alvo: Minecraft Java Edition 26.3 com Forge, usando versões exatas e toolchain registradas em [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md). O projeto é privado para um grupo de amigos; compatibilidade com o loader usado pelo grupo pode justificar reavaliar NeoForge antes do primeiro build. Não manter dois loaders no MVP.

Bedrock fica fora do alvo. NeoForge é alternativa em avaliação, não compatibilidade prometida.

## Ciclo principal

Semente(variedade) → planta com estágios de crescimento → flor fresca → suporte de secagem → flor seca → peneira de bancada → resina fictícia → prensa manual → haxixe. A flor seca também pode ser combinada com folha de seda para craftar um baseado. O conteúdo inclui bong e outros artigos de fumo como itens craftáveis.

O jogador obtém sementes em fontes de mundo, escolhe uma variedade e pode cultivar de duas formas: outdoor em qualquer bioma e local válido para plantio, com comportamento integrado ao sistema de lavoura do trigo; ou indoor em uma estufa construída pelo jogador. Ambas as rotas permitem crescer e colher. A estufa oferece condições mais controladas e gera produto de qualidade superior; o cultivo outdoor é viável, mas tende a qualidade um pouco inferior. Qualidade é um atributo de gameplay persistente que influencia os resultados do processamento. A descoberta de sementes fica concentrada em baús de mais da metade dos tipos de aldeia existentes na versão escolhida e em plantações/campos nos biomas de planície e selva quando existirem. Se um desses biomas não gerar aldeias no jogo, incluir fonte equivalente de sementes em campos ou loot natural desse bioma. Nem todo baú ou aldeia precisa conter sementes. Biomas adicionais podem ser incluídos se balanceamento ou cobertura justificar. O suporte de secagem avança em ticks; peneira e prensa são estações de crafting. O estado persiste após salvar/reabrir o mundo.

## MVP

- Três variedades originais identificadas por semente, nome e aparência, cobrindo perfis de ciclo e genética inspirados nas categorias de referência (ex.: automática/fotoperíodo e sativa/índica/híbrida). Brisa, Aurora e Eclipse são nomes provisórios. Não copiar marcas ou cultivares comerciais.
- Sementes têm níveis de raridade que influenciam sua chance de aparecer em baús e plantações de aldeias. Proposta inicial: Brisa comum, Aurora incomum e Eclipse rara; raridade afeta descoberta, não deve bloquear plantio em biomas.
- Sementes com raridade e distribuição em baús/loot de aldeias e plantações de aldeias em planícies e selvas; cultivo outdoor em qualquer lugar válido e cultivo indoor em estufa.
- Plantio, estágios visuais, colheita, flores, qualidade diferenciada pela rota e drops de sementes.
- Recursos completos de apresentação: texturas, modelos, blockstates, ícones, partículas/sons quando necessários, tooltips e traduções pt-BR/en-US; o agente de implementação deve criar os assets originais do mod.
- Secagem, peneiração e prensagem em estações/blocos.
- Cadeia de processamento até haxixe e receita abstrata de baseado usando folha de seda + flor seca.
- Artigos de fumo craftáveis, incluindo bong, cachimbo, narguilé, chillum, papel/folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro; itens decorativos/funcionais adicionais podem ser acrescentados sem simular instruções reais.
- Traduções pt-BR/en-US e IDs persistentes.
- Lógica autoritativa no servidor e cliente/servidor dedicados compatíveis com o loader escolhido.

## Fora do MVP

Bedrock e outros loaders; efeitos de consumo/intoxicação/dependência; crime/comércio; simulação de técnicas ou parâmetros reais; integração obrigatória com outros mods. Os artigos de fumo são itens de gameplay; efeitos de consumo ficam fora do MVP.

## Critérios de aceite

1. Build compila e jogo inicia na combinação exata de Minecraft, loader e Java declarada.
2. As três variedades plantam, crescem e produzem os drops esperados em outdoor e estufa; as duas rotas têm diferenças de qualidade visíveis e persistentes.
3. Sementes podem ser obtidas pelas fontes de mundo documentadas; plantio outdoor funciona em qualquer bioma/local que aceite as condições de plantio definidas; a estufa oferece uma alternativa indoor funcional.
4. Sementes com raridades distintas aparecem em baús de mais da metade dos tipos de aldeia existentes e em plantações/campos de planície e selva ou fontes equivalentes nesses biomas; testes validam fontes e pesos configurados.
5. A cadeia de processamento funciona e persiste após reload; flor seca + folha de seda crafta o baseado conforme a receita do jogo.
6. Bong e demais artigos definidos no MVP podem ser obtidos pelas receitas documentadas.
7. Saída cheia, chunk unload/reload e interações repetidas não perdem nem duplicam itens.
8. Servidor dedicado valida interações, progresso e drops; cliente não pode forjar resultados.
9. O jar carrega numa instalação limpa do loader escolhido com dependências declaradas, sem arquivos do jogo empacotados.
10. Todos os recursos visuais/audiovisuais referenciados existem, são originais e carregam sem placeholders.
