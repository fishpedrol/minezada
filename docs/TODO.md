# Plano do mod

Estado: pendente. Ainda não existe código do mod.

## 1. Confirmar loader e criar base
- [ ] Confirmar com o grupo se todos usarão Forge ou se algum mod indispensável exige NeoForge; seguir a recomendação atual de Forge se não houver dependência de grupo.
- [ ] Rever [TECHNICAL_RESEARCH.md](TECHNICAL_RESEARCH.md) e conferir o release estável/Minecraft e MDK oficiais do loader escolhido.
- [ ] Obter o MDK e registrar a combinação realmente selecionada (Minecraft, loader, JDK, Gradle e plugin) antes do primeiro build.
- [ ] Inspecionar build.gradle, settings.gradle, gradle.properties e exemplos do próprio MDK.
- [ ] Confirmar JDK/toolchain dentro do MDK e seguir a configuração mais específica para a versão alvo; resolver diferenças com o guia genérico pelas fontes oficiais.
- [ ] Definir mod ID/namespace provisório e substituir todos os examplemod.
- [ ] Gerar configurações de execução para IDE e compilar.
- [ ] Iniciar cliente de desenvolvimento e servidor dedicado.
**Gate:** build reproduzível e ambos iniciam na matriz Minecraft/loader/Java documentada.

## 2. Conteúdo e cultivo
- [ ] Implementar plantio outdoor em qualquer bioma/local válido usando comportamento de lavoura vanilla, sem filtro de bioma.
- [ ] Distribuir sementes raras em baús de mais da metade dos tipos de aldeia da versão alvo. Adicioná-las também em plantações/campos de planície e selva onde existam, ou em fontes equivalentes nesses biomas; registrar a cobertura e os pesos de loot.
- [ ] Definir requisitos de construção da estufa e bônus moderado de qualidade.
- [ ] Registrar três variedades fictícias, sementes, plantas, itens e blocos com IDs estáveis usando o padrão do MDK atual.
- [ ] Implementar plantio outdoor integrado à lavoura vanilla e plantio indoor na estufa, estágios, qualidade persistente e colheita.
- [ ] Criar assets originais completos: texturas, modelos, blockstates, ícones, traduções e sons/partículas usados, sem placeholders.
- [ ] Criar e executar testes automatizados para crescimento, plantio outdoor sem restrição de bioma, qualidade por rota, raridades e aquisição de sementes em baús/plantios de aldeia e receitas dos artigos de fumo.
- [ ] Validar e persistir mudanças no servidor.
**Gate:** ciclo funciona em mundo novo e depois de salvar/reabrir; testes de regras de cultivo e distribuição de sementes passam; todos os recursos referenciados existem e carregam sem placeholders.

## 3. Processamento
- [ ] Suporte de secagem com progresso por ticks.
- [ ] Peneira: flor seca → resina fictícia.
- [ ] Prensa: resina fictícia → haxixe.
- [ ] Adicionar receita do baseado: folha de seda + flor seca → baseado.
- [ ] Criar receitas para bong, cachimbo, narguilé, chillum, dichavador, folha de seda, piteira, isqueiro, bandeja, pote e cinzeiro, com assets próprios.
- [ ] Resolver saída cheia, unload/reload e ações repetidas sem perda/duplicação.
**Gate:** cadeia completa e receitas de artigos de fumo funcionam em single-player e servidor dedicado com resultado autoritativo.

## 4. Conteúdo de release
- [ ] Conferir se todos os assets referenciados estão incluídos no jar e se os avisos de licença de terceiros estão corretos.
- [ ] Rodar testes automatizados, build, cliente e servidor dedicado no loader escolhido; corrigir falhas antes de marcar gates como concluídos.
- [ ] Escolher licença/avisos de terceiros.
- [ ] Inspecionar jar; declarar versões realmente testadas e instruções de instalação.
**Gate:** critérios de PRODUCT_SPEC passam e release não contém arquivos proprietários do Minecraft.
