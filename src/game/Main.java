package game;

import com.jme3.system.AppSettings;
import settings.Configuration;

//Entry point to the project.

public class Main {

	private static boolean USE_CONFIG = false;

	public static void main(String[] args) {
		App app = new App();

		if (USE_CONFIG) {
			Configuration config = Configuration.Read();
			AppSettings settings = new AppSettings(true);
			if (config.ifFullscreen()) {
				settings.setFullscreen(true);
				//TODO untested
				//will probably cause some resolution issues if it doesn't match up nice
			} else {
				settings.setResolution(config.getWidth(),config.getHeight());
			}
			settings.setFrameRate(config.getFrameRate());
			settings.setUseJoysticks(true);
			settings.setTitle(config.getTitle());
			settings.setVSync(config.ifVsnyc());
			
			settings.setFrameRate(config.getFrameRate());
			app.setSettings(settings);
			app.setShowSettings(false);
		}
		
		app.setDisplayStatView(true); //defaults to on, shows the triangle count and stuff
		app.start();
	}
}
