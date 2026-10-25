# Itens em slots conhecidos: o run-probe.ps1 passa o mouse em cada um para mostrar a dica (tooltip).
gamemode survival @a
tp @a 10.5 151 42.5 180 0
clear @a
effect clear @a
item replace entity @a hotbar.0 with cultivo:morango_com_banana_seeds 5
item replace entity @a hotbar.1 with cultivo:brisa_seeds 16
item replace entity @a hotbar.2 with cultivo:gorilao_seeds 3
item replace entity @a hotbar.3 with cultivo:manga_gelada_fresh_flower[cultivo:quality=2] 3
item replace entity @a hotbar.4 with cultivo:eclipse_dried_flower[cultivo:quality=5,cultivo:cured={}] 5
item replace entity @a hotbar.5 with cultivo:hash[cultivo:quality=4,cultivo:strain="mirtilo"] 2
item replace entity @a hotbar.6 with cultivo:joint[cultivo:strain="manga_gelada",cultivo:quality=4,cultivo:filtered={}] 3
item replace entity @a hotbar.7 with cultivo:joint[cultivo:strain="aurora",cultivo:quality=3] 4
item replace entity @a hotbar.8 with cultivo:lighter
item replace entity @a inventory.0 with cultivo:resin[cultivo:quality=3,cultivo:strain="brisa"] 6
item replace entity @a inventory.1 with cultivo:rolling_paper 16
item replace entity @a inventory.2 with cultivo:filter_tip 16
item replace entity @a inventory.3 with cultivo:grinder
item replace entity @a inventory.4 with cultivo:rolling_tray
item replace entity @a inventory.5 with cultivo:jar
item replace entity @a inventory.6 with cultivo:ashtray
item replace entity @a inventory.7 with cultivo:drying_rack
item replace entity @a inventory.8 with cultivo:sieve
item replace entity @a inventory.9 with cultivo:press
item replace entity @a inventory.10 with cultivo:bong
item replace entity @a inventory.11 with cultivo:pipe
item replace entity @a inventory.12 with cultivo:hookah
item replace entity @a inventory.13 with cultivo:chillum
item replace entity @a inventory.14 with cultivo:wild_plant
item replace entity @a inventory.15 with cultivo:bala_sortida_dried_flower[cultivo:quality=1]
item replace entity @a inventory.16 with cultivo:ultravioleta_fresh_flower[cultivo:quality=3]
item replace entity @a inventory.17 with cultivo:joint
item replace entity @a inventory.18 with cultivo:grow_tent
say probe:inventory
