// Modelos 3D (elements) gerados: as três estações (blockstate multipart com facing + contents) e os modelos na
// mão dos aparelhos de fumo. Coordenadas em 1/16 de bloco, como nos JSON vanilla do 26.3.
// Usado por tools/gen/generate.mjs; as texturas citadas aqui são desenhadas em tools/art/textures.mjs.

const NS = 'cultivo';
const id = (path) => `${NS}:${path}`;

// ------------------------------------------------------------------ caixas com veio
// Eixos U/V naturais de cada face (FaceBakery.defaultFaceUV do 26.3)
const FACE_UV_AXES = {
  down: ['x', 'z'], up: ['x', 'z'], north: ['x', 'y'], south: ['x', 'y'], west: ['z', 'y'], east: ['z', 'y'],
};
const NORMAL_AXIS = { down: 'y', up: 'y', north: 'z', south: 'z', west: 'x', east: 'x' };
const ALL_FACES = ['down', 'up', 'north', 'south', 'west', 'east'];
const AXIS = { x: 0, y: 1, z: 2 };

/** UV padrão de uma face, igual ao FaceBakery.defaultFaceUV do 26.3. */
export function defaultFaceUv(from, to, face) {
  switch (face) {
    case 'down': return [from[0], 16 - to[2], to[0], 16 - from[2]];
    case 'up': return [from[0], from[2], to[0], to[2]];
    case 'north': return [16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]];
    case 'south': return [from[0], 16 - to[1], to[0], 16 - from[1]];
    case 'west': return [from[2], 16 - to[1], to[2], 16 - from[1]];
    case 'east': return [16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]];
    default: throw new Error(face);
  }
}

const clampStart = (start, size) => Math.max(0, Math.min(16 - size, start));
const round = (v) => Math.round(v * 1000) / 1000;

/**
 * Face com o veio da textura (linhas horizontais) seguindo o eixo `grain`. Se o veio cai no V natural da face, o
 * UV é transposto e girado 90°, como numa tora deitada.
 */
function grainFace(from, to, face, texture, grain) {
  const [u1, v1, u2, v2] = defaultFaceUv(from, to, face);
  const w = u2 - u1;
  const h = v2 - v1;
  const [, vAxis] = FACE_UV_AXES[face];
  if (grain && grain === vAxis && grain !== NORMAL_AXIS[face]) {
    const a = clampStart(v1, h);
    const b = clampStart(u1, w);
    return { uv: [a, b, a + h, b + w].map(round), rotation: 90, texture };
  }
  return { uv: [clampStart(u1, w), clampStart(v1, h), clampStart(u1, w) + w, clampStart(v1, h) + h].map(round), texture };
}

/**
 * Caixa de 6 faces (ou só as listadas) com UV padrão, opcionalmente com veio e cullface nas faces que encostam na
 * borda do bloco.
 */
export function box(from, to, texture, { grain = null, faces = ALL_FACES, uv = {}, cull = true, rotation, shade } = {}) {
  const out = { from, to, faces: {} };
  for (const face of faces) {
    const f = uv[face] ? { uv: uv[face], texture } : grainFace(from, to, face, texture, grain);
    if (cull) {
      const axis = AXIS[NORMAL_AXIS[face]];
      const atMin = (face === 'down' || face === 'north' || face === 'west') && from[axis] === 0;
      const atMax = (face === 'up' || face === 'south' || face === 'east') && to[axis] === 16;
      if (atMin || atMax) f.cullface = face;
    }
    out.faces[face] = f;
  }
  if (rotation) out.rotation = rotation;
  if (shade) out.shade_direction_override = shade;
  return out;
}

// ------------------------------------------------------------------ estações
/**
 * Propriedades do blockstate das estações e seus valores (espelhadas em ContentIds.stateProperties; a peneira também
 * tem grinder = true|false).
 */
export const STATION_PROPERTIES = {
  facing: ['north', 'east', 'south', 'west'],
  contents: ['empty', 'loaded', 'working', 'done'],
};
const FACING_Y = { north: 0, east: 90, south: 180, west: 270 };

const blockParent = (textures, elements, extra = {}) => ({ parent: 'minecraft:block/block', textures, ...extra, elements });

/**
 * Modelos de cada estação: base (sempre) e partes ligadas a propriedades do blockstate. Cada parte diz em que valores
 * aparece (`when: {contents: [...]}`); o dichavador da peneira é a parte com `when: {grinder: ['true']}`. Carregada
 * (loaded: a entrada está lá mas o ciclo não anda) mostra a entrada com texturas paradas; trabalhando (working), as
 * animadas. `itemParts` são as partes que o ícone do item mostra junto com a base (a estação vazia como ela fica
 * colocada).
 */
export function stationModels() {
  const rackWood = '#wood';
  const bundles = [];
  for (const cx of [5, 8, 11]) {
    // Maço: dois planos cruzados a 45°, pendurados na barra de cima, com um nó de barbante em volta da barra
    for (const angle of [45, -45]) {
      bundles.push({
        from: [cx - 2, 5, 8], to: [cx + 2, 13, 8],
        rotation: { origin: [cx, 9, 8], axis: 'y', angle },
        shade_direction_override: 'up',
        faces: {
          north: { uv: [6, 2, 10, 10], texture: '#bundle' },
          south: { uv: [6, 2, 10, 10], texture: '#bundle' },
        },
      });
    }
    bundles.push(box([cx - 0.5, 12.75, 7.25], [cx + 0.5, 14.25, 8.75], '#bundle', {
      cull: false, uv: Object.fromEntries(ALL_FACES.map((f) => [f, [6, 2, 7, 3]])),
    }));
  }

  return {
    drying_rack: {
      base: blockParent({ particle: id('block/drying_rack_wood'), wood: id('block/drying_rack_wood') }, [
        box([0, 0, 6], [4, 1, 10], rackWood, { grain: 'x' }),
        box([12, 0, 6], [16, 1, 10], rackWood, { grain: 'x' }),
        box([1, 1, 7], [3, 15, 9], rackWood, { grain: 'y' }),
        box([13, 1, 7], [15, 15, 9], rackWood, { grain: 'y' }),
        box([3, 13, 7.5], [13, 14, 8.5], rackWood, { grain: 'x', faces: ['down', 'up', 'north', 'south'] }),
        box([3, 3, 7.5], [13, 4, 8.5], rackWood, { grain: 'x', faces: ['down', 'up', 'north', 'south'] }),
      ]),
      templates: {
        drying_rack_bundles: { parent: 'minecraft:block/block', ambientocclusion: false, textures: { particle: '#bundle' }, elements: bundles },
      },
      parts: {
        loaded: { when: { contents: ['loaded'] }, model: { parent: id('block/drying_rack_bundles'), textures: { bundle: id('block/drying_bundle_fresh_still') } } },
        working: { when: { contents: ['working'] }, model: { parent: id('block/drying_rack_bundles'), textures: { bundle: id('block/drying_bundle_fresh') } } },
        done: { when: { contents: ['done'] }, model: { parent: id('block/drying_rack_bundles'), textures: { bundle: id('block/drying_bundle_dried') } } },
      },
      itemParts: [],
    },
    sieve: {
      base: blockParent({ particle: id('block/sieve_wood'), wood: id('block/sieve_wood'), mesh: id('block/sieve_mesh') }, [
        box([1, 0, 1], [3, 10, 3], '#wood', { grain: 'y' }),
        box([13, 0, 1], [15, 10, 3], '#wood', { grain: 'y' }),
        box([1, 0, 13], [3, 10, 15], '#wood', { grain: 'y' }),
        box([13, 0, 13], [15, 10, 15], '#wood', { grain: 'y' }),
        box([0, 10, 0], [16, 13, 2], '#wood', { grain: 'x' }),
        box([0, 10, 14], [16, 13, 16], '#wood', { grain: 'x' }),
        box([0, 10, 2], [2, 13, 14], '#wood', { grain: 'z' }),
        box([14, 10, 2], [16, 13, 14], '#wood', { grain: 'z' }),
        { from: [2, 11, 2], to: [14, 11, 14], faces: { up: { uv: [2, 2, 14, 14], texture: '#mesh' }, down: { uv: [2, 2, 14, 14], texture: '#mesh' } } },
        box([2, 2, 2], [14, 3, 14], '#wood', { grain: 'x' }),
      ]),
      templates: {},
      parts: {
        loaded: { when: { contents: ['loaded'] }, model: sieveFlowers('block/sieve_flowers_still') },
        working: { when: { contents: ['working'] }, model: sieveFlowers('block/sieve_flowers') },
        done: {
          when: { contents: ['done'] },
          model: blockParent({ particle: id('block/sieve_resin'), resin: id('block/sieve_resin') }, [
            box([4, 3, 4], [12, 4, 12], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
            box([6, 4, 6], [10, 5, 10], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
          ]),
        },
        // Dichavador instalado: cilindro de metal 4x4x2 apoiado no canto da moldura (tampa serrilhada em cima, UV 1:1).
        // A caixa de seleção da peneira cresce junto (SieveBlock.GRINDER_BOX, as mesmas medidas)
        grinder: {
          when: { grinder: ['true'] },
          model: blockParent({ particle: id('block/grinder_block'), grinder: id('block/grinder_block') }, [
            box([11, 13, 11], [15, 15, 15], '#grinder', {
              uv: { up: [0, 0, 4, 4], down: [4, 0, 8, 4], north: [0, 8, 4, 10], south: [4, 8, 8, 10], west: [8, 8, 12, 10], east: [12, 8, 16, 10] },
            }),
          ]),
        },
      },
      itemParts: [],
    },
    press: {
      base: blockParent({ particle: id('block/press_wood'), wood: id('block/press_wood'), iron: id('block/press_iron') }, [
        box([1, 0, 1], [15, 3, 15], '#wood', { grain: 'x' }),
        box([1, 3, 6], [3, 15, 10], '#wood', { grain: 'y' }),
        box([13, 3, 6], [15, 15, 10], '#wood', { grain: 'y' }),
        box([3, 12, 6], [13, 15, 10], '#wood', { grain: 'x', faces: ['down', 'up', 'north', 'south'] }),
        box([4, 15, 7.5], [12, 16, 8.5], '#iron', { grain: 'x' }),
      ]),
      templates: {},
      parts: {
        // Placa parada em cima (vazia, carregada ou pronta). Trabalhando, só a resina fica no modelo do bloco: a placa
        // que sobe e desce é desenhada pelo cliente (PressPlatenRenderer, modelo press_platen_moving)
        platen_up: {
          when: { contents: ['empty', 'loaded', 'done'] },
          model: blockParent({ particle: id('block/press_iron'), iron: id('block/press_iron') }, [
            box([7.5, 8.5, 7.5], [8.5, 12, 8.5], '#iron', { grain: 'y', faces: ['north', 'south', 'west', 'east'] }),
            box([4, 7, 4], [12, 8.5, 12], '#iron', { grain: 'x' }),
          ]),
        },
        // Carregada: um montinho de resina parado na base, debaixo da placa erguida (ainda não dá para prensar)
        loaded: {
          when: { contents: ['loaded'] },
          model: blockParent({ particle: id('block/press_resin'), resin: id('block/press_resin') }, [
            box([5, 3, 5], [11, 4, 11], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
          ]),
        },
        squeeze: {
          when: { contents: ['working'] },
          model: blockParent({ particle: id('block/press_resin'), resin: id('block/press_resin') }, [
            box([3.5, 3, 3.5], [12.5, 4.5, 12.5], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
          ]),
        },
        hash: {
          when: { contents: ['done'] },
          model: blockParent({ particle: id('block/press_hash'), hash: id('block/press_hash') }, [
            box([5, 3, 5], [11, 4.5, 11], '#hash', {
              faces: ['up', 'north', 'south', 'west', 'east'],
              uv: { up: [5, 5, 11, 11], north: [5, 0, 11, 1.5], south: [5, 0, 11, 1.5], west: [5, 0, 11, 1.5], east: [5, 0, 11, 1.5] },
            }),
          ]),
        },
      },
      // A prensa vazia colocada tem a placa e o parafuso (platen_up); o ícone mostra o mesmo, não só a armação
      itemParts: ['platen_up'],
    },
  };
}

/** Camada de flores na tela da peneira (carregada: textura parada; trabalhando: a que treme). */
function sieveFlowers(texture) {
  return blockParent({ particle: id(texture), flowers: id(texture) }, [
    box([3, 11, 3], [13, 12, 13], '#flowers', {
      faces: ['up', 'north', 'south', 'west', 'east'],
      uv: { up: [3, 3, 13, 13], north: [3, 0, 13, 1], south: [3, 0, 13, 1], west: [3, 0, 13, 1], east: [3, 0, 13, 1] },
    }),
  ]);
}

/** Condição multipart de uma parte: {propriedade: [valores]} vira {propriedade: "a|b"}. */
const whenOf = (when) => Object.fromEntries(Object.entries(when).map(([prop, values]) => [prop, values.join('|')]));

/** Blockstate multipart de uma estação: base girada pelo facing e cada parte só nos valores da condição dela. */
export function stationBlockstate(stationId, parts) {
  const multipart = [];
  for (const facing of STATION_PROPERTIES.facing) {
    const y = FACING_Y[facing];
    const apply = (model) => (y ? { model, y } : { model });
    multipart.push({ when: { facing }, apply: apply(id(`block/${stationId}`)) });
    for (const [part, def] of Object.entries(parts)) {
      multipart.push({ when: { facing, ...whenOf(def.when) }, apply: apply(id(`block/${stationId}_${part}`)) });
    }
  }
  return { multipart };
}

// ------------------------------------------------------------------ artigos colocáveis (plano 2.5)
/** Nível máximo de cinza do cinzeiro (espelho de AshtrayRules.MAX_ASH). */
export const ASHTRAY_MAX_ASH = 4;

/** Face com UV explícito: as peças pequenas usam regiões próprias da textura, sempre 1:1. */
const face = (texture, uv, extra = {}) => ({ uv, texture, ...extra });

/**
 * Modelos dos artigos que viram bloco: base (sempre) e partes por propriedade. A bandeja gira com o facing (variantes,
 * como as estações); o pote mostra o conteúdo atrás do vidro com filled=true; o cinzeiro empilha a cinza por nível,
 * como os conteúdos da composteira vanilla.
 */
export function articleModels() {
  const tray = '#tray';
  // Bandeja (frente para o norte): fundo esmaltado, borda de 1 px, uma folha de seda e um botão em cima
  const rim = (from, to, long) => {
    const top = long ? [0, 12, 12, 13] : [0, 12, 8, 13];
    const side = long ? [0, 13, 12, 14] : [0, 13, 8, 14];
    const end = [0, 13, 1, 14];
    return {
      from, to,
      faces: long
        ? { up: face(tray, top), north: face(tray, side), south: face(tray, side), west: face(tray, end), east: face(tray, end) }
        : { up: face(tray, top, { rotation: 90 }), west: face(tray, side), east: face(tray, side) },
    };
  };
  const rollingTray = blockParent({ particle: id('block/rolling_tray_block'), tray: id('block/rolling_tray_block') }, [
    {
      from: [2, 0, 3], to: [14, 0.5, 13],
      faces: {
        up: face(tray, [0, 0, 12, 10]),
        down: face(tray, [0, 10, 12, 11], { cullface: 'down' }),
        north: face(tray, [0, 10, 12, 10.5]), south: face(tray, [0, 10, 12, 10.5]),
        west: face(tray, [0, 10, 10, 10.5]), east: face(tray, [0, 10, 10, 10.5]),
      },
    },
    rim([2, 0.5, 3], [14, 1.5, 4], true),
    rim([2, 0.5, 12], [14, 1.5, 13], true),
    rim([2, 0.5, 4], [3, 1.5, 12], false),
    rim([13, 0.5, 4], [14, 1.5, 12], false),
    {
      from: [9, 0.5, 5], to: [12, 0.75, 9],
      faces: {
        up: face(tray, [12, 0, 15, 4]),
        north: face(tray, [12, 0, 15, 0.25]), south: face(tray, [12, 3.75, 15, 4]),
        west: face(tray, [12, 0, 16, 0.25]), east: face(tray, [12, 3.75, 16, 4]),
      },
    },
    {
      from: [4, 0.5, 6], to: [6, 2, 8],
      faces: {
        up: face(tray, [12, 4, 14, 6]),
        north: face(tray, [12, 6, 14, 7.5]), south: face(tray, [14, 6, 16, 7.5]),
        west: face(tray, [14, 6, 16, 7.5]), east: face(tray, [12, 6, 14, 7.5]),
      },
    },
  ]);

  // Pote: vidro com borda e reflexo opacos e o resto transparente (cutout), tampa de madeira; conteúdo atrás do vidro
  const glassSide = [5, 8, 11, 16];
  const jar = blockParent({ particle: id('block/jar_glass'), glass: id('block/jar_glass'), lid: id('block/jar_lid') }, [
    {
      from: [5, 0, 5], to: [11, 8, 11],
      faces: {
        down: face('#glass', [5, 1, 11, 7], { cullface: 'down' }),
        north: face('#glass', glassSide), south: face('#glass', glassSide),
        west: face('#glass', glassSide), east: face('#glass', glassSide),
      },
    },
    box([4.5, 8, 4.5], [11.5, 9.5, 11.5], '#lid', { grain: 'x' }),
  ]);
  const jarFilled = blockParent({ particle: id('block/jar_contents'), contents: id('block/jar_contents') }, [
    box([5.5, 0.5, 5.5], [10.5, 6.5, 10.5], '#contents', { faces: ['up', 'north', 'south', 'west', 'east'] }),
  ]);

  // Cinzeiro: prato raso de terracota (fundo e quatro paredes) e a cinza subindo 0,25 por nível
  const clay = '#clay';
  const ashtray = blockParent({ particle: id('block/ashtray_block'), clay: id('block/ashtray_block') }, [
    box([4, 0, 4], [12, 0.5, 12], clay),
    box([4, 0.5, 4], [12, 2, 5], clay, { faces: ['up', 'north', 'south', 'west', 'east'] }),
    box([4, 0.5, 11], [12, 2, 12], clay, { faces: ['up', 'north', 'south', 'west', 'east'] }),
    box([4, 0.5, 5], [5, 2, 11], clay, { faces: ['up', 'west', 'east'] }),
    box([11, 0.5, 5], [12, 2, 11], clay, { faces: ['up', 'west', 'east'] }),
  ]);
  const ashParts = {};
  for (let n = 1; n <= ASHTRAY_MAX_ASH; n++) {
    const elements = [box([5, 0.5, 5], [11, 0.5 + 0.25 * n, 11], '#ash', { faces: ['up'] })];
    if (n === ASHTRAY_MAX_ASH) {
      // Cheio: um montinho no meio
      elements.push(box([6.5, 1.5, 6.5], [9.5, 2.25, 9.5], '#ash', { faces: ['up', 'north', 'south', 'west', 'east'] }));
    }
    ashParts[`ash${n}`] = {
      when: { ash: [String(n)] },
      model: blockParent({ particle: id('block/ashtray_ash'), ash: id('block/ashtray_ash') }, elements),
    };
  }

  return {
    rolling_tray: { facing: true, base: rollingTray, parts: {} },
    jar: { facing: false, base: jar, parts: { filled: { when: { filled: ['true'] }, model: jarFilled } } },
    ashtray: { facing: false, base: ashtray, parts: ashParts },
  };
}

/**
 * Blockstate de um artigo colocável. Com facing (bandeja), só a base girada, em variantes por facing; sem facing,
 * multipart com a base sempre e cada parte só nos valores da condição dela.
 */
export function articleBlockstate(articleId, def) {
  if (def.facing) {
    const variants = {};
    for (const facing of STATION_PROPERTIES.facing) {
      const y = FACING_Y[facing];
      variants[`facing=${facing}`] = y ? { model: id(`block/${articleId}`), y } : { model: id(`block/${articleId}`) };
    }
    return { variants };
  }
  const multipart = [{ apply: { model: id(`block/${articleId}`) } }];
  for (const [part, p] of Object.entries(def.parts)) {
    multipart.push({ when: whenOf(p.when), apply: { model: id(`block/${articleId}_${part}`) } });
  }
  return { multipart };
}

// ------------------------------------------------------------------ aparelhos de fumo na mão
// Transformações de exibição (conferidas num renderizador que repete a cadeia de 1ª/3ª pessoa do 26.3): na 3ª
// pessoa, sem rotação, o +Y do modelo aponta para a frente da mão; por isso os aparelhos em pé levam ~75° em X (o
// braço já pende 18° para a frente). Na 1ª pessoa, sem rotação, o modelo fica em pé no canto inferior direito, como a
// luneta vanilla. O ícone plano continua no inventário, no chão e em molduras (select em display_context).
const HAND = (third, first) => ({
  thirdperson_righthand: third,
  thirdperson_lefthand: { ...third, rotation: [third.rotation[0], -third.rotation[1], -third.rotation[2]] },
  firstperson_righthand: first,
  firstperson_lefthand: { ...first, rotation: [first.rotation[0], -first.rotation[1], -first.rotation[2]] },
});

export function handModels() {
  const m = (name) => id(`item/model_${name}`);
  const ash = id('item/smoke_ash');
  // Peça sem tampa/fundo quando outra peça encosta (as faces escondidas não são geradas)
  const part = (from, to, texture, faces) => box(from, to, texture, { cull: false, ...(faces ? { faces } : {}) });
  const SIDES = ['north', 'south', 'west', 'east'];
  return {
    bong: {
      textures: { particle: m('glass'), glass: m('glass'), water: m('water'), steel: m('steel'), bowl: ash },
      elements: [
        // Câmara com água, tubo, boca, downstem inclinado e fornilho de lado
        part([5, 0, 5], [11, 5, 11], '#water', ['down', ...SIDES]),
        part([5, 4.99, 5], [11, 5, 11], '#glass', ['up']),
        part([6.5, 5, 6.5], [9.5, 15, 9.5], '#glass', SIDES),
        part([6, 15, 6], [10, 16, 10], '#glass'),
        { ...part([10.5, 2.5, 7.5], [14, 3.5, 8.5], '#steel', ['up', 'down', 'north', 'south', 'east']),
          rotation: { origin: [10.5, 3, 8], axis: 'z', angle: 45 } },
        part([12.5, 5, 7], [14.5, 7, 9], '#steel', ['down', ...SIDES]),
        part([12.5, 6.99, 7], [14.5, 7, 9], '#bowl', ['up']),
      ],
      display: HAND(
        { rotation: [75, 0, 0], translation: [0, 2, 1.5], scale: [0.55, 0.55, 0.55] },
        { rotation: [0, -25, 5], translation: [-1, 5, -2], scale: [0.55, 0.55, 0.55] },
      ),
    },
    hookah: {
      textures: { particle: m('green_glass'), glass: m('green_glass'), brass: m('brass'), hose: m('hose'), clay: m('clay'), bowl: ash },
      elements: [
        part([5, 0, 5], [11, 5, 11], '#glass'),
        part([6.5, 5, 6.5], [9.5, 6, 9.5], '#glass', ['up', ...SIDES]),
        part([7.25, 6, 7.25], [8.75, 13, 8.75], '#brass', SIDES),
        part([5.5, 10, 5.5], [10.5, 10.5, 10.5], '#brass'),
        part([6.5, 13, 6.5], [9.5, 15, 9.5], '#clay', ['down', ...SIDES]),
        part([6.5, 14.99, 6.5], [9.5, 15, 9.5], '#bowl', ['up']),
        part([8.75, 7, 7.5], [12, 8, 8.5], '#hose', ['up', 'down', 'north', 'south', 'east']),
        part([11, 1, 7.5], [12, 7, 8.5], '#hose', SIDES),
        part([10.75, 0, 7.25], [12.25, 1, 8.75], '#brass'),
      ],
      display: HAND(
        { rotation: [75, 0, 0], translation: [0, 2, 1.5], scale: [0.55, 0.55, 0.55] },
        { rotation: [0, -25, 5], translation: [-1, 5, -2], scale: [0.55, 0.55, 0.55] },
      ),
    },
    pipe: {
      textures: { particle: m('dark_wood'), bowl_wood: m('dark_wood'), wood: m('wood'), bone: m('bone'), bowl: ash },
      elements: [
        part([4, 4, 6.5], [7, 9, 9.5], '#bowl_wood', ['down', ...SIDES]),
        part([4, 8.99, 6.5], [7, 9, 9.5], '#bowl', ['up']),
        part([7, 4.5, 7.5], [13, 5.5, 8.5], '#wood', ['up', 'down', 'north', 'south']),
        part([13, 4.75, 7.6], [15, 5.25, 8.4], '#bone', ['up', 'down', 'north', 'south', 'east']),
      ],
      display: HAND(
        { rotation: [75, -90, 0], translation: [0, 3, 1], scale: [0.6, 0.6, 0.6] },
        { rotation: [10, -60, 0], translation: [-1, 6, -2], scale: [0.65, 0.65, 0.65] },
      ),
    },
    chillum: {
      textures: { particle: m('clay'), clay: m('clay'), band: m('clay_band'), bowl: ash },
      elements: [
        part([7.25, 0, 7.25], [8.75, 6, 8.75], '#clay', ['down', ...SIDES]),
        part([7, 6, 7], [9, 7, 9], '#band', ['down', ...SIDES]),
        part([7, 7, 7], [9, 11, 9], '#clay', SIDES),
        part([6.75, 11, 6.75], [9.25, 12, 9.25], '#band', ['down', ...SIDES]),
        part([6.5, 12, 6.5], [9.5, 14, 9.5], '#clay', ['down', ...SIDES]),
        part([6.5, 13.99, 6.5], [9.5, 14, 9.5], '#bowl', ['up']),
      ],
      display: HAND(
        { rotation: [75, 0, 0], translation: [0, 2, 4], scale: [0.6, 0.6, 0.6] },
        { rotation: [0, -20, -20], translation: [0, 5, -1], scale: [0.6, 0.6, 0.6] },
      ),
    },
    joint: {
      // Cone deitado no eixo X: ponta da boca de papel torcido (com piteira, papelão: FILTERED_HAND), corpo de papel
      // que engrossa e a ponta torcida (acesa: brasa)
      textures: { particle: m('paper'), paper: m('paper'), crutch: m('paper'), tip: m('paper') },
      elements: [
        part([3, 7.6, 7.6], [4.2, 8.4, 8.4], '#crutch', ['up', 'down', 'north', 'south', 'west']),
        part([4.2, 7.55, 7.55], [10, 8.45, 8.45], '#paper', ['up', 'down', 'north', 'south']),
        part([10, 7.45, 7.45], [12.6, 8.55, 8.55], '#paper', ['up', 'down', 'north', 'south', 'west']),
        part([12.6, 7.55, 7.55], [13.4, 8.45, 8.45], '#tip', ['up', 'down', 'north', 'south', 'east']),
      ],
      display: HAND(
        { rotation: [90, 90, 0], translation: [0, 2, 0], scale: [0.7, 0.7, 0.7] },
        { rotation: [0, 30, 40], translation: [0, 5, 0], scale: [0.6, 0.6, 0.6] },
      ),
    },
    lighter: {
      textures: { particle: m('plastic'), plastic: m('plastic'), steel: m('steel') },
      elements: [
        part([6.5, 0, 7], [9.5, 5, 9], '#plastic', ['down', ...SIDES]),
        part([6.5, 5, 7], [9.5, 6.5, 9], '#steel', ['up', ...SIDES]),
        part([7, 6.5, 7.6], [8.5, 7.3, 8.4], '#steel', ['up', ...SIDES]),
      ],
      display: HAND(
        { rotation: [75, 0, 0], translation: [0, 2, 2.5], scale: [0.55, 0.55, 0.55] },
        { rotation: [0, -30, 10], translation: [-1, 6, -2], scale: [0.7, 0.7, 0.7] },
      ),
    },
  };
}

// ------------------------------------------------------------------ animação de fumar
const DEG = Math.PI / 180;
const PX = 1 / 16;
// Matrizes 4x4 row-major (só para resolver a posição na mão; o jogo usa as do JOML, com a mesma convenção)
const mat = {
  mul(a, b) {
    const r = new Array(16).fill(0);
    for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) for (let k = 0; k < 4; k++) r[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
    return r;
  },
  chain: (...ms) => ms.reduce((acc, m) => mat.mul(acc, m)),
  T: (x, y, z) => [1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1],
  S: (s) => [s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1],
  RX: (a) => [1, 0, 0, 0, 0, Math.cos(a), -Math.sin(a), 0, 0, Math.sin(a), Math.cos(a), 0, 0, 0, 0, 1],
  RY: (a) => [Math.cos(a), 0, Math.sin(a), 0, 0, 1, 0, 0, -Math.sin(a), 0, Math.cos(a), 0, 0, 0, 0, 1],
  RZ: (a) => [Math.cos(a), -Math.sin(a), 0, 0, Math.sin(a), Math.cos(a), 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  apply: (m, [x, y, z]) => [0, 1, 2].map((r) => m[r * 4] * x + m[r * 4 + 1] * y + m[r * 4 + 2] * z + m[r * 4 + 3]),
};
const round3 = (v) => Math.round(v * 1000) / 1000;

/**
 * Translação de exibição (em 1/16 de bloco) que leva o ponto `point` do modelo na mão (1/16) até `target` (px do
 * modelo do jogador, cabeça reta) com o braço em [xRot, yRot] (radianos, a pose do braço direito) e a exibição com
 * `rotation`/`scale` aplicadas de fato. É a cadeia do 26.3: ModelPart do braço (pivô (-5, 2, 0) px, rotação Z·Y·X),
 * ItemInHandLayer (X -90°, Y 180° e (1, 2, -10) px) e ItemTransform (translação, rotação XYZ, escala, -0,5).
 *
 * <p>Com `side = 'left'` resolve o braço esquerdo: pivô (5, 2, 0), yRot negado (SmokeAnimation.armAngles espelha) e
 * (-1, 2, -10) px na camada da mão. A rotação aplicada é a da direita (o ItemTransform#apply do 26.3 nega Y e Z de
 * novo na mão esquerda, então o JSON da esquerda guarda a rotação espelhada); o resultado é a translação aplicada, que
 * o JSON da esquerda guarda com o x negado ({@link leftHandJson}), porque o jogo nega o x outra vez.
 */
export function solveHandTranslation(arm, rotation, scale, point, target, side = 'right') {
  const sx = side === 'left' ? -1 : 1;
  const A = mat.chain(mat.T(-5 * PX * sx, 2 * PX, 0), mat.RY(arm[1] * sx), mat.RX(arm[0]), mat.RX(-90 * DEG), mat.RY(180 * DEG),
    mat.T(PX * sx, 2 * PX, -10 * PX));
  const RS = mat.chain(mat.RX(rotation[0] * DEG), mat.RY(rotation[1] * DEG), mat.RZ(rotation[2] * DEG), mat.S(scale));
  const local = mat.apply(RS, point.map((v) => v / 16 - 0.5));
  const d = target.map((v, k) => v * PX - A[k * 4 + 3]);
  // A é rígida: a inversa da rotação é a transposta
  const back = [0, 1, 2].map((c) => A[c] * d[0] + A[4 + c] * d[1] + A[8 + c] * d[2]);
  return back.map((v, k) => Math.round((v - local[k]) * 1600) / 100);
}

/** A translação aplicada na mão esquerda escrita no JSON: o ItemTransform#apply do 26.3 nega o x de novo. */
export function leftHandJson(translation) {
  return [round3(-translation[0]), translation[1], translation[2]];
}

/**
 * Exibições da animação de fumar (seção smoke_animation do manifesto): `lit` é o modelo aceso com o aparelho na boca na
 * 3ª pessoa e a exibição de quem segura na 1ª (o cliente anima a subida, SmokeAnimation.firstPersonMatrix); `keyframes`
 * são os quadros k0..k(K-1) da 3ª pessoa, interpolados (rotação XYZ, translação e escala) entre segurar (k0) e a boca,
 * um por tick, no mesmo ritmo do braço (SmokeAnimation.thirdPersonRaise). Cada mão da 3ª pessoa é resolvida com o
 * próprio braço: copiar a translação da direita deixava o baseado e o cachimbo uns 4–5 px fora da boca na mão
 * esquerda (o pivô do braço esquerdo fica do outro lado e o yRot é espelhado).
 */
export function smokeDisplays(animation, hand) {
  const K = animation.keyframes;
  const out = {};
  for (const [device, pose] of Object.entries(animation.poses)) {
    if (!hand[device]) throw new Error(`pose de fumar sem modelo na mão: ${device}`);
    const idle = hand[device].display;
    const s = [pose.scale, pose.scale, pose.scale];
    const target = pose.target ?? animation.mouth;
    const solve = (side) => solveHandTranslation(pose.arm, pose.rotation, pose.scale, pose.mouth_point, target, side);
    const mouth = {
      thirdperson_righthand: { rotation: pose.rotation, translation: solve('right'), scale: s },
      thirdperson_lefthand: {
        rotation: [pose.rotation[0], -pose.rotation[1], -pose.rotation[2]],
        translation: leftHandJson(solve('left')),
        scale: s,
      },
    };
    const lerp3 = (a, b, f) => a.map((v, k) => round3(v + (b[k] - v) * f));
    const lerpDisplay = (a, b, f) => ({
      rotation: lerp3(a.rotation, b.rotation, f),
      translation: lerp3(a.translation, b.translation, f),
      scale: lerp3(a.scale, b.scale, f),
    });
    // Mesma ordem de contextos de HAND: 3ª pessoa direita e esquerda, depois a 1ª pessoa (a exibição de quem segura)
    const display = (thirdRight, thirdLeft) => ({
      thirdperson_righthand: thirdRight,
      thirdperson_lefthand: thirdLeft,
      firstperson_righthand: idle.firstperson_righthand,
      firstperson_lefthand: idle.firstperson_lefthand,
    });
    const at = (f) => display(lerpDisplay(idle.thirdperson_righthand, mouth.thirdperson_righthand, f),
      lerpDisplay(idle.thirdperson_lefthand, mouth.thirdperson_lefthand, f));
    out[device] = {
      lit: display(mouth.thirdperson_righthand, mouth.thirdperson_lefthand),
      keyframes: Array.from({ length: K }, (_, i) => at(i / K)),
    };
  }
  return out;
}

/**
 * Texturas trocadas no modelo aceso: a brasa (animada) no fornilho ou na ponta; no bong também a água, que borbulha; no
 * narguilé, o carvão em brasa.
 */
export const LIT_TEXTURES = {
  bong: { bowl: id('item/smoke_ember'), water: id('item/model_water_bubbling') },
  hookah: { bowl: id('item/hookah_coal') },
  pipe: { bowl: id('item/smoke_ember') },
  chillum: { bowl: id('item/smoke_ember') },
  joint: { tip: id('item/smoke_ember') },
};

/**
 * Baseado com piteira (cultivo:filtered): os modelos na mão herdam os do baseado (geometria e exibição, aceso ou não,
 * inclusive os quadros da animação) e só trocam a ponta da boca de papel pela piteira de papelão.
 */
export function filteredHandModels(keyframes) {
  const crutch = { crutch: id('item/model_cardboard') };
  const out = {
    joint_filtered_in_hand: { parent: id('item/joint_in_hand'), textures: crutch },
    joint_filtered_in_hand_lit: { parent: id('item/joint_in_hand_lit'), textures: crutch },
  };
  for (let i = 0; i < keyframes; i++) {
    out[`joint_filtered_in_hand_lit_k${i}`] = { parent: id(`item/joint_in_hand_lit_k${i}`), textures: crutch };
  }
  return out;
}

// ------------------------------------------------------------------ placa da prensa (desenhada pelo cliente)
/** Quanto a placa da prensa desce (px) no fundo do curso (espelho de StationAnimation.PRESS_STROKE_PIXELS). */
export const PRESS_STROKE_PIXELS = 2.5;

/**
 * Placa e parafuso da prensa que sobem e descem enquanto ela trabalha. O cliente desenha este modelo pelo estado
 * sintético cultivo:press_platen (só facing) com o deslocamento da animação. Em repouso fica na altura de platen_up; o
 * parafuso vai até dentro da travessa (y 15), então não abre vão quando a placa desce PRESS_STROKE_PIXELS.
 */
export function pressPlatenModel() {
  return blockParent({ particle: id('block/press_iron'), iron: id('block/press_iron') }, [
    box([7.5, 8.5, 7.5], [8.5, 15, 8.5], '#iron', { grain: 'y', faces: ['north', 'south', 'west', 'east'] }),
    box([4, 7, 4], [12, 8.5, 12], '#iron', { grain: 'x' }),
  ]);
}

/** Blockstate do estado sintético cultivo:press_platen: o modelo da placa girado pelo facing, como a prensa. */
export function pressPlatenBlockstate() {
  const variants = {};
  for (const facing of STATION_PROPERTIES.facing) {
    const y = FACING_Y[facing];
    variants[`facing=${facing}`] = y ? { model: id('block/press_platen_moving'), y } : { model: id('block/press_platen_moving') };
  }
  return { variants };
}

/** Aparelhos cujo modelo na mão ganha a variante "_lit" (brasa) enquanto o jogador fuma. */
export const LIT_DEVICES = ['bong', 'hookah', 'pipe', 'chillum', 'joint'];

// ------------------------------------------------------------------ estufinha (grow tent)
/** Metades da estufinha, na ordem do Minecraft (espelho de ContentIds.DOUBLE_BLOCK_HALVES). */
export const GROW_TENT_HALVES = ['upper', 'lower'];

/** Brilho do mylar de dentro da estufinha: o reflexo da lâmpada roxa aparece mesmo de dia e no escuro. */
export const GROW_TENT_MYLAR_GLOW = 11;

const OPPOSITE = { north: 'south', south: 'north', west: 'east', east: 'west', up: 'down', down: 'up' };

/**
 * Parede fina da estufinha: lona por fora (e nas bordas finas) e, por dentro, um plano de mylar que brilha com a luz
 * roxa (light_emission só vale por elemento, então o mylar é um elemento à parte, sem a face interna da lona atrás).
 * `outside` é a face que dá para fora.
 */
function tentWall(from, to, outside) {
  const inside = OPPOSITE[outside];
  const axis = AXIS[NORMAL_AXIS[inside]];
  const wall = box(from, to, '#canvas', { cull: false, faces: ALL_FACES.filter((f) => f !== inside) });
  // O plano fica na superfície de dentro da parede
  const at = (inside === 'south' || inside === 'east' || inside === 'up') ? to[axis] : from[axis];
  const planeFrom = [...from];
  const planeTo = [...to];
  planeFrom[axis] = at;
  planeTo[axis] = at;
  const mylar = {
    from: planeFrom, to: planeTo,
    light_emission: GROW_TENT_MYLAR_GLOW,
    faces: { [inside]: { uv: defaultFaceUv(planeFrom, planeTo, inside).map(round), texture: '#mylar' } },
  };
  return [wall, mylar];
}

/**
 * Modelos das duas metades da estufinha, com a frente (aberta) para o norte, como as estações: quatro postes de metal,
 * paredes de lona com mylar por dentro, chão de lona e o vaso de tecido em baixo; teto, aba enrolada e a lâmpada roxa
 * em cima. O painel de LEDs e o ponto roxo no poste da frente têm light_emission 15 (novo no 26.3): brilham mesmo no
 * escuro. A planta não está no modelo: o cliente desenha a lavoura da variedade dentro dela (GrowTentRenderer).
 */
export function growTentModels() {
  const textures = {
    particle: id('block/grow_tent_canvas'),
    canvas: id('block/grow_tent_canvas'),
    mylar: id('block/grow_tent_mylar'),
    frame: id('block/grow_tent_frame'),
    flap: id('block/grow_tent_flap'),
    lamp: id('block/grow_tent_lamp'),
    pot: id('block/grow_tent_pot'),
  };
  const poles = (top) => [[1, 1], [14, 1], [1, 14], [14, 14]].map(([x, z]) =>
    box([x, 0, z], [x + 1, top, z + 1], '#frame', { grain: 'y', cull: false }));
  // O vaso: laterais de tecido preto (linhas 8–15 da textura) e substrato em cima (linhas 0–5)
  const potSide = [5, 12, 11, 15];
  const pot = {
    from: [5, 1, 5], to: [11, 4, 11],
    faces: {
      up: face('#pot', [5, 0, 11, 6]),
      north: face('#pot', potSide), south: face('#pot', potSide), west: face('#pot', potSide), east: face('#pot', potSide),
    },
  };
  const lower = blockParent(textures, [
    ...poles(16),
    box([1, 0, 1], [15, 1, 15], '#canvas'),
    ...tentWall([2, 1, 14.5], [14, 16, 15], 'south'),
    ...tentWall([1, 1, 2], [1.5, 16, 14], 'west'),
    ...tentWall([14.5, 1, 2], [15, 16, 14], 'east'),
    // Soleira da porta: a lona da frente enrolada embaixo
    box([2, 1, 1], [14, 3, 1.5], '#flap', { cull: false }),
    pot,
  ]);
  // Lâmpada: caixa preta (linhas 0–3 da textura) e o painel de LEDs roxos (linhas 4–15) virado para baixo
  const lampHousing = [4, 0, 12, 3];
  const upper = blockParent(textures, [
    ...poles(13),
    ...tentWall([2, 0, 14.5], [14, 13, 15], 'south'),
    ...tentWall([1, 0, 2], [1.5, 13, 14], 'west'),
    ...tentWall([14.5, 0, 2], [15, 13, 14], 'east'),
    // Teto: lona em cima e nas bordas, mylar brilhando por baixo
    ...tentWall([1, 13, 1], [15, 14, 15], 'up'),
    // Aba da frente enrolada no alto, com o puxador do zíper
    box([1.5, 10, 1], [14.5, 13, 3], '#flap', { cull: false }),
    {
      from: [4, 11, 4], to: [12, 12.5, 12],
      faces: {
        up: face('#lamp', lampHousing), north: face('#lamp', lampHousing), south: face('#lamp', lampHousing),
        west: face('#lamp', lampHousing), east: face('#lamp', lampHousing),
      },
    },
    {
      from: [4.5, 10.9, 4.5], to: [11.5, 11, 11.5],
      light_emission: 15,
      shade_direction_override: 'up',
      faces: { down: face('#lamp', [4.5, 4.5, 11.5, 11.5]) },
    },
    // Luz de "ligada" no poste da frente, para ver de fora que a lâmpada está acesa
    {
      from: [1.25, 5, 0.75], to: [1.75, 6, 1],
      light_emission: 15,
      shade_direction_override: 'up',
      faces: { north: face('#lamp', [7, 7, 8, 8]), west: face('#lamp', [7, 7, 8, 8]), east: face('#lamp', [7, 7, 8, 8]) },
    },
  ]);
  return { grow_tent_lower: lower, grow_tent_upper: upper };
}

/** Blockstate da estufinha: uma variante por metade e facing (8), girada como a porta vanilla. */
export function growTentBlockstate() {
  const variants = {};
  for (const facing of STATION_PROPERTIES.facing) {
    const y = FACING_Y[facing];
    for (const half of GROW_TENT_HALVES) {
      const model = id(`block/grow_tent_${half}`);
      variants[`facing=${facing},half=${half}`] = y ? { model, y } : { model };
    }
  }
  return { variants };
}
