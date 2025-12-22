<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center>
A hand-drawn clientside world map, with map sharing, structure discovery, and more!<br/>
A rewrite of <a href="https://modrinth.com/mod/antique-atlas">Antique Atlas</a> by <a href="https://github.com/Hunternif">Hunternif</a>, as continued by <a href="https://github.com/Kenkron">Kenkron</a>, <a href="https://github.com/asiekierka">asie</a>, and <a href="https://github.com/tyra314">tyra314</a>.<br/>
<b>Requires <a href="https://modrinth.com/mod/surveyor">Surveyor Map Framework</a>.</b>
<b>Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on (neo)forge.</b><br/>
</center>

---

Press **[M]** at any time to bring up the world map.<br/>
Drag the map to pan, scroll to zoom, and use the bookmark buttons to create and remove map markers.

## Client-Side Features

- A physical-feeling map screen, with hand-drawn tiles representing chunks in the world:

> ![map preview](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/14513bf9172fa0d058e9486958de4884408ed4e4.png)

- Waypoint markers in a variety of styles and accent colors:

> ![marker styles](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/b7064c3287c5535cd9ac6d454c10ead984c7a7b3.png)

- Hold the map in your hands by renaming a book "Antique Atlas" at an anvil:

> ![handheld atlas](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/b3002225851522c2d4eabc7462a374fbcdd2db6b.png)

- Automatic migration of waypoints from Xaero's Minimap, and a shared save format with [Hoofprint](https://modrinth.com/mod/hoofprint).
- Extra features via addons like [Antique Trains](https://modrinth.com/mod/antique-trains) and [Antique Atlas Compass HUD](https://modrinth.com/mod/antique-atlas-compass-hud)!

## Mixed-Side / Singleplayer Features

- Markers are automatically added for notable structures and active nether portals:

> ![structure markers](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/190cc4eaa2e8784dd0f46bee9c225228a05f191a.png)

- Structures only appear on the map after you've looked at them or stood on them in-game:

> ![structure discovery](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/86054c7949fed59341cef60d0d9f27aee86ae6ef.gif)

- Map exploration and waypoints can be shared with friends via `/surveyor share [player]`:

> ![map sharing](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/305e5c88b384bc1a0a6fd2d0fcdc21f72b8d3a57.png)

- Add [AA4 Atlas](https://modrinth.com/mod/aa4-atlas) to give the handheld atlas a full item ID and crafting recipe!

## Troubleshooting / Suggestions

Antique Atlas 4 is a **clientside map frontend** for [Surveyor Map Framework](https://modrinth.com/mod/surveyor).<br/>
It renders surveyor save data as tiles on screen using respacks, and provides a GUI editor for waypoints.<br/>
Issues and suggestions regarding the screen, tiles, markers, and resource packs are [Antique Atlas 4 Issues](https://github.com/sisby-folk/antique-atlas/issues).<br/>
Issues and suggestions regarding map sharing, explored map area, and automatic markers are [Surveyor Issues](https://github.com/sisby-folk/surveyor).<br/>
**Crash reports must have AA4 and Surveyor on the latest version, and include `logs/latest.log` via [mclo.gs](https://mclo.gs/).** 

## Configuration

Antique Atlas can be configured from `config/antique-atlas.toml` or in-game using [McQoy](https://modrinth.com/mod/mcqoy), including:<br/>
- Whether to require having an atlas item in the inventory in order to enable the map hotkey.
- Adjustments to the size and scale of the map screen.
- Adjustments to which structures to mark on the map.
- Adjustments for how to stylize player graves.

Additional options can be found in the Surveyor config in `config/surveyor.toml`.

## Addon & Resource Pack Developers

### Resource Packs

Tiles, markers, biome detection, and structure detection is fully data-driven via resource packs.<br/>
**Without defined biomes, atlas "guesses" tiles via tags, and shows `???` if that fails.**

For a how-to on AA4 resource packs, check out the [resource pack tutorial](https://github.com/sisby-folk/antique-atlas/blob/1.20/RESPACKS.md).

### API

Nothing stable! But feel free to poke around:

```groovy
repositories {
	maven { url "https://repo.sleeping.town/" }
}
dependencies {
	modImplementation "folk.sisby:antique-atlas:3.0.0+1.20"
}
```

Try `AtlasRenderer.registerOverlay()` for non-surveyor mod compat (i.e. mods that already have client sync)

To automatically mark non-structure points of interest - instead use [Surveyor](https://modrinth.com/mod/surveyor)'s Landmark API.

### Licensing + Credit

Please match your addon/respack licenses to LGPLv3 for code & CC BY-NC-SA for assets to help the ecosystem!<br/>
(LGPLv3 and CC BY-NC-SA are a copyleft licenses, so this is required for anything directly adapted from AA4)

If you've made an addon/respack, hit us up and we might link it here! If it's still WIP feel free to ask us questions.<br/>
Devs can reach out to us through the [modfest discord](https://discord.gg/gn543Ee) (#projects->Surveyor), on [mastodon](https://tech.lgbt/@sleepingdragoninn), or hell, via [email](mailto:sleepingdragoninn@gmail.com).

## Afterword

All mods are built on the work of many others.

The art for antique atlas was created by [Hunternif](https://github.com/Hunternif) ([DA](https://www.deviantart.com/hunternif)) and [lumiscosity](https://github.com/lumiscosity) ([Neocities](https://lumiscosity.neocities.org/)).<br/>
[Click here](https://github.com/sisby-folk/antique-atlas/blob/1.20/CREDITS) for detailed art credit.

This mod is a loveletter rewrite, and relies heavily on contributions of many developers and artists before us.<br/>
