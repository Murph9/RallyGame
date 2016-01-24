package game;

import com.jme3.input.JoystickAxis;
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
	
	ActionListener a;
	double stickdeadzone = 0.15f;
	
	JoystickEventListner (ActionListener a) {
		this.a = a;
	}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		JoystickAxis axis = arg0.getAxis();
		float value = arg0.getValue();
		
		if (Math.abs(value) > stickdeadzone) {

			//TODO fix
			if (axis == axis.getJoystick().getXAxis()) { // left/right normal stick
				if (value > 0) {
					a.onAction("Left", true, value);
					a.onAction("Right", true, 0);
				} else {
					a.onAction("Left", true, value);
					a.onAction("Right", true, 0);
				}
			}/* else if (axis == axis.getJoystick().getYAxis()) {
				cur.yAxis = -value;

			} else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS)) {
				// Note: in the above condition, we could check the axis name but
				// I have at least one joystick that reports 2 "Z Axis" axes.
				// In this particular case, the first one is the right one so
				// a name based lookup will find the proper one. It's a problem
				// because the erroneous axis sends a constant stream of values.
//				System.out.println("triggers=" + value);
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
//				TODO - fix the pov_x and pov_y buttons
				
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
			}*/
		}
		arg0.isConsumed();
	}

	@Override
	public void onJoyButtonEvent(JoyButtonEvent arg0) {
		
		arg0.isConsumed();
	}

	public void beginInput() {}
	public void endInput() {}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
}

/*package game;

import game.Controller.PlayerControl;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

class JoystickEventListner implements RawInputListener {

	ActionListener a;
	
	JoystickEventListner (ActionListener a) {
		this.a = a;
	}
	
	public void beginInput() {}
	public void endInput() {}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		if (Math.abs(arg0.getValue()) > 0.1) { //because deadzone TODO setting
			a.onAction(arg0.getAxis().getJoystick(), arg0.getAxis(), arg0.getValue());
		}
	}

	@Override
	public void onJoyButtonEvent(JoyButtonEvent arg0) {
		con.setButtonValue(arg0.getButton().getJoystick(), arg0.getButton(), arg0.isPressed());
	}

	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
}
*/