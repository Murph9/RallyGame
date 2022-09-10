package rallygame.game;

import com.jme3.system.AppSettings;
import rallygame.settings.Configuration;

//Entry point to the project.

public class Main {

	private static boolean USE_CONFIG = false;

	public static void main(String[] args) {
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean(). getInputArguments().toString().contains("-agentlib:jdwp");
		System.out.println("Program isdebug: " + isDebug);
		
		App app = new App();

		if (USE_CONFIG) {
			Configuration config = Configuration.Read();
			AppSettings settings = new AppSettings(true);
			if (config.ifFullscreen()) {
				settings.setFullscreen(true);
				//untested and will probably cause some resolution issues if it doesn't match up nice
			} else {
				settings.setResolution(config.getWidth(),config.getHeight());
			}
			settings.setUseJoysticks(true);
			settings.setTitle(config.getTitle());
			settings.setVSync(config.ifVsnyc());
			
			settings.setFrameRate(config.getFrameRate());
			app.setSettings(settings);
			app.setShowSettings(false);
		}
		
		app.setDisplayStatView(false); //defaults to true, shows the triangle count and stuff
		app.start();
	}
}
