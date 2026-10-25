// Gera todas as texturas originais 16x16 do Cultivo a partir de pixel art em ASCII.
// Uso: node tools/art/textures.mjs   (escreve em src/generated/resources/assets/cultivo/textures)
// Cada textura é uma grade de 16 linhas x 16 colunas; '.' é transparente e os demais símbolos vêm da paleta.
// Só há pixels opacos ou totalmente transparentes (nada de alfa parcial): assim o jogo escolhe a camada cutout
// sozinho e nada cai na camada translúcida (AssetCompletenessTest confere).
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { encodePng, parseColor } from './png.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'src', 'generated', 'resources', 'assets', 'cultivo', 'textures');

function render(name, grid, palette) {
  // Itens e blocos são 16x16; ícones de efeito (mob_effect) são 18x18, como os vanilla
  const size = grid.length;
  if (size !== 16 && size !== 18) throw new Error(`${name}: ${size} linhas`);
  const rgba = new Uint8Array(size * size * 4);
  grid.forEach((row, y) => {
    if (row.length !== size) throw new Error(`${name}: linha ${y} tem ${row.length} colunas`);
    [...row].forEach((ch, x) => {
      if (ch === '.') return;
      const hex = palette[ch];
      if (!hex) throw new Error(`${name}: cor ausente para '${ch}'`);
      rgba.set(parseColor(hex), (y * size + x) * 4);
    });
  });
  const file = join(OUT, `${name}.png`);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, encodePng(size, size, rgba));
  return file;
}

/**
 * Textura animada, como a água e o fogo vanilla: os quadros 16x16 ficam empilhados na vertical num PNG de 16 x 16·n e o
 * <nome>.png.mcmeta diz quantos ticks cada quadro dura (frametime) e, se preciso, a ordem dos quadros. Sem interpolação
 * (pixel art), e cada quadro segue a regra de alfa só 0 ou 255.
 * @param frames lista de {grid, palette}
 */
function renderAnimation(name, frames, { frametime, order } = {}) {
  if (!Number.isInteger(frametime) || frametime <= 0) throw new Error(`${name}: frametime inválido`);
  const size = 16;
  const rgba = new Uint8Array(size * size * frames.length * 4);
  frames.forEach(({ grid, palette }, f) => {
    if (grid.length !== size) throw new Error(`${name}[${f}]: ${grid.length} linhas`);
    grid.forEach((row, y) => {
      if (row.length !== size) throw new Error(`${name}[${f}]: linha ${y} tem ${row.length} colunas`);
      [...row].forEach((ch, x) => {
        if (ch === '.') return;
        const hex = palette[ch];
        if (!hex) throw new Error(`${name}[${f}]: cor ausente para '${ch}'`);
        rgba.set(parseColor(hex), ((f * size + y) * size + x) * 4);
      });
    });
  });
  const file = join(OUT, `${name}.png`);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, encodePng(size, size * frames.length, rgba));
  if (order && order.some((i) => !Number.isInteger(i) || i < 0 || i >= frames.length)) throw new Error(`${name}: ordem inválida`);
  const animation = order ? { frametime, frames: order } : { frametime };
  writeFileSync(`${file}.mcmeta`, JSON.stringify({ animation }, null, 2) + '\n');
  return file;
}

// ---------------------------------------------------------------- cores
function rgbToHsl([r, g, b]) {
  const [rn, gn, bn] = [r / 255, g / 255, b / 255];
  const max = Math.max(rn, gn, bn);
  const min = Math.min(rn, gn, bn);
  const l = (max + min) / 2;
  if (max === min) return [0, 0, l];
  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  let h;
  if (max === rn) h = (gn - bn) / d + (gn < bn ? 6 : 0);
  else if (max === gn) h = (bn - rn) / d + 2;
  else h = (rn - gn) / d + 4;
  return [h * 60, s, l];
}

function hslToRgb([h, s, l]) {
  if (s === 0) return [l, l, l].map((v) => Math.round(v * 255));
  const hn = (((h % 360) + 360) % 360) / 360;
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const channel = (t) => {
    const u = t < 0 ? t + 1 : t > 1 ? t - 1 : t;
    if (u < 1 / 6) return p + (q - p) * 6 * u;
    if (u < 1 / 2) return q;
    if (u < 2 / 3) return p + (q - p) * (2 / 3 - u) * 6;
    return p;
  };
  return [channel(hn + 1 / 3), channel(hn), channel(hn - 1 / 3)].map((v) => Math.round(v * 255));
}

const toHex = (rgb) => '#' + rgb.map((v) => Math.max(0, Math.min(255, v)).toString(16).padStart(2, '0')).join('');
const hsl = (hex) => rgbToHsl(parseColor(hex).slice(0, 3));

/** Gira o tom (graus) e soma à claridade (0–1) de uma cor '#rrggbb'. */
function shift(hex, dHue, dLight) {
  const [h, s, l] = hsl(hex);
  return toHex(hslToRgb([h + dHue, s, Math.min(0.95, Math.max(0.05, l + dLight))]));
}

/**
 * Versão "seca" de uma cor: puxa o tom para o oliva/palha, tira saturação e escurece. As nativas têm paletas
 * secas escritas à mão (plano de arte 2.1); as famílias do catálogo usam esta regra.
 */
function dry(hex, { hueTo = 50, hueMix = 0.4, sat = 0.55, light = 0.82 } = {}) {
  const [h, s, l] = hsl(hex);
  const dh = ((hueTo - h + 540) % 360) - 180;
  return toHex(hslToRgb([h + dh * hueMix, s * sat, l * light]));
}

// ---------------------------------------------------------------- paletas das plantas
// Símbolos: L folha, D folha escura/pecíolo (carrega o tom da família), S caule, B botão, b botão escuro/mancha,
// P pistilo, F tricoma/brilho de geada. Sementes: o contorno, O corpo, h brilho, b listra.
const NATIVE_PALETTES = {
  // Brisa: verde claro e dourado
  brisa: {
    plant: { L: '#8fd14f', D: '#5c9a2e', S: '#6f9a3a', B: '#d6e07a', b: '#b39a3e', P: '#f0a640', F: '#fbf6d2' },
    dried: { L: '#8a8a3a', D: '#6a6a2a', S: '#6b5a3a', B: '#b8b45a', b: '#7f7a3a', P: '#c07a30', F: '#e8e2c0' },
    seed: { o: '#6e5434', O: '#c9a873', h: '#ead6a8', b: '#9a7a4e' },
    stamp: '#d6b84a',
  },
  // Aurora: verde com manchas roxo-rosadas (o botão não é mais lilás, para não parecer lavanda)
  aurora: {
    plant: { L: '#4fa83c', D: '#8a4a86', S: '#4d7a2c', B: '#9cc46a', b: '#b06aa8', P: '#ff8fb8', F: '#f6e6f6' },
    dried: { L: '#6a7a3a', D: '#6a4a5a', S: '#6b5a3a', B: '#7f9450', b: '#7d4f78', P: '#c86a8a', F: '#e4d8e4' },
    seed: { o: '#4c433d', O: '#9a8f86', h: '#cbc3bb', b: '#6a6058' },
    stamp: '#d07aa8',
  },
  // Eclipse: verde escuro e roxo profundo
  eclipse: {
    plant: { L: '#2f6a3a', D: '#4a2a5c', S: '#3c5030', B: '#5e4a86', b: '#33244f', P: '#e3843e', F: '#dcd2ea' },
    dried: { L: '#3a4a30', D: '#3a2a40', S: '#5a4a32', B: '#4a3a62', b: '#2a1d3c', P: '#a85a2c', F: '#c8bfd6' },
    seed: { o: '#1e1712', O: '#4a3c32', h: '#7a6656', b: '#2a211a' },
    stamp: '#8a6ac0',
  },
};

// Paletas-base das famílias do catálogo (campo "family" do manifesto). As folhas (L) das 12 famílias ficam a pelo
// menos 40 de distância RGB umas das outras, para cada família se reconhecer na lavoura (AssetCompletenessTest).
const FAMILY_PALETTES = {
  verde: {
    plant: { L: '#2fc45c', D: '#1f8a3e', S: '#3f8a3a', B: '#b4e47a', b: '#6aa046', P: '#f08c3c', F: '#f0fae0' },
    seed: { o: '#5e4a32', O: '#b8a07a', h: '#e0cfa6', b: '#8a7454' },
    stamp: '#6cc860',
  },
  verde_escuro: {
    plant: { L: '#507a1c', D: '#2e5214', S: '#3c5a20', B: '#a8d07a', b: '#5a7e36', P: '#e07a3a', F: '#eef6dc' },
    seed: { o: '#33281c', O: '#6e5a44', h: '#9c8466', b: '#4e3e2c' },
    stamp: '#4e9a40',
  },
  limao: {
    plant: { L: '#bcdc46', D: '#7ea024', S: '#8aa236', B: '#f0f07a', b: '#c4c040', P: '#f5b041', F: '#fffbe0' },
    seed: { o: '#6a5634', O: '#c8b07a', h: '#ece0b0', b: '#9a8656' },
    stamp: '#e6e65a',
  },
  dourado: {
    plant: { L: '#a0a436', D: '#6e7420', S: '#7a7a30', B: '#f0c85a', b: '#b88a2e', P: '#e8742e', F: '#fff2c8' },
    seed: { o: '#5c4626', O: '#b89a62', h: '#e6d09a', b: '#8a6e40' },
    stamp: '#f0a838',
  },
  roxo: {
    plant: { L: '#5a6452', D: '#6a3c8a', S: '#4a5a44', B: '#a070d0', b: '#5e3a8a', P: '#f08a4a', F: '#efe0fa' },
    seed: { o: '#2e2420', O: '#6a5a52', h: '#9a8a80', b: '#463a34' },
    stamp: '#b07ae0',
  },
  azul: {
    plant: { L: '#3c9682', D: '#2a5e6e', S: '#3a6a5a', B: '#8aa8e8', b: '#4a64a8', P: '#f0a050', F: '#e8f0ff' },
    seed: { o: '#2a2e34', O: '#5e6670', h: '#8e98a4', b: '#3e444c' },
    stamp: '#6a9ae0',
  },
  rosa: {
    plant: { L: '#7a9060', D: '#8a4a6a', S: '#5a7040', B: '#f0a0c8', b: '#c0608e', P: '#ffd070', F: '#fff0f6' },
    seed: { o: '#4e3a34', O: '#a08478', h: '#d0b8ac', b: '#765c52' },
    stamp: '#f2a0c4',
  },
  gelo: {
    plant: { L: '#a4cca4', D: '#6a9a7a', S: '#6a8a6a', B: '#eef6ee', b: '#a8c0b0', P: '#e8a070', F: '#ffffff' },
    seed: { o: '#56524c', O: '#b0aca4', h: '#e4e0d8', b: '#86827a' },
    stamp: '#e8f4f4',
  },
  creme: {
    plant: { L: '#96b46e', D: '#6a8648', S: '#7a8048', B: '#f0dcae', b: '#c8a874', P: '#d8783a', F: '#fffaf0' },
    seed: { o: '#624a30', O: '#c0a07a', h: '#ead6b4', b: '#907456' },
    stamp: '#e8d2a0',
  },
};

/**
 * Paleta seca de uma família do catálogo, derivada da fresca. O botão seca sem perder o tom da família (roxo continua
 * roxo, azul continua azulado): com tudo puxado para o oliva, flores secas de famílias diferentes saíam iguais.
 */
function driedOf(plant) {
  return {
    L: dry(plant.L), D: dry(plant.D, { light: 0.78 }), S: '#6b5a3a',
    B: dry(plant.B, { hueMix: 0.15, sat: 0.8, light: 0.82 }), b: dry(plant.b, { hueMix: 0.15, sat: 0.8, light: 0.74 }),
    P: dry(plant.P, { hueMix: 0.15, sat: 0.75, light: 0.76 }), F: dry(plant.F, { hueMix: 0.3, sat: 0.6, light: 0.92 }),
  };
}

/** Mistura linear de duas cores '#rrggbb' (t = 0 dá a primeira, 1 a segunda). */
function mix(a, b, t) {
  const ca = parseColor(a);
  const cb = parseColor(b);
  return toHex([0, 1, 2].map((k) => Math.round(ca[k] + (cb[k] - ca[k]) * t)));
}

/**
 * Sementes de uma variedade do catálogo: a casca puxa o tom do botão da variedade (contorno e listra do botão escuro,
 * corpo e brilho do botão), então cada família tem sementes de cor própria em vez de todas marrons.
 */
function seedOf(plant) {
  return {
    o: mix(plant.b, '#2a1e14', 0.2), O: mix(plant.B, '#9a7a56', 0.25),
    h: mix(plant.B, '#fbf3e4', 0.55), b: mix(plant.b, '#3a2a1c', 0.15),
  };
}

// Variação de destaque determinística pela posição da variedade dentro da família (na ordem do manifesto): a 1ª usa a
// paleta-base; as seguintes giram o tom dos botões, mudam a claridade das folhas e sementes e trocam a cor dos
// pistilos, para duas variedades da mesma família nunca saírem iguais nos itens (AssetCompletenessTest confere).
const ACCENTS = [
  null,
  { hue: 14, light: 0.05, pistil: '#f2c14e' },
  { hue: -14, light: -0.05, pistil: '#d9653a' },
  { hue: 28, light: -0.02, pistil: '#ff9ec4' },
  { hue: -28, light: 0.02, pistil: '#f6e3b0' },
  { hue: 7, light: 0.08, pistil: '#e8503a' },
];

function withAccent(family, a) {
  if (!a) {
    // Nativas: paletas secas e de semente escritas à mão; a 1ª variedade de cada família do catálogo deriva as suas
    return family.dried
      ? { plant: family.plant, dried: family.dried, seed: family.seed }
      : { plant: family.plant, dried: driedOf(family.plant), seed: seedOf(family.plant) };
  }
  const p = family.plant;
  const plant = {
    L: shift(p.L, a.hue * 0.25, a.light), D: shift(p.D, a.hue * 0.25, a.light), S: shift(p.S, a.hue * 0.25, a.light),
    B: shift(p.B, a.hue, a.light * 0.5), b: shift(p.b, a.hue, a.light * 0.5), P: a.pistil, F: shift(p.F, a.hue * 0.5, 0),
  };
  return { plant, dried: driedOf(plant), seed: seedOf(plant) };
}

function familyBase(family) {
  return NATIVE_PALETTES[family] ?? FAMILY_PALETTES[family];
}

/** Famílias de arte usadas pelas variedades ({id, family} do manifesto), na ordem de primeira aparição. */
export function artFamilies(strains) {
  return [...new Set(strains.map((s) => s.family))];
}

/**
 * Deslocamento do formato de cada família do catálogo: a variedade na posição i da família usa o formato
 * (i + deslocamento) mod 6. Famílias de cores parecidas (verde e verde escuro, limão e dourado, gelo e creme) ficam com
 * formatos diferentes nas mesmas posições, para os ícones não dependerem só de um tom de diferença.
 */
const FAMILY_VARIANT_OFFSET = { verde: 0, verde_escuro: 3, limao: 1, dourado: 4, roxo: 0, azul: 4, rosa: 0, gelo: 3, creme: 5 };

/** Formato dos itens (0–5) da variedade na posição {@code index} da família; null para as nativas (desenhos originais). */
export function variantFor(family, index) {
  if (NATIVE_PALETTES[family]) return null;
  const offset = FAMILY_VARIANT_OFFSET[family];
  if (offset === undefined) throw new Error(`família sem deslocamento de formato: ${family}`);
  return (index + offset) % ACCENTS.length;
}

/** Paleta e formato de cada variedade, na ordem dada: id → {plant, dried, seed, variant}. */
export function strainPalettes(strains) {
  const seen = {};
  const palettes = {};
  for (const strain of strains) {
    const index = seen[strain.family] ?? 0;
    seen[strain.family] = index + 1;
    if (NATIVE_PALETTES[strain.family]) {
      if (index > 0) throw new Error(`família nativa compartilhada: ${strain.family}`);
      palettes[strain.id] = { ...withAccent(NATIVE_PALETTES[strain.family], null), variant: null };
      continue;
    }
    const base = FAMILY_PALETTES[strain.family];
    if (!base) throw new Error(`família de arte desconhecida: ${strain.family} (${strain.id})`);
    if (index >= ACCENTS.length) throw new Error(`família ${strain.family} passou de ${ACCENTS.length} variedades`);
    palettes[strain.id] = { ...withAccent(base, ACCENTS[index]), variant: variantFor(strain.family, index) };
  }
  return palettes;
}

// ---------------------------------------------------------------- lavoura: 8 estágios (um por idade)
// O caule fica na coluna 7; a linha 15 encosta na farmland (o modelo crop do 26.3 começa em y=-1).
// Folha palmada de 7 folíolos (7x5): '...L...' / 'L..L..L' / '.L.L.L.' / '..LDL..' / 'LLLDLLL'.
const E = '................';
const CROP_STAGES = [
  // 0: dois cotilédones
  [E, E, E, E, E, E, E, E, E, E, E, E,
    '................',
    '.....LL.LL......',
    '......LSL.......',
    '.......S........'],
  // 1: primeira folha de 3 folíolos
  [E, E, E, E, E, E, E, E, E, E, E,
    '.......L........',
    '....L.LSL.L.....',
    '.....LLSLL......',
    '.......S........',
    '.......S........'],
  // 2: dois nós (folhas de 5 folíolos em cima, a de 3 embaixo)
  [E, E, E, E, E, E, E,
    '.......L........',
    '..L...LLL...L...',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    '.LLDLL.S.LLDLL..',
    '....DD.S.DD.....',
    '....L.LSL.L.....',
    '.....LLSLL......',
    '.......S........'],
  // 3: primeiro par de leques de 7 folíolos
  [E, E, E, E, E,
    '.......L........',
    '......LLL.......',
    '...L...S...L....',
    'L..L...S...L..L.',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    'LLLDLLLSLLLDLLL.',
    '....DD.S.DD.....',
    '.......S........',
    '.......S........',
    '.......S........'],
  // 4: dois pares de leques, mais alta
  [E, E, E,
    '.......L........',
    '......LLL.......',
    '...L...S...L....',
    'L..L...S...L..L.',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    'LLLDLLLSLLLDLLL.',
    '....DD.S.DD.....',
    '.......S........',
    '.L.L.L.S.L.L.L..',
    '..LLL.DSD.LLL...',
    '.......S........',
    '.......S........'],
  // 5: pré-floração (ponta apical com pistilos)
  [E,
    '.......L........',
    '......LPL.......',
    '.......S........',
    '...L...S...L....',
    'L..L..LSL..L..L.',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    'LLLDLLLSLLLDLLL.',
    '....DD.S.DD.....',
    '.......S........',
    '.L.L.L.S.L.L.L..',
    '..LLL.DSD.LLL...',
    '.......S........',
    '.......S........',
    '.......S........'],
  // 6: cacho apical pequeno e pistilos nos nós (ainda não está pronta)
  [E,
    '.......P........',
    '......BPB.......',
    '......bBP.......',
    '...L..PBb..L....',
    'L..P..LSL..P..L.',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    'LLLDLLLSLLLDLLL.',
    '....DDPSPDD.....',
    '.......S........',
    '.L.L.LPSP.L.L.L.',
    '..LLL.DSD.LLL...',
    '.......S........',
    '.......S........',
    '.......S........'],
  // 7: madura (cola apical, colas laterais, botões nos nós)
  [
    '.......Bb.......',
    '......bPBB......',
    '......BFPb......',
    '..B...BBFb..B...',
    '.bPB..bBPB.BPb..',
    '.BFb..PBbB.bFB..',
    'L.bBL.BbBL.BbL.L',
    '.L.L.L.S.L.L.L..',
    '..LDL..S..LDL...',
    'LLLDLLLSLLLDLLL.',
    '....DDBSBDD.....',
    '.....bPSPb......',
    '.L.L.LBSB.L.L.L.',
    '..LLL.DSD..LLL..',
    '.......S........',
    '.......S........'],
];

// Planta silvestre: grade própria, baixa (linhas 5–15), dois leques assimétricos e cabeças de semente secas
const WILD_PLANT = [E, E, E, E, E,
  '..........hk....',
  '.........hHh....',
  '..L.......S.k...',
  '.L.L.....LS.hH..',
  'L..L..L.L.SLLh..',
  '.L.L.L..LLSDL...',
  '..LDL....LS.L.L.',
  'LLLDLLL...S.LL..',
  '...D.DD..DSD....',
  '......DDS.S.....',
  '.........SS.....',
];
const WILD = { L: '#7a9a4c', D: '#4f6a2e', S: '#5f7a36', h: '#8a6a3a', H: '#6a4e28', k: '#b09060' };

// ---------------------------------------------------------------- itens das variedades
const SEEDS = [E, E, E, E, E,
  '.....oo.........',
  '....obOo........',
  '....oOho....oo..',
  '....oObo...obOo.',
  '.....oo....oOho.',
  '...........oObo.',
  '......oo....oo..',
  '.....obOo.......',
  '.....oOho.......',
  '.....oObo.......',
  '......oo........',
];

// Flor fresca: cola alongada com folhas de açúcar, pistilos, geada e caule curvo
const FRESH_BUD = [
  '................',
  '.......bB.......',
  '......BPBb......',
  '.....LbBFBP.....',
  '....LLBBbBB.....',
  '.....bBPBFbL....',
  '.....BFBbBPLL...',
  '....LLbBBPBb....',
  '...LL.BPbBFB....',
  '......bBBbBL....',
  '.....LBFPBbLL...',
  '....LL.bBB......',
  '........S.......',
  '.......SL.......',
  '......S.........',
  '................',
];

// Flor seca: menor, densa, sem folhas de açúcar e com o caule aparado (silhueta diferente da fresca)
const DRIED_BUD = [E, E, E,
  '.......bB.......',
  '......bBPb......',
  '......BFbB......',
  '.....bBbPBb.....',
  '.....BPBBFB.....',
  '.....bBbBbB.....',
  '......BPbB......',
  '......bBFb......',
  '.......bB.......',
  '.......S........',
  E, E, E,
];

// ---------------------------------------------------------------- formatos por variedade (itens)
// As nativas ficam com os desenhos acima. Cada variedade do catálogo ganha um formato próprio dentro da família
// (variantFor): as sementes em outra arrumação, a flor fresca espelhada ou com outro desenho, a flor seca com outra
// silhueta. Junto com as cores (as sementes e as flores secas puxam o tom do botão da família), dois ícones de
// variedades diferentes sempre diferem em pelo menos 12 pixels bem visíveis (AssetCompletenessTest confere).
const mirror = (grid) => grid.map((row) => [...row].reverse().join(''));
const swapSymbols = (grid, pairs) => grid.map((row) => [...row].map((ch) => pairs[ch] ?? ch).join(''));

// Uma semente em pé (4x5) e deitada (5x4), com o mesmo contorno, corpo, brilho e listra das nativas
const SEED_V = ['.oo.', 'obOo', 'oOho', 'oObo', '.oo.'];
const SEED_H = ['.ooo.', 'oOObo', 'obhOo', '.ooo.'];

/** Grade 16x16 com três sementes: cada item é [V|H, coluna, linha] do canto de cima à esquerda. */
function seedGrid(placements) {
  const rows = Array.from({ length: 16 }, () => [...E]);
  for (const [glyph, x, y] of placements) {
    (glyph === 'V' ? SEED_V : SEED_H).forEach((row, dy) => [...row].forEach((ch, dx) => {
      if (ch === '.') return;
      if (rows[y + dy][x + dx] !== '.') throw new Error(`sementes sobrepostas em ${x + dx},${y + dy}`);
      rows[y + dy][x + dx] = ch;
    }));
  }
  return rows.map((r) => r.join(''));
}

const SEED_LAYOUTS = [
  [['V', 2, 2], ['V', 6, 6], ['V', 10, 10]], // em diagonal
  [['H', 1, 3], ['H', 10, 3], ['V', 6, 9]], // duas deitadas em cima, uma em pé embaixo
  [['V', 6, 1], ['V', 2, 8], ['V', 10, 8]], // triângulo
  [['H', 5, 2], ['H', 1, 7], ['H', 9, 12]], // zigue-zague deitado
  [['H', 5, 1], ['H', 5, 6], ['H', 5, 11]], // pilha deitada
  [['V', 1, 6], ['V', 6, 6], ['V', 11, 6]], // fileira em pé
].map(seedGrid);

// Flor fresca com duas colas no topo (as variantes 4 e 5)
const FRESH_TWIN_BUD = [
  '......P...P.....',
  '.....bBb.bBb....',
  '.....BPBbBPB....',
  '....LbFBBbFBL...',
  '...LLBBbPBBbLL..',
  '.....bBPBFbB....',
  '.....BFBbBPB....',
  '....LLbBBPBbL...',
  '...LL.BPbBFBLL..',
  '......bBBbBb....',
  '.....LBFPBbBL...',
  '....LL.bBBb.....',
  '........S.......',
  '.......SL.......',
  '......S.........',
  '................',
];
// Flor fresca com pistilos e geada trocados de lugar e o sombreado do botão invertido (as variantes 2 e 3)
const FRESH_SWAPPED = swapSymbols(FRESH_BUD, { P: 'F', F: 'P', B: 'b', b: 'B' });
const FRESH_VARIANTS = [FRESH_BUD, mirror(FRESH_BUD), FRESH_SWAPPED, mirror(FRESH_SWAPPED), FRESH_TWIN_BUD, mirror(FRESH_TWIN_BUD)];

// Flores secas do catálogo: seis silhuetas (redonda, lança, dois lobos, gota para cada lado e oval deitada)
const DRIED_LEAN = [E, E, E,
  '....bB..........',
  '....BPb.........',
  '....bBFb........',
  '.....BbPBb......',
  '.....bBBFBb.....',
  '......BPbBBb....',
  '......bBBPbB....',
  '.......bBBb.....',
  '........bS......',
  '.........S......',
  E, E, E,
];
const DRIED_VARIANTS = [
  [E, E, E,
    '.......bB.......',
    '......bBPBb.....',
    '.....bBFBbBb....',
    '.....BPbBPbB....',
    '.....bBBFBBb....',
    '.....BbPbBPB....',
    '......bBBbB.....',
    '.......bBb......',
    '........S.......',
    E, E, E, E],
  [E,
    '.......B........',
    '......bPb.......',
    '......BFB.......',
    '......bBP.......',
    '.....bPBBb......',
    '.....BBFbB......',
    '.....bBPBb......',
    '......BbBB......',
    '......bPBb......',
    '......BBFB......',
    '.......bB.......',
    '.......S........',
    '.......S........',
    E, E],
  [E, E, E, E, E,
    '....bB....Bb....',
    '...bBPBb.bPBBb..',
    '...BFbBBbBbFB...',
    '...bBPbBBPBbB...',
    '....BbBFbBBb....',
    '.....bBBbBb.....',
    '.......bB.......',
    '.......S........',
    E, E, E],
  DRIED_LEAN,
  mirror(DRIED_LEAN),
  [E, E, E,
    '..........bB....',
    '.........bBPb...',
    '........bBFBBb..',
    '.......bBPbBFb..',
    '......bBBbPBb...',
    '.....bBFBBbB....',
    '....bBPbBBb.....',
    '....bBBFb.......',
    '.....bBb........',
    '....S...........',
    '...S............',
    E, E],
];

// ---------------------------------------------------------------- itens genéricos e artigos
// Baseado: corpo (linhas 2–11 apagado, 1–11 aceso) e a boca (linhas 12–14), que muda com a piteira
const JOINT_BODY = [
  '.............tt.',
  '............wwt.',
  '...........wwwW.',
  '..........wwwWg.',
  '.........wwwWg..',
  '........wwwWg...',
  '.......wwwg.....',
  '......wwwg......',
  '.....wwg........',
  '....wwg.........',
];
const JOINT_LIT_BODY = [
  '.............m..',
  '.............ye.',
  '............wEr.',
  '...........wwwr.',
  '..........wwwWg.',
  '.........wwwWg..',
  '........wwwWg...',
  '.......wwwg.....',
  '......wwwg......',
  '.....wwg........',
  '....wwg.........',
];
// Sem piteira: o papel só afina e torce na boca
const JOINT_PAPER_END = [
  '...WWg..........',
  '..WWg...........',
  '..g.............',
];
// Com piteira: rolinho de papelão creme com o zigue-zague marrom, saindo 1 px além do papel
const JOINT_FILTER_END = [
  '...zcg..........',
  '..czk...........',
  '.zck............',
];
const JOINT_PALETTE = {
  w: '#f4f0e6', W: '#d8d0bc', g: '#b8ae96', t: '#e6dcc4',
  c: '#efe2c0', z: '#9a6a34', k: '#8a6a3a',
  y: '#ffd060', E: '#ff8a2a', e: '#e85a1a', r: '#a8301a', m: '#b4b4b4',
};

const ITEMS = {
  // Montinho de grânulos âmbar e três grânulos soltos
  resin: {
    grid: [E, E, E, E,
      '...........kk...',
      '..........khAk..',
      '...kk.....kAak..',
      '..khAk.....kk...',
      '..kAak..........',
      '...kk..kkkk.....',
      '......khAhAk....',
      '.....khAaAAak...',
      '....kAAhAaAhAk..',
      '...kAaAAAhAaAAk.',
      '...kkkkkkkkkkkk.',
      E,
    ],
    palette: { k: '#6e3e10', A: '#e0a040', a: '#a8661a', h: '#ffe2a0' },
  },
  // Baseado: cone na diagonal, fino na boca (papel torcido, embaixo à esquerda) e largo na ponta torcida
  joint: {
    grid: [E, E, ...JOINT_BODY, ...JOINT_PAPER_END, E],
    palette: JOINT_PALETTE,
  },
  // Com piteira (cultivo:filtered): a boca é um rolinho de papelão creme com o zigue-zague marrom, 1 px mais comprido
  joint_filtered: {
    grid: [E, E, ...JOINT_BODY, ...JOINT_FILTER_END, E],
    palette: JOINT_PALETTE,
  },
  // Bong: tubo de vidro, câmara com água, downstem de 1 px e fornilho de lado
  bong: {
    grid: [
      '.....gg.........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg...mm...',
      '.....gGg..mMMm..',
      '....gGGGg..mm...',
      '...gGGGGGgm.....',
      '..gGwwwwwmg.....',
      '..gwWWWWmWwg....',
      '..gwWWWWWWwg....',
      '..gwwWWWWwwg....',
      '...ggggggg......',
      E,
    ],
    palette: { g: '#4e8aa0', G: '#b9e2ee', w: '#5fa6c4', W: '#7fc4e0', m: '#5c5f66', M: '#9aa0a8' },
  },
  pipe: {
    grid: [E, E, E, E,
      '..bbbb..........',
      '.bBccBb.........',
      '.bBddBb.........',
      '.bBBBBb.........',
      '.bBBBBbbb.......',
      '..bBBBBBBbb.....',
      '...bbbbBBBBbb...',
      '.......bbBBBBbm.',
      '.........bbbbmm.',
      E, E, E,
    ],
    palette: { b: '#4a2c17', B: '#8b5a32', c: '#2a1a10', d: '#3a2416', m: '#d8c8a0' },
  },
  hookah: {
    grid: [
      '......mmmm......',
      '.......MM.......',
      '......mMMm......',
      '.......MM.......',
      '.......MM.......',
      '.......MM...hh..',
      '......mMMm...h..',
      '.......MM....h..',
      '.....gggggg..h..',
      '....gGGGGGGg.h..',
      '...gGwwwwwwGgh..',
      '...gwWWWWWWwg...',
      '...gwWWWWWWwg...',
      '....gwWWWWwg....',
      '.....gggggg.....',
      E,
    ],
    palette: { m: '#8a6a2a', M: '#d4b25a', g: '#3f6d52', G: '#9fd4b4', w: '#4e9a78', W: '#79c39f', h: '#2f2f38' },
  },
  // Chillum: cone de cerâmica com boca larga em cima e duas faixas claras
  chillum: {
    grid: [E,
      '..........cccc..',
      '.........cdddCc.',
      '.........cdddCc.',
      '.........ccCCc..',
      '........cCCCc...',
      '.......cBBBc....',
      '......cCCCc.....',
      '.....cCCCc......',
      '....cBBBc.......',
      '...cCCCc........',
      '...cCCc.........',
      '..cCCc..........',
      '..ccc...........',
      E, E,
    ],
    palette: { c: '#7a3d22', C: '#c06a3e', B: '#e8b088', d: '#3a1d10' },
  },
  // Livreto de papelão com uma folha branca saindo 3 px por cima
  rolling_paper: {
    grid: [E, E, E,
      '......wwwwww....',
      '......wWWWWw....',
      '......wwwwww....',
      '...kkkkkkkkkk...',
      '...kRRRRRRRRk...',
      '...kRyyyyyyRk...',
      '...kRyRRRRyRk...',
      '...kRyyyyyyRk...',
      '...kRRRRRRRRk...',
      '...kddddddddk...',
      '...kkkkkkkkkk...',
      E, E,
    ],
    palette: { k: '#5a2a1a', R: '#b8442e', d: '#8a3424', y: '#f0c860', w: '#fbf8f0', W: '#dcd4c4' },
  },
  // Piteira: um rolinho de papelão na diagonal, com o zigue-zague marrom na ponta visível
  filter_tip: {
    grid: [E, E, E,
      '..........kkk...',
      '.........kCzCk..',
      '........kzCzCk..',
      '.......kcCzCzk..',
      '......kcccCzk...',
      '.....kcccckk....',
      '....kcccck......',
      '...kcccck.......',
      '..kccck.........',
      '..kcck..........',
      '...kk...........',
      E, E,
    ],
    palette: { k: '#8a6a3a', c: '#efe2c0', C: '#fbf3dc', z: '#9a6a34' },
  },
  // Dichavador: cilindro de duas partes em 3/4, borda serrilhada e uma folha na tampa
  grinder: {
    grid: [E, E,
      '.....kkkkkk.....',
      '...kkMMMMMMkk...',
      '..kMMMMgMMMhMk..',
      '..kMMMgggMMMMk..',
      '..kMMMMgMMMMMk..',
      '..kkMMMMMMMMkk..',
      '..kHhHhHhHhHhk..',
      '..kHhHhHhHhHhk..',
      '..kkkkkkkkkkkk..',
      '..kGGGGGGGGGGk..',
      '..kGgGgGgGgGGk..',
      '...kkkkkkkkkk...',
      E, E,
    ],
    palette: { k: '#2e3338', M: '#9aa4ae', h: '#e2e8ec', g: '#4f9a4a', H: '#c4ccd4', G: '#6c7680' },
  },
  lighter: {
    grid: [E, E,
      '......kkkk......',
      '.....kMMMMk.....',
      '.....kMhMgk.....',
      '.....kkkkkk.....',
      '.....kRRRRk.....',
      '.....kRrRRk.....',
      '.....kRrRRk.....',
      '.....kRrRRk.....',
      '.....kRRRRk.....',
      '.....kRRRRk.....',
      '.....kRRRdk.....',
      '.....kRRddk.....',
      '......kkkk......',
      E,
    ],
    palette: { k: '#2a2a30', M: '#a7afb7', h: '#e4e8ec', g: '#5d6670', R: '#2f7fc1', r: '#7fb8e6', d: '#20578a' },
  },
  // Bandeja de metal em 3/4: borda alta, cantos arredondados, fundo esmaltado verde, uma flor e uma folha
  rolling_tray: {
    grid: [E, E, E, E,
      '...kkkkkkkkkk...',
      '..kRRRRRRRRRRk..',
      '.kRggggggggggRk.',
      '.kRgwwwgggBbgRk.',
      '.kRgwwwggbBBgRk.',
      '.kRgggggggSggRk.',
      '.kRggggggggggRk.',
      '.krRRRRRRRRRRrk.',
      '..krrrrrrrrrrk..',
      '...kkkkkkkkkk...',
      E, E,
    ],
    palette: { k: '#3a3e44', R: '#b8c0c8', r: '#7c848c', g: '#3f8a5a', w: '#f6f2e8', B: '#9cc46a', b: '#5e8a3a', S: '#6b5a3a' },
  },
  // Pote: vidro com conteúdo escuro na metade de baixo e um reflexo
  jar: {
    grid: [
      E,
      '....wwwwwwww....',
      '...wWWWWWWWWw...',
      '...wwwwwwwwww...',
      '....gggggggg....',
      '...gGGGGGGGGg...',
      '...gGhGGGGGGg...',
      '...gGhGGGGGGg...',
      '...gGhGGGGGGg...',
      '...gGcpcCcpcg...',
      '...gcCpcpCcpg...',
      '...gpcCcpcCcg...',
      '...gcpcCpccCg...',
      '....gggggggg....',
      E, E,
    ],
    palette: { w: '#6b4a2a', W: '#a27448', g: '#6f9aa6', G: '#cfe8ee', h: '#ffffff', c: '#3f6a2e', C: '#5a8a3a', p: '#5e3f6e' },
  },
  // Cinzeiro de terracota visto de cima: três entalhes na borda e cinza no meio
  ashtray: {
    grid: [E, E, E,
      '.....cc..cc.....',
      '...ccTT..TTcc...',
      '..cTTttttttTTc..',
      '..cTtaaAaaatTc..',
      '.cTtaAaaaAaatTc.',
      '.TTtaaaAaaaat...',
      '.cTtaAaaaAaatTc.',
      '..cTtaaaAaatTc..',
      '..cTTttttttTTc..',
      '...ccTT..TTcc...',
      '.....cc..cc.....',
      E, E,
    ],
    palette: { c: '#7a3a1c', T: '#b5653a', t: '#8e4a28', a: '#8a8a86', A: '#c4c4be' },
  },
  // Estufinha: tenda escura de frente aberta, a lâmpada roxa no alto, mylar iluminado por dentro e uma muda no vaso
  grow_tent: {
    grid: [
      E,
      '..kkkkkkkkkkkk..',
      '..kcLLLLLLLLck..',
      '..kcmPPPPPPmck..',
      '..kcmmmggmmmck..',
      '..kcmmgGGgmmck..',
      '..kcmgGgGGgmck..',
      '..kcmgGGgGgmck..',
      '..kcmmgGGgmmck..',
      '..kcmmmgGmmmck..',
      '..kcmmmoommmck..',
      '..kcmoooooomck..',
      '..kcmoooooomck..',
      '..kcccccccccck..',
      '..kkkkkkkkkkkk..',
      E,
    ],
    palette: {
      k: '#1e1e22', c: '#34343a', L: '#e0a8ff', P: '#b04dff', m: '#8e8a9e', g: '#5aa874', G: '#3d6b2c', o: '#2a2a2e',
    },
  },
};

// Haxixe: placa marrom-escura em 3/4, bisel de 1 px e uma folha de 5 folíolos carimbada (a cor do carimbo muda
// com a família da variedade; sem variedade, neutro)
const HASH = [E, E, E,
  '...hhhhhhhhhh...',
  '..hTTTTTTTTTTk..',
  '..hTTTTsTTTTTk..',
  '..hTTsTsTsTTTk..',
  '..hTTTsssTTTTk..',
  '..hTTTTsTTTTTk..',
  '..hTTTTTTTTTTk..',
  '..kSSSSSSSSSSk..',
  '..kSSSSSSSSSSk..',
  '...kkkkkkkkkk...',
  E, E, E,
];
const HASH_PALETTE = { h: '#6a4a30', T: '#4a2f1c', S: '#2e1c10', k: '#1e120a' };
const HASH_NEUTRAL_STAMP = '#7a5a3a';

// ---------------------------------------------------------------- blocos das estações
/** Gerador pseudoaleatório fixo (as texturas saem sempre iguais). */
function lcg(seed) {
  let x = seed >>> 0;
  return () => {
    x = (Math.imul(x, 1664525) + 1013904223) >>> 0;
    return x / 4294967296;
  };
}

/**
 * Grade de veio horizontal: cada linha tem o tom base com riscos escuros ('w') e claros ('l') de 3 a 8 px. As
 * linhas de "anel" (a cada 4) ganham um risco escuro longo, como as tábuas vanilla.
 */
function grainGrid(seed, base, dark, light) {
  const rand = lcg(seed);
  return Array.from({ length: 16 }, (_, y) => {
    const row = Array(16).fill(base);
    const streaks = y % 4 === 3 ? [[dark, 7 + Math.floor(rand() * 5)]] : [];
    streaks.push([rand() < 0.6 ? dark : light, 3 + Math.floor(rand() * 4)]);
    if (rand() < 0.5) streaks.push([light, 2 + Math.floor(rand() * 3)]);
    for (const [ch, len] of streaks) {
      const start = Math.floor(rand() * 16);
      for (let i = 0; i < len; i++) row[(start + i) % 16] = ch;
    }
    return row.join('');
  });
}

// Madeira com veio horizontal: as peças verticais giram o UV 90° no modelo para o veio seguir a peça
const WOOD_GRID = grainGrid(7, 'W', 'w', 'l');
// Ferro escovado: riscos horizontais claros e escuros
const IRON_GRID = grainGrid(11, 'I', 'i', 'h');
// Grânulos de resina (âmbar), usados na peneira pronta e na prensa trabalhando
const RESIN_GRID = [
  'AaAAhAAaAAAhAaAA',
  'AAAaAAhAAaAAAAhA',
  'hAaAAAAaAAhAaAAA',
  'AAAAhAaAAAAAAAaA',
  'AaAAAAAAhAaAhAAA',
  'AAhAaAAAAAAAAaAA',
  'aAAAAhAaAAhAAAAh',
  'AAaAAAAAAaAAaAAA',
  'AAAAhAaAAAAAAAhA',
  'hAaAAAAAhAAaAAAA',
  'AAAAaAhAAAaAAhAa',
  'AaAAAAAAAAAAaAAA',
  'AAAhAaAAhAAAAAhA',
  'AhAAAAAaAAAhAaAA',
  'AAAaAhAAAAAAAAAA',
  'aAAAAAAAhAaAAhAa',
];
const RESIN_PALETTE = { A: '#d8923a', a: '#a05c1a', h: '#f6c870' };

// Maço pendurado no suporte: só a janela de UV [6,2,10,10] (4x8) aparece no modelo. Barbante em cima, talos,
// folhas e flores viradas para baixo, afinando na ponta.
const BUNDLE = [E, E,
  '......ssss......',
  '......SLLS......',
  '......LSSL......',
  '......LBBL......',
  '......BbPB......',
  '......BBBb......',
  '......bPBB......',
  '.......Bb.......',
  E, E, E, E, E, E,
];
/** Maço fresco (suporte trabalhando ou carregado). */
const BUNDLE_FRESH_PALETTE = { s: '#e6dcb4', S: '#5f8a34', L: '#6aae3e', B: '#a8cc6a', b: '#5e8a3a', P: '#f0a640' };

// Flores secas espalhadas na tela da peneira (cutout): moitas verdes com pontos roxos e laranja
const SIEVE_FLOWERS = [
  'GgGGgGGgGGgGGgGG',
  '................',
  '................',
  '...GgG.gGG..gG..',
  '..gGpGGGoGg.GGg.',
  '...GGg.gGGGgGpG.',
  '....gGGGg.GGgG..',
  '...gGoGgGGg..gG.',
  '..GGGgG..gGpGGg.',
  '...gGpG.GgGGgG..',
  '....GGgGGoGg....',
  '...gGGgGgGGGgG..',
  '..GgoGG.gGpGGg..',
  '...GgGg..gGGg...',
  '................',
  '................',
];
const SIEVE_FLOWERS_PALETTE = { G: '#8a9a4a', g: '#5f6b30', p: '#7a4f78', o: '#c07a30' };

const BLOCKS = {
  drying_rack_wood: { grid: WOOD_GRID, palette: { W: '#a87c4c', w: '#8a6038', l: '#c49a66' } },
  drying_bundle_dried: {
    grid: BUNDLE,
    palette: { s: '#e6dcb4', S: '#6b5a3a', L: '#7f7a3a', B: '#a8a05a', b: '#6a6232', P: '#b0702c' },
  },
  // Maço fresco parado: o suporte carregado mas sem o ciclo andando (a versão animada é drying_bundle_fresh)
  drying_bundle_fresh_still: { grid: BUNDLE, palette: BUNDLE_FRESH_PALETTE },
  sieve_wood: { grid: WOOD_GRID, palette: { W: '#8a5c33', w: '#6e4727', l: '#a87344' } },
  // Flores paradas na tela: a peneira carregada mas sem o ciclo andando (a versão animada é sieve_flowers)
  sieve_flowers_still: { grid: SIEVE_FLOWERS, palette: SIEVE_FLOWERS_PALETTE },
  // Tela: arame de 1 px com vãos de 2 px (cutout)
  sieve_mesh: {
    grid: Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
      if (y % 3 === 0 && x % 3 === 0) return 'N';
      return y % 3 === 0 || x % 3 === 0 ? 'm' : '.';
    }).join('')),
    palette: { m: '#9a9690', N: '#6a6660' },
  },
  sieve_resin: { grid: RESIN_GRID, palette: RESIN_PALETTE },
  press_wood: { grid: WOOD_GRID, palette: { W: '#6e4a2a', w: '#553820', l: '#8a5e36' } },
  press_iron: { grid: IRON_GRID, palette: { I: '#8c96a0', i: '#5c656e', h: '#c8d0d8' } },
  press_resin: { grid: RESIN_GRID, palette: RESIN_PALETTE },
  // Barra de haxixe na base da prensa (topo em UV [5,5,11,11] com o carimbo)
  press_hash: {
    grid: [
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSShhhhhhSSSSS',
      'SSSSShTTsTTSSSSS',
      'SSSSShTsssTSSSSS',
      'SSSSShTTsTTSSSSS',
      'SSSSShTTTTTSSSSS',
      'SSSSShTTTTTSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
      'SSSSSSSSSSSSSSSS',
    ],
    palette: { S: '#3a2414', h: '#6a4a30', T: '#4a2f1c', s: '#7a5a3a' },
  },
  // Dichavador na peneira (cilindro 4x4x2, UV 1:1): tampa em cima com a borda arredondada e a folha [0,0,4,4], fundo
  // [4,0,8,4]; a lateral (linhas 8–9, uma região de 4 px por face) tem a tampa serrilhada e o corpo mais escuro
  grinder_block: {
    grid: [
      'kMMkdGGdMMMMMMMM',
      'MhMMGGGGMMMMMMMM',
      'MMgMGGGGMMMMMMMM',
      'kMMkdGGdMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'HkHkHkHkHkHkHkHk',
      'GGdGGGdGGGdGGGdG',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
      'MMMMMMMMMMMMMMMM',
    ],
    palette: { k: '#4a525a', M: '#9aa4ae', h: '#e2e8ec', g: '#4f9a4a', H: '#d4dce2', G: '#6c7680', d: '#4a525a' },
  },
  // Bandeja colocada: fundo esmaltado verde [0,0,12,10], folha de seda [12,0,15,4] com a faixa de cola, botão em cima
  // [12,4,14,6] e de lado [12,6,16,7.5], beirada do fundo (linha 10), borda: topo (linha 12) e lado (linha 13)
  rolling_tray_block: {
    grid: [
      'GGGGGGGGGGGGwwyw',
      'GggggggggggGwwyW',
      'GgghgggggggGwwyw',
      'GggggggggggGwwyW',
      'GgggggghgggGbBPB',
      'GggggggggggGBbBb',
      'GgggghgggggGbBBb',
      'GggggggggggGBPbB',
      'GggggggggggGGGGG',
      'GGGGGGGGGGGGGGGG',
      'rrrrrrrrrrrrrrrr',
      'kkkkkkkkkkkkkkkk',
      'RRHRRRRRRHRRRRRR',
      'MMMMMMMMMMMMMMMM',
      'kkkkkkkkkkkkkkkk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: {
      G: '#2f6e48', g: '#3f8a5a', h: '#5aa874', w: '#f6f2e8', W: '#e6e0d2', y: '#ece0b8',
      B: '#9cc46a', b: '#5e8a3a', P: '#f0a640', r: '#7c848c', k: '#3a3e44', R: '#c4ccd4', H: '#eef2f6', M: '#8e969e',
    },
  },
  // Vidro do pote (cutout): lateral [5,8,11,16] com borda, reflexo e fundo opacos e o resto transparente; anel do fundo
  // em [5,1,11,7]. Nada de alfa parcial (TextureArtTest): o vidro some onde não é borda.
  jar_glass: {
    grid: [
      E,
      '.....DDDDDD.....',
      '.....D....D.....',
      '.....D....D.....',
      '.....D....D.....',
      '.....D....D.....',
      '.....DDDDDD.....',
      E,
      '.....eeeeee.....',
      '.....Eh...E.....',
      '.....Eh...E.....',
      '.....Ehh..E.....',
      '.....Eh...E.....',
      '.....E....E.....',
      '.....E...hE.....',
      '.....DDDDDD.....',
    ],
    palette: { e: '#e4f4f8', E: '#a8d0d8', h: '#ffffff', D: '#7fa8b2' },
  },
  jar_lid: { grid: grainGrid(51, 'W', 'w', 'l'), palette: { W: '#a27448', w: '#6b4a2a', l: '#b88a5c' } },
  // Flores secas atrás do vidro do pote: botões oliva com pontos roxos, laranja e de geada
  jar_contents: {
    grid: material(53, 'B', [['b', 0.32], ['p', 0.09], ['o', 0.06], ['F', 0.05]]),
    palette: { B: '#7f9450', b: '#56662e', p: '#7d4f78', o: '#c07a30', F: '#d8d0b0' },
  },
  // Cinzeiro colocado: o topo (linhas 4–11, colunas 4–11) é a borda clara com três entalhes escuros e o fundo por
  // dentro; as laterais (linhas 14–15) vão do claro ao escuro
  ashtray_block: {
    grid: [
      'TTTTTTTTTTTTTTTT',
      'TTTTTTTTTTTTTTTT',
      'TTTTTTTTTTTTTTTT',
      'TTTTTTTTTTTTTTTT',
      'TTTTlllcclllTTTT',
      'TTTTltffffflTTTT',
      'TTTTlfffffflTTTT',
      'TTTTcfftfffcTTTT',
      'TTTTcffffffcTTTT',
      'TTTTlfffffflTTTT',
      'TTTTlffffftlTTTT',
      'TTTTllllllllTTTT',
      'TTTTTTTTTTTTTTTT',
      'TTTTTTTTTTTTTTTT',
      'TTlTTTlTTTTlTTTT',
      'tttttttttttttttt',
    ],
    palette: { T: '#b5653a', t: '#8e4a28', l: '#cf8456', c: '#6a2e14', f: '#9a5530' },
  },
  // Cinza: cinza-claro com pontos escuros e claros
  ashtray_ash: {
    grid: material(55, 'a', [['A', 0.3], ['d', 0.16]]),
    palette: { a: '#8a8a86', A: '#b4b4ae', d: '#5c5c58' },
  },
  // Estufinha (grow tent). Lona: tecido grafite em trama, com uma costura vertical pespontada de roxo
  grow_tent_canvas: { grid: tentCanvasGrid(), palette: { C: '#2b2b30', c: '#34343a', s: '#1f1f24', p: '#6a3fa0' } },
  // Mylar por dentro: prata em losangos tingida de lilás pela lâmpada roxa (o modelo faz o mylar brilhar)
  grow_tent_mylar: { grid: tentMylarGrid(), palette: { M: '#cbb8ec', m: '#a58ad2', l: '#f1e4ff' } },
  // Postes de metal escuro
  grow_tent_frame: { grid: grainGrid(71, 'F', 'f', 'h'), palette: { F: '#55555e', f: '#44444b', h: '#6e6e78' } },
  // Aba enrolada: dobras horizontais da lona e o puxador dourado do zíper no meio (linhas 3–6)
  grow_tent_flap: { grid: tentFlapGrid(), palette: { C: '#34343a', c: '#2b2b30', d: '#1f1f24', z: '#c9b037', Z: '#8a7a22' } },
  // Lâmpada: caixa preta com aletas (linhas 0–3) e a grade de LEDs roxos (linhas 4–15; o ponto 7,7 é um LED aceso)
  grow_tent_lamp: { grid: tentLampGrid(), palette: { k: '#1e1e22', K: '#2c2c33', d: '#7a1fd1', p: '#b04dff', w: '#e0a8ff' } },
  // Vaso de tecido: substrato escuro em cima (linhas 0–5) e tecido preto costurado nas laterais (linhas 6–15)
  grow_tent_pot: { grid: tentPotGrid(), palette: { s: '#4a3222', S: '#3a2618', b: '#6a4a30', k: '#202024', K: '#2c2c32', t: '#44444c' } },
};

/** Lona da estufinha: trama em xadrez fino e a costura nas colunas 7–8, com ponto roxo a cada 2 linhas. */
function tentCanvasGrid() {
  return Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
    if (x === 7) return 's';
    if (x === 8) return y % 2 === 0 ? 'p' : 's';
    return (x + y) % 2 === 0 ? 'C' : 'c';
  }).join(''));
}

/** Mylar: losangos de 4 px (as diagonais escuras) e um reflexo lilás no meio de alguns losangos. */
function tentMylarGrid() {
  return Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
    if ((x + y) % 4 === 0 || (x - y + 16) % 4 === 0) return 'm';
    if (x % 8 === 2 && y % 8 === 0) return 'l';
    if (x % 8 === 6 && y % 8 === 4) return 'l';
    return 'M';
  }).join(''));
}

/** Aba enrolada: faixas de dobra a cada 3 linhas e o puxador do zíper nas colunas 7–8, linhas 3–6. */
function tentFlapGrid() {
  return Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
    if ((x === 7 || x === 8) && y >= 3 && y <= 6) return y === 6 ? 'Z' : 'z';
    if (y % 3 === 2) return 'd';
    return y % 3 === 0 ? 'C' : 'c';
  }).join(''));
}

/** Lâmpada: aletas da caixa nas linhas 0–3 e LEDs nas linhas 4–15 (aceso no centro de cada célula de 2 px). */
function tentLampGrid() {
  return Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
    if (y < 4) return x % 2 === 0 && y % 2 === 1 ? 'K' : 'k';
    if (x % 2 === 0 || y % 2 === 0) return 'd';
    return (x + y) % 4 === 2 ? 'w' : 'p';
  }).join(''));
}

/** Vaso: substrato com pedrinhas nas linhas 0–5 e tecido com uma costura clara na linha 12. */
function tentPotGrid() {
  const rand = lcg(73);
  return Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
    if (y < 6) {
      const r = rand();
      return r < 0.25 ? 'S' : r < 0.35 ? 'b' : 's';
    }
    if (y === 12) return x % 2 === 0 ? 't' : 'k';
    return (x + y) % 3 === 0 ? 'K' : 'k';
  }).join(''));
}

// ---------------------------------------------------------------- texturas dos modelos 3D na mão
// Cada aparelho de fumo tem um modelo de elementos para a mão (o ícone plano continua no inventário). Os modelos
// usam uma textura por material, com um padrão leve; qualquer região de UV da peça cai dentro do material.
/** Grade 16x16 de um material: tom base com pontos/riscos determinísticos de outros tons. */
function material(seed, base, marks) {
  const rand = lcg(seed);
  return Array.from({ length: 16 }, () => [...Array(16)].map(() => {
    const r = rand();
    let acc = 0;
    for (const [ch, p] of marks) {
      acc += p;
      if (r < acc) return ch;
    }
    return base;
  }).join(''));
}
/** Vidro: colunas de brilho fixas (o brilho aparece em pé nas faces laterais do tubo). */
const GLASS_GRID = Array.from({ length: 16 }, (_, y) => [...Array(16)].map((__, x) => {
  if (x % 8 === 0) return 'g';
  if (x % 8 === 2 || (x % 8 === 3 && y % 3 !== 0)) return 'h';
  return 'G';
}).join(''));
const MODEL_TEXTURES = {
  model_glass: { grid: GLASS_GRID, palette: { G: '#a8d8e8', g: '#78b0c4', h: '#e8f8ff' } },
  model_water: { grid: material(21, 'w', [['W', 0.25], ['s', 0.06]]), palette: { w: '#4e94b4', W: '#5fa6c4', s: '#a8dcf0' } },
  model_green_glass: { grid: GLASS_GRID, palette: { G: '#4e9a78', g: '#3f6d52', h: '#9fd4b4' } },
  model_brass: { grid: grainGrid(23, 'M', 'm', 'h'), palette: { M: '#d4b25a', m: '#a8863a', h: '#f0d890' } },
  model_hose: { grid: material(25, 'h', [['H', 0.3]]), palette: { h: '#2f2f38', H: '#4a4a56' } },
  model_clay: { grid: material(27, 'C', [['c', 0.25], ['l', 0.12]]), palette: { C: '#c06a3e', c: '#9a4e2a', l: '#d88a5a' } },
  model_clay_band: { grid: material(29, 'B', [['b', 0.3]]), palette: { B: '#e8b088', b: '#d49a72' } },
  model_wood: { grid: grainGrid(31, 'W', 'w', 'l'), palette: { W: '#8b5a32', w: '#6e4426', l: '#a87344' } },
  model_dark_wood: { grid: grainGrid(33, 'W', 'w', 'l'), palette: { W: '#5a3820', w: '#4a2c17', l: '#6e4a2c' } },
  model_bone: { grid: material(35, 'm', [['M', 0.3]]), palette: { m: '#e8dcc0', M: '#d0c4a4' } },
  model_paper: { grid: material(37, 'w', [['W', 0.12]]), palette: { w: '#f4f0e6', W: '#e2dccc' } },
  model_cardboard: { grid: material(39, 'c', [['C', 0.3]]), palette: { c: '#c8a26a', C: '#a8824e' } },
  model_plastic: { grid: material(41, 'R', [['r', 0.12], ['d', 0.15]]), palette: { R: '#2f7fc1', r: '#7fb8e6', d: '#20578a' } },
  model_steel: { grid: grainGrid(43, 'M', 'g', 'h'), palette: { M: '#a7afb7', g: '#5d6670', h: '#e4e8ec' } },
  // Cinza apagada e brasa acesa, no fornilho/ponta (o modelo "_lit" troca uma pela outra)
  smoke_ash: { grid: material(45, 'a', [['A', 0.3]]), palette: { a: '#3a3636', A: '#6a6662' } },
};

// ---------------------------------------------------------------- texturas animadas (.png.mcmeta)
/** Desloca uma linha da grade em `dx` colunas, preenchendo com transparente (sem dar a volta). */
function shiftRow(row, dx) {
  if (dx === 0) return row;
  const cells = [...row];
  const out = Array(cells.length).fill('.');
  cells.forEach((ch, x) => {
    if (x + dx >= 0 && x + dx < cells.length) out[x + dx] = ch;
  });
  return out.join('');
}

/** Ponta do baseado aceso, quadro a quadro: [linha 0, fumaça, brasa de cima, brasa de baixo]. */
const JOINT_EMBER_FRAMES = [
  // brasa normal, 1 pixel de fumaça logo acima
  [E, '.............m..', '.............ye.', '............wEr.'],
  // a brasa aviva e a fumaça sobe
  ['..............m.', E, '.............Ey.', '............wyE.'],
  // a brasa esmorece, sem fumaça
  [E, E, '.............er.', '............wer.'],
  // nova baforada saindo
  [E, '............m...', '.............yE.', '............wEe.'],
];
const jointLitFrames = (end) => JOINT_EMBER_FRAMES.map((top) => ({
  grid: [...top, ...JOINT_LIT_BODY.slice(3), ...end, E],
  palette: JOINT_PALETTE,
}));

/** Água do bong borbulhando (só no modelo aceso): bolhas claras sobem 2 px por quadro em colunas defasadas. */
const BUBBLE_COLUMNS = [[1, 3], [4, 11], [6, 6], [9, 14], [11, 1], [14, 9]];
const WATER_BASE = material(21, 'w', [['W', 0.25], ['s', 0.02]]);
const bubblingWaterFrames = Array.from({ length: 8 }, (_, f) => {
  const rows = WATER_BASE.map((row) => [...row]);
  for (const [x, phase] of BUBBLE_COLUMNS) {
    const y = (((phase - 2 * f) % 16) + 16) % 16;
    rows[y][x] = 'o';
    rows[(y + 1) % 16][x] = 'O';
  }
  return { grid: rows.map((r) => r.join('')), palette: { w: '#4e94b4', W: '#5fa6c4', s: '#a8dcf0', o: '#ffffff', O: '#bfe8f8' } };
});

/** Brasa dos modelos acesos (fornilho, ponta do baseado): os pontos quentes mudam de lugar e o brilho pulsa. */
const EMBER_PALETTES = [
  { e: '#ff7a22', r: '#c83a14', y: '#ffd060' },
  { e: '#ff8c30', r: '#d84a1c', y: '#ffe488' },
  { e: '#ff7a22', r: '#c83a14', y: '#ffd060' },
  { e: '#f06a1c', r: '#b0300e', y: '#ffc04a' },
];
const emberFrames = EMBER_PALETTES.map((palette, f) => ({
  grid: material(47 + 100 * f, 'e', [['r', 0.3], ['y', 0.2]]),
  palette,
}));

/** Carvão do narguilé aceso: o carvão escuro fica parado e as rachaduras acendem e apagam devagar. */
const COAL_GRID = material(61, 'c', [['C', 0.25], ['a', 0.2], ['g', 0.14], ['y', 0.04]]);
const coalFrames = ['#a8300c', '#c83a10', '#e85418', '#ff7a28', '#e85418', '#c83a10'].map((g, f) => ({
  grid: COAL_GRID,
  palette: { c: '#2a2420', C: '#3a322c', a: '#8a8682', g, y: ['#e8803a', '#ff9a40', '#ffb850', '#ffd070', '#ffb850', '#ff9a40'][f] },
}));

/** Maço fresco no suporte trabalhando: a ponta de baixo balança de leve para os lados (só a janela UV [6,2,10,10]). */
const bundleSwayFrames = [0, 1, 0, -1].map((dx) => ({
  grid: BUNDLE.map((row, y) => (y >= 8 ? shiftRow(row, dx) : row)),
  palette: BUNDLE_FRESH_PALETTE,
}));

/** Flores na tela da peneira trabalhando: as linhas tremem 1 px para um lado e para o outro, como numa peneira sacudida. */
const sieveShakeFrames = [[0, 0], [1, -1], [0, 0], [-1, 1]].map(([odd, even]) => ({
  grid: SIEVE_FLOWERS.map((row, y) => shiftRow(row, y % 2 ? odd : even)),
  palette: SIEVE_FLOWERS_PALETTE,
}));

/**
 * Texturas animadas: quadros e frametime (ticks por quadro). Todas aparecem enquanto algo acontece: o baseado aceso e
 * a brasa só enquanto o jogador fuma, a água borbulhando só no bong aceso, o maço fresco e as flores da peneira só com a
 * estação trabalhando.
 */
export const ANIMATED = {
  'item/joint_lit': { frames: jointLitFrames(JOINT_PAPER_END), frametime: 4 },
  'item/joint_filtered_lit': { frames: jointLitFrames(JOINT_FILTER_END), frametime: 4 },
  'item/smoke_ember': { frames: emberFrames, frametime: 3 },
  'item/model_water_bubbling': { frames: bubblingWaterFrames, frametime: 2 },
  'item/hookah_coal': { frames: coalFrames, frametime: 4 },
  'block/drying_bundle_fresh': { frames: bundleSwayFrames, frametime: 8 },
  'block/sieve_flowers': { frames: sieveShakeFrames, frametime: 2 },
};

// Ícone do efeito cultivo:high (18x18): folha estilizada com um fio de fumaça
const EFFECTS = {
  high: {
    grid: [
      '..................',
      '.......ww.........',
      '......w..w........',
      '.........w........',
      '........L.........',
      '.......LLL........',
      '..L....LLL....L...',
      '..LL...LGL...LL...',
      '...LL..LGL..LL....',
      '...LLL.LGL.LLL....',
      '....LLLLGLLLL.....',
      '.LL...LLGLL...LL..',
      '..LLLL.LGL.LLLL...',
      '....LLLLGLLLL.....',
      '........G.........',
      '........G.........',
      '.......GG.........',
      '..................',
    ],
    palette: { L: '#6aa84f', G: '#3d6b2c', w: '#e8e8e8c8' },
  },
};

/** Número de estágios visuais da lavoura (um por idade 0–7). */
export const CROP_STAGE_COUNT = CROP_STAGES.length;

/** IDs (sem namespace) das texturas de bloco das estações e dos modelos 3D na mão, para o gerador conferir. */
export const STATION_TEXTURES = Object.keys(BLOCKS);
export const MODEL_TEXTURE_IDS = Object.keys(MODEL_TEXTURES);

// Bong no chão: o 26.3 recusa um modelo de bloco com sprites do atlas de itens ("outside of supported atlas"), então o
// bloco usa cópias em block/ das mesmas artes do modelo na mão
Object.assign(BLOCKS, {
  bong_glass: MODEL_TEXTURES.model_glass,
  bong_water: MODEL_TEXTURES.model_water,
  bong_steel: MODEL_TEXTURES.model_steel,
  bong_ash: MODEL_TEXTURES.smoke_ash,
});

/** @param strains seção "strains" do manifesto (id e family de cada variedade) */
export function generateAll(strains) {
  const written = [];
  // Lavoura: as texturas são por família (as variedades da mesma família compartilham a planta no campo)
  for (const family of artFamilies(strains)) {
    const base = familyBase(family);
    if (!base) throw new Error(`família de arte desconhecida: ${family}`);
    CROP_STAGES.forEach((grid, stage) => written.push(render(`block/${family}_crop_stage${stage}`, grid, base.plant)));
    written.push(render(`item/hash_${family}`, HASH, { ...HASH_PALETTE, s: base.stamp }));
  }
  // Sementes e flores: por variedade (paleta da família com o destaque da variedade)
  for (const [id, c] of Object.entries(strainPalettes(strains))) {
    const native = c.variant === null;
    written.push(render(`item/${id}_seeds`, native ? SEEDS : SEED_LAYOUTS[c.variant], c.seed));
    written.push(render(`item/${id}_fresh_flower`, native ? FRESH_BUD : FRESH_VARIANTS[c.variant], c.plant));
    written.push(render(`item/${id}_dried_flower`, native ? DRIED_BUD : DRIED_VARIANTS[c.variant], c.dried));
  }
  written.push(render('block/wild_plant', WILD_PLANT, WILD));
  written.push(render('item/hash', HASH, { ...HASH_PALETTE, s: HASH_NEUTRAL_STAMP }));
  for (const [id, t] of Object.entries(ITEMS)) written.push(render(`item/${id}`, t.grid, t.palette));
  for (const [id, t] of Object.entries(MODEL_TEXTURES)) written.push(render(`item/${id}`, t.grid, t.palette));
  for (const [id, t] of Object.entries(BLOCKS)) written.push(render(`block/${id}`, t.grid, t.palette));
  for (const [id, t] of Object.entries(EFFECTS)) written.push(render(`mob_effect/${id}`, t.grid, t.palette));
  for (const [id, a] of Object.entries(ANIMATED)) written.push(renderAnimation(id, a.frames, { frametime: a.frametime }));
  return written;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const manifest = JSON.parse(readFileSync(join(ROOT, 'tools', 'gen', 'manifest.json'), 'utf8'));
  const files = generateAll(manifest.strains);
  console.log(`${files.length} texturas geradas em ${OUT}`);
}
