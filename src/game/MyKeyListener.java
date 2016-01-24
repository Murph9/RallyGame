package game;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

public class MyKeyListener implements RawInputListener {

	ActionListener a;
	Map<Integer, String> layout = new HashMap<Integer, String>();
	
	public MyKeyListener(ActionListener a) {
		this.a = a;
		
		layout.put(KeyInput.KEY_LEFT, "Left");
		layout.put(KeyInput.KEY_RIGHT, "Right");
		layout.put(KeyInput.KEY_UP, "Up");
		layout.put(KeyInput.KEY_DOWN, "Down");
		layout.put(KeyInput.KEY_Q, "Jump");
		layout.put(KeyInput.KEY_SPACE, "Handbrake");
		layout.put(KeyInput.KEY_RETURN, "Reset");
		layout.put(KeyInput.KEY_Z, "Lookback");
		layout.put(KeyInput.KEY_LSHIFT, "Reverse");
	}
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onKeyEvent(KeyInputEvent arg0) {
		if (layout.containsKey(arg0.getKeyCode())) {
			a.onAction(layout.get(arg0.getKeyCode()), arg0.isPressed(), 0);
		}
	}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}

}
