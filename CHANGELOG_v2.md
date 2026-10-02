#### Configuration
- Every mechanic of the mod can now be **turned on or off** from the top of the main config page, without restarting:
  - **Custom Poké Balls:** when off, the custom balls can't be crafted or found in chests anymore, Cobblemon's Poké, Premier, Azure, Citrine, Roseate, Slate and Verdant ball recipes (and their Ancient versions) craft Cobblemon's own balls again, the creative tab shows Cobblemon's balls, and `pokeball=` in commands works like in Cobblemon. Balls the player already has keep working.
  - **Berry Mechanics:** when off, berries go back to Cobblemon's own growth times and yields, the Hoe Fortune bonus stops, and mulch is no longer limited to 4 harvests.
  - **Level Cap** and **Spawn Cap;**
  - **Pokémon Follow Speed;**
- Turning the custom Poké Balls on or off reloads the datapacks automatically, so recipes change right after saving in the config screen (in singleplayer) or after `/caraca reload`.
- **Spawnable Species Surplus** option in the Spawn Groups tab, from 0 to 2 (25% by default). It's how many extra species a tier must offer beyond what the next tier needs, so players don't have to catch every single species around to move on.
-  **Early Tier Spawn Rate** option for each spawn group, from 0.01 to 1. It's the share of the max spawn rate the group gets in the tier right before its starting tier (25% by default), and also the rate used when it's unlocked early to fill a tier. Each earlier tier still gets half of the next one.
- New **Spawn Groups** tab in the Mod Menu config screen, with a toggle for the whole spawn cap and a section for each group with:
  - **Starting Tier:** the tier where the group spawns at its full rate.
  - **Max Spawn Rate Multiplier:** from 0.01 (1% of the normal rate) to 100 (100 times the normal rate).
  - **Early Tiers with Reduced Rate:** from 0 to 3. The group shows up that many tiers earlier, with 25% of its rate in the tier right before, then 12.5% and 6.25%.
  - **Reduce Spawn Rate Until Tier** and **Max Reduced Spawn Rate:** make the group get rarer in equal steps after its starting tier, until it reaches the reduced rate.
- All of them are synced to players on servers and can be reloaded with `/caraca reload`.

#### Player tiers
- New progression system based on how many **different species** the player has caught. Every species registered as caught in their Pokédex counts towards their tier.
- By default there are **6 tiers**, and the player reaches the last one at **512 species**. The other tiers are spread evenly: 0, 102, 204, 306, 408 and 512 species.
- `/caraca tier` shows the player current tier, how many species they've caught, their level cap and what the next tier needs. Operators can also check other players with `/caraca tier <player>`.
- The player gets a message in chat every time they reach a new tier.

#### Pokémon spawning
- New **tier spawn cap**: the player'd tier now also decides which Pokémon spawn naturally around them, fishing included. Stronger Pokémon only start showing up once they've caught enough different species.
- Species are split into **16 groups** by evolutionary stage and potential: low, medium and high potential for each stage, lowest potential for stages 2 and 3 (early level evolutions), three starter groups, regular legendaries and mythical legendaries.
- Natural Pokémon now **spawn at levels that follow the player's tier**. The highest level a Pokémon can spawn at around you is that tier's level cap.
- The lowest level is half the level cap, minus 3 if the cap is below 50, minus 5 if it's below 80, and minus 10 from 80 onward.
- Most Pokémon spawn in an **average range** inside that window, from 40% to 80% of the way between the lowest level and the level cap. Every level in it is equally common, and it gets at least 65% of the spawns.
- Levels outside the average range get rarer the further they are from it, so Pokémon at the lowest level or at the level cap are very rare. Levels below the range fade out faster than the ones above it, so spawns lean slightly towards the higher levels. For example, within levels 1 to 20, most Pokémon spawn between 9 and 16, levels 17 and 18 are a bit rare, 19 is rarer and 20 is as rare as level 1.
- With the default 6 tiers:
  - Tier 1 spawns levels 5 to 16, mostly 9 to 14.
  - Tier 2 spawns levels 13 to 32, mostly 21 to 28.
  - Tier 3 spawns levels 21 to 48, mostly 32 to 43.
  - Tier 4 spawns levels 27 to 64, mostly 42 to 57.
  - Tier 5 spawns levels 30 to 80, mostly 50 to 70.
  - Tier 6 spawns levels 40 to 100, mostly 64 to 88.
- This replaces Cobblemon's own spawn levels for wild Pokémon around players, fishing and herds included.

#### Pokémon behavior
- Pokémon now have a **level cap** that depends on their owner's tier. With the default settings, the caps are 16, 32, 48, 64, 80 and 100.
- The caps follow Cobblemon's max level, so they adjust on their own if a server changes it. Levels are split evenly between tiers and whatever is left over from the division goes to the last tier (with 6 tiers, the jump from tier 5 to 6 is 20 levels instead of 16).
- A Pokémon at the cap stops gaining experience from battles and candies, and the player gets a message when it gets there. Candies aren't used up on a capped Pokémon.
- Operators can still level Pokémon past the cap with commands like `/levelup` or `/pokeedit`.
- A player's sent out Pokémon now **keeps up with them** instead of falling behind and teleporting. They follow at a distance that fits their size, and walk faster the further behind they are:
  - Small Pokémon (under 1 m) and medium Pokémon (1 m to 2.5 m) stay between 2 and 5 blocks away.
  - Big Pokémon (2.5 m to 5 m) stay between 3 and 6 blocks away.
  - Extra big Pokémon (over 5 m) stay between 5 and 9 blocks away.
- Only the player's party Pokémon get this. Wild Pokémon, even of the same species, keep their normal speed, and Pokémon in battle, being ridden or in a pasture are left alone.
- If a Pokémon still gets stuck far away, Cobblemon's own teleport still brings it back.

#### Pokéballs
- The mod's Poké Balls can now be used in Pokémon properties, like `/givepokemon pichu pokeball=lavender_ball` or `/pokeedit 1 pokeball=lavender_ball`, and they show up in the tab completion. Before, Cobblemon only looked for its own balls when no namespace was typed, so the custom ones never showed up.
- `pokeball=poke_ball` (and the other names shared with Cobblemon, like `premier_ball` or `azure_ball`) now gives the Caraca Customs version. Cobblemon's original is still available by typing its full id, like `pokeball=cobblemon:poke_ball`, which is also how it's listed in the tab completion.

- The **Ancient Caraca Ball** recipe wasn't loading at all. It can be crafted now.
- The **Caraca Ball** recipe had two bread slots that couldn't be filled. It can be crafted now.
- The **Ultra Verdant Ball** was missing its thrown ball appearance.
- The recipe book now groups the Poké Ball recipes instead of showing one entry per ball. The regular tier balls share one entry, like Cobblemon does with its own basic balls, and their Ancient versions share another (also like Cobblemon). The Ultra balls share one entry and the Ancient Ultra balls another. Caraca Balls keep their own entries.
- Every custom ball recipe now sits in the recipe book's equipment tab, next to Cobblemon's balls, instead of the miscellaneous tab.

#### Commands
- `/caraca` is no longer operator-only as a whole. `/caraca reload` still is, but any player can use `/caraca tier` to check their own progress.
