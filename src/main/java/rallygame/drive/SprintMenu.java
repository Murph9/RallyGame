package rallygame.drive;

import java.util.Collections;
import java.util.LinkedList;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.H;
import rallygame.service.Screen;
import rallygame.service.Screen.HorizontalPos;
import rallygame.service.Screen.VerticalPos;

public class SprintMenu extends DriveMenu {

    private Node rootNode;

	private Container countDown;
	private Label countDownLabel;

	private Container scores;
	private Label scoresLabel;


    public SprintMenu(IDrive drive) {
        super(drive);
    }

    
	@Override
	public void initialize(Application app) {
		super.initialize(app);
        
        this.rootNode = new Node("root SprintMenu node");
        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().attachChild(rootNode);

        Screen screen = new Screen(app.getContext().getSettings());

		countDown = new Container();
		countDownLabel = new Label("Sprint state?");
		countDown.attachChild(countDownLabel);
		countDown.setLocalTranslation(screen.get(HorizontalPos.Middle, VerticalPos.Middle));
		rootNode.attachChild(countDown);

		scores = new Container();
		scoresLabel = new Label("scoresLabel");
		scores.attachChild(scoresLabel);
		scores.setLocalTranslation(screen.get(HorizontalPos.Left, VerticalPos.Top));
		rootNode.attachChild(scores);
	}

    @Override
    public void cleanup(Application app) {
        super.cleanup(app);

        ((SimpleApplication) app).getGuiNode().detachChild(rootNode);
    }

	public void setText(String text) {
		if (countDownLabel != null)
			countDownLabel.setText(text);
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);

		StringBuilder sb = new StringBuilder();
		var carList = new LinkedList<RayCarControl>(this.drive.getAllCars());
		Collections.sort(carList, (o1, o2) -> (int)(o2.location.z - o1.location.z));
		for (RayCarControl car: carList) {
			sb.append(H.leftPad(H.roundDecimal(car.location.z, 0), 5, ' '));
			sb.append("m @ ");
			sb.append(H.leftPad(H.roundDecimal(car.getCurrentVehicleSpeedKmHour(), 0), 4, ' '));
			sb.append("km/h ");
			sb.append(H.substringBeforeFirst(H.substringAfterLast(car.getCarData().carModel, '/'), '.'));
			sb.append("\n");
		}
		scoresLabel.setText(sb.toString());
	}
}
