# O que cada print do teste visual deve mostrar

Prints de `run-probe.ps1` (pasta do `-OutDir`). "Tick" é quando o datapack monta a cena (veja `datapack/data/probe/function/tick.mcfunction`). Conferido no cliente real em 25/10/2026.

Sem F1 aparecem a mira e as legendas de som (o teste liga `showSubtitles`). Mensagens na barra de ação aparecem mesmo com o chat escondido.

| # | Nome | Tick do passo | Deve mostrar | Seria bug |
|---|---|---|---|---|
| 01 | plantas | 100 (cam_crops) | Fileiras Brisa/Aurora/Eclipse com idade 0→7 da esquerda para a direita, 8 estágios distintos (7 ≠ 6), planta silvestre na grama. Mesma câmera do print de antes. | Textura preta/roxa, estágios repetidos, cara de samambaia |
| 02 | familias | 260 | 12 colunas (x 2..13): brisa, aurora, eclipse, dourado=Manga Gelada, limao=Amnésia Neblina, gelo=Cola de Gorila #4, verde=Bruno Banner, roxo=Ultravioleta, verde_escuro=Kush do Bubba, azul=Mirtilo, creme=Caramelo Crítico, rosa=Bala Sortida. Fundo com idade 7, frente com idade 4. | Duas famílias com a mesma cor; modelo faltando |
| 03 | suporte_secagem | 420 | x20 vazio; x22 maços frescos balançando (textura animada) com farelo verde caindo; x24 maços secos; x26 girado 90° (leste, visto de lado). Um ciclo do x22 termina perto do print (progress 190): sopro + "Processo concluído". "Maços farfalham" só às vezes (a cada 80 ticks). | Cubo; giro igual; maços ausentes |
| 04 | peneira | 580 | x20 vazia; x22 flor na tela com pó âmbar caindo; x24 resina na bandeja; x26 com o dichavador montado e trabalhando. Legenda "Peneira sacode"; fim de ciclo do x22 cerca de 0,5 s antes (progress 30) dá sopro + "Processo concluído". | Dichavador invisível; sem partículas |
| 05–07 | prensa_a/b/c | 740 | x20 placa em cima; x22 placa em alturas diferentes nos 3 prints (ciclo de 40 ticks) e gotas de mel nas bordas; x24 barra de haxixe na base e placa em cima; x26 virada para o leste, trabalhando. "Prensa aperta"; fim de ciclo do x22 entre a e b (progress 160). | **Cubo preto/roxo no lugar da placa**: o modelo sintético `cultivo:press_platen` não carregou. Placa igual nos 3 prints. |
| 08 | artigos | 900 | z=2: bandeja (sul), bandeja (leste), pote vazio, pote com resina, pote x38 com flores. z=6: cinzeiros com cinza 0→4. | Pote cheio igual ao vazio; cinza sem degraus |
| 09–10 | pote_cura_a/b | +2,3 s / +2,9 s | Brilho verde (aldeão feliz) no pote x38 e legenda "Flores curadas" (`cure_progress` 2360 de 2400). | Sem brilho e sem legenda |
| 11 | estufinha | 1060 (cam_grow_tent) | Sala de pedra-negra aberta para a câmera, 3 estufinhas de 2 de altura viradas para cá: x40 vazia (vaso só com terra), x42 Brisa pequena (idade 3), x44 da família roxa madura. Lona escura, mylar lilás brilhando por dentro, painel de LEDs roxos aceso no teto de cada uma, ponto roxo no poste da frente, pó roxo caindo da lâmpada; luz roxa no chão em volta. Planta no vaso, sem atravessar as paredes. | **Cubo preto/roxo** (modelo ou textura faltando); planta fora do vaso ou atravessando a lona; LEDs apagados no escuro; a madura sem planta (renderer não registrado) |
| 12–13 | geneticas_1/2 | 1220 / 1380 | 20 colunas cada: semente (em cima), flor fresca, flor seca; famílias juntas (ordem exata nos comentários de `build.mcfunction`). Ícones de inventário (`item_display:"gui"`). | Ícone faltando; variedades da mesma família idênticas (a arte promete acento por variedade) |
| 14 | produtos | 1540 | Linha 1: 13 haxixes (sem variedade + 12 famílias, cores diferentes). Linha 2: resina, baseado, baseado com variedade, baseado com piteira, folha de seda, piteira, dichavador, isqueiro, bandeja, pote, cinzeiro, planta silvestre. Linha 3: bong, cachimbo, narguilé, chillum, suporte, peneira, prensa (3D) e o ícone plano da estufinha. | Haxixe igual em todas as famílias; piteira sem diferença |
| 15 | na_mao_3p | 1700 | Modelos "na mão" (contexto `thirdperson_righthand`): bong, narguilé, cachimbo, chillum, baseado, baseado com piteira, isqueiro. | Ícone plano no lugar do modelo na mão |
| 16 | inventario | 1860 | Inventário cheio; dica de quem estiver no centro. | Nome em inglês ou chave crua |
| 17 | dica_semente_rara | | "Sementes de Morango com Banana" + "Rara · Automática · Índica · Potência ★★★" (Rara em lilás). | ★ como quadrado; chave `tooltip.cultivo…` crua |
| 18 | dica_semente_incomum | | "Sementes de Gorilão" + "Incomum (amarelo) · Automática · Híbrida · Potência ★★★". | |
| 19 | dica_flor_curada | | "Flor Seca de Eclipse", perfil "Rara · Fotoperíodo · Índica · ★★★", "Qualidade: Excepcional" (lilás), "Cura concluída" (dourado). | |
| 20 | dica_haxixe | | "Haxixe", "Variedade: Mirtilo", "Qualidade: Ótima" (ciano). | |
| 21 | dica_baseado_piteira | | "Baseado", "Variedade: Manga Gelada", "Qualidade: Ótima", "Com piteira" (amarelo), "Segure o botão direito para fumar · precisa de um isqueiro". | |
| 22 | dica_isqueiro | | "Isqueiro" + "Necessário para fumar · gasta 1 de durabilidade a cada uso". | |
| 23 | dica_dichavador | | Texto longo quebrado em linhas, sem sair da tela. | Dica cortada fora da tela |
| 24 | dica_estacao | | "Suporte de Secagem" + "Seca flores frescas com o tempo" + uso. | |
| 25 | dica_bandeja | | "Bandeja de Enrolar" + dica. | |
| 26 | dica_baseado_sem_variedade | | "Baseado" só com a linha de fumar. | Linha "Variedade: null" |
| 27 | criativo_aba_cultivo | 2260 | Aba com ícone de baseado selecionada: estações, estufinha, planta silvestre, resina, haxixe, baseado, artigos. | Outra aba: posição calculada errada (ver riscos) |
| 28 | criativo_aba_geneticas | | Aba "Cultivo: Genéticas" (dica com o nome sob o mouse): 120 itens, nativas primeiro. | |
| 29 | criativo_dica | | Dica do 1º item: Sementes de Brisa, "Comum · Automática · Sativa · Potência ★☆☆". | |
| 30 | bandeja_enrolou | 2520 | Visto de cima: mão com 3 baseados de Eclipse; legenda "Baseado enrolado". | Flores ainda na mão |
| 31 | bandeja_recusa | 2640 | Barra de ação "Você precisa de 3 folhas de seda no inventário"; as 2 flores continuam na mão. | Sem aviso |
| 32 | suporte_inserir | 2760 | Suporte x20 com maços frescos; mão vazia (as 4 flores entraram). | Flor colocada no chão/nada; mão ainda cheia |
| 33 | suporte_retirar | | Agachado e de mãos vazias: suporte x20 vazio de novo; as flores voltaram ao inventário. | Suporte ainda com maços |
| 34 | peneira_dichavador | 2940 | Peneira x20 com o dichavador; legenda "Dichavador mói"; mão vazia. | |
| 35 | bong_primeira_pessoa | 3060 | Bong 3D pequeno na mão (não um palito) e flor seca na mão esquerda. | Palito fino (problema de antes) |
| 36 | bong_terceira_pessoa | | Visão de frente com o bong na mão. | |
| 37 | fumando_baseado_1p | 3240 | Baseado subindo à boca, chama do isqueiro e brasa (sem fios de fumaça na própria câmera), legendas "Isqueiro acende" e "Tragando". | **Item gigante** ou **mancha preta de fumaça** cobrindo a tela (os dois já aconteceram) |
| 38 | efeito_chapado | | Ícones de efeito (Chapado + os da Brisa sativa), distorção da náusea, nuvem branca, "Fumaça soprada", "Cinza cai no cinzeiro". | Fumou duas vezes (só há 1 baseado, então não deve) |
| 39 | inventario_efeitos | | Lista de efeitos no inventário em português ("Chapado"). | |
| 40 | baseado_piteira_3p | 3460 | De frente: braço erguido, baseado com piteira na boca, brasa/fumaça. | Pose da corneta vanilla |
| 41 | baforada_3p | | Fumaça branca saindo da boca. | |
| 42 | bong_3p_duas_maos | 3640 | Duas mãos no bong (mão secundária vazia), bolhas, "Bong borbulha". | Uma mão só |
| 43 | bong_1p | | Bong subindo à boca em 1ª pessoa, bolhas. | |
| 44 | recusa_sem_material | | Barra de ação "Você precisa de flor seca ou haxixe no inventário". | |
| 45 | narguile_3p | 3940 | Narguilé com as duas mãos, fumaça branca e bolhas. | |
| 46 | cachimbo_3p | 4120 | Uma mão, brasa e fumaça. | |
| 47 | chillum_3p | 4280 | Uma mão, brasa e fumaça. | |
| 48 | dica_estufinha | (com o 16) | "Estufinha" + "Clique com o botão direito:", "com semente, planta sem terra nem água", "de mão vazia, colhe a planta madura", "Luz roxa: qualidade 4 a 5". | Chave crua; linha cortada |
| 49 | estufinha_colheu | 4440 (use_tent) | Dentro da sala, de mão vazia: a estufinha x44 ficou vazia; na barra, 1 flor fresca e de 1 a 4 sementes da família roxa; legenda "Estufinha colhida". | Planta continua lá; nada na barra |
| 50 | estufinha_plantou | | Com as sementes de Brisa: muda pequena no vaso da x44; 3 sementes na barra; legenda "Semente plantada na estufinha". | Semente colocada no chão; estufinha vazia |
| — | `log-problems.txt` | fim | "nenhum". Procurar especialmente `Missing`, `Unable to load model`, `press_platen`, `Unable to play unknown soundEvent`, `Exception loading blockstate`, `lost connection` (multiplayer). | Qualquer linha em "Ligados ao Cultivo" |

**Notas:**
- A bandeja tem 2 px de altura: olhando de longe, a mira escorregava para a pedra ao lado e o clique não chegava a ela. Por isso a cena 30/31 olha de cima.
- O cinzeiro da área de fumar recebe 1 cinza por sessão bem-sucedida; narguilé, cachimbo e chillum já não acham cinzeiro livre, o que é normal.
- Avisos `Object did not get ID it asked for` no log vêm do mundo de teste antigo e são normais.
