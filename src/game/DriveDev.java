package game;

import world.BasicWorld;
import world.FlatWorld;
import world.FullCityWorld;
import world.ObjectWorld;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;
import world.WorldType;
import world.curve.CurveWorld;
import world.highway.HighwayWorld;
import world.wp.WP.DynamicType;

import java.lang.reflect.Field;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;

import car.*;
import helper.H;


public class DriveDev extends DriveSimple implements RawInputListener {

	private TextField tf;
	private Container myWindow;
	
	public DriveDev (CarData car, World world) {
    	super(car, world);
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	App.rally.getInputManager().addRawInputListener(this);
    	
    	//init gui
		myWindow = new Container();
		
		float width = 160;
		App.rally.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(H.screenMiddle().subtract(width/2,0,0));
		
		tf = new TextField("");
		tf.setPreferredWidth(width);
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}

	//TODO interface feedback
	//TODO tab complete fields
	private void parseInput(String input) {
		input = input.replace("=", ""); //no equals
		
		String[] in = input.split("\\s+");
		if (in.length < 2) {
			H.p("Not correct input: ", in);
			return;
		}
		
		if (in[0].equals("world")) {
			WorldType type = null;
			try {
				type = Enum.valueOf(WorldType.class, in[1]);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			if (type != null) {
				World newWorld = null;
				switch (type) {
					case OBJECT:
						newWorld = new ObjectWorld(); //TODO move constructors to WorldType enum
						break;
						
					case FULLCITY:
						newWorld = new FullCityWorld();
						break;
					
					case CURVE:
						newWorld = new CurveWorld();
						break;
						
					case HIGHWAY:
						newWorld = new HighwayWorld();
						break;
					
					case BASIC:
						newWorld = new BasicWorld();
						break;
						
					case FLAT:
						newWorld = new FlatWorld();
						break;
					
					case STATIC:
						StaticWorld t = null;
						try {
							t = Enum.valueOf(StaticWorld.class, in[2]);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
						if (t != null) {
							newWorld = new StaticWorldBuilder(t);
						}
						break;
					case DYNAMIC:
						DynamicType a = null;
						try {
							a = Enum.valueOf(DynamicType.class, in[2]);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
						if (a != null) {
							newWorld = a.getBuilder();
						}
						break;
					default:
						try {
							throw new Exception("oh");
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
				}
				
				if (newWorld != null) {
					App.rally.getStateManager().detach(world);
					App.rally.getStateManager().attach(newWorld);
					world = newWorld;
					
					this.cb.get(0).setPhysicsLocation(world.getStartPos());
				}
				
			}
			
			return;
		}
		
		if (in.length != 2) {
			H.p("Not correct input: ", in);
			return;
		}
		setField(this.cb.get(0).car, in[0], in[1]);
	}
	
	private void setField(Object obj, String str, String value) {
		if (str == null) {
			H.p("Not correct input: ", str);
			return;
		}
		try {
			String[] parts = str.split("\\."); //follow prop.field.mass
			
			for (String field: parts) {
				Class<?> clazz = obj.getClass();
				Field f = clazz.getField(field);
				Boolean set = false;
				if (f.getType() == float.class) {
					f.setFloat(obj, Float.parseFloat(value));
					set = true;
				} else if (f.getType() == int.class) {
					f.setInt(obj, Integer.parseInt(value));
					set = true;
				} else { //TODO any other types?
					f.set(obj, value);
					set = true;
				}
				
				if (set) {
					reloadCar();
					break;
				}
				obj = f.get(obj);
			}

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			H.p("Remember this can only set public fields");
			return;
		}
	}
	
	public void reloadCar() {
		Vector3f pos = this.cb.get(0).getPhysicsLocation();
		Vector3f vel = this.cb.get(0).getLinearVelocity();
		Quaternion q = this.cb.get(0).getPhysicsRotation();
		
		//TODO minimap can't be reset
		App.rally.getStateManager().detach(uiNode);
		App.rally.getStateManager().detach(menu);
		this.cb.removePlayer(0);
		
		cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true);
		App.rally.getStateManager().attach(cb);
		
		uiNode = new CarUI(cb.get(0));
		App.rally.getStateManager().attach(uiNode);
		
		camera.setCar(cb.get(0));
		
		//keep current pos, vel, rot
		this.cb.get(0).setPhysicsLocation(pos);
		this.cb.get(0).setLinearVelocity(vel);
		this.cb.get(0).setPhysicsRotation(q);
	}
	
	public void beginInput() { }
	public void endInput() { }
	public void onJoyAxisEvent(JoyAxisEvent evt) {}
	public void onJoyButtonEvent(JoyButtonEvent evt) {}
	public void onMouseMotionEvent(MouseMotionEvent evt) {}
	public void onMouseButtonEvent(MouseButtonEvent evt) {}
	public void onKeyEvent(KeyInputEvent evt) {
		if (evt.isPressed())
			return; //only listen to releases (this way it prevents the held duplicates)
		
		if (evt.getKeyCode() == KeyInput.KEY_TAB) {
			if (tf.getParent() == null) {
				myWindow.addChild(tf);
				GuiGlobals.getInstance().requestFocus(tf);
			} else {
				myWindow.removeChild(tf);
				GuiGlobals.getInstance().requestFocus(App.rally.getGuiNode());
			}
			myWindow.updateModelBound();
		}
		
		String input = null;
		if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
			//fetch the console info
			input = tf.getText();
			tf.setText(null);
			
			if (tf.getParent() != null)
				evt.setConsumed(); //stop the enter from passing through
		}
		
		if (input == null || input.length() < 1)
			return;
		
		parseInput(input);
	}
	public void onTouchEvent(TouchEvent evt) {}
	
	@Override
	public void cleanup() {
		super.cleanup();
		App.rally.getInputManager().removeRawInputListener(this);
		
		App.rally.getGuiNode().detachChild(myWindow);
	}
}
