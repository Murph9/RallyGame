package car;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.math.Matrix3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.InsetsComponent;

import helper.H;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class CarEditor extends Container {

	private MyPhysicsVehicle p;
	private Map<Field, FieldEntry> fields;
	private Runnable a;
	
	public CarEditor(MyPhysicsVehicle p, Runnable a) {
		super("CarEditor");
		
		this.p = p;
		this .a = a;
		this.fields = new HashMap<Field, FieldEntry>();
		try {
			attachTree(this.p.car, this, "Car Data", 0);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void attachTree(Object o, Panel root, String name, int depth) throws IllegalArgumentException, IllegalAccessException {
		//TODO scroll. couldn't find a lemur way of doing it, you should attempt a jme3 input way now
		
		if (o == null)
			return;
		if (depth > 4)
			return;
		if (o instanceof com.jme3.math.Vector3f)
			return;
		if (depth == 0) {
			Button b = new Button("Save");
			b.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                	saveAllFields();
                }
            });
			addChild(b);
		}
		
		Class<?> clazz = o.getClass();
		RollupPanel rp = new RollupPanel(name + ": " + clazz.getName(), "");
		Container rpContents = new Container();

		addChild(rp);
		int i = 0; 
		int j = 0;
		
		for (Field f: clazz.getFields()) {
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
				attachTree(f.get(o), rpContents, f.getName(), depth + 1);
				continue;
			}
			
			List<TextField> fields = new LinkedList<TextField>();
			rpContents.addChild(new Label(str), ++i, ++j);
			if (value.getClass().isArray()) {
				Object[] os = (Object[])value;
				for (int index = 0; index < os.length; index++) {
					TextField tf = new TextField(os[index].toString());
					tf.setInsetsComponent(new InsetsComponent(0, 2, 0, 0));
					rpContents.addChild(tf, i, ++j);
					fields.add(tf);
				}
			} else {
				TextField tf = new TextField(value.toString());
				rpContents.addChild(tf, i, ++j);
				fields.add(tf);
			}
			Button b = new Button("Save");
			b.addClickCommands(new Command<Button>() {
                @Override
                public void execute( Button source ) {
                	saveField(f);
                }
            });
			rpContents.addChild(b, i, ++j);
			
			FieldEntry fe = new FieldEntry(f, o, fields.toArray(new TextField[fields.size()]));
			this.fields.put(f, fe);
		}
		
		rp.setContents(rpContents);
	}
	
	private void saveField(Field f) {
		FieldEntry fe = this.fields.get(f);
		
		if (fe.inputs.length != 1) {
			throw new NotImplementedException();
		}
		
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
			} else {
				throw new NotImplementedException();
			}
			
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			H.p("Remember this can only set public fields");
		}
		
		if (a != null)
			a.run();
	}
	
	private void saveAllFields() {
		try {
			throw new NotImplementedException();
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
		List<Entry<String, Object>> list = new ArrayList<Entry<String, Object>>(H.toMap(p.car).entrySet());

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
