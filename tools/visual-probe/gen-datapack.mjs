// Gera o datapack de cena do teste visual a partir de tools/gen/manifest.json (variedades e familias de cor).
// Uso: node gen-datapack.mjs [raizDoRepo] [pastaDeSaida]
// Os arquivos saem em UTF-8 sem BOM e com \n (o Minecraft le .mcfunction como UTF-8). So comentarios ASCII.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(process.argv[2] ?? path.join(here, '..', '..'));
const out = path.resolve(process.argv[3] ?? path.join(here, 'datapack'));
const manifest = JSON.parse(fs.readFileSync(path.join(repo, 'tools', 'gen', 'manifest.json'), 'utf8'));
const strains = manifest.strains;

// Ordem das familias na vitrine: nativas primeiro; verde antes de roxo para as duas fotos de geneticas ficarem
// com 20 colunas cada (familia desconhecida vai para o fim, na ordem do manifesto)
const FAMILY_ORDER = ['brisa', 'aurora', 'eclipse', 'dourado', 'limao', 'gelo', 'verde', 'roxo', 'verde_escuro',
  'azul', 'creme', 'rosa'];
const famIndex = f => { const i = FAMILY_ORDER.indexOf(f); return i < 0 ? FAMILY_ORDER.length : i; };
const sorted = strains.map((s, i) => ({ ...s, i }))
  .sort((a, b) => famIndex(a.family) - famIndex(b.family) || a.i - b.i);
// Representante de cada familia: a primeira variedade dela no manifesto (as nativas sao a propria familia)
const reps = [];
for (const s of strains) if (!reps.some(r => r.family === s.family)) reps.push(s);
reps.sort((a, b) => famIndex(a.family) - famIndex(b.family));
const firstCatalog = reps.find(r => r.origin !== 'nativa');

// Corte das duas fotos de geneticas na troca de familia mais perto da metade
const bounds = [];
for (let k = 1; k < sorted.length; k++) if (sorted[k].family !== sorted[k - 1].family) bounds.push(k);
const split = bounds.reduce((best, k) => Math.abs(k - sorted.length / 2) < Math.abs(best - sorted.length / 2) ? k : best,
  bounds[0]);
const pages = [sorted.slice(0, split), sorted.slice(split)];

const num = v => (Number.isInteger(v) ? String(v) : v.toFixed(2).replace(/0+$/, '').replace(/\.$/, ''));
const snbtValue = v => (typeof v === 'string' ? `"${v}"` : v === true ? '{}' : String(v));
const comps = c => Object.entries(c).map(([k, v]) => `"cultivo:${k}":${snbtValue(v)}`).join(',');
const hasComps = c => c && Object.keys(c).length > 0;
// Pilha no formato dos comandos give/item: cultivo:id[cultivo:k=v,...] (true vira {} para componentes sem valor)
const itemArg = (id, c) => `cultivo:${id}` +
  (hasComps(c) ? `[${Object.entries(c).map(([k, v]) => `cultivo:${k}=${snbtValue(v)}`).join(',')}]` : '');
// Slot de BlockEntity (ContainerHelper: lista Items com Slot)
const slot = (n, id, count, c) => `{Slot:${n}b,id:"cultivo:${id}",count:${count}` +
  (hasComps(c) ? `,components:{${comps(c)}}` : '') + '}';
const display = (x, y, z, id, c, ctx, s) => `summon minecraft:item_display ${num(x)} ${num(y)} ${num(z)} ` +
  `{item:{id:"cultivo:${id}",count:1${hasComps(c) ? `,components:{${comps(c)}}` : ''}},item_display:"${ctx}",` +
  `billboard:"center",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],` +
  `translation:[0f,0f,0f],scale:[${num(s)}f,${num(s)}f,${num(s)}f]}}`;

// Linha do tempo (ticks desde que o jogador entrou). O run-probe.ps1 espera cada "say probe:<nome>".
// Os progressos das estacoes no build contam com o build em 60 e as cameras nestes tempos (ver comentarios).
const steps = [
  [20, 'prepare'], [60, 'build'], [100, 'cam_crops'], [260, 'cam_families'], [420, 'cam_drying'],
  [580, 'cam_sieve'], [740, 'cam_press'], [900, 'cam_articles'], [1060, 'cam_grow_tent'], [1220, 'cam_strains_a'],
  [1380, 'cam_strains_b'], [1540, 'cam_products'], [1700, 'cam_in_hand'], [1860, 'inventory'], [2260, 'creative'],
  [2520, 'use_tray_ok'], [2640, 'use_tray_refused'], [2760, 'use_rack'], [2940, 'use_grinder'], [3060, 'held'],
  [3240, 'smoke_joint'], [3460, 'smoke_filtered'], [3640, 'smoke_bong'], [3940, 'smoke_hookah'],
  [4120, 'smoke_pipe'], [4280, 'smoke_chillum'], [4440, 'use_tent'], [4600, 'end'],
];

const fn = {};
fn.load = [
  'scoreboard objectives add probe dummy',
  'scoreboard players set #t probe 0',
  'gamerule random_tick_speed 0',
  'gamerule advance_time false',
  'gamerule advance_weather false',
  'gamerule spawn_mobs false',
  'time set noon',
  'weather clear',
];
fn.tick = [
  '# Linha do tempo em ticks desde que o jogador entrou. O run-probe.ps1 espera cada "say probe:<nome>".',
  'execute if entity @a run scoreboard players add #t probe 1',
  ...steps.map(([t, name]) => `execute if score #t probe matches ${t} run function probe:${name}`),
];
fn.prepare = [
  'gamemode spectator @a',
  'forceload add 0 0 47 47',
  'tp @a 6 160 12',
  '# Todas as receitas ja conhecidas: o aviso "Ha novas receitas!" aparece agora e some antes dos prints',
  'recipe give @a *',
  'effect clear @a',
];

// ---------- Cena ----------
const b = [];
b.push('# Plataforma no alto (y=150), longe do relevo, com luz do dia.');
b.push('fill 0 150 0 47 150 47 minecraft:smooth_stone');
b.push('fill 0 151 0 47 160 47 minecraft:air');

b.push('# A) Nativas nos 8 estagios (x 2..9 = idade 0..7), como no print de antes.');
const natives = strains.filter(s => s.origin === 'nativa');
natives.forEach((s, r) => {
  const z = 2 + 2 * r;
  b.push(`fill 2 150 ${z} 9 150 ${z} minecraft:farmland[moisture=7]`);
  for (let age = 0; age <= 7; age++) b.push(`setblock ${2 + age} 151 ${z} cultivo:${s.id}_crop[age=${age}]`);
  if (r < natives.length - 1) b.push(`fill 2 150 ${z + 1} 9 150 ${z + 1} minecraft:water`);
});
b.push('# Planta silvestre na grama.');
b.push('fill 11 150 1 12 150 3 minecraft:grass_block');
b.push('setblock 11 151 2 cultivo:wild_plant');
b.push('setblock 12 151 2 cultivo:wild_plant');

b.push('# B) Uma variedade de cada familia de cor: z=14 madura (idade 7), z=16 no meio (idade 4).');
b.push(`# x = 2.. : ${reps.map(r => `${r.family}=${r.id}`).join(', ')}`);
b.push(`fill 2 150 14 ${1 + reps.length} 150 14 minecraft:farmland[moisture=7]`);
b.push(`fill 2 150 15 ${1 + reps.length} 150 15 minecraft:water`);
b.push(`fill 2 150 16 ${1 + reps.length} 150 16 minecraft:farmland[moisture=7]`);
reps.forEach((r, i) => {
  b.push(`setblock ${2 + i} 151 14 cultivo:${r.id}_crop[age=7]`);
  b.push(`setblock ${2 + i} 151 16 cultivo:${r.id}_crop[age=4]`);
});

b.push('# C) Estacoes, viradas para a camera (sul): x=20 vazia, 22 trabalhando, 24 pronta, 26 variante.');
b.push('# O contents vem dos slots (a BlockEntity acerta o blockstate a cada tick), por isso os itens vao no NBT.');
b.push('# progress alinha um fim de ciclo (sopro + legenda) com o print: secagem 600 ticks, peneira 200, prensa 300.');
b.push('setblock 20 151 2 cultivo:drying_rack[facing=south]');
b.push(`setblock 22 151 2 cultivo:drying_rack[facing=south]{Items:[${slot(0, 'aurora_fresh_flower', 64, { quality: 3 })}],progress:190}`);
b.push(`setblock 24 151 2 cultivo:drying_rack[facing=south]{Items:[${slot(1, 'aurora_dried_flower', 8, { quality: 3 })}]}`);
b.push(`setblock 26 151 2 cultivo:drying_rack[facing=east]{Items:[${slot(0, `${firstCatalog.id}_fresh_flower`, 64, { quality: 2 })}]}`);
b.push('setblock 20 151 9 cultivo:sieve[facing=south]');
b.push(`setblock 22 151 9 cultivo:sieve[facing=south]{Items:[${slot(0, 'eclipse_dried_flower', 64, { quality: 4 })}],progress:30}`);
b.push(`setblock 24 151 9 cultivo:sieve[facing=south]{Items:[${slot(1, 'resin', 6, { quality: 4, strain: 'eclipse' })}]}`);
b.push(`setblock 26 151 9 cultivo:sieve[facing=south,grinder=true]{Items:[${slot(0, `${firstCatalog.id}_dried_flower`, 64, { quality: 3 })}]}`);
b.push('setblock 20 151 16 cultivo:press[facing=south]');
b.push(`setblock 22 151 16 cultivo:press[facing=south]{Items:[${slot(0, 'resin', 64, { quality: 3, strain: 'aurora' })}],progress:160}`);
b.push(`setblock 24 151 16 cultivo:press[facing=south]{Items:[${slot(1, 'hash', 3, { quality: 4, strain: 'eclipse' })}]}`);
b.push(`setblock 26 151 16 cultivo:press[facing=east]{Items:[${slot(0, 'resin', 64, { quality: 3 })}]}`);

b.push('# D) Artigos colocaveis: z=2 bandeja (sul), bandeja (leste), pote vazio, pote com resina (nao cura);');
b.push('# o pote que cura e colocado em cam_articles (x=38). z=6: cinzeiros com cinza 0..4.');
b.push('setblock 30 151 2 cultivo:rolling_tray[facing=south]');
b.push('setblock 32 151 2 cultivo:rolling_tray[facing=east]');
b.push('setblock 34 151 2 cultivo:jar');
b.push(`setblock 36 151 2 cultivo:jar[filled=true]{Items:[${slot(0, 'resin', 32, { quality: 3 })}]}`);
for (let ash = 0; ash <= 4; ash++) b.push(`setblock ${30 + 2 * ash} 151 6 cultivo:ashtray[ash=${ash}]`);

b.push('# E) Estufinha: sala escura de pedra-negra (parede sul aberta para a camera), chao de pedra, sem farmland nem agua.');
b.push('# Tres estufinhas viradas para o sul: x40 vazia, x42 Brisa idade 3, x44 da familia roxa madura. Metade de baixo');
b.push('# primeiro (com a planta na BlockEntity), depois a de cima.');
b.push('fill 39 150 10 45 154 16 minecraft:blackstone hollow');
b.push('fill 40 151 16 44 153 16 minecraft:air');
const roxoRep = reps.find(r => r.family === 'roxo') ?? firstCatalog;
const tents = [[40, null, 0], [42, 'brisa', 3], [44, roxoRep.id, 7]];
for (const [x, variety, age] of tents) {
  const plant = variety ? `{variety:"${variety}",age:${age}}` : '';
  b.push(`setblock ${x} 151 13 cultivo:grow_tent[facing=south,half=lower]${plant}`);
  b.push(`setblock ${x} 152 13 cultivo:grow_tent[facing=south,half=upper]`);
}

b.push('# F) Vitrines (item_display sempre virado para a camera). Geneticas: uma coluna por variedade, de cima');
b.push('# para baixo semente, flor fresca e flor seca, agrupadas por familia; duas fotos.');
const pageX0 = [1.5, 16.5];
pages.forEach((page, p) => {
  b.push(`# Foto ${p + 1}: ${page.map(s => s.id).join(', ')}`);
  page.forEach((s, i) => {
    const x = pageX0[p] + 0.6 * i;
    b.push(display(x, 153.9, 30.5, `${s.id}_seeds`, null, 'gui', 0.5));
    b.push(display(x, 153.2, 30.5, `${s.id}_fresh_flower`, { quality: 3 }, 'gui', 0.5));
    b.push(display(x, 152.5, 30.5, `${s.id}_dried_flower`, { quality: 3 }, 'gui', 0.5));
  });
});
b.push('# Produtos e artigos (icone de inventario). Linha 1: haxixe sem variedade e de cada familia.');
const hashRow = [null, ...reps];
hashRow.forEach((r, i) => b.push(display(31.5 + 0.6 * i, 153.9, 30.5, 'hash', r ? { quality: 4, strain: r.id } : null, 'gui', 0.5)));
const row2 = [
  ['resin', { quality: 3 }], ['joint', null], ['joint', { strain: firstCatalog.id, quality: 4 }],
  ['joint', { strain: firstCatalog.id, quality: 4, filtered: true }], ['rolling_paper', null], ['filter_tip', null],
  ['grinder', null], ['lighter', null], ['rolling_tray', null], ['jar', null], ['ashtray', null], ['wild_plant', null],
];
row2.forEach(([id, c], i) => b.push(display(31.5 + 0.6 * i, 153.2, 30.5, id, c, 'gui', 0.5)));
const row3 = ['bong', 'pipe', 'hookah', 'chillum', 'drying_rack', 'sieve', 'press', 'grow_tent'];
row3.forEach((id, i) => b.push(display(31.5 + 0.6 * i, 152.5, 30.5, id, null, 'gui', 0.5)));
b.push('# Modelos na mao (contexto thirdperson_righthand), maiores.');
const inHand = [['bong', null], ['hookah', null], ['pipe', null], ['chillum', null], ['joint', null],
  ['joint', { filtered: true }], ['lighter', null]];
inHand.forEach(([id, c], i) => b.push(display(31.5 + 1.2 * i, 152.4, 42.5, id, c, 'thirdperson_righthand', 1)));

b.push('# G) Area de fumar: o jogador fica em 10.5 151 42.5 olhando para o norte; cinzeiro vazio a direita.');
b.push('setblock 12 151 42 cultivo:ashtray[ash=0]');
fn.build = b;

const centerX = p => pageX0[p] + 0.6 * (pages[p].length - 1) / 2;
fn.cam_crops = ['tp @a 5.5 153.2 10.5 180 22', 'say probe:cam_crops'];
fn.cam_families = [`tp @a ${num(2 + reps.length / 2)} 154.5 21.5 180 25`, 'say probe:cam_families'];
fn.cam_drying = ['tp @a 23.5 152.8 6.2 180 22', 'say probe:cam_drying'];
fn.cam_sieve = ['tp @a 23.5 152.8 13.2 180 22', 'say probe:cam_sieve'];
fn.cam_press = ['tp @a 23.5 152.8 20.2 180 22', 'say probe:cam_press'];
fn.cam_articles = [
  '# Pote com flores secas a 40 ticks de curar: brilho e legenda "Flores curadas" uns 2 s depois do marcador',
  `setblock 38 151 2 cultivo:jar[filled=true]{Items:[${slot(0, 'aurora_dried_flower', 16, { quality: 3 })}],cure_progress:2360}`,
  'tp @a 34.5 153.4 10 180 28',
  'say probe:cam_articles',
];
fn.cam_grow_tent = ['tp @a 42.5 152.4 20.5 180 8', 'say probe:cam_grow_tent'];
fn.cam_strains_a = [`tp @a ${num(centerX(0))} 153.2 35.6 180 0`, 'say probe:cam_strains_a'];
fn.cam_strains_b = [`tp @a ${num(centerX(1))} 153.2 35.6 180 0`, 'say probe:cam_strains_b'];
fn.cam_products = ['tp @a 35.1 153.2 34.8 180 0', 'say probe:cam_products'];
fn.cam_in_hand = ['tp @a 35.1 152.8 47.3 180 5', 'say probe:cam_in_hand'];

const rare = strains.find(s => s.rarity === 'rara' && s.potency === 3 && s.origin !== 'nativa') ?? strains[2];
const uncommon = strains.find(s => s.rarity === 'incomum' && s.origin !== 'nativa') ?? strains[1];
const lowPurple = reps.find(r => r.family === 'azul') ?? firstCatalog;
fn.inventory = [
  '# Itens em slots conhecidos: o run-probe.ps1 passa o mouse em cada um para mostrar a dica (tooltip).',
  'gamemode survival @a',
  'tp @a 10.5 151 42.5 180 0',
  'clear @a',
  'effect clear @a',
  `item replace entity @a hotbar.0 with ${itemArg(`${rare.id}_seeds`)} 5`,
  `item replace entity @a hotbar.1 with ${itemArg('brisa_seeds')} 16`,
  `item replace entity @a hotbar.2 with ${itemArg(`${uncommon.id}_seeds`)} 3`,
  `item replace entity @a hotbar.3 with ${itemArg(`${firstCatalog.id}_fresh_flower`, { quality: 2 })} 3`,
  `item replace entity @a hotbar.4 with ${itemArg('eclipse_dried_flower', { quality: 5, cured: true })} 5`,
  `item replace entity @a hotbar.5 with ${itemArg('hash', { quality: 4, strain: lowPurple.id })} 2`,
  `item replace entity @a hotbar.6 with ${itemArg('joint', { strain: firstCatalog.id, quality: 4, filtered: true })} 3`,
  `item replace entity @a hotbar.7 with ${itemArg('joint', { strain: 'aurora', quality: 3 })} 4`,
  `item replace entity @a hotbar.8 with ${itemArg('lighter')}`,
  `item replace entity @a inventory.0 with ${itemArg('resin', { quality: 3, strain: 'brisa' })} 6`,
  `item replace entity @a inventory.1 with ${itemArg('rolling_paper')} 16`,
  `item replace entity @a inventory.2 with ${itemArg('filter_tip')} 16`,
  `item replace entity @a inventory.3 with ${itemArg('grinder')}`,
  `item replace entity @a inventory.4 with ${itemArg('rolling_tray')}`,
  `item replace entity @a inventory.5 with ${itemArg('jar')}`,
  `item replace entity @a inventory.6 with ${itemArg('ashtray')}`,
  `item replace entity @a inventory.7 with ${itemArg('drying_rack')}`,
  `item replace entity @a inventory.8 with ${itemArg('sieve')}`,
  `item replace entity @a inventory.9 with ${itemArg('press')}`,
  `item replace entity @a inventory.10 with ${itemArg('bong')}`,
  `item replace entity @a inventory.11 with ${itemArg('pipe')}`,
  `item replace entity @a inventory.12 with ${itemArg('hookah')}`,
  `item replace entity @a inventory.13 with ${itemArg('chillum')}`,
  `item replace entity @a inventory.14 with ${itemArg('wild_plant')}`,
  `item replace entity @a inventory.15 with ${itemArg((reps.find(r => r.family === 'rosa') ?? firstCatalog).id + '_dried_flower', { quality: 1 })}`,
  `item replace entity @a inventory.16 with ${itemArg((reps.find(r => r.family === 'roxo') ?? firstCatalog).id + '_fresh_flower', { quality: 3 })}`,
  `item replace entity @a inventory.17 with ${itemArg('joint')}`,
  `item replace entity @a inventory.18 with ${itemArg('grow_tent')}`,
  'say probe:inventory',
];
fn.creative = ['clear @a', 'effect clear @a', 'gamemode creative @a', 'say probe:creative'];
fn.use_tray_ok = [
  '# Cliques reais: o jogador fica em cima da bandeja (x=30) olhando reto para baixo: de longe a mira escorregava para a pedra.',
  'gamemode survival @a',
  'clear @a',
  'effect clear @a',
  'tp @a 30.5 151.2 2.5 180 90',
  `item replace entity @a hotbar.0 with ${itemArg('eclipse_dried_flower', { quality: 4 })} 2`,
  `item replace entity @a inventory.0 with ${itemArg('rolling_paper')} 3`,
  'say probe:use_tray_ok',
];
fn.use_tray_refused = [
  'clear @a',
  'tp @a 30.5 151.2 2.5 180 90',
  `item replace entity @a hotbar.0 with ${itemArg('eclipse_dried_flower', { quality: 4 })} 2`,
  'say probe:use_tray_refused',
];
fn.use_rack = [
  '# Mira a face sul do suporte vazio (x=20, z=2) a 2 blocos.',
  'clear @a',
  'tp @a 20.5 151 4.5 180 29',
  `item replace entity @a hotbar.0 with ${itemArg(`${firstCatalog.id}_fresh_flower`, { quality: 3 })} 4`,
  'say probe:use_rack',
];
fn.use_grinder = [
  '# Mira o topo da peneira vazia (x=20, z=9).',
  'clear @a',
  'tp @a 20.5 151 11.5 180 22',
  `item replace entity @a hotbar.0 with ${itemArg('grinder')}`,
  'say probe:use_grinder',
];
fn.held = [
  'clear @a',
  'effect clear @a',
  'tp @a 10.5 151 42.5 180 0',
  `item replace entity @a weapon.mainhand with ${itemArg('bong')}`,
  `item replace entity @a weapon.offhand with ${itemArg('eclipse_dried_flower', { quality: 5 })} 8`,
  `item replace entity @a inventory.0 with ${itemArg('lighter')}`,
  'say probe:held',
];
const smoke = (name, main, mainCount, fuel) => [
  'clear @a',
  'effect clear @a',
  'tp @a 10.5 151 42.5 180 0',
  `item replace entity @a weapon.mainhand with ${main}${mainCount ? ` ${mainCount}` : ''}`,
  `item replace entity @a inventory.0 with ${itemArg('lighter')}`,
  ...(fuel ? [`item replace entity @a inventory.1 with ${fuel}`] : []),
  `say probe:${name}`,
];
fn.smoke_joint = smoke('smoke_joint', itemArg('joint', { strain: 'brisa', quality: 5 }), 1, null);
fn.smoke_filtered = smoke('smoke_filtered', itemArg('joint', { strain: firstCatalog.id, quality: 4, filtered: true }), 1, null);
fn.smoke_bong = ['# Mao secundaria vazia: o bong vai com as duas maos. 2 flores = 2 sessoes; a 3a e recusada.',
  ...smoke('smoke_bong', itemArg('bong'), 0, `${itemArg('aurora_dried_flower', { quality: 4 })} 2`)];
fn.smoke_hookah = smoke('smoke_hookah', itemArg('hookah'), 0, `${itemArg('eclipse_dried_flower', { quality: 3 })} 2`);
fn.smoke_pipe = smoke('smoke_pipe', itemArg('pipe'), 0, `${itemArg('hash', { strain: 'eclipse', quality: 4 })} 2`);
fn.smoke_chillum = smoke('smoke_chillum', itemArg('chillum'), 0, `${itemArg('brisa_dried_flower', { quality: 3 })} 2`);
fn.use_tent = [
  '# Dentro da sala da estufinha, mirando a metade de cima da madura (x=44): mao vazia (slot 2) colhe, semente (slot 1) planta.',
  'gamemode survival @a',
  'clear @a',
  'effect clear @a',
  'tp @a 44.5 151 15.5 180 10',
  `item replace entity @a hotbar.0 with ${itemArg('brisa_seeds')} 4`,
  'say probe:use_tent',
];
fn.end = ['say probe:end'];

// ---------- Escrita ----------
const write = (rel, text) => {
  const file = path.join(out, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, text.endsWith('\n') ? text : text + '\n', 'utf8');
};
fs.rmSync(out, { recursive: true, force: true });
write('pack.mcmeta', JSON.stringify({ pack: { description: 'Cultivo visual probe (dev only, not shipped in the jar)',
  min_format: 121, max_format: 121 } }, null, 2));
write('data/minecraft/tags/function/load.json', JSON.stringify({ values: ['probe:load'] }));
write('data/minecraft/tags/function/tick.json', JSON.stringify({ values: ['probe:tick'] }));
for (const [name, lines] of Object.entries(fn)) write(`data/probe/function/${name}.mcfunction`, lines.join('\n'));
const missing = steps.map(([, n]) => n).filter(n => !fn[n]);
if (missing.length) throw new Error(`passo sem funcao: ${missing.join(', ')}`);
console.log(`datapack em ${out}: ${Object.keys(fn).length} funcoes, ${b.length} linhas no build, ` +
  `fotos de geneticas ${pages.map(p => p.length).join('+')}, raro=${rare.id}, incomum=${uncommon.id}`);
