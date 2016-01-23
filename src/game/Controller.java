package game;

import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;

import java.util.HashMap;
import java.util.Map;

public class Controller {

	private PlayerControl cur;
	Map<Joystick, PlayerControl> toControl = new HashMap<Joystick, PlayerControl>();
		//this is a mapping between the control recieved from JME3, to what is currently pressed

	
	public void setButtonValue(Joystick joystick, JoystickButton button, boolean isPressed) {
//		System.out.println("Button:" + button.getName() + "=" + (isPressed ? "Down" : "Up"));
		if (!toControl.containsKey(joystick)) {
			toControl.put(joystick, new PlayerControl());
		}
		cur = toControl.get(joystick);
		
		cur.buttons.put(button.getLogicalId(), isPressed);
	}

	public void setAxisValue(Joystick joystick, JoystickAxis axis, float value) {
		H.p( "Axis:" + axis.getName() + "=" + value );
		if (!toControl.containsKey(joystick)) {
			toControl.put(joystick, new PlayerControl());
		}
		cur = toControl.get(joystick);
		
		if (axis == axis.getJoystick().getXAxis()) { // THE NORMAL STICK
			cur.xAxis = value;
		} else if (axis == axis.getJoystick().getYAxis()) {
			cur.yAxis = -value;

		} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS)) {
			// Note: in the above condition, we could check the axis name but
			// I have at least one joystick that reports 2 "Z Axis" axes.
			// In this particular case, the first one is the right one so
			// a name based lookup will find the proper one. It's a problem
			// because the erroneous axis sends a constant stream of values.
//			System.out.println("triggers=" + value);
			// left is positive value and right is negative value
			// ignore small numbers and numbers that are close to one..

			cur.zAxis = value;
			
		} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_ROTATION)) {
			System.out.println("???Button 12???");

		} else if (axis.getName().equals("Y Rotation")) { //The other stick
			cur.yRot = value;
		} else if (axis.getName().equals("X Rotation")) { //The other stick
			cur.xRot = value;

		} else if (axis == axis.getJoystick().getPovXAxis()) { //DPAD
//			TODO - fix the pov_x and pov_y buttons
			
			if (cur.lastPovX < 0) {
				cur.buttons.put("POV -X", false); //dpad left/right
			} else if (cur.lastPovX > 0) {
				cur.buttons.put("POV +X", false);
			}
			if (value < 0) {
				cur.buttons.put("POV -X", true);
			} else if (value > 0) {
				cur.buttons.put("POV +X", true);
			}
			cur.lastPovX = value;
		} else if (axis == axis.getJoystick().getPovYAxis()) {
			System.out.println(axis.getJoystick().getPovYAxis());
			if (cur.lastPovY < 0) {
				cur.buttons.put("POV -Y", false); //dpad up/dwown
			} else if (cur.lastPovY > 0) {
				cur.buttons.put("POV +Y", false);
			}
			if (value < 0) {
				cur.buttons.put("POV -Y", true);
			} else if (value > 0) {
				cur.buttons.put("POV +Y", true);
			}
			cur.lastPovY = value;
		}
	}

	//for storing the states of all the buttons
	class PlayerControl {
		// button mappings
		float xAxis = 0; //left stick x
		float yAxis = 0; //left stick y
		float xRot = 0; //right stick x
		float yRot = 0; //right stick y
		
		float zAxis = 0; //triggers

		float lastPovX = 0;
		float lastPovY = 0;

		Map<String, Boolean> buttons = new HashMap<String, Boolean>();

		PlayerControl() {
			//These are setup for my x-box controller
			// A "standard" mapping... fits a majority of my game pads
	        buttons.put(JoystickButton.BUTTON_3, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_1, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_0, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_2, new Boolean(false));

	        // Front buttons  Some of these have the top ones and the bottoms ones flipped.           
	        buttons.put(JoystickButton.BUTTON_4, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_5, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_10, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_11, new Boolean(false));

	        // Select and start buttons           
	        buttons.put(JoystickButton.BUTTON_6, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_7, new Boolean(false));
	        
	        // Joystick push buttons
	        buttons.put(JoystickButton.BUTTON_8, new Boolean(false));
	        buttons.put(JoystickButton.BUTTON_9, new Boolean(false));

	        // Fake button highlights for the POV axes
	        //    +Y  
	        //  -X  +X
	        //    -Y
	        buttons.put("POV +Y", new Boolean(false));
	        buttons.put("POV +X", new Boolean(false));
	        buttons.put("POV -Y", new Boolean(false));
	        buttons.put("POV -X", new Boolean(false));
		}
		
		public String toString() {
			String out = "xx"+xAxis;
			out += "yx"+yAxis;
			out += "xr"+xRot;
			out += "yr"+yRot;
			
			out += "zx"+zAxis;

			for(String s: buttons.keySet()) {
				out += s+""+buttons.get(s);
			}
			return out;
		}
	}
}
