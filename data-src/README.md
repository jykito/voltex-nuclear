# data-src

Recipes, tags, loot tables and advancements of the add-on, written once in the 1.21.1 layout:

```
data-src/<namespace>/recipe/...        data-src/<namespace>/tags/block/...
data-src/<namespace>/loot_table/...    data-src/<namespace>/tags/item/...
data-src/<namespace>/advancement/...   data-src/<namespace>/tags/fluid/...
```

Common tags use the `c:` namespace and recipe results use `{"id": ..., "count": ...}`.
`node tools/gen_data.js` writes `generated/neoforge-1.21.1` as is and `generated/forge-1.20.1` converted:
plural folders (`recipes`, `loot_tables`, `tags/blocks`, ...), `c:` tags as `forge:`, results as `{"item": ...}`,
and the enchantment check of a `match_tool` loot condition in its 1.20.1 form (silk touch on ores).
The loader folders inside a namespace go to their own version only: `forge/` (Forge biome modifiers) to 1.20.1,
`neoforge/` to 1.21.1.
