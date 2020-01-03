package car;

import com.jme3.app.state.AbstractAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;

import car.ray.RayCarControl;

public class RotatesCarCamera extends AbstractAppState {
	// render() based camera to prevent jumpiness

	private final Camera c;
	private final RayCarControl rcc;

	private final Vector3f offset;
	private final Vector3f lookAtHeight;

	private float angle;

	public RotatesCarCamera(Camera c, RayCarControl rcc) {
		super();

		this.c = c;
		this.rcc = rcc;

		this.offset = new Vector3f(0, rcc.getCarData().cam_offsetHeight, rcc.getCarData().cam_offsetLength);
		this.lookAtHeight = new Vector3f(0, rcc.getCarData().cam_lookAtHeight, 0);

		this.c.setLocation(rcc.getRootNode().getLocalTranslation().add(offset));
		this.c.lookAt(rcc.getRootNode().getLocalTranslation().add(lookAtHeight), Vector3f.UNIT_Y);
	}

	@Override
	public void update(float tpf) {
		if (!isEnabled())
			return;
		super.update(tpf);

		angle += tpf / 8;
	}

	@Override
	public void render(RenderManager rm) {
		Vector3f movedOffset = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y).mult(this.offset);
		Vector3f camPos = rcc.getRootNode().getLocalTranslation().add(movedOffset);

		this.c.setLocation(camPos);
		this.c.lookAt(rcc.getRootNode().getLocalTranslation().add(lookAtHeight), Vector3f.UNIT_Y);
	}
}