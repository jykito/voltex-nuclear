# Voltex:Nuclear

An add-on for [Voltex](https://www.curseforge.com/minecraft/mc-mods/voltex): uranium, a centrifuge cascade, a fission
reactor you lay out cell by cell, spent fuel reprocessing, heavy water and a fusion torus.

One source tree, two game versions:

| Folder | What | Shared |
|---|---|---|
| `core` | Plain Java without Minecraft: simulations, formulas, numbers. JUnit tests | yes |
| `common` | Minecraft code that compiles unchanged on both versions. Differences go through `platform.Platform` | yes |
| `resources` | Textures, models, languages, guide chapters | yes |
| `data-src` | Recipes, tags, loot, advancements, written once; `tools/gen_data.js` writes both formats into `generated/` | yes |
| `forge-1.20.1` | Forge 1.20.1 (Java 17): only what differs on this version | no |
| `neoforge-1.21.1` | NeoForge 1.21.1 (Java 21): only what differs on this version | no |

The mod name, version and the Voltex range live in `mod.properties` and are read by both builds.

## Building

```
node tools/refresh_libs.js
node tools/gen_data.js
cd forge-1.20.1 && gradlew.bat build
cd neoforge-1.21.1 && gradlew.bat build
```

`refresh_libs.js` copies the Voltex jars the add-on compiles against into each `libs/` folder.
The jars land in `forge-1.20.1/build/libs` and `neoforge-1.21.1/build/libs`.

## Pack makers

`docs/PACKS.md` shows how KubeJS or Java code adds fuel rods, heat sinks, cooling cells and irradiation blanks to the
reactor.

## License

All Rights Reserved, see `LICENSE.txt`. The source is open to read; the mod itself is shared only through its official
pages.
