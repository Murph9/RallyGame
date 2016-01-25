package game;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

class JoystickEventListner implements RawInputListener {

	//TODO:
	//currently this assumes every controller controlls the same car
	//would need to abstract it out using the old Controller class (which is still in version control)
	
	//it also is currently hardcoded..
	
	Map<String, String> intToButton = new HashMap<String, String>(); //internal int to human readable string
	Map<String, String> buttonToMapping = new HashMap<String, String>(); //to game mapping
	
	ActionListener a;
	double stickdeadzone = 0.15f;
	
	JoystickEventListner (ActionListener a) {
		this.a = a;
		
		intToButton.put(JoystickButton.BUTTON_0, "A"); //A
		intToButton.put(JoystickButton.BUTTON_1, "B"); //B
		intToButton.put(JoystickButton.BUTTON_2, "X"); //X
		intToButton.put(JoystickButton.BUTTON_3, "Y"); //Y
		
		intToButton.put(JoystickButton.BUTTON_4, "LeftBumper"); //LeftBumper
		intToButton.put(JoystickButton.BUTTON_5, "RightBumper"); //RightBumper
		
		intToButton.put(JoystickButton.BUTTON_6, "Select"); //Select
		intToButton.put(JoystickButton.BUTTON_7, "Start"); //Start
		
		intToButton.put(JoystickButton.BUTTON_8, "LeftStick"); //LeftStick
		intToButton.put(JoystickButton.BUTTON_9, "RightStick"); //RightStick
	}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		JoystickAxis axis = arg0.getAxis();
		float value = arg0.getValue();
		
		//TODO fix
		if (axis == axis.getJoystick().getXAxis()) { // left/right normal stick
			if (value > 0) {
				a.onAction("Left", false, 0);
				a.onAction("Right", true, Math.abs(value) < stickdeadzone ? 0 : value); //less than deadzone = 0
			} else {
				a.onAction("Left", false, 0);
				a.onAction("Right", true, Math.abs(value) < stickdeadzone ? 0 : value);
			}
		} else if (axis == axis.getJoystick().getYAxis()) { //up/down normal stick
			//not mapped yet			
			
		} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS)) { //triggers?
			if (value > 0) { //brake
				a.onAction("Up", false, 0);
				a.onAction("Down", true, Math.abs(value) < stickdeadzone ? 0 : value); //less than deadzone = 0
			} else {
				a.onAction("Down", false, 0);
				a.onAction("Up", true, Math.abs(value) < stickdeadzone ? 0 : -value);
			}
			
		} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_ROTATION)) {
			//not mapped yet
				
		} else if (axis.getName().equals("Y Rotation")) { //The other stick
			//not mapped yet
			
		} else if (axis.getName().equals("X Rotation")) { //The other stick
			//not mapped yet
		
		} else if (axis == axis.getJoystick().getPovXAxis()) { //DPAD
			// TODO - fix the pov_x and pov_y buttons

		} else if (axis == axis.getJoystick().getPovYAxis()) {
		
		}
		
		arg0.isConsumed();
	}

	@Override
	public void onJoyButtonEvent(JoyButtonEvent arg0) {
		a.onAction(Integer.toString(arg0.getButtonIndex()), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
		
		arg0.isConsumed();
	}

	public void beginInput() {}
	public void endInput() {}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
}