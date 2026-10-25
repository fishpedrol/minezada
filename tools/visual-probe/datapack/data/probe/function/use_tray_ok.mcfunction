# Cliques reais: o jogador fica em cima da bandeja (x=30) olhando reto para baixo: de longe a mira escorregava para a pedra.
gamemode survival @a
clear @a
effect clear @a
tp @a 30.5 151.2 2.5 180 90
item replace entity @a hotbar.0 with cultivo:eclipse_dried_flower[cultivo:quality=4] 2
item replace entity @a inventory.0 with cultivo:rolling_paper 3
say probe:use_tray_ok
