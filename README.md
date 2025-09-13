# Tutorials

All file paths below that don’t start with a `/` are relative to `src/main/java/org/nguh/nguhcraft` unless otherwise stated.

## How to add a block family
E.g. a new stone or wood type. 

### In `block/NguhBlocks.kt`
1. For each block of the family, perform the following steps; these are a small subset of the steps that are used in ‘how to add a block’ below; that is, **ONLY** perform these steps:
   1. step 1 of the section ‘How to add a block: In `block/NguhBlocks.kt`’ below;
   2. add a translation key (in `/src/main/resources/assets/nguhcraft/lang/en_us.json`; see below);
   3. add a texture for the block (to `/src/main/resources/assets/nguhcraft/textures/block`; see below).
2. Create a `BlockFamily` that contains all the new blocks that are part of a family.
3. Add the family to either `STONE_VARIANT_FAMILIES` or `WOOD_VARIANT_FAMILIES`.
4. If you’re adding a wood family, also add it to the `VANILLA_AND_NGUHCRAFT_EXTENDED_WOOD_FAMILIES` array if your family provides all the blocks required by it (that is, planks, log, wood, stripped log, stripped wood).

## How to add a block
 This assumes that the block does not require completely custom behaviour but is instead similar to an existing vanilla (or Nguhcraft) block. 

### In `block/NguhBlocks.kt`:
1. Add a declaration for the block:
   1. First, take a look at how other blocks are declared to get a basic idea as to what to do.
   2. Call the appropriate registration function; for most blocks, that can just be `Register()`, but there are other `RegisterXY()` functions for certain kinds of blocks; use those instead if there is one that works for you (e.g. use `RegisterVSlab()` for vertical slabs).
   3. The first argument should be a string that is not used for anything else in nguhcraft (it’s *not* an issue if it conflicts with an existing block in vanilla or another mod).
   4. For second argument, use whatever constructor the block you’re basing it on uses. Usually, that’ll be `::Block`.
   5. The third argument should be the block settings, copy those of a block that is similar to yours by calling `AbstractBlock.Settings.copy()`. If the block is transparent or has gaps, make sure to add `.nonOpaque()` here as well. 
2. If the block should drop itself when mined, add it to `DROPS_SELF`.
3. If the block can be mined by a pickaxe, add it to `PICKAXE_MINEABLE`.
4. In `Init()`, add the block to the appropriate item group.

### In `block/NguhBlockModels.kt`:
1. Add the block to `BootstrapModels()`; what function needs to be called for this depends on the block; look at the other blocks in that function or at how vanilla minecraft creates its models to figure that out.
2. If the block is transparent or has gaps, add it to the appropriate render layer in `InitRenderLayers()`; as with the models, which render layer is the correct one can be inferred from the other blocks there or from vanilla code. 

### In `data/NguhcraftDataGenerator.kt`:
1. If the block requires a tool other than a pickaxe add it to the corresponding block tag in `NguhcraftBlockTagProvider`.
2. If your block is a double block or if its dropping behaviour is a bit more complicated (e.g. when mining a door block, you don’t want it to drop 2 doors even though there are 2 blocks, viz. one for each half of the door), create a loot table for it in `NguhcraftLootTableProvider` and **DO NOT** add it to the `DROPS_SELF` array mentioned above.

### In `data/NguhcraftRecipeGenerator.kt`:
1. Add a crafting recipe for your block; look at the recipes for the other blocks in this file to figure out how they work.
2. If your block is a stone or wood block, also consider adding a stonecutting recipe for it.

### In `/src/main/resources/assets/nguhcraft/lang/en_us.json`:
1. Add a translation key for the block.
2. If the British spelling is different, also add one to `en_gb.json` in the same directory.

### In `/src/main/resources/assets/nguhcraft/textures/block`
1. Add a texture for your block in this directory; the name of the file should be `block_name.png`, where`block_name` is the string you passed to `Register()` when you first declared the block; note that the file name is case-sensitive; in particular, the `.png` **MUST** be lowercase.
