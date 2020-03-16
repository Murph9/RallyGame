package rallygame.settings;

public interface UserSettings {
	
	int getHeight();
	int getWidth();
	boolean ifFullscreen();
	boolean ifVsnyc();
	int getFrameRate();
	int getSamples();
	
	String getTitle();
	boolean ifInput();
}
