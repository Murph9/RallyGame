package game;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.controls.ActionListener;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;
import com.jme3.system.Timer;

public class Rally extends SimpleApplication {
	
	public DriveState drive;
	public MenuState menu;
    
	public static void main(String[] args) {
		int fps = 60; //default is 60
		
		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280,720); //1280,720 //1920,1080
		settings.setFrameRate(fps);
		settings.setUseJoysticks(true);
		settings.setVSync(false);
		
		app.setSettings(settings);
		app.setTimer(new NanoT60(fps));
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();
	}
	
	@Override
	public void simpleInitApp() {
		App.rally = this;
		
		menu = new MenuState();
		
		drive = new DriveState();
		getStateManager().attach(drive);
	}

	public BitmapFont getFont() {
		return guiFont;
	}
	public AppSettings getSettings() {
		return settings;
	}

	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		stateManager.update(tpf);
	}
	
	/////////////// menu
	public void pause(boolean startPause) {
		if (startPause) {
			stateManager.detach(drive);
			stateManager.attach(menu);
		} else {
			stateManager.detach(menu);
			stateManager.attach(drive);
		}
	}
	
	public Timer getTimer() {
		return timer;
	}
	
}

class NanoT60 extends NanoTimer {
	
	private float frames;
	NanoT60(float frames) {
		super();
		this.frames = frames;
	}
	
	public void setFrames(float frames) {
		//for setting slow mo or something..
	}
	
	@Override
	public float getTimePerFrame() {
		//return tpf;
	    return 1f/frames; //frame time for 60fps
	}
}
