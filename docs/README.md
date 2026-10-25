# Documentação

Projeto: mod privado de cultivo e crafting para Minecraft Java 26.3 com Forge 26.3-66.0.13 (o build; o jar aceita a partir do 66.0.2), para jogar com amigos. Estado em 25/10/2026: 40 genéticas, estufa, estações animadas, artigos com uso e a mecânica fictícia de fumar, tudo em português; `build` (316 testes JUnit) e `runGameTestServer` (62 GameTests do Cultivo) passam, e o teste visual automático conferiu o cliente gráfico (47 cenas). Compatível com o Forge 66.0.2 do launcher do dono e com o Serene Seasons: os GameTests passam nessa combinação e, no GameTest que simula o single player, as lavouras do Cultivo seguem as estações como o trigo (num servidor dedicado o Serene Seasons não age em lavoura nenhuma). Faltam o teste em multiplayer do começo ao fim e a conferência do Serene Seasons em single player (veja [TODO.md](TODO.md)).

- [Orientação para a próxima sessão](NEXT_SESSION.md): estado, ambiente, verificação, mapa do código, armadilhas da API 26.3 e pendências. Comece por aqui.
- [Guia do iniciante](GUIA_INICIANTE.md): passo a passo para o dono (preparar o Windows, compilar, jogar, testar à mão, mudar números).
- [Especificação do produto](PRODUCT_SPEC.md): escopo e critérios de aceite.
- [Decisões](DECISIONS.md): pedidos do dono, regras e números de jogo, questões em aberto.
- [Pesquisa técnica](TECHNICAL_RESEARCH.md): matriz de versões fixada, versões testadas e compatibilidade, toolchain e fontes oficiais.
- [Plano e evidências](TODO.md): resultados reais de build e testes, e o que ainda falta.
- [Prompt para a nuvem](CLOUD_PROMPT.md): texto pronto para colar numa sessão nova do Claude na nuvem.
- [Catálogo de genéticas](strains-catalog.json): a lista do dono, com os nomes de paródia e, só aqui, os nomes reais da loja.

Claude: comece por [CLAUDE.md](../CLAUDE.md) e por [NEXT_SESSION.md](NEXT_SESSION.md). As versões estão fixadas em [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md); troque-as só a pedido do dono.
