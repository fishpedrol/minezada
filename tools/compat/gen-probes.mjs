// Gera as sondas de console de tools/compat/probes/*.txt usadas por forge-server-compat.sh.
// Uso: node tools/compat/gen-probes.mjs
// Cada condição tem REPS plantas numa fileira (sem vizinhas na diagonal, então sem a penalidade de fileira do
// trigo), e a sonda relata a idade exata (0 a 7) de cada uma com "say SONDA <condição> <planta> idade=N".
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = join(dirname(fileURLToPath(import.meta.url)), 'probes');
const REPS = 4;
const Y = 120; // farmland; as plantas ficam em Y+1
const CROP = 'cultivo:brisa_crop';
const WHEAT = 'minecraft:wheat';

const setup = [
  'forceload add -16 -16 15 15',
  'gamerule random_tick_speed 300',
  `fill -10 ${Y - 8} -10 10 ${Y + 10} 10 minecraft:air`,
  // fillbiome aceita no máximo 32768 blocos
  `fillbiome -16 ${Y - 8} -16 15 ${Y + 15} 15 minecraft:plains`,
  `fill -8 ${Y} -8 8 ${Y} 8 minecraft:farmland[moisture=7]`,
  `execute if biome 0 ${Y + 1} 0 minecraft:plains run say SONDA diagnostico bioma=plains`,
  `execute unless biome 0 ${Y + 1} 0 minecraft:plains run say SONDA diagnostico bioma=OUTRO`,
];

/** Uma fileira de REPS plantas em z, começando em x0, com água logo atrás (z+1) para manter a farmland molhada. */
function row(block, x0, z) {
  const lines = [`fill ${x0} ${Y} ${z + 1} ${x0 + REPS - 1} ${Y} ${z + 1} minecraft:water`];
  for (let i = 0; i < REPS; i++) lines.push(`setblock ${x0 + i} ${Y + 1} ${z} ${block}`);
  return lines;
}

function glassOver(x0, z) {
  return [`fill ${x0} ${Y + 4} ${z} ${x0 + REPS - 1} ${Y + 4} ${z} minecraft:glass`];
}

function report(label, block, x0, z) {
  const lines = [];
  for (let i = 0; i < REPS; i++) {
    for (let age = 0; age <= 7; age++) {
      lines.push(`execute if block ${x0 + i} ${Y + 1} ${z} ${block}[age=${age}] run say SONDA ${label} ${i + 1} idade=${age}`);
    }
  }
  return lines;
}

function write(name, header, body) {
  const text = [...header.map((h) => `# ${h}`), ...setup, ...body].join('\n') + '\n';
  writeFileSync(join(OUT, name), text);
}

const C = { x: -6, z: -4 }; // Cultivo ao ar livre
const W = { x: 2, z: -4 }; // trigo ao ar livre
const CG = { x: -6, z: 3 }; // Cultivo sob vidro
const WG = { x: 2, z: 3 }; // trigo sob vidro

write('growth.txt', [
  'Sonda de crescimento em produção (gerada por tools/compat/gen-probes.mjs; não edite à mão).',
  `${REPS} lavouras do Cultivo e ${REPS} trigos de controle em farmland molhada, numa plataforma a céu aberto em`,
  `y=${Y} com bioma plains forçado. Com random_tick_speed 300 o trigo leva cerca de 15 s; esperamos 30 s.`,
], [
  ...row(CROP, C.x, C.z), ...row(WHEAT, W.x, W.z),
  '@wait 30',
  ...report('cultivo', CROP, C.x, C.z), ...report('trigo', WHEAT, W.x, W.z),
]);

write('seasons.txt', [
  'Sonda de estações em produção (gerada por tools/compat/gen-probes.mjs; não edite à mão).',
  'Precisa do Serene Seasons + GlitchCore na pasta de mods extras. Plataforma a céu aberto em y=120 (acima do',
  'limite de 48 em que o Serene Seasons considera tudo fértil no subsolo), bioma plains forçado (temperado).',
  'Fase 1, início da primavera: o trigo está fora de estação. Fileiras ao ar livre e sob vidro (3 blocos acima).',
  'Fase 2, meio do verão: o trigo está na estação; replanta as fileiras ao ar livre.',
  'Com out_of_season_crop_behavior = 1 (CULTIVO_COMPAT_CONFIG=tools/compat/config-cant-grow), uma lavoura',
  'listada e fora de estação NÃO cresce: se o Serene Seasons estiver ativo, o trigo ao ar livre fica em idade 0.',
], [
  'season set early_spring',
  ...row(CROP, C.x, C.z), ...row(WHEAT, W.x, W.z),
  ...row(CROP, CG.x, CG.z), ...row(WHEAT, WG.x, WG.z),
  ...glassOver(CG.x, CG.z), ...glassOver(WG.x, WG.z),
  '@wait 30',
  ...report('primavera cultivo_ar_livre', CROP, C.x, C.z), ...report('primavera trigo_ar_livre', WHEAT, W.x, W.z),
  ...report('primavera cultivo_sob_vidro', CROP, CG.x, CG.z), ...report('primavera trigo_sob_vidro', WHEAT, WG.x, WG.z),
  'season set mid_summer',
  ...row(CROP, C.x, C.z), ...row(WHEAT, W.x, W.z),
  '@wait 30',
  ...report('verao cultivo_ar_livre', CROP, C.x, C.z), ...report('verao trigo_ar_livre', WHEAT, W.x, W.z),
]);

console.log(`sondas geradas em ${OUT}`);
