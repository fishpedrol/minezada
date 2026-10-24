# Plataforma no alto (y=150), longe do relevo, com luz do dia.
fill 0 150 0 30 150 30 minecraft:smooth_stone
fill 0 151 0 30 160 30 minecraft:air
fill 2 150 2 9 150 2 minecraft:farmland[moisture=7]
setblock 2 151 2 cultivo:brisa_crop[age=0]
setblock 3 151 2 cultivo:brisa_crop[age=1]
setblock 4 151 2 cultivo:brisa_crop[age=2]
setblock 5 151 2 cultivo:brisa_crop[age=3]
setblock 6 151 2 cultivo:brisa_crop[age=4]
setblock 7 151 2 cultivo:brisa_crop[age=5]
setblock 8 151 2 cultivo:brisa_crop[age=6]
setblock 9 151 2 cultivo:brisa_crop[age=7]
fill 2 150 4 9 150 4 minecraft:farmland[moisture=7]
setblock 2 151 4 cultivo:aurora_crop[age=0]
setblock 3 151 4 cultivo:aurora_crop[age=1]
setblock 4 151 4 cultivo:aurora_crop[age=2]
setblock 5 151 4 cultivo:aurora_crop[age=3]
setblock 6 151 4 cultivo:aurora_crop[age=4]
setblock 7 151 4 cultivo:aurora_crop[age=5]
setblock 8 151 4 cultivo:aurora_crop[age=6]
setblock 9 151 4 cultivo:aurora_crop[age=7]
fill 2 150 6 9 150 6 minecraft:farmland[moisture=7]
setblock 2 151 6 cultivo:eclipse_crop[age=0]
setblock 3 151 6 cultivo:eclipse_crop[age=1]
setblock 4 151 6 cultivo:eclipse_crop[age=2]
setblock 5 151 6 cultivo:eclipse_crop[age=3]
setblock 6 151 6 cultivo:eclipse_crop[age=4]
setblock 7 151 6 cultivo:eclipse_crop[age=5]
setblock 8 151 6 cultivo:eclipse_crop[age=6]
setblock 9 151 6 cultivo:eclipse_crop[age=7]
# Agua para a terra arada ficar hidratada na foto.
fill 2 150 3 9 150 3 minecraft:water
fill 2 150 5 9 150 5 minecraft:water
# Planta silvestre na grama.
fill 11 150 1 12 150 3 minecraft:grass_block
setblock 11 151 2 cultivo:wild_plant
setblock 12 151 2 cultivo:wild_plant
# Estacoes de processamento.
setblock 14 151 2 cultivo:drying_rack
setblock 16 151 2 cultivo:sieve
setblock 18 151 2 cultivo:press
# Estufa: caixa de vidro com uma planta madura de cada variedade dentro.
fill 21 151 1 27 154 7 minecraft:glass hollow
fill 22 150 4 26 150 4 minecraft:farmland[moisture=7]
setblock 22 151 4 cultivo:brisa_crop[age=7]
setblock 24 151 4 cultivo:aurora_crop[age=7]
setblock 26 151 4 cultivo:eclipse_crop[age=7]
fill 22 150 3 26 150 3 minecraft:water
# Vitrine de itens (item_display sempre virado para a camera).
summon minecraft:item_display 2.5 153.6 14.5 {item:{id:"cultivo:brisa_seeds",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 3.5 153.6 14.5 {item:{id:"cultivo:aurora_seeds",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 4.5 153.6 14.5 {item:{id:"cultivo:eclipse_seeds",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 5.5 153.6 14.5 {item:{id:"cultivo:brisa_fresh_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 6.5 153.6 14.5 {item:{id:"cultivo:aurora_fresh_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 7.5 153.6 14.5 {item:{id:"cultivo:eclipse_fresh_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 8.5 153.6 14.5 {item:{id:"cultivo:brisa_dried_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 9.5 153.6 14.5 {item:{id:"cultivo:aurora_dried_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 10.5 153.6 14.5 {item:{id:"cultivo:eclipse_dried_flower",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 2.5 152.6 14.5 {item:{id:"cultivo:resin",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 3.5 152.6 14.5 {item:{id:"cultivo:hash",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 4.5 152.6 14.5 {item:{id:"cultivo:joint",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 5.5 152.6 14.5 {item:{id:"cultivo:rolling_paper",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 6.5 152.6 14.5 {item:{id:"cultivo:filter_tip",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 7.5 152.6 14.5 {item:{id:"cultivo:grinder",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 8.5 152.6 14.5 {item:{id:"cultivo:lighter",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 9.5 152.6 14.5 {item:{id:"cultivo:rolling_tray",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 10.5 152.6 14.5 {item:{id:"cultivo:jar",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 2.5 151.6 14.5 {item:{id:"cultivo:ashtray",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 3.5 151.6 14.5 {item:{id:"cultivo:bong",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 4.5 151.6 14.5 {item:{id:"cultivo:pipe",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 5.5 151.6 14.5 {item:{id:"cultivo:hookah",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 6.5 151.6 14.5 {item:{id:"cultivo:chillum",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 7.5 151.6 14.5 {item:{id:"cultivo:drying_rack",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 8.5 151.6 14.5 {item:{id:"cultivo:sieve",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
summon minecraft:item_display 9.5 151.6 14.5 {item:{id:"cultivo:press",count:1},billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.8f,0.8f,0.8f]}}
