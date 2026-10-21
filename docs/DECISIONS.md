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
- Escopo inicial sugerido para artigos craftáveis: baseado, bong, cachimbo, narguilé, chillum, folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro. Outros artigos podem entrar conforme necessidade do grupo. Os itens não implicam efeitos de consumo no MVP.
- Não adicionar checagem de conta nem alterar autenticação do jogo.
- Nomes de variedades e balanceamento exato continuam provisórios até protótipo.
- O agente que implementar o mod deve criar os assets originais completos (texturas, modelos, blockstates, ícones e outros recursos visuais/audiovisuais necessários), além do código e conteúdo.
- Cada incremento deve incluir testes automatizados apropriados e execução dos testes/build; atualizar TODO apenas com evidência dos resultados.

## Em aberto
- Nome/namespace; licença do código/assets caso haja compartilhamento; balanceamento de qualidade e rendimento.
- Quais tipos de aldeia da versão selecionada entram na cobertura; Claude deve inventariar as variantes existentes e implementar fontes em mais da metade delas.
- Dimensões e requisitos da estufa (luz, teto, blocos transparentes, irrigação ou controles dedicados).
- Se e como os artigos de fumo terão interação/uso; efeitos de consumo permanecem fora do MVP até decisão explícita.
- Confirmar se os amigos usam Forge ou NeoForge antes de fixar o loader no workspace.
