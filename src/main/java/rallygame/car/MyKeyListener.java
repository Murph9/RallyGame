package rallygame.car;

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

import rallygame.car.ray.RayCarControlInput;

public class MyKeyListener implements RawInputListener {

	private final RayCarControlInput input;
	Map<Integer, String> layout = new HashMap<Integer, String>();
	
	public MyKeyListener(RayCarControlInput a) {
		this.input = a;
		
		layout.put(KeyInput.KEY_LEFT, RayCarControlInput.ACTION_LEFT);
		layout.put(KeyInput.KEY_A, RayCarControlInput.ACTION_LEFT);
		
		layout.put(KeyInput.KEY_RIGHT, RayCarControlInput.ACTION_RIGHT);
		layout.put(KeyInput.KEY_D, RayCarControlInput.ACTION_RIGHT);
		
		layout.put(KeyInput.KEY_UP, RayCarControlInput.ACTION_ACCEL);
		layout.put(KeyInput.KEY_W, RayCarControlInput.ACTION_ACCEL);
		
		layout.put(KeyInput.KEY_DOWN, RayCarControlInput.ACTION_BRAKE);
		layout.put(KeyInput.KEY_S, RayCarControlInput.ACTION_BRAKE);
		
		layout.put(KeyInput.KEY_LSHIFT, RayCarControlInput.ACTION_REVERSE);
		layout.put(KeyInput.KEY_RSHIFT, RayCarControlInput.ACTION_REVERSE);
		
		layout.put(KeyInput.KEY_SPACE, RayCarControlInput.ACTION_HANDBRAKE);

		layout.put(KeyInput.KEY_LCONTROL, RayCarControlInput.ACTION_NITRO);
		layout.put(KeyInput.KEY_RCONTROL, RayCarControlInput.ACTION_NITRO);
		
		//hacks
		layout.put(KeyInput.KEY_F, RayCarControlInput.ACTION_FLIP);
		layout.put(KeyInput.KEY_RETURN, RayCarControlInput.ACTION_RESET);
		layout.put(KeyInput.KEY_Q, RayCarControlInput.ACTION_JUMP);
		layout.put(KeyInput.KEY_I, RayCarControlInput.ACTION_IgnoreSteeringSpeedFactor);
		layout.put(KeyInput.KEY_T, RayCarControlInput.ACTION_IgnoreTractionModel);
	}
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onKeyEvent(KeyInputEvent arg0) {
		if (layout.containsKey(arg0.getKeyCode())) {
			input.handleInput(layout.get(arg0.getKeyCode()), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
		}
	}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}

}
