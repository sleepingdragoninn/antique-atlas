package folk.sisby.antique_atlas;

/**
 * The enum represents the different height levels in biomes.
 */
public enum TileElevation {
	VALLEY("valley"),
	LOW("low"),
	MID("mid"),
	HIGH("high"),
	PEAK("peak");

	public final String name;

	TileElevation(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return getName();
	}

	public static TileElevation fromBlocksAboveSea(int elevation) {
		if (elevation < 10) {
			return TileElevation.VALLEY;
		} else if (elevation < 20) {
			return TileElevation.LOW;
		} else if (elevation < 35) {
			return TileElevation.MID;
		} else if (elevation < 50) {
			return TileElevation.HIGH;
		} else {
			return TileElevation.PEAK;
		}
	}
}
