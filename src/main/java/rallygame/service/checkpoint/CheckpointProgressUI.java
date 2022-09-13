package rallygame.service.checkpoint;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.game.DebugAppState;
import rallygame.helper.H;
import rallygame.service.Screen;
import rallygame.service.WorldGuiText;
import rallygame.service.WorldGuiText.WorldText;

public class CheckpointProgressUI extends BaseAppState {

    private final ICheckpointProgress progress;
    private final Map<RacerState, WorldText> positionLabels;

    private Container main;
    private RacerStateTableView progressTable;
    private Screen screen;

    private Label basicLabel;

    // debug things
    private Node debugNode;

    public CheckpointProgressUI(ICheckpointProgress progress) {
        this.progress = progress;
        this.positionLabels = new HashMap<>();
    }

    @Override
    protected void initialize(Application app) {
        screen = new Screen(app.getContext().getSettings());

        this.main = new Container();
        screen.topRightMe(main);
        ((SimpleApplication) app).getGuiNode().attachChild(main);

        progressTable = new RacerStateTableView(progress.getRaceState());
        this.main.addChild(progressTable);

        WorldGuiText textEngine = getState(WorldGuiText.class);
        for (RacerState state : progress.getRaceState()) {
            if (state == progress.getPlayerRacerState())
                continue;
            this.positionLabels.put(state, textEngine.addTextAt("yo", new Vector3f()));
        }
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(main);

        WorldGuiText textEngine = getState(WorldGuiText.class);
        for (WorldText text : positionLabels.values()) {
            textEngine.removeWorldText(text);
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    public void setBasicText(String text) {
        if (basicLabel != null)
            basicLabel.setText(text);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        List<RacerState> racers = progress.getRaceState();
        Collections.sort(racers);

        RacerState player = progress.getPlayerRacerState();
        progressTable.update(racers, player);

        screen.topRightMe(main);

        // update position in world labels
        int i = 0;
        for (RacerState racer : racers) {
            i++;
            if (racer == player)
                continue;

            WorldText text = this.positionLabels.get(racer);
            Vector3f pos = racer.car.location.add(0, 1, 0);
            text.setText(H.asOrdinal(i));
            text.setWorldPos(pos);
        }

        // update the debug checkpoint lines
        if (debugNode != null)
            ((SimpleApplication) getApplication()).getRootNode().detachChild(debugNode);
        if (getState(DebugAppState.class).DEBUG()) {
            debugNode = new Node("debugnode");
            for (RacerState entry : racers) {
                entry.arrow = rallygame.helper.Geo.makeShapeLine(getApplication().getAssetManager(), ColorRGBA.Cyan,
                        entry.car.location, entry.nextCheckpoint.position, 3);
                debugNode.attachChild(entry.arrow);
            }
            ((SimpleApplication) getApplication()).getRootNode().attachChild(debugNode);
        }
    }
}

class RacerStateTableView extends Container {

    private final int rowLength;

    public RacerStateTableView(List<RacerState> initialState) {
        int count = 0;
        String[] values = headerValueRows();
        this.addChild(new Label(""), count, 0);
        for (int i = 0; i < values.length; i++) {
            this.addChild(new Label(values[i]), count, i + 1);
        }
        count++;
        rowLength = values.length;

        for (RacerState state : initialState) {
            values = convertToValueRows(state, false);

            this.addChild(new Label(count + ""), count, 0);
            for (int i = 0; i < values.length; i++) {
                this.addChild(new Label(values[i]), count, i + 1);
            }
            count++;
        }
    }

    public void update(List<RacerState> states, RacerState playerState) {
        int count = 1;
        for (RacerState racer : states) {
            String[] values = convertToValueRows(racer, racer == playerState);
            for (int i = 0; i < values.length; i++) {
                // this is kind of a hack to prevent recreating labels
                ((Label) this.getChild((rowLength + 1) * count + i + 1)).setText(values[i]);
            }
            count++;
        }
    }

    private String[] headerValueRows() {
        return new String[] { "player", "lap", "diff", "" };
    }

    private String[] convertToValueRows(RacerState state, boolean isPlayer) {
        Duration d = state.duration;
        String durationStr = "";
        
        if (d != null) {
            durationStr = d.toString();
            /* TODO This only compiles in java 11
            if (d.toHoursPart() > 0)
                durationStr = "+59:99:999";
            else
                durationStr = String.format("%02d:%02d:%03d", d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
            */
        }

        return new String[] {
            state.getName(),
            state.lap + "(" + state.lastCheckpoint.num + ")",
            durationStr,
            isPlayer ? "-" : ""
        };
    }
}
