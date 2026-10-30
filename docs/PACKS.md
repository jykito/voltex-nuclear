# Voltex:Nuclear for pack makers

A pack can give the fission reactor parts of its own: fuel rods, heat sinks, cooling cells and irradiation channel
blanks. The reactor, its window, the lid, the schemes in JEI and the irradiation channel category take them as they
take the add-on's own. The add-on's recipes are ordinary data (Voltex machine recipes and vanilla ones), so KubeJS
changes them through the Voltex recipe schemas; the add-on has no recipe types of its own.

## KubeJS (startup script)

The items come from the pack (KubeJS or another mod); the event binds them to reactor parts. Same script on 1.20.1
(KubeJS 6) and 1.21.1 (KubeJS 7).

```js
NuclearEvents.reactor(event => {
  // strength: neutron source of one rod (add-on: low enriched 1, high enriched 2, MOX 1.5)
  // heat: heat factor (1, 1.4, 1.2); rods: 1, 2 or 4; life: ticks at full power (72000, an hour)
  // cap: the lid's glowing cap, 'low_enriched', 'high_enriched' or 'mox'
  // spent: what a burnt-out rod leaves (default: the add-on's spent rod of its size)
  event.fuelRod('kubejs:thorium_rod', {strength: 1.2, heat: 0.8, rods: 1, life: 108000, cap: 'mox', spent: 'kubejs:spent_thorium_rod'})

  // rate: heat a tick it takes from the fuel next to it (the Voltex heat vent 80)
  event.heatSink('kubejs:copper_vent', {rate: 120})

  // takes up to rate heat a tick while the input ports give it its fluid, 1 mB per heatPerMb heat;
  // with an output it pours as much of it into the output ports and works only while it fits,
  // without one the fluid boils away (add-on: water cell 150, water, 20; cryo cell 600, coolant, 600)
  event.cell('kubejs:nitrogen_cell', {rate: 400, fluid: 'kubejs:liquid_nitrogen', heatPerMb: 100, output: 'kubejs:nitrogen_gas'})

  // becomes product after this much irradiation (lithium blank 6000, about 30 s in a good channel)
  event.channelBlank('kubejs:cobalt_blank', {product: 'kubejs:cobalt_60', work: 120000})
})
```

Defaults: `heat` 1, `rods` 1, `life` 72000, `cap` 'low_enriched'; `strength`, `rate`, `fluid`, `heatPerMb`,
`product` and `work` are needed. A wrong value stops the script at startup with the item named.

## Java

`com.jykito.voltexnuclear.api.ReactorPartRegistry`: `fuelRod`, `heatSink`, `cell`, `channelBlank`, the same numbers.
Call it from the mod's constructor or common setup, before a world loads.

## Rules

- Register on both sides, in the same order: the client learns a cell's part by its place in the list.
- An item that does not exist, or is a reactor part already, is left out with a line in the log.
- A cell that gives its fluid back counts as a cryo cell on the lid and in the window, one that does not as a water cell.
