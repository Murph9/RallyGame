package game;

import com.jme3.app.state.AbstractAppState;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.dropdown.DropDownListBoxSelectionChangedEventSubscriber;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class ChooseState extends AbstractAppState implements ScreenController {

	DropDown<String> dropdown;
	
	public void choose() {
		App.rally.startDrive();
		App.nifty.gotoScreen("noop");
	}

	@Override
	public void bind(Nifty arg0, Screen arg1) {
		dropdown = findDropDownControl(arg1, "dropdown");
		if (dropdown != null) {
			//TODO get list of car types
			dropdown.addItem("Nifty GUI");
			dropdown.addItem("Slick2d");
			dropdown.addItem("Lwjgl");
			dropdown.selectItemByIndex(1);
		}
	}

	public void onEndScreen() { }
	public void onStartScreen() { }

	private <T> DropDown<T> findDropDownControl(Screen screen, final String id) {
		return screen.findNiftyControl(id, DropDown.class);
	}

	@NiftyEventSubscriber(id="dropdown")
	public void onDropDownSelectionChanged(final String id, final DropDownSelectionChangedEvent<String> event) {
		if (event.getSelection() == null) {
			H.p("");
		} else {
			H.p(event.getSelection().toString());
		}
		//TODO set the vehicle
	}
}
