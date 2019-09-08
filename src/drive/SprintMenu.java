package drive;

import com.jme3.app.Application;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.ray.RayCarControl;
import helper.H;

public class SprintMenu extends DriveMenu {

	private Container countDown;
	private Label countDownLabel;

	private Container scores;
	private Label scoresLabel;


    public SprintMenu(DriveBase drive) {
        super(drive);
    }

    
	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		countDown = new Container();
		countDownLabel = new Label("Sprint state?");
		countDown.attachChild(countDownLabel);
		countDown.setLocalTranslation(H.screenMiddle(app.getContext().getSettings()));
		rootNode.attachChild(countDown);

		scores = new Container();
		scoresLabel = new Label("scoresLabel");
		scores.attachChild(scoresLabel);
		scores.setLocalTranslation(H.screenTopLeft(app.getContext().getSettings()));
		rootNode.attachChild(scores);
	}

	public void setText(String text) {
		if (countDownLabel != null)
			countDownLabel.setText(text);
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);

		StringBuilder sb = new StringBuilder();
		for (RayCarControl car: this.drive.cb.getAll()) {
			sb.append(H.leftPad(H.roundDecimal(car.getPhysicsLocation().z, 0), 5, ' '));
			sb.append("m @ ");
			sb.append(H.leftPad(H.roundDecimal(car.getCurrentVehicleSpeedKmHour(), 0), 4, ' '));
			sb.append("km/h ");
			sb.append(H.substringBeforeFirst(H.substringAfterLast(car.getCarData().carModel, '/'), '.'));
			sb.append("\n");
		}
		scoresLabel.setText(sb.toString());
	}
}