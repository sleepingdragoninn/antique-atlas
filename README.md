<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center><img alt="mod preview" src="https://cdn.modrinth.com/data/Y5Ve4Ui4/images/14513bf9172fa0d058e9486958de4884408ed4e4.png" /></center>

<center>
A hand-drawn client-side world map with biomes, structures, waypoints, and less!<br/>

A rewrite of <a href="https://modrinth.com/mod/antique-atlas">Antique Atlas</a> by <a href="https://github.com/Hunternif">Hunternif</a>, as continued by <a href="https://github.com/Kenkron">Kenkron</a>, <a href="https://github.com/asiekierka">asie</a>, and <a href="https://github.com/tyra314">tyra314</a>.<br/>
<b>Requires <a href="https://modrinth.com/mod/surveyor">Surveyor Map Framework</a>.</b>
<b>Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on (neo)forge.</b><br/>
</center>

---

Press **[M]** at any time to bring up a stylized world map screen.<br/>
Drag the map to pan, scroll to zoom, and use the bookmark buttons to create and remove waypoints.

**AA4 has _absolutely no items!_** - This means the map works right away, can't be lost, and works on any server!

## Client-Side Features

- Antique Atlas 4 is designed to let you focus on exploring the world, not get stuck staring at the map!
	- The map is rendered using hand-drawn "tiles" to represent terrain, biomes, and structures.
	- Tiles represent entire chunks at least - no peeking for caves or bases!
- Gravestones automatically appear where you die, with customizable flavour text:

> ![grave style euphemisms](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/c6f5e20bcef2c26c40390e888e540dcdd89a1818.png)

- Waypoint markers come in a variety of styles:

> ![marker styles](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/b7064c3287c5535cd9ac6d454c10ead984c7a7b3.png)

- Books renamed "Antique Atlas" will display your immediate surroundings, like a minimap:

> ![handheld atlas](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/b3002225851522c2d4eabc7462a374fbcdd2db6b.png)

If you'd instead prefer a low-tech compass, try [PicoHUD](https://modrinth.com/mod/picohud)!

## Mixed-Side Features

_These work in singleplayer, or on servers with [Surveyor](https://modrinth.com/mod/surveyor) installed._

- Markers are automatically added for notable structures and active nether portals:

> ![structure markers](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/190cc4eaa2e8784dd0f46bee9c225228a05f191a.png)

- Structures only appear on the map after you've looked at them or stood on them in-game:

> ![structure discovery](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/86054c7949fed59341cef60d0d9f27aee86ae6ef.gif)

- Map exploration can be shared with `/surveyor share [player]` which will also reveal those players' position:

> ![map sharing](https://cdn.modrinth.com/data/Y5Ve4Ui4/images/4422049c395a856c35bbc361c52e8bcd30e89523.png)

## Configuration

Antique Atlas can be configured from `config/antique-atlas.toml`:<br/>

> `fullscreen` can be disabled to lock the size of the map screen based on your GUI scale.<br/>
> `mapScale` can be adjusted to change the effective GUI scale of the tiles on the map.<br/>
> `structureMarkers` can be edited to toggle markers for structures - this is populated at runtime.<br/>
> `graveStyle` will change the icon and tooltip for player graves - try each out to suit your pack's aesthetics.<br/>

Surveyor, which handles features including map sharing & visibility, can be configured from `config/surveyor.toml`

## Resource Packs

Tiles, markers, biome detection, and structure detection is fully data-driven via resource packs.<br/>
**Without defined biomes, atlas "guesses" tiles via tags, and shows `???` if that fails.**

For a how-to on AA4 resource packs, check out the [resource pack tutorial](https://github.com/sisby-folk/antique-atlas/wiki/Resource-Packs).

To automatically mark non-structure points of interest - instead use Surveyor's Landmark API, as in [Surveystones](https://modrinth.com/mod/surveystones).

## Version History

This is a loveletter rewrite. We want to introduce new players to atlas, and make it easier to maintain and improve.<br/>
`0.x` uses arch, keeps the old `antiqueatlas` ID, and is API-compatible with [tyra's port](https://modrinth.com/mod/antique-atlas).<br/>
`1.x` uses fabric, and is API-incompatible with 0.x.<br/>
`2.x` uses fabric and [Surveyor](https://modrinth.com/mod/surveyor) - and is save/API/network/respack-incompatible with older versions.<br/>

Once stable and feature-complete, we intend to maintain atlas on most modpack versions >=1.18.2 ([more info](https://github.com/sisby-folk/antique-atlas/issues/81))

## Afterword

All mods are built on the work of many others.

The art for antique atlas was created by [Hunternif](https://github.com/Hunternif) ([DA](https://www.deviantart.com/hunternif)) and [lumiscosity](https://github.com/lumiscosity) ([Neocities](https://lumiscosity.neocities.org/)).<br/>
[Click here](https://github.com/sisby-folk/antique-atlas/blob/1.20/CREDITS) for detailed art credit.

This mod is a fourth-gen rewrite, and relies heavily on contributions of many developers and artists before us.<br/>
We can't draw autotile to save our lives - feel free to [contribute](https://github.com/sisby-folk/antique-atlas/issues?q=is%3Aissue+is%3Aopen+label%3Atexturing)!

This mod is included in [Tinkerer's Quilt Plus](https://modrinth.com/modpack/tinkerers-quilt) - our modpack about rediscovering vanilla.

We're open to better ways to implement our mods. If you see something odd and have an idea, let us know!

---

<center>
<b>Tinkerer's:</b> <a href="https://modrinth.com/modpack/tinkerers-quilt">Quilt</a> - <a href="https://modrinth.com/mod/tinkerers-smithing">Smithing</a> - <a href="https://modrinth.com/mod/origins-minus">Origins</a> - <a href="https://modrinth.com/mod/tinkerers-statures">Statures</a> - <a href="https://modrinth.com/mod/picohud">HUD</a><br/>
<b>Loveletters:</b> <a href="https://modrinth.com/mod/inventory-tabs">Tabs</a> - <i>Atlas</i> - <a href="https://modrinth.com/mod/portable-crafting">Portable Crafting</a> - <a href="https://modrinth.com/mod/drogstyle">Drogstyle</a><br/>
<b>Others:</b> <a href="https://modrinth.com/mod/switchy">Switchy</a> - <a href="https://modrinth.com/mod/crunchy-crunchy-advancements">Crunchy</a> - <a href="https://modrinth.com/mod/starcaller">Starcaller</a><br/>
</center>
