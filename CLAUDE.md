# Guia para Claude

Este repositório começa do zero para um mod privado de Minecraft Java. Leia antes de codar:
1. [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) — requisitos e aceite.
2. [docs/DECISIONS.md](docs/DECISIONS.md) — escolhas e pendências técnicas.
3. [docs/TECHNICAL_RESEARCH.md](docs/TECHNICAL_RESEARCH.md) — fontes oficiais, versões e notas de compatibilidade.
4. [docs/TODO.md](docs/TODO.md) — próxima tarefa.

Antes do setup, use Forge como recomendado salvo incompatibilidade necessária com mods do grupo. Consulte [docs/TECHNICAL_RESEARCH.md](docs/TECHNICAL_RESEARCH.md) e confirme as versões atuais de Minecraft, loader e JDK nas fontes oficiais. Se as páginas divergirem, use o MDK oficial efetivamente baixado como fonte para as versões do projeto e atualize TECHNICAL_RESEARCH.md. Use o MDK do loader escolhido e examine seus arquivos antes de alterar build logic, plugins ou APIs. O MDK específico pode divergir de páginas genéricas.

Implemente cultivo outdoor em qualquer bioma/local válido, sem filtro de bioma e com comportamento próximo à lavoura de trigo. Distribua sementes com raridades distintas em baús de mais da metade dos tipos de aldeia existentes na versão escolhida. Inclua também plantações/campos de planície e selva onde existam; se não houver aldeias nesses biomas, crie fonte equivalente de campo/loot natural. Registre as variantes cobertas e os pesos conforme PRODUCT_SPEC.md e DECISIONS.md. Inclua também a rota de estufa e sua qualidade superior. Crie os assets originais completos necessários ao mod — texturas, modelos, blockstates, ícones, traduções e quaisquer sons/partículas usados — em vez de deixar placeholders. Faça testes automatizados para as regras principais de cultivo, qualidade, fontes de sementes, processamento e receitas dos artigos de fumo; rode os testes e o build aplicável e registre os resultados reais no TODO. Implemente todo resultado de cultivo e crafting no servidor. Trate secagem, peneiração e prensagem como receitas ficcionais por ticks de jogo. Inclua a receita de baseado com folha de seda e flor seca e os artigos do escopo MVP (bong, cachimbo, narguilé, chillum, folha de seda, piteira, dichavador, isqueiro, bandeja, pote e cinzeiro), todos com receitas e assets originais; efeitos de consumo não fazem parte do MVP. Não inclua instruções utilizáveis de extração real nem altere autenticação/licença do jogo.

Atualize TODO somente após evidência. Ao concluir, informe arquivos alterados, versão exata, comandos de build/teste executados e pendências.
