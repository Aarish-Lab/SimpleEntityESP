# ESP Test (Fabric, Minecraft 26.1.2)

Client-side entity ESP for testing anti-ESP / entity-hiding plugins on a server **you control**.
It outlines entities through walls using vanilla's own glow-outline pass. It sends nothing to the
server, so the server cannot see that it is running; use it to check what your plugin still lets
through (entities it should have hidden or culled).

| | |
|---|---|
| Minecraft | 26.1.2 (the manifest accepts any 26.1.x) |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.155.2+26.1.2 (newer 26.1.x builds are fine) |
| Mod Menu | 18.0.2 (latest for 26.1.x), optional |
| Java | 25 |

## Get the .jar (GitHub builds it for you, no local Gradle needed)

1. Create a new repository on GitHub (any name, private is fine).
2. Unzip this project. On the empty repo page click **uploading an existing file**, drag in
   **everything inside the `esptest` folder** (including the `.github` folder) and click **Commit changes**.
   * If `.github` does not upload: **Add file -> Create new file**, type `.github/workflows/build.yml`
     as the name and paste the contents of that file.
3. Open the **Actions** tab. The **Build** workflow starts by itself (or press **Run workflow**).
   The first run takes a few minutes because Minecraft, Fabric API and Mod Menu are downloaded.
4. When the run shows a green check, open it, scroll to **Artifacts** and download
   `ESPTest-1.1.0+mc26.1.2.jar`. GitHub wraps it in a zip, so unzip it to get the real `.jar`.
5. Put that `.jar` and Fabric API (and Mod Menu, if you want the Mods-list button) into `%appdata%\.minecraft\mods`.
6. A red X means the build failed (for example a changed API name or import). Open the failed step,
   copy the log and send it over.

Local build instead: JDK 25 + Gradle 9.4.1, then run `gradle build` in this folder.
The jar ends up in `build/libs/ESPTest-1.1.0+mc26.1.2.jar` (ignore the `-sources` jar).

## Opening the menu

* **Hotkey:** default **O**. Change it in **Options > Controls > Key Binds > ESP Test > Open ESP Menu**.
* **Mod Menu:** Mods list > ESP Test > config button.

The menu has on/off switches for ESP, each entity group (players, hostile, passive, items, other),
"show myself", invisible entities and "vanilla mobs only", the colour mode, the range (`-` / `+` change it
by 16, the middle button steps through presets), and a **Colors...** screen where each colour button cycles
through the 16 chat colours. Changes apply instantly and are saved when you leave the menu.

## Other controls

| | |
|---|---|
| **G** | toggle ESP (also rebindable in Key Binds) |
| `/esp` | show status |
| `/esp toggle` / `/esp reload` | master switch / re-read `config/esptest.json` |
| `/esp range <blocks>` | max distance (1-1024) |
| `/esp mode category` | colour by entity category |
| `/esp mode visibility` | green = line of sight, red = something blocks the view (leak check) |
| `/esp players\|hostile\|passive\|items\|other [true\|false]` | category filters |
| `/esp self\|invisible\|vanillaonly [true\|false]` | extra filters |

Defaults: players + hostile mobs on, range 128, invisible players included, vanilla entities only.
All values live in `config/esptest.json`; colours can be any `RRGGBB` hex there (the menu shows
non-palette colours as `#RRGGBB`).

## How it works

Two small `@Inject` mixins, nothing is overwritten and no terrain/chunk code is touched:

* `Minecraft#shouldEntityAppearGlowing` (HEAD): returns `true` for ESP targets, so vanilla draws the outline.
* `EntityRenderer#extractRenderState` (TAIL): swaps the outline colour in `EntityRenderState.outlineColor`.

`defaultRequire` is 1 in the mixin config, so if a Minecraft update renames either target the game
fails loudly at startup instead of silently showing nothing.

## Compatibility

* **Sodium / Iris / Lithium etc.:** no chunk, terrain or shader code is replaced; the outline is vanilla's.
* **Mod Menu:** optional. The `modmenu` entrypoint is only loaded by Mod Menu itself; without it the mod
  still works and the menu opens with the hotkey.
* **Other mods:** only `@Inject` is used, so other mods can hook the same methods.
* **Modded entities:** skipped by default (`vanillaEntitiesOnly`). Some modded renderers have no outline
  render type, and vanilla throws when asked to outline them.
* The line-of-sight check is eye-to-eye and cached once per tick, so it is an approximation (a half-covered
  entity can read as "blocked").

## Not verified / limitations

* This source has not been compiled against the real 26.1.2 jars or run in-game; the first GitHub Actions
  run is the real compile test.
* The two mixin targets above were taken from another 26.1.2 mod's notes, not from the game's source.
* The menu uses `Screen`, `Button` and `GuiGraphicsExtractor` exactly as shown in Fabric's 26.1.2 docs, but the
  docs do not show the import lines. The packages used are `net.minecraft.client.gui.screens.Screen`,
  `net.minecraft.client.gui.components.Button` and `net.minecraft.client.gui.GuiGraphicsExtractor`. If the build
  complains about one of those imports, fix the import and everything else should compile.
* Default keys (G, O) and the Mod Menu entrypoint follow the documented APIs but were not tried in-game.

## Not included (easy to add)

Boxes, tracers, nametags (Fabric's `LevelRenderEvents`), block ESP / X-ray (touches chunk meshing, so kept out
for Sodium safety), and a hex-colour text field in the menu.
