package game;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import drive.DriveBase;
import drive.DriveMenu;
import helper.H;

public class RaceMenu extends DriveMenu {

	private Container state;
	private Label raceLabel;
	
	public RaceMenu(DriveBase drive) {
		super(drive);
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		state = new Container();
		raceLabel = new Label("Race state?");
		state.attachChild(raceLabel);
		state.setLocalTranslation(H.screenTopLeft().subtract(new Vector3f(0,300,0)));
		App.rally.getGuiNode().attachChild(state);
	}

	public void setText(String text) {
		if (raceLabel != null)
			raceLabel.setText(text);
	}
}
