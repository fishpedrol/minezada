# Mao secundaria vazia: o bong vai com as duas maos. 2 flores = 2 sessoes; a 3a e recusada.
clear @a
effect clear @a
tp @a 10.5 151 42.5 180 0
item replace entity @a weapon.mainhand with cultivo:bong
item replace entity @a inventory.0 with cultivo:lighter
item replace entity @a inventory.1 with cultivo:aurora_dried_flower[cultivo:quality=4] 2
say probe:smoke_bong
