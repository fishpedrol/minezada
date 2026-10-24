# Linha do tempo em ticks desde que o jogador entrou. O run-probe.ps1 tira os prints nos mesmos tempos.
execute if entity @a run scoreboard players add #t probe 1
execute if score #t probe matches 20 run function probe:prepare
execute if score #t probe matches 60 run function probe:build
execute if score #t probe matches 100 run function probe:cam_crops
execute if score #t probe matches 300 run function probe:cam_stations
execute if score #t probe matches 500 run function probe:cam_greenhouse
execute if score #t probe matches 700 run function probe:cam_items
execute if score #t probe matches 900 run function probe:inventory
execute if score #t probe matches 1100 run function probe:held
execute if score #t probe matches 1300 run function probe:smoke
