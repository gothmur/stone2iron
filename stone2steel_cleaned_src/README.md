# stone2steel (NeoForge 1.21.x)

Early-game overhaul: fire-making, heated/fractured rock workflow, primitive tools, and basic materials.

## Features
- **Fire-making** (bow drill / fireboard / kindling / tinder) → produces **Ember** (consumes Tinder).
- **Heating**: lit campfire gradually heats adjacent stone / copper ore (3 stages), with clear overlays.
- **Cooling**: once the campfire is gone, heated blocks cool down over time and revert to vanilla.
- **Quenching**: use **Bark Container (Full)** on a lit campfire to crack nearby heated blocks into **Fractured Rock** (stage ≥ 3).
- **Granite Maul**: fast on *Fractured Rock* (~3 swings), slow on raw stone/ore (~15 swings). Craft: stick + granite boulder + rope.
- **Granite Boulder**: rare gravel drop (global loot modifier), or via crafting where applicable.
- **Flint tools**: knife, axe, shovel; flint shovel has improved chance to get flint from gravel.

## Data pack (resources) layout
- Recipes live under `data/**/recipes/` (renamed from `recipe/`).
- Loot tables under `data/**/loot_tables/` (renamed from `loot_table/`).
- Tags under `data/**/tags/items/` (renamed from `tags/item/`).
- Global loot modifiers are listed in **one** place: `data/neoforge/loot_modifiers/global_loot_modifiers.json`.

## Notes
- The creative tab no longer shows internal debug block items (heated/fractured); they are still registered but hidden from the tab.
- If you want to tweak timings, see `CampfireHeatingHandler` constants.
- If you want to gate vanilla progression:
  - `data/minecraft/recipes/crafting_table.json` requires an axe in the recipe.
  - `data/minecraft/loot_tables/blocks/oak_log.json` requires the `minecraft:axes` tag to drop logs.
  Adjust/remove if not desired.

## Build
```powershell
# Windows PowerShell
.\gradlew clean build
.\gradlew runClient
```
The built JAR is in `build/libs/` (the one **without** `-sources`).
