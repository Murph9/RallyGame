package drive.race;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.ray.RayCarControl;
import helper.H;

public class DriveRaceUI {

    private final Map<RayCarControl, RaceUIRow> rows;
    private final Container main;
    
    private RayCarControl player;
    private Container headerRow;

    public DriveRaceUI (DriveRaceProgress progress, Node uiRoot) {
        rows = new HashMap<>();
        
        this.main = new Container();
        main.addChild(new Label("Yo"));
        main.setLocalTranslation(new Vector3f(100, 400, 0));
        uiRoot.attachChild(main);

    }
    
    public void updateState(float tpf, List<Entry<RayCarControl, RacerState>> racers, int checkpointCount) {
        if (headerRow == null && rows.isEmpty()) {
            headerRow = new Container();
            headerRow.addChild(new Label("name"), 0, 0);
            headerRow.addChild(new Label("lap"), 0, 1);
            headerRow.addChild(new Label("ch /" + checkpointCount), 0, 2);
            headerRow.addChild(new Label("dist"), 0, 3);
            headerRow.addChild(new Label(""), 0, 4);

            for (Entry<RayCarControl, RacerState> racer : racers) {
                rows.put(racer.getKey(), new RaceUIRow());
            }
        }
        
        Collections.sort(racers, new Comparator<Entry<RayCarControl, RacerState>>() {
            @Override
            public int compare(Entry<RayCarControl, RacerState> o1, Entry<RayCarControl, RacerState> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        main.detachAllChildren();

        int x = 0;
        for (Spatial element: headerRow.getChildren()) {
            main.addChild((Node)element, 0, x);
            x++;
        }

        int count = 1;
        for (Entry<RayCarControl, RacerState> racer: racers) {
            RaceUIRow row = rows.get(racer.getKey());
            Label[] elements = row.update(racer.getKey().getCarData().name, racer.getValue(), player == racer.getKey());
            for (int i = 0; i < elements.length; i++) {
                main.addChild(elements[i], count, i);
            }
            count++;
        }
    }

	public void setPlayer(RayCarControl player) {
        this.player = player;
	}
}

class RaceUIRow {

    private final Label name;
    private final Label lap;
    private final Label checkpoint;
    private final Label distance;
    private final Label playerTag;

    public RaceUIRow() {
        this.name = new Label("");
        this.lap = new Label("");
        this.checkpoint = new Label("");
        this.distance = new Label("");
        this.playerTag = new Label("");
    }

    public Label[] update(String name, RacerState state, boolean isPlayer) {
        this.name.setText(name);
        this.lap.setText(state.lap+"");
        this.checkpoint.setText(state.nextCheckpoint.num+"");
        this.distance.setText(H.roundDecimal(state.distanceToNextCheckpoint, 1));
        this.playerTag.setText(isPlayer ? "---" : "");

        return new Label[] {
            this.name, lap, checkpoint, distance, playerTag
        };
    }
}
