package folk.sisby.antique_atlas.gui.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A mechanism to encapsulate actions that need to be done every time a GUI
 * switches between distinct states of behavior.
 *
 * @author Hunternif
 */
public class ScreenState<T> {
	protected final BiConsumer<State<T>, State<T>> onChangedState;

	/**
	 * Meant to declare anonymous classes.
	 */
	public interface State<T> {
		void onEnterState(T screen);

		void onExitState(T screen);
	}

	public record ToggleState<T>(Function<T, ToggleButtonComponent> toggleButton, Consumer<T> onEnterState, Consumer<T> onExitState) implements State<T> {
		public ToggleState(Function<T, ToggleButtonComponent> toggleButton) {
			this(toggleButton, null, null);
		}

		public ToggleState() {
			this(null, null, null);
		}

		@Override
		public void onEnterState(T screen) {
			if (toggleButton != null) toggleButton.apply(screen).setSelected(true);
			if (onEnterState != null) onEnterState.accept(screen);
		}

		@Override
		public void onExitState(T screen) {
			if (toggleButton != null) toggleButton.apply(screen).setSelected(false);
			if (onExitState != null) onExitState.accept(screen);
		}
	}

	public ScreenState(BiConsumer<State<T>, State<T>> onChangedState) {
		this.onChangedState = onChangedState;
	}

	public ScreenState() {
		this.onChangedState = null;
	}

	protected volatile State<T> currentState;

	public State<T> current() {
		return currentState;
	}

	public boolean is(State<T> state) {
		return current() == state;
	}

	public void switchTo(State<T> state, T screen) {
		if (currentState != null) {
			currentState.onExitState(screen);
		}
		if (onChangedState != null) onChangedState.accept(currentState, state);
		currentState = state;
		if (state != null) {
			state.onEnterState(screen);
		}
	}
}
