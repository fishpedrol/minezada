// Gera todos os recursos JSON do Cultivo a partir de tools/gen/manifest.json e as texturas de tools/art.
// Uso: node tools/gen/generate.mjs
// Saída: src/generated/resources (apagada e recriada a cada execução; não edite à mão).
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { artFamilies, CROP_STAGE_COUNT, generateAll as generateTextures } from '../art/textures.mjs';
import { handModels, LIT_DEVICES, LIT_DISPLAY, LIT_TEXTURE_VAR, stationBlockstate, stationModels } from './models.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'src', 'generated', 'resources');
const M = JSON.parse(readFileSync(join(ROOT, 'tools', 'gen', 'manifest.json'), 'utf8'));
const NS = 'cultivo';

let count = 0;
function write(path, data) {
  const file = join(OUT, path);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, JSON.stringify(data, null, 2) + '\n');
  count++;
}

const id = (path) => `${NS}:${path}`;
const ingredient = (s) => s; // 26.3: ingrediente é string "ns:item" ou "#ns:tag"

// ------------------------------------------------------------------ ids
// Variedades (nativas + catálogo), na ordem do enum core/Variety.java
const strains = M.strains;
const stations = M.stations;
const wild = M.wild_plant;
const qualityItems = [
  ...strains.flatMap((v) => [`${v.id}_fresh_flower`, `${v.id}_dried_flower`]),
  'resin', 'hash',
];
const flatItems = [
  ...strains.map((v) => `${v.id}_seeds`),
  ...qualityItems,
  'joint',
  ...M.articles.map((a) => a.id),
];
const blockItems = [...stations.map((s) => s.id), wild.id];

rmSync(OUT, { recursive: true, force: true });
const textures = generateTextures(strains);

// ------------------------------------------------------------------ assets: blockstates e modelos
// Lavoura: 8 estágios visuais (um por idade, como o trigo). Os modelos e texturas são por família de arte: as
// variedades da mesma família compartilham a planta no campo; sementes e flores são por variedade.
const families = artFamilies(strains);
for (const family of families) {
  for (let stage = 0; stage < CROP_STAGE_COUNT; stage++) {
    write(`assets/${NS}/models/block/${family}_crop_stage${stage}.json`, {
      parent: 'minecraft:block/crop',
      textures: { crop: id(`block/${family}_crop_stage${stage}`) },
    });
  }
}
for (const v of strains) {
  const variants = {};
  for (let age = 0; age <= 7; age++) variants[`age=${age}`] = { model: id(`block/${v.family}_crop_stage${age}`) };
  write(`assets/${NS}/blockstates/${v.id}_crop.json`, { variants });
}
write(`assets/${NS}/blockstates/${wild.id}.json`, { variants: { '': { model: id(`block/${wild.id}`) } } });
write(`assets/${NS}/models/block/${wild.id}.json`, { parent: 'minecraft:block/cross', textures: { cross: id(`block/${wild.id}`) } });
write(`assets/${NS}/models/item/${wild.id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: id(`block/${wild.id}`) } });

// Estações: modelos 3D com elementos e blockstate multipart (facing gira a base; contents liga as partes)
const stationDefs = stationModels();
for (const s of stations) {
  const def = stationDefs[s.id];
  if (!def) throw new Error(`estação sem modelo: ${s.id}`);
  write(`assets/${NS}/models/block/${s.id}.json`, def.base);
  for (const [name, model] of Object.entries(def.templates)) write(`assets/${NS}/models/block/${name}.json`, model);
  for (const [part, p] of Object.entries(def.parts)) write(`assets/${NS}/models/block/${s.id}_${part}.json`, p.model);
  write(`assets/${NS}/blockstates/${s.id}.json`, stationBlockstate(s.id, def.parts));
  // O item mostra a estação vazia (a base), com as transformações de bloco de minecraft:block/block
  write(`assets/${NS}/models/item/${s.id}.json`, { parent: id(`block/${s.id}`) });
}

// Itens planos (ícone gerado da textura)
const flatModel = (item, texture = item) => write(`assets/${NS}/models/item/${item}.json`, {
  parent: 'minecraft:item/generated', textures: { layer0: id(`item/${texture}`) },
});
for (const item of flatItems) flatModel(item);
flatModel('joint_lit');
for (const family of families) flatModel(`hash_${family}`);

// Modelos 3D na mão dos aparelhos de fumo e do isqueiro (+ variante acesa com brasa)
const hand = handModels();
for (const [device, model] of Object.entries(hand)) {
  write(`assets/${NS}/models/item/${device}_in_hand.json`, model);
  if (LIT_DEVICES.includes(device)) {
    write(`assets/${NS}/models/item/${device}_in_hand_lit.json`, {
      parent: id(`item/${device}_in_hand`),
      textures: { [LIT_TEXTURE_VAR[device]]: id('item/smoke_ember') },
      display: LIT_DISPLAY[device],
    });
  }
}

// Definições de item (assets/cultivo/items): modelo simples, ou árvore de seleção do 26.3
const model = (path) => ({ type: 'minecraft:model', model: id(path) });
const FLAT_CONTEXTS = ['gui', 'ground', 'fixed', 'on_shelf'];
// Como a luneta vanilla: ícone plano no inventário, no chão e em molduras; modelo 3D na mão
const flatOrHand = (flat, inHand) => ({
  type: 'minecraft:select', property: 'minecraft:display_context',
  cases: [{ when: FLAT_CONTEXTS, model: flat }], fallback: inHand,
});
const usingItem = (onTrue, onFalse) => ({ type: 'minecraft:condition', property: 'minecraft:using_item', on_true: onTrue, on_false: onFalse });
const itemDefinitions = {};
for (const item of [...flatItems, ...blockItems]) itemDefinitions[item] = model(`item/${item}`);
// Haxixe: carimbo com a cor da família da variedade gravada em cultivo:strain (sem variedade, carimbo neutro)
itemDefinitions.hash = {
  type: 'minecraft:select', property: 'minecraft:component', component: id('strain'),
  cases: families.map((family) => ({
    when: strains.filter((v) => v.family === family).map((v) => v.id),
    model: model(`item/hash_${family}`),
  })),
  fallback: model('item/hash'),
};
// Baseado: aceso (brasa e fumaça) enquanto o jogador fuma, também no ícone da hotbar
itemDefinitions.joint = usingItem(
  flatOrHand(model('item/joint_lit'), model('item/joint_in_hand_lit')),
  flatOrHand(model('item/joint'), model('item/joint_in_hand')),
);
for (const device of Object.keys(hand)) {
  if (device === 'joint') continue;
  const inHand = LIT_DEVICES.includes(device)
    ? usingItem(model(`item/${device}_in_hand_lit`), model(`item/${device}_in_hand`))
    : model(`item/${device}_in_hand`);
  itemDefinitions[device] = flatOrHand(model(`item/${device}`), inHand);
}
for (const [item, def] of Object.entries(itemDefinitions)) write(`assets/${NS}/items/${item}.json`, { model: def });

// ------------------------------------------------------------------ assets: lang
for (const lang of ['en_us', 'pt_br']) {
  const en = lang === 'en_us';
  const t = { ...M.lang[lang] };
  for (const v of strains) {
    const name = en ? v.en : v.pt;
    t[`item.${NS}.${v.id}_seeds`] = en ? `${name} Seeds` : `Sementes de ${name}`;
    t[`block.${NS}.${v.id}_crop`] = en ? `${name} Crop` : `Plantação de ${name}`;
    t[`item.${NS}.${v.id}_fresh_flower`] = en ? `Fresh ${name} Flower` : `Flor Fresca de ${name}`;
    t[`item.${NS}.${v.id}_dried_flower`] = en ? `Dried ${name} Flower` : `Flor Seca de ${name}`;
    // Nome da variedade gravada em cultivo:strain (baseado, resina, haxixe)
    t[`tooltip.${NS}.variety.${v.id}`] = name;
  }
  for (const g of [...M.generic_items, ...M.articles]) t[`item.${NS}.${g.id}`] = en ? g.en : g.pt;
  for (const b of [...stations, wild]) t[`block.${NS}.${b.id}`] = en ? b.en : b.pt;
  for (const e of M.effects) t[`effect.${NS}.${e.id}`] = en ? e.en : e.pt;
  write(`assets/${NS}/lang/${lang}.json`, Object.fromEntries(Object.entries(t).sort(([a], [b]) => a.localeCompare(b))));
}

// ------------------------------------------------------------------ data: loot tables (formato 26.3: type/condition/modifier)
const blockState = (block, age) => ({ type: 'minecraft:match_block', blocks: id(block), state: { age: String(age) } });
for (const v of strains) {
  const crop = `${v.id}_crop`;
  // Mesmo formato de loot_table/blocks/wheat.json do 26.3
  write(`data/${NS}/loot_table/blocks/${crop}.json`, {
    type: 'minecraft:block',
    modifier: { type: 'minecraft:explosion_decay' },
    pools: [
      {
        rolls: 1,
        entries: [{
          type: 'minecraft:alternatives',
          children: [
            {
              type: 'minecraft:item',
              name: id(`${v.id}_fresh_flower`),
              condition: blockState(crop, 7),
              modifier: { type: id('set_harvest_quality') },
            },
            { type: 'minecraft:item', name: id(`${v.id}_seeds`) },
          ],
        }],
      },
      {
        rolls: 1,
        condition: blockState(crop, 7),
        entries: [{
          type: 'minecraft:item',
          name: id(`${v.id}_seeds`),
          modifier: {
            type: 'minecraft:apply_bonus',
            enchantment: 'minecraft:fortune',
            formula: 'minecraft:binomial_with_bonus_count',
            parameters: { extra: 3, probability: 0.5714286 },
          },
        }],
      },
    ],
    random_sequence: id(`blocks/${crop}`),
  });
}
write(`data/${NS}/loot_table/blocks/${wild.id}.json`, {
  type: 'minecraft:block',
  pools: [{
    rolls: 1,
    entries: M.seed_sources.wild_drops.map((d) => ({
      type: 'minecraft:item',
      name: id(`${d.variety}_seeds`),
      weight: d.weight,
      modifier: { type: 'minecraft:explosion_decay' },
    })),
  }],
  random_sequence: id(`blocks/${wild.id}`),
});
for (const s of stations) {
  write(`data/${NS}/loot_table/blocks/${s.id}.json`, {
    type: 'minecraft:block',
    pools: [{
      rolls: 1,
      condition: { type: 'minecraft:survives_explosion' },
      entries: [{ type: 'minecraft:item', name: id(s.id) }],
    }],
    random_sequence: id(`blocks/${s.id}`),
  });
}
const ss = M.seed_sources;
// Baús de aldeia: uma entrada por variedade com peso e quantidades da raridade; vazio = soma dos pesos (50% rola semente)
const chestEntries = strains.map((v) => {
  const loot = ss.chest_by_rarity[v.rarity];
  if (!loot) throw new Error(`raridade sem peso de baú: ${v.id} (${v.rarity})`);
  return { variety: v.id, weight: loot.weight, min: loot.min, max: loot.max };
});
const chestEmptyWeight = chestEntries.reduce((sum, e) => sum + e.weight, 0);
write(`data/${NS}/loot_table/inject/village_seeds.json`, {
  type: 'minecraft:chest',
  pools: [{
    rolls: 1,
    entries: [
      { type: 'minecraft:empty', weight: chestEmptyWeight },
      ...chestEntries.map((e) => ({
        type: 'minecraft:item',
        name: id(`${e.variety}_seeds`),
        weight: e.weight,
        modifier: { type: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: e.min, max: e.max } },
      })),
    ],
  }],
});

// ------------------------------------------------------------------ data: receitas
// Baseado: um crafting_transmute por variedade (o transmute só copia o patch de componentes da flor, então a
// variedade vai fixa no resultado de cada receita)
const crafting = [
  ...M.crafting,
  ...strains.map((v) => ({
    id: `joint_${v.id}`,
    result: 'joint',
    count: 1,
    transmute: { input: id(`${v.id}_dried_flower`), material: M.joint_recipe.material, strain: v.id },
  })),
];
for (const r of crafting) {
  const result = { id: id(r.result), count: r.count };
  const file = `data/${NS}/recipe/${r.id ?? r.result}.json`;
  if (r.pattern) {
    write(file, {
      type: 'minecraft:crafting_shaped',
      category: 'misc',
      pattern: r.pattern,
      key: Object.fromEntries(Object.entries(r.key).map(([k, v]) => [k, ingredient(v)])),
      result,
    });
  } else if (r.transmute) {
    // Baseado: transmute copia os componentes da flor seca (qualidade) e grava a variedade (cultivo:strain)
    write(file, {
      type: 'minecraft:crafting_transmute',
      category: 'misc',
      group: M.joint_recipe.group,
      input: ingredient(r.transmute.input),
      material: ingredient(r.transmute.material),
      result: { ...result, components: { [id('strain')]: r.transmute.strain } },
    });
  } else {
    write(file, {
      type: 'minecraft:crafting_shapeless',
      category: 'misc',
      ingredients: r.ingredients.map(ingredient),
      result,
    });
  }
}
// Desbloqueio no livro de receitas (formato de data/minecraft/advancement/recipes/... do 26.3): a receita aparece
// quando o jogador pega qualquer ingrediente dela. Um critério has_<ingrediente> por ingrediente distinto.
const recipeIngredients = (r) => {
  if (r.pattern) return Object.values(r.key);
  if (r.transmute) return [r.transmute.input, r.transmute.material];
  return r.ingredients;
};
const criterionName = (ingredientId) => `has_${ingredientId.replace(/^#/, '').split(':').pop().replace(/\//g, '_')}`;
for (const r of crafting) {
  const recipeId = id(r.id ?? r.result);
  const criteria = {};
  for (const ing of [...new Set(recipeIngredients(r))]) {
    criteria[criterionName(ing)] = {
      conditions: { items: [{ items: ing }] },
      trigger: 'minecraft:inventory_changed',
    };
  }
  criteria.has_the_recipe = { conditions: { recipes: recipeId }, trigger: 'minecraft:recipe_unlocked' };
  write(`data/${NS}/advancement/recipes/misc/${r.id ?? r.result}.json`, {
    parent: 'minecraft:recipes/root',
    criteria,
    requirements: [['has_the_recipe', ...Object.keys(criteria).filter((k) => k !== 'has_the_recipe')]],
    rewards: { recipes: [recipeId] },
  });
}
const proc = M.processing;
for (const v of strains) {
  write(`data/${NS}/recipe/drying/${v.id}_dried_flower.json`, {
    type: id('drying'),
    ingredient: id(`${v.id}_fresh_flower`),
    input_count: 1,
    result: { id: id(`${v.id}_dried_flower`), count: 1 },
    processing_time: proc.drying.ticks,
  });
}
write(`data/${NS}/recipe/sieving/resin.json`, {
  type: id('sieving'),
  ingredient: proc.sieving.ingredient,
  input_count: 1,
  result: { id: proc.sieving.result, count: proc.sieving.count },
  processing_time: proc.sieving.ticks,
});
write(`data/${NS}/recipe/pressing/hash.json`, {
  type: id('pressing'),
  ingredient: proc.pressing.ingredient,
  input_count: proc.pressing.input_count,
  result: { id: proc.pressing.result, count: proc.pressing.count },
  processing_time: proc.pressing.ticks,
});

// ------------------------------------------------------------------ data: tags
const crops = strains.map((v) => id(`${v.id}_crop`));
write(`data/${NS}/tags/block/greenhouse_glass.json`, { replace: false, values: M.greenhouse_glass });
write(`data/${NS}/tags/item/dried_flowers.json`, { replace: false, values: strains.map((v) => id(`${v.id}_dried_flower`)) });
write(`data/${NS}/tags/item/fresh_flowers.json`, { replace: false, values: strains.map((v) => id(`${v.id}_fresh_flower`)) });
write(`data/${NS}/tags/item/seeds.json`, { replace: false, values: strains.map((v) => id(`${v.id}_seeds`)) });
// Entradas de cada estação (cultivo:<estação>_inputs), espelhando os ingredientes das receitas acima. O cliente
// não recebe as receitas, mas recebe as tags: o bloco usa esta tag para prever o clique.
const stationInputs = {
  drying: ['#cultivo:fresh_flowers'],
  sieving: [proc.sieving.ingredient],
  pressing: [proc.pressing.ingredient],
};
for (const s of stations) {
  write(`data/${NS}/tags/item/${s.id}_inputs.json`, { replace: false, values: stationInputs[s.type] });
}
for (const tag of ['crops', 'maintains_farmland', 'bee_growables']) {
  write(`data/minecraft/tags/block/${tag}.json`, { replace: false, values: crops });
}

// ------------------------------------------------------------------ data: worldgen
// Lavouras de aldeia de planície: regras do mod ANTES das regras vanilla (que são mantidas).
const rule = (block, probability, output) => ({
  input_predicate: { predicate_type: 'minecraft:random_block_match', block: 'minecraft:wheat', probability },
  location_predicate: { predicate_type: 'minecraft:always_true' },
  output_state: output, // string de block state, como no farm_plains.json vanilla (age padrão 0)
});
write('data/minecraft/worldgen/processor_list/farm_plains.json', {
  processors: [{
    processor_type: 'minecraft:rule',
    rules: [
      ...ss.farm_rules.map((r) => rule('minecraft:wheat', r.probability, id(`${r.variety}_crop`))),
      ...ss.vanilla_farm_plains_rules.map((r) => rule('minecraft:wheat', r.probability, r.block)),
    ],
  }],
});
write(`data/${NS}/worldgen/feature/${wild.id}.json`, {
  type: 'minecraft:simple_block',
  to_place: { id: id(wild.id) },
});
const patch = (rarity) => ({
  feature: id(wild.id),
  placement: [
    { type: 'minecraft:rarity_filter', chance: rarity },
    { type: 'minecraft:in_square' },
    { type: 'minecraft:heightmap', heightmap: 'MOTION_BLOCKING' },
    { type: 'minecraft:biome' },
    { type: 'minecraft:count', count: 12 },
    {
      // Como em placed_feature/patch_melon.json do 26.3 (random_patch não existe mais)
      type: 'minecraft:offset',
      x: { type: 'minecraft:trapezoid', min: -4, max: 4, plateau: 0 },
      y: { type: 'minecraft:trapezoid', min: -2, max: 2, plateau: 0 },
      z: { type: 'minecraft:trapezoid', min: -4, max: 4, plateau: 0 },
    },
    {
      type: 'minecraft:block_predicate_filter',
      predicate: {
        type: 'minecraft:all_of',
        predicates: [
          { type: 'minecraft:replaceable' },
          { type: 'minecraft:matching_fluids', fluids: 'minecraft:empty' },
          { type: 'minecraft:would_survive', state: id(wild.id) },
        ],
      },
    },
  ],
});
write(`data/${NS}/worldgen/placed_feature/${wild.id}_jungle.json`, patch(16));
write(`data/${NS}/worldgen/placed_feature/${wild.id}_plains.json`, patch(48));
write(`data/${NS}/forge/biome_modifier/${wild.id}_jungle.json`, {
  type: 'forge:add_features',
  biomes: ss.wild_biomes.jungle,
  features: id(`${wild.id}_jungle`),
  step: 'vegetal_decoration',
});
write(`data/${NS}/forge/biome_modifier/${wild.id}_plains.json`, {
  type: 'forge:add_features',
  biomes: ss.wild_biomes.plains,
  features: id(`${wild.id}_plains`),
  step: 'vegetal_decoration',
});

// ------------------------------------------------------------------ data: GameTest instances
for (const t of M.game_tests) {
  write(`data/${NS}/test_instance/${t.name}.json`, {
    type: 'minecraft:function',
    function: id(t.name),
    environment: 'minecraft:default',
    structure: 'forge:empty9x9x9',
    max_ticks: t.max_ticks,
  });
}

console.log(`${textures.length} texturas e ${count} arquivos JSON gerados em ${OUT}`);
