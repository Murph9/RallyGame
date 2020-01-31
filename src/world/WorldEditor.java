package world;

import java.util.function.Consumer;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.RollupPanel;

import world.wp.WP.DynamicType;

public class WorldEditor extends Container {

	private World currentSelection;
	private Consumer<World> a;
	
	public float width;
	
	public WorldEditor(Consumer<World> a) {
		super("WorldEditor");
		
		this.a = a;
		init();
	}
	
	@SuppressWarnings("unchecked")
	private void init() {
		
		RollupPanel rp = new RollupPanel("Choose Map", "");
		addChild(rp);
		
		Container rpContents = new Container();
		int i = 0;
		int j = 0;
		rpContents.addChild(new Label("Static"), j, i);
		j++;
		for (StaticWorld s : StaticWorld.values()) {
			addButton(rpContents, WorldType.STATIC, s.name(), j, i);
			i++;
		}
		i = 0;
		j++;
		rpContents.addChild(new Label("Dynamic"), j, i);
		j++;
		for (DynamicType d : DynamicType.values()) {
			addButton(rpContents, WorldType.DYNAMIC, d.name(), j, i);
			i++;
		}
		i = 0;
		j++;
		rpContents.addChild(new Label("Other"), j, i);
		j++;
		for (WorldType t: WorldType.values()) {
			if (t != WorldType.STATIC && t != WorldType.DYNAMIC && t != WorldType.NONE) {
				addButton(rpContents, t, t.name(), j, i);
				i++;
			}
		}
		i = 0;
		j++;
		Button button = rpContents.addChild(new Button("Set"), j, i);
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	if (currentSelection != null)
            		a.accept(currentSelection);
            }
        });
		
		
		rp.setContents(rpContents);
		width = this.getPreferredSize().x;
		rp.setOpen(false);
	}
	
	@SuppressWarnings("unchecked")
	private void addButton(Container myWindow, WorldType world, String s, int j, int i) {
		Button button = myWindow.addChild(new Button(s), j, i);
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	setWorld(world.name(), s);
            }
        });
	}
	private void setWorld(String typeStr, String subType) {
        currentSelection = WorldType.getWorld(typeStr, subType);
	}
}
