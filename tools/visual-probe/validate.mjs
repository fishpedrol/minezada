// Confere o datapack de cena sem abrir o jogo: comandos conhecidos, IDs cultivo:* existentes, propriedades de
// blockstate validas, componentes validos, chaves/aspas balanceadas, marcadores say x linha do tempo x run-probe.ps1.
// Uso: node validate.mjs <raizDoRepo> <pastaDoDatapack> <run-probe.ps1>
import fs from 'node:fs';
import path from 'node:path';

const [repo, pack, ps1] = process.argv.slice(2).map(p => path.resolve(p));
const assets = path.join(repo, 'src', 'generated', 'resources', 'assets', 'cultivo');
const manifest = JSON.parse(fs.readFileSync(path.join(repo, 'tools', 'gen', 'manifest.json'), 'utf8'));
const strainIds = new Set(manifest.strains.map(s => s.id));
const items = new Set(fs.readdirSync(path.join(assets, 'items')).map(f => f.replace(/\.json$/, '')));
// press_platen e um estado sintetico do cliente (sem bloco registrado)
const blocks = new Set(fs.readdirSync(path.join(assets, 'blockstates')).map(f => f.replace(/\.json$/, ''))
  .filter(b => b !== 'press_platen'));

// Propriedades por bloco, lidas do proprio blockstate (variants "k=v,..." e multipart "when")
const props = {};
for (const b of blocks) {
  const json = JSON.parse(fs.readFileSync(path.join(assets, 'blockstates', `${b}.json`), 'utf8'));
  const p = {};
  const add = (k, v) => { (p[k] ??= new Set()); String(v).split('|').forEach(x => p[k].add(x)); };
  if (json.variants) for (const key of Object.keys(json.variants)) if (key) for (const kv of key.split(',')) { const [k, v] = kv.split('='); add(k, v); }
  if (json.multipart) for (const part of json.multipart) if (part.when) for (const [k, v] of Object.entries(part.when)) add(k, v);
  props[b] = p;
}
// Valores que o blockstate JSON nao lista mas o bloco aceita (ContentIds.stateProperties / CropBlock.AGE)
for (const s of ['drying_rack', 'sieve', 'press']) props[s].contents.add('empty');
props.ashtray.ash.add('0');
props.jar.filled.add('false');
if (props.sieve.grinder) props.sieve.grinder.add('false');

const COMMANDS = new Set(['fill', 'setblock', 'summon', 'tp', 'item', 'clear', 'effect', 'gamemode', 'gamerule', 'time',
  'weather', 'scoreboard', 'execute', 'forceload', 'recipe', 'say']);
const SLOTS = /^(hotbar\.[0-8]|inventory\.([0-9]|1[0-9]|2[0-6])|weapon\.mainhand|weapon\.offhand)$/;
const errors = [];
const err = (file, n, msg) => errors.push(`${file}:${n}: ${msg}`);

const fnDir = path.join(pack, 'data', 'probe', 'function');
const fnFiles = fs.readdirSync(fnDir).filter(f => f.endsWith('.mcfunction'));
const fnNames = new Set(fnFiles.map(f => f.replace(/\.mcfunction$/, '')));
const markers = [];
let commands = 0;
for (const f of fnFiles) {
  const buf = fs.readFileSync(path.join(fnDir, f));
  if (buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) err(f, 1, 'BOM UTF-8 (o Minecraft le como parte do comando)');
  if (buf.includes(0x0d)) err(f, 1, 'CR encontrado (use so \\n)');
  const lines = buf.toString('utf8').split('\n');
  lines.forEach((line, i) => {
    const n = i + 1;
    if (!line.trim() || line.startsWith('#')) return;
    if (/[^\x20-\x7e]/.test(line)) err(f, n, 'caractere nao ASCII');
    if (line !== line.trim()) err(f, n, 'espaco nas pontas');
    commands++;
    const cmd = line.split(' ')[0];
    if (!COMMANDS.has(cmd)) err(f, n, `comando inesperado: ${cmd}`);
    // Balanceamento de {} [] e aspas
    let depth = 0, sq = 0, inStr = false;
    for (const ch of line) {
      if (ch === '"') inStr = !inStr;
      if (inStr) continue;
      if (ch === '{') depth++; if (ch === '}') depth--;
      if (ch === '[') sq++; if (ch === ']') sq--;
      if (depth < 0 || sq < 0) break;
    }
    if (depth || sq || inStr) err(f, n, 'chaves/colchetes/aspas desbalanceados');
    const say = line.match(/^say probe:([a-z_]+)$/);
    if (say) markers.push([f.replace(/\.mcfunction$/, ''), say[1]]);
    const fnRef = line.match(/run function probe:([a-z_]+)/);
    if (fnRef && !fnNames.has(fnRef[1])) err(f, n, `funcao inexistente: ${fnRef[1]}`);
    if (cmd === 'item') {
      const m = line.match(/^item replace entity @a (\S+) with /);
      if (!m) err(f, n, 'item replace com forma inesperada');
      else if (!SLOTS.test(m[1])) err(f, n, `slot invalido: ${m[1]}`);
    }
    // Blocos com estado: setblock/fill <x y z> [x y z] cultivo:id[props]{nbt}
    for (const m of line.matchAll(/(?:setblock(?: \S+){3}|fill(?: \S+){6}) (cultivo:[a-z0-9_]+)(\[[^\]]*\])?/g)) {
      const id = m[1].slice(8);
      if (!blocks.has(id)) { err(f, n, `bloco inexistente: ${id}`); continue; }
      if (m[2]) for (const kv of m[2].slice(1, -1).split(',')) {
        const [k, v] = kv.split('=');
        if (!props[id][k]) err(f, n, `${id}: propriedade inexistente ${k}`);
        else if (!props[id][k].has(v)) err(f, n, `${id}: ${k}=${v} fora de ${[...props[id][k]].join('/')}`);
      }
    }
    // Itens: id:"cultivo:x" em NBT e cultivo:x[...] em give/item replace
    for (const m of line.matchAll(/id:"cultivo:([a-z0-9_]+)"/g)) if (!items.has(m[1])) err(f, n, `item inexistente: ${m[1]}`);
    const withItem = line.match(/ with (cultivo:[a-z0-9_]+)(\[[^\]]*\])?/);
    if (withItem && !items.has(withItem[1].slice(8))) err(f, n, `item inexistente: ${withItem[1]}`);
    // Componentes: quality 1..5, strain conhecida, filtered/cured = {}
    for (const m of line.matchAll(/"?cultivo:(quality|strain|filtered|cured)"?\s*[:=]\s*("[^"]*"|\{\}|-?\d+)/g)) {
      const [, k, v] = m;
      if (k === 'quality' && !(/^\d+$/.test(v) && +v >= 1 && +v <= 5)) err(f, n, `quality invalida: ${v}`);
      if (k === 'strain' && !strainIds.has(v.replace(/"/g, ''))) err(f, n, `variedade desconhecida: ${v}`);
      if ((k === 'filtered' || k === 'cured') && v !== '{}') err(f, n, `${k} deve ser {}`);
    }
    for (const m of line.matchAll(/"?cultivo:([a-z_]+)"?\s*[:=]/g)) {
      if (!['quality', 'strain', 'filtered', 'cured'].includes(m[1])) err(f, n, `componente desconhecido: cultivo:${m[1]}`);
    }
    if (/item_display:"(?!none"|thirdperson_lefthand"|thirdperson_righthand"|firstperson_lefthand"|firstperson_righthand"|head"|gui"|ground"|fixed"|on_shelf")/.test(line)) err(f, n, 'item_display com contexto invalido');
    if (/\d,\d/.test(line.replace(/"[^"]*"/g, '')) && !line.includes('[')) err(f, n, 'virgula decimal?');
  });
}

// Linha do tempo: cada passo tem funcao e marcador com o mesmo nome (exceto prepare/build), em ordem
const tick = fs.readFileSync(path.join(fnDir, 'tick.mcfunction'), 'utf8');
const timeline = [...tick.matchAll(/matches (\d+) run function probe:([a-z_]+)/g)].map(m => [+m[1], m[2]]);
for (let i = 1; i < timeline.length; i++) if (timeline[i][0] <= timeline[i - 1][0]) err('tick.mcfunction', 0, 'tempos fora de ordem');
const markerOf = Object.fromEntries(markers);
for (const [, name] of timeline) {
  if (name === 'prepare' || name === 'build') continue;
  if (markerOf[name] !== name) err(`${name}.mcfunction`, 0, `sem "say probe:${name}"`);
}
// run-probe.ps1 espera os mesmos marcadores, na mesma ordem
if (ps1 && fs.existsSync(ps1)) {
  const waits = [...fs.readFileSync(ps1, 'utf8').matchAll(/Wait-Step '([a-z_]+)'/g)].map(m => m[1]);
  const expected = timeline.map(([, n]) => n).filter(n => n !== 'prepare' && n !== 'build');
  if (waits.join(',') !== expected.join(',')) err('run-probe.ps1', 0, `esperas ${waits.join(',')} != linha do tempo ${expected.join(',')}`);
  // Nenhum marcador e prefixo de outro (a espera usa \b, mas melhor nem arriscar)
  for (const a of expected) for (const b2 of expected) if (a !== b2 && b2.startsWith(a)) err('marcadores', 0, `${a} e prefixo de ${b2}`);
}

console.log(`${fnFiles.length} funcoes, ${commands} comandos, ${timeline.length} passos, ${markers.length} marcadores`);
console.log(`blocos conhecidos ${blocks.size}, itens conhecidos ${items.size}, variedades ${strainIds.size}`);
if (errors.length) { console.log(`ERROS (${errors.length}):\n` + errors.join('\n')); process.exit(1); }
console.log('OK: nenhum erro');
