package folk.sisby.antique_atlas.gui.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * A GuiComponent that can act like a button.
 */
@SuppressWarnings("rawtypes")
public class ButtonComponent extends Component {
	protected final List<IButtonListener> listeners = new ArrayList<>();

	protected SoundEvent clickSound = SoundEvents.UI_BUTTON_CLICK.value();

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (!isClipped && click.button() == 0 && isMouseOver(click.x(), click.y())) {
			onClick();
			return true;
		}

		return super.mouseClicked(click, doubled);
	}

	/**
	 * Called when the user left-clicks on this component.
	 */
	@SuppressWarnings("unchecked")
	public void onClick() {
		if (clickSound != null) {
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(clickSound, 1.0F));
		}

		for (IButtonListener listener : listeners) {
			listener.onClick(this);
		}
	}

	public void addListener(IButtonListener listener) {
		listeners.add(listener);
	}
}
