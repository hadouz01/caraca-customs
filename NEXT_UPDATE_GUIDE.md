# Next update guide: Exp. Share All and player-shared tiering

Step-by-step plan for the two mechanics in the `NEXT UPDATE` section of `CLAUDE.md`. Nothing here is implemented yet; every
file, class and method name below is a suggestion that follows the existing naming (`LevelCapHandler`, `SpawnTierTable`,
`Feature`, `DynamicConfig`...). Rename freely.

Target version: **1.9.4 → 1.11.0** (two minor additions, patch count reset).

---

## 0. How Cobblemon 1.8.4 hands out battle experience (read this first)

Everything happens in `PokemonBattle.end()` (`common/.../api/battles/model/PokemonBattle.kt`, lines ~273-320):

```
for each actor with fainted Pokémon
  for each opposing actor
    for each fainted Pokémon
      for each opposing BattlePokemon (alive ones, or all if awardExperienceToFaintedPokemon)
        facedFainted = opponentPokemon.facedOpponents.contains(faintedPokemon)
        multiplier = when {
            !facedFainted && holding EXPERIENCE_SHARE -> Cobblemon.config.experienceShareMultiplier (0.5)
            facedFainted -> 1.0
            else -> continue          <- benched Pokémon without Exp. Share stop here
        }
        experience = Cobblemon.experienceCalculator.calculate(opponentPokemon, faintedPokemon, multiplier)
        opponent.awardExperience(opponentPokemon, experience)   <- PlayerBattleActor -> addExperienceWithPlayer
        EVs = Cobblemon.evYieldCalculator.calculate(...)        <- also given to everyone who got this far
```

Key facts used by the plan:

- `Cobblemon.experienceCalculator` is a public `var` of type `ExperienceCalculator` (a `fun interface`). It can be
  **wrapped** from Kotlin without a mixin.
- `StandardExperienceCalculator` already applies the **Lucky Egg** of the Pokémon being calculated
  (`CobblemonItemTags.LUCKY_EGG`, `Cobblemon.config.luckyEggMultiplier`, 1.5 by default). That's the "held item boost".
- Cobblemon even has a `ToDo` right on that `when`: "when Exp. All is implemented ... this will have priority over the
  Exp. Share". So the only missing piece is letting benched Pokémon past the `continue`.
- The `ItemStack.is(TagKey)` call on that line is the **only** `is` call inside `end()`, which makes it a clean injection
  point.
- Each award still goes through `addExperience` → `EXPERIENCE_GAINED_EVENT_PRE`, so `LevelCapHandler` keeps trimming
  shared experience at the cap with zero changes.
- `awardExperience` already respects `allowExperienceFromPvP`.

---

## 1. Exp. Share All

### 1.1 Config (`config/config.kt`)

Add to `ConfigData` (with KDoc `@property` lines, like the others):

| Field | Type | Suggested default |
|---|---|---|
| `expShareAllEnabled` | `Boolean` | `true` |
| `expDivision` | `ExpDivision` | `ExpDivision.MULTIPLIED` |
| `expShareMethod` | `ExpShareMethod` | `ExpShareMethod.EQUALLY` |
| `expShareMultiplier` | `Float` | `0.5f` (same as Cobblemon's own Exp. Share) |

Create two small enums. They can live in a new file `progression/ExpShareAll.kt` (next to the handler) or in
`config/config.kt` beside the entries; I'd keep them in the config file since Gson needs them for `ConfigData`:

```kotlin
enum class ExpDivision { SHARED, MULTIPLIED }
enum class ExpShareMethod { EQUALLY, BALANCED }
```

Gson writes enums by name (`"MULTIPLIED"`), so the JSON stays readable. A typo in the file makes Gson put `null` in the
field even though it's non-null in Kotlin, so in `sanitizeAndValidateEntries()` add a null fallback:

```kotlin
if ((data.expDivision as ExpDivision?) == null) data.expDivision = ExpDivision.MULTIPLIED
```

(same for `expShareMethod`), and clamp the multiplier to two decimals:

```kotlin
data.expShareMultiplier = (data.expShareMultiplier.coerceIn(0.0f, 1.0f) * 100).roundToInt() / 100.0f
```

Add the toggle to `config/Feature.kt`:

```kotlin
EXP_SHARE_ALL("exp_share_all", { it.expShareAllEnabled }),
```

Sync needs nothing new: `ConfigSyncPayload` already sends the whole `ConfigData`.

### 1.2 The math (`progression/ExpShareAll.kt`)

Suggested `object ExpShareAll` holding the distribution rules, plus a `class ExpShareAllCalculator(private val delegate:
ExperienceCalculator) : ExperienceCalculator` that wraps Cobblemon's calculator.

Definitions, per fainted opponent:

- **members**: the `BattlePokemon`s of the player's actor that Cobblemon's loop would iterate
  (`battlePokemon.actor.pokemonList`, filtered by `health > 0` unless `Cobblemon.config.awardExperienceToFaintedPokemon`).
  `n = members.size`.
- **active**: members whose `facedOpponents` contains the fainted Pokémon. Usually one; two or more after switches or in
  doubles.
- **held boost** of a Pokémon: `Cobblemon.config.luckyEggMultiplier` if `heldItemNoCopy().is(CobblemonItemTags.LUCKY_EGG)`,
  else `1.0`. Put it in a `heldItemMultiplier(pokemon: Pokemon): Double` function so other items can be added later.
- **base**: what the active Pokémon would get, without its own held boost:
  `delegate.calculate(active, fainted, 1.0) / heldItemMultiplier(active)`. With several active Pokémon, use the highest
  one, so there's a single number to split per fainted opponent.

Then, for the Pokémon being calculated (`battlePokemon`), multiplying by **its own** held boost at the end:

| Division / method | Active Pokémon | Every other member |
|---|---|---|
| SHARED / EQUALLY | `base / n` | `base / n` |
| SHARED / BALANCED | `base * 0.5 / activeCount` | `base * 0.5 / (n - activeCount)` |
| MULTIPLIED | `delegate.calculate(...)` untouched | `base * expShareMultiplier` |

Check with the spec examples (100 exp, party of 4, no Lucky Eggs):
EQUALLY → 25 each; BALANCED → 50 and 16.67 each; MULTIPLIED at 0.8 → 100 and 80 each. When `n == activeCount`
(everyone fought, or a party of 1) BALANCED should fall back to the active Pokémon keeping the whole `base`.

`calculate` returns `Int`: use `roundToInt()` and `coerceAtLeast(1)` (Cobblemon's own formula always ends with `+ 1`, so
no Pokémon ever gets 0 from a win).

The wrapper should return `delegate.calculate(...)` untouched when:
- `Feature.EXP_SHARE_ALL.isEnabled` is false;
- the actor isn't a `PlayerBattleActor` (wild and NPC Pokémon keep vanilla rules);
- no active Pokémon is found (safety).

Install it once, without stacking wrappers (another mod may have its own calculator, so wrap whatever is there):

```kotlin
fun register() {
    ServerLifecycleEvents.SERVER_STARTING.register {
        val current = Cobblemon.experienceCalculator
        if (current !is ExpShareAllCalculator) Cobblemon.experienceCalculator = ExpShareAllCalculator(current)
    }
}
```

Call `ExpShareAll.register()` in `CaracaCustoms.onInitialize()` after `LevelCapHandler.register()`. Doing it on
`SERVER_STARTING` instead of `onInitialize` avoids depending on Fabric's init order between Cobblemon and us.

### 1.3 Letting benched Pokémon in (Java mixin)

New package `mixin/progression/`, new config `caraca_customs.progression.mixins.json` (copy the berry one, `required:
true`), registered in `fabric.mod.json` under `"mixins"`.

`ExpShareAllMixin`, targeting `PokemonBattle`:

- MixinExtras is bundled with Fabric Loader 0.19, no extra dependency.
- `@ModifyExpressionValue(method = "end", at = @At(value = "INVOKE", target =
  "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z", remap = true), remap = false)`
  (Cobblemon method → `remap = false` on the annotation, vanilla target → `remap = true` on the `@At`, following the
  project's remap rule).
- Capture the Pokémon with `@Local(name = "pokemon") Pokemon pokemon`. Kotlin keeps local variable names in bytecode and
  Cobblemon's `forEach` blocks are inlined, so `pokemon` is a real local of `end()`. If it ever fails to resolve, add
  `print = true` to the `@Local` once and read the locals table in the log.
- Return `original || ExpShareAll.appliesTo(pokemon)`, where `appliesTo` is a `@JvmStatic` on `ExpShareAll` checking the
  feature toggle and `pokemon.isPlayerOwned()` (same pattern as `Feature.berriesEnabled()`).

With that, every benched party member enters the Exp. Share branch, reaches the calculator, and the wrapper replaces
Cobblemon's 0.5 participation with the configured split. Consequences to be aware of:

- A held **Exp. Share** gives nothing extra while Exp. Share All is on (Exp. All has priority, as Cobblemon's own
  `ToDo` intends). See idea 2 below if you'd rather reward it.
- Benched members also receive **EVs** (and Power items work on them), like the Exp. Share in mainline games since
  gen 6. If you don't want that, see idea 5.
- Defeat-based evolution progress (Bisharp → Kingambit, etc.) still only counts for the Pokémon that actually faced the
  opponent, since that check uses `facedFainted` directly.

### 1.4 Mod Menu (`client/gui/ModMenuIntegration.kt`)

- Toggle on the General page next to the other `Feature` toggles: `startBooleanToggle`.
- The three detailed options in the Progression tab, inside a `SubCategoryBuilder` "Exp. Share All" (like the spawn group
  drawers):
  - `entryBuilder.startEnumSelector(Component, ExpDivision::class.java, data.expDivision)` with
    `.setEnumNameProvider { Component.translatable("option.caraca_customs.exp_division.${it.name.lowercase()}") }`;
  - same for `ExpShareMethod`;
  - the existing `fraction(...)` helper already does a 0-1 float field, reuse it for `expShareMultiplier`.

Cloth Config shows every field all the time; the tooltips should say which one applies to which division (method only
for SHARED, multiplier only for MULTIPLIED).

---

## 2. Player-shared tiering

### 2.1 Config

| Field | Type | Suggested default |
|---|---|---|
| `playerSharedTieringEnabled` | `Boolean` | `false` (singleplayer gains nothing from it) |
| `tierShareBlocklist` | `MutableList<String>` | `mutableListOf()` |

`Feature.SHARED_TIERING("shared_tiering", { it.playerSharedTieringEnabled })`.

Sanitize the list: trim names, drop blanks, `distinctBy { it.lowercase() }`. Minecraft names are case-insensitive, so
always compare in lowercase.

Mod Menu: toggle on the General page, list in the Progression tab with `entryBuilder.startStrList(Component, list)`
(`.setSaveConsumer { data.tierShareBlocklist = it.toMutableList() }`; make the field a `var`).

### 2.2 The world Pokédex (`progression/SharedPokedex.kt`)

Stored with vanilla `SavedData`, so it lives in the world folder (`world/data/caraca_customs_shared_pokedex.dat`), gets
saved with the world and is separate per world in singleplayer.

Store **species → set of player UUIDs who caught it**, not just a set of species. That one decision keeps the blocklist
simple: a blocklisted player's captures don't count for the others, and taking someone off the list instantly brings
their past captures back, without ever rebuilding anything.

```kotlin
class SharedPokedexData(val species: MutableMap<ResourceLocation, MutableSet<UUID>> = mutableMapOf()) : SavedData()
```

1.21.1 `SavedData` API:
- `override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag` — write a `ListTag` of compounds
  `{ "species": "cobblemon:bulbasaur", "players": [<int arrays>] }`. Use `NbtUtils.createUUID(uuid)` /
  `NbtUtils.loadUUID(tag)` for the UUIDs.
- a `companion object` with `fun load(tag: CompoundTag, registries: HolderLookup.Provider): SharedPokedexData` and
  `val FACTORY = SavedData.Factory(::SharedPokedexData, ::load, null)`.
- call `setDirty()` after every change.

Then `object SharedPokedex`:

- `register()`: on `ServerLifecycleEvents.SERVER_STARTED` grab
  `server.overworld().dataStorage.computeIfAbsent(SharedPokedexData.FACTORY, "caraca_customs_shared_pokedex")` and keep
  it (plus the server) in a private `var`; clear both on `SERVER_STOPPED`.
- `contribute(playerId: UUID, speciesId: ResourceLocation): Boolean` — adds the player to the species' set; returns
  `true` only when the species wasn't counted in the shared pool before (so the caller knows whether everyone's tier may
  have changed).
- `backfill(playerId: UUID): Boolean` — reads `Cobblemon.playerDataManager.getPokedexData(playerId).speciesRecords` and
  contributes every entry whose `getKnowledge() == PokedexEntryProgress.OWNED` (that's exactly what Cobblemon's
  `CaughtCount` counts). Needed for captures made before the feature existed or while it was off.
- `sharedCount(): Int` — species with at least one non-blocklisted contributor. Cache it in an `Int` and recompute only
  when a species is added or the blocklist changes; `PlayerTiers.infoFor` is called on **every** experience gain, so it
  must stay cheap.

Always record contributions, even with the feature off. Turning it on later then just works.

### 2.3 The blocklist (`progression/PlayerTiers.kt`)

Contributors are stored by UUID, the list is by name. Resolve names when the config loads (`/caraca reload`, Mod Menu
save, server start): `server.profileCache?.get(name)` returns an `Optional<GameProfile>` for anyone who ever joined the
server. Keep the result as `private var blockedIds: Set<UUID>` in `PlayerTiers` (or `SharedPokedex`), plus a
`fun isBlocked(playerId: UUID)` that also checks the online player's current name, so a name added before that player's
first join still works.

### 2.4 Hooking it into the tiers

`PlayerTiers`:

- `fun sharesTier(playerId: UUID): Boolean = Feature.SHARED_TIERING.isEnabled && !isBlocked(playerId)`.
- `fun progressSpecies(playerId: UUID): Int` — `max(sharedCount(), caughtSpecies(playerId))` when `sharesTier`, else
  `caughtSpecies(playerId)`. The `max` covers a player whose captures haven't been backfilled yet.
- Swap `caughtSpecies(...)` for `progressSpecies(...)` in `cachedTier`, `refreshTier` and `infoFor`.
- `TierInfo`: keep `caughtSpecies` as the count used for progression (so the level cap message's "missing species" is
  automatically right) and add `ownCaughtSpecies: Int` and `isShared: Boolean` for the `/caraca tier` output.

`LevelCapHandler`:

- `onPokedexChanged`: species id is `event.dataSource.pokemon.species.resourceIdentifier`. Call
  `SharedPokedex.contribute(...)` first. If it returned `true` and the catcher `sharesTier`, refresh the tier of **every
  online player that shares tiers** (not only the catcher) and send each one who went up a message. Otherwise keep the
  current single-player path.
- `JOIN`: `SharedPokedex.backfill(uuid)` before `PlayerTiers.refreshTier(uuid)`. If the backfill added species to the
  shared pool, refresh the other online players too.
- `refreshTiers(server)` (already called by `/caraca reload`): rebuild the blocked ids before refreshing.

Spawn cap and spawn levels read `PlayerTiers.cachedTier`, so they follow the shared tier with no changes.

### 2.5 Commands and messages

- `/caraca tier`: when the target shares tiers, add a line like "Shared species: 80 (yours: 40)". Blocklisted players
  could get a line saying they progress alone.
- New tier-up message for shared progress: `text.caraca_customs.tier.up_shared` — "%s registered a new species! Everyone
  reached tier %s of %s..." (the catcher's name as the first argument).
- Optional, ops only: `/caraca sharedex rebuild` — lists every `.dat` in `server.getWorldPath(LevelResource.PLAYER_DATA_DIR)`
  (file name = UUID) and backfills each, so offline players' old captures count without them rejoining.
  `getPokedexData` already works for offline UUIDs (the level cap uses it that way).

---

## 3. Translations (`en_us.json` and `pt_br.json`)

Every new option needs `option.caraca_customs.<key>` and `tooltip.caraca_customs.<key>`:

- `exp_share_all_enabled`, `exp_division`, `exp_division.shared`, `exp_division.multiplied`, `exp_share_method`,
  `exp_share_method.equally`, `exp_share_method.balanced`, `exp_share_multiplier`
- `player_shared_tiering_enabled`, `tier_share_blocklist`
- `category`/subcategory title for the Exp. Share All drawer
- `text.caraca_customs.tier.up_shared`, `text.caraca_customs.command.tier.shared`, `text.caraca_customs.command.tier.solo`
  (and the `sharedex` command keys if you add it)

Suggested pt_br names: "Exp. Share para todos", "Divisão de EXP" (Dividida / Multiplicada), "Método de divisão"
(Igualitário / Equilibrado), "Multiplicador de EXP", "Tiers compartilhados entre jogadores", "Jogadores fora do
compartilhamento".

---

## 4. Docs and version

- `gradle.properties`: `mod_version=1.11.0`.
- `CHANGELOG.md`: `## 1.11.0` → `### Additions` → `#### Pokémon experience` and `#### Player tiers`.
- `DESCRIPTION.md` + `README.md` Features: a short table for the three Exp. Share All combinations (the 100 exp / 4
  members example reads well) and a paragraph on shared tiers and the blocklist.
- `CLAUDE.md` technical notes: the calculator wrapper + `end()` mixin, and the species → UUIDs world Pokédex.

## 5. Testing checklist

`runServer` can't battle (no headless player), so this needs `runClient`, ideally with a second client (offline account)
for the shared tiers:

1. Party of 4, beat a wild Pokémon with each division/method; compare level bars against the table in 1.2.
2. Give the active Pokémon a Lucky Egg, then a benched one: only the holder gets the 1.5×.
3. A benched member at its level cap: stops at the cap, gets the cap message.
4. Exp. Share All off: Cobblemon's vanilla behaviour, held Exp. Share giving 50%.
5. Two players each catch different species; the sum crosses a threshold → both get the shared tier-up message.
6. Add one of them to the blocklist + `/caraca reload`: their tier falls back to their own count and the other's shared
   count drops by the species only they caught.
7. Restart the server: the shared Pokédex is still there.

---

## 6. Ideas to make the mechanics better

1. **Don't waste capped shares.** In SHARED mode, a member at its level cap throws its slice away. Leave capped
   members out of `n` (check `level >= PlayerTiers.infoFor(owner).levelCap` in the wrapper), so the rest of the party gets
   more.
2. **Reward a held Exp. Share.** While Exp. Share All is on, the item is useless. A benched holder could count as active
   (full exp in MULTIPLIED, active slice in BALANCED).
3. **Opt-in battle summary.** One chat line after the battle with the exp each member got, instead of a wall of level-up
   messages. Handy to understand SHARED vs MULTIPLIED.
4. **Fainted members.** Already handled by following `awardExperienceToFaintedPokemon` in the member count, but worth a
   line in the tooltip so people know where to change it.
5. **"Share EVs" toggle.** Some players won't want benched Pokémon picking up EVs; the mixin could skip the EV yield too,
   or the wrapper could be paired with a small `EvCalculator` wrapper (`Cobblemon.evYieldCalculator` is also a `var`)
   returning an empty map for non-active members.
6. **Anti-carry limit for shared tiers.** A "Max tiers above own" option (0 = unlimited): a brand new player in a late
   server wouldn't jump straight to tier 6 with level 100 spawns. Their tier becomes
   `min(sharedTier, ownTier + limit)`.
7. **Blocklist robust to name changes.** Store the resolved UUID next to each name the first time it resolves (or accept
   UUIDs in the list directly).
8. **Team-based sharing later.** The species → UUIDs storage already allows counting per group of players
   (scoreboard teams, or an FTB Teams compat) instead of the whole server.
9. **Credit in the message.** The shared tier-up message naming who caught the species makes the mechanic feel social;
   `/caraca sharedex` could also show the top contributors.
