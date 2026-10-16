# Sum1reM0d artwork — 16×16 redesign

Created with the built-in `image_gen` tool on 2026-10-16. Five assets were generated separately from new prompts, without reference images. The exact prompt set is in [prompts-16x16.json](prompts-16x16.json); original generated PNGs are preserved in `source/16x16-redesign/`.

All first-party game PNGs are native **16×16 RGBA**:

- `../src/main/resources/assets/sum1rem0d/textures/item/coin.png` — gold S coin.
- `../src/main/resources/assets/sum1rem0d/textures/item/coin_roll.png` — green paper bundle with a violet band, displayed as **Sum1re札↑たば↓**, worth 10 coins.
- `../src/main/resources/assets/sum1rem0d/textures/item/soap.png` — ivory soap with a violet wrapper.
- `../src/main/resources/assets/sum1rem0d/textures/item/mud_ball.png` — earthy mud ball.
- `../src/main/resources/assets/sum1rem0d/icon.png` — grass block and violet flower emblem.

`manifest-16x16.json` records the saved runtime paths and hashes. `preview/redesign-16x16.png` shows nearest-neighbor enlargements of the actual exported sprites. The `*-512.png` files are inspection previews, not game textures. Older source images/prompts remain as artwork history.

Run `./art/prepare-assets.ps1` to reproduce the exports. System.Drawing performs technical sprite preparation: trim transparent padding using `layout-16x16.json`, retain the aspect ratio inside a 14×14 region centered on the 16×16 canvas, sample with NearestNeighbor/SourceCopy/Half pixel offset, and normalize alpha to a binary native-item cutout mask at threshold 128. The original AI images are untouched. This script also produces 512px inspection enlargements.

The `coin_roll` item ID remains unchanged for save compatibility. Its model now has its own texture rather than sharing the coin sprite. Mod metadata and the Wiki/notification/preview renderers reference the new dimensions.
