# Sum1reM0d artwork

Created on 2026-10-16 using the built-in image generation tool, with no reference images.
The exact prompts are saved in [prompts.json](prompts.json).

- `source/soap-ai.png`: original generated transparent soap image (1254×1254).
- `source/mod-icon-ai.png`: original generated mod emblem (1254×1254).
- `../src/main/resources/assets/sum1rem0d/textures/item/soap.png`: game-ready 32×32 RGBA item sprite.
- `../src/main/resources/assets/sum1rem0d/icon.png`: game-ready 256×256 mod-list icon.
- `preview/soap-512.png`: nearest-neighbor enlargement of the actual 32×32 sprite for inspection.

The image generation tool produced the artwork. Technical size conversion uses
System.Drawing with SourceCopy compositing, NearestNeighbor interpolation and Half
pixel offset, retaining the soap's alpha channel. Run `./art/prepare-assets.ps1`
from PowerShell to regenerate the game assets from the saved original PNGs.

The soap previously used Minecraft's iron-ingot texture. Its model now references
the dedicated `sum1rem0d:item/soap` sprite. Fabric's icon, NeoForge 26.2+ iconFile,
and older NeoForge/Forge logoFile all point to `assets/sum1rem0d/icon.png` inside
the mod jar. The resource task selects the compatible metadata key per target.

## Coin asset

`coin-prompt.json` records the built-in image_gen prompt and asset paths. `source/coin-ai.png` is the untouched generated transparent PNG. `prepare-assets.ps1` produces the 32×32 coin texture and a nearest-neighbor 512px preview. The 1-coin item and 10-coin roll share this sprite; their names and wallet controls distinguish the denominations.
