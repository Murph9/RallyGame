package game;

import com.jme3.app.state.AbstractAppState;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class StartState extends AbstractAppState implements ScreenController {

	
	public void start() {
		App.rally.startChoose();
		App.nifty.gotoScreen("choose");
	}
	
	@Override
	public void bind(Nifty arg0, Screen arg1) {
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
