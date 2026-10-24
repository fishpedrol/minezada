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
/** Propriedades do blockstate das estações e seus valores (espelhadas em ContentIds.STATION_STATE_PROPERTIES). */
export const STATION_PROPERTIES = {
  facing: ['north', 'east', 'south', 'west'],
  contents: ['empty', 'working', 'done'],
};
const FACING_Y = { north: 0, east: 90, south: 180, west: 270 };

const blockParent = (textures, elements, extra = {}) => ({ parent: 'minecraft:block/block', textures, ...extra, elements });

/**
 * Modelos de cada estação: base (sempre) e partes ligadas a `contents`. Cada parte diz em que valores aparece.
 * Para a peneira ganhar o dichavador depois, basta uma parte com `when: {grinder: 'true'}` e a propriedade em
 * STATION_PROPERTIES/ContentIds.
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
        working: { when: ['working'], model: { parent: id('block/drying_rack_bundles'), textures: { bundle: id('block/drying_bundle_fresh') } } },
        done: { when: ['done'], model: { parent: id('block/drying_rack_bundles'), textures: { bundle: id('block/drying_bundle_dried') } } },
      },
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
        working: {
          when: ['working'],
          model: blockParent({ particle: id('block/sieve_flowers'), flowers: id('block/sieve_flowers') }, [
            box([3, 11, 3], [13, 12, 13], '#flowers', {
              faces: ['up', 'north', 'south', 'west', 'east'],
              uv: { up: [3, 3, 13, 13], north: [3, 0, 13, 1], south: [3, 0, 13, 1], west: [3, 0, 13, 1], east: [3, 0, 13, 1] },
            }),
          ]),
        },
        done: {
          when: ['done'],
          model: blockParent({ particle: id('block/sieve_resin'), resin: id('block/sieve_resin') }, [
            box([4, 3, 4], [12, 4, 12], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
            box([6, 4, 6], [10, 5, 10], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
          ]),
        },
      },
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
        // Placa em cima (vazia ou pronta) e embaixo apertando a resina (trabalhando)
        platen_up: {
          when: ['empty', 'done'],
          model: blockParent({ particle: id('block/press_iron'), iron: id('block/press_iron') }, [
            box([7.5, 8.5, 7.5], [8.5, 12, 8.5], '#iron', { grain: 'y', faces: ['north', 'south', 'west', 'east'] }),
            box([4, 7, 4], [12, 8.5, 12], '#iron', { grain: 'x' }),
          ]),
        },
        platen_down: {
          when: ['working'],
          model: blockParent({ particle: id('block/press_iron'), iron: id('block/press_iron'), resin: id('block/press_resin') }, [
            box([7.5, 6, 7.5], [8.5, 12, 8.5], '#iron', { grain: 'y', faces: ['north', 'south', 'west', 'east'] }),
            box([4, 4.5, 4], [12, 6, 12], '#iron', { grain: 'x' }),
            box([3.5, 3, 3.5], [12.5, 4.5, 12.5], '#resin', { faces: ['up', 'north', 'south', 'west', 'east'] }),
          ]),
        },
        hash: {
          when: ['done'],
          model: blockParent({ particle: id('block/press_hash'), hash: id('block/press_hash') }, [
            box([5, 3, 5], [11, 4.5, 11], '#hash', {
              faces: ['up', 'north', 'south', 'west', 'east'],
              uv: { up: [5, 5, 11, 11], north: [5, 0, 11, 1.5], south: [5, 0, 11, 1.5], west: [5, 0, 11, 1.5], east: [5, 0, 11, 1.5] },
            }),
          ]),
        },
      },
    },
  };
}

/** Blockstate multipart de uma estação: base girada pelo facing e cada parte só nos valores de `contents` dela. */
export function stationBlockstate(stationId, parts) {
  const multipart = [];
  for (const facing of STATION_PROPERTIES.facing) {
    const y = FACING_Y[facing];
    const apply = (model) => (y ? { model, y } : { model });
    multipart.push({ when: { facing }, apply: apply(id(`block/${stationId}`)) });
    for (const [part, def] of Object.entries(parts)) {
      multipart.push({ when: { facing, contents: def.when.join('|') }, apply: apply(id(`block/${stationId}_${part}`)) });
    }
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
      // Cone deitado no eixo X: piteira de papelão, corpo de papel que engrossa e a ponta torcida (acesa: brasa)
      textures: { particle: m('paper'), paper: m('paper'), crutch: m('cardboard'), tip: m('paper') },
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

/**
 * Exibição do modelo aceso, usado só enquanto o jogador fuma: na 3ª pessoa o braço está erguido (pose TOOT_HORN),
 * então o aparelho fica em pé diante do rosto (bong, narguilé, chillum) ou com a boquilha na boca (cachimbo,
 * baseado). Na 1ª pessoa o uso não move a mão; o aparelho chega um pouco para o centro, sem cobrir a tela.
 */
export const LIT_DISPLAY = {
  bong: HAND({ rotation: [0, 0, 0], translation: [0, 1, 1], scale: [0.55, 0.55, 0.55] }, { rotation: [0, -15, 0], translation: [-5, 4, -3], scale: [0.55, 0.55, 0.55] }),
  hookah: HAND({ rotation: [0, 0, 0], translation: [0, 1, 1], scale: [0.55, 0.55, 0.55] }, { rotation: [0, -15, 0], translation: [-5, 4, -3], scale: [0.55, 0.55, 0.55] }),
  pipe: HAND({ rotation: [0, -90, 0], translation: [0, 1, -2], scale: [0.6, 0.6, 0.6] }, { rotation: [0, -90, 0], translation: [-6, 3, -1], scale: [0.65, 0.65, 0.65] }),
  chillum: HAND({ rotation: [0, 0, 0], translation: [0, 3, -1], scale: [0.6, 0.6, 0.6] }, { rotation: [0, -20, -10], translation: [-5, 4, -2], scale: [0.6, 0.6, 0.6] }),
  joint: HAND({ rotation: [0, 90, 0], translation: [0, 2, -2], scale: [0.7, 0.7, 0.7] }, { rotation: [0, 90, 0], translation: [-7, 2, -2], scale: [0.7, 0.7, 0.7] }),
};

/** Aparelhos cujo modelo na mão ganha a variante "_lit" (brasa) enquanto o jogador fuma. */
export const LIT_DEVICES = ['bong', 'hookah', 'pipe', 'chillum', 'joint'];
/** Textura trocada na variante acesa de cada modelo na mão. */
export const LIT_TEXTURE_VAR = { bong: 'bowl', hookah: 'bowl', pipe: 'bowl', chillum: 'bowl', joint: 'tip' };
