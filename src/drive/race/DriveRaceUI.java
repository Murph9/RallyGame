package drive.race;

import java.util.Collections;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import helper.H;
import helper.Screen;

public class DriveRaceUI extends BaseAppState {

    private final DriveRaceProgress progress;

    private Container main;
    private RaceProgressTable progressTable;

    private Container basicPanel;
    private Label basicLabel;

    public DriveRaceUI(DriveRaceProgress progress) {
        this.progress = progress;
    }
    
    @Override
    protected void initialize(Application app) {
        Screen screen = new Screen(app.getContext().getSettings());

        basicPanel = new Container();
        basicLabel = new Label("Race state?");
        basicPanel.attachChild(basicLabel);
        basicPanel.setLocalTranslation(screen.topLeft().add(0, -25, 0));
        ((SimpleApplication) app).getGuiNode().attachChild(basicPanel);

        this.main = new Container();
        main.setLocalTranslation(screen.topLeft().add(0, -100, 0));
        ((SimpleApplication) app).getGuiNode().attachChild(main);

        progressTable = new RaceProgressTable(progress.getRaceState());
        this.main.addChild(progressTable);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(basicPanel);
        ((SimpleApplication) app).getGuiNode().detachChild(main);
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

    public void setBasicText(String text) {
        if (basicLabel != null)
            basicLabel.setText(text);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);

        List<RacerState> racers = progress.getRaceState();

        Collections.sort(racers);

        RacerState st = progress.getPlayerRacerState();
        progressTable.update(racers, st);
    }
}

class RaceProgressTable extends Container {

    private final int rowLength;
    
    public RaceProgressTable(List<RacerState> initialState) {
        int count = 0;
        String[] values = convertToValueRows(HEADER, false);
        for (int i = 0; i < values.length; i++) {
            this.addChild(new Label(values[i]), count, i);
        }
        count++;
        rowLength = values.length;

        for (RacerState state: initialState) {
            values = convertToValueRows(state, false);
            for (int i = 0; i < values.length; i++) {
                this.addChild(new Label(values[i]), count, i);
            }
            count++;
        }
    }

    public void update(List<RacerState> states, RacerState playerState) {
        int count = 1;
        for (RacerState racer: states) {
            String[] values = convertToValueRows(racer, racer == playerState); 
            for (int i = 0; i < values.length; i++) {
                ((Label)this.getChild(rowLength*count + i)).setText(values[i]);
            }
            count++;
        }
    }

    private static RacerState HEADER = new RacerState("Player");

    private String[] convertToValueRows(RacerState state, boolean isPlayer) {
        if (state == HEADER) {
            return new String[] {
                state.name,
                "lap",
                "checkpoint",
                "distance",
                "playerTag"
            };
        }

        return new String[] {
            state.name,
            state.lap+"",
            state.nextCheckpoint.num+"",
            H.roundDecimal(state.distanceToNextCheckpoint, 1),
            isPlayer ? "---" : ""
        };
    }
}
