# Mira a face sul do suporte vazio (x=20, z=2) a 2 blocos.
clear @a
tp @a 20.5 151 4.5 180 29
item replace entity @a hotbar.0 with cultivo:manga_gelada_fresh_flower[cultivo:quality=3] 4
say probe:use_rack
