// Gera todas as texturas originais 16x16 do Cultivo a partir de pixel art em ASCII.
// Uso: node tools/art/textures.mjs   (escreve em src/generated/resources/assets/cultivo/textures)
// Cada textura é uma grade de 16 linhas x 16 colunas; '.' é transparente e os demais símbolos vêm da paleta.
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

// ---------------------------------------------------------------- variedades
// Paletas próprias das nativas: a família de arte delas é o próprio ID (brisa, aurora, eclipse)
const NATIVE_PALETTES = {
  brisa: {
    leaf: '#7cc142', leafDark: '#4e8f2a', stem: '#5f8f32',
    bud: '#cfe38a', budDark: '#9fbf55', pistil: '#e8a04a',
    seed: '#c9a873', seedDark: '#6e5434', seedHi: '#ead6a8',
    dried: '#b7b56a', driedDark: '#7f7c3f', driedPistil: '#b8743a',
  },
  aurora: {
    leaf: '#5aa83a', leafDark: '#3c7a26', stem: '#4d7a2c',
    bud: '#c6b6dc', budDark: '#8f78b0', pistil: '#f2c14e',
    seed: '#9a8f86', seedDark: '#4c433d', seedHi: '#cbc3bb',
    dried: '#a39ab0', driedDark: '#6f6680', driedPistil: '#c79a3c',
  },
  eclipse: {
    leaf: '#3f7a4a', leafDark: '#2a5535', stem: '#35603a',
    bud: '#7d5ea3', budDark: '#4e3870', pistil: '#d9773a',
    seed: '#5b4a3c', seedDark: '#2a211a', seedHi: '#8e7a66',
    dried: '#6e5a73', driedDark: '#45374b', driedPistil: '#a65a2c',
  },
};

// Paletas-base das famílias do catálogo (campo "family" de manifest.json). Até o estágio de arte redesenhar tudo,
// as variedades do catálogo usam as mesmas grades das nativas com estas cores.
const FAMILY_PALETTES = {
  verde: {
    leaf: '#6fb83e', leafDark: '#457f27', stem: '#548a30',
    bud: '#b9dc78', budDark: '#84ad4a', pistil: '#e39a45',
    seed: '#b8a07a', seedDark: '#5e4a32', seedHi: '#e0cfa6',
    dried: '#a8b062', driedDark: '#707838', driedPistil: '#b06a34',
  },
  verde_escuro: {
    leaf: '#3e7a36', leafDark: '#26522a', stem: '#355e2e',
    bud: '#7fae5a', budDark: '#4f7a3a', pistil: '#d98a40',
    seed: '#6e5a44', seedDark: '#33281c', seedHi: '#9c8466',
    dried: '#6a7a44', driedDark: '#424e2a', driedPistil: '#9a5a2a',
  },
  limao: {
    leaf: '#8ccf3e', leafDark: '#5a9a26', stem: '#6a9a32',
    bud: '#e4ec7a', budDark: '#b8c444', pistil: '#f2b84a',
    seed: '#c8b07a', seedDark: '#6a5634', seedHi: '#ece0b0',
    dried: '#c4c068', driedDark: '#8a8638', driedPistil: '#c08a36',
  },
  dourado: {
    leaf: '#7cb040', leafDark: '#4f7d28', stem: '#6b8a34',
    bud: '#e8c85a', budDark: '#b8963a', pistil: '#f08a3a',
    seed: '#b89a62', seedDark: '#5c4626', seedHi: '#e6d09a',
    dried: '#c8a85a', driedDark: '#8a7034', driedPistil: '#b8662c',
  },
  roxo: {
    leaf: '#4f8a40', leafDark: '#33602e', stem: '#456e34',
    bud: '#9a6cc0', budDark: '#6a4890', pistil: '#e8904a',
    seed: '#6a5a52', seedDark: '#2e2420', seedHi: '#9a8a80',
    dried: '#7a6488', driedDark: '#4e3e5c', driedPistil: '#b06a34',
  },
  azul: {
    leaf: '#4a8a5a', leafDark: '#2e5e3e', stem: '#3f6a46',
    bud: '#7a9ad8', budDark: '#4e68a8', pistil: '#f0a050',
    seed: '#5e6670', seedDark: '#2a2e34', seedHi: '#8e98a4',
    dried: '#6e7e9e', driedDark: '#46526c', driedPistil: '#b07a3c',
  },
  rosa: {
    leaf: '#5ea544', leafDark: '#3c742c', stem: '#4f7f34',
    bud: '#e898bc', budDark: '#b8648e', pistil: '#f4c060',
    seed: '#a08478', seedDark: '#4e3a34', seedHi: '#d0b8ac',
    dried: '#b88aa0', driedDark: '#7e586c', driedPistil: '#c0843c',
  },
  gelo: {
    leaf: '#6aa86a', leafDark: '#44784a', stem: '#557f50',
    bud: '#e6eee6', budDark: '#b4c4b8', pistil: '#e8a868',
    seed: '#b0aca4', seedDark: '#56524c', seedHi: '#e4e0d8',
    dried: '#c8ccc0', driedDark: '#8e9488', driedPistil: '#b88a54',
  },
  creme: {
    leaf: '#78a848', leafDark: '#4e782e', stem: '#648a3a',
    bud: '#ecd8a8', budDark: '#c0a070', pistil: '#d8783a',
    seed: '#c0a07a', seedDark: '#624a30', seedHi: '#ead6b4',
    dried: '#c8b088', driedDark: '#8e7650', driedPistil: '#a8602c',
  },
};

// Variação de destaque determinística pela posição da variedade dentro da família (na ordem do manifesto): a 1ª usa a
// paleta-base; as seguintes giram o tom dos botões, mudam a claridade das folhas e sementes e trocam a cor dos
// pistilos, para duas variedades da mesma família nunca saírem iguais (AssetCompletenessTest confere).
const ACCENTS = [
  null,
  { hue: 14, light: 0.05, pistil: '#f2c14e' },
  { hue: -14, light: -0.05, pistil: '#d9653a' },
  { hue: 28, light: -0.02, pistil: '#ff9ec4' },
  { hue: -28, light: 0.02, pistil: '#f6e3b0' },
  { hue: 7, light: 0.08, pistil: '#e8503a' },
];

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

/** Gira o tom (graus) e soma à claridade (0–1) de uma cor '#rrggbb'. */
function shift(hex, dHue, dLight) {
  const [h, s, l] = rgbToHsl(parseColor(hex).slice(0, 3));
  const rgb = hslToRgb([h + dHue, s, Math.min(0.95, Math.max(0.05, l + dLight))]);
  return '#' + rgb.map((v) => v.toString(16).padStart(2, '0')).join('');
}

function withAccent(base, a) {
  if (!a) return base;
  return {
    leaf: shift(base.leaf, a.hue * 0.25, a.light),
    leafDark: shift(base.leafDark, a.hue * 0.25, a.light),
    stem: shift(base.stem, a.hue * 0.25, a.light),
    bud: shift(base.bud, a.hue, a.light * 0.5),
    budDark: shift(base.budDark, a.hue, a.light * 0.5),
    pistil: a.pistil,
    seed: shift(base.seed, a.hue * 0.5, a.light),
    seedDark: shift(base.seedDark, a.hue * 0.5, a.light),
    seedHi: shift(base.seedHi, a.hue * 0.5, a.light),
    dried: shift(base.dried, a.hue * 0.6, a.light * 0.5),
    driedDark: shift(base.driedDark, a.hue * 0.6, a.light * 0.5),
    driedPistil: shift(a.pistil, 0, -0.18),
  };
}

/** Paleta de cada variedade ({id, family} do manifesto), na ordem dada: id → cores. */
export function strainPalettes(strains) {
  const seen = {};
  const palettes = {};
  for (const strain of strains) {
    const index = seen[strain.family] ?? 0;
    seen[strain.family] = index + 1;
    if (NATIVE_PALETTES[strain.family]) {
      if (index > 0) throw new Error(`família nativa compartilhada: ${strain.family}`);
      palettes[strain.id] = NATIVE_PALETTES[strain.family];
      continue;
    }
    const base = FAMILY_PALETTES[strain.family];
    if (!base) throw new Error(`família de arte desconhecida: ${strain.family} (${strain.id})`);
    if (index >= ACCENTS.length) throw new Error(`família ${strain.family} passou de ${ACCENTS.length} variedades`);
    palettes[strain.id] = withAccent(base, ACCENTS[index]);
  }
  return palettes;
}

const CROP_STAGES = [
  [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....L.....L....',
    '.....LL...LL....',
    '......LLSLL.....',
    '........S.......',
    '........S.......',
  ],
  [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......L........',
    '......LLL.......',
    '...L...S...L....',
    '...LL..S..LL....',
    '....LL.S.LL.....',
    '.L...LLSLL...L..',
    '..LL..DSD..LL...',
    '...LLL.S.LLL....',
    '.......S........',
  ],
  [
    '................',
    '................',
    '.......L........',
    '......LLL.......',
    '...L..LSL..L....',
    '...LL..S..LL....',
    '....LLLSLLL.....',
    '.L.....S.....L..',
    '.LL..L.S.L..LL..',
    '..LLDL.S.LDLL...',
    '...LLLLSLLLL....',
    '.L.....S.....L..',
    '.LLL..DSD..LLL..',
    '..LLLLLSLLLLL...',
    '....LLLSLLL.....',
    '.......S........',
  ],
  [
    '.......B........',
    '......BPB.......',
    '......BbB.......',
    '...B..LSL..B....',
    '..BPB..S..BPB...',
    '..BbLLLSLLLbB...',
    '....L..S..L.....',
    '.B..L.BSB.L..B..',
    'BPB.LLBPB.L.BPB.',
    'BbBLDL.S.LDLBbB.',
    '..LLLLLSLLLL....',
    '.L....BSB....L..',
    '.LLL.BPSPB.LLL..',
    '..LLLLBSBLLLL...',
    '....LLLSLLL.....',
    '.......S........',
  ],
];

const SEEDS = [
  '................',
  '................',
  '................',
  '................',
  '................',
  '.....oo.........',
  '....oOOo........',
  '....oOho....oo..',
  '....oOOo...oOOo.',
  '.....oo....oOho.',
  '...........oOOo.',
  '......oo....oo..',
  '.....oOOo.......',
  '.....oOho.......',
  '.....oOOo.......',
  '......oo........',
];

const FLOWER = [
  '................',
  '.......PB.......',
  '......BBPB......',
  '.....BPBBbB.....',
  '.....BbBPBB.....',
  '....LBBPBbBL....',
  '...LLBbBBPBLL...',
  '....LBPBbBBL....',
  '.....BBBPBb.....',
  '....LLBbBBLL....',
  '...LL.BPBB.LL...',
  '..L....SS....L..',
  '.......S........',
  '......S.........',
  '................',
  '................',
];

// ---------------------------------------------------------------- itens genéricos
const ITEMS = {
  resin: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '.......aa.......',
      '.....aaAAa......',
      '....aAhhAAa.....',
      '...aAhAAAAAa....',
      '...aAAAAAAAAa...',
      '..aAAAAAAAdAa...',
      '..aAAAAAAddAAa..',
      '...aAAAAAAAAa...',
      '....aaAAAAaa....',
      '......aaaa......',
      '................',
      '................',
    ],
    palette: { a: '#7a4a12', A: '#d08a2a', h: '#f6d27a', d: '#a5661c' },
  },
  hash: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '...kkkkkkkkkk...',
      '..kHHHHHHHHHHk..',
      '..kHhhHHHHHHHk..',
      '..kHhHHssHHHHk..',
      '..kHHHsHHsHHHk..',
      '..kHHHsHHsHHHk..',
      '..kHHHHssHHHdk..',
      '..kHHHHHHHHddk..',
      '...kkkkkkkkkk...',
      '................',
      '................',
      '................',
    ],
    palette: { k: '#2e1d10', H: '#6b4423', h: '#8f6236', s: '#4a2e17', d: '#553519' },
  },
  joint: {
    grid: [
      '................',
      '................',
      '............wcc.',
      '...........wwcce',
      '..........wwwwc.',
      '.........wwwwg..',
      '........wwwwg...',
      '.......wwwwg....',
      '......wwwwg.....',
      '.....wwwwg......',
      '....wwwwg.......',
      '...wwwwg........',
      '..fwwwg.........',
      '.ffwgg..........',
      '.ff.............',
      '................',
    ],
    palette: { w: '#f1ece0', g: '#cfc6b2', c: '#7a9a46', e: '#e09a40', f: '#c8a26a' },
  },
  bong: {
    grid: [
      '.....gg.........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg........',
      '.....gGg..mm....',
      '.....gGg.mMg....',
      '.....gGg.gm.....',
      '....gGGGgg......',
      '...gGGGGGg......',
      '..gGwwwwwGg.....',
      '..gwWWWWWwg.....',
      '..gwWWWWWwg.....',
      '..gwwWWWwwg.....',
      '...ggggggg......',
      '................',
    ],
    palette: { g: '#4e8aa0', G: '#b9e2ee', w: '#5fa6c4', W: '#7fc4e0', m: '#5c5f66', M: '#9aa0a8' },
  },
  pipe: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '..bbbb..........',
      '.bBccBb.........',
      '.bBddBb.........',
      '.bBBBBb.........',
      '.bBBBBbbb.......',
      '..bBBBBBBbb.....',
      '...bbbbBBBBbb...',
      '.......bbBBBBbm.',
      '.........bbbbmm.',
      '................',
      '................',
      '................',
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
      '................',
    ],
    palette: { m: '#8a6a2a', M: '#d4b25a', g: '#3f6d52', G: '#9fd4b4', w: '#4e9a78', W: '#79c39f', h: '#2f2f38' },
  },
  chillum: {
    grid: [
      '................',
      '................',
      '...........cc...',
      '..........cCCc..',
      '.........cCddCc.',
      '........cCCddc..',
      '.......cCCCCc...',
      '......cCCCCc....',
      '.....cCCCCc.....',
      '....cCCCCc......',
      '...cCCCCc.......',
      '..cCCCCc........',
      '..cCCCc.........',
      '...ccc..........',
      '................',
      '................',
    ],
    palette: { c: '#7a3d22', C: '#c06a3e', d: '#3a1d10' },
  },
  rolling_paper: {
    grid: [
      '................',
      '................',
      '................',
      '..bbbbbbbbbbbb..',
      '..bRRRRRRRRRRb..',
      '..bRRyyyyyyRRb..',
      '..bRRyRRRRyRRb..',
      '..bRRyyyyyyRRb..',
      '..bRRRRRRRRRRb..',
      '..bwwwwwwwwwwb..',
      '..bWWWWWWWWWWb..',
      '..bwwwwwwwwwwb..',
      '..bbbbbbbbbbbb..',
      '................',
      '................',
      '................',
    ],
    palette: { b: '#5a2a1a', R: '#b8442e', y: '#f0c860', w: '#f4efe4', W: '#d8d0c0' },
  },
  filter_tip: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '...........ss...',
      '..........sTTs..',
      '.........sTTs...',
      '...ss...sTTs....',
      '..sTTs.sTTs.....',
      '.sTTs.sTTs......',
      'sTTs..sss.......',
      'sss.............',
      '................',
      '................',
      '................',
      '................',
    ],
    palette: { s: '#b39a6a', T: '#efe2c0' },
  },
  grinder: {
    grid: [
      '................',
      '................',
      '................',
      '.....kkkkkk.....',
      '...kkMMMMMMkk...',
      '..kMMhhMMMMMMk..',
      '..kMhMMMMMMMMk..',
      '..kMMMMMMMMMMk..',
      '..kkMMMMMMMMkk..',
      '..kGkkkkkkkkGk..',
      '..kGGGGGGGGGGk..',
      '..kGgGgGgGgGGk..',
      '..kkGGGGGGGGkk..',
      '....kkkkkkkk....',
      '................',
      '................',
    ],
    palette: { k: '#2e3338', M: '#8c96a0', h: '#d6dde3', G: '#5c6670', g: '#3d454c' },
  },
  lighter: {
    grid: [
      '................',
      '................',
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
      '................',
    ],
    palette: { k: '#2a2a30', M: '#a7afb7', h: '#e4e8ec', g: '#5d6670', R: '#2f7fc1', r: '#7fb8e6', d: '#20578a' },
  },
  rolling_tray: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '..kkkkkkkkkkkk..',
      '.kTTTTTTTTTTTTk.',
      '.kTppppppppppTk.',
      '.kTpPPPPPPPPpTk.',
      '.kTpPPPPPPPPpTk.',
      '.kTppppppppppTk.',
      '.kTTTTTTTTTTTTk.',
      '..kkkkkkkkkkkk..',
      '................',
      '................',
      '................',
    ],
    palette: { k: '#2c3a2a', T: '#4f8a4a', p: '#3c6e3a', P: '#6aa863' },
  },
  jar: {
    grid: [
      '................',
      '....wwwwwwww....',
      '...wWWWWWWWWw...',
      '...wwwwwwwwww...',
      '....gggggggg....',
      '...gGGGGGGGGg...',
      '...gGhGGGGGGg...',
      '...gGhGGGGGGg...',
      '...gGGGGGGGGg...',
      '...gGGGGGGGGg...',
      '...gGGGGGGGGg...',
      '...gGGGGGGGGg...',
      '...gGGGGGGGGg...',
      '....gggggggg....',
      '................',
      '................',
    ],
    palette: { w: '#6b4a2a', W: '#a27448', g: '#6f9aa6', G: '#cfe8ee', h: '#ffffff' },
  },
  ashtray: {
    grid: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '................',
      '....cccccccc....',
      '..ccCCCCCCCCcc..',
      '.cCCaaaaaaaaCCc.',
      '.cCaAAaAAAaAaCc.',
      '.cCCaaaaaaaaCCc.',
      '..cCCCCCCCCCCc..',
      '...cccccccccc...',
      '................',
      '................',
      '................',
    ],
    palette: { c: '#3b3f46', C: '#7e8792', a: '#4b4f55', A: '#9a9a96' },
  },
};

// ---------------------------------------------------------------- blocos de estação
const WOOD = { k: '#3b2614', W: '#8a5c33', w: '#6e4727', l: '#a87344' };

const BLOCKS = {
  drying_rack_top: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kWkkkkkkkkkkkkWk',
      'kWk..s..s..s.kWk',
      'kWWWWWWWWWWWWWWk',
      'kWk.s..s..s..kWk',
      'kWk..s..s..s.kWk',
      'kWWWWWWWWWWWWWWk',
      'kWk.s..s..s..kWk',
      'kWk..s..s..s.kWk',
      'kWWWWWWWWWWWWWWk',
      'kWk.s..s..s..kWk',
      'kWk..s..s..s.kWk',
      'kWkkkkkkkkkkkkWk',
      'kWWWWWWWWWWWWWWk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: { ...WOOD, s: '#d9cfae' },
  },
  drying_rack_side: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kkkkkkkkkkkkkkkk',
      'kWk.s..s..s..kWk',
      'kWk.L..L..L..kWk',
      'kWkLLLLLLLLLLkWk',
      'kWk.L..L..L..kWk',
      'kWk.d..d..d..kWk',
      'kWk.s..s..s..kWk',
      'kWk.L..L..L..kWk',
      'kWkLLLLLLLLLLkWk',
      'kWk.L..L..L..kWk',
      'kWk.d..d..d..kWk',
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: { ...WOOD, s: '#d9cfae', L: '#8a9a4a', d: '#5f6b30' },
  },
  sieve_top: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kWlWWWWWWWWWWWWk',
      'kWWmMmMmMmMmMWWk',
      'kWWMmMmMmMmMmWWk',
      'kWWmMmMmMmMmMWWk',
      'kWWMmMmMmMmMmWWk',
      'kWWmMmMmMmMmMWWk',
      'kWWMmMmMmMmMmWWk',
      'kWWmMmMmMmMmMWWk',
      'kWWMmMmMmMmMmWWk',
      'kWWmMmMmMmMmMWWk',
      'kWWMmMmMmMmMmWWk',
      'kWWWWWWWWWWWWWWk',
      'kWWWWWWWWWWWWWWk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: { ...WOOD, m: '#b8b0a0', M: '#5a5248' },
  },
  sieve_side: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kWlWWWWWWWWWWlWk',
      'kwwwwwwwwwwwwwwk',
      'kkkkkkkkkkkkkkkk',
      'kWk..........kWk',
      'kWk..a....a..kWk',
      'kWk....a.....kWk',
      'kWk.a.....a..kWk',
      'kWk.AAAAAAAA.kWk',
      'kWkAAAAAAAAAAkWk',
      'kWk..........kWk',
      'kWk..........kWk',
      'kWk..........kWk',
      'kWk..........kWk',
      'kkk..........kkk',
    ],
    palette: { ...WOOD, a: '#d08a2a', A: '#a5661c' },
  },
  press_top: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kWwwwwwwwwwwwwWk',
      'kWwWWWWWWWWWWwWk',
      'kWwWWiiiiiiWWwWk',
      'kWwWiIIIIIIiWwWk',
      'kWwWiIhIIIIiWwWk',
      'kWwWiIIggIIiWwWk',
      'kWwWiIIggIIiWwWk',
      'kWwWiIIIIIIiWwWk',
      'kWwWiIIIIIIiWwWk',
      'kWwWWiiiiiiWWwWk',
      'kWwWWWWWWWWWWwWk',
      'kWwwwwwwwwwwwwWk',
      'kWWWWWWWWWWWWWWk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: { ...WOOD, i: '#3a3f45', I: '#8c96a0', h: '#d6dde3', g: '#4d555c' },
  },
  press_side: {
    grid: [
      'kkkkkkkkkkkkkkkk',
      'kIIIIIIIIIIIIIIk',
      'kkkkkkkiikkkkkkk',
      'kWk....iI....kWk',
      'kWk....iI....kWk',
      'kWk..iiiiii..kWk',
      'kWk..iIIIIi..kWk',
      'kWk..iiiiii..kWk',
      'kWk..wwwwww..kWk',
      'kWk..wWWWWw..kWk',
      'kWk..wwwwww..kWk',
      'kWk..........kWk',
      'kkkkkkkkkkkkkkkk',
      'kWWWWWWWWWWWWWWk',
      'kWlWWWWWWWWWWlWk',
      'kkkkkkkkkkkkkkkk',
    ],
    palette: { ...WOOD, i: '#3a3f45', I: '#8c96a0' },
  },
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

const WILD = {
  L: '#6a9a3c', D: '#46702a', S: '#557a2e', B: '#a9c46a', b: '#7f9a46', P: '#d99b4a',
};

/** @param strains seção "strains" do manifesto (id e family de cada variedade) */
export function generateAll(strains) {
  const written = [];
  for (const [id, c] of Object.entries(strainPalettes(strains))) {
    const plant = { L: c.leaf, D: c.leafDark, S: c.stem, B: c.bud, b: c.budDark, P: c.pistil };
    CROP_STAGES.forEach((grid, stage) => written.push(render(`block/${id}_crop_stage${stage}`, grid, plant)));
    written.push(render(`item/${id}_seeds`, SEEDS, { o: c.seedDark, O: c.seed, h: c.seedHi }));
    written.push(render(`item/${id}_fresh_flower`, FLOWER, plant));
    written.push(render(`item/${id}_dried_flower`, FLOWER, {
      L: c.driedDark, D: c.driedDark, S: '#6b5a3a', B: c.dried, b: c.driedDark, P: c.driedPistil,
    }));
  }
  written.push(render('block/wild_plant', CROP_STAGES[3], WILD));
  for (const [id, t] of Object.entries(ITEMS)) written.push(render(`item/${id}`, t.grid, t.palette));
  for (const [id, t] of Object.entries(BLOCKS)) written.push(render(`block/${id}`, t.grid, t.palette));
  for (const [id, t] of Object.entries(EFFECTS)) written.push(render(`mob_effect/${id}`, t.grid, t.palette));
  return written;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const manifest = JSON.parse(readFileSync(join(ROOT, 'tools', 'gen', 'manifest.json'), 'utf8'));
  const files = generateAll(manifest.strains);
  console.log(`${files.length} texturas geradas em ${OUT}`);
}
