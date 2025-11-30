package folk.sisby.antique_atlas.gui.core;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A group of GuiToggleButtons only one of which can be selected at any time.
 */
public class ToggleButtonRadioGroup<B extends ToggleButtonComponent> implements Iterable<B> {
	protected final List<B> buttons = new ArrayList<>();

	protected final List<ISelectListener<? extends B>> listeners = new ArrayList<>();

	protected B selectedButton = null;

	protected final ClickListener clickListener;

	public ToggleButtonRadioGroup() {
		clickListener = this.new ClickListener();
	}

	public void addButton(B button) {
		if (!buttons.contains(button)) {
			buttons.add(button);
			button.addListener(clickListener);
		}
	}

	/**
	 * Sets the specified button as selected, provided it is in this group.
	 * Doesn't trigger the select handlers!
	 */
	public void setSelectedButton(B button) {
		if (buttons.contains(button)) {
			if (selectedButton != null) {
				selectedButton.setSelected(false);
			}
			button.setSelected(true);
			selectedButton = button;
		}
	}

	@Override
	public @NotNull Iterator<B> iterator() {
		return buttons.iterator();
	}

	public void addListener(ISelectListener<? extends B> listener) {
		listeners.add(listener);
	}

	public class ClickListener implements IButtonListener<B> {
		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public void onClick(B button) {
			if (button != selectedButton) {
				if (selectedButton != null) {
					selectedButton.setSelected(false);
				}
				selectedButton = button;
				for (ISelectListener listener : listeners) {
					listener.onSelect(selectedButton);
				}
			}
		}
	}
}
