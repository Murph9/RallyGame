package game;

import com.jme3.app.state.AbstractAppState;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class StartState extends AbstractAppState implements ScreenController {

	public void startFast() {
		App.rally.startFast();
	}
	
	public void startBasic() {
		App.rally.next(this);
	}
	
	@Override
	public void bind(Nifty arg0, Screen arg1) {
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
