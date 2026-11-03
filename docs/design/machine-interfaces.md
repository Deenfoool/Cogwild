# Machine interfaces — 0.11.0

All ten standalone machines have a compact 176x166 container in the approved gray metal/yellow-accent style. Inventory grids are standard 27+9 vanilla slots. Titles, status, energy and tooltips are localized to Russian/English. No start button. Right-clicking any occupied segment resolves the primary entity and opens the same inventory. Cable and shears interactions pass through.

| Machine | Material inputs | Product outputs |
|---|---:|---:|
| Lathe | 1 | 1 |
| Crusher | 1 | 1 |
| Rolling mill | 1 | 1 |
| Quenching bath | 2 | 1 |
| Air separator | 1 | 2 |
| Air compressor | 0 | 0 |
| Industrial press | 1 | 1 |
| Fermenter | 3 | 2 |
| Universal mixer | 3 | 1 |
| Diesel generator | 0 | 0 |

Material machines accept items into input slots and expose actual stored products in protected output slots. Compressor/generator panels expose their existing energy buffer only; no fictional pressure, fuel, tank or production readings. Their fuel/pneumatic behavior is separate unfinished work. Other machines without registered processing recipes show Waiting for input / No recipe, never simulated progress. The existing coke/blast furnace interfaces are retained.

## Initial automatic recipes

- Crusher: 1 cobblestone -> 1 gravel, 100 ticks.
- Crusher: 1 gravel -> 1 sand, 100 ticks.
- Crusher: 1 limestone -> 4 limestone flux, 100 ticks (existing manual recipe remains).
- Air separator: 1 gravel -> 1 flint + 1 sand, 140 ticks.
- Each recipe costs 8 FE per processing tick. Capacity remains 4000 FE. No standby drain; power/blocked/no-recipe states do not consume energy. Redstone can still trigger legacy visual power states, but never supplies processing energy.

Recipes are reloadable/synchronized `suitcaseglider:machine_processing` recipes with `machine`, ordered `ingredients` (one item per used slot), `results` (one or two item stacks), `duration` and `energy`. Outputs are preflighted and committed together on the server. Input remains in its slot while processing; removing it cancels progress, as in a furnace. Matching input additions preserve progress. Saved recipe identity/progress and all five storage slots survive chunk/world reload. Unknown/no-longer-matching recipes reset progress without consuming items. Output-full and power outages pause a matching job.

The root entity exposes ITEM_HANDLER: insert inputs, extract outputs; GUI players may also retrieve inputs. Machine inventories drop exactly once when a root or secondary segment is broken. Legacy saves without an Inventory tag retain a five-slot handler and existing energy. Menu validity checks dimension, current block entity and distance. Shift-click does not write to output slots.

## Assets and validation

`tools/export_machine_gui.py` deterministically draws native pixel textures; `MachineUiSpec` supplies the matching Java slot positions. Existing machinery models, item icons and previous rendering optimizations are preserved.

`./gradlew build runServer -PmachineGuiSmoke --offline` checks all machine placements plus storage, transfer, protected outputs, energy accounting, pausing, two-output atomicity, NBT, old-save compatibility and secondary break drops. `./gradlew runClient -PmachineGuiQa --offline` opens all ten screens via secondary-segment use in a separate test world, checks layout/data, and captures each screen. Both profiles are opt-in and do not run in a normal game.

## Animated mechanism icons — 0.11.1

Crusher GUI rollers counter-rotate; the air separator fan rotates. Each uses a 24-frame native 32x32 atlas. The screen renders one extra quad, with a reusable tick-based clock and partial-tick interpolation. Atlas geometry is prepared offline by the existing GUI exporter. No model rendering, runtime pixel generation or texture uploads are added.

The clock advances only when the synchronized processing state is RUNNING. Empty input, unsupported recipes, missing FE and blocked outputs freeze the last displayed frame; normal processing resumes the clock. Server processing and recipes are unchanged. The client GUI walkthrough captures two motion phases and explicitly tests no-power freeze, blocked-output freeze and resumed motion for both machines. Unit tests exercise all frames, wrapping, idle and paused clocks.
