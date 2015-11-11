package game;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

class JoystickEventListner implements RawInputListener {

	private Controller con;
	
	JoystickEventListner (Controller con, InputManager input) {
		this.con = con;
		input.addRawInputListener(this);
	}
	
	public void beginInput() {}
	public void endInput() {}

	@Override
	public void onJoyAxisEvent(JoyAxisEvent arg0) {
		if (Math.abs(arg0.getValue()) > 0.1) { //because deadzone TODO setting
			con.setAxisValue(arg0.getAxis().getJoystick(), arg0.getAxis(), arg0.getValue());
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