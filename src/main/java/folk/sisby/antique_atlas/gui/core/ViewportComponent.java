package folk.sisby.antique_atlas.gui.core;

/**
 * The children of this component are rendered and process input only inside
 * the viewport frame. Use {@link #setSize(int, int)} to set its bounds.
 *
 * @author Hunternif
 */
public class ViewportComponent extends Component {
	protected final Component content = new Component(); // must have its own component so it can move relatively to the viewport

	public ViewportComponent() {
		this.addChild(content);
	}

	public Component addContent(Component child) {
		return content.addChild(child);
	}

	public void removeAllContent() {
		content.removeAllChildren();
	}

	@Override
	public int getWidth() {
		return properWidth;
	}

	@Override
	public int getHeight() {
		return properHeight;
	}

	@Override
	public void updateSize() {
		super.updateSize();
		// Update the clipping flag on content's child components:
		for (Component child : content.getChildren()) {
			child.setClipped(child.getGuiY() > getGuiY() + properHeight ||
				child.getGuiY() + child.getHeight() < getGuiY() ||
				child.getGuiX() > getGuiX() + properWidth ||
				child.getGuiX() + child.getWidth() < getGuiX()
			);
		}
	}
}
