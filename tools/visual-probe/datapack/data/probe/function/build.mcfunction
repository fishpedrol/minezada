# Plataforma no alto (y=150), longe do relevo, com luz do dia.
fill 0 150 0 47 150 47 minecraft:smooth_stone
fill 0 151 0 47 160 47 minecraft:air
# A) Nativas nos 8 estagios (x 2..9 = idade 0..7), como no print de antes.
fill 2 150 2 9 150 2 minecraft:farmland[moisture=7]
setblock 2 151 2 cultivo:brisa_crop[age=0]
setblock 3 151 2 cultivo:brisa_crop[age=1]
setblock 4 151 2 cultivo:brisa_crop[age=2]
setblock 5 151 2 cultivo:brisa_crop[age=3]
setblock 6 151 2 cultivo:brisa_crop[age=4]
setblock 7 151 2 cultivo:brisa_crop[age=5]
setblock 8 151 2 cultivo:brisa_crop[age=6]
setblock 9 151 2 cultivo:brisa_crop[age=7]
fill 2 150 3 9 150 3 minecraft:water
fill 2 150 4 9 150 4 minecraft:farmland[moisture=7]
setblock 2 151 4 cultivo:aurora_crop[age=0]
setblock 3 151 4 cultivo:aurora_crop[age=1]
setblock 4 151 4 cultivo:aurora_crop[age=2]
setblock 5 151 4 cultivo:aurora_crop[age=3]
setblock 6 151 4 cultivo:aurora_crop[age=4]
setblock 7 151 4 cultivo:aurora_crop[age=5]
setblock 8 151 4 cultivo:aurora_crop[age=6]
setblock 9 151 4 cultivo:aurora_crop[age=7]
fill 2 150 5 9 150 5 minecraft:water
fill 2 150 6 9 150 6 minecraft:farmland[moisture=7]
setblock 2 151 6 cultivo:eclipse_crop[age=0]
setblock 3 151 6 cultivo:eclipse_crop[age=1]
setblock 4 151 6 cultivo:eclipse_crop[age=2]
setblock 5 151 6 cultivo:eclipse_crop[age=3]
setblock 6 151 6 cultivo:eclipse_crop[age=4]
setblock 7 151 6 cultivo:eclipse_crop[age=5]
setblock 8 151 6 cultivo:eclipse_crop[age=6]
setblock 9 151 6 cultivo:eclipse_crop[age=7]
# Planta silvestre na grama.
fill 11 150 1 12 150 3 minecraft:grass_block
setblock 11 151 2 cultivo:wild_plant
setblock 12 151 2 cultivo:wild_plant
# B) Uma variedade de cada familia de cor: z=14 madura (idade 7), z=16 no meio (idade 4).
# x = 2.. : brisa=brisa, aurora=aurora, eclipse=eclipse, dourado=manga_gelada, limao=amnesia_neblina, gelo=cola_de_gorila, verde=bruno_banner, roxo=ultravioleta, verde_escuro=kush_do_bubba, azul=mirtilo, creme=caramelo_critico, rosa=bala_sortida
fill 2 150 14 13 150 14 minecraft:farmland[moisture=7]
fill 2 150 15 13 150 15 minecraft:water
fill 2 150 16 13 150 16 minecraft:farmland[moisture=7]
setblock 2 151 14 cultivo:brisa_crop[age=7]
setblock 2 151 16 cultivo:brisa_crop[age=4]
setblock 3 151 14 cultivo:aurora_crop[age=7]
setblock 3 151 16 cultivo:aurora_crop[age=4]
setblock 4 151 14 cultivo:eclipse_crop[age=7]
setblock 4 151 16 cultivo:eclipse_crop[age=4]
setblock 5 151 14 cultivo:manga_gelada_crop[age=7]
setblock 5 151 16 cultivo:manga_gelada_crop[age=4]
setblock 6 151 14 cultivo:amnesia_neblina_crop[age=7]
setblock 6 151 16 cultivo:amnesia_neblina_crop[age=4]
setblock 7 151 14 cultivo:cola_de_gorila_crop[age=7]
setblock 7 151 16 cultivo:cola_de_gorila_crop[age=4]
setblock 8 151 14 cultivo:bruno_banner_crop[age=7]
setblock 8 151 16 cultivo:bruno_banner_crop[age=4]
setblock 9 151 14 cultivo:ultravioleta_crop[age=7]
setblock 9 151 16 cultivo:ultravioleta_crop[age=4]
setblock 10 151 14 cultivo:kush_do_bubba_crop[age=7]
setblock 10 151 16 cultivo:kush_do_bubba_crop[age=4]
setblock 11 151 14 cultivo:mirtilo_crop[age=7]
setblock 11 151 16 cultivo:mirtilo_crop[age=4]
setblock 12 151 14 cultivo:caramelo_critico_crop[age=7]
setblock 12 151 16 cultivo:caramelo_critico_crop[age=4]
setblock 13 151 14 cultivo:bala_sortida_crop[age=7]
setblock 13 151 16 cultivo:bala_sortida_crop[age=4]
# C) Estacoes, viradas para a camera (sul): x=20 vazia, 22 trabalhando, 24 pronta, 26 variante.
# O contents vem dos slots (a BlockEntity acerta o blockstate a cada tick), por isso os itens vao no NBT.
# progress alinha um fim de ciclo (sopro + legenda) com o print: secagem 600 ticks, peneira 200, prensa 300.
setblock 20 151 2 cultivo:drying_rack[facing=south]
setblock 22 151 2 cultivo:drying_rack[facing=south]{Items:[{Slot:0b,id:"cultivo:aurora_fresh_flower",count:64,components:{"cultivo:quality":3}}],progress:190}
setblock 24 151 2 cultivo:drying_rack[facing=south]{Items:[{Slot:1b,id:"cultivo:aurora_dried_flower",count:8,components:{"cultivo:quality":3}}]}
setblock 26 151 2 cultivo:drying_rack[facing=east]{Items:[{Slot:0b,id:"cultivo:manga_gelada_fresh_flower",count:64,components:{"cultivo:quality":2}}]}
setblock 20 151 9 cultivo:sieve[facing=south]
setblock 22 151 9 cultivo:sieve[facing=south]{Items:[{Slot:0b,id:"cultivo:eclipse_dried_flower",count:64,components:{"cultivo:quality":4}}],progress:30}
setblock 24 151 9 cultivo:sieve[facing=south]{Items:[{Slot:1b,id:"cultivo:resin",count:6,components:{"cultivo:quality":4,"cultivo:strain":"eclipse"}}]}
setblock 26 151 9 cultivo:sieve[facing=south,grinder=true]{Items:[{Slot:0b,id:"cultivo:manga_gelada_dried_flower",count:64,components:{"cultivo:quality":3}}]}
setblock 20 151 16 cultivo:press[facing=south]
setblock 22 151 16 cultivo:press[facing=south]{Items:[{Slot:0b,id:"cultivo:resin",count:64,components:{"cultivo:quality":3,"cultivo:strain":"aurora"}}],progress:160}
setblock 24 151 16 cultivo:press[facing=south]{Items:[{Slot:1b,id:"cultivo:hash",count:3,components:{"cultivo:quality":4,"cultivo:strain":"eclipse"}}]}
setblock 26 151 16 cultivo:press[facing=east]{Items:[{Slot:0b,id:"cultivo:resin",count:64,components:{"cultivo:quality":3}}]}
# D) Artigos colocaveis: z=2 bandeja (sul), bandeja (leste), pote vazio, pote com resina (nao cura);
# o pote que cura e colocado em cam_articles (x=38). z=6: cinzeiros com cinza 0..4.
setblock 30 151 2 cultivo:rolling_tray[facing=south]
setblock 32 151 2 cultivo:rolling_tray[facing=east]
setblock 34 151 2 cultivo:jar
setblock 36 151 2 cultivo:jar[filled=true]{Items:[{Slot:0b,id:"cultivo:resin",count:32,components:{"cultivo:quality":3}}]}
setblock 30 151 6 cultivo:ashtray[ash=0]
setblock 32 151 6 cultivo:ashtray[ash=1]
setblock 34 151 6 cultivo:ashtray[ash=2]
setblock 36 151 6 cultivo:ashtray[ash=3]
setblock 38 151 6 cultivo:ashtray[ash=4]
# E) Estufa: caixa de vidro (chao de vidro em y=150) com nativas e duas do catalogo, maduras.
fill 39 150 10 45 154 16 minecraft:glass hollow
fill 40 150 13 44 150 13 minecraft:farmland[moisture=7]
fill 40 150 12 44 150 12 minecraft:water
setblock 40 151 13 cultivo:brisa_crop[age=7]
setblock 41 151 13 cultivo:manga_gelada_crop[age=7]
setblock 42 151 13 cultivo:aurora_crop[age=7]
setblock 43 151 13 cultivo:ultravioleta_crop[age=7]
setblock 44 151 13 cultivo:eclipse_crop[age=7]
# F) Vitrines (item_display sempre virado para a camera). Geneticas: uma coluna por variedade, de cima
# para baixo semente, flor fresca e flor seca, agrupadas por familia; duas fotos.
# Foto 1: brisa, aurora, eclipse, manga_gelada, ouro_colombiano, ouro_18k, kush_de_manga, bananona, amnesia_neblina, jack_heroi, ligeirinha, cao_quimico, cola_de_gorila, ak_48, bolo_de_casamento, luzes_do_norte, viuva_branca, bruno_banner, tri_campea, viagem_25
summon minecraft:item_display 1.5 153.9 30.5 {item:{id:"cultivo:brisa_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 1.5 153.2 30.5 {item:{id:"cultivo:brisa_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 1.5 152.5 30.5 {item:{id:"cultivo:brisa_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.1 153.9 30.5 {item:{id:"cultivo:aurora_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.1 153.2 30.5 {item:{id:"cultivo:aurora_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.1 152.5 30.5 {item:{id:"cultivo:aurora_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.7 153.9 30.5 {item:{id:"cultivo:eclipse_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.7 153.2 30.5 {item:{id:"cultivo:eclipse_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 2.7 152.5 30.5 {item:{id:"cultivo:eclipse_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.3 153.9 30.5 {item:{id:"cultivo:manga_gelada_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.3 153.2 30.5 {item:{id:"cultivo:manga_gelada_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.3 152.5 30.5 {item:{id:"cultivo:manga_gelada_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.9 153.9 30.5 {item:{id:"cultivo:ouro_colombiano_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.9 153.2 30.5 {item:{id:"cultivo:ouro_colombiano_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 3.9 152.5 30.5 {item:{id:"cultivo:ouro_colombiano_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 4.5 153.9 30.5 {item:{id:"cultivo:ouro_18k_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 4.5 153.2 30.5 {item:{id:"cultivo:ouro_18k_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 4.5 152.5 30.5 {item:{id:"cultivo:ouro_18k_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.1 153.9 30.5 {item:{id:"cultivo:kush_de_manga_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.1 153.2 30.5 {item:{id:"cultivo:kush_de_manga_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.1 152.5 30.5 {item:{id:"cultivo:kush_de_manga_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.7 153.9 30.5 {item:{id:"cultivo:bananona_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.7 153.2 30.5 {item:{id:"cultivo:bananona_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 5.7 152.5 30.5 {item:{id:"cultivo:bananona_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.3 153.9 30.5 {item:{id:"cultivo:amnesia_neblina_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.3 153.2 30.5 {item:{id:"cultivo:amnesia_neblina_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.3 152.5 30.5 {item:{id:"cultivo:amnesia_neblina_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.9 153.9 30.5 {item:{id:"cultivo:jack_heroi_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.9 153.2 30.5 {item:{id:"cultivo:jack_heroi_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 6.9 152.5 30.5 {item:{id:"cultivo:jack_heroi_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 7.5 153.9 30.5 {item:{id:"cultivo:ligeirinha_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 7.5 153.2 30.5 {item:{id:"cultivo:ligeirinha_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 7.5 152.5 30.5 {item:{id:"cultivo:ligeirinha_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.1 153.9 30.5 {item:{id:"cultivo:cao_quimico_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.1 153.2 30.5 {item:{id:"cultivo:cao_quimico_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.1 152.5 30.5 {item:{id:"cultivo:cao_quimico_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.7 153.9 30.5 {item:{id:"cultivo:cola_de_gorila_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.7 153.2 30.5 {item:{id:"cultivo:cola_de_gorila_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 8.7 152.5 30.5 {item:{id:"cultivo:cola_de_gorila_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.3 153.9 30.5 {item:{id:"cultivo:ak_48_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.3 153.2 30.5 {item:{id:"cultivo:ak_48_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.3 152.5 30.5 {item:{id:"cultivo:ak_48_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.9 153.9 30.5 {item:{id:"cultivo:bolo_de_casamento_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.9 153.2 30.5 {item:{id:"cultivo:bolo_de_casamento_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 9.9 152.5 30.5 {item:{id:"cultivo:bolo_de_casamento_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 10.5 153.9 30.5 {item:{id:"cultivo:luzes_do_norte_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 10.5 153.2 30.5 {item:{id:"cultivo:luzes_do_norte_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 10.5 152.5 30.5 {item:{id:"cultivo:luzes_do_norte_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.1 153.9 30.5 {item:{id:"cultivo:viuva_branca_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.1 153.2 30.5 {item:{id:"cultivo:viuva_branca_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.1 152.5 30.5 {item:{id:"cultivo:viuva_branca_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.7 153.9 30.5 {item:{id:"cultivo:bruno_banner_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.7 153.2 30.5 {item:{id:"cultivo:bruno_banner_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 11.7 152.5 30.5 {item:{id:"cultivo:bruno_banner_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.3 153.9 30.5 {item:{id:"cultivo:tri_campea_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.3 153.2 30.5 {item:{id:"cultivo:tri_campea_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.3 152.5 30.5 {item:{id:"cultivo:tri_campea_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.9 153.9 30.5 {item:{id:"cultivo:viagem_25_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.9 153.2 30.5 {item:{id:"cultivo:viagem_25_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 12.9 152.5 30.5 {item:{id:"cultivo:viagem_25_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
# Foto 2: ultravioleta, kush_roxinha, neblina_roxa, soco_roxo, do_si_do, viagem_lisergica, kush_do_bubba, gorilao, mirtilo, andarilho_do_ceu, azul_arroxeado, gelato_azul_42, caramelo_critico, creme_de_caramelo, bala_sortida, confeitez, morango_com_banana, frutinha_veloz, chiclete, morango_azedo
summon minecraft:item_display 16.5 153.9 30.5 {item:{id:"cultivo:ultravioleta_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 16.5 153.2 30.5 {item:{id:"cultivo:ultravioleta_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 16.5 152.5 30.5 {item:{id:"cultivo:ultravioleta_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.1 153.9 30.5 {item:{id:"cultivo:kush_roxinha_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.1 153.2 30.5 {item:{id:"cultivo:kush_roxinha_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.1 152.5 30.5 {item:{id:"cultivo:kush_roxinha_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.7 153.9 30.5 {item:{id:"cultivo:neblina_roxa_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.7 153.2 30.5 {item:{id:"cultivo:neblina_roxa_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 17.7 152.5 30.5 {item:{id:"cultivo:neblina_roxa_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.3 153.9 30.5 {item:{id:"cultivo:soco_roxo_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.3 153.2 30.5 {item:{id:"cultivo:soco_roxo_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.3 152.5 30.5 {item:{id:"cultivo:soco_roxo_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.9 153.9 30.5 {item:{id:"cultivo:do_si_do_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.9 153.2 30.5 {item:{id:"cultivo:do_si_do_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 18.9 152.5 30.5 {item:{id:"cultivo:do_si_do_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 19.5 153.9 30.5 {item:{id:"cultivo:viagem_lisergica_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 19.5 153.2 30.5 {item:{id:"cultivo:viagem_lisergica_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 19.5 152.5 30.5 {item:{id:"cultivo:viagem_lisergica_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.1 153.9 30.5 {item:{id:"cultivo:kush_do_bubba_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.1 153.2 30.5 {item:{id:"cultivo:kush_do_bubba_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.1 152.5 30.5 {item:{id:"cultivo:kush_do_bubba_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.7 153.9 30.5 {item:{id:"cultivo:gorilao_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.7 153.2 30.5 {item:{id:"cultivo:gorilao_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 20.7 152.5 30.5 {item:{id:"cultivo:gorilao_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.3 153.9 30.5 {item:{id:"cultivo:mirtilo_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.3 153.2 30.5 {item:{id:"cultivo:mirtilo_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.3 152.5 30.5 {item:{id:"cultivo:mirtilo_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.9 153.9 30.5 {item:{id:"cultivo:andarilho_do_ceu_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.9 153.2 30.5 {item:{id:"cultivo:andarilho_do_ceu_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 21.9 152.5 30.5 {item:{id:"cultivo:andarilho_do_ceu_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 22.5 153.9 30.5 {item:{id:"cultivo:azul_arroxeado_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 22.5 153.2 30.5 {item:{id:"cultivo:azul_arroxeado_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 22.5 152.5 30.5 {item:{id:"cultivo:azul_arroxeado_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.1 153.9 30.5 {item:{id:"cultivo:gelato_azul_42_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.1 153.2 30.5 {item:{id:"cultivo:gelato_azul_42_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.1 152.5 30.5 {item:{id:"cultivo:gelato_azul_42_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.7 153.9 30.5 {item:{id:"cultivo:caramelo_critico_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.7 153.2 30.5 {item:{id:"cultivo:caramelo_critico_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 23.7 152.5 30.5 {item:{id:"cultivo:caramelo_critico_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.3 153.9 30.5 {item:{id:"cultivo:creme_de_caramelo_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.3 153.2 30.5 {item:{id:"cultivo:creme_de_caramelo_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.3 152.5 30.5 {item:{id:"cultivo:creme_de_caramelo_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.9 153.9 30.5 {item:{id:"cultivo:bala_sortida_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.9 153.2 30.5 {item:{id:"cultivo:bala_sortida_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 24.9 152.5 30.5 {item:{id:"cultivo:bala_sortida_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 25.5 153.9 30.5 {item:{id:"cultivo:confeitez_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 25.5 153.2 30.5 {item:{id:"cultivo:confeitez_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 25.5 152.5 30.5 {item:{id:"cultivo:confeitez_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.1 153.9 30.5 {item:{id:"cultivo:morango_com_banana_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.1 153.2 30.5 {item:{id:"cultivo:morango_com_banana_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.1 152.5 30.5 {item:{id:"cultivo:morango_com_banana_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.7 153.9 30.5 {item:{id:"cultivo:frutinha_veloz_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.7 153.2 30.5 {item:{id:"cultivo:frutinha_veloz_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 26.7 152.5 30.5 {item:{id:"cultivo:frutinha_veloz_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.3 153.9 30.5 {item:{id:"cultivo:chiclete_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.3 153.2 30.5 {item:{id:"cultivo:chiclete_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.3 152.5 30.5 {item:{id:"cultivo:chiclete_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.9 153.9 30.5 {item:{id:"cultivo:morango_azedo_seeds",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.9 153.2 30.5 {item:{id:"cultivo:morango_azedo_fresh_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 27.9 152.5 30.5 {item:{id:"cultivo:morango_azedo_dried_flower",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
# Produtos e artigos (icone de inventario). Linha 1: haxixe sem variedade e de cada familia.
summon minecraft:item_display 31.5 153.9 30.5 {item:{id:"cultivo:hash",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.1 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"brisa"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.7 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"aurora"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.3 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"eclipse"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.9 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"manga_gelada"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 34.5 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"amnesia_neblina"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 35.1 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"cola_de_gorila"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 35.7 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"bruno_banner"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 36.3 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"ultravioleta"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 36.9 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"kush_do_bubba"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 37.5 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"mirtilo"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 38.1 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"caramelo_critico"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 38.7 153.9 30.5 {item:{id:"cultivo:hash",count:1,components:{"cultivo:quality":4,"cultivo:strain":"bala_sortida"}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 31.5 153.2 30.5 {item:{id:"cultivo:resin",count:1,components:{"cultivo:quality":3}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.1 153.2 30.5 {item:{id:"cultivo:joint",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.7 153.2 30.5 {item:{id:"cultivo:joint",count:1,components:{"cultivo:strain":"manga_gelada","cultivo:quality":4}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.3 153.2 30.5 {item:{id:"cultivo:joint",count:1,components:{"cultivo:strain":"manga_gelada","cultivo:quality":4,"cultivo:filtered":{}}},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.9 153.2 30.5 {item:{id:"cultivo:rolling_paper",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 34.5 153.2 30.5 {item:{id:"cultivo:filter_tip",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 35.1 153.2 30.5 {item:{id:"cultivo:grinder",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 35.7 153.2 30.5 {item:{id:"cultivo:lighter",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 36.3 153.2 30.5 {item:{id:"cultivo:rolling_tray",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 36.9 153.2 30.5 {item:{id:"cultivo:jar",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 37.5 153.2 30.5 {item:{id:"cultivo:ashtray",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 38.1 153.2 30.5 {item:{id:"cultivo:wild_plant",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 31.5 152.5 30.5 {item:{id:"cultivo:bong",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.1 152.5 30.5 {item:{id:"cultivo:pipe",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 32.7 152.5 30.5 {item:{id:"cultivo:hookah",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.3 152.5 30.5 {item:{id:"cultivo:chillum",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 33.9 152.5 30.5 {item:{id:"cultivo:drying_rack",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 34.5 152.5 30.5 {item:{id:"cultivo:sieve",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
summon minecraft:item_display 35.1 152.5 30.5 {item:{id:"cultivo:press",count:1},item_display:"gui",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]}}
# Modelos na mao (contexto thirdperson_righthand), maiores.
summon minecraft:item_display 31.5 152.4 42.5 {item:{id:"cultivo:bong",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 32.7 152.4 42.5 {item:{id:"cultivo:hookah",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 33.9 152.4 42.5 {item:{id:"cultivo:pipe",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 35.1 152.4 42.5 {item:{id:"cultivo:chillum",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 36.3 152.4 42.5 {item:{id:"cultivo:joint",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 37.5 152.4 42.5 {item:{id:"cultivo:joint",count:1,components:{"cultivo:filtered":{}}},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
summon minecraft:item_display 38.7 152.4 42.5 {item:{id:"cultivo:lighter",count:1},item_display:"thirdperson_righthand",billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
# G) Area de fumar: o jogador fica em 10.5 151 42.5 olhando para o norte; cinzeiro vazio a direita.
setblock 12 151 42 cultivo:ashtray[ash=0]
