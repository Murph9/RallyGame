package settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Configuration implements UserSettings {

	private final static String APP_DATA = System.getProperty("user.home")+"/.murph9/";
	private final static String CONFIG_FILE_PATH = APP_DATA + "rally/config.text";
	
	public static void Write(Configuration config) {
		Properties prop = config.getProperties();

		OutputStream output = null;
		try {
			output = new FileOutputStream(CONFIG_FILE_PATH);
			prop.store(output, null);
			
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static Configuration Read() {
		Properties prop = new Properties();
		InputStream input = null;
		boolean saveAtEnd = false;
		
		try {
			//make file if it doesn't exist
			File saveFile = new File(CONFIG_FILE_PATH);
			
			if (!saveFile.exists()) {//need to write if not exists
				saveFile.getParentFile().mkdirs();
				saveFile.createNewFile();
				
				saveAtEnd = true;
			}

			input = new FileInputStream(saveFile);

			prop.load(input);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		Configuration config = new Configuration(prop);
		
		if (saveAtEnd)
			Configuration.Write(config);
			
		return config;
	}
	
	////////////////////////

	private final String gameTitle = "Rally"; //setTitle("Rally");
	private final boolean useInput = true; //setUseInput(true);
	
	private int height; //720
	private int width; //1280
	
	private boolean fullScreen; //setFullscreen(false);
	private boolean setVsync; //setVSync(false);
	private int frameRate; //setFrameRate(60);
	private int samples; //setSamples(0);
	
	private int aiCount; //2
	
	//TODO more
	//default car, ai car
	//default map, dynamic map
	
	private Configuration(Properties prop) {
		if (prop == null) {
			height = 720;
			width = 1280;
			fullScreen = false;
			setVsync = false;
			frameRate = 60;
			samples = 0;
			aiCount = 0;
			return;
		}
		
		height = Integer.parseInt(Prop.Height.GetOrDefault(prop));
		width = Integer.parseInt(Prop.Width.GetOrDefault(prop));
		fullScreen = Boolean.parseBoolean(Prop.FullScreen.GetOrDefault(prop));
		setVsync = Boolean.parseBoolean(Prop.Vsync.GetOrDefault(prop));
		frameRate = Integer.parseInt(Prop.FrameRate.GetOrDefault(prop));
		samples = Integer.parseInt(Prop.Samples.GetOrDefault(prop));
		aiCount = Integer.parseInt(Prop.AICount.GetOrDefault(prop));
	}
	
	private Properties getProperties() {
		Properties prop = new Properties();
		
		prop.setProperty(Prop.Height.value, String.valueOf(height));
		prop.setProperty(Prop.Width.value, String.valueOf(width));
		prop.setProperty(Prop.FullScreen.value, String.valueOf(fullScreen));
		prop.setProperty(Prop.Vsync.value, String.valueOf(setVsync));
		prop.setProperty(Prop.FrameRate.value, String.valueOf(frameRate));
		prop.setProperty(Prop.Samples.value, String.valueOf(samples));
		prop.setProperty(Prop.AICount.value, String.valueOf(aiCount));
		
		return prop;
	}
	
	private enum Prop {
		Width("width", "1280"), 
		Height("height", "720"), 
		FullScreen("fullscreen", "false"), 
		Vsync("vsync", "false"), 
		FrameRate("frameRate", "60"),
		Samples("samples", "0"),
		AICount("aiCount", "0"),
		;
		
		String value; 
		String defaultValue;
		Prop (String v, String def) {
			this.value = v;
			this.defaultValue = def;
		}
		
		public String GetOrDefault(Properties prop) {
			String temp = prop.getProperty(this.value);
			if (temp == null)
				temp = this.defaultValue;
			return temp;
		}
	}

	public String getTitle() { return gameTitle; }
	public boolean ifInput() { return useInput; } 
	
	public int getHeight() { return height; }
	public int getWidth() { return width; }
	public boolean ifFullscreen() { return fullScreen; }
	public boolean ifVsnyc() { return setVsync; }
	public int getFrameRate() { return frameRate; }
	public int getSamples() { return samples; }
}
