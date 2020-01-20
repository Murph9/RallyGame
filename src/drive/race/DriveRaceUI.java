package drive.race;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
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

        for (Entry<RayCarControl, RacerState> racer: racers) {
            RaceUIRow row = rows.get(racer.getKey());
            row.update(racer.getKey().getCarData().name, racer.getValue(), player == racer.getKey());
            main.addChild(row);
        }
    }

	public void updateState(float tpf, List<Entry<RayCarControl, RacerState>> racers, Checkpoint[] checkpoints) {
        main.detachAllChildren();
        //TODO slow

        Collections.sort(racers, new Comparator<Entry<RayCarControl, RacerState>>() {
            @Override
            public int compare(Entry<RayCarControl, RacerState> o1, Entry<RayCarControl, RacerState> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        int count = 0;
        main.addChild(new Label("name"), count, 0);
        main.addChild(new Label("lap"), count, 1);
        main.addChild(new Label("ch /" + checkpoints.length), count, 2);
        main.addChild(new Label("dist"), count, 3);
        main.addChild(new Label(""), count, 4);
        count++;

        
        for (Entry<RayCarControl, RacerState> racer: racers) {
            RacerState state = racer.getValue();
            main.addChild(new Label(racer.getKey().getCarData().name), count, 0);
            main.addChild(new Label(state.lap+""), count, 1);
            main.addChild(new Label(state.nextCheckpoint.num+""), count, 2);
            main.addChild(new Label(H.roundDecimal(state.distanceToNextCheckpoint, 1)), count, 3);
            main.addChild(new Label(player == racer.getKey() ? "---" : ""), count, 4);
            count++;
        }
	}

	public void setPlayer(RayCarControl player) {
        this.player = player;
	}
}

class RaceUIRow extends Container {

    private final Label name;
    private final Label lap;
    private final Label checkpoint;
    private final Label distance;
    private final Label playerTag;

    public RaceUIRow() {
        this.name = this.addChild(new Label(""), 0, 0);
        this.lap = this.addChild(new Label(""), 0, 1);
        this.checkpoint = this.addChild(new Label(""), 0, 2);
        this.distance = this.addChild(new Label(""), 0, 3);
        this.playerTag = this.addChild(new Label(""), 0, 4);
    }

    public void update(String name, RacerState state, boolean isPlayer) {
        this.name.setText(name);
        this.lap.setText(state.lap+"");
        this.checkpoint.setText(state.nextCheckpoint.num+"");
        this.distance.setText(H.roundDecimal(state.distanceToNextCheckpoint, 1));
        this.playerTag.setText(isPlayer ? "---" : "");
    }
}
