package car;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;

import car.ray.RayCarControl;
import helper.Log;

public class JoystickEventListener implements RawInputListener {

	//TODO its very hardcoded
	
	private Map<String, String> intToButton = new HashMap<String, String>();
	private Map<String, String> buttonToAction = new HashMap<String, String>();
	
	private RayCarControl a;
	private float stickdeadzone = 0.2f;
	
	public JoystickEventListener (RayCarControl a) {
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
		
		
		buttonToAction.put("A", "Handbrake");
		buttonToAction.put("B", "Nitro");
		
		buttonToAction.put("Start", "Reset");
		buttonToAction.put("X", "Flip");
		buttonToAction.put("Y", "Jump");
		
		buttonToAction.put("RightStick", "Reverse");
	}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		JoystickAxis axis = arg0.getAxis();
		float value = arg0.getValue();
		float valueAbs = Math.abs(value);
		
		//TODO fix
		if (axis == axis.getJoystick().getXAxis()) { // left/right normal stick
			if (value > 0) {
				a.onAction("Left", false, 0);
				a.onAction("Right", true, nonLinearInput(valueAbs, stickdeadzone));
			} else {
				a.onAction("Right", false, 0);
				a.onAction("Left", true, nonLinearInput(valueAbs, stickdeadzone));
			}
		} else if (axis == axis.getJoystick().getYAxis()) { //up/down normal stick
			//not mapped yet			
			
		} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS)) { //triggers?
			if (value > 0) { //brake
				a.onAction("Accel", false, 0);
				a.onAction("Brake", true, nonLinearInput(valueAbs, stickdeadzone));
			} else {
				a.onAction("Brake", false, 0);
				a.onAction("Accel", true, nonLinearInput(valueAbs, stickdeadzone));
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
		String button = this.intToButton.get(Integer.toString(arg0.getButtonIndex())); 
		a.onAction(this.buttonToAction.get(button), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
		
		arg0.isConsumed();
	}

	public void beginInput() {}
	public void endInput() {}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	
	private float nonLinearInput(float input, float deadzone) {
		if (input < deadzone)//less than deadzone = 0
			return 0;
		
		float offsetValue = ((1)*(input - deadzone))/(1 - deadzone); 
		
		
		float output =1/(1+FastMath.exp(-offsetValue*8 + 4)) + 0.03f; //0.03 hack so 1=1
		Log.p(input, output);
		return output;
	}
}