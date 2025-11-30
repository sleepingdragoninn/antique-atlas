package folk.sisby.antique_atlas.gui.tiles;

import folk.sisby.antique_atlas.util.ArrayIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * The 4 subtiles in a corner between 4 tiles, each subtile belonging to a
 * different tile. When the tiles are positioned as follows:
 * <pre>
 *  a b
 *  c d
 * </pre>
 * then the subtiles 0-1-2-3 belong to tiles a-b-c-d respectively.
 *
 * @author Hunternif
 */
public class SubTileQuartet implements Iterable<SubTile> {
	protected final SubTile[] array;

	public SubTileQuartet(SubTile a, SubTile b, SubTile c, SubTile d) {
		array = new SubTile[]{a, b, c, d};
	}

	/**
	 * Set the coordinates for the top left subtile, and the rest of them
	 * have their coordinates updated respectively.
	 */
	public void setCoords(int x, int y) {
		array[0].x = x;
		array[1].x = x + 1;
		array[2].x = x;
		array[3].x = x + 1;

		array[0].y = y;
		array[1].y = y;
		array[2].y = y + 1;
		array[3].y = y + 1;
	}

	@Override
	public @NotNull Iterator<SubTile> iterator() {
		return new ArrayIterator<>(array);
	}
}
