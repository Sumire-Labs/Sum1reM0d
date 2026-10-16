# Sum1reM0d artwork — 16×16 redesign

Created with the built-in `image_gen` tool on 2026-10-16. Five assets were generated separately from new prompts, without reference images. The exact prompt set is in [prompts-16x16.json](prompts-16x16.json); original generated PNGs are preserved in `source/16x16-redesign/`.

The five original redesign assets below are native **16×16 RGBA**:

- `../src/main/resources/assets/sum1rem0d/textures/item/coin.png` — gold S coin.
- `../src/main/resources/assets/sum1rem0d/textures/item/coin_roll.png` — green paper bundle with a violet band, displayed as **Sum1re札↑たば↓**, worth 10 coins.
- `../src/main/resources/assets/sum1rem0d/textures/item/soap.png` — ivory soap with a violet wrapper.
- `../src/main/resources/assets/sum1rem0d/textures/item/mud_ball.png` — earthy mud ball.
- `../src/main/resources/assets/sum1rem0d/icon.png` — grass block and violet flower emblem.

`manifest-16x16.json` records the saved runtime paths and hashes. `preview/redesign-16x16.png` shows nearest-neighbor enlargements of the actual exported sprites. The `*-512.png` files are inspection previews, not game textures. Older source images/prompts remain as artwork history.

Run `./art/prepare-assets.ps1` to reproduce the exports. System.Drawing performs technical sprite preparation: trim transparent padding using `layout-16x16.json`, retain the aspect ratio inside a 14×14 region centered on the 16×16 canvas, sample with NearestNeighbor/SourceCopy/Half pixel offset, and normalize alpha to a binary native-item cutout mask at threshold 128. The original AI images are untouched. This script also produces 512px inspection enlargements.

The `coin_roll` item ID remains unchanged for save compatibility. Its model now has its own texture rather than sharing the coin sprite. Mod metadata and the Wiki/notification/preview renderers reference the new dimensions.

## Dedicated textures and vanilla-shaped equipment (2026-10-16)

The user's final direction is to preserve the vanilla equipment silhouette, pattern and shading positions, with material-specific colors. The earlier independently shaped equipment drafts are not used.

The built-in `image_gen` tool produced individual material palette edits from vanilla iron/netherite item references. `prepare-equipment.ps1` projects those palettes onto the unchanged native pixel layout, preserving the exact alpha mask and brown wooden pixels. This avoids blur, resampling noise and invented outlines. The final icons are dedicated PNGs, not runtime color conversions. Worn armor retains its native 64×32 / 64×64 / 128×128 UV sizes; the held spear retains its native 32×32 texture. All GUI item sprites are 16×16.

The tamed book, ingots, smithing template, piston base, mixed-material source tiles and wiring use new GPT artwork. `prepare-dedicated.ps1` performs technical export: transparent-padding trim for item sprites, full-canvas tile sampling, nearest-neighbor scaling, binary alpha and deterministic non-dithered palette normalization. Dyeable tiles use neutral grayscale. Mixed-color functionality remains intact.

Sources: `source/vanilla-color-2026/` and `source/dedicated-2026/`. Exact final prompts: `prompts-dedicated-2026.json`. Export manifest: `manifest-vanilla-color-2026.json`. Actual exported equipment preview: `preview/vanilla-colors.png`.

Run `prepare-equipment.ps1`, `prepare-dedicated.ps1`, and `preview-equipment.ps1` from a fresh PowerShell process to reproduce exports. Equipment export reads the local Minecraft client JAR; pass `-MinecraftJar` if its cache path differs. Geometry and texture patterns for the vanilla-shaped equipment derive from the Minecraft assets; their new material palettes were produced with the built-in image tool. Original Minecraft reference files are development-only inputs and are not bundled as artwork sources.
