package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapText;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;

public class MenuState extends AbstractAppState {

	private AppActionListener aal = new AppActionListener();
	
	//TODO
	
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		BitmapText statsText = new BitmapText(App.rally.getFont(), false);		  
		statsText.setSize(App.rally.getFont().getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(App.rally.getSettings().getWidth()-200, 500, 0); // position
		App.rally.getGuiNode().attachChild(statsText);
	}
	
	

	private class AppActionListener implements ActionListener {
		public void onAction(String arg0, boolean arg1, float arg2) {
			 App.rally.pause(arg1);
		}
	}
}
