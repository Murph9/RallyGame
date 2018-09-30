package car.ray;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

//TODO move to global scope so other things can use this to listen
public class RayCarKeyListener implements RawInputListener {

	RayCarControl a;
	Map<Integer, String> layout = new HashMap<Integer, String>();
	
	public RayCarKeyListener(RayCarControl a) {
		this.a = a;
		
		layout.put(KeyInput.KEY_LEFT, "Left");
		layout.put(KeyInput.KEY_A, "Left");
		
		layout.put(KeyInput.KEY_RIGHT, "Right");
		layout.put(KeyInput.KEY_D, "Right");
		
		layout.put(KeyInput.KEY_UP, "Accel");
		layout.put(KeyInput.KEY_W, "Accel");
		
		layout.put(KeyInput.KEY_DOWN, "Brake");
		layout.put(KeyInput.KEY_S, "Brake");
		
		layout.put(KeyInput.KEY_LSHIFT, "Reverse");
		layout.put(KeyInput.KEY_RSHIFT, "Reverse");
		
		layout.put(KeyInput.KEY_SPACE, "Handbrake");

		layout.put(KeyInput.KEY_LCONTROL, "Nitro");
		layout.put(KeyInput.KEY_RCONTROL, "Nitro");
		
		layout.put(KeyInput.KEY_F, "Flip");
		layout.put(KeyInput.KEY_RETURN, "Reset");
		
		layout.put(KeyInput.KEY_Q, "Jump");
		
		layout.put(KeyInput.KEY_1, "Rotate180");
	}
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onKeyEvent(KeyInputEvent arg0) {
		if (layout.containsKey(arg0.getKeyCode())) {
			a.onAction(layout.get(arg0.getKeyCode()), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
		}
	}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}

}
