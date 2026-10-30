// Writes the add-on data for both game versions from one source, data-src/ (1.21.1 layout).
// Output: generated/neoforge-1.21.1/data/... as is, generated/forge-1.20.1/data/... converted.
// The folders forge/ and neoforge/ inside a namespace go to their own version only.
// Never deletes: files left over from earlier runs are listed so they can be removed by hand.
// Run: node tools/gen_data.js
const fs = require('fs'), path = require('path');

const ROOT = path.join(__dirname, '..');
const SRC = path.join(ROOT, 'data-src');
const OUT = { neo: path.join(ROOT, 'generated', 'neoforge-1.21.1', 'data'), forge: path.join(ROOT, 'generated', 'forge-1.20.1', 'data') };

// 1.21.1 folder -> 1.20.1 folder (first matching prefix of the path inside a namespace)
const FOLDERS_120 = [
  ['recipe/', 'recipes/'], ['loot_table/', 'loot_tables/'], ['advancement/', 'advancements/'],
  ['tags/block/', 'tags/blocks/'], ['tags/item/', 'tags/items/'], ['tags/fluid/', 'tags/fluids/'],
  ['worldgen/', 'worldgen/'], ['forge/', 'forge/'], ['neoforge/', 'neoforge/'],
];

const walk = d => fs.existsSync(d) ? fs.readdirSync(d, { withFileTypes: true })
  .flatMap(e => e.isDirectory() ? walk(path.join(d, e.name)) : [path.join(d, e.name)]) : [];

/**
 * A tool predicate of a loot condition in its 1.20.1 form. On 1.21.1 enchantments sit under item sub-predicates
 * ({"predicates": {"minecraft:enchantments": [{"enchantments": id, "levels": ...}]}}), on 1.20.1 directly
 * ({"enchantments": [{"enchantment": id, "levels": ...}]}). Anything else in "predicates" stops the run instead of
 * being dropped quietly.
 */
function toolPredicate120(pred) {
  if (!pred || !pred.predicates) return pred;
  const { predicates, ...rest } = pred;
  const keys = Object.keys(predicates);
  if (keys.some(k => k !== 'minecraft:enchantments')) throw new Error('no 1.20.1 form for tool predicates: ' + keys.join(', '));
  return { ...rest, enchantments: predicates['minecraft:enchantments'].map(({ enchantments, ...e }) => ({ enchantment: enchantments, ...e })) };
}
function lootTable120(node) {
  if (Array.isArray(node)) return node.map(lootTable120);
  if (!node || typeof node !== 'object') return node;
  const out = {};
  for (const [k, v] of Object.entries(node)) out[k] = lootTable120(v);
  if (out.condition === 'minecraft:match_tool' && out.predicate) out.predicate = toolPredicate120(out.predicate);
  return out;
}

/**
 * 1.20.1 form of one json file: common tags in forge:, recipe results as {"item"} (every "result", "result2", ... of
 * the machines with more outputs too), loot tool predicates the old way, an advancement's icon as {"item"}.
 */
function to120(kind, json) {
  let text = JSON.stringify(json, null, 2)
    .replace(/"#c:/g, '"#forge:')
    .replace(/"tag": "c:/g, '"tag": "forge:');
  let out = JSON.parse(text);
  if (kind === 'recipe') for (const key of Object.keys(out)) {
    const r = out[key];
    if (!/^result\d*$/.test(key) || !r || typeof r !== 'object' || !r.id) continue;
    const { id, ...rest } = r;
    out[key] = { item: id, ...rest };
  }
  if (kind === 'loot_table') out = lootTable120(out);
  if (kind === 'advancement' && out.display && out.display.icon && out.display.icon.id) {
    const { id, ...rest } = out.display.icon;
    out.display.icon = { item: id, ...rest };
  }
  return out;
}

const written = { neo: new Set(), forge: new Set() };
let count = 0;
for (const file of walk(SRC)) {
  const rel = path.relative(SRC, file).split(path.sep).join('/');
  if (rel === 'README.md') continue;
  const [ns, ...rest] = rel.split('/');
  const inner = rest.join('/');
  const rule = FOLDERS_120.find(([from]) => inner.startsWith(from));
  if (!ns || !rule) throw new Error('unknown data folder: ' + rel);
  const kind = rule[0].replace(/\/$/, '').split('/').pop();

  const neoPath = path.join(OUT.neo, ns, inner);
  // c: tag files themselves move to the forge namespace on 1.20.1
  const forgeNs = ns === 'c' ? 'forge' : ns;
  const forgePath = path.join(OUT.forge, forgeNs, rule[1] + inner.slice(rule[0].length));

  const isJson = file.endsWith('.json');
  const json = isJson ? JSON.parse(fs.readFileSync(file, 'utf8')) : null;
  // the loader folders (biome modifiers and the like) belong to one version only
  const targets = [
    ['neo', neoPath, isJson ? JSON.stringify(json, null, 2) + '\n' : fs.readFileSync(file)],
    ['forge', forgePath, isJson ? JSON.stringify(to120(kind, json), null, 2) + '\n' : fs.readFileSync(file)],
  ].filter(([key]) => !(inner.startsWith('forge/') && key === 'neo') && !(inner.startsWith('neoforge/') && key === 'forge'));
  for (const [key, target, body] of targets) {
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, body);
    written[key].add(path.resolve(target));
  }
  count++;
}

for (const key of ['neo', 'forge']) {
  const stale = walk(OUT[key]).filter(f => !written[key].has(path.resolve(f)));
  if (stale.length) console.log(`stale in ${key} (not in data-src, remove by hand):\n  ` + stale.join('\n  '));
}
console.log(`data files: ${count} -> generated/neoforge-1.21.1 and generated/forge-1.20.1`);
