package rallygame.car;

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

import rallygame.car.ray.RayCarControlInput;
import rallygame.helper.Duo;

public class JoystickEventListener implements RawInputListener {

	private static final float STICK_DEADZONE = 0.2f;

	private final RayCarControlInput input;

	private final Map<String, String> intToButton = new HashMap<>();
	private final Map<String, String> buttonToAction = new HashMap<>();
	private final Map<String, Duo<String,String>> axisToAction = new HashMap<>();
	
	public JoystickEventListener (RayCarControlInput a) {
		this.input = a;
		
		intToButton.put(JoystickButton.BUTTON_0, "A");
		intToButton.put(JoystickButton.BUTTON_1, "B");
		intToButton.put(JoystickButton.BUTTON_2, "X");
		intToButton.put(JoystickButton.BUTTON_3, "Y");
		
		intToButton.put(JoystickButton.BUTTON_4, "LeftBumper");
		intToButton.put(JoystickButton.BUTTON_5, "RightBumper");
		
		intToButton.put(JoystickButton.BUTTON_6, "Select");
		intToButton.put(JoystickButton.BUTTON_7, "Start");
		
		intToButton.put(JoystickButton.BUTTON_8, "LeftStick");
		intToButton.put(JoystickButton.BUTTON_9, "RightStick");
		
		//map to game actions
		buttonToAction.put("A", RayCarControlInput.ACTION_HANDBRAKE);
		buttonToAction.put("B", RayCarControlInput.ACTION_NITRO);
		
		buttonToAction.put("Start", RayCarControlInput.ACTION_RESET);
		buttonToAction.put("X", RayCarControlInput.ACTION_FLIP);
		buttonToAction.put("Y", RayCarControlInput.ACTION_JUMP);
		
		buttonToAction.put("RightStick", RayCarControlInput.ACTION_REVERSE);

		//map axis
		axisToAction.put(JoystickAxis.X_AXIS, new Duo<>(RayCarControlInput.ACTION_RIGHT, RayCarControlInput.ACTION_LEFT));
		axisToAction.put(JoystickAxis.Y_AXIS, new Duo<>(RayCarControlInput.ACTION_NOTHING, RayCarControlInput.ACTION_NOTHING));
		axisToAction.put(JoystickAxis.Z_AXIS, new Duo<>(RayCarControlInput.ACTION_NOTHING, RayCarControlInput.ACTION_NOTHING));
		
		axisToAction.put(JoystickAxis.LEFT_TRIGGER, new Duo<>(RayCarControlInput.ACTION_BRAKE, RayCarControlInput.ACTION_ACCEL));
		axisToAction.put(JoystickAxis.RIGHT_TRIGGER, new Duo<>(RayCarControlInput.ACTION_NOTHING, null));
		
		axisToAction.put(JoystickAxis.Z_ROTATION, new Duo<>(RayCarControlInput.ACTION_NOTHING, RayCarControlInput.ACTION_NOTHING));
		axisToAction.put(JoystickAxis.POV_X, new Duo<>(RayCarControlInput.ACTION_NOTHING, RayCarControlInput.ACTION_NOTHING));
		axisToAction.put(JoystickAxis.POV_Y, new Duo<>(RayCarControlInput.ACTION_NOTHING, RayCarControlInput.ACTION_NOTHING));
	}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		JoystickAxis axis = arg0.getAxis();
		float value = arg0.getValue();
		float valueAbs = Math.abs(value);
		arg0.isConsumed();

		var action = axisToAction.get(axis.getLogicalId());
		
		String actionS = null;
		if (value > 0) {
			actionS = action.first;
		} else {
			actionS = action.second;
		}

		input.handleInput(actionS, false, nonLinearInput(valueAbs, STICK_DEADZONE));
	}

	@Override
	public void onJoyButtonEvent(JoyButtonEvent arg0) {
		String button = this.intToButton.get(Integer.toString(arg0.getButtonIndex())); 
		input.handleInput(this.buttonToAction.get(button), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
		
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
		return output;
	}
}