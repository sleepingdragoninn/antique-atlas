package folk.sisby.antique_atlas.gui.core;

/**
 * A button that can be toggled on, and only toggled off by selecting
 * a different ToggleButton.
 */
public class ToggleButtonComponent extends ButtonComponent {
	protected final boolean canToggle;
	protected boolean selected;

	public ToggleButtonComponent(boolean canToggle) {
		this.canToggle = canToggle;
	}

	/**
	 * Sets the button selected state. If the button is part of a RadioGroup,
	 * use the RadioGroup's setSelected method instead!
	 */
	public void setSelected(boolean value) {
		this.selected = value;
	}

	public boolean isSelected() {
		return selected;
	}

	@Override
	public void onClick() {
		if (!isSelected() || canToggle) setSelected(!isSelected());
		super.onClick();
	}
}
