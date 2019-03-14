package car;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.InsetsComponent;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;

import car.data.Car;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import game.App;
import helper.H;
import helper.Log;

//TODO cleanup
public class CarEditor extends Container {

	private RayCarControl p;
	private HashMap<String, FieldEntry> fields;
	private Consumer<CarDataConst> reloadCar;
	private Function<Car, RayCarControl> resetCar;
	
	private boolean mouseIn = false;
	private AnalogListener actionListener = new AnalogListener() {
		public void onAnalog(String name, float value, float tpf) {
			value *= name.equals("scroll_neg") ? -1 : 1;
			scrollFields(value);
		}
	};
	
	public CarEditor(RayCarControl p, Consumer<CarDataConst> a, Function<Car, RayCarControl> reset) {
		super("CarEditor");
		
		this.p = p;
		this.reloadCar = a;
		this.resetCar = reset;
		this.fields = new HashMap<String, FieldEntry>();
		try {
			attachTree(this.p.getCarData(), this, "Car Data");
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		//and scroll listener
		InputManager i = App.rally.getInputManager();
		i.addMapping("scroll", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
		i.addMapping("scroll_neg", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
		i.addListener(actionListener, "scroll");
		i.addListener(actionListener, "scroll_neg");
		
		MouseEventControl.addListenersToSpatial(this, new DefaultMouseListener() {
			@Override
			public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
				mouseIn = true;
			}
			@Override
			public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
				mouseIn = false;
			}
		});
	}
	
	private void scrollFields(float value) {
		if (mouseIn)
			this.setLocalTranslation(getLocalTranslation().add(0, value*30, 0));
	}

	@SuppressWarnings("unchecked")
	private void attachTree(Object o, Container root, String name) throws IllegalArgumentException, IllegalAccessException {
		if (o == null)
			return;
		if (o instanceof com.jme3.math.Vector3f)
			return; //:(
		if (root == this) {
			this.fields.clear(); //reset fields
			
			RollupPanel rp = new RollupPanel("Change Car Type", "");
			addChild(rp, 0, 0);
			rp.setOpen(false);
			Container c = new Container();
			int i = 0;
			for (Car car: Car.values()) {
				Button carButton = c.addChild(new Button(car.name()), 0, i++);
				carButton.addClickCommands(new Command<Button>() {
		            @Override
		            public void execute(Button source) {
		            	p = resetCar.apply(car);
		            	try {
		            		detachAllChildren();
		        			attachTree(p.getCarData(), CarEditor.this, "Car Data");
		        		} catch (IllegalArgumentException | IllegalAccessException e) {
		        			e.printStackTrace();
		        		}
		            }
		        });
			}
			rp.setContents(c);
			
			//root stuff
			addChild(new Label("Car Editor"), 1, 0);
			
			Button b = new Button("Save All");
			b.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                	saveAllFields();
                }
            });
			addChild(b, 2, 0);
		}
		
		Class<?> clazz = o.getClass();
		RollupPanel rp = new RollupPanel(name + ": " + clazz.getName(), "");
		addChild(rp);
		rp.setOpen(false);
		
		Container rpContents = new Container();

		int i = 0;
		int j = 0;
		
		for (Field f: clazz.getFields()) {
			if (Modifier.isFinal(f.getModifiers()))
				continue;
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			
			j = 0;
			
			String str = null;
			Object value = null;
			if (f.getType() == float.class) {
				str = f.getName();
				value = f.getFloat(o);
			} else if (f.getType() == String.class) {
				str = f.getName();
				value = f.get(o);
			} else if (f.getType() == int.class) {
				str = f.getName();
				value = f.getInt(o);
			} else if (f.getType() == boolean.class) {
				str = f.getName();
				value = f.getBoolean(o);
			} else if (f.getType().isArray()) {
				str = f.getName() + f.get(o).getClass().getComponentType().getName();
				value = unpackArray(f.get(o));
			} else {
				attachTree(f.get(o), rpContents, f.getName());
				continue;
			}
			
			InsetsComponent ic = new InsetsComponent(1, 2, 1, 1);
			
			List<TextField> fields = new LinkedList<TextField>();
			rpContents.addChild(new Label(str), ++i, ++j);
			if (value == null) {
				//TODO no idea
			} else if (value.getClass().isArray()) {
				Object[] os = (Object[])value;
				for (int index = 0; index < os.length; index++) {
					TextField tf = new TextField(os[index].toString());
					tf.setInsetsComponent(ic);
					rpContents.addChild(tf, i, ++j);
					fields.add(tf);
				}
			} else {
				TextField tf = new TextField(value.toString());
				tf.setInsetsComponent(ic);
				rpContents.addChild(tf, i, ++j);
				fields.add(tf);
			}
			Button b = new Button("Save");
			b.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                	saveField(name+""+f.getName());
                }
            });
			rpContents.addChild(b, i, ++j);
			
			FieldEntry fe = new FieldEntry(f, o, fields.toArray(new TextField[fields.size()]));
			this.fields.put(name+""+f.getName(), fe);
		}
		
		rp.setContents(rpContents);
	}
	
	private void saveField(String s) {
		FieldEntry fe = this.fields.get(s);
		Field f = fe.f;
		try {
			String str = fe.inputs[0].getText();
			if (f.getType() == float.class) {
				f.setFloat(fe.o, Float.parseFloat(str));
			} else if (f.getType() == String.class) {
				f.set(fe.o, str);
			} else if (f.getType() == int.class) {
				f.setInt(fe.o, Integer.parseInt(str));
			} else if (f.getType() == boolean.class) {
				f.setBoolean(fe.o, Boolean.parseBoolean(str));
			} else if (f.getType().isArray()) {
				//TODO setting float arrays is untested, probably need to check the type of the elements
				float[] values = new float[fe.inputs.length]; //TODO other types
				for (int i = 0; i < fe.inputs.length; i++) {
					values[i] = Float.parseFloat(fe.inputs[i].getText());
				}
				f.set(fe.o, values);
			} else {
				throw new UnsupportedOperationException();
			}
			
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			Log.p("Remember this can only set public fields");
		}
		
		if (reloadCar != null)
			reloadCar.accept(p.getCarData());
	}
	
	private void saveAllFields() {
		try {
			throw new UnsupportedOperationException();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Object[] unpackArray(Object array) {
	    Object[] array2 = new Object[Array.getLength(array)];
	    for(int i=0;i<array2.length;i++)
	        array2[i] = Array.get(array, i);
	    return array2;
	}
	

	public String carDataToString() {
		List<Entry<String, Object>> list = new ArrayList<Entry<String, Object>>(H.toMap(p.getCarData()).entrySet());

		list.sort((a,b) -> a.getKey().compareTo(b.getKey()));
		
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> e: list) { //tree set sorts values
			sb.append(e.getKey()+": "+e.getValue()+"\n");
		}
		
		return sb.toString();
	}
	
	class FieldEntry {
		Field f; //field of the...
		Object o; //...object
		TextField[] inputs;
		
		public FieldEntry(Field f, Object o, TextField[] inputs) {
			this.f = f;
			this.o = o;
			this.inputs = inputs;
		}
	}
}
