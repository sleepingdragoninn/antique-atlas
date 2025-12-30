## Texturing

### Tile Textures

> `assets/packid/textures/atlas/tile/*.png|mcmeta`

#### Image

![test tile texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/test.png?raw=true) ![square plateau texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/base/plateau_square.png?raw=true) ![savanna house](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/structure/village/savanna/small_house.png?raw=true) ![nether bridge crossing](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile/structure/fortress/nether/nether_fortress_bridge_crossing.png?raw=true)

Tiles are in an autotile-like format.<br/>
They're split into 8x8 "subtiles", each being drawn depending on which of the neighbouring textures are "tiling" to them:

![autotile guide](https://github.com/sisby-folk/antique-atlas/assets/55819817/92ebcd3e-b189-429a-9ad0-667bc26a0ed6)

The top left subtiles are used for tight outer corners, the top right used for inner corners, and the bottom subtiles are used for larger contiguous areas.<br/>
Because of this, the center 4 tiles must tile to themselves in order for the texture to appear without seams - this is often achieved by leaving it almost blank as above.

#### Metafile

```json5
// base/sand.json
{
	"antique_atlas:tiling": {
		"parent": "antique_atlas:base/flat",
		"tags": [
			"antique_atlas:sand"
		],
		"tilesTo": [
			"#antique_atlas:sand",
			"#antique_atlas:sand_hills_low"
		]
	}
}
```

The metafile is used entirely to control which other textures should be "tiled" to.<br/>
That is, when "connecting" subtiles should be used instead of the border ones when it is a neighbour.

The `parent` field can be used to inherit all fields from another texture - most biomes inherit from a "base" that provides a terrain shape.<br/>
The `tag` field can be used to add this texture to a group, used when specifying tiling.<br/>
The `tilesTo`, `tilesToVertical`, and `tilesToHorizontal` arrays make this texture connect to those specified.<br/>
The `tilesToThis`, `tilesToThisVertical`, and `tilesToThisHorizontal` arrays make the *specified* textures connect to this one instead.

This takes some time to unpick and has a lot of redundant options - peruse the [builtin pack](https://github.com/sisby-folk/antique-atlas/tree/1.20/src/main/resources/assets/antique_atlas/textures/atlas/tile) for usage examples and always be sure to test in-game.

### Marker Textures

> `assets/packid/textures/atlas/marker/*.png|mcmeta`

#### Image

![unknown marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/unknown.png?raw=true) ![tower marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/tower.png?raw=true) ![tower accent texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/tower_accent.png?raw=true) ![x marker texture](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/red_x_large.png?raw=true) ![x accent](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/custom/red_x_large_accent.png?raw=true)

Markers without metafiles are 32x32 and displayed centered on the cursor.<br/>
If a marker is in the `custom/` subfolder, it will be added to the "add marker" modal in-game.

An optional `*_accent.png` can be added which will be drawn directly over the main texture, tinted to the dye colour of the landmark.

To texture non-custom markers, note that the closest texture ID to the landmark ID will be used whenever possible.<br/>
e.g. the landmark `surveyor:player_death/void/29/71/-100` try to use `surveyor/textures/atlas/marker/player_death/void.png`, then `surveyor/textures/atlas/marker/player_death.png`, then `surveyor/textures/atlas/marker/default.png`.

#### Metafile

```json5
// end_city.json
{
	"antique_atlas:marker": {
		"textureWidth": 64,
		"textureHeight": 64,
		"mipLevels": 2,
		"offsetY": -48
	}
}
```

The metafile is used to adjust texture rendering specifics for the marker.

This can be used to create abnormally large (`textureWidth` and `textureHeight`) or uncentered (`offsetX` and `offsetY`) markers as above.<br/>
The offset is how the texture should be panned relative to the top left of the texture - this is `-width/2` and `-height/2` by default (centered).

![end city marker](https://github.com/sisby-folk/antique-atlas/blob/1.20/src/main/resources/assets/antique_atlas/textures/atlas/marker/structure/end_city.png?raw=true)

The `mipLevels` field allows power-of-two mipmap textures to be added to the right of the main texture.<br/>
In the end city example, the first mip level is 32x32, then the second mip level is 16x16 - for a total of 2 mip levels. (and a 112x64 texture, which is calculated for you)

## Biome Tiles (Biome Tile Providers)

> `assets/namespace/atlas/biome/path.json` for biome `namespace:path`

```json5
// assets/minecraft/atlas/biome/frozen_ocean
{
	"textures": "antique_atlas:biome/nonland/ocean/frozen/ice_ice_spikes"
}
```

The `parent` field allows deferring the entire definition of the biome to another existing biome's - useful for quickly configuring modpacks.

Otherwise, the `textures` field is used to directly set the textures for the biome.

The field accepts a tile texture ID (without the `textures/atlas/tile/` part) - which is then used to represent the biome in the map.

It can also accept an array of texture IDs, which will be used at random, or a texture -> integer object, for randomness with weights.

Finally, the texture field also accepts an object with elevations - `valley`, `low`, `mid`, `high`, and `peak` - each accepting the above.

These will be used when the terrain is at Y <73, <83, <98, <113, and above respectively - adjusting to sea level changes.

```json5
// assets/minecraft/atlas/biome/badlands
{
	"textures": {
		"valley": [
			"antique_atlas:biome/arid/desert/sand_1",
			"antique_atlas:biome/arid/desert/sand_cacti_sparse",
			"antique_atlas:biome/arid/desert/sand_shrubs"
		],
		"low": {
			"antique_atlas:biome/arid/badlands/terracotta_shrubs_tiered": 4,
			"antique_atlas:biome/arid/badlands/terracotta_tiered": 2,
			"antique_atlas:biome/arid/badlands/terracotta_tiered_rough": 2,
			"antique_atlas:biome/arid/badlands/terracotta_tiered_separate": 1
		},
		"peak": [
			"antique_atlas:biome/arid/badlands/shrubs_plateau_low",
			"antique_atlas:biome/arid/badlands/terracotta_plateau_low"
		]
	}
}
```

## Structures

> `assets/namespace/atlas/structure/start/path.json` for structure `namespace:path`

> `assets/namespace/atlas/structure/tag/path.json` for structures of tag `#namespace:path`

> `assets/namespace/atlas/structure/type/path.json` for structures of type `namespace:path`

> `assets/namespace/atlas/structure/piece/type/path.json` for structure pieces of type `namespace:path`

> `assets/namespace/atlas/structure/piece/jigsaw/single/path.json` for single jigsaw piece `namespace:path`

> `assets/namespace/atlas/structure/piece/jigsaw/feature/path.json` for feature jigsaw piece `namespace:path`

### Tiles (Structure Tile Providers)

```json5
// assets/minecraft/atlas/structure/piece/type/necsr.json
{
	"displayId": "minecraft:nether_fortress_corridor_nether_warts_room",
	// for debug mode
	"priority": 50,
	"textures": "antique_atlas:structure/fortress/nether/nether_fortress_corridor_nether_warts_room"
}
```

Structures are provided a `priority` field, which determines how whether they should appear on top of other structures if they appear in the same chunk.<br/>
Lower is more important.

Like biomes, structures use the `textures` field in the same manner for tile textures.

Instead of elevation, structures instead have a variety of identified "Chunk Matchers" which determine whether the texture should appear, as well as over which chunks.

| ID                       | Behavior                                                                                     |
|--------------------------|----------------------------------------------------------------------------------------------|
| center                   | always shown, at the center of the object's bounding box. Used when no matcher is specified. |
| center_above_ground      | only shown if the center of the box is above sea level.                                      |
| center_top_above_ground  | only shown if the top of the box is above sea level.                                         |
| center_horizontal        | only shown if the box is longer horizontally                                                 |
| center_vertical          | only shown if the box is longer vertically                                                   |
| bridge_horizontal        | only shown if multiple chunks are spanned horizontally, over all those chunks.               |
| bridge_vertical          | only shown if multiple chunks are spanned vertically, over all those chunks.                 |
| path_straight_horizontal | only shown if a jigsaw has two junctions and they are aligned horizontally                   |
| path_straight_vertical   | only shown if a jigsaw has two junctions and they are aligned vertically                     |

```json5
// assets/minecraft/atlas/structure/piece/type/iglu.json
{
	"displayId": "minecraft:igloo",
	"textures": {
		"center_top_above_ground": "antique_atlas:structure/igloo/igloo"
	}
}
```

### Markers (Structure Marker Providers)

```json5
// assets/minecraft/atlas/structure/start/village_savanna.json
{
	"markers": "antique_atlas:structure/plains_village"
}
```

```json5
// assets/antique_atlas/lang/en_us.json
{
	"structure.start.minecraft.village_savanna": "Savanna Village"
}
```

Just a direct reference to the marker texture (without the `texture/atlas/marker` part).

The translation key is used for the tooltip, and matches the path used for the json file.

## Dimensions

Dimensions are given custom bookmark icons from `namespace/textures/atlas/dimension/path.png`, with the optional metafile:
```json5
// minecraft/textures/atlas/dimension/the_end.png.mcmeta
{
	"antique_atlas:dimension": {
		"name": "advancements.end.root.title",
		"color": "#DECF2A"
	}
}
```

Dimensions will otherwise use a question mark texture and a random color hashed from their ID.
